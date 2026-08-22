package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CommentaryCandidateRuleEvidence
import com.nanzhufeng.nanfengbazi.domain.CommentaryTextRange
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeAdoptionErrorCode
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeAdoptionResult
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeCandidate
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeSourceEvidence
import com.nanzhufeng.nanfengbazi.domain.FeedbackThemeTextRange
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidate
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateAdoptionErrorCode
import com.nanzhufeng.nanfengbazi.domain.MasterCommentaryCandidateAdoptionResult
import com.nanzhufeng.nanfengbazi.domain.TimeZoneChoiceRequiredException
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventTimelineLevel
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordRevision
import com.nanzhufeng.nanfengbazi.domain.model.EventDatePrecision
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.RecordChangeType
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SourceAttachment
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseManagementTest {
    private val fixedClock = Clock.fixed(FixedInstant.plusSeconds(3600), ZoneOffset.UTC)

    @Test
    fun `同一命例可添加时间候选并显式切换采用值`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-candidate"] = sampleStoredCase("case-candidate")
        }
        val ids = ArrayDeque(
            listOf("candidate-original", "snapshot-candidate", "candidate-new"),
        )
        val useCase = BirthTimeCandidateUseCase(
            baziEngine = RecordingEngine(),
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        assertEquals(
            CaseMutationResult.Saved("case-candidate", 2),
            useCase.add(
                caseId = "case-candidate",
                expectedRevision = 1,
                label = "上午十一点候选",
                form = validForm().copy(alias = "", hour = "11"),
            ),
        )
        val withCandidate = repository.stored.getValue("case-candidate")
        assertEquals(2, withCandidate.birthTimeCandidates.size)
        assertEquals(2, withCandidate.calculationSnapshots.size)
        assertTrue(withCandidate.birthTimeCandidates.first().adopted)
        assertFalse(withCandidate.birthTimeCandidates.last().adopted)

        assertEquals(
            CaseMutationResult.Saved("case-candidate", 3),
            useCase.adopt(
                caseId = "case-candidate",
                expectedRevision = 2,
                candidateId = "candidate-new",
            ),
        )
        val adopted = repository.stored.getValue("case-candidate")
        val solar = adopted.birthInput.calendarInput as BirthCalendarInput.Solar
        assertEquals(11, solar.dateTime.hour)
        assertTrue(adopted.birthTimeCandidates.last().adopted)
        assertTrue(adopted.calculationSnapshots.last().adopted)
    }

    @Test
    fun `重复候选和旧修订均零写入拒绝`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-candidate-reject"] = sampleStoredCase("case-candidate-reject")
        }
        val useCase = BirthTimeCandidateUseCase(RecordingEngine(), repository)

        assertEquals(
            CaseMutationResult.ValidationFailed("该出生时间已经存在，无需重复添加。"),
            useCase.add(
                "case-candidate-reject",
                1,
                "重复时间",
                validForm().copy(alias = ""),
            ),
        )
        assertEquals(1L, repository.stored.getValue("case-candidate-reject").revision)
        assertEquals(
            CaseMutationResult.RevisionConflict(1),
            useCase.add(
                "case-candidate-reject",
                0,
                "旧修订",
                validForm().copy(alias = "", hour = "11"),
            ),
        )
    }

    @Test
    fun `编辑出生资料会重算并保留旧快照`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-edit"] = sampleStoredCase("case-edit")
        }
        val engine = RecordingEngine()
        val ids = ArrayDeque(listOf("candidate-old", "snapshot-new", "candidate-new"))
        val useCase = EditCaseUseCase(
            baziEngine = engine,
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        val result = useCase(
            caseId = "case-edit",
            expectedRevision = 1,
            form = validForm().copy(
                alias = "合成命例乙",
                sex = SexForFortuneDirection.WOMAN,
                hour = "11",
            ),
        )

        val savedResult = result as? CaseMutationResult.Saved
        assertTrue(savedResult != null)
        assertEquals("case-edit", savedResult?.caseId)
        assertEquals(2L, savedResult?.revision)
        assertEquals("合成命例乙", savedResult?.savedCase?.alias)
        val saved = repository.stored.getValue("case-edit")
        assertEquals("合成命例乙", saved.alias)
        assertEquals(2, saved.calculationSnapshots.size)
        assertFalse(saved.calculationSnapshots.first().adopted)
        assertTrue(saved.calculationSnapshots.last().adopted)
        assertEquals(1, engine.calls)
    }

    @Test
    fun `编辑保存采用规范化时区证据且夏令时重叠不会写库`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-zone"] = sampleStoredCase("case-zone")
        }
        val normalizedEngine = BaziEngine { input, _ ->
            calculationResult(
                input.copy(
                    resolvedUtcOffsetSeconds = 28_800,
                    timeZoneDataVersion = "tzdb:test",
                ),
            )
        }
        val zoneIds = ArrayDeque(
            listOf("candidate-zone-old", "snapshot-zone", "candidate-zone-new"),
        )
        val saved = EditCaseUseCase(
            normalizedEngine,
            repository,
            fixedClock,
            IdGenerator { zoneIds.removeFirst() },
        )("case-zone", 1, validForm())

        val savedResult = saved as? CaseMutationResult.Saved
        assertTrue(savedResult != null)
        assertEquals(2L, savedResult?.revision)
        assertEquals("case-zone", savedResult?.savedCase?.id)
        assertEquals(
            "tzdb:test",
            repository.stored.getValue("case-zone").birthInput.timeZoneDataVersion,
        )

        val choiceEngine = BaziEngine { _, _ ->
            throw TimeZoneChoiceRequiredException(
                "America/New_York",
                listOf(-14_400, -18_000),
                "tzdb:test",
            )
        }
        val choice = EditCaseUseCase(choiceEngine, repository)(
            "case-zone",
            2,
            validForm().copy(timeZoneId = "America/New_York"),
        )

        assertTrue(choice is CaseMutationResult.TimeZoneChoiceRequired)
        assertEquals(2L, repository.stored.getValue("case-zone").revision)
    }

    @Test
    fun `编辑时清空已有姓名保存为CLEARED而非ABSENT`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-name"] = sampleStoredCase("case-name")
        }
        val nameIds = ArrayDeque(
            listOf("candidate-name-old", "snapshot-name", "candidate-name-new"),
        )
        val useCase = EditCaseUseCase(
            baziEngine = RecordingEngine(),
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { nameIds.removeFirst() },
        )

        useCase("case-name", 1, validForm().copy(name = ""))

        assertEquals(
            ExplicitText.cleared(),
            repository.stored.getValue("case-name").name,
        )
    }

    @Test
    fun `旧修订在重算前被拒绝`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-edit"] = sampleStoredCase("case-edit").copy(revision = 3)
        }
        val engine = RecordingEngine()
        val useCase = EditCaseUseCase(engine, repository)

        val result = useCase("case-edit", 1, validForm())

        assertEquals(CaseMutationResult.RevisionConflict(3), result)
        assertEquals(0, engine.calls)
    }

    @Test
    fun `文本记录新增修改删除保留完整版本并统一分析分类`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-record"] = sampleStoredCase("case-record")
        }
        val useCase = TextRecordUseCase(
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { "record-1" },
        )

        assertEquals(
            CaseMutationResult.Saved("case-record", 2),
            useCase.save(
                "case-record",
                1,
                null,
                TextRecordDraft(CaseTextRecordType.OWNER_FEEDBACK, "合成反馈"),
            ),
        )
        val created = repository.stored.getValue("case-record").textRecords.single()
        assertEquals("record-1", created.id)
        assertEquals("合成反馈", created.content)

        useCase.save(
            "case-record",
            2,
            "record-1",
            TextRecordDraft(
                type = CaseTextRecordType.ANALYSIS,
                content = "修改后的合成分析",
                analysisCategory = AnalysisCategory.CAREER,
            ),
        )
        val edited = repository.stored.getValue("case-record").textRecords.single()
        assertEquals(created.createdAt, edited.createdAt)
        assertEquals(CaseTextRecordType.ANALYSIS, edited.type)
        assertEquals(AnalysisCategory.CAREER, edited.analysisCategory)

        assertEquals(
            CaseMutationResult.Saved("case-record", 4),
            useCase.delete("case-record", 3, "record-1"),
        )
        val deleted = repository.stored.getValue("case-record")
        assertTrue(deleted.textRecords.isEmpty())
        assertEquals(
            listOf(
                RecordChangeType.CREATED,
                RecordChangeType.UPDATED,
                RecordChangeType.DELETED,
            ),
            deleted.textRecordRevisions.map { it.changeType },
        )
        assertEquals(listOf(1, 2, 3), deleted.textRecordRevisions.map { it.version })
        assertEquals("合成反馈", deleted.textRecordRevisions.first().snapshot.content)
        assertEquals(
            "修改后的合成分析",
            deleted.textRecordRevisions.last().snapshot.content,
        )
    }

    @Test
    fun `采用点评候选新增分析但完整原点评与历史保持不变`() = runTest {
        val commentary = CaseTextRecord(
            id = "commentary-1",
            type = CaseTextRecordType.MASTER_COMMENTARY,
            content = "事业需要核对。财运也需核对",
            sourceAttachmentId = "attachment-1",
            createdAt = FixedInstant,
            updatedAt = FixedInstant,
        )
        val commentaryRevision = CaseTextRecordRevision(
            id = "commentary-1:revision:1",
            recordId = commentary.id,
            version = 1,
            changeType = RecordChangeType.CREATED,
            snapshot = commentary,
            changedAt = FixedInstant,
        )
        val repository = FakeCaseRepository().apply {
            stored["case-commentary"] = sampleStoredCase("case-commentary").copy(
                textRecords = listOf(commentary),
                textRecordRevisions = listOf(commentaryRevision),
                attachments = listOf(
                    SourceAttachment(
                        id = "attachment-1",
                        relativePath = "attachments/commentary.jpg",
                        originalFileName = "commentary.jpg",
                        mimeType = "image/jpeg",
                        sha256 = "0".repeat(64),
                        byteSize = 1,
                        createdAt = FixedInstant,
                    ),
                ),
            )
        }
        val useCase = TextRecordUseCase(
            repository,
            fixedClock,
            IdGenerator { "analysis-from-candidate" },
        )
        val candidate = candidate().copy(
            proposedContent = "编辑后保留事实边界的观点",
            proposedCategory = AnalysisCategory.CAREER,
        )

        val result = useCase.adoptCommentaryCandidate(
            "case-commentary",
            1,
            candidate,
        )

        assertEquals(
            MasterCommentaryCandidateAdoptionResult.Saved(
                "case-commentary",
                2,
                "analysis-from-candidate",
            ),
            result,
        )
        val stored = repository.stored.getValue("case-commentary")
        assertEquals(commentary, stored.textRecords.first())
        assertEquals(commentaryRevision, stored.textRecordRevisions.first())
        val analysis = stored.textRecords.last()
        assertEquals(CaseTextRecordType.ANALYSIS, analysis.type)
        assertEquals("编辑后保留事实边界的观点", analysis.content)
        assertEquals(AnalysisCategory.CAREER, analysis.analysisCategory)
        assertEquals("attachment-1", analysis.sourceAttachmentId)
        assertEquals(RecordChangeType.CREATED, stored.textRecordRevisions.last().changeType)
    }

    @Test
    fun `点评版本和原文区间过期均零写入拒绝`() = runTest {
        val commentary = CaseTextRecord(
            id = "commentary-1",
            type = CaseTextRecordType.MASTER_COMMENTARY,
            content = "事业需要核对。财运也需核对",
            createdAt = FixedInstant,
            updatedAt = FixedInstant,
        )
        val repository = FakeCaseRepository().apply {
            stored["case-commentary-stale"] = sampleStoredCase("case-commentary-stale").copy(
                textRecords = listOf(commentary),
                textRecordRevisions = listOf(
                    CaseTextRecordRevision(
                        id = "commentary-1:revision:2",
                        recordId = commentary.id,
                        version = 2,
                        changeType = RecordChangeType.UPDATED,
                        snapshot = commentary,
                        changedAt = FixedInstant,
                    ),
                ),
            )
        }
        val useCase = TextRecordUseCase(repository, fixedClock)

        val revisionFailure = useCase.adoptCommentaryCandidate(
            "case-commentary-stale",
            1,
            candidate(),
        ) as MasterCommentaryCandidateAdoptionResult.Failure
        assertEquals(
            MasterCommentaryCandidateAdoptionErrorCode.SOURCE_REVISION_STALE,
            revisionFailure.failure.code,
        )

        repository.stored["case-commentary-stale"] =
            repository.stored.getValue("case-commentary-stale").copy(
                textRecordRevisions = emptyList(),
                textRecords = listOf(commentary.copy(content = "原文已被替换")),
            )
        val rangeFailure = useCase.adoptCommentaryCandidate(
            "case-commentary-stale",
            1,
            candidate().copy(sourceRevision = 0),
        ) as MasterCommentaryCandidateAdoptionResult.Failure
        assertEquals(
            MasterCommentaryCandidateAdoptionErrorCode.SOURCE_RANGE_STALE,
            rangeFailure.failure.code,
        )
        assertEquals(1, repository.stored.getValue("case-commentary-stale").textRecords.size)
    }

    @Test
    fun `空记录与无效事件日期不会写入`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-invalid"] = sampleStoredCase("case-invalid")
        }
        val records = TextRecordUseCase(repository)
        val events = CaseEventUseCase(repository)

        assertEquals(
            CaseMutationResult.ValidationFailed("记录内容不能为空。"),
            records.save("case-invalid", 1, null, TextRecordDraft()),
        )
        assertEquals(
            CaseMutationResult.ValidationFailed("事件日期无效，请检查年月日。"),
            events.save(
                "case-invalid",
                1,
                null,
                EventDraft("2025", "2", "29", rawText = "合成事件"),
            ),
        )
        assertEquals(
            CaseMutationResult.ValidationFailed("事件标题不能超过 80 个字符。"),
            events.save(
                "case-invalid",
                1,
                null,
                EventDraft(
                    rawText = "合成事件",
                    title = "题".repeat(81),
                ),
            ),
        )
        assertTrue(repository.stored.getValue("case-invalid").events.isEmpty())
    }

    private fun candidate() = MasterCommentaryCandidate(
        id = "candidate-1",
        sourceRecordId = "commentary-1",
        sourceRevision = 1,
        sourceRange = CommentaryTextRange(0, 6),
        sourceExcerpt = "事业需要核对",
        proposedContent = "事业需要核对",
        proposedCategory = AnalysisCategory.CAREER,
        ruleEvidence = listOf(
            CommentaryCandidateRuleEvidence("test-rule", "合成测试规则"),
        ),
    )

    @Test
    fun `旧记录首次修改先补建基线版本`() = runTest {
        val legacy = CaseTextRecord(
            id = "legacy-record",
            type = CaseTextRecordType.NOTE,
            content = "旧版原文",
            createdAt = FixedInstant,
            updatedAt = FixedInstant,
        )
        val repository = FakeCaseRepository().apply {
            stored["case-legacy-record"] = sampleStoredCase("case-legacy-record").copy(
                textRecords = listOf(legacy),
            )
        }

        TextRecordUseCase(repository, fixedClock).save(
            "case-legacy-record",
            1,
            legacy.id,
            TextRecordDraft(content = "新版原文"),
        )

        val revisions = repository.stored
            .getValue("case-legacy-record")
            .textRecordRevisions
        assertEquals(listOf("旧版原文", "新版原文"), revisions.map { it.snapshot.content })
        assertEquals(
            listOf(RecordChangeType.CREATED, RecordChangeType.UPDATED),
            revisions.map { it.changeType },
        )
    }

    @Test
    fun `关键事件支持未知日期与年月日精度`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-event"] = sampleStoredCase("case-event")
        }
        val ids = ArrayDeque(listOf("event-unknown", "event-day"))
        val useCase = CaseEventUseCase(
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        useCase.save(
            "case-event",
            1,
            null,
            EventDraft(rawText = "时间待核对的合成事件"),
        )
        useCase.save(
            "case-event",
            2,
            null,
            EventDraft(
                year = "2024",
                month = "6",
                day = "1",
                status = "已确认",
                rawText = "有日期的合成事件",
                title = "事业阶段变化",
                category = CaseEventCategory.CAREER,
            ),
        )

        val events = repository.stored.getValue("case-event").events
        assertEquals(EventDatePrecision.UNKNOWN, events[0].datePrecision)
        assertEquals(EventDatePrecision.DAY, events[1].datePrecision)
        assertEquals("已确认", events[1].status)
        assertEquals("事业阶段变化", events[1].title)
        assertEquals(CaseEventCategory.CAREER, events[1].category)
        assertEquals(
            listOf(1, 1),
            repository.stored.getValue("case-event").eventRevisions.map { it.version },
        )
        assertTrue(
            repository.stored.getValue("case-event").eventRevisions.all {
                it.changeType == RecordChangeType.CREATED
            },
        )
    }

    @Test
    fun `旧事件修改删除保留基线和删除快照`() = runTest {
        val legacy = CaseEvent(
            id = "legacy-event",
            year = 2020,
            datePrecision = EventDatePrecision.YEAR,
            rawText = "旧版事件原文",
            createdAt = FixedInstant,
        )
        val repository = FakeCaseRepository().apply {
            stored["case-legacy-event"] = sampleStoredCase("case-legacy-event").copy(
                events = listOf(legacy),
            )
        }
        val useCase = CaseEventUseCase(repository, fixedClock)

        useCase.save(
            "case-legacy-event",
            1,
            legacy.id,
            EventDraft("2021", rawText = "新版事件原文"),
        )
        useCase.delete("case-legacy-event", 2, legacy.id)

        val stored = repository.stored.getValue("case-legacy-event")
        assertTrue(stored.events.isEmpty())
        assertEquals(
            listOf(
                RecordChangeType.CREATED,
                RecordChangeType.UPDATED,
                RecordChangeType.DELETED,
            ),
            stored.eventRevisions.map { it.changeType },
        )
        assertEquals(
            listOf("旧版事件原文", "新版事件原文", "新版事件原文"),
            stored.eventRevisions.map { it.snapshot.rawText },
        )
    }

    @Test
    fun `断事笔记一次保存反馈点评和大运包含的流年时间线`() = runTest {
        val ownerOne = CaseTextRecord(
            id = "owner-one",
            type = CaseTextRecordType.OWNER_FEEDBACK,
            content = "第一段旧反馈",
            createdAt = FixedInstant,
            updatedAt = FixedInstant,
        )
        val ownerTwo = ownerOne.copy(id = "owner-two", content = "第二段旧反馈")
        val legacyAnnual = CaseEvent(
            id = "annual-2024",
            year = 2024,
            datePrecision = EventDatePrecision.YEAR,
            stemBranch = "甲辰",
            rawText = "旧流年记录",
            createdAt = FixedInstant,
        )
        val repository = FakeCaseRepository().apply {
            stored["case-notes"] = sampleStoredCase("case-notes").copy(
                textRecords = listOf(ownerOne, ownerTwo),
                events = listOf(legacyAnnual),
            )
        }
        val useCase = CaseNotesEditorUseCase(
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { "commentary-record" },
        )

        val result = useCase.save(
            caseId = "case-notes",
            expectedRevision = 1,
            draft = CaseNotesDraft(
                ownerFeedback = "合并后的命主反馈",
                masterCommentary = "师傅统一点评",
                timeline = listOf(
                    CaseNotesTimelineDraft(
                        id = "decade-2020",
                        level = CaseEventTimelineLevel.DECADE,
                        year = 2020,
                        stemBranch = "庚子",
                    ),
                    CaseNotesTimelineDraft(
                        id = legacyAnnual.id,
                        level = CaseEventTimelineLevel.ANNUAL,
                        year = 2024,
                        stemBranch = "甲辰",
                        status = "吉",
                        content = "更新后的流年记录",
                    ),
                ),
            ),
        )

        assertEquals(CaseMutationResult.Saved("case-notes", 2), result)
        val stored = repository.stored.getValue("case-notes")
        assertEquals(1, stored.revision - 1)
        assertEquals(
            listOf("合并后的命主反馈", "师傅统一点评"),
            stored.textRecords.map { it.content }.sorted(),
        )
        assertEquals(2, stored.events.size)
        assertEquals("吉", stored.events.first { it.id == legacyAnnual.id }.status)
        assertEquals(
            CaseEventTimelineLevel.DECADE,
            stored.events.first { it.id == "decade-2020" }.timelineLevel,
        )
        assertEquals("", stored.events.first { it.id == "decade-2020" }.rawText)
        assertEquals(
            CaseEventTimelineLevel.ANNUAL,
            stored.events.first { it.id == legacyAnnual.id }.timelineLevel,
        )
        assertTrue(
            stored.textRecordRevisions.any {
                it.recordId == ownerTwo.id && it.changeType == RecordChangeType.DELETED
            },
        )
        assertTrue(
            stored.eventRevisions.any {
                it.eventId == legacyAnnual.id && it.changeType == RecordChangeType.UPDATED
            },
        )
    }

    @Test
    fun `AI点评允许未生成时手动录入并保存为独立记录`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-ai-notes"] = sampleStoredCase("case-ai-notes")
        }
        val useCase = CaseNotesEditorUseCase(
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { "manual-ai-commentary" },
        )

        val result = useCase.save(
            caseId = "case-ai-notes",
            expectedRevision = 1,
            draft = CaseNotesDraft(aiCommentary = "我自己整理的 AI 点评"),
        )

        assertEquals(CaseMutationResult.Saved("case-ai-notes", 2), result)
        val record = repository.stored.getValue("case-ai-notes").textRecords.single()
        assertEquals("manual-ai-commentary", record.id)
        assertEquals(CaseTextRecordType.ANALYSIS, record.type)
        assertEquals(TextRecordSourceType.USER, record.sourceType)
        assertEquals("我自己整理的 AI 点评", record.aiCommentaryBody())
    }

    @Test
    fun `切换编辑某个模型点评不会覆盖其他模型版本`() = runTest {
        val deepSeek = CaseTextRecord(
            id = "ai-deepseek",
            type = CaseTextRecordType.ANALYSIS,
            content = "${AI_COMMENTARY_MARKER}\n服务：DeepSeek\n模型：deepseek-v4-pro\n\nDeepSeek 原点评",
            analysisCategory = AnalysisCategory.GENERAL,
            sourceType = TextRecordSourceType.EXTERNAL_AI,
            createdAt = FixedInstant,
            updatedAt = FixedInstant,
        )
        val qwen = deepSeek.copy(
            id = "ai-qwen",
            content = "${AI_COMMENTARY_MARKER}\n服务：千问\n模型：qwen3.7-max\n\n千问原点评",
        )
        val repository = FakeCaseRepository().apply {
            stored["case-ai-versions"] = sampleStoredCase("case-ai-versions").copy(
                textRecords = listOf(deepSeek, qwen),
            )
        }
        val useCase = CaseNotesEditorUseCase(
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { error("编辑既有版本不应创建新记录") },
        )

        val result = useCase.save(
            caseId = "case-ai-versions",
            expectedRevision = 1,
            draft = CaseNotesDraft(
                aiCommentary = "千问修订后的点评",
                aiCommentaryRecordId = qwen.id,
            ),
        )

        assertEquals(CaseMutationResult.Saved("case-ai-versions", 2), result)
        val records = repository.stored.getValue("case-ai-versions").textRecords.associateBy { it.id }
        assertEquals("DeepSeek 原点评", records.getValue(deepSeek.id).aiCommentaryBody())
        assertEquals("千问修订后的点评", records.getValue(qwen.id).aiCommentaryBody())
        assertEquals("DeepSeek · V4 Pro", records.getValue(deepSeek.id).aiCommentaryVersionLabel())
        assertEquals("千问 · 3.7 Max", records.getValue(qwen.id).aiCommentaryVersionLabel())
    }

    @Test
    fun `读取数据库异常不会被误报为记录不存在`() = runTest {
        val repository = FakeCaseRepository().apply {
            readFailure = IllegalStateException("database closed")
        }
        val useCase = TextRecordUseCase(repository)

        val result = useCase.save(
            "case-1",
            1,
            null,
            TextRecordDraft(content = "合成记录"),
        )

        assertTrue(result is CaseMutationResult.StorageFailed)
    }

    @Test
    fun `分类编辑复用同名目录并保存收藏置顶`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-meta"] = sampleStoredCase("case-meta")
            stored["case-catalog"] = sampleStoredCase("case-catalog").copy(
                groups = listOf(CaseGroup("group-existing", "家人")),
                tags = listOf(CaseTag("tag-existing", "已核对")),
            )
        }
        val ids = ArrayDeque(listOf("group-new", "tag-new"))
        val useCase = CaseMetadataUseCase(
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        val result = useCase.save(
            caseId = "case-meta",
            expectedRevision = 1,
            draft = CaseMetadataDraft(
                groupNames = "家人，研究",
                tagNames = "已核对, 待复盘",
                isFavorite = true,
                isPinned = true,
            ),
        )

        assertEquals(CaseMutationResult.Saved("case-meta", 2), result)
        val saved = repository.stored.getValue("case-meta")
        assertEquals(listOf("group-existing", "group-new"), saved.groups.map { it.id })
        assertEquals(listOf("tag-existing", "tag-new"), saved.tags.map { it.id })
        assertTrue(saved.isFavorite)
        assertTrue(saved.isPinned)
    }

    @Test
    fun `分类数量和名称长度在写入前校验`() = runTest {
        val repository = FakeCaseRepository().apply {
            stored["case-meta"] = sampleStoredCase("case-meta")
        }
        val useCase = CaseMetadataUseCase(repository)

        val tooMany = (1..11).joinToString("，") { "分组$it" }
        assertEquals(
            CaseMutationResult.ValidationFailed("每个命例最多设置 10 个分组。"),
            useCase.save("case-meta", 1, CaseMetadataDraft(groupNames = tooMany)),
        )
        assertEquals(1L, repository.stored.getValue("case-meta").revision)
    }

    @Test
    fun `采用反馈主题只追加正式标签并复用全局同名身份`() = runTest {
        val feedback = feedbackRecord("工作有变化。")
        val stored = sampleStoredCase("case-feedback-theme").copy(
            textRecords = listOf(feedback),
            textRecordRevisions = listOf(feedbackRevision(feedback, 1)),
            events = listOf(sampleEvent()),
        )
        val repository = FakeCaseRepository().apply {
            this.stored[stored.id] = stored
            this.stored["case-catalog-theme"] = sampleStoredCase("case-catalog-theme").copy(
                tags = listOf(CaseTag("tag-career-existing", "事业")),
            )
        }
        val useCase = CaseMetadataUseCase(repository, fixedClock, IdGenerator { "unused" })

        val result = useCase.adoptFeedbackThemeCandidate(
            caseId = stored.id,
            expectedRevision = stored.revision,
            candidate = feedbackThemeCandidate(feedback),
        )

        assertEquals(
            FeedbackThemeAdoptionResult.Saved(
                caseId = stored.id,
                revision = 2,
                tagId = "tag-career-existing",
                tagName = "事业",
            ),
            result,
        )
        val saved = repository.stored.getValue(stored.id)
        assertEquals(listOf(CaseTag("tag-career-existing", "事业")), saved.tags)
        assertEquals(listOf(feedback), saved.textRecords)
        assertEquals(stored.textRecordRevisions, saved.textRecordRevisions)
        assertEquals(stored.events, saved.events)
    }

    @Test
    fun `反馈来源版本或证据过期均零写入拒绝`() = runTest {
        val feedback = feedbackRecord("工作有变化。")
        val stored = sampleStoredCase("case-feedback-theme-stale").copy(
            textRecords = listOf(feedback),
            textRecordRevisions = listOf(feedbackRevision(feedback, 2)),
        )
        val repository = FakeCaseRepository().apply { this.stored[stored.id] = stored }
        val useCase = CaseMetadataUseCase(repository)

        val revisionFailure = useCase.adoptFeedbackThemeCandidate(
            stored.id,
            stored.revision,
            feedbackThemeCandidate(feedback, sourceRevision = 1),
        ) as FeedbackThemeAdoptionResult.Failure
        assertEquals(
            FeedbackThemeAdoptionErrorCode.SOURCE_REVISION_STALE,
            revisionFailure.failure.code,
        )
        val rangeFailure = useCase.adoptFeedbackThemeCandidate(
            stored.id,
            stored.revision,
            feedbackThemeCandidate(feedback, sourceRevision = 2).copy(
                sourceEvidence = listOf(
                    FeedbackThemeSourceEvidence(
                        FeedbackThemeTextRange(0, 2),
                        "事业",
                        listOf("事业"),
                    ),
                ),
            ),
        ) as FeedbackThemeAdoptionResult.Failure
        assertEquals(
            FeedbackThemeAdoptionErrorCode.SOURCE_EVIDENCE_STALE,
            rangeFailure.failure.code,
        )
        assertEquals(stored, repository.stored.getValue(stored.id))
    }

    @Test
    fun `反馈主题同名标签和标签上限分别拒绝`() = runTest {
        val feedback = feedbackRecord("工作有变化。")
        val duplicate = sampleStoredCase("case-feedback-theme-duplicate").copy(
            textRecords = listOf(feedback),
            textRecordRevisions = listOf(feedbackRevision(feedback, 1)),
            tags = listOf(CaseTag("tag-career", "事业")),
        )
        val full = sampleStoredCase("case-feedback-theme-full").copy(
            textRecords = listOf(feedback),
            textRecordRevisions = listOf(feedbackRevision(feedback, 1)),
            tags = (1..10).map { CaseTag("tag-$it", "标签$it") },
        )
        val repository = FakeCaseRepository().apply {
            stored[duplicate.id] = duplicate
            stored[full.id] = full
        }
        val useCase = CaseMetadataUseCase(repository)

        val duplicateFailure = useCase.adoptFeedbackThemeCandidate(
            duplicate.id,
            duplicate.revision,
            feedbackThemeCandidate(feedback),
        ) as FeedbackThemeAdoptionResult.Failure
        val fullFailure = useCase.adoptFeedbackThemeCandidate(
            full.id,
            full.revision,
            feedbackThemeCandidate(feedback),
        ) as FeedbackThemeAdoptionResult.Failure
        assertEquals(
            FeedbackThemeAdoptionErrorCode.TAG_ALREADY_PRESENT,
            duplicateFailure.failure.code,
        )
        assertEquals(FeedbackThemeAdoptionErrorCode.TOO_MANY_TAGS, fullFailure.failure.code)
        assertEquals(duplicate, repository.stored.getValue(duplicate.id))
        assertEquals(full, repository.stored.getValue(full.id))
    }

    @Test
    fun `软删除完整保留聚合并可恢复`() = runTest {
        val record = CaseTextRecord(
            id = "record-preserved",
            type = CaseTextRecordType.NOTE,
            content = "必须保留的合成记录",
            createdAt = FixedInstant,
            updatedAt = FixedInstant,
        )
        val repository = FakeCaseRepository().apply {
            stored["case-trash"] = sampleStoredCase("case-trash").copy(
                textRecords = listOf(record),
            )
        }
        val useCase = CaseLifecycleUseCase(repository, fixedClock)

        assertEquals(
            CaseMutationResult.Saved("case-trash", 2),
            useCase.moveToTrash("case-trash", 1),
        )
        val trashed = repository.stored.getValue("case-trash")
        assertTrue(trashed.deletedAt != null)
        assertEquals(listOf(record), trashed.textRecords)

        assertEquals(
            CaseMutationResult.Saved("case-trash", 3),
            useCase.restore("case-trash", 2),
        )
        assertEquals(null, repository.stored.getValue("case-trash").deletedAt)
        assertEquals(listOf(record), repository.stored.getValue("case-trash").textRecords)
    }

    @Test
    fun `复制生成新身份并只复制出生资料与计算快照`() = runTest {
        val sourceRecord = CaseTextRecord(
            id = "source-record",
            type = CaseTextRecordType.NOTE,
            content = "不应进入副本的记录",
            createdAt = FixedInstant,
            updatedAt = FixedInstant,
        )
        val repository = FakeCaseRepository().apply {
            stored["case-source"] = sampleStoredCase("case-source").copy(
                textRecords = listOf(sourceRecord),
                textRecordRevisions = listOf(
                    CaseTextRecordRevision(
                        id = "source-record-revision",
                        recordId = sourceRecord.id,
                        version = 1,
                        changeType = RecordChangeType.CREATED,
                        snapshot = sourceRecord,
                        changedAt = FixedInstant,
                    ),
                ),
            )
        }
        val ids = ArrayDeque(listOf("case-copy", "snapshot-copy"))
        val useCase = CaseLifecycleUseCase(
            caseRepository = repository,
            clock = fixedClock,
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        val result = useCase.duplicate("case-source", 1)

        assertEquals(CaseMutationResult.Saved("case-copy", 1), result)
        val copied = repository.stored.getValue("case-copy")
        assertEquals("case-source", copied.copiedFromCaseId)
        assertEquals(CaseSourceType.CASE_COPY, copied.sourceType)
        assertEquals("snapshot-copy", copied.calculationSnapshots.single().id)
        assertTrue(copied.textRecords.isEmpty())
        assertTrue(copied.textRecordRevisions.isEmpty())
        assertTrue(copied.events.isEmpty())
        assertTrue(copied.eventRevisions.isEmpty())
        assertTrue(copied.attachments.isEmpty())
        assertTrue(copied.fieldEvidence.isEmpty())
    }

    @Test
    fun `命主反馈提示词上下文包含总结与按时间排序的事件`() {
        val case = sampleStoredCase("case-ai-feedback").copy(
            textRecords = listOf(feedbackRecord("命主总结：近年经历过转岗。")),
            events = listOf(
                CaseEvent(
                    id = "event-later",
                    title = "迁居",
                    year = 2025,
                    month = 5,
                    datePrecision = EventDatePrecision.MONTH,
                    stemBranch = "乙巳",
                    status = "已发生",
                    rawText = "因工作迁居。",
                    createdAt = FixedInstant,
                ),
                CaseEvent(
                    id = "event-earlier",
                    title = "转岗",
                    year = 2024,
                    month = 3,
                    datePrecision = EventDatePrecision.MONTH,
                    stemBranch = "甲辰",
                    rawText = "由技术岗转为管理岗。",
                    createdAt = FixedInstant,
                ),
            ),
        )

        val feedback = case.toAiAnalysisOwnerFeedback()

        assertEquals("命主总结：近年经历过转岗。", feedback.summary)
        assertEquals(listOf("2024年03月", "2025年05月"), feedback.timeline.map { it.timeLabel })
        assertEquals("由技术岗转为管理岗。", feedback.timeline.first().content)
        assertEquals("已发生", feedback.timeline.last().status)
    }

    private fun feedbackRecord(content: String) = CaseTextRecord(
        id = "feedback-record",
        type = CaseTextRecordType.OWNER_FEEDBACK,
        content = content,
        createdAt = FixedInstant,
        updatedAt = FixedInstant,
    )

    private fun feedbackRevision(record: CaseTextRecord, version: Int) =
        CaseTextRecordRevision(
            id = "feedback-revision-$version",
            recordId = record.id,
            version = version,
            changeType = RecordChangeType.CREATED,
            snapshot = record,
            changedAt = FixedInstant,
        )

    private fun feedbackThemeCandidate(
        record: CaseTextRecord,
        sourceRevision: Int = 1,
    ) = FeedbackThemeCandidate(
        id = "feedback-theme-career",
        sourceRecordId = record.id,
        sourceRevision = sourceRevision,
        canonicalTagName = "事业",
        proposedTagName = "事业",
        suggestedEventCategory = CaseEventCategory.CAREER,
        sourceEvidence = listOf(
            FeedbackThemeSourceEvidence(
                FeedbackThemeTextRange(0, 5),
                "工作有变化",
                listOf("工作"),
            ),
        ),
        ruleId = "feedback-theme-career-v1",
        ruleExplanation = "合成主题候选",
    )

    private fun sampleEvent() = CaseEvent(
        id = "feedback-event",
        title = "合成事件",
        category = CaseEventCategory.CAREER,
        year = 2020,
        datePrecision = EventDatePrecision.YEAR,
        rawText = "合成反馈事件",
        createdAt = FixedInstant,
    )
}
