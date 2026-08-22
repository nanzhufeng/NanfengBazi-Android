package com.nanzhufeng.nanfengbazi

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseListEmptyStateContractTest {
    @Test
    fun filteredCelebrityEmptyStateNamesCriteriaAndOffersToClearThem() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val list = source.substringAfter("private fun CaseListScreen(")
            .substringBefore("private fun CaseSummaryCard(")
        val emptyState = source.substringAfter("private fun EmptyCaseList(")
            .substringBefore("private fun CaseSummaryCard(")

        assertTrue(list.contains("val emptyResultCriteria = buildList"))
        assertTrue(list.contains("关键词“${'$'}query”"))
        assertTrue(list.contains("分组“${'$'}groupName”"))
        assertTrue(list.contains("标签“${'$'}tagName”"))
        assertTrue(emptyState.contains("未找到符合当前条件的名人案例"))
        assertTrue(emptyState.contains("当前条件："))
        assertTrue(emptyState.contains("清除搜索与筛选"))
        assertTrue(emptyState.contains("case_empty_clear_conditions"))
    }

    private fun locateSourceRoot(): File = generateSequence(File(".").canonicalFile) { it.parentFile }
        .flatMap { root ->
            sequenceOf(
                File(root, "app/src/main/kotlin/com/nanzhufeng/nanfengbazi"),
                File(root, "src/main/kotlin/com/nanzhufeng/nanfengbazi"),
            )
        }
        .firstOrNull(File::isDirectory)
        ?: error("找不到 app Compose 源码目录。")
}
