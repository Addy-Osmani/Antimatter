package dev.saifmukhtar.antimatter.core.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GzipUtils {
    fun compress(data: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(data.toByteArray(Charsets.UTF_8))
        }
        return bos.toByteArray()
    }

    fun decompress(compressedData: ByteArray): String {
        val bis = ByteArrayInputStream(compressedData)
        val gzip = GZIPInputStream(bis)
        return gzip.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
