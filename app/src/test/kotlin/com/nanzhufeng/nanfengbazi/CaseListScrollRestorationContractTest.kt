package com.nanzhufeng.nanfengbazi

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseListScrollRestorationContractTest {
    @Test
    fun caseListScrollStateIsOwnedAboveDetailNavigationAndPassedBackToTheList() {
        val source = File(locateSourceRoot(), "StageTwoScreens.kt").readText()
        val app = source.substringAfter("fun NanfengBaziApp(")
            .substringBefore("private fun CaseListScreen(")
        val list = source.substringAfter("private fun CaseListScreen(")
            .substringBefore("private fun CaseSummaryCard(")

        assertTrue(app.contains("val caseListState = rememberLazyListState()"))
        assertTrue(app.contains("AppDestination.CaseList -> CaseListScreen("))
        assertTrue(app.contains("caseListState = caseListState,"))
        assertTrue(list.contains("caseListState: LazyListState,"))
        assertTrue(list.contains("LazyColumn(\n                    state = caseListState,"))
        assertFalse(list.contains("val caseListState = rememberLazyListState()"))
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
