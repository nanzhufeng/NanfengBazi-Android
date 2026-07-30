package com.nanzhufeng.nanfengbazi

import com.nanzhufeng.nanfengbazi.domain.BaziEngine
import com.nanzhufeng.nanfengbazi.domain.CaseRepository
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
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
    fun validate(form: CaseFormState): CaseFormValidation {
        val alias = form.alias.trim()
        if (alias.isEmpty()) {
            return CaseFormValidation.Invalid("请填写命例别名。")
        }
        if (alias.length > 60) {
            return CaseFormValidation.Invalid("命例别名不能超过 60 个字符。")
        }
        val sex = form.sex
            ?: return CaseFormValidation.Invalid("请选择性别。")
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
        val dateTime = try {
            LocalDateTime.of(
                numbers[0],
                numbers[1],
                numbers[2],
                numbers[3],
                numbers[4],
                numbers[5],
            )
        } catch (_: DateTimeException) {
            return CaseFormValidation.Invalid("出生日期或时间无效，请检查年月日和时分秒。")
        }
        val birthInput = try {
            BirthInput(
                calendarInput = BirthCalendarInput.Solar(
                    CivilDateTime(
                        year = dateTime.year,
                        month = dateTime.monthValue,
                        day = dateTime.dayOfMonth,
                        hour = dateTime.hour,
                        minute = dateTime.minute,
                        second = dateTime.second,
                    ),
                ),
                sexForFortuneDirection = sex,
                timePrecision = if (dateTime.second == 0) {
                    TimePrecision.EXACT_TO_MINUTE
                } else {
                    TimePrecision.EXACT_TO_SECOND
                },
            )
        } catch (_: IllegalArgumentException) {
            return CaseFormValidation.Invalid("出生资料超出支持范围，请检查输入。")
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
    data class ValidationFailed(val message: String) : CreateCaseResult
    data class CalculationFailed(val message: String) : CreateCaseResult
    data class AlreadyExists(val caseId: String) : CreateCaseResult
    data class RevisionConflict(val caseId: String) : CreateCaseResult
    data class StorageFailed(val message: String) : CreateCaseResult
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
    suspend operator fun invoke(form: CaseFormState): CreateCaseResult {
        val valid = when (val validation = CaseFormValidator.validate(form)) {
            is CaseFormValidation.Invalid -> {
                return CreateCaseResult.ValidationFailed(validation.message)
            }
            is CaseFormValidation.Valid -> validation
        }
        val profile = CalculationProfile.tymeDefault()
        val calculation = try {
            baziEngine.calculate(valid.birthInput, profile)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return CreateCaseResult.CalculationFailed(
                if (error is IllegalArgumentException) {
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "当前出生资料或计算口径不受支持。"
                } else {
                    "计算引擎暂时无法完成排盘，请稍后重试。"
                },
            )
        }
        val now = clock.instant()
        val caseId = idGenerator.nextId()
        val case = BaziCase(
            id = caseId,
            alias = valid.alias,
            name = valid.name?.let(ExplicitText::present) ?: ExplicitText.absent(),
            sexForFortuneDirection = valid.birthInput.sexForFortuneDirection,
            sourceType = CaseSourceType.MANUAL,
            birthInput = valid.birthInput,
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
}
