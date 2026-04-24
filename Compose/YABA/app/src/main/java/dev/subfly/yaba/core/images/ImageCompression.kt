package dev.subfly.yaba.core.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.ByteArrayOutputStream

/**
 * Re-encodes bitmap-backed images for storage size using user [compressionPercent] (0..50).
 * Effective quality shown in UI: `100 - compressionPercent` in range 50..100.
 */
object ImageCompression {
    const val DEFAULT_COMPRESSION_PERCENT = 25
    private const val MAX_DECODE_SIDE = 4096

    data class Result(val bytes: ByteArray, val extension: String)

    /** Maps stored 0..50 to an encoder quality 100..50 (higher = better quality, less compression). */
    fun effectiveQualityPercent(stored: Int): Int {
        val s = stored.coerceIn(0, 50)
        return (100 - s).coerceIn(50, 100)
    }

    /**
     * Returns re-encoded bytes, or null if the input could not be decoded to a bitmap.
     * [sourceExtension] is a hint; output format may differ (e.g. JPEG for opaque photos, WebP/PNG for alpha).
     */
    fun compressForStorage(
        input: ByteArray,
        sourceExtension: String,
        compressionPercent: Int = DEFAULT_COMPRESSION_PERCENT,
    ): Result? {
        if (input.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(input, 0, input.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = sampleSizeForMaxSide(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxSide = MAX_DECODE_SIDE,
        )
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(input, 0, input.size, opts) ?: return null
        return try {
            val quality = effectiveQualityPercent(compressionPercent)
            val out = ByteArrayOutputStream()
            val hasAlpha = bitmap.hasAlpha()
            if (hasAlpha) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val ok = bitmap.compress(
                        Bitmap.CompressFormat.WEBP_LOSSY,
                        quality,
                        out,
                    )
                    if (!ok) return null
                    Result(out.toByteArray(), "webp")
                } else {
                    // PNG is lossless; no quality knob—still smaller than some sources when re-encoded.
                    val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    if (!ok) return null
                    Result(out.toByteArray(), "png")
                }
            } else {
                val ok = bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    out,
                )
                if (!ok) return null
                Result(out.toByteArray(), "jpeg")
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun sampleSizeForMaxSide(width: Int, height: Int, maxSide: Int): Int {
        val w = width
        val h = height
        var inSampleSize = 1
        while (w / inSampleSize > maxSide || h / inSampleSize > maxSide) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
