package com.atelier.virtualstylist.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class EncodedImage(
    val dataUrl: String,
    val base64: String,
    val mimeType: String = "image/jpeg"
)

object ImageCodec {
    private val httpClient = OkHttpClient()

    fun splitDataUrl(dataUrl: String): Pair<String, String> {
        val commaIndex = dataUrl.indexOf(',')
        if (commaIndex == -1) return "image/jpeg" to dataUrl
        val header = dataUrl.substring(0, commaIndex)
        val mime = header.removePrefix("data:").substringBefore(";").ifBlank { "image/jpeg" }
        val base64 = dataUrl.substring(commaIndex + 1)
        return mime to base64
    }

    fun decodeDataUrlToBitmap(dataUrl: String): Bitmap? {
        return try {
            val (_, base64) = splitDataUrl(dataUrl)
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun encodeUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024,
        maxDataUrlChars: Int = 420_000
    ): EncodedImage = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Unable to read selected image.")
        compressBytes(bytes, maxDimension, maxDataUrlChars)
    }

    suspend fun fetchAndEncodeUrl(
        url: String,
        maxDimension: Int = 1024,
        maxDataUrlChars: Int = 420_000
    ): EncodedImage = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Unable to load sample garment (${resp.code})")
            }
            val bytes = resp.body?.bytes()
                ?: throw IllegalStateException("Empty sample image response.")
            compressBytes(bytes, maxDimension, maxDataUrlChars)
        }
    }

    fun compressDataUrlForFirestore(
        dataUrl: String,
        maxDimension: Int = 900,
        maxDataUrlChars: Int = 380_000
    ): String {
        if (dataUrl.length <= maxDataUrlChars) return dataUrl
        val (_, base64) = splitDataUrl(dataUrl)
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        return compressBytes(bytes, maxDimension, maxDataUrlChars).dataUrl
    }

    private fun compressBytes(
        bytes: ByteArray,
        maxDimension: Int,
        maxDataUrlChars: Int
    ): EncodedImage {
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Could not decode image data.")

        val largestEdge = max(raw.width, raw.height)
        val scale = if (largestEdge > maxDimension) maxDimension.toFloat() / largestEdge else 1f
        var width = max(1, (raw.width * scale).roundToInt())
        var height = max(1, (raw.height * scale).roundToInt())
        var quality = 86
        var base64 = ""

        for (attempt in 0 until 4) {
            val scaled = Bitmap.createScaledBitmap(raw, width, height, true)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            if (scaled != raw) scaled.recycle()
            base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            if ("data:image/jpeg;base64,$base64".length <= maxDataUrlChars) {
                break
            }
            width = max(320, (width * 0.8f).roundToInt())
            height = max(320, (height * 0.8f).roundToInt())
            quality = max(60, quality - 8)
        }

        raw.recycle()
        return EncodedImage(
            dataUrl = "data:image/jpeg;base64,$base64",
            base64 = base64,
            mimeType = "image/jpeg"
        )
    }
}
