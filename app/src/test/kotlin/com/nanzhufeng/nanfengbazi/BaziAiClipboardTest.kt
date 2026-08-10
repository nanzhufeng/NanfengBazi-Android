package com.nanzhufeng.nanfengbazi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaziAiClipboardTest {
    @Test
    fun `系统接受写入即成功不依赖即时读回`() {
        var writtenLabel = ""
        var writtenText = ""

        val result = tryWritePromptToClipboard(
            writer = PromptClipboardWriter { label, text ->
                writtenLabel = label
                writtenText = text
            },
            text = "合成 AI 提示词",
        )

        assertTrue(result)
        assertEquals("南枫八字 AI 指令", writtenLabel)
        assertEquals("合成 AI 提示词", writtenText)
    }

    @Test
    fun `剪贴板拒绝写入时返回失败而不抛出`() {
        val result = tryWritePromptToClipboard(
            writer = PromptClipboardWriter { _, _ ->
                throw SecurityException("synthetic clipboard denial")
            },
            text = "合成 AI 提示词",
        )

        assertFalse(result)
    }

    @Test
    fun `空内容不会写入剪贴板`() {
        var calls = 0
        val result = tryWritePromptToClipboard(
            writer = PromptClipboardWriter { _, _ -> calls += 1 },
            text = " ",
        )

        assertFalse(result)
        assertEquals(0, calls)
    }
}
