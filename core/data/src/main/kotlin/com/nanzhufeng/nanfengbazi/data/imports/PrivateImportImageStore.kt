package com.nanzhufeng.nanfengbazi.data.imports

import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrivateImportImageStore(
    private val root: Path,
    private val maxImageBytes: Long = DEFAULT_MAX_IMAGE_BYTES,
) {
    init {
        require(maxImageBytes > 0) { "图片大小上限必须为正数" }
    }

    suspend fun copyImage(
        sessionId: String,
        imageId: String,
        originalFileName: String,
        mimeType: String,
        createdAt: Instant,
        openInput: () -> InputStream?,
    ): ImportImageRef = withContext(Dispatchers.IO) {
        requireSafeId(sessionId, "导入会话")
        requireSafeId(imageId, "导入图片")
        require(originalFileName.isNotBlank()) { "图片原文件名不能为空" }
        require(mimeType.startsWith("image/")) { "仅支持图片文件" }

        val relativePath = "$sessionId/$imageId.${mimeType.safeExtension()}"
        val target = resolveSafe(relativePath)
        val temp = target.resolveSibling("${target.fileName}.part-${UUID.randomUUID()}")
        Files.createDirectories(target.parent)
        check(!Files.exists(target)) { "导入图片 id 已存在" }

        val digest = MessageDigest.getInstance("SHA-256")
        var byteSize = 0L
        try {
            val input = openInput() ?: error("无法读取所选图片")
            input.use { source ->
                Files.newOutputStream(temp).use { sink ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        byteSize += count
                        require(byteSize <= maxImageBytes) { "图片超过允许的大小上限" }
                        digest.update(buffer, 0, count)
                        sink.write(buffer, 0, count)
                    }
                }
            }
            require(byteSize > 0) { "图片文件为空" }
            moveAtomically(temp, target)
        } catch (error: Throwable) {
            Files.deleteIfExists(temp)
            throw error
        }

        ImportImageRef(
            id = imageId,
            originalFileName = originalFileName,
            mimeType = mimeType,
            relativePath = relativePath,
            sha256 = digest.digest().joinToString("") { "%02x".format(it) },
            byteSize = byteSize,
            createdAt = createdAt,
        )
    }

    suspend fun readBytes(image: ImportImageRef): ByteArray = withContext(Dispatchers.IO) {
        val path = resolveSafe(image.relativePath)
        require(Files.size(path) == image.byteSize) { "导入图片大小校验失败" }
        val bytes = Files.readAllBytes(path)
        val actualHash = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
        require(actualHash == image.sha256) { "导入图片完整性校验失败" }
        bytes
    }

    suspend fun deleteImage(image: ImportImageRef): Boolean = withContext(Dispatchers.IO) {
        Files.deleteIfExists(resolveSafe(image.relativePath))
    }

    private fun resolveSafe(relativePath: String): Path {
        require(!relativePath.startsWith("/") && '\\' !in relativePath && ':' !in relativePath) {
            "导入图片路径格式无效"
        }
        require(relativePath.split('/').none { it.isBlank() || it == "." || it == ".." }) {
            "导入图片路径不能越界"
        }
        val normalizedRoot = root.toAbsolutePath().normalize()
        val resolved = normalizedRoot.resolve(relativePath).normalize()
        require(resolved.startsWith(normalizedRoot)) { "导入图片路径越界" }
        return resolved
    }

    private fun moveAtomically(
        source: Path,
        target: Path,
    ) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target)
        }
    }

    private fun requireSafeId(
        value: String,
        label: String,
    ) {
        require(value.matches(SAFE_ID)) { "$label id 格式无效" }
    }

    private fun String.safeExtension(): String = when (lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/heic" -> "heic"
        "image/heif" -> "heif"
        else -> "img"
    }

    private companion object {
        val SAFE_ID = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
        const val DEFAULT_MAX_IMAGE_BYTES = 100L * 1024L * 1024L
    }
}
