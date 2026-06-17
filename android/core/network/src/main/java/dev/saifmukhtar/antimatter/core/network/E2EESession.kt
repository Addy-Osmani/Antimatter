package dev.saifmukhtar.antimatter.core.network

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

class E2EESession {
    private val keyPair = KeyPairGenerator.getInstance("X25519").generateKeyPair()
    
    // We send this to the Python gateway in the HELLO message.
    // In Java, X25519 public key encoded is 44 bytes (12-byte SPKI prefix + 32-byte raw key).
    // The Python gateway expects the raw 32 bytes.
    val publicKeyBase64: String = run {
        val encoded = keyPair.public.encoded
        val rawBytes = if (encoded.size == 44) encoded.copyOfRange(12, 44) else encoded
        Base64.encodeToString(rawBytes, Base64.NO_WRAP)
    }

    private var c2sKey: ByteArray? = null
    private var s2cKey: ByteArray? = null
    private val msgCounter = java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * Completes the ECDH handshake and derives the directional keys.
     * @param gatewayPubKeyBase64 The x25519_pub value from the QR code (raw bytes, or X.509 encoded)
     */
    fun deriveSessionKeys(gatewayPubKeyBase64: String) {
        val pubKeyBytes = Base64.decode(gatewayPubKeyBase64, Base64.DEFAULT)
        
        // In Java/Android, X25519 keys must be X.509 encoded. The Python cryptography library
        // public_bytes_raw() returns raw 32 bytes. We must wrap it in the X.509 SubjectPublicKeyInfo prefix.
        val x509Bytes = if (pubKeyBytes.size == 32) {
            // SPKI prefix for X25519 (OID 1.3.101.110 = 2B 65 6E)
            val prefix = byteArrayOf(
                0x30, 0x2A, 0x30, 0x05, 0x06, 0x03, 0x2B, 0x65, 0x6E, 0x03, 0x21, 0x00
            )
            prefix + pubKeyBytes
        } else {
            pubKeyBytes
        }

        val kf = KeyFactory.getInstance("X25519")
        val gatewayPublicKey = kf.generatePublic(X509EncodedKeySpec(x509Bytes))

        val keyAgreement = KeyAgreement.getInstance("X25519")
        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(gatewayPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        // Derive directional keys using RFC 5869 HKDF-SHA256
        c2sKey = hkdfSha256(sharedSecret, "antimatter-v1:client-to-server".toByteArray())
        s2cKey = hkdfSha256(sharedSecret, "antimatter-v1:server-to-client".toByteArray())
    }

    /**
     * Encrypts a payload.
     * Python AESGCM prepends the tag or appends it. JCA AES/GCM appends the tag to the ciphertext.
     */
    fun encrypt(plaintext: String, direction: String = "cmd:"): EncryptedEnvelope {
        val key = c2sKey ?: throw IllegalStateException("Session keys not derived")
        val id = msgCounter.incrementAndGet()
        
        val aad = "$direction:v1:msg_id:$id".toByteArray()
        val nonce = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, nonce)
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        cipher.updateAAD(aad)
        val ct = cipher.doFinal(plaintext.toByteArray())

        return EncryptedEnvelope(
            iv = Base64.encodeToString(nonce, Base64.NO_WRAP),
            ct = Base64.encodeToString(ct, Base64.NO_WRAP),
            aad = String(aad)
        )
    }

    /**
     * Decrypts an incoming envelope.
     */
    fun decrypt(ivB64: String, ctB64: String, aad: String, expectedDirection: String = "output:"): String {
        val key = s2cKey ?: throw IllegalStateException("Session keys not derived")
        
        if (!aad.startsWith(expectedDirection)) {
            throw IllegalArgumentException("AAD direction mismatch: expected prefix $expectedDirection, got $aad")
        }

        val nonce = Base64.decode(ivB64, Base64.DEFAULT)
        val ct = Base64.decode(ctB64, Base64.DEFAULT)
        val aadBytes = aad.toByteArray()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, nonce)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        cipher.updateAAD(aadBytes)
        
        val plaintextBytes = cipher.doFinal(ct)
        return String(plaintextBytes)
    }

    data class EncryptedEnvelope(
        val iv: String,
        val ct: String,
        val aad: String
    )

    // --- RFC 5869 HKDF-SHA256 Implementation (No Third-Party Libs) ---
    private fun hkdfSha256(ikm: ByteArray, info: ByteArray, outLength: Int = 32): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        
        // Step 1: Extract (Since salt is not provided in Python, we use a zero-filled byte array of hash length)
        val salt = ByteArray(32) // SHA-256 hash length is 32 bytes
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(ikm)
        
        // Step 2: Expand
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        var t = ByteArray(0)
        val okm = ByteArray(outLength)
        var offset = 0
        var blockIndex = 1.toByte()

        val n = ceil(outLength.toDouble() / 32).toInt()
        for (i in 0 until n) {
            mac.update(t)
            mac.update(info)
            mac.update(blockIndex)
            t = mac.doFinal()

            val copyLength = minOf(32, outLength - offset)
            System.arraycopy(t, 0, okm, offset, copyLength)
            offset += copyLength
            blockIndex++
        }
        return okm
    }
}
