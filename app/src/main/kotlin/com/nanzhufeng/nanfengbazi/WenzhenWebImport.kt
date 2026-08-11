package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CalculationWarning
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventTimelineLevel
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseLibraryType
import com.nanzhufeng.nanfengbazi.domain.model.CaseProfile
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.EventDatePrecision
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
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

@Serializable
data class WenzhenWebImportPackage(
    val format: String,
    val version: Int,
    val extractedAt: String,
    val userGroups: List<String>,
    val celebrityGroups: List<String>,
    val userCases: List<WenzhenUserCase>,
    val celebrityCases: List<WenzhenCelebrityCase>,
) {
    init {
        require(format == FORMAT) { "不是南枫八字支持的问真网页数据包。" }
        require(version == VERSION) { "问真网页数据包版本不受支持：$version。" }
    }

    companion object {
        const val FORMAT = "nanfeng-bazi-wenzhen-web-import"
        const val VERSION = 1
    }
}

@Serializable
data class WenzhenDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int = 0,
) {
    fun toDomain(): CivilDateTime = CivilDateTime(year, month, day, hour, minute, second)
    fun display(): String = "%04d-%02d-%02d %02d:%02d:%02d".format(
        year,
        month,
        day,
        hour,
        minute,
        second,
    )
}

@Serializable
data class WenzhenFourPillars(
    val year: String,
    val month: String,
    val day: String,
    val hour: String,
) {
    fun toDomain(): FourPillars = FourPillars(year, month, day, hour)
}

@Serializable
data class WenzhenProfile(
    val occupation: String = "",
    val education: String = "",
    val finance: String = "",
    val marriage: String = "",
    val health: String = "",
)

@Serializable
data class WenzhenTimelineEvent(
    val sourceId: String,
    val level: String,
    val year: Int,
    val stemBranch: String,
    val sourceLabel: String = "",
    val status: String = "",
    val content: String = "",
    val order: Int,
)

@Serializable
data class WenzhenUserCase(
    val sourceId: String,
    val name: String,
    val sex: String,
    val groupName: String? = null,
    val originalSolarTime: WenzhenDateTime,
    val adoptedSourceTime: WenzhenDateTime,
    val location: String = "",
    val fourPillars: WenzhenFourPillars,
    val profile: WenzhenProfile = WenzhenProfile(),
    val ownerFeedback: String = "",
    val masterCommentary: String = "",
    val timeline: List<WenzhenTimelineEvent> = emptyList(),
)

@Serializable
data class WenzhenCelebrityCase(
    val sourceId: String,
    val name: String,
    val sex: String,
    val groupName: String,
    val solarTime: WenzhenDateTime,
    val fourPillars: WenzhenFourPillars,
    val periodTag: String = "",
    val identityTag: String = "",
    val ownerFeedback: String = "",
    val masterCommentary: String = "",
    val timeline: List<WenzhenTimelineEvent> = emptyList(),
)

data class WenzhenImportPreview(
    val userCaseCount: Int,
    val celebrityCaseCount: Int,
    val userGroupCount: Int,
    val celebrityGroupCount: Int,
    val celebrityTagCount: Int,
)

data class WenzhenImportProgress(
    val completed: Int,
    val total: Int,
    val stage: String,
)

data class WenzhenImportResult(
    val created: Int,
    val skipped: Int,
    val invalid: Int,
    val userCreated: Int,
    val celebrityCreated: Int,
    val errors: List<String>,
)

class WenzhenWebImporter(
    private val caseRepository: CaseRepository,
    private val baziEngine: BaziEngine,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = true
    }

    fun decode(raw: String): WenzhenWebImportPackage =
        json.decodeFromString(WenzhenWebImportPackage.serializer(), raw)

    fun preview(data: WenzhenWebImportPackage): WenzhenImportPreview = WenzhenImportPreview(
        userCaseCount = data.userCases.size,
        celebrityCaseCount = data.celebrityCases.size,
        userGroupCount = data.userGroups.distinct().size,
        celebrityGroupCount = data.celebrityGroups.distinct().size,
        celebrityTagCount = data.celebrityCases
            .flatMap { listOf(it.periodTag, it.identityTag) }
            .filter { it.isNotBlank() }
            .distinct()
            .size,
    )

    suspend fun import(
        data: WenzhenWebImportPackage,
        onProgress: (WenzhenImportProgress) -> Unit = {},
    ): WenzhenImportResult {
        val userGroups = ensureGroups(data.userGroups, CaseLibraryType.USER)
        val celebrityGroups = ensureGroups(data.celebrityGroups, CaseLibraryType.CELEBRITY)
        val total = data.userCases.size + data.celebrityCases.size
        var completed = 0
        var created = 0
        var skipped = 0
        var invalid = 0
        var userCreated = 0
        var celebrityCreated = 0
        val errors = mutableListOf<String>()

        suspend fun persist(sourceId: String, libraryLabel: String, build: suspend () -> BaziCase) {
            try {
                val candidate = build()
                if (caseRepository.findById(candidate.id) != null) {
                    skipped++
                    return
                }
                when (caseRepository.save(candidate, expectedRevision = null)) {
                    is CaseWriteResult.Created -> {
                        created++
                        if (libraryLabel == "用户案例") userCreated++ else celebrityCreated++
                    }
                    is CaseWriteResult.AlreadyExists -> skipped++
                    is CaseWriteResult.Updated,
                    is CaseWriteResult.RevisionConflict,
                    -> {
                        invalid++
                        errors += "$libraryLabel $sourceId：发生非预期覆盖冲突。"
                    }
                }
            } catch (error: Exception) {
                invalid++
                errors += "$libraryLabel $sourceId：${error.message ?: "导入失败"}"
            } finally {
                completed++
                onProgress(WenzhenImportProgress(completed, total, libraryLabel))
            }
        }

        data.userCases.forEach { source ->
            persist(source.sourceId, "用户案例") {
                buildUserCase(source, userGroups[source.groupName])
            }
        }
        data.celebrityCases.forEach { source ->
            persist(source.sourceId, "名人案例") {
                buildCelebrityCase(
                    source,
                    requireNotNull(celebrityGroups[source.groupName]) {
                        "名人分类不存在：${source.groupName}"
                    },
                )
            }
        }
        return WenzhenImportResult(
            created = created,
            skipped = skipped,
            invalid = invalid,
            userCreated = userCreated,
            celebrityCreated = celebrityCreated,
            errors = errors.take(20),
        )
    }

    private suspend fun ensureGroups(
        names: List<String>,
        libraryType: CaseLibraryType,
    ): Map<String, CaseGroup> {
        val existing = caseRepository.listGroups(libraryType).associateBy { it.name }.toMutableMap()
        names.map(String::trim).filter(String::isNotEmpty).distinct().forEach { name ->
            if (name !in existing) {
                caseRepository.createGroup(name, libraryType)?.let { existing[name] = it }
            }
        }
        return existing
    }

    private suspend fun buildUserCase(source: WenzhenUserCase, group: CaseGroup?): BaziCase {
        val birthInput = source.adoptedSourceTime.toBirthInput(
            sex = source.sex.toSex(),
            location = source.location,
            sourceNote = buildString {
                append("问真网页原始阳历：${source.originalSolarTime.display()}；")
                append("问真来源真太阳时：${source.adoptedSourceTime.display()}。")
                append("问真来源四柱：${source.fourPillars.toDomain().compact()}。")
                append("为一比一保持来源四柱，本命例采用来源真太阳时作为计算输入，未重复校正。")
            },
        )
        val calculation = validatedCalculation(birthInput, source.fourPillars.toDomain())
        val now = importInstant()
        val textRecords = buildList {
            source.ownerFeedback.trim().takeIf(String::isNotEmpty)?.let { content ->
                add(sourceRecord(source.sourceId, "owner-feedback", CaseTextRecordType.OWNER_FEEDBACK, content, now))
            }
            source.masterCommentary.trim().takeIf(String::isNotEmpty)?.let { content ->
                add(sourceRecord(source.sourceId, "master-commentary", CaseTextRecordType.MASTER_COMMENTARY, content, now))
            }
        }
        return BaziCase(
            id = stableId("user-case", source.sourceId),
            alias = source.name.trim().ifBlank { "未命名案例" },
            name = source.name.toExplicit(),
            sexForFortuneDirection = source.sex.toSex(),
            sourceType = CaseSourceType.WENZHEN_WEB_IMPORT,
            birthInput = birthInput,
            libraryType = CaseLibraryType.USER,
            profile = CaseProfile(
                occupation = source.profile.occupation.toExplicit(),
                education = source.profile.education.toExplicit(),
                finance = source.profile.finance.toExplicit(),
                marriage = source.profile.marriage.toExplicit(),
                health = source.profile.health.toExplicit(),
            ),
            textRecords = textRecords,
            events = source.timeline.sortedBy { it.order }.map { event ->
                CaseEvent(
                    id = stableId("user-event:${source.sourceId}", event.sourceId),
                    category = CaseEventCategory.GENERAL,
                    title = event.sourceLabel.trim().ifBlank { null },
                    year = event.year,
                    datePrecision = EventDatePrecision.YEAR,
                    stemBranch = event.stemBranch.trim(),
                    timelineLevel = when (event.level) {
                        "DECADE" -> CaseEventTimelineLevel.DECADE
                        "ANNUAL" -> CaseEventTimelineLevel.ANNUAL
                        else -> error("未知时间线级别：${event.level}")
                    },
                    status = event.status.trim().ifBlank { null },
                    rawText = event.content.trim(),
                    createdAt = now,
                )
            },
            calculationSnapshots = listOf(
                CaseCalculationSnapshot(
                    id = stableId("user-calculation", source.sourceId),
                    result = calculation,
                    adopted = true,
                    createdAt = now,
                ),
            ),
            groups = listOfNotNull(group),
            createdAt = now,
            updatedAt = now,
        )
    }

    private suspend fun buildCelebrityCase(
        source: WenzhenCelebrityCase,
        group: CaseGroup,
    ): BaziCase {
        val sex = source.sex.toSex()
        val birthInput = source.solarTime.toBirthInput(
            sex = sex,
            location = "",
            sourceNote = "问真网页名人案例；来源阳历：${source.solarTime.display()}；" +
                "来源四柱：${source.fourPillars.toDomain().compact()}。",
        )
        val calculation = validatedCalculation(birthInput, source.fourPillars.toDomain())
        val now = importInstant()
        val tags = listOf(source.periodTag, source.identityTag)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .map { CaseTag(stableId("celebrity-tag", it), it) }
        val textRecords = buildList {
            source.ownerFeedback.trim().takeIf(String::isNotEmpty)?.let { content ->
                add(
                    sourceRecord(
                        source.sourceId,
                        "owner-feedback",
                        CaseTextRecordType.OWNER_FEEDBACK,
                        content,
                        now,
                    ),
                )
            }
            source.masterCommentary.trim().takeIf(String::isNotEmpty)?.let { content ->
                add(
                    sourceRecord(
                        source.sourceId,
                        "master-commentary",
                        CaseTextRecordType.MASTER_COMMENTARY,
                        content,
                        now,
                    ),
                )
            }
        }
        return BaziCase(
            id = stableId("celebrity-case", source.sourceId),
            alias = source.name.trim().ifBlank { "未命名名人" },
            name = source.name.toExplicit(),
            sexForFortuneDirection = sex,
            sourceType = CaseSourceType.WENZHEN_WEB_IMPORT,
            birthInput = birthInput,
            libraryType = CaseLibraryType.CELEBRITY,
            textRecords = textRecords,
            events = source.timeline.sortedBy { it.order }.map { event ->
                CaseEvent(
                    id = stableId("celebrity-event:${source.sourceId}", event.sourceId),
                    title = event.sourceLabel.trim().ifBlank { null },
                    category = CaseEventCategory.GENERAL,
                    year = event.year,
                    datePrecision = EventDatePrecision.YEAR,
                    stemBranch = event.stemBranch.trim(),
                    timelineLevel = when (event.level) {
                        "DECADE" -> CaseEventTimelineLevel.DECADE
                        "ANNUAL" -> CaseEventTimelineLevel.ANNUAL
                        else -> error("未知时间线级别：${event.level}")
                    },
                    status = event.status.trim().ifBlank { null },
                    rawText = event.content.trim(),
                    createdAt = now,
                )
            },
            calculationSnapshots = listOf(
                CaseCalculationSnapshot(
                    id = stableId("celebrity-calculation", source.sourceId),
                    result = calculation,
                    adopted = true,
                    createdAt = now,
                ),
            ),
            groups = listOf(group),
            tags = tags,
            createdAt = now,
            updatedAt = now,
        )
    }

    private suspend fun validatedCalculation(
        birthInput: BirthInput,
        sourcePillars: FourPillars,
    ) = baziEngine.calculate(birthInput, CalculationProfile.tymeDefault()).let { result ->
        when {
            result.fourPillars == sourcePillars -> result
            !sourcePillars.isStructurallyValid() -> result.copy(
                warnings = result.warnings + CalculationWarning(
                    code = "WENZHEN_INVALID_SOURCE_PILLARS_RECALCULATED",
                    message = "问真来源四柱含无效干支，已按来源阳历时间使用本地引擎复算。",
                ),
            )
            else -> result.copy(
                fourPillars = sourcePillars,
                basicChartDetails = null,
                warnings = result.warnings + CalculationWarning(
                    code = "WENZHEN_SOURCE_PILLARS_ADOPTED",
                    message = "问真来源四柱与本地复算不一致，已按迁移规则保留来源四柱。",
                ),
            )
        }
    }

    private fun WenzhenDateTime.toBirthInput(
        sex: SexForFortuneDirection,
        location: String,
        sourceNote: String,
    ) = BirthInput(
        calendarInput = BirthCalendarInput.Solar(toDomain()),
        sexForFortuneDirection = sex,
        timePrecision = if (second == 0) {
            TimePrecision.EXACT_TO_MINUTE
        } else {
            TimePrecision.EXACT_TO_SECOND
        },
        locationName = location.trim().ifBlank { null },
        useTrueSolarTime = false,
        timeSourceType = TimeSourceType.WENZHEN_WEB_IMPORT,
        sourceNote = sourceNote,
    )

    private fun sourceRecord(
        caseSourceId: String,
        kind: String,
        type: CaseTextRecordType,
        content: String,
        now: Instant,
    ) = CaseTextRecord(
        id = stableId("user-record:$caseSourceId", kind),
        type = type,
        content = content,
        sourceType = TextRecordSourceType.WENZHEN_WEB_IMPORT,
        createdAt = now,
        updatedAt = now,
    )

    private fun importInstant(): Instant = clock.instant()
}

private fun String.toSex(): SexForFortuneDirection = when (trim()) {
    "男", "MAN", "1" -> SexForFortuneDirection.MAN
    "女", "WOMAN", "0" -> SexForFortuneDirection.WOMAN
    else -> error("未知性别：$this")
}

private fun String.toExplicit(): ExplicitText = trim().takeIf(String::isNotEmpty)
    ?.let(ExplicitText::present)
    ?: ExplicitText.absent()

private fun stableId(namespace: String, value: String): String = UUID.nameUUIDFromBytes(
    "$namespace:$value".toByteArray(StandardCharsets.UTF_8),
).toString()

private fun FourPillars.compact(): String = "$year$month$day$hour"

private fun FourPillars.isStructurallyValid(): Boolean {
    val stems = "甲乙丙丁戊己庚辛壬癸"
    val branches = "子丑寅卯辰巳午未申酉戌亥"
    return listOf(year, month, day, hour).all { pillar ->
        val chars = pillar.toList()
        chars.size == 2 && chars[0] in stems && chars[1] in branches
    }
}
