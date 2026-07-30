package com.nanzhufeng.nanfengbazi.data

import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CalculationWarning
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseFieldEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CaseGroup
import com.nanzhufeng.nanfengbazi.domain.model.CaseProfile
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTag
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.EventDatePrecision
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.FortuneStart
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.SourceAttachment
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import java.security.MessageDigest
import java.time.Instant

internal val FixtureInstant: Instant = Instant.parse("2026-07-30T04:00:00Z")

internal fun sampleCase(
    attachmentBytes: ByteArray = "脱敏截图夹具".encodeToByteArray(),
): BaziCase {
    val birthInput = BirthInput(
        calendarInput = BirthCalendarInput.Solar(
            CivilDateTime(1986, 5, 29, 13, 37, 0),
        ),
        sexForFortuneDirection = SexForFortuneDirection.MAN,
        timePrecision = TimePrecision.EXACT_TO_SECOND,
        locationName = "脱敏测试地区",
    )
    val profile = CalculationProfile.tymeDefault()
    val result = CalculationResult(
        normalizedInput = birthInput,
        profile = profile,
        fourPillars = FourPillars("丙寅", "癸巳", "癸酉", "己未"),
        ownSign = "癸巳",
        bodySign = "辛丑",
        fetalOrigin = "甲申",
        fetalBreath = "戊辰",
        fortuneStart = FortuneStart(
            direction = FortuneDirection.FORWARD,
            startAt = CivilDateTime(1986, 5, 29, 13, 37, 0),
            endAt = CivilDateTime(1990, 1, 1, 0, 0, 0),
            years = 3,
            months = 7,
            days = 2,
            hours = 10,
            minutes = 0,
        ),
        decadeFortunes = listOf(
            DecadeFortune("甲午", 4, 13, 1990, 1999),
        ),
        evidence = CalculationEvidence(
            engineName = "Tyme4j",
            engineVersion = "1.5.1",
            ruleVersion = "stage0-v1",
            calculatedAt = FixtureInstant,
        ),
        warnings = listOf(
            CalculationWarning("FIXTURE", "仅用于脱敏测试"),
        ),
    )
    val attachment = SourceAttachment(
        id = "attachment-1",
        relativePath = "case-1/source/screen.png",
        originalFileName = "脱敏测试截图.png",
        mimeType = "image/png",
        sha256 = sha256(attachmentBytes),
        byteSize = attachmentBytes.size.toLong(),
        createdAt = FixtureInstant,
    )
    return BaziCase(
        id = "case-1",
        alias = "脱敏案例一",
        name = ExplicitText.present("测试甲"),
        sexForFortuneDirection = SexForFortuneDirection.MAN,
        sourceType = CaseSourceType.WENZHEN_SCREENSHOT,
        birthInput = birthInput,
        profile = CaseProfile(
            occupation = ExplicitText.present("测试职业"),
            education = ExplicitText.absent(),
            finance = ExplicitText.cleared(),
            marriage = ExplicitText.present("测试婚姻记录"),
            health = ExplicitText.absent(),
        ),
        textRecords = listOf(
            CaseTextRecord(
                id = "record-feedback-1",
                type = CaseTextRecordType.OWNER_FEEDBACK,
                content = "1999 年：脱敏事件原文。",
                sourceAttachmentId = attachment.id,
                createdAt = FixtureInstant,
                updatedAt = FixtureInstant,
            ),
            CaseTextRecord(
                id = "record-commentary-1",
                type = CaseTextRecordType.MASTER_COMMENTARY,
                content = "脱敏师傅点评完整原文。",
                sourceAttachmentId = attachment.id,
                createdAt = FixtureInstant,
                updatedAt = FixtureInstant,
            ),
        ),
        events = listOf(
            CaseEvent(
                id = "event-1",
                year = 1999,
                datePrecision = EventDatePrecision.YEAR,
                stemBranch = "己卯",
                status = "已确认",
                rawText = "1999 年：脱敏事件原文。",
                normalizedText = ExplicitText.present("脱敏事件"),
                sourceAttachmentId = attachment.id,
                createdAt = FixtureInstant,
            ),
        ),
        calculationSnapshots = listOf(
            CaseCalculationSnapshot(
                id = "snapshot-1",
                result = result,
                adopted = true,
                createdAt = FixtureInstant,
            ),
        ),
        attachments = listOf(attachment),
        fieldEvidence = listOf(
            CaseFieldEvidence(
                id = "evidence-1",
                attachmentId = attachment.id,
                fieldKey = "occupation",
                rawText = "职业 测试职业",
                normalizedValue = TypedFieldValue.Text("测试职业"),
                adoptedValue = TypedFieldValue.Text("测试职业"),
                ocrConfidence = 0.98f,
                parserConfidence = 0.97f,
                consistencyConfidence = 1f,
                parserRuleId = "fixture-rule-v1",
                userEdited = false,
                createdAt = FixtureInstant,
            ),
        ),
        groups = listOf(CaseGroup("group-1", "脱敏分组")),
        tags = listOf(CaseTag("tag-1", "脱敏标签")),
        createdAt = FixtureInstant,
        updatedAt = FixtureInstant,
    )
}

internal fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
