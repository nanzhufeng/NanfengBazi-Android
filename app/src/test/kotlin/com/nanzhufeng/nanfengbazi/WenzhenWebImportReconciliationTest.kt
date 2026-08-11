package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventTimelineLevel
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * 对私有问真迁移包执行逐案例、逐笔记、逐事件的一比一对账。
 * 默认跳过；真机导入前通过 WENZHEN_IMPORT_FILE 显式运行。
 */
class WenzhenWebImportReconciliationTest {
    @Test
    fun `真实问真包逐案例全字段对账为零差异`() = runTest {
        val sourcePath = System.getenv("WENZHEN_IMPORT_FILE").orEmpty()
        assumeTrue("未提供私有问真迁移包", sourcePath.isNotBlank())
        val raw = String(Files.readAllBytes(Path.of(sourcePath)), StandardCharsets.UTF_8)
        val repository = FakeCaseRepository()
        val localPillars = calculationResult(
            WenzhenDateTime(2000, 1, 1, 0, 0).toBirthInputForTest(),
        ).fourPillars
        val importer = WenzhenWebImporter(
            repository,
            BaziEngine { input, _ -> calculationResult(input) },
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
        val source = importer.decode(raw)
        val result = importer.import(source)

        assertEquals(result.errors.toString(), 0, result.invalid)
        assertEquals(0, result.skipped)
        assertEquals(source.userCases.size + source.celebrityCases.size, result.created)
        assertEquals(result.created, repository.stored.size)

        source.userCases.forEach { expected ->
            val actual = repository.stored.getValue(stableTestId("user-case", expected.sourceId))
            assertEquals(CaseLibraryType.USER, actual.libraryType)
            assertEquals(CaseSourceType.WENZHEN_WEB_IMPORT, actual.sourceType)
            assertEquals(expected.name.trim().ifBlank { "未命名案例" }, actual.alias)
            assertEquals(expected.name.trim().toExplicitForTest(), actual.name)
            assertEquals(expected.sex.toSexForTest(), actual.sexForFortuneDirection)
            assertEquals(
                BirthCalendarInput.Solar(expected.adoptedSourceTime.toDomain()),
                actual.birthInput.calendarInput,
            )
            assertEquals(expected.location.trim().ifBlank { null }, actual.birthInput.locationName)
            assertFalse(actual.birthInput.useTrueSolarTime)
            assertEquals(TimeSourceType.WENZHEN_WEB_IMPORT, actual.birthInput.timeSourceType)
            assertTrue(actual.birthInput.sourceNote.orEmpty().contains(expected.originalSolarTime.display()))
            assertTrue(actual.birthInput.sourceNote.orEmpty().contains(expected.adoptedSourceTime.display()))
            assertTrue(actual.birthInput.sourceNote.orEmpty().contains(expected.fourPillars.compactForTest()))
            assertEquals(expected.profile.occupation.trim().toExplicitForTest(), actual.profile.occupation)
            assertEquals(expected.profile.education.trim().toExplicitForTest(), actual.profile.education)
            assertEquals(expected.profile.finance.trim().toExplicitForTest(), actual.profile.finance)
            assertEquals(expected.profile.marriage.trim().toExplicitForTest(), actual.profile.marriage)
            assertEquals(expected.profile.health.trim().toExplicitForTest(), actual.profile.health)
            assertEquals(listOfNotNull(expected.groupName?.trim()?.takeIf(String::isNotEmpty)), actual.groups.map { it.name })
            assertTrue(actual.tags.isEmpty())
            assertTextRecords(expected.ownerFeedback, expected.masterCommentary, actual.textRecords)
            assertEvents("user-event:${expected.sourceId}", expected.timeline, actual.events)
            val sourcePillars = expected.fourPillars.toDomain()
            assertEquals(
                if (sourcePillars.isStructurallyValidForTest()) sourcePillars else localPillars,
                actual.calculationSnapshots.single().result.fourPillars,
            )
            assertTrue(actual.calculationSnapshots.single().adopted)
        }

        source.celebrityCases.forEach { expected ->
            val actual = repository.stored.getValue(stableTestId("celebrity-case", expected.sourceId))
            assertEquals(CaseLibraryType.CELEBRITY, actual.libraryType)
            assertEquals(CaseSourceType.WENZHEN_WEB_IMPORT, actual.sourceType)
            assertEquals(expected.name.trim().ifBlank { "未命名名人" }, actual.alias)
            assertEquals(expected.name.trim().toExplicitForTest(), actual.name)
            assertEquals(expected.sex.toSexForTest(), actual.sexForFortuneDirection)
            assertEquals(BirthCalendarInput.Solar(expected.solarTime.toDomain()), actual.birthInput.calendarInput)
            assertFalse(actual.birthInput.useTrueSolarTime)
            assertEquals(TimeSourceType.WENZHEN_WEB_IMPORT, actual.birthInput.timeSourceType)
            assertTrue(actual.birthInput.sourceNote.orEmpty().contains(expected.solarTime.display()))
            assertTrue(actual.birthInput.sourceNote.orEmpty().contains(expected.fourPillars.compactForTest()))
            assertEquals(listOf(expected.groupName.trim()), actual.groups.map { it.name })
            assertEquals(
                listOf(expected.periodTag, expected.identityTag).map(String::trim).filter(String::isNotEmpty).distinct(),
                actual.tags.map { it.name },
            )
            assertTextRecords(expected.ownerFeedback, expected.masterCommentary, actual.textRecords)
            assertEvents("celebrity-event:${expected.sourceId}", expected.timeline, actual.events)
            val sourcePillars = expected.fourPillars.toDomain()
            assertEquals(
                if (sourcePillars.isStructurallyValidForTest()) sourcePillars else localPillars,
                actual.calculationSnapshots.single().result.fourPillars,
            )
            assertTrue(actual.calculationSnapshots.single().adopted)
        }

        val stored = repository.stored.values
        println(
            "WENZHEN_RECONCILIATION_OK cases=${stored.size} " +
                "user=${stored.count { it.libraryType == CaseLibraryType.USER }} " +
                "celebrity=${stored.count { it.libraryType == CaseLibraryType.CELEBRITY }} " +
                "records=${stored.sumOf { it.textRecords.size }} " +
                "events=${stored.sumOf { it.events.size }} " +
                "tags=${stored.sumOf { it.tags.size }}",
        )
    }

    private fun assertTextRecords(
        ownerFeedback: String,
        masterCommentary: String,
        actual: List<com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord>,
    ) {
        val expected = buildMap {
            ownerFeedback.trim().takeIf(String::isNotEmpty)?.let { put(CaseTextRecordType.OWNER_FEEDBACK, it) }
            masterCommentary.trim().takeIf(String::isNotEmpty)?.let { put(CaseTextRecordType.MASTER_COMMENTARY, it) }
        }
        assertEquals(expected, actual.associate { it.type to it.content })
        assertTrue(actual.all { it.sourceType == TextRecordSourceType.WENZHEN_WEB_IMPORT })
    }

    private fun assertEvents(
        namespace: String,
        expected: List<WenzhenTimelineEvent>,
        actual: List<com.nanzhufeng.nanfengbazi.domain.model.CaseEvent>,
    ) {
        val sorted = expected.sortedBy { it.order }
        assertEquals(sorted.size, actual.size)
        sorted.zip(actual).forEach { (source, event) ->
            assertEquals(stableTestId(namespace, source.sourceId), event.id)
            assertEquals(source.sourceLabel.trim().ifBlank { null }, event.title)
            assertEquals(source.year, event.year)
            assertEquals(source.stemBranch.trim(), event.stemBranch)
            assertEquals(source.status.trim().ifBlank { null }, event.status)
            assertEquals(source.content.trim(), event.rawText)
            assertEquals(
                if (source.level == "DECADE") CaseEventTimelineLevel.DECADE else CaseEventTimelineLevel.ANNUAL,
                event.timelineLevel,
            )
        }
    }
}

private fun WenzhenDateTime.toBirthInputForTest() = com.nanzhufeng.nanfengbazi.domain.model.BirthInput(
    calendarInput = BirthCalendarInput.Solar(toDomain()),
    sexForFortuneDirection = SexForFortuneDirection.MAN,
    timePrecision = TimePrecision.EXACT_TO_MINUTE,
)

private fun String.toSexForTest() = when (trim()) {
    "男", "MAN", "1" -> SexForFortuneDirection.MAN
    "女", "WOMAN", "0" -> SexForFortuneDirection.WOMAN
    else -> error("未知性别：$this")
}

private fun String.toExplicitForTest(): ExplicitText =
    takeIf(String::isNotEmpty)?.let(ExplicitText::present) ?: ExplicitText.absent()

private fun WenzhenFourPillars.compactForTest() = "$year$month$day$hour"

private fun FourPillars.isStructurallyValidForTest(): Boolean {
    val stems = "甲乙丙丁戊己庚辛壬癸"
    val branches = "子丑寅卯辰巳午未申酉戌亥"
    return listOf(year, month, day, hour).all { pillar ->
        pillar.length == 2 && pillar[0] in stems && pillar[1] in branches
    }
}

private fun stableTestId(namespace: String, value: String): String = UUID.nameUUIDFromBytes(
    "$namespace:$value".toByteArray(StandardCharsets.UTF_8),
).toString()
