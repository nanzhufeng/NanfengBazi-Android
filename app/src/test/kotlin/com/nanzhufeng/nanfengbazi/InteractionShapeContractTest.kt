package com.nanzhufeng.nanfengbazi

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionShapeContractTest {
    @Test
    fun manualClickablesAreEitherShapeClippedOrExplicitRectangularRegions() {
        val sourceRoot = locateSourceRoot()
        val expectedDirectClickables = mapOf(
            "AlmanacFeature.kt" to setOf("AlmanacCalendarCard"),
            "BirthInputPickers.kt" to setOf("FixedPickerSheet"),
            "FourPillarsInputPresentation.kt" to setOf("BaziCharacterSlot"),
            "StageTwoScreens.kt" to setOf(
                "SettingsActionRow",
                "CaseSummaryRow",
                "HomePickerRow",
                "RecordAdvancedFilterDialog",
                "RecordSortDialog",
                "RecordGroupEditorDialog",
                "RecordCaseSelectionDialog",
                "ReferenceOtherNotes",
                "ReferenceEventTimelineItem",
            ),
        )
        val clippedFunctions = setOf("AlmanacCalendarCard", "BaziCharacterSlot")

        val actual = sourceRoot.listFiles()
            .orEmpty()
            .filter { it.extension == "kt" }
            .associate { source ->
                var currentFunction = ""
                val clickables = mutableSetOf<String>()
                val lines = source.readLines()
                lines.forEachIndexed { index, line ->
                    functionName(line)?.let { currentFunction = it }
                    if (".clickable" in line && !line.trimStart().startsWith("import ")) {
                        clickables += currentFunction
                        if (currentFunction in clippedFunctions) {
                            val modifierWindow = lines.subList((index - 4).coerceAtLeast(0), index)
                            assertTrue(
                                "$currentFunction 的手写点击反馈必须先按自身形状裁切。",
                                modifierWindow.any { ".clip(" in it },
                            )
                        }
                    }
                }
                source.name to clickables
            }
            .filterValues { it.isNotEmpty() }

        assertEquals(
            "新增交互面应优先使用可点击 Surface/Card；确属矩形区域时才加入显式清单。",
            expectedDirectClickables,
            actual,
        )
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

    private fun functionName(line: String): String? =
        Regex("""(?:private|internal|public)?\s*fun\s+(?:<[^>]+>\s*)?(\w+)""")
            .find(line)
            ?.groupValues
            ?.get(1)
}
