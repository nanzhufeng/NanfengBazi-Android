package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.DuplicateCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.TimeZoneChoiceRequiredException
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CalendarSystem
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.CoordinateSource
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.LunarDateTime
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SolarTimeMode
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import java.time.Clock
import java.time.DateTimeException
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CancellationException

data class CaseFormState(
    val alias: String = "",
    val name: String = "",
    val sex: SexForFortuneDirection? = null,
    val year: String = "",
    val month: String = "",
    val day: String = "",
    val hour: String = "",
    val minute: String = "",
    val second: String = "0",
    val calendarSystem: CalendarSystem = CalendarSystem.SOLAR,
    val isLeapMonth: Boolean = false,
    val locationName: String = "",
    val longitude: String = "",
    val latitude: String = "",
    val timeZoneId: String = "Asia/Shanghai",
    val resolvedUtcOffsetSeconds: Int? = null,
    val availableUtcOffsetSeconds: List<Int> = emptyList(),
    val useTrueSolarTime: Boolean = false,
)

internal fun CaseFormState.clearTimeZoneResolution(): CaseFormState = copy(
    resolvedUtcOffsetSeconds = null,
    availableUtcOffsetSeconds = emptyList(),
)

sealed interface CaseFormValidation {
    data class Valid(
        val alias: String,
        val name: String?,
        val birthInput: BirthInput,
    ) : CaseFormValidation

    data class Invalid(val message: String) : CaseFormValidation
}

object CaseFormValidator {
    fun validate(
        form: CaseFormState,
        requireAlias: Boolean = true,
    ): CaseFormValidation {
        val alias = form.alias.trim()
        if (requireAlias && alias.isEmpty()) {
            return CaseFormValidation.Invalid("请填写命例别名。")
        }
        if (alias.length > 60) {
            return CaseFormValidation.Invalid("命例别名不能超过 60 个字符。")
        }
        val sex = form.sex
            ?: return CaseFormValidation.Invalid("请选择性别。")
        val locationName = form.locationName.trim()
        if (locationName.isEmpty()) {
            return CaseFormValidation.Invalid("请填写出生地区。")
        }
        if (locationName.length > 120) {
            return CaseFormValidation.Invalid("出生地区不能超过 120 个字符。")
        }
        val timeZoneId = form.timeZoneId.trim()
        if (timeZoneId.isEmpty()) {
            return CaseFormValidation.Invalid("请填写 IANA 时区。")
        }
        val longitudeRaw = form.longitude.trim()
        val latitudeRaw = form.latitude.trim()
        if ((longitudeRaw.isEmpty()) != (latitudeRaw.isEmpty())) {
            return CaseFormValidation.Invalid("经度和纬度必须同时填写或同时留空。")
        }
        val longitude = longitudeRaw.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
            ?: if (longitudeRaw.isNotEmpty()) {
                return CaseFormValidation.Invalid("经度必须是数字。")
            } else {
                null
            }
        val latitude = latitudeRaw.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
            ?: if (latitudeRaw.isNotEmpty()) {
                return CaseFormValidation.Invalid("纬度必须是数字。")
            } else {
                null
            }
        if (longitude != null && longitude !in -180.0..180.0) {
            return CaseFormValidation.Invalid("经度必须在 -180 到 180 之间。")
        }
        if (latitude != null && latitude !in -90.0..90.0) {
            return CaseFormValidation.Invalid("纬度必须在 -90 到 90 之间。")
        }
        if (form.useTrueSolarTime && (longitude == null || latitude == null)) {
            return CaseFormValidation.Invalid("启用真太阳时必须填写出生地经度和纬度。")
        }
        val values = listOf(
            "年份" to form.year,
            "月份" to form.month,
            "日期" to form.day,
            "小时" to form.hour,
            "分钟" to form.minute,
            "秒" to form.second,
        )
        val missing = values.firstOrNull { it.second.isBlank() }?.first
        if (missing != null) {
            return CaseFormValidation.Invalid("请填写$missing。")
        }
        val numbers = values.map { (label, raw) ->
            raw.toIntOrNull()
                ?: return CaseFormValidation.Invalid("${label}必须是数字。")
        }
        val birthInput = try {
            val calendarInput = when (form.calendarSystem) {
                CalendarSystem.SOLAR -> {
                    val dateTime = LocalDateTime.of(
                        numbers[0],
                        numbers[1],
                        numbers[2],
                        numbers[3],
                        numbers[4],
                        numbers[5],
                    )
                    BirthCalendarInput.Solar(
                        CivilDateTime(
                            year = dateTime.year,
                            month = dateTime.monthValue,
                            day = dateTime.dayOfMonth,
                            hour = dateTime.hour,
                            minute = dateTime.minute,
                            second = dateTime.second,
                        ),
                    )
                }
                CalendarSystem.LUNAR -> BirthCalendarInput.Lunar(
                    LunarDateTime(
                        year = numbers[0],
                        month = numbers[1],
                        day = numbers[2],
                        hour = numbers[3],
                        minute = numbers[4],
                        second = numbers[5],
                        isLeapMonth = form.isLeapMonth,
                    ),
                )
            }
            BirthInput(
                calendarInput = calendarInput,
                sexForFortuneDirection = sex,
                timePrecision = if (numbers[5] == 0) {
                    TimePrecision.EXACT_TO_MINUTE
                } else {
                    TimePrecision.EXACT_TO_SECOND
                },
                timeZoneId = timeZoneId,
                resolvedUtcOffsetSeconds = form.resolvedUtcOffsetSeconds,
                locationName = locationName,
                longitude = longitude,
                latitude = latitude,
                coordinateSource = if (longitude != null) {
                    CoordinateSource.USER_ENTERED
                } else {
                    null
                },
                useTrueSolarTime = form.useTrueSolarTime,
            )
        } catch (_: DateTimeException) {
            return CaseFormValidation.Invalid("出生日期或时间无效，请检查年月日和时分秒。")
        } catch (error: IllegalArgumentException) {
            return CaseFormValidation.Invalid(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "出生资料超出支持范围，请检查输入。",
            )
        }
        return CaseFormValidation.Valid(
            alias = alias,
            name = form.name.trim().takeIf { it.isNotEmpty() },
            birthInput = birthInput,
        )
    }
}

sealed interface CreateCaseResult {
    data class Created(val caseId: String) : CreateCaseResult
    data class DuplicateCandidates(
        val candidates: List<DuplicateCaseCandidate>,
    ) : CreateCaseResult
    data class TimeZoneChoiceRequired(
        val timeZoneId: String,
        val validUtcOffsetSeconds: List<Int>,
        val timeZoneDataVersion: String,
    ) : CreateCaseResult
    data class ValidationFailed(val message: String) : CreateCaseResult
    data class CalculationFailed(val message: String) : CreateCaseResult
    data class AlreadyExists(val caseId: String) : CreateCaseResult
    data class RevisionConflict(val caseId: String) : CreateCaseResult
    data class StorageFailed(val message: String) : CreateCaseResult
}

sealed interface PreviewCaseResult {
    data class Calculated(
        val alias: String,
        val name: String?,
        val calculation: CalculationResult,
    ) : PreviewCaseResult

    data class TimeZoneChoiceRequired(
        val timeZoneId: String,
        val validUtcOffsetSeconds: List<Int>,
        val timeZoneDataVersion: String,
    ) : PreviewCaseResult

    data class ValidationFailed(val message: String) : PreviewCaseResult
    data class CalculationFailed(val message: String) : PreviewCaseResult
}

fun interface IdGenerator {
    fun nextId(): String
}

class UuidGenerator : IdGenerator {
    override fun nextId(): String = UUID.randomUUID().toString()
}

class CreateCaseUseCase(
    private val baziEngine: BaziEngine,
    private val caseRepository: CaseRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: IdGenerator = UuidGenerator(),
) {
    suspend fun preview(form: CaseFormState): PreviewCaseResult =
        calculate(form = form, requireAlias = false)

    suspend operator fun invoke(
        form: CaseFormState,
        allowDuplicate: Boolean = false,
    ): CreateCaseResult {
        val prepared = when (val preview = calculate(form = form, requireAlias = true)) {
            is PreviewCaseResult.Calculated -> preview
            is PreviewCaseResult.ValidationFailed -> {
                return CreateCaseResult.ValidationFailed(preview.message)
            }
            is PreviewCaseResult.CalculationFailed -> {
                return CreateCaseResult.CalculationFailed(preview.message)
            }
            is PreviewCaseResult.TimeZoneChoiceRequired -> {
                return CreateCaseResult.TimeZoneChoiceRequired(
                    timeZoneId = preview.timeZoneId,
                    validUtcOffsetSeconds = preview.validUtcOffsetSeconds,
                    timeZoneDataVersion = preview.timeZoneDataVersion,
                )
            }
        }
        val calculation = prepared.calculation
        if (!allowDuplicate) {
            val candidates = try {
                caseRepository.findDuplicateCandidates(
                    birthInput = calculation.normalizedInput,
                    fourPillars = calculation.fourPillars,
                    canonicalSolarDateTime = calculation.calendarConversion?.solarDateTime,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return CreateCaseResult.StorageFailed(
                    "无法完成重复命例检查，未保存任何内容。输入仍保留，可稍后重试。",
                )
            }
            if (candidates.isNotEmpty()) {
                return CreateCaseResult.DuplicateCandidates(candidates)
            }
        }
        val now = clock.instant()
        val caseId = idGenerator.nextId()
        val case = BaziCase(
            id = caseId,
            alias = prepared.alias,
            name = prepared.name?.let(ExplicitText::present) ?: ExplicitText.absent(),
            sexForFortuneDirection = calculation.normalizedInput.sexForFortuneDirection,
            sourceType = CaseSourceType.MANUAL,
            birthInput = calculation.normalizedInput,
            calculationSnapshots = listOf(
                CaseCalculationSnapshot(
                    id = idGenerator.nextId(),
                    result = calculation,
                    adopted = true,
                    createdAt = now,
                ),
            ),
            createdAt = now,
            updatedAt = now,
        )
        return try {
            when (val write = caseRepository.save(case, expectedRevision = null)) {
                is CaseWriteResult.Created -> CreateCaseResult.Created(write.caseId)
                is CaseWriteResult.Updated -> CreateCaseResult.Created(write.caseId)
                is CaseWriteResult.AlreadyExists -> CreateCaseResult.AlreadyExists(write.caseId)
                is CaseWriteResult.RevisionConflict ->
                    CreateCaseResult.RevisionConflict(write.caseId)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            CreateCaseResult.StorageFailed(
                "命例未保存，数据库暂时不可用。请稍后重试；原始输入仍保留在当前页面。",
            )
        }
    }

    private suspend fun calculate(
        form: CaseFormState,
        requireAlias: Boolean,
    ): PreviewCaseResult {
        val valid = when (
            val validation = CaseFormValidator.validate(
                form = form,
                requireAlias = requireAlias,
            )
        ) {
            is CaseFormValidation.Invalid -> {
                return PreviewCaseResult.ValidationFailed(validation.message)
            }
            is CaseFormValidation.Valid -> validation
        }
        val profile = CalculationProfile.tymeDefault(
            solarTimeMode = if (valid.birthInput.useTrueSolarTime) {
                SolarTimeMode.TRUE_SOLAR_TIME
            } else {
                SolarTimeMode.CIVIL_TIME
            },
        )
        val calculation = try {
            baziEngine.calculate(valid.birthInput, profile)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: TimeZoneChoiceRequiredException) {
            return PreviewCaseResult.TimeZoneChoiceRequired(
                timeZoneId = error.timeZoneId,
                validUtcOffsetSeconds = error.validUtcOffsetSeconds,
                timeZoneDataVersion = error.timeZoneDataVersion,
            )
        } catch (error: Exception) {
            return PreviewCaseResult.CalculationFailed(
                if (error is IllegalArgumentException) {
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "当前出生资料或计算口径不受支持。"
                } else {
                    "计算引擎暂时无法完成排盘，请稍后重试。"
                },
            )
        }
        return PreviewCaseResult.Calculated(
            alias = valid.alias,
            name = valid.name,
            calculation = calculation,
        )
    }
}
