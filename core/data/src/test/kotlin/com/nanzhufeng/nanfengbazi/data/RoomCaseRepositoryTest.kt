package com.nanzhufeng.nanfengbazi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nanzhufeng.nanfengbazi.data.db.NanfengBaziDatabase
import com.nanzhufeng.nanfengbazi.data.repository.RoomCaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseSortOrder
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.DuplicateReason
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CalendarConversionResult
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.LunarDateTime
import com.nanzhufeng.nanfengbazi.domain.model.RecordChangeType
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomCaseRepositoryTest {
    private lateinit var database: NanfengBaziDatabase
    private lateinit var repository: RoomCaseRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NanfengBaziDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomCaseRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `命例及来源证据可以完整往返`() = runTest {
        val base = sampleCase()
        val records = base.textRecords.map { record ->
            if (record.id == "record-commentary-1") {
                record.copy(
                    type = CaseTextRecordType.ANALYSIS,
                    analysisCategory = AnalysisCategory.HEALTH,
                )
            } else {
                record
            }
        }
        val source = base.copy(
            textRecords = records,
            textRecordRevisions = records.mapIndexed { index, record ->
                CaseTextRecordRevision(
                    id = "record-revision-$index",
                    recordId = record.id,
                    version = 1,
                    changeType = RecordChangeType.CREATED,
                    snapshot = record,
                    changedAt = FixtureInstant,
                )
            },
            eventRevisions = listOf(
                CaseEventRevision(
                    id = "event-revision-1",
                    eventId = base.events.single().id,
                    version = 1,
                    changeType = RecordChangeType.CREATED,
                    snapshot = base.events.single(),
                    changedAt = FixtureInstant,
                ),
            ),
            isFavorite = true,
            isPinned = true,
            lastViewedAt = FixtureInstant.plusSeconds(30),
        )

        assertEquals(CaseWriteResult.Created("case-1", 1), repository.save(source, null))

        val restored = repository.findById(source.id)
        assertEquals(source.copy(revision = 1), restored)
        val nameResults = repository.search(CaseSearchRequest(query = "测试甲"))
        assertEquals(listOf("case-1"), nameResults.map { it.id })
        assertEquals(
            listOf("case-1"),
            repository.search(CaseSearchRequest(query = "脱敏案例")).map { it.id },
        )
        assertEquals(
            listOf("case-1"),
            repository.search(CaseSearchRequest(query = "癸酉")).map { it.id },
        )
        assertEquals(source.birthInput, nameResults.single().birthInput)
        assertEquals(
            source.calculationSnapshots.single().result.fourPillars,
            nameResults.single().fourPillars,
        )
        assertEquals("虎", nameResults.single().zodiac)
        assertTrue(nameResults.single().isFavorite)
        assertTrue(nameResults.single().isPinned)
        assertEquals(source.groups, nameResults.single().groups)
        assertEquals(source.tags, nameResults.single().tags)
    }

    @Test
    fun `修订号阻止旧编辑覆盖新数据`() = runTest {
        val source = sampleCase()
        repository.save(source, null)

        val updated = source.copy(
            profile = source.profile.copy(health = ExplicitText.cleared()),
            updatedAt = FixtureInstant.plusSeconds(60),
        )
        assertEquals(CaseWriteResult.Updated("case-1", 2), repository.save(updated, 1))
        assertEquals(
            CaseWriteResult.RevisionConflict("case-1", 1, 2),
            repository.save(source, 1),
        )
        assertEquals(ExplicitText.cleared(), repository.findById("case-1")?.profile?.health)
    }

    @Test
    fun `没有修订号的重复稳定ID不会覆盖已有命例`() = runTest {
        val source = sampleCase()
        repository.save(source, null)

        val result = repository.save(source.copy(alias = "不应覆盖"), null)

        assertTrue(result is CaseWriteResult.AlreadyExists)
        assertEquals("脱敏案例一", repository.findById("case-1")?.alias)
        assertNull(repository.findById("missing"))
    }

    @Test
    fun `分组标签筛选和排序由仓库统一执行`() = runTest {
        val first = sampleCase().copy(
            groups = listOf(CaseGroup("group-shared", "共同分组")),
            tags = listOf(CaseTag("tag-first", "甲标签")),
            updatedAt = FixtureInstant.plusSeconds(10),
        )
        val second = sampleCase().copy(
            id = "case-2",
            alias = "第二脱敏案例",
            name = ExplicitText.present("测试乙"),
            textRecords = emptyList(),
            events = emptyList(),
            attachments = emptyList(),
            fieldEvidence = emptyList(),
            calculationSnapshots = sampleCase().calculationSnapshots.map {
                it.copy(id = "snapshot-2")
            },
            groups = listOf(CaseGroup("group-shared", "共同分组")),
            tags = listOf(CaseTag("tag-second", "乙标签")),
            isPinned = true,
            createdAt = FixtureInstant.plusSeconds(5),
            updatedAt = FixtureInstant.plusSeconds(5),
        )
        repository.save(first, null)
        repository.save(second, null)

        assertEquals(
            listOf("case-2", "case-1"),
            repository.search(
                CaseSearchRequest(
                    groupId = "group-shared",
                    sortOrder = CaseSortOrder.UPDATED_DESC,
                ),
            ).map { it.id },
        )
        assertEquals(
            listOf("case-2"),
            repository.search(CaseSearchRequest(tagId = "tag-second")).map { it.id },
        )
    }

    @Test
    fun `最近查看只更新查看时间且不制造修订冲突`() = runTest {
        repository.save(sampleCase(), null)
        val viewedAt = Instant.parse("2026-07-30T10:00:00Z")

        assertTrue(repository.markViewed("case-1", viewedAt))
        assertEquals(viewedAt, repository.findById("case-1")?.lastViewedAt)
        assertEquals(1L, repository.findById("case-1")?.revision)
        assertTrue(!repository.markViewed("missing", viewedAt))
    }

    @Test
    fun `软删除命例只在回收站查询且不能更新最近查看`() = runTest {
        val deletedAt = FixtureInstant.plusSeconds(90)
        repository.save(sampleCase().copy(deletedAt = deletedAt), null)

        assertTrue(repository.search().isEmpty())
        val trashed = repository.search(
            CaseSearchRequest(visibility = CaseVisibility.TRASHED),
        )
        assertEquals(listOf("case-1"), trashed.map { it.id })
        assertEquals(deletedAt, trashed.single().deletedAt)
        assertTrue(!repository.markViewed("case-1", FixtureInstant.plusSeconds(120)))
        assertNull(repository.findById("case-1")?.lastViewedAt)
    }

    @Test
    fun `重复候选同时报告出生身份和四柱且包含回收站位置`() = runTest {
        val source = sampleCase().copy(deletedAt = FixtureInstant.plusSeconds(90))
        repository.save(source, null)

        val candidates = repository.findDuplicateCandidates(
            birthInput = source.birthInput,
            fourPillars = source.calculationSnapshots.single().result.fourPillars,
        )

        assertEquals(1, candidates.size)
        assertEquals(
            setOf(
                DuplicateReason.SAME_BIRTH_INPUT,
                DuplicateReason.SAME_FOUR_PILLARS,
            ),
            candidates.single().reasons,
        )
        assertEquals(source.deletedAt, candidates.single().summary.deletedAt)
    }

    @Test
    fun `农历命例按换算公历排序且跨历法识别同一出生时刻`() = runTest {
        val base = sampleCase()
        val solarDateTime = CivilDateTime(2024, 1, 1, 10, 30, 0)
        val solarInput = base.birthInput.copy(
            calendarInput = BirthCalendarInput.Solar(solarDateTime),
        )
        val solarCase = base.copy(
            id = "case-solar",
            birthInput = solarInput,
            textRecords = emptyList(),
            textRecordRevisions = emptyList(),
            events = emptyList(),
            eventRevisions = emptyList(),
            attachments = emptyList(),
            fieldEvidence = emptyList(),
            calculationSnapshots = base.calculationSnapshots.map {
                it.copy(
                    id = "snapshot-solar",
                    result = it.result.copy(
                        normalizedInput = solarInput,
                        calendarConversion = CalendarConversionResult(
                            inputCalendarSystem = CalendarSystem.SOLAR,
                            solarDateTime = solarDateTime,
                            lunarDateTime = LunarDateTime(
                                2023,
                                11,
                                20,
                                10,
                                30,
                                0,
                                isLeapMonth = false,
                            ),
                        ),
                    ),
                )
            },
            groups = emptyList(),
            tags = emptyList(),
        )
        val lunarInput = base.birthInput.copy(
            calendarInput = BirthCalendarInput.Lunar(
                LunarDateTime(2023, 1, 1, 13, 0, 0, isLeapMonth = false),
            ),
        )
        val lunarCanonical = CivilDateTime(2023, 1, 22, 13, 0, 0)
        val lunarCase = solarCase.copy(
            id = "case-lunar",
            alias = "农历合成案例",
            birthInput = lunarInput,
            calculationSnapshots = solarCase.calculationSnapshots.map {
                it.copy(
                    id = "snapshot-lunar",
                    result = it.result.copy(
                        normalizedInput = lunarInput,
                        calendarConversion = CalendarConversionResult(
                            inputCalendarSystem = CalendarSystem.LUNAR,
                            solarDateTime = lunarCanonical,
                            lunarDateTime = (lunarInput.calendarInput as BirthCalendarInput.Lunar)
                                .dateTime,
                        ),
                    ),
                )
            },
        )
        repository.save(solarCase, null)
        repository.save(lunarCase, null)

        assertEquals(
            listOf("case-lunar", "case-solar"),
            repository.search(
                CaseSearchRequest(sortOrder = CaseSortOrder.BIRTH_ASC),
            ).map { it.id },
        )

        val equivalentLunarInput = lunarInput.copy(
            calendarInput = BirthCalendarInput.Lunar(
                LunarDateTime(2023, 11, 20, 10, 30, 0, isLeapMonth = false),
            ),
        )
        val candidates = repository.findDuplicateCandidates(
            birthInput = equivalentLunarInput,
            fourPillars = null,
            canonicalSolarDateTime = solarDateTime,
        )
        assertEquals(
            setOf(DuplicateReason.SAME_BIRTH_INPUT),
            candidates.single { it.summary.id == "case-solar" }.reasons,
        )
    }
}
