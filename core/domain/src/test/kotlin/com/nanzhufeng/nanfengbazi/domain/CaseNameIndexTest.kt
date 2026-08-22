package com.nanzhufeng.nanfengbazi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseNameIndexTest {
    @Test
    fun `Chinese names use pinyin initials`() {
        assertEquals('C', "崔召伟".caseNameInitial())
        assertEquals('N', "倪海清".caseNameInitial())
        assertEquals('X', "席楼".caseNameInitial())
        assertEquals('Z', "张大千02".caseNameInitial())
    }

    @Test
    fun `Chinese names sort in pinyin order`() {
        val names = listOf("倪海清", "崔召伟", "张大千02", "董鑫", "高腾林")

        val sorted = names.sortedWith(Comparator(::compareCaseNames))

        assertEquals(listOf("崔召伟", "董鑫", "高腾林", "倪海清", "张大千02"), sorted)
        assertTrue(compareCaseNames("Alpha", "Beta") < 0)
    }

    @Test
    fun `Chinese and Latin names with the same initial share one contiguous group`() {
        val names = listOf("张大千", "Zack", "阿兰", "Alpha", "李四", "Luna")

        val sorted = names.sortedWith(Comparator(::compareCaseNameGroups))

        assertEquals(listOf('A', 'A', 'L', 'L', 'Z', 'Z'), sorted.map(String::caseNameInitial))
        assertEquals(1, sorted.count { it.caseNameInitial() == 'A' && it.startsWith("阿") })
        assertEquals(1, sorted.count { it.caseNameInitial() == 'A' && it.startsWith("A") })
    }
}
