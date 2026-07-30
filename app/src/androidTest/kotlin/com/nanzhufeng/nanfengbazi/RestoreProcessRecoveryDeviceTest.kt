package com.nanzhufeng.nanfengbazi

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class RestoreProcessRecoveryDeviceTest {
    @Test
    fun seedInterruptedRestoreAndWaitForHostKill() {
        assumePhase("seed")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val attachmentRoot = context.filesDir.toPath().resolve("attachments")
        val finalRoot = attachmentRoot.resolve("restored/$TRANSACTION_ID")
        val stageRoot = attachmentRoot.resolve(".restore-staging/$TRANSACTION_ID")
        val journalRoot = attachmentRoot.resolve(".restore-journal")
        finalRoot.toFile().deleteRecursively()
        stageRoot.toFile().deleteRecursively()
        Files.createDirectories(finalRoot)
        Files.createDirectories(stageRoot)
        Files.createDirectories(journalRoot)
        Files.write(finalRoot.resolve("orphan-attachment"), ORPHAN_BYTES)
        Files.write(stageRoot.resolve("staged-orphan"), ORPHAN_BYTES)
        Files.write(
            journalRoot.resolve("$TRANSACTION_ID.json"),
            """
                {
                  "formatVersion": 1,
                  "transactionId": "$TRANSACTION_ID",
                  "state": "FILES_MOVED",
                  "finalDirectory": "restored/$TRANSACTION_ID",
                  "expectedCases": [
                    {
                      "caseId": "process-kill-missing-case",
                      "payloadSha256": "${"0".repeat(64)}"
                    }
                  ]
                }
            """.trimIndent().encodeToByteArray(),
        )
        Files.write(
            context.filesDir.toPath().resolve(READY_MARKER),
            android.os.Process.myPid().toString().encodeToByteArray(),
        )
        assertTrue(Files.exists(finalRoot))
        assertTrue(Files.exists(journalRoot.resolve("$TRANSACTION_ID.json")))
        while (true) {
            Thread.sleep(1_000)
        }
    }

    @Test
    fun relaunchFinalizesInterruptedRestore() {
        assumePhase("verify")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val attachmentRoot = context.filesDir.toPath().resolve("attachments")
        val finalRoot = attachmentRoot.resolve("restored/$TRANSACTION_ID")
        val stageRoot = attachmentRoot.resolve(".restore-staging/$TRANSACTION_ID")
        val journal = attachmentRoot.resolve(".restore-journal/$TRANSACTION_ID.json")
        val marker = context.filesDir.toPath().resolve(READY_MARKER)
        assertTrue("强杀前状态标记必须保留", Files.exists(marker))
        val killedPid = Files.readAllBytes(marker).decodeToString().trim().toInt()
        assertNotEquals("重启验证必须运行在新进程", killedPid, android.os.Process.myPid())

        ActivityScenario.launch(MainActivity::class.java).use {
            val deadline = System.currentTimeMillis() + 15_000
            while (
                System.currentTimeMillis() < deadline &&
                (Files.exists(finalRoot) || Files.exists(stageRoot) || Files.exists(journal))
            ) {
                Thread.sleep(100)
            }
            assertFalse("启动恢复应清理未提交最终目录", Files.exists(finalRoot))
            assertFalse("启动恢复应清理未提交暂存目录", Files.exists(stageRoot))
            assertFalse("启动恢复应删除已处理日志", Files.exists(journal))
        }
        Files.deleteIfExists(marker)
    }

    private fun assumePhase(expected: String) {
        val actual = InstrumentationRegistry.getArguments().getString(PHASE_ARGUMENT)
        assumeTrue("仅由两阶段设备脚本显式运行", actual == expected)
    }

    private companion object {
        const val PHASE_ARGUMENT = "processRecoveryPhase"
        const val READY_MARKER = "restore-process-ready"
        const val TRANSACTION_ID = "44444444-4444-4444-8444-444444444444"
        val ORPHAN_BYTES = "脱敏进程中断附件".encodeToByteArray()
    }
}
