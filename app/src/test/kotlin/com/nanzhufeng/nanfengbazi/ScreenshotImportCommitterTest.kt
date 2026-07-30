package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.data.imports.PrivateImportImageStore
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.DuplicateReason
import com.nanzhufeng.nanfengbazi.domain.ImportSessionDeleteResult
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CaseFieldEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.ImportCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.model.ImportSession
import com.nanzhufeng.nanfengbazi.domain.model.ImportSourceApp
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
import com.nanzhufeng.nanfengbazi.domain.model.ImportedLongTextEvidence
import com.nanzhufeng.nanfengbazi.domain.model.ImportedLongTextType
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.canTransitionTo
import com.nanzhufeng.nanfengbazi.engine.tyme.TymeBaziEngine
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ScreenshotImportCommitterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `全部人工采用且本机复算一致后写入正式命例并完成会话`() = runTest {
        val fixture = fixture(
            pillars = FourPillars("壬申", "戊申", "壬申", "丙午"),
            adoptAll = true,
        )
        val result = fixture.committer.commitCandidate("session-1", "candidate-1")

        assertTrue(result is ScreenshotCandidateCommitResult.Committed)
        assertTrue((result as ScreenshotCandidateCommitResult.Committed).sessionCompleted)
        val case = fixture.caseRepository.cases.values.single()
        assertEquals(CaseSourceType.WENZHEN_SCREENSHOT, case.sourceType)
        assertEquals("案例甲", case.alias)
        assertEquals(FourPillars("壬申", "戊申", "壬申", "丙午"), case.adoptedPillars())
        assertEquals(1, case.attachments.size)
        assertEquals(4, case.fieldEvidence.size)
        assertEquals("反馈完整原文", case.textRecords.single().content)
        assertTrue(
            java.nio.file.Files.exists(
                fixture.attachmentRoot.resolve(case.attachments.single().relativePath),
            ),
        )
        val session = requireNotNull(fixture.importRepository.findById("session-1"))
        assertEquals(ImportStatus.COMPLETED, session.status)
        assertNotNull(session.completedAt)
        assertEquals(case.id, session.caseCandidates.single().targetCaseId)
    }

    @Test
    fun `字段未人工采用时拒绝写入且原会话保持待核对`() = runTest {
        val fixture = fixture(
            pillars = FourPillars("壬申", "戊申", "壬申", "丙午"),
            adoptAll = false,
        )

        val result = fixture.committer.commitCandidate("session-1", "candidate-1")

        assertTrue(result is ScreenshotCandidateCommitResult.Rejected)
        assertTrue(fixture.caseRepository.cases.isEmpty())
        assertEquals(
            ImportStatus.NEEDS_REVIEW,
            fixture.importRepository.findById("session-1")?.status,
        )
    }

    @Test
    fun `来源四柱与本机复算不一致时硬拦截且不复制附件`() = runTest {
        val fixture = fixture(
            pillars = FourPillars("甲子", "乙丑", "丙寅", "丁卯"),
            adoptAll = true,
        )

        val result = fixture.committer.commitCandidate("session-1", "candidate-1")

        assertTrue(result is ScreenshotCandidateCommitResult.Rejected)
        assertTrue((result as ScreenshotCandidateCommitResult.Rejected).message.contains("复算不一致"))
        assertTrue(fixture.caseRepository.cases.isEmpty())
        assertFalse(java.nio.file.Files.exists(fixture.attachmentRoot.resolve("wenzhen")))
    }

    @Test
    fun `疑似重复必须人工确认后才能保留两份`() = runTest {
        val seed = fixture(
            pillars = FourPillars("壬申", "戊申", "壬申", "丙午"),
            adoptAll = true,
        )
        assertTrue(
            seed.committer.commitCandidate("session-1", "candidate-1") is
                ScreenshotCandidateCommitResult.Committed,
        )
        val existing = seed.caseRepository.cases.values.single()
        val fixture = fixture(
            pillars = FourPillars("壬申", "戊申", "壬申", "丙午"),
            adoptAll = true,
            folderSuffix = "-duplicate",
        )
        fixture.caseRepository.duplicateCandidates = listOf(
            DuplicateCaseCandidate(
                summary = CaseSummary(
                    id = existing.id,
                    alias = existing.alias,
                    name = existing.name,
                    sexForFortuneDirection = existing.sexForFortuneDirection,
                    sourceType = existing.sourceType,
                    birthInput = existing.birthInput,
                    fourPillars = existing.adoptedPillars(),
                    groups = existing.groups,
                    tags = existing.tags,
                    isFavorite = existing.isFavorite,
                    isPinned = existing.isPinned,
                    copiedFromCaseId = existing.copiedFromCaseId,
                    createdAt = existing.createdAt,
                    updatedAt = existing.updatedAt,
                    lastViewedAt = existing.lastViewedAt,
                    deletedAt = existing.deletedAt,
                    revision = existing.revision,
                ),
                reasons = setOf(DuplicateReason.SAME_FOUR_PILLARS),
            ),
        )

        val blocked = fixture.committer.commitCandidate("session-1", "candidate-1")
        assertTrue(blocked is ScreenshotCandidateCommitResult.DuplicateFound)
        assertTrue(fixture.caseRepository.cases.isEmpty())

        val confirmed = fixture.committer.commitCandidate(
            "session-1",
            "candidate-1",
            allowDuplicate = true,
        )
        assertTrue(confirmed is ScreenshotCandidateCommitResult.Committed)
        assertEquals(1, fixture.caseRepository.cases.size)
    }

    @Test
    fun `采用基本资料页字段后使用精确时间姓名地区和经纬度`() = runTest {
        val fixture = fixture(
            pillars = FourPillars("壬申", "戊申", "壬申", "丙午"),
            adoptAll = true,
            extraFields = listOf(
                "identity.name" to TypedFieldValue.Text("席瑞"),
                "birth.solar_datetime" to TypedFieldValue.DateTimeValue(
                    CivilDateTime(1992, 8, 24, 12, 0, 0),
                ),
                "birth.location" to TypedFieldValue.Text("江苏省宿迁市泗阳县"),
                "birth.latitude" to TypedFieldValue.DecimalNumber("33.72"),
                "birth.longitude" to TypedFieldValue.DecimalNumber("118.68"),
            ),
        )

        val result = fixture.committer.commitCandidate("session-1", "candidate-1")

        assertTrue(result is ScreenshotCandidateCommitResult.Committed)
        val case = fixture.caseRepository.cases.values.single()
        assertEquals("席瑞", case.name.value)
        assertEquals(TimePrecision.EXACT_TO_SECOND, case.birthInput.timePrecision)
        assertEquals("江苏省宿迁市泗阳县", case.birthInput.locationName)
        assertEquals(33.72, case.birthInput.latitude)
        assertEquals(118.68, case.birthInput.longitude)
        val solar = case.birthInput.calendarInput as BirthCalendarInput.Solar
        assertEquals(CivilDateTime(1992, 8, 24, 12, 0, 0), solar.dateTime)
        assertEquals(9, case.fieldEvidence.size)
    }

    private suspend fun fixture(
        pillars: FourPillars,
        adoptAll: Boolean,
        folderSuffix: String = "",
        extraFields: List<Pair<String, TypedFieldValue>> = emptyList(),
    ): Fixture {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val importRoot =
            temporaryFolder.newFolder("imports-${pillars.year}$folderSuffix").toPath()
        val attachmentRoot =
            temporaryFolder.newFolder("attachments-${pillars.year}$folderSuffix").toPath()
        val imageStore = PrivateImportImageStore(importRoot)
        val image = imageStore.copyImage(
            sessionId = "session-1",
            imageId = "image-1",
            originalFileName = "fixture.png",
            mimeType = "image/png",
            createdAt = now,
            openInput = { ByteArrayInputStream("fixture-image".encodeToByteArray()) },
        )
        fun field(
            id: String,
            key: String,
            value: TypedFieldValue,
        ) = CaseFieldEvidence(
            id = id,
            attachmentId = image.id,
            fieldKey = key,
            rawText = value.toString(),
            normalizedValue = value,
            adoptedValue = value.takeIf { adoptAll },
            parserConfidence = 0.95f,
            parserRuleId = "fixture",
            userEdited = false,
            createdAt = now,
        )
        val fields = listOf(
            field("alias", "identity.alias", TypedFieldValue.Text("案例甲")),
            field("sex", "identity.sex", TypedFieldValue.Text("男")),
            field("date", "birth.solar_date", TypedFieldValue.Text("1992-08-24")),
            field(
                "pillars",
                "chart.four_pillars",
                TypedFieldValue.FourPillarsValue(pillars),
            ),
        ) + extraFields.mapIndexed { index, (key, value) ->
            field("extra-$index", key, value)
        }
        val longText = ImportedLongTextEvidence(
            id = "feedback",
            imageId = image.id,
            type = ImportedLongTextType.OWNER_FEEDBACK,
            rawText = "反馈完整原文",
            ocrConfidence = 0.9f,
            parserConfidence = 0.9f,
            parserRuleId = "fixture",
            adopted = adoptAll,
        )
        val session = ImportSession(
            id = "session-1",
            sourceApp = ImportSourceApp.WENZHEN_BAZI,
            status = ImportStatus.NEEDS_REVIEW,
            images = listOf(image),
            extractedFields = fields,
            extractedLongTexts = listOf(longText),
            caseCandidates = listOf(
                ImportCaseCandidate(
                    id = "candidate-1",
                    imageIds = listOf(image.id),
                    fieldEvidenceIds = fields.map { it.id },
                    longTextEvidenceIds = listOf(longText.id),
                    suggestedAlias = "案例甲",
                    groupingConfidence = 0.95f,
                    requiresReview = true,
                ),
            ),
            parserVersion = "fixture",
            createdAt = now,
            updatedAt = now,
            revision = 1,
        )
        val importRepository = FakeCommitImportRepository(session)
        val caseRepository = FakeCommitCaseRepository()
        return Fixture(
            committer = ScreenshotImportCommitter(
                caseRepository = caseRepository,
                importSessionRepository = importRepository,
                baziEngine = TymeBaziEngine(),
                importImageStore = imageStore,
                attachmentRoot = attachmentRoot,
                clock = Clock.fixed(now, ZoneOffset.UTC),
            ),
            importRepository = importRepository,
            caseRepository = caseRepository,
            attachmentRoot = attachmentRoot,
        )
    }

    private data class Fixture(
        val committer: ScreenshotImportCommitter,
        val importRepository: FakeCommitImportRepository,
        val caseRepository: FakeCommitCaseRepository,
        val attachmentRoot: java.nio.file.Path,
    )
}

private class FakeCommitImportRepository(
    initial: ImportSession,
) : ImportSessionRepository {
    private var session: ImportSession? = initial

    override suspend fun save(
        session: ImportSession,
        expectedRevision: Long?,
    ): ImportSessionWriteResult {
        val current = this.session
        if (current == null) {
            this.session = session.copy(revision = 1)
            return ImportSessionWriteResult.Created(session.id, 1)
        }
        if (expectedRevision != current.revision) {
            return ImportSessionWriteResult.RevisionConflict(
                session.id,
                expectedRevision ?: -1,
                current.revision,
            )
        }
        if (!current.status.canTransitionTo(session.status)) {
            return ImportSessionWriteResult.InvalidTransition(
                session.id,
                current.status,
                session.status,
            )
        }
        this.session = session.copy(revision = current.revision + 1)
        return ImportSessionWriteResult.Updated(session.id, current.revision + 1)
    }

    override suspend fun findById(id: String): ImportSession? = session?.takeIf { it.id == id }

    override suspend fun list(statuses: Set<ImportStatus>): List<ImportSession> =
        listOfNotNull(session).filter { statuses.isEmpty() || it.status in statuses }

    override suspend fun delete(
        id: String,
        expectedRevision: Long,
    ): ImportSessionDeleteResult = ImportSessionDeleteResult.NotFound
}

private class FakeCommitCaseRepository : CaseRepository {
    val cases = linkedMapOf<String, BaziCase>()
    var duplicateCandidates: List<DuplicateCaseCandidate> = emptyList()

    override suspend fun save(
        case: BaziCase,
        expectedRevision: Long?,
    ): CaseWriteResult {
        val existing = cases[case.id]
        if (existing != null) return CaseWriteResult.AlreadyExists(case.id, existing.revision)
        cases[case.id] = case.copy(revision = 1)
        return CaseWriteResult.Created(case.id, 1)
    }

    override suspend fun findById(id: String): BaziCase? = cases[id]

    override suspend fun search(request: CaseSearchRequest): List<CaseSummary> = emptyList()

    override suspend fun findDuplicateCandidates(
        birthInput: BirthInput,
        fourPillars: FourPillars?,
        canonicalSolarDateTime: CivilDateTime?,
        excludeCaseId: String?,
    ): List<DuplicateCaseCandidate> = duplicateCandidates

    override suspend fun markViewed(caseId: String, viewedAt: Instant): Boolean = false
}

private fun BaziCase.adoptedPillars(): FourPillars =
    calculationSnapshots.single { it.adopted }.result.fourPillars
