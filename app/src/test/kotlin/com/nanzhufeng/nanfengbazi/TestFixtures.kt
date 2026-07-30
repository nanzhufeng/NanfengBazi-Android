package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseSearchRequest
import com.nanzhufeng.nanfengbazi.domain.CaseVisibility
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.DuplicateReason
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseSummary
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.FortuneStart
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import java.time.Instant

internal val FixedInstant: Instant = Instant.parse("2026-07-30T08:00:00Z")

internal fun validForm() = CaseFormState(
    alias = "合成命例甲",
    name = "测试甲",
    sex = SexForFortuneDirection.MAN,
    year = "2000",
    month = "2",
    day = "29",
    hour = "10",
    minute = "30",
    second = "0",
)

internal fun calculationResult(input: BirthInput): CalculationResult = CalculationResult(
    normalizedInput = input,
    profile = CalculationProfile.tymeDefault(),
    fourPillars = FourPillars("庚辰", "戊寅", "丁巳", "乙巳"),
    ownSign = "丁酉",
    bodySign = "己丑",
    fetalOrigin = "己巳",
    fetalBreath = "壬午",
    fortuneStart = FortuneStart(
        direction = FortuneDirection.FORWARD,
        startAt = CivilDateTime(2000, 2, 29, 10, 30, 0),
        endAt = CivilDateTime(2003, 6, 1, 0, 0, 0),
        years = 3,
        months = 3,
        days = 0,
        hours = 0,
        minutes = 0,
    ),
    decadeFortunes = listOf(
        DecadeFortune("己卯", 4, 13, 2003, 2012),
    ),
    evidence = CalculationEvidence(
        engineName = "TestEngine",
        engineVersion = CalculationProfile.TYME_ENGINE_VERSION,
        ruleVersion = CalculationProfile.DEFAULT_RULE_VERSION,
        calculatedAt = FixedInstant,
    ),
)

internal class RecordingEngine : BaziEngine {
    var calls = 0
    var lastInput: BirthInput? = null

    override suspend fun calculate(
        input: BirthInput,
        profile: CalculationProfile,
    ): CalculationResult {
        calls += 1
        lastInput = input
        return calculationResult(input)
    }
}

internal class FakeCaseRepository : CaseRepository {
    val stored = linkedMapOf<String, BaziCase>()
    val searchQueries = mutableListOf<String>()
    val searchRequests = mutableListOf<CaseSearchRequest>()
    var writeOverride: CaseWriteResult? = null
    var saveFailure: Exception? = null
    var readFailure: Exception? = null
    var lastExpectedRevision: Long? = -1

    override suspend fun save(
        case: BaziCase,
        expectedRevision: Long?,
    ): CaseWriteResult {
        saveFailure?.let { throw it }
        lastExpectedRevision = expectedRevision
        val override = writeOverride
        if (override != null) return override
        val current = stored[case.id]
        val nextRevision = (current?.revision ?: 0) + 1
        stored[case.id] = case.copy(revision = nextRevision)
        return if (current == null) {
            CaseWriteResult.Created(case.id, nextRevision)
        } else {
            CaseWriteResult.Updated(case.id, nextRevision)
        }
    }

    override suspend fun findById(id: String): BaziCase? {
        readFailure?.let { throw it }
        return stored[id]
    }

    override suspend fun search(request: CaseSearchRequest): List<CaseSummary> {
        readFailure?.let { throw it }
        val query = request.query
        searchQueries += query
        searchRequests += request
        return stored.values
            .filter {
                when (request.visibility) {
                    CaseVisibility.ACTIVE -> it.deletedAt == null
                    CaseVisibility.TRASHED -> it.deletedAt != null
                    CaseVisibility.ALL -> true
                }
            }
            .filter {
                query.isBlank() ||
                    it.alias.contains(query) ||
                    it.name.value?.contains(query) == true ||
                    it.calculationSnapshots.any { snapshot ->
                        val pillars = snapshot.result.fourPillars
                        listOf(pillars.year, pillars.month, pillars.day, pillars.hour)
                            .any { pillar -> pillar.contains(query) }
                    }
            }
            .filter { item ->
                request.groupId == null || item.groups.any { it.id == request.groupId }
            }
            .filter { item ->
                request.tagId == null || item.tags.any { it.id == request.tagId }
            }
            .map { case ->
                CaseSummary(
                    id = case.id,
                    alias = case.alias,
                    name = case.name,
                    sexForFortuneDirection = case.sexForFortuneDirection,
                    sourceType = case.sourceType,
                    birthInput = case.birthInput,
                    fourPillars = case.calculationSnapshots
                        .asReversed()
                        .firstOrNull { it.adopted }
                        ?.result
                        ?.fourPillars,
                    groups = case.groups,
                    tags = case.tags,
                    isFavorite = case.isFavorite,
                    isPinned = case.isPinned,
                    copiedFromCaseId = case.copiedFromCaseId,
                    createdAt = case.createdAt,
                    updatedAt = case.updatedAt,
                    lastViewedAt = case.lastViewedAt,
                    deletedAt = case.deletedAt,
                    revision = case.revision,
                    canonicalSolarDateTime = case.calculationSnapshots
                        .asReversed()
                        .firstOrNull { it.adopted }
                        ?.result
                        ?.calendarConversion
                        ?.solarDateTime,
                )
            }
    }

    override suspend fun findDuplicateCandidates(
        birthInput: BirthInput,
        fourPillars: FourPillars?,
        canonicalSolarDateTime: CivilDateTime?,
        excludeCaseId: String?,
    ): List<DuplicateCaseCandidate> {
        readFailure?.let { throw it }
        return stored.values
            .filterNot { it.id == excludeCaseId }
            .mapNotNull { candidate ->
                val reasons = buildSet {
                    if (
                        candidate.sexForFortuneDirection == birthInput.sexForFortuneDirection &&
                        candidate.calculationSnapshots
                            .asReversed()
                            .firstOrNull { it.adopted }
                            ?.result
                            ?.calendarConversion
                            ?.solarDateTime
                            ?.let { existing ->
                                canonicalSolarDateTime != null &&
                                    existing == canonicalSolarDateTime
                            }
                            ?: (candidate.birthInput.calendarInput == birthInput.calendarInput)
                    ) {
                        add(DuplicateReason.SAME_BIRTH_INPUT)
                    }
                    val adopted = candidate.calculationSnapshots
                        .asReversed()
                        .firstOrNull { it.adopted }
                        ?.result
                        ?.fourPillars
                    if (fourPillars != null && adopted == fourPillars) {
                        add(DuplicateReason.SAME_FOUR_PILLARS)
                    }
                }
                if (reasons.isEmpty()) {
                    null
                } else {
                    val summary = search(
                        CaseSearchRequest(visibility = CaseVisibility.ALL),
                    ).first { it.id == candidate.id }
                    DuplicateCaseCandidate(summary, reasons)
                }
            }
    }

    override suspend fun markViewed(caseId: String, viewedAt: Instant): Boolean {
        readFailure?.let { throw it }
        val current = stored[caseId] ?: return false
        if (current.deletedAt != null) return false
        stored[caseId] = current.copy(lastViewedAt = viewedAt)
        return true
    }
}

internal fun sampleStoredCase(id: String = "case-detail"): BaziCase {
    val form = CaseFormValidator.validate(validForm()) as CaseFormValidation.Valid
    val result = calculationResult(form.birthInput)
    return BaziCase(
        id = id,
        alias = form.alias,
        name = form.name?.let(ExplicitText::present) ?: ExplicitText.absent(),
        sexForFortuneDirection = SexForFortuneDirection.MAN,
        sourceType = com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType.MANUAL,
        birthInput = form.birthInput,
        calculationSnapshots = listOf(
            com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot(
                id = "snapshot-detail",
                result = result,
                adopted = true,
                createdAt = FixedInstant,
            ),
        ),
        createdAt = FixedInstant,
        updatedAt = FixedInstant,
        revision = 1,
    )
}
