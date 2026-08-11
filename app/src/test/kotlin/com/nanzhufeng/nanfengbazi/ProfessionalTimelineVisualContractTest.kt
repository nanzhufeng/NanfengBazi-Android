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

        assertTrue(row.contains("Modifier.drawWithContent"))
        assertTrue(row.contains(".height(ProfessionalTimelineCardHeight)"))
        assertTrue(row.contains(".fillMaxHeight()"))
        assertTrue(row.contains(".width(timelineColumnWidth)"))
        assertTrue(row.indexOf("drawContent()") < row.indexOf("drawLine("))
        assertTrue(row.contains("size.width - dividerStrokeWidth / 2f"))
        assertFalse(cell.contains("Modifier.drawWithContent"))
        assertFalse(cell.contains("Modifier.drawBehind"))
        assertFalse(cell.contains("showTrailingDivider"))
        assertTrue(
            cell.contains("LocalMinimumInteractiveComponentEnforcement provides false"),
        )
        assertTrue(cell.contains("LocalViewConfiguration provides exactCellViewConfiguration"))
        assertTrue(cell.contains("minimumTouchTargetSize = DpSize(0.dp, 0.dp)"))
        assertTrue(
            "天干十神必须排在天干字上方。",
            cell.indexOf("timeline_${'$'}{item.key}_stem_detail") <
                cell.indexOf("timeline_${'$'}{item.key}_stem\""),
        )
    }

    @Test
    fun timelineSelectionRespondsImmediatelyAndPrefetchCannotStarveLowerLayers() {
        val screenSource = locateStageTwoScreens().readText()
        val viewModelSource = locateStageTwoViewModel().readText()
        val rows = screenSource.substringAfter("private fun ProfessionalTimelineRows(")
            .substringBefore("private fun ProfessionalTimelineRow(")
        val row = screenSource.substringAfter("private fun ProfessionalTimelineRow(")
            .substringBefore("private const val PROFESSIONAL_TIMELINE_VISIBLE_COLUMNS")
        val resolver = viewModelSource.substringAfter("private fun resolveFortunePosition(")
            .substringBefore("fun openEditCase()")

        assertTrue(row.contains("pendingSelection: ProfessionalFortuneSelection?"))
        assertTrue(row.contains("it.layer == layer"))
        assertTrue(row.contains("selected = selected"))
        assertTrue(rows.contains("var pendingSelection by remember(position)"))
        assertTrue(rows.contains("pendingSelection = selection"))
        assertTrue(resolver.contains("requestId != fortunePositionRequestId"))
        assertTrue(resolver.contains("delay(PROFESSIONAL_SELECTION_SETTLE_MILLIS)"))
        assertTrue(viewModelSource.contains("ioDispatcher.limitedParallelism(1)"))
        assertTrue(resolver.contains("withContext(fortuneCalculationDispatcher)"))
        assertTrue(resolver.contains("ProfessionalFortuneLayer.HOURLY to current.hourlyTimeline"))
        assertTrue(resolver.contains("distinctBy { it.layer to it.observedAt }"))
        assertTrue(resolver.contains("selectionLayer = selection.layer"))
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
