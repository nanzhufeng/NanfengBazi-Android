package com.nanzhufeng.nanfengbazi.imageparser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import kotlin.math.max

data class ImageFingerprint(
    val widthPx: Int,
    val heightPx: Int,
    val perceptualHash: String,
) {
    init {
        require(widthPx > 0 && heightPx > 0) { "图片尺寸必须为正数" }
        require(perceptualHash.matches(Regex("[0-9a-f]{16}"))) {
            "感知哈希必须是 64 位十六进制"
        }
    }
}

fun interface ImageFingerprintEngine {
    fun fingerprint(bytes: ByteArray): ImageFingerprint
}

class DHashImageFingerprintEngine : ImageFingerprintEngine {
    override fun fingerprint(bytes: ByteArray): ImageFingerprint {
        require(bytes.isNotEmpty()) { "感知哈希输入图片不能为空" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法解析图片尺寸" }

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        val decoded = requireNotNull(
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                    inSampleSize = sampleSize
                },
            ),
        ) { "无法解码图片以计算感知哈希" }
        val sample = Bitmap.createScaledBitmap(decoded, HASH_WIDTH + 1, HASH_HEIGHT, true)
        return try {
            var hash = 0UL
            repeat(HASH_HEIGHT) { y ->
                repeat(HASH_WIDTH) { x ->
                    hash = hash shl 1
                    if (luminance(sample.getPixel(x, y)) > luminance(sample.getPixel(x + 1, y))) {
                        hash = hash or 1UL
                    }
                }
            }
            ImageFingerprint(
                widthPx = bounds.outWidth,
                heightPx = bounds.outHeight,
                perceptualHash = hash.toString(16).padStart(16, '0'),
            )
        } finally {
            if (sample !== decoded) sample.recycle()
            decoded.recycle()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (max(width / sampleSize, height / sampleSize) > MAX_DECODED_EDGE) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun luminance(color: Int): Int =
        (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1_000

    private companion object {
        const val HASH_WIDTH = 8
        const val HASH_HEIGHT = 8
        const val MAX_DECODED_EDGE = 512
    }
}

enum class DuplicateImageKind {
    EXACT,
    VISUALLY_SIMILAR,
}

data class DuplicateImageMatch(
    val firstImageId: String,
    val secondImageId: String,
    val kind: DuplicateImageKind,
    val perceptualDistance: Int?,
)

class ImportImageDuplicateDetector(
    private val similarDistanceThreshold: Int = DEFAULT_SIMILAR_DISTANCE_THRESHOLD,
) {
    init {
        require(similarDistanceThreshold in 0..64) { "感知哈希相似阈值必须在 0 到 64 之间" }
    }

    fun findMatches(images: List<ImportImageRef>): List<DuplicateImageMatch> = buildList {
        images.forEachIndexed { firstIndex, first ->
            for (secondIndex in firstIndex + 1 until images.size) {
                val second = images[secondIndex]
                if (first.sha256 == second.sha256) {
                    add(
                        DuplicateImageMatch(
                            firstImageId = first.id,
                            secondImageId = second.id,
                            kind = DuplicateImageKind.EXACT,
                            perceptualDistance = 0,
                        ),
                    )
                    continue
                }
                val firstHash = first.perceptualHash ?: continue
                val secondHash = second.perceptualHash ?: continue
                val distance = hammingDistance(firstHash, secondHash)
                if (distance <= similarDistanceThreshold) {
                    add(
                        DuplicateImageMatch(
                            firstImageId = first.id,
                            secondImageId = second.id,
                            kind = DuplicateImageKind.VISUALLY_SIMILAR,
                            perceptualDistance = distance,
                        ),
                    )
                }
            }
        }
    }

    private fun hammingDistance(first: String, second: String): Int {
        require(first.matches(HASH_PATTERN) && second.matches(HASH_PATTERN)) {
            "感知哈希格式无效"
        }
        return (first.toULong(16) xor second.toULong(16)).countOneBits()
    }

    private companion object {
        val HASH_PATTERN = Regex("[0-9a-f]{16}")
        const val DEFAULT_SIMILAR_DISTANCE_THRESHOLD = 6
    }
}
