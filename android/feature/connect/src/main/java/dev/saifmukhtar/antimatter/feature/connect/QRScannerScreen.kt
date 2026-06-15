package dev.saifmukhtar.antimatter.feature.connect

import android.Manifest
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.PhotoLibrary
import com.google.zxing.RGBLuminanceSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QRScannerScreen(
    onQRScanned: (url: String, token: String, cfId: String?, cfSecret: String?, pubKey: String?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    var hasDetected by remember { mutableStateOf(false) }

    val handleBarcode = { barcodeValue: String ->
        if (!hasDetected) {
            hasDetected = true
            try {
                val uri = android.net.Uri.parse(barcodeValue)
                val isAntimatterScheme = uri.scheme == "antimatter" && uri.host == "connect"
                val isHttpsScheme = uri.scheme == "https" && uri.host == "antimatter.saifmukhtar.dev" && uri.path == "/connect"
                
                if (isAntimatterScheme || isHttpsScheme) {
                    val url = uri.getQueryParameter("url")
                    val token = uri.getQueryParameter("token")
                    var cfId = uri.getQueryParameter("cfid")
                    var cfSecret = uri.getQueryParameter("cfsec")
                    val cfEnc = uri.getQueryParameter("cfenc")
                    val pubKey = uri.getQueryParameter("x25519_pub")

                    if (cfEnc != null && token != null) {
                        try {
                            val parts = cfEnc.split(":")
                            if (parts.size == 3) {
                                val iv = android.util.Base64.decode(parts[0], android.util.Base64.DEFAULT)
                                val authTag = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)
                                val ciphertext = android.util.Base64.decode(parts[2], android.util.Base64.DEFAULT)
                                
                                val md = java.security.MessageDigest.getInstance("SHA-256")
                                val keyBytes = md.digest(token.toByteArray(Charsets.UTF_8))
                                val keySpec = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
                                val gcmSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
                                
                                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, gcmSpec)
                                
                                val combined = ByteArray(ciphertext.size + authTag.size)
                                System.arraycopy(ciphertext, 0, combined, 0, ciphertext.size)
                                System.arraycopy(authTag, 0, combined, ciphertext.size, authTag.size)
                                
                                val decryptedBytes = cipher.doFinal(combined)
                                val decryptedStr = String(decryptedBytes, Charsets.UTF_8)
                                val json = org.json.JSONObject(decryptedStr)
                                cfId = json.optString("id").takeIf { it.isNotBlank() } ?: cfId
                                cfSecret = json.optString("secret").takeIf { it.isNotBlank() } ?: cfSecret
                            }
                        } catch (e: Exception) {
                            Log.e("QRScanner", "Failed to decrypt cfenc", e)
                        }
                    }
                    if (url != null && token != null) {
                        onQRScanned(url, token, cfId, cfSecret, pubKey)
                    } else {
                        Log.e("QRScanner", "Missing url or token in QR: $barcodeValue")
                        hasDetected = false
                    }
                } else {
                    Log.e("QRScanner", "Invalid QR scheme: $barcodeValue")
                    hasDetected = false
                }
            } catch (e: Exception) {
                Log.e("QRScanner", "Invalid QR format: $barcodeValue", e)
                hasDetected = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }

                val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val intArray = IntArray(softwareBitmap.width * softwareBitmap.height)
                softwareBitmap.getPixels(intArray, 0, softwareBitmap.width, 0, 0, softwareBitmap.width, softwareBitmap.height)
                
                val source = RGBLuminanceSource(softwareBitmap.width, softwareBitmap.height, intArray)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                
                val hints = java.util.EnumMap<com.google.zxing.DecodeHintType, Any>(com.google.zxing.DecodeHintType::class.java)
                hints[com.google.zxing.DecodeHintType.TRY_HARDER] = true
                hints[com.google.zxing.DecodeHintType.POSSIBLE_FORMATS] = listOf(com.google.zxing.BarcodeFormat.QR_CODE)
                
                val result = MultiFormatReader().decode(binaryBitmap, hints)
                
                handleBarcode(result.text)
            } catch (e: Exception) {
                Log.e("QRScanner", "Failed to decode QR from image", e)
                Toast.makeText(context, "No valid QR code found in image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Pairing QR") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (cameraPermissionState.status.isGranted) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(onBarcodeDetected = handleBarcode)
                    
                    FloatingActionButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick QR from Gallery")
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Camera permission is required to scan QR codes.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Or you can scan from an image:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick from Gallery")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(onBarcodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(
                    Executors.newSingleThreadExecutor(),
                    QRCodeAnalyzer { barcodeValue ->
                        onBarcodeDetected(barcodeValue)
                    }
                )

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

class QRCodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader()

    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val width = image.width
        val height = image.height

        val source = PlanarYUVLuminanceSource(
            data, width, height,
            0, 0, width, height, false
        )

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decode(binaryBitmap)
            onBarcodeDetected(result.text)
        } catch (e: Exception) {
            // ZXing throws NotFoundException if no QR is in this frame
        } finally {
            image.close()
        }
    }
}
