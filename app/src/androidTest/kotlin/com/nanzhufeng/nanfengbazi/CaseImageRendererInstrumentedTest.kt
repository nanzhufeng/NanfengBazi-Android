package com.nanzhufeng.nanfengbazi

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nanzhufeng.nanfengbazi.domain.CaseImageExportErrorCode
import com.nanzhufeng.nanfengbazi.domain.CaseImageExportInput
import com.nanzhufeng.nanfengbazi.domain.CaseImageRenderResult
import com.nanzhufeng.nanfengbazi.domain.model.BaziCase
import com.nanzhufeng.nanfengbazi.domain.model.BirthCalendarInput
import com.nanzhufeng.nanfengbazi.domain.model.BirthInput
import com.nanzhufeng.nanfengbazi.domain.model.CalculationEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CalculationProfile
import com.nanzhufeng.nanfengbazi.domain.model.CalculationResult
import com.nanzhufeng.nanfengbazi.domain.model.CaseCalculationSnapshot
import com.nanzhufeng.nanfengbazi.domain.model.CaseSourceType
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.DecadeFortune
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.FortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.FortuneStart
import com.nanzhufeng.nanfengbazi.domain.model.FourPillars
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TimePrecision
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaseImageRendererInstrumentedTest {
    private val renderer = AndroidCaseImageRenderer(Dispatchers.Unconfined)

    @Test
    fun sameFactsProduceSameCompletePngAndChineseGlyphs() = runBlocking {
        val source = sampleCase("记录".repeat(1_200))

        val first = renderer.render(CaseImageExportInput(source)) as
            CaseImageRenderResult.Success
        val second = renderer.render(CaseImageExportInput(source)) as
            CaseImageRenderResult.Success

        assertArrayEquals(first.image.bytes, second.image.bytes)
        assertEquals(first.image.sha256, second.image.sha256)
        assertEquals(1080, first.image.widthPixels)
        assertTrue(first.image.heightPixels > 1_350)
        assertArrayEquals(
            byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47),
            first.image.bytes.copyOfRange(0, 4),
        )
        val bitmap = BitmapFactory.decodeByteArray(
            first.image.bytes,
            0,
            first.image.bytes.size,
        )
        assertEquals(first.image.widthPixels, bitmap.width)
        assertEquals(first.image.heightPixels, bitmap.height)
        bitmap.recycle()
        assertTrue(
            Paint().apply {
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }.hasGlyph("命"),
        )
    }

    @Test
    fun contentBeyondSingleLongImageLimitIsStructuredFailure() = runBlocking {
        val result = renderer.render(
            CaseImageExportInput(sampleCase("超长记录".repeat(40_000))),
        )

        assertEquals(
            CaseImageExportErrorCode.CONTENT_TOO_LARGE,
            (result as CaseImageRenderResult.Rejected).failure.code,
        )
    }

    private fun sampleCase(record: String): BaziCase {
        val input = BirthInput(
            calendarInput = BirthCalendarInput.Solar(
                CivilDateTime(2000, 2, 29, 10, 30, 0),
            ),
            sexForFortuneDirection = SexForFortuneDirection.MAN,
            timePrecision = TimePrecision.EXACT_TO_MINUTE,
            resolvedUtcOffsetSeconds = 28_800,
        )
        val result = CalculationResult(
            normalizedInput = input,
            profile = CalculationProfile.tymeDefault(),
            fourPillars = FourPillars("庚辰", "戊寅", "丁巳", "乙巳"),
            ownSign = "丁酉",
            bodySign = "己丑",
            fetalOrigin = "己巳",
            fetalBreath = "壬午",
            fortuneStart = FortuneStart(
                direction = FortuneDirection.FORWARD,
                startAt = CivilDateTime(2003, 6, 1, 0, 0, 0),
                endAt = CivilDateTime(2013, 6, 1, 0, 0, 0),
                years = 3,
                months = 3,
                days = 0,
                hours = 0,
                minutes = 0,
            ),
            decadeFortunes = listOf(
                DecadeFortune("己卯", 4, 13, 2003, 2012),
                DecadeFortune("庚辰", 14, 23, 2013, 2022),
            ),
            evidence = CalculationEvidence(
                engineName = "InstrumentedEngine",
                engineVersion = "1.0",
                ruleVersion = "instrumented-v1",
                calculatedAt = NOW,
            ),
        )
        return BaziCase(
            id = "case-image-renderer",
            alias = "合成导出命例",
            name = ExplicitText.present("字体核对"),
            sexForFortuneDirection = SexForFortuneDirection.MAN,
            sourceType = CaseSourceType.MANUAL,
            birthInput = input,
            textRecords = listOf(
                CaseTextRecord(
                    id = "record-long",
                    type = CaseTextRecordType.NOTE,
                    content = record,
                    createdAt = NOW,
                    updatedAt = NOW,
                ),
            ),
            calculationSnapshots = listOf(
                CaseCalculationSnapshot(
                    id = "snapshot-adopted",
                    result = result,
                    adopted = true,
                    createdAt = NOW,
                ),
            ),
            createdAt = NOW,
            updatedAt = NOW,
            revision = 2,
        )
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-07-31T00:00:00Z")
    }
}
