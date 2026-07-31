package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.data.imports.PrivateImportImageStore
import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.ImportSessionRepository
import com.nanzhufeng.nanfengbazi.domain.ImportSessionWriteResult
import com.nanzhufeng.nanfengbazi.domain.ProfessionalFortuneResolver
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthTimeCandidate
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseFieldEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.CoordinateSource
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.EventDatePrecision
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.ImportCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.model.ImportSession
import com.nanzhufeng.nanfengbazi.domain.model.ImportStatus
import com.nanzhufeng.nanfengbazi.domain.model.ImportedLongTextEvidence
import com.nanzhufeng.nanfengbazi.domain.model.ImportedLongTextType
import com.nanzhufeng.nanfengbazi.domain.model.PillarDetail
import com.nanzhufeng.nanfengbazi.domain.model.PillarPosition
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SourceAttachment
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ScreenshotCandidateCommitResult {
    data class Committed(
        val caseId: String,
        val sessionCompleted: Boolean,
    ) : ScreenshotCandidateCommitResult

    data class DuplicateFound(
        val candidates: List<DuplicateCaseCandidate>,
    ) : ScreenshotCandidateCommitResult

    data class Rejected(val message: String) : ScreenshotCandidateCommitResult
    data class Failed(val message: String) : ScreenshotCandidateCommitResult
}

class ScreenshotImportCommitter(
    private val caseRepository: CaseRepository,
    private val importSessionRepository: ImportSessionRepository,
    private val baziEngine: BaziEngine,
    private val importImageStore: PrivateImportImageStore,
    private val attachmentRoot: Path,
    private val clock: Clock = Clock.systemUTC(),
    private val professionalFortuneResolver: ProfessionalFortuneResolver? = null,
) {
    suspend fun commitCandidate(
        sessionId: String,
        candidateId: String,
        allowDuplicate: Boolean = false,
    ): ScreenshotCandidateCommitResult {
        val session = importSessionRepository.findById(sessionId)
            ?: return ScreenshotCandidateCommitResult.Rejected("导入会话已经不存在。")
        if (
            session.status !in setOf(
                ImportStatus.NEEDS_REVIEW,
                ImportStatus.READY_TO_COMMIT,
                ImportStatus.COMMITTING,
            )
        ) {
            return ScreenshotCandidateCommitResult.Rejected("当前导入状态不能提交命例。")
        }
        val candidate = session.caseCandidates.singleOrNull { it.id == candidateId }
            ?: return ScreenshotCandidateCommitResult.Rejected("找不到对应待核对候选。")
        val caseId = stableCaseId(session.id, candidate.id)
        candidate.targetCaseId?.let { targetCaseId ->
            return completeSessionIfReady(session, targetCaseId)
        }

        val prepared = when (val result = prepareCase(session, candidate, caseId)) {
            is PreparedCaseResult.Valid -> result
            is PreparedCaseResult.Invalid ->
                return ScreenshotCandidateCommitResult.Rejected(result.message)
        }
        val existing = caseRepository.findById(caseId)
        if (existing == null) {
            val duplicates = try {
                caseRepository.findDuplicateCandidates(
                    birthInput = prepared.case.birthInput,
                    fourPillars = prepared.case.calculationSnapshots
                        .single()
                        .result
                        .fourPillars,
                    canonicalSolarDateTime = prepared.case.birthInput
                        .calendarInput
                        .let { it as BirthCalendarInput.Solar }
                        .dateTime,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                return ScreenshotCandidateCommitResult.Failed(
                    "重复命例检查失败，本次没有写入正式命例。",
                )
            }
            if (duplicates.isNotEmpty() && !allowDuplicate) {
                return ScreenshotCandidateCommitResult.DuplicateFound(duplicates)
            }
        } else if (existing.sourceType != CaseSourceType.WENZHEN_SCREENSHOT) {
            return ScreenshotCandidateCommitResult.Rejected("稳定命例标识已被其他来源占用。")
        }

        val createdPaths = mutableListOf<Path>()
        return try {
            copyAttachments(prepared, createdPaths)
            if (existing == null) {
                when (caseRepository.save(prepared.case, expectedRevision = null)) {
                    is CaseWriteResult.Created,
                    is CaseWriteResult.AlreadyExists,
                    -> Unit

                    is CaseWriteResult.Updated,
                    is CaseWriteResult.RevisionConflict,
                    -> error("正式命例写入出现非预期修订结果")
                }
            }
            markCandidateCommitted(sessionId, candidateId, caseId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            if (caseRepository.findById(caseId) == null) {
                cleanupCreatedPaths(createdPaths)
            }
            ScreenshotCandidateCommitResult.Failed(
                "命例提交未完成；待核对会话和原图仍保留，可稍后重试。",
            )
        }
    }

    suspend fun resumeCompletedSession(sessionId: String): ScreenshotCandidateCommitResult {
        val session = importSessionRepository.findById(sessionId)
            ?: return ScreenshotCandidateCommitResult.Rejected("导入会话已经不存在。")
        val targetCaseIds = session.caseCandidates
            .mapNotNull(ImportCaseCandidate::targetCaseId)
        val targetCaseId = targetCaseIds.lastOrNull()
            ?: return ScreenshotCandidateCommitResult.Rejected("当前会话没有可恢复的已提交候选。")
        if (session.caseCandidates.any { it.targetCaseId == null }) {
            return ScreenshotCandidateCommitResult.Rejected("当前会话仍有候选等待核对。")
        }
        if (targetCaseIds.any { caseRepository.findById(it) == null }) {
            return ScreenshotCandidateCommitResult.Failed(
                "导入完成状态与正式命例不一致，已停止自动收尾。",
            )
        }
        return completeSessionIfReady(session, targetCaseId)
    }

    private suspend fun prepareCase(
        session: ImportSession,
        candidate: ImportCaseCandidate,
        caseId: String,
    ): PreparedCaseResult {
        val fields = candidate.fieldEvidenceIds.mapNotNull { id ->
            session.extractedFields.singleOrNull { it.id == id }
        }
        fun adopted(fieldKey: String): TypedFieldValue? =
            fields.singleOrNull { it.fieldKey == fieldKey }?.adoptedValue

        val alias = (adopted(FIELD_ALIAS) as? TypedFieldValue.Text)
            ?.value
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: return PreparedCaseResult.Invalid("请先确认命例名称。")
        val sex = when ((adopted(FIELD_SEX) as? TypedFieldValue.Text)?.value) {
            "男" -> SexForFortuneDirection.MAN
            "女" -> SexForFortuneDirection.WOMAN
            else -> return PreparedCaseResult.Invalid("请先确认性别。")
        }
        val solarDate = (adopted(FIELD_SOLAR_DATE) as? TypedFieldValue.Text)
            ?.value
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return PreparedCaseResult.Invalid("请先确认有效的公历生日。")
        val sourcePillars = (adopted(FIELD_FOUR_PILLARS) as? TypedFieldValue.FourPillarsValue)
            ?.value
            ?: return PreparedCaseResult.Invalid("请先确认四柱。")
        val exactSolarDateTime =
            (adopted(FIELD_SOLAR_DATETIME) as? TypedFieldValue.DateTimeValue)?.value
        if (
            exactSolarDateTime != null &&
            (
                exactSolarDateTime.year != solarDate.year ||
                    exactSolarDateTime.month != solarDate.monthValue ||
                    exactSolarDateTime.day != solarDate.dayOfMonth
                )
        ) {
            return PreparedCaseResult.Invalid("公历日期与公历时间不是同一天，请返回核对。")
        }
        val representativeHour = sourcePillars.hour
            .lastOrNull()
            ?.let(HOUR_BY_BRANCH::get)
            ?: return PreparedCaseResult.Invalid("无法从时柱确定对应时辰，请人工补充出生时间。")
        val latitude = (adopted(FIELD_LATITUDE) as? TypedFieldValue.DecimalNumber)
            ?.canonicalValue
            ?.toDoubleOrNull()
        val longitude = (adopted(FIELD_LONGITUDE) as? TypedFieldValue.DecimalNumber)
            ?.canonicalValue
            ?.toDoubleOrNull()
        val hasCoordinates = latitude != null && longitude != null
        val locationName = (adopted(FIELD_LOCATION) as? TypedFieldValue.Text)
            ?.value
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        val birthDateTime = exactSolarDateTime ?: CivilDateTime(
            year = solarDate.year,
            month = solarDate.monthValue,
            day = solarDate.dayOfMonth,
            hour = representativeHour,
            minute = 0,
            second = 0,
        )
        val birthInput = BirthInput(
            calendarInput = BirthCalendarInput.Solar(
                birthDateTime,
            ),
            sexForFortuneDirection = sex,
            timePrecision = if (exactSolarDateTime == null) {
                TimePrecision.DOUBLE_HOUR_ONLY
            } else {
                TimePrecision.EXACT_TO_SECOND
            },
            timeZoneId = "Asia/Shanghai",
            resolvedUtcOffsetSeconds = 8 * 60 * 60,
            timeZoneDataVersion = TIME_ZONE_EVIDENCE_VERSION,
            locationName = locationName,
            longitude = longitude.takeIf { hasCoordinates },
            latitude = latitude.takeIf { hasCoordinates },
            coordinateSource = CoordinateSource.USER_ENTERED.takeIf { hasCoordinates },
            useTrueSolarTime = false,
            timeSourceType = TimeSourceType.WENZHEN_SCREENSHOT,
            sourceNote = if (exactSolarDateTime == null) {
                "由问真截图时柱推定对应时辰代表时刻，并与来源四柱复核。"
            } else {
                "采用问真基本资料页公历时间，并与来源四柱复核；真太阳时仅保留为证据，" +
                    "不重复校正。"
            },
        )
        val calculation = try {
            baziEngine.calculate(birthInput, CalculationProfile.tymeDefault())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            return PreparedCaseResult.Invalid("采用的出生资料无法完成排盘，请返回核对。")
        }
        if (calculation.fourPillars != sourcePillars) {
            return PreparedCaseResult.Invalid(
                "来源四柱与本机复算不一致，已阻止写入；请人工补充时间或修正 OCR。",
            )
        }
        val comparedFields = fields
            .withBasicChartComparisons(calculation)
            .withProfessionalComparisons(calculation)
        val normalizedBirthInput = calculation.normalizedInput
        val now = clock.instant()
        val snapshotId = "$caseId-snapshot"
        val birthCandidateId = "$caseId-birth"
        val candidateImageIds = candidate.imageIds.toSet()
        val attachments = session.images
            .filter { it.id in candidateImageIds }
            .map { image ->
                SourceAttachment(
                    id = image.id,
                    relativePath = attachmentRelativePath(caseId, image.id, image.mimeType),
                    originalFileName = image.originalFileName,
                    mimeType = image.mimeType,
                    sha256 = image.sha256,
                    byteSize = image.byteSize,
                    createdAt = image.createdAt,
                )
            }
        val adoptedLongTexts = candidate.longTextEvidenceIds.mapNotNull { id ->
            session.extractedLongTexts.singleOrNull { it.id == id }?.takeIf { it.adopted }
        }
        val adoptedEvents = fields.mapNotNull { field ->
            field.toCaseEvent(caseId, now)
        }
        val case = BaziCase(
            id = caseId,
            alias = alias,
            name = (adopted(FIELD_NAME) as? TypedFieldValue.Text)
                ?.value
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let(ExplicitText::present)
                ?: ExplicitText.absent(),
            sexForFortuneDirection = sex,
            sourceType = CaseSourceType.WENZHEN_SCREENSHOT,
            birthInput = normalizedBirthInput,
            birthTimeCandidates = listOf(
                BirthTimeCandidate(
                    id = birthCandidateId,
                    label = "问真截图时辰",
                    birthInput = normalizedBirthInput,
                    calculationSnapshotId = snapshotId,
                    adopted = true,
                    createdAt = now,
                ),
            ),
            textRecords = adoptedLongTexts.map { it.toCaseTextRecord(caseId, now) },
            events = adoptedEvents,
            calculationSnapshots = listOf(
                CaseCalculationSnapshot(
                    id = snapshotId,
                    result = calculation,
                    adopted = true,
                    birthTimeCandidateId = birthCandidateId,
                    createdAt = now,
                ),
            ),
            attachments = attachments,
            fieldEvidence = comparedFields,
            createdAt = now,
            updatedAt = now,
        )
        return PreparedCaseResult.Valid(
            case = case,
            imagesById = session.images.associateBy { it.id },
        )
    }

    private fun List<CaseFieldEvidence>.withProfessionalComparisons(
        calculation: com.nanzhufeng.nanfengbazi.domain.model.CalculationResult,
    ): List<CaseFieldEvidence> {
        val resolver = professionalFortuneResolver ?: return this
        val observedAt = singleOrNull {
            it.fieldKey == FIELD_PROFESSIONAL_OBSERVED_AT
        }?.normalizedValue
            ?.let { it as? TypedFieldValue.DateTimeValue }
            ?.value
            ?: return this
        val professional = runCatching {
            resolver.locate(calculation, observedAt)
        }.getOrNull() ?: return this
        val calculatedByKey = mapOf(
            FIELD_PROFESSIONAL_FLOW_YEAR to
                TypedFieldValue.Text(professional.flowPillars.year),
            FIELD_PROFESSIONAL_FLOW_MONTH to
                TypedFieldValue.Text(professional.flowPillars.month),
            FIELD_PROFESSIONAL_FLOW_DAY to
                TypedFieldValue.Text(professional.flowPillars.day),
            FIELD_PROFESSIONAL_FLOW_HOUR to
                TypedFieldValue.Text(professional.flowPillars.hour),
            FIELD_PROFESSIONAL_DECADE to professional.position.decadeFortune
                ?.name
                ?.let(TypedFieldValue::Text),
            FIELD_PROFESSIONAL_NATAL_YEAR to
                TypedFieldValue.Text(calculation.fourPillars.year),
            FIELD_PROFESSIONAL_NATAL_MONTH to
                TypedFieldValue.Text(calculation.fourPillars.month),
            FIELD_PROFESSIONAL_NATAL_DAY to
                TypedFieldValue.Text(calculation.fourPillars.day),
            FIELD_PROFESSIONAL_NATAL_HOUR to
                TypedFieldValue.Text(calculation.fourPillars.hour),
        )
        return withCalculatedValues(calculatedByKey)
    }

    private fun List<CaseFieldEvidence>.withBasicChartComparisons(
        calculation: com.nanzhufeng.nanfengbazi.domain.model.CalculationResult,
    ): List<CaseFieldEvidence> {
        val details = calculation.basicChartDetails ?: return this
        val sex = calculation.normalizedInput.sexForFortuneDirection
        val calculatedByKey = buildMap<String, TypedFieldValue> {
            put(
                FIELD_CONSTELLATION,
                TypedFieldValue.Text(
                    details.westernZodiac.let { if (it.endsWith("座")) it else "${it}座" },
                ),
            )
            put(FIELD_ZODIAC, TypedFieldValue.Text(details.zodiac))
            put(FIELD_FETAL_ORIGIN, TypedFieldValue.Text(calculation.fetalOrigin))
            put(FIELD_FETAL_BREATH, TypedFieldValue.Text(calculation.fetalBreath))
            put(FIELD_OWN_SIGN, TypedFieldValue.Text(calculation.ownSign))
            put(FIELD_BODY_SIGN, TypedFieldValue.Text(calculation.bodySign))
            details.previousJie?.let { term ->
                put(FIELD_PREVIOUS_JIE, TypedFieldValue.Text(term.sourceDisplay()))
            }
            details.nextJie?.let { term ->
                put(FIELD_NEXT_JIE, TypedFieldValue.Text(term.sourceDisplay()))
            }
            details.pillars.forEach { pillar ->
                val column = when (pillar.position) {
                    PillarPosition.YEAR -> "year"
                    PillarPosition.MONTH -> "month"
                    PillarPosition.DAY -> "day"
                    PillarPosition.HOUR -> "hour"
                }
                put(
                    "chart.$column.main_star",
                    TypedFieldValue.Text(pillar.mainStarForSourceComparison(sex)),
                )
                put(
                    "chart.$column.hidden_stems",
                    TypedFieldValue.Text(
                        pillar.hiddenStems.joinToString("\n") {
                            "${it.heavenStem}${it.element}"
                        },
                    ),
                )
                put(
                    "chart.$column.secondary_stars",
                    TypedFieldValue.Text(
                        pillar.hiddenStems.joinToString("\n") { it.tenGod },
                    ),
                )
                put(
                    "chart.$column.fortune_stage",
                    TypedFieldValue.Text(pillar.terrain),
                )
                put(
                    "chart.$column.self_stage",
                    TypedFieldValue.Text(pillar.selfSittingTerrain),
                )
                put(
                    "chart.$column.void",
                    TypedFieldValue.Text(pillar.voidEarthBranches.joinToString("")),
                )
                put(
                    "chart.$column.nayin",
                    TypedFieldValue.Text(pillar.naYin),
                )
            }
        }
        return withCalculatedValues(calculatedByKey)
    }

    private fun PillarDetail.mainStarForSourceComparison(
        sex: SexForFortuneDirection,
    ): String = if (position == PillarPosition.DAY) {
        when (sex) {
            SexForFortuneDirection.MAN -> "元男"
            SexForFortuneDirection.WOMAN -> "元女"
        }
    } else {
        primaryTenGod
    }

    private fun List<CaseFieldEvidence>.withCalculatedValues(
        calculatedByKey: Map<String, TypedFieldValue?>,
    ): List<CaseFieldEvidence> = map { field ->
        val calculated = calculatedByKey[field.fieldKey] ?: return@map field
        field.copy(
            calculatedValue = calculated,
            consistencyConfidence = if (field.normalizedValue == calculated) 1f else 0f,
        )
    }

    private suspend fun copyAttachments(
        prepared: PreparedCaseResult.Valid,
        createdPaths: MutableList<Path>,
    ) {
        prepared.case.attachments.forEach { attachment ->
            val image = requireNotNull(prepared.imagesById[attachment.id])
            val bytes = importImageStore.readBytes(image)
            val target = resolveAttachment(attachment.relativePath)
            withContext(Dispatchers.IO) {
                Files.createDirectories(target.parent)
                if (Files.exists(target)) {
                    require(Files.size(target) == attachment.byteSize) {
                        "既有附件大小不一致"
                    }
                    require(sha256(target) == attachment.sha256) {
                        "既有附件摘要不一致"
                    }
                    return@withContext
                }
                val temp = target.resolveSibling("${target.fileName}.part-${UUID.randomUUID()}")
                try {
                    Files.write(temp, bytes)
                    try {
                        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE)
                    } catch (_: AtomicMoveNotSupportedException) {
                        Files.move(temp, target)
                    }
                    createdPaths.add(target)
                } catch (error: Throwable) {
                    Files.deleteIfExists(temp)
                    throw error
                }
            }
        }
    }

    private suspend fun markCandidateCommitted(
        sessionId: String,
        candidateId: String,
        caseId: String,
    ): ScreenshotCandidateCommitResult {
        var session = importSessionRepository.findById(sessionId)
            ?: return ScreenshotCandidateCommitResult.Failed("正式命例已写入，但导入会话需要恢复。")
        if (session.caseCandidates.none { it.id == candidateId }) {
            return ScreenshotCandidateCommitResult.Failed("正式命例已写入，但候选状态需要恢复。")
        }
        if (session.caseCandidates.single { it.id == candidateId }.targetCaseId == null) {
            val updated = session.copy(
                caseCandidates = session.caseCandidates.map { candidate ->
                    if (candidate.id == candidateId) {
                        candidate.copy(targetCaseId = caseId)
                    } else {
                        candidate
                    }
                },
                updatedAt = clock.instant().coerceAtLeast(session.updatedAt),
            )
            val write = importSessionRepository.save(updated, session.revision)
            if (write !is ImportSessionWriteResult.Updated) {
                return ScreenshotCandidateCommitResult.Failed(
                    "正式命例已写入，但导入会话标记失败；请重试以完成恢复。",
                )
            }
            session = requireNotNull(importSessionRepository.findById(sessionId))
        }
        return completeSessionIfReady(session, caseId)
    }

    private suspend fun completeSessionIfReady(
        initial: ImportSession,
        caseId: String,
    ): ScreenshotCandidateCommitResult {
        if (initial.caseCandidates.any { it.targetCaseId == null }) {
            return ScreenshotCandidateCommitResult.Committed(caseId, sessionCompleted = false)
        }
        var session = initial
        if (session.status == ImportStatus.NEEDS_REVIEW) {
            session = persistStatus(session, ImportStatus.READY_TO_COMMIT)
                ?: return ScreenshotCandidateCommitResult.Failed(
                    "命例已写入，但完成状态待恢复。",
                )
        }
        if (session.status == ImportStatus.READY_TO_COMMIT) {
            session = persistStatus(session, ImportStatus.COMMITTING)
                ?: return ScreenshotCandidateCommitResult.Failed(
                    "命例已写入，但完成状态待恢复。",
                )
        }
        if (session.status == ImportStatus.COMMITTING) {
            val completed = session.copy(
                status = ImportStatus.COMPLETED,
                completedAt = clock.instant().coerceAtLeast(session.updatedAt),
                updatedAt = clock.instant().coerceAtLeast(session.updatedAt),
            )
            if (importSessionRepository.save(completed, session.revision)
                !is ImportSessionWriteResult.Updated
            ) {
                return ScreenshotCandidateCommitResult.Failed(
                    "命例已写入，但完成状态待恢复。",
                )
            }
        }
        return ScreenshotCandidateCommitResult.Committed(caseId, sessionCompleted = true)
    }

    private suspend fun persistStatus(
        session: ImportSession,
        status: ImportStatus,
    ): ImportSession? {
        val updated = session.copy(
            status = status,
            updatedAt = clock.instant().coerceAtLeast(session.updatedAt),
        )
        if (importSessionRepository.save(updated, session.revision)
            !is ImportSessionWriteResult.Updated
        ) {
            return null
        }
        return importSessionRepository.findById(session.id)
    }

    private fun ImportedLongTextEvidence.toCaseTextRecord(
        caseId: String,
        now: java.time.Instant,
    ) = CaseTextRecord(
        id = "$caseId-text-${id.takeLast(16)}",
        type = when (type) {
            ImportedLongTextType.OWNER_FEEDBACK -> CaseTextRecordType.OWNER_FEEDBACK
            ImportedLongTextType.MASTER_COMMENTARY -> CaseTextRecordType.MASTER_COMMENTARY
            ImportedLongTextType.UNKNOWN -> CaseTextRecordType.NOTE
        },
        content = rawText,
        sourceAttachmentId = imageId,
        createdAt = now,
        updatedAt = now,
    )

    private fun CaseFieldEvidence.toCaseEvent(
        caseId: String,
        now: java.time.Instant,
    ): CaseEvent? {
        if (!fieldKey.startsWith(FIELD_EVENT_PREFIX)) return null
        val adoptedText = (adoptedValue as? TypedFieldValue.Text)
            ?.value
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: return null
        val year = EVENT_FIELD_PATTERN.matchEntire(fieldKey)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: return null
        val stemBranch = EVENT_STEM_BRANCH_PATTERN.find(rawText)?.groupValues?.get(1)
        return CaseEvent(
            id = "$caseId-event-${id.takeLast(16)}",
            title = adoptedText.lineSequence().first().take(40),
            category = adoptedText.inferredEventCategory(),
            year = year,
            datePrecision = EventDatePrecision.YEAR,
            stemBranch = stemBranch,
            rawText = rawText,
            normalizedText = ExplicitText.present(adoptedText),
            sourceAttachmentId = attachmentId,
            createdAt = now,
        )
    }

    private fun String.inferredEventCategory(): CaseEventCategory = when {
        containsAny("学校", "大学", "考试", "学习", "毕业", "入学") ->
            CaseEventCategory.EDUCATION
        containsAny("工作", "单位", "职业", "公司", "入职", "离职", "创业") ->
            CaseEventCategory.CAREER
        containsAny("结婚", "婚姻", "恋爱", "对象", "离婚", "感情") ->
            CaseEventCategory.RELATIONSHIP
        containsAny("疾病", "住院", "手术", "健康", "受伤") ->
            CaseEventCategory.HEALTH
        containsAny("收入", "财务", "亏损", "赚钱", "投资", "买房") ->
            CaseEventCategory.WEALTH
        containsAny("父亲", "母亲", "父母", "子女", "孩子", "家庭") ->
            CaseEventCategory.FAMILY
        else -> CaseEventCategory.GENERAL
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any(::contains)

    private fun resolveAttachment(relativePath: String): Path {
        require(!relativePath.startsWith("/") && '\\' !in relativePath && ':' !in relativePath)
        require(relativePath.split('/').none { it.isBlank() || it == "." || it == ".." })
        val root = attachmentRoot.toAbsolutePath().normalize()
        val target = root.resolve(relativePath).normalize()
        require(target.startsWith(root)) { "附件路径越界" }
        return target
    }

    private suspend fun cleanupCreatedPaths(paths: List<Path>) {
        withContext(Dispatchers.IO) {
            paths.asReversed().forEach { Files.deleteIfExists(it) }
        }
    }

    private fun stableCaseId(sessionId: String, candidateId: String): String =
        "wenzhen-${stableToken("$sessionId\u0000$candidateId")}"

    private fun stableToken(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(24)

    private fun attachmentRelativePath(
        caseId: String,
        imageId: String,
        mimeType: String,
    ): String = "wenzhen/$caseId/$imageId.${mimeType.extension()}"

    private fun String.extension(): String = when (lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/heic" -> "heic"
        "image/heif" -> "heif"
        else -> "img"
    }

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private sealed interface PreparedCaseResult {
        data class Valid(
            val case: BaziCase,
            val imagesById: Map<String, com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef>,
        ) : PreparedCaseResult

        data class Invalid(val message: String) : PreparedCaseResult
    }

    private companion object {
        const val FIELD_ALIAS = "identity.alias"
        const val FIELD_NAME = "identity.name"
        const val FIELD_SEX = "identity.sex"
        const val FIELD_CONSTELLATION = "identity.constellation"
        const val FIELD_ZODIAC = "identity.zodiac"
        const val FIELD_SOLAR_DATE = "birth.solar_date"
        const val FIELD_SOLAR_DATETIME = "birth.solar_datetime"
        const val FIELD_LOCATION = "birth.location"
        const val FIELD_LATITUDE = "birth.latitude"
        const val FIELD_LONGITUDE = "birth.longitude"
        const val FIELD_PREVIOUS_JIE = "birth.previous_jie"
        const val FIELD_NEXT_JIE = "birth.next_jie"
        const val FIELD_FOUR_PILLARS = "chart.four_pillars"
        const val FIELD_FETAL_ORIGIN = "chart.fetal_origin"
        const val FIELD_FETAL_BREATH = "chart.fetal_breath"
        const val FIELD_OWN_SIGN = "chart.own_sign"
        const val FIELD_BODY_SIGN = "chart.body_sign"
        const val FIELD_PROFESSIONAL_OBSERVED_AT = "professional.observed_at"
        const val FIELD_PROFESSIONAL_FLOW_YEAR = "professional.flow_year"
        const val FIELD_PROFESSIONAL_FLOW_MONTH = "professional.flow_month"
        const val FIELD_PROFESSIONAL_FLOW_DAY = "professional.flow_day"
        const val FIELD_PROFESSIONAL_FLOW_HOUR = "professional.flow_hour"
        const val FIELD_PROFESSIONAL_DECADE = "professional.decade"
        const val FIELD_PROFESSIONAL_NATAL_YEAR = "professional.natal_year"
        const val FIELD_PROFESSIONAL_NATAL_MONTH = "professional.natal_month"
        const val FIELD_PROFESSIONAL_NATAL_DAY = "professional.natal_day"
        const val FIELD_PROFESSIONAL_NATAL_HOUR = "professional.natal_hour"
        const val FIELD_EVENT_PREFIX = "event.candidate."
        const val TIME_ZONE_EVIDENCE_VERSION = "Asia-Shanghai-fixed-UTC+08-import-v1"
        val HOUR_BY_BRANCH = mapOf(
            '子' to 0,
            '丑' to 2,
            '寅' to 4,
            '卯' to 6,
            '辰' to 8,
            '巳' to 10,
            '午' to 12,
            '未' to 14,
            '申' to 16,
            '酉' to 18,
            '戌' to 20,
            '亥' to 22,
        )
        val EVENT_FIELD_PATTERN = Regex(
            "event\\.candidate\\.((?:19|20)\\d{2})\\.\\d+",
        )
        val EVENT_STEM_BRANCH_PATTERN = Regex(
            "(?:19|20)\\d{2}年\\s*([甲乙丙丁戊己庚辛壬癸][子丑寅卯辰巳午未申酉戌亥])",
        )
    }
}

private fun com.nanzhufeng.nanfengbazi.domain.model.SolarTermPoint.sourceDisplay(): String =
    "$name %04d-%02d-%02d %02d:%02d:%02d".format(
        Locale.ROOT,
        at.year,
        at.month,
        at.day,
        at.hour,
        at.minute,
        at.second,
    )
