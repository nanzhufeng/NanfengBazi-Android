package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventTimelineLevel
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WenzhenWebImportTest {
    @Test
    fun `导入用户笔记和名人标签并支持稳定续传`() = runTest {
        val repository = FakeCaseRepository()
        val engine = BaziEngine { input, _ -> calculationResult(input) }
        val importer = WenzhenWebImporter(
            repository,
            engine,
            Clock.fixed(FixedInstant, ZoneOffset.UTC),
        )
        val sourcePillars = WenzhenFourPillars("庚辰", "戊寅", "丁巳", "乙巳")
        val data = WenzhenWebImportPackage(
            format = WenzhenWebImportPackage.FORMAT,
            version = WenzhenWebImportPackage.VERSION,
            extractedAt = "2026-08-10T08:00:00Z",
            userGroups = listOf("清娟"),
            celebrityGroups = listOf("君主"),
            userCases = listOf(
                WenzhenUserCase(
                    sourceId = "user-source-1",
                    name = "合成用户案例",
                    sex = "男",
                    groupName = "清娟",
                    originalSolarTime = WenzhenDateTime(2000, 2, 29, 10, 30),
                    adoptedSourceTime = WenzhenDateTime(2000, 2, 29, 10, 30),
                    fourPillars = sourcePillars,
                    masterCommentary = "师傅点评原文",
                    timeline = listOf(
                        WenzhenTimelineEvent(
                            sourceId = "annual-2024",
                            level = "ANNUAL",
                            year = 2024,
                            stemBranch = "甲辰",
                            status = "吉",
                            content = "关键事件原文",
                            order = 0,
                        ),
                    ),
                ),
            ),
            celebrityCases = listOf(
                WenzhenCelebrityCase(
                    sourceId = "celebrity-source-1",
                    name = "合成名人案例",
                    sex = "女",
                    groupName = "君主",
                    solarTime = WenzhenDateTime(2000, 2, 29, 10, 30),
                    fourPillars = sourcePillars,
                    periodTag = "测试朝代",
                    identityTag = "测试身份",
                ),
            ),
        )

        val first = importer.import(data)
        val second = importer.import(data)

        assertEquals(first.errors.toString(), 2, first.created)
        assertEquals(0, first.invalid)
        assertEquals(0, second.created)
        assertEquals(2, second.skipped)
        val userCase = repository.stored.values.single {
            it.libraryType == CaseLibraryType.USER
        }
        assertEquals("清娟", userCase.groups.single().name)
        assertEquals(TimePrecision.EXACT_TO_MINUTE, userCase.birthInput.timePrecision)
        assertEquals(
            "师傅点评原文",
            userCase.textRecords.single {
                it.type == CaseTextRecordType.MASTER_COMMENTARY
            }.content,
        )
        assertEquals(CaseEventTimelineLevel.ANNUAL, userCase.events.single().timelineLevel)
        assertEquals("吉", userCase.events.single().status)
        val celebrity = repository.stored.values.single {
            it.libraryType == CaseLibraryType.CELEBRITY
        }
        assertEquals("君主", celebrity.groups.single().name)
        assertEquals(TimePrecision.DOUBLE_HOUR_ONLY, celebrity.birthInput.timePrecision)
        assertTrue(celebrity.birthInput.sourceNote.orEmpty().contains("尚无公开出生时刻证据"))
        assertEquals(setOf("测试朝代", "测试身份"), celebrity.tags.map { it.name }.toSet())
        assertTrue(
            repository.listGroups(CaseLibraryType.USER).none {
                it.libraryType == CaseLibraryType.CELEBRITY
            },
        )
    }
}
