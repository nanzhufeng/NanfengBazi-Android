package com.nanzhufeng.nanfengbazi.data

import com.nanzhufeng.nanfengbazi.data.imports.PrivateImportImageStore
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PrivateImportImageStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `图片流式复制到私有目录并在读取时校验哈希`() = runTest {
        val root = temporaryFolder.newFolder("imports").toPath()
        val store = PrivateImportImageStore(root)
        val bytes = "synthetic-image-bytes".encodeToByteArray()

        val image = store.copyImage(
            sessionId = "session-1",
            imageId = "image-1",
            originalFileName = "问真合成样本.png",
            mimeType = "image/png",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            openInput = { ByteArrayInputStream(bytes) },
        )

        assertEquals("session-1/image-1.png", image.relativePath)
        assertEquals(bytes.size.toLong(), image.byteSize)
        assertEquals(
            "ea154f2b845db9cf192c94024a7883180edb89a64dbfab16e4109619c43750a2",
            image.sha256,
        )
        assertArrayEquals(bytes, store.readBytes(image))
    }

    @Test
    fun `超过上限时清理临时文件且不产生目标图片`() = runTest {
        val root = temporaryFolder.newFolder("limited").toPath()
        val store = PrivateImportImageStore(root, maxImageBytes = 4)

        val error = runCatching {
            store.copyImage(
                sessionId = "session-1",
                imageId = "image-1",
                originalFileName = "large.png",
                mimeType = "image/png",
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                openInput = { ByteArrayInputStream(ByteArray(5)) },
            )
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)

        Files.walk(root).use { paths ->
            assertFalse(paths.anyMatch(Files::isRegularFile))
        }
    }

    @Test
    fun `会话和图片标识不能用于路径越界`() = runTest {
        val store = PrivateImportImageStore(temporaryFolder.root.toPath())
        val error = runCatching {
            store.copyImage(
                sessionId = "../outside",
                imageId = "image-1",
                originalFileName = "synthetic.png",
                mimeType = "image/png",
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                openInput = { ByteArrayInputStream(byteArrayOf(1)) },
            )
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }
}
