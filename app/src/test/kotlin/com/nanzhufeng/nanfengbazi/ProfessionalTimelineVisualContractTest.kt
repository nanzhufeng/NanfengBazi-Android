package com.nanzhufeng.nanfengbazi

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalTimelineVisualContractTest {
    @Test
    fun timelineDividerIsDrawnAboveSelectionAtTheRealCellBoundary() {
        val source = locateStageTwoScreens().readText()
        val row = source.substringAfter("private fun ProfessionalTimelineRow(")
            .substringBefore("private const val PROFESSIONAL_TIMELINE_VISIBLE_COLUMNS")
        val cell = source.substringAfter("private fun ProfessionalTimelineCell(")
            .substringBefore("private fun ProfessionalTimelineBranchDetail(")
        val rows = source.substringAfter("private fun FortuneDetailsView(")
            .substringBefore("private enum class ProfessionalPagePart(")

        assertTrue(row.contains("Modifier.drawWithContent"))
        assertTrue(row.contains(".height(ProfessionalTimelineCardHeight)"))
        assertTrue(row.contains(".fillMaxHeight()"))
        assertTrue(row.contains(".fillParentMaxWidth(1f / PROFESSIONAL_TIMELINE_VISIBLE_COLUMNS)"))
        assertTrue(row.indexOf("drawContent()") < row.indexOf("drawLine("))
        assertTrue(row.contains("size.width - dividerStrokeWidth / 2f"))
        assertFalse(cell.contains("Modifier.drawWithContent"))
        assertFalse(cell.contains("Modifier.drawBehind"))
        assertFalse(cell.contains("showTrailingDivider"))
        assertTrue(
            rows.contains("LocalMinimumInteractiveComponentEnforcement provides false"),
        )
        assertTrue(rows.contains("LocalViewConfiguration provides exactCellViewConfiguration"))
        assertTrue(rows.contains("minimumTouchTargetSize = DpSize(0.dp, 0.dp)"))
        assertFalse(cell.contains("CompositionLocalProvider"))
        assertTrue(
            "天干十神必须排在天干字上方。",
            cell.indexOf("timeline_${'$'}{item.key}_stem_detail") <
                cell.indexOf("timeline_${'$'}{item.key}_stem\""),
        )
    }

    @Test
    fun timelineSelectionRespondsImmediatelyWithoutSpeculativeFortuneWork() {
        val screenSource = locateStageTwoScreens().readText()
        val viewModelSource = locateStageTwoViewModel().readText()
        val rows = screenSource.substringAfter("private fun FortuneDetailsView(")
            .substringBefore("private enum class ProfessionalPagePart(")
        val row = screenSource.substringAfter("private fun ProfessionalTimelineRow(")
            .substringBefore("private const val PROFESSIONAL_TIMELINE_VISIBLE_COLUMNS")
        val resolver = viewModelSource.substringAfter("private fun resolveFortunePosition(")
            .substringBefore("fun openEditCase()")

        assertTrue(row.contains("pendingSelection: ProfessionalFortuneSelection?"))
        assertTrue(row.contains("it.layer == layer"))
        assertTrue(row.contains("selected = selected"))
        assertTrue(rows.contains("mutableStateOf<Pair<ProfessionalFortunePosition, ProfessionalFortuneSelection>?>"))
        assertTrue(rows.contains("pending = it to selection"))
        assertTrue(rows.contains("it.first === professionalFortunePosition"))
        assertTrue(resolver.contains("requestId != fortunePositionRequestId"))
        assertFalse(resolver.contains("delay("))
        assertTrue(viewModelSource.contains("ioDispatcher.limitedParallelism(1)"))
        assertTrue(resolver.contains("withContext(fortuneCalculationDispatcher)"))
        assertFalse(viewModelSource.contains("prefetchProfessionalFortuneSelections"))
        assertTrue(resolver.contains("selectionLayer = selection?.layer"))
    }

    private fun locateStageTwoScreens(): File = generateSequence(File(".").canonicalFile) {
        it.parentFile
    }.map { root ->
        File(root, "app/src/main/kotlin/com/nanzhufeng/nanfengbazi/StageTwoScreens.kt")
    }.firstOrNull(File::isFile)
        ?: error("找不到 StageTwoScreens.kt。")

    private fun locateStageTwoViewModel(): File = generateSequence(File(".").canonicalFile) {
        it.parentFile
    }.map { root ->
        File(root, "app/src/main/kotlin/com/nanzhufeng/nanfengbazi/StageTwoViewModel.kt")
    }.firstOrNull(File::isFile)
        ?: error("找不到 StageTwoViewModel.kt。")
}
