package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthTimeCandidate
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.CoordinateSource
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TimeSourceType
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A reviewable, offline package of researched public-figure birth data.
 *
 * The package deliberately contains evidence and uncertainty rather than copied
 * astrological interpretations. It is a separate protocol from the Wenzhen
 * fidelity importer: no public-source record can be labelled as Wenzhen data.
 */
@Serializable
data class CuratedCelebrityCatalogPackage(
    val format: String,
    val version: Int,
    val catalogVersion: String,
    val compiledAt: String,
    val cases: List<CuratedCelebrityCase>,
) {
    init {
        require(format == FORMAT) { "不是南枫八字支持的名人案例统一资料包。" }
        require(version == VERSION) { "名人案例统一资料包版本不受支持：$version。" }
        require(cases.map { it.sourceId }.distinct().size == cases.size) { "资料包存在重复案例标识。" }
    }

    companion object {
        const val FORMAT = "nanfeng-bazi-curated-celebrity-catalog"
        const val VERSION = 1
    }
}

@Serializable
data class CuratedCelebrityCase(
    val sourceId: String,
    val name: String,
    val sex: String,
    val groupName: String,
    val tags: List<String>,
    val birthPlace: String,
    val timeZoneId: String,
    val longitude: Double,
    val latitude: Double,
    val birthDate: CuratedDate,
    val alternativeBirthDates: List<CuratedDateCandidate> = emptyList(),
    val defaultTime: CuratedTimeCandidate,
    val alternativeTimes: List<CuratedTimeCandidate> = emptyList(),
    val evidenceRating: String,
    val sources: List<CuratedSourceReference>,
    val editorialCommentary: String = "",
) {
    init {
        require(sourceId.isNotBlank() && name.isNotBlank()) { "名人资料缺少标识或姓名。" }
        require(groupName.isNotBlank()) { "名人资料缺少领域分类。" }
        require(timeZoneId.isNotBlank()) { "名人资料缺少出生地时区。" }
        require(longitude in -180.0..180.0 && latitude in -90.0..90.0) { "名人资料坐标无效。" }
        require(sources.isNotEmpty()) { "名人资料必须至少保留一个来源。" }
        require(evidenceRating in allowedEvidenceRatings) { "未知的出生资料等级：$evidenceRating。" }
    }
}

@Serializable
data class CuratedDate(val year: Int, val month: Int, val day: Int)

/**
 * 公开人物出生日期存在异说时，保留未被本条排盘采用的候选和可点击来源。
 * 不参与当前排盘，也不能被展示层误写成已确认的前三柱。
 */
@Serializable
data class CuratedDateCandidate(
    val label: String,
    val date: CuratedDate,
    val rationale: String,
    val sourceUrl: String,
) {
    init {
        require(label.isNotBlank() && rationale.isNotBlank()) { "日期候选必须说明依据。" }
        require(sourceUrl.startsWith("https://")) { "日期候选来源必须使用 HTTPS 地址。" }
    }
}

@Serializable
data class CuratedTimeCandidate(
    val label: String,
    val hour: Int,
    val minute: Int,
    val second: Int = 0,
    val precision: TimePrecision,
    val rationale: String,
) {
    init {
        require(label.isNotBlank() && label.length <= 60) { "时刻候选标签无效。" }
        require(hour in 0..23 && minute in 0..59 && second in 0..59) { "时刻候选无效。" }
        require(rationale.isNotBlank()) { "时刻候选必须说明依据。" }
    }
}

@Serializable
data class CuratedSourceReference(
    val title: String,
    val url: String,
    val publisher: String,
    val accessedAt: String,
    val claim: String,
) {
    init {
        require(title.isNotBlank() && publisher.isNotBlank() && claim.isNotBlank())
        require(url.startsWith("https://")) { "资料来源必须使用 HTTPS 地址。" }
        require(accessedAt.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "来源访问日期格式无效。" }
    }
}

data class CuratedCelebrityImportPreview(
    val caseCount: Int,
    val groupCount: Int,
    val ratedCount: Map<String, Int>,
    val alternativeTimeCaseCount: Int,
)

data class CuratedCelebrityImportProgress(
    val completed: Int,
    val total: Int,
)

data class CuratedCelebrityImportResult(
    val created: Int,
    val updated: Int,
    val skipped: Int,
    val invalid: Int,
    val errors: List<String>,
)

class CuratedCelebrityImporter(
    private val caseRepository: CaseRepository,
    private val baziEngine: BaziEngine,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = true
    }

    fun decode(raw: String): CuratedCelebrityCatalogPackage =
        json.decodeFromString(CuratedCelebrityCatalogPackage.serializer(), raw)

    fun preview(data: CuratedCelebrityCatalogPackage): CuratedCelebrityImportPreview =
        CuratedCelebrityImportPreview(
            caseCount = data.cases.size,
            groupCount = data.cases.map { it.groupName }.distinct().size,
            ratedCount = data.cases.groupingBy { it.evidenceRating }.eachCount(),
            alternativeTimeCaseCount = data.cases.count { it.alternativeTimes.isNotEmpty() },
        )

    suspend fun import(
        data: CuratedCelebrityCatalogPackage,
        onProgress: (CuratedCelebrityImportProgress) -> Unit = {},
    ): CuratedCelebrityImportResult = write(
        data = data,
        replaceManagedCatalogEntries = false,
        onProgress = onProgress,
    )

    /**
     * Installs the catalog packaged with the app. Existing managed entries are refreshed from
     * the package, while user cases, manual celebrity cases and historical imports are never
     * candidates for replacement.
     */
    suspend fun synchronizeBuiltInCatalog(
        data: CuratedCelebrityCatalogPackage,
        onProgress: (CuratedCelebrityImportProgress) -> Unit = {},
    ): CuratedCelebrityImportResult = write(
        data = data,
        replaceManagedCatalogEntries = true,
        onProgress = onProgress,
    )

    private suspend fun write(
        data: CuratedCelebrityCatalogPackage,
        replaceManagedCatalogEntries: Boolean,
        onProgress: (CuratedCelebrityImportProgress) -> Unit,
    ): CuratedCelebrityImportResult {
        val groups = ensureGroups(data.cases.map { it.groupName })
        var created = 0
        var updated = 0
        var skipped = 0
        var invalid = 0
        val errors = mutableListOf<String>()
        data.cases.forEachIndexed { index, source ->
            try {
                val caseId = curatedStableId("case", source.sourceId)
                val existing = caseRepository.findById(caseId)
                when {
                    existing == null -> {
                        val candidate = buildCase(source, requireNotNull(groups[source.groupName]))
                        when (caseRepository.save(candidate, expectedRevision = null)) {
                            is CaseWriteResult.Created -> created++
                            is CaseWriteResult.AlreadyExists -> skipped++
                            is CaseWriteResult.Updated,
                            is CaseWriteResult.RevisionConflict,
                            -> {
                                invalid++
                                errors += "${source.name}：发生非预期覆盖冲突。"
                            }
                        }
                    }
                    replaceManagedCatalogEntries && existing.isManagedCuratedCelebrity() -> {
                        val candidate = buildCase(source, requireNotNull(groups[source.groupName]))
                            .preservingUserPresentationState(existing)
                        when (caseRepository.save(candidate, expectedRevision = existing.revision)) {
                            is CaseWriteResult.Updated -> updated++
                            is CaseWriteResult.RevisionConflict -> {
                                invalid++
                                errors += "${source.name}：资料包更新与本地修改冲突，已保留本地记录。"
                            }
                            is CaseWriteResult.Created,
                            is CaseWriteResult.AlreadyExists,
                            -> {
                                invalid++
                                errors += "${source.name}：发生非预期写入结果。"
                            }
                        }
                    }
                    else -> skipped++
                }
            } catch (error: Exception) {
                invalid++
                errors += "${source.name}：${error.message ?: "导入失败"}"
            } finally {
                onProgress(CuratedCelebrityImportProgress(index + 1, data.cases.size))
            }
        }
        return CuratedCelebrityImportResult(created, updated, skipped, invalid, errors.take(20))
    }

    private suspend fun ensureGroups(names: List<String>): Map<String, CaseGroup> {
        val existing = caseRepository.listGroups(CaseLibraryType.CELEBRITY)
            .associateBy { it.name }
            .toMutableMap()
        names.map(String::trim).filter(String::isNotEmpty).distinct().forEach { name ->
            if (name !in existing) {
                caseRepository.createGroup(name, CaseLibraryType.CELEBRITY)?.let {
                    existing[name] = it
                }
            }
        }
        return existing
    }

    internal suspend fun buildCase(
        source: CuratedCelebrityCase,
        group: CaseGroup,
        caseId: String = curatedStableId("case", source.sourceId),
    ): BaziCase {
        val now = clock.instant()
        val timeCandidates = listOf(source.defaultTime) + source.alternativeTimes
        val builtCandidates = timeCandidates.mapIndexed { index, candidate ->
            val adopted = index == 0
            val candidateId = curatedStableId("time:$caseId", "${index}:${candidate.label}")
            val snapshotId = curatedStableId("snapshot:$caseId", candidateId)
            val calculation = baziEngine.calculate(
                source.toBirthInput(candidate),
                CalculationProfile.tymeDefault(),
            )
            val normalizedBirthInput = calculation.normalizedInput
            val snapshot = CaseCalculationSnapshot(
                id = snapshotId,
                result = calculation,
                birthTimeCandidateId = candidateId,
                adopted = adopted,
                createdAt = now,
            )
            CandidateAndSnapshot(
                candidate = BirthTimeCandidate(
                    id = candidateId,
                    label = candidate.label,
                    birthInput = normalizedBirthInput,
                    calculationSnapshotId = snapshotId,
                    adopted = adopted,
                    createdAt = now,
                ),
                snapshot = snapshot,
            )
        }
        val ownerFeedback = source.ownerFeedback()
        val records = buildList {
            add(
                CaseTextRecord(
                    id = curatedStableId("record:$caseId", "owner-feedback"),
                    type = CaseTextRecordType.OWNER_FEEDBACK,
                    content = ownerFeedback,
                    sourceType = TextRecordSourceType.CURATED_RESEARCH,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            source.editorialCommentary.trim().takeIf(String::isNotEmpty)?.let { commentary ->
                add(
                    CaseTextRecord(
                        id = curatedStableId("record:$caseId", "master-commentary"),
                        type = CaseTextRecordType.MASTER_COMMENTARY,
                        content = "【人物概览】\n$commentary",
                        sourceType = TextRecordSourceType.CURATED_RESEARCH,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
        }
        return BaziCase(
            id = caseId,
            alias = source.name.trim(),
            name = ExplicitText.present(source.name.trim()),
            sexForFortuneDirection = source.sex.toCuratedSex(),
            sourceType = CaseSourceType.CURATED_CELEBRITY_CATALOG,
            birthInput = builtCandidates.first().candidate.birthInput,
            libraryType = CaseLibraryType.CELEBRITY,
            birthTimeCandidates = builtCandidates.map { it.candidate },
            textRecords = records,
            calculationSnapshots = builtCandidates.map { it.snapshot },
            groups = listOf(group),
            tags = source.tags.map(String::trim).filter(String::isNotEmpty).distinct().map { tag ->
                CaseTag(curatedStableId("tag", tag), tag)
            },
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun CuratedCelebrityCase.toBirthInput(time: CuratedTimeCandidate): BirthInput = BirthInput(
        calendarInput = BirthCalendarInput.Solar(
            CivilDateTime(birthDate.year, birthDate.month, birthDate.day, time.hour, time.minute, time.second),
        ),
        sexForFortuneDirection = sex.toCuratedSex(),
        timePrecision = time.precision,
        timeZoneId = timeZoneId,
        locationName = birthPlace,
        longitude = longitude,
        latitude = latitude,
        coordinateSource = CoordinateSource.USER_ENTERED,
        useTrueSolarTime = false,
        timeSourceType = if (evidenceRating == "AA") {
            TimeSourceType.OFFICIAL_RECORD
        } else {
            TimeSourceType.OTHER_RECORD
        },
        sourceNote = sourceNote(time),
    )

    private fun CuratedCelebrityCase.ownerFeedback(): String = buildString {
        append("【资料可信度】${evidenceRating.toChineseEvidenceSummary()}\n")
        append("【出生地】$birthPlace\n")
        if (alternativeBirthDates.isNotEmpty()) {
            append("【日期说明】默认采用公历 ${birthDate.year}-${birthDate.month.toString().padStart(2, '0')}-${birthDate.day.toString().padStart(2, '0')} 仅用于本条排盘；其他公开日期候选：\n")
            alternativeBirthDates.forEach { candidate ->
                append("- ${candidate.label}：${candidate.date.year}-${candidate.date.month.toString().padStart(2, '0')}-${candidate.date.day.toString().padStart(2, '0')}；${candidate.rationale}\n${candidate.sourceUrl}\n")
            }
        }
        val hasOnlyDate = alternativeTimes.isEmpty() && defaultTime.isDateOnlyPlaceholder()
        val defaultLabel = if (hasOnlyDate) "暂定午时（排盘占位）" else defaultTime.label
        append("【默认采用】$defaultLabel（${timeDisplay(defaultTime)}）：${defaultTime.rationale}\n")
        if (hasOnlyDate) {
            append("【其他可能时辰】公开资料只核到日期、未载出生时段；除暂定午时外，")
            append(allHourPillars.filterNot { it == "午时" }.joinToString("、"))
            append("均可能。需要出生证明、家人回忆或可复核的早年事件资料，才能缩小范围；当前不把任何一项称为最可能时柱。")
        } else if (alternativeTimes.isEmpty()) {
            append("【其他可能时辰】当前无同源的其他候选。")
        } else {
            append("【其他可能时辰】\n")
            alternativeTimes.forEach { candidate ->
                append("- ${candidate.label}（${timeDisplay(candidate)}）：${candidate.rationale}\n")
            }
            append("以上候选均已分别复算；当前仅采用“默认采用”项，不代表其他候选被证伪。")
        }
        append("\n【来源】\n")
        sources.forEach { reference ->
            append("- ${reference.publisher.toChinesePublisher()}｜${reference.title}（${reference.accessedAt}）：${reference.claim}\n${reference.url}\n")
        }
    }.trim()

    private fun CuratedTimeCandidate.isDateOnlyPlaceholder(): Boolean =
        label.contains("未知时刻占位") &&
            hour == 12 && minute == 0 && precision == TimePrecision.APPROXIMATE

    private fun CuratedCelebrityCase.sourceNote(time: CuratedTimeCandidate): String = buildString {
        append("名人案例统一资料；资料可信度：${evidenceRating.toChineseEvidenceSummary()}；默认 ${timeDisplay(time)}。")
        alternativeBirthDates.takeIf(List<CuratedDateCandidate>::isNotEmpty)?.let {
            append("出生日期存在公开异说，当前只按默认日期排盘；详情见命主反馈。")
        }
        append(time.rationale)
        sources.firstOrNull()?.let { source ->
            append(" 来源：${source.publisher.toChinesePublisher()}，${source.url}")
        }
    }

    private data class CandidateAndSnapshot(
        val candidate: BirthTimeCandidate,
        val snapshot: CaseCalculationSnapshot,
    )
}

private fun BaziCase.isManagedCuratedCelebrity(): Boolean =
    libraryType == CaseLibraryType.CELEBRITY &&
        sourceType == CaseSourceType.CURATED_CELEBRITY_CATALOG

private fun BaziCase.preservingUserPresentationState(existing: BaziCase): BaziCase = copy(
    isFavorite = existing.isFavorite,
    isPinned = existing.isPinned,
    lastViewedAt = existing.lastViewedAt,
    deletedAt = existing.deletedAt,
)

private val allowedEvidenceRatings = setOf("AA", "A", "B", "C", "DD", "X")

/** 仅知公历日期时，十二时辰在逻辑上都尚未被排除；不是对其中任何一项的反推结论。 */
private val allHourPillars = listOf(
    "子时", "丑时", "寅时", "卯时", "辰时", "巳时",
    "午时", "未时", "申时", "酉时", "戌时", "亥时",
)

internal fun String.toChineseEvidenceSummary(): String = when (trim()) {
    "AA" -> "高（出生证明或出生登记）"
    "A" -> "较高（本人或近亲回忆）"
    "B" -> "中等（传记资料）"
    "C" -> "待核（校正或推定时刻）"
    "DD" -> "待核（来源冲突或未验证）"
    "X" -> "仅有生日（时刻未证实）"
    else -> "待核"
}

private fun String.toChinesePublisher(): String = when (trim()) {
    "Astro-Databank" -> "国际人物出生资料库"
    else -> trim()
}

private fun String.toCuratedSex(): SexForFortuneDirection = when (trim()) {
    "男", "MAN", "M" -> SexForFortuneDirection.MAN
    "女", "WOMAN", "F" -> SexForFortuneDirection.WOMAN
    else -> error("未知性别：$this")
}

private fun timeDisplay(time: CuratedTimeCandidate): String = "%02d:%02d:%02d".format(
    time.hour,
    time.minute,
    time.second,
)

private fun curatedStableId(namespace: String, value: String): String = UUID.nameUUIDFromBytes(
    "$namespace:$value".toByteArray(StandardCharsets.UTF_8),
).toString()
