package com.nanzhufeng.nanfengbazi

import org.junit.Assert.assertEquals
import org.junit.Test

class CaseNotesWebLinkTest {
    @Test
    fun extractsDistinctWebUrlsAndStripsChineseSentencePunctuation() {
        assertEquals(
            listOf(
                "https://example.com/profile",
                "http://example.org/a?ref=notes",
            ),
            extractCaseNotesWebUrls(
                "资料：https://example.com/profile。备用 http://example.org/a?ref=notes，" +
                    "再次引用 https://example.com/profile",
            ),
        )
    }

    @Test
    fun findsOnlyTheTappedUrlRangeInsideTheOriginalInput() {
        val value = "来源 https://example.com/profile。备用 https://example.org/a"

        assertEquals("https://example.com/profile", caseNotesWebUrlAtOffset(value, 8))
        assertEquals("https://example.org/a", caseNotesWebUrlAtOffset(value, value.lastIndex))
        assertEquals(null, caseNotesWebUrlAtOffset(value, 1))
        assertEquals(null, caseNotesWebUrlAtOffset(value, value.indexOf('。')))
    }
}
