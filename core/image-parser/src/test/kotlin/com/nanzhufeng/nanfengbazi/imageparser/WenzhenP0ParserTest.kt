package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.model.CaseFieldEvidence
import com.nanzhufeng.nanfengbazi.domain.model.CivilDateTime
import com.nanzhufeng.nanfengbazi.domain.model.EvidenceBoundingBox
import com.nanzhufeng.nanfengbazi.domain.model.ImportCaseCandidate
import com.nanzhufeng.nanfengbazi.domain.model.ImportImageRef
import com.nanzhufeng.nanfengbazi.domain.model.ImportedLongTextType
import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.OcrTextBlock
import com.nanzhufeng.nanfengbazi.domain.model.TypedFieldValue
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WenzhenP0ParserTest {
    private val parser = WenzhenP0Parser()

    @Test
    fun `用户列表一张图拆出多条命例且字段等待人工采用`() {
        val image = image("user-list", WenzhenPageType.USER_LIST)
        val document = document(
            imageId = image.id,
            blocks = listOf(
                block("name-1", "案例甲 男", 30, 100, 210, 140),
                block("stems-1", "壬戊壬丙", 560, 105, 760, 125),
                block("date-1", "阳历1992年8月24日 申申申午", 30, 160, 760, 195),
                block("name-2", "案例乙 女", 30, 300, 210, 340),
                block("stems-2", "庾癸乙甲", 560, 305, 760, 325),
                block("branches-2", "辰末未中", 560, 330, 760, 350),
                block("date-2", "阳历2000年8月5日", 30, 360, 300, 395),
            ),
        )

        val result = parser.parse(
            images = listOf(image),
            documents = listOf(document),
            groupedCandidates = listOf(candidate(image.id)),
        )

        assertEquals(2, result.candidates.size)
        assertEquals(listOf("案例甲", "案例乙"), result.candidates.map { it.suggestedAlias })
        assertEquals(8, result.fields.size)
        assertTrue(result.fields.all { it.adoptedValue == null && !it.userEdited })
        assertTrue(result.candidates.all { it.requiresReview && it.fieldEvidenceIds.size == 4 })
        val secondFields = result.fields.filter {
            it.id in result.candidates.last().fieldEvidenceIds
        }
        assertEquals(
            null,
            secondFields.single { it.fieldKey == "chart.four_pillars" }.normalizedValue,
        )

        val firstFields = result.fields.filter { it.id in result.candidates.first().fieldEvidenceIds }
        assertEquals(
            "1992-08-24",
            (firstFields.single { it.fieldKey == "birth.solar_date" }.normalizedValue as
                TypedFieldValue.Text).value,
        )
        assertEquals(
            listOf("壬申", "戊申", "壬申", "丙午"),
            (firstFields.single { it.fieldKey == "chart.four_pillars" }.normalizedValue as
                TypedFieldValue.FourPillarsValue).value.let {
                listOf(it.year, it.month, it.day, it.hour)
            },
        )
    }

    @Test
    fun `命主反馈和师傅点评完整保留OCR原文并关联同一候选`() {
        val feedback = image("feedback", WenzhenPageType.FEEDBACK)
        val commentary = image("commentary", WenzhenPageType.COMMENTARY)
        val feedbackText = "姓名：案例甲\n阳历1992年8月24日\n命主反馈\n第一段。\n第二段保留。"
        val commentaryText = "姓名：案例甲\n公历1992-08-24\n师傅点评\n原文不可截断。"

        val result = parser.parse(
            images = listOf(feedback, commentary),
            documents = listOf(
                document(feedback.id, feedbackText),
                document(commentary.id, commentaryText),
            ),
            groupedCandidates = listOf(
                ImportCaseCandidate(
                    id = "grouped",
                    imageIds = listOf(feedback.id, commentary.id),
                    suggestedAlias = "案例甲",
                    groupingConfidence = 0.84f,
                    requiresReview = true,
                ),
            ),
        )

        assertEquals(2, result.longTexts.size)
        assertEquals(
            setOf(ImportedLongTextType.OWNER_FEEDBACK, ImportedLongTextType.MASTER_COMMENTARY),
            result.longTexts.mapTo(mutableSetOf()) { it.type },
        )
        assertEquals(feedbackText, result.longTexts.single { it.imageId == feedback.id }.rawText)
        assertEquals(
            commentaryText,
            result.longTexts.single { it.imageId == commentary.id }.rawText,
        )
        assertEquals(
            result.longTexts.map { it.id }.toSet(),
            result.candidates.single().longTextEvidenceIds.toSet(),
        )
        assertTrue(result.fields.all { it.adoptedValue == null })
    }

    @Test
    fun `基本资料页提取出生资料并为每项保留原图区域`() {
        val image = image("basic-info", WenzhenPageType.BASIC_INFO)
        val document = document(
            imageId = image.id,
            blocks = listOf(
                block("identity", "姓名：席瑞 性别：男", 24, 80, 420, 120),
                block("lunar", "农历：1992年七月廿六 午时 乾造", 24, 130, 560, 170),
                block("solar", "阳历：1992年08月24日 12:00:00", 24, 180, 560, 220),
                block("true-solar", "真太阳时：1992-08-24 11:53:00", 24, 230, 560, 270),
                block("location", "出生地区：江苏省宿迁市泗阳县", 24, 280, 560, 320),
                block("coordinates", "地址经纬：北纬33.72 东经118.68", 24, 330, 560, 370),
                block("identity-tags", "星座：处女座(Virgo) 属相：猴", 24, 380, 560, 420),
                block("pillars", "壬申 戊申 壬申 丙午", 24, 430, 560, 470),
            ),
        )

        val result = parser.parse(
            images = listOf(image),
            documents = listOf(document),
            groupedCandidates = listOf(candidate(image.id)),
        )

        val fieldsByKey = result.fields.associateBy { it.fieldKey }
        assertEquals(
            setOf(
                "identity.alias",
                "identity.name",
                "identity.sex",
                "identity.constellation",
                "identity.zodiac",
                "birth.solar_date",
                "birth.solar_datetime",
                "birth.lunar_text",
                "birth.true_solar_datetime",
                "birth.location",
                "birth.latitude",
                "birth.longitude",
                "chart.four_pillars",
            ),
            fieldsByKey.keys,
        )
        assertEquals(
            "席瑞",
            (fieldsByKey.getValue("identity.name").normalizedValue as
                TypedFieldValue.Text).value,
        )
        assertEquals(
            12,
            (fieldsByKey.getValue("birth.solar_datetime").normalizedValue as
                TypedFieldValue.DateTimeValue).value.hour,
        )
        assertEquals(
            11,
            (fieldsByKey.getValue("birth.true_solar_datetime").normalizedValue as
                TypedFieldValue.DateTimeValue).value.hour,
        )
        assertEquals(
            "33.72",
            (fieldsByKey.getValue("birth.latitude").normalizedValue as
                TypedFieldValue.DecimalNumber).canonicalValue,
        )
        assertEquals(
            "118.68",
            (fieldsByKey.getValue("birth.longitude").normalizedValue as
                TypedFieldValue.DecimalNumber).canonicalValue,
        )
        assertTrue(result.fields.all { it.boundingBox != null && it.adoptedValue == null })
        assertEquals(result.fields.map { it.id }.toSet(), result.candidates.single().fieldEvidenceIds.toSet())
    }

    @Test
    fun `命主反馈按年份拆成默认不采用的事件候选`() {
        val image = image("feedback-events", WenzhenPageType.FEEDBACK)
        val document = document(
            imageId = image.id,
            blocks = listOf(
                block("name", "姓名：案例甲", 24, 60, 280, 100),
                block("date", "阳历：1992年8月24日", 24, 110, 380, 150),
                block("event-1999", "1999年 己卯", 24, 220, 260, 260),
                block("event-1999-body", "请嫁入，后续待核对。", 48, 270, 620, 320),
                block("event-2001", "2001年 辛巳 已经进入三级单位", 24, 360, 720, 410),
                block("event-2001-body", "这是第二行完整反馈。", 48, 420, 620, 470),
            ),
        )

        val result = parser.parse(
            images = listOf(image),
            documents = listOf(document),
            groupedCandidates = listOf(candidate(image.id)),
        )

        val eventFields = result.fields.filter {
            it.fieldKey.startsWith("event.candidate.")
        }
        assertEquals(2, eventFields.size)
        assertEquals(
            listOf("event.candidate.1999.0", "event.candidate.2001.1"),
            eventFields.map { it.fieldKey },
        )
        assertEquals(
            "请嫁入，后续待核对。",
            (eventFields.first().normalizedValue as TypedFieldValue.Text).value,
        )
        assertEquals(
            "已经进入三级单位\n这是第二行完整反馈。",
            (eventFields.last().normalizedValue as TypedFieldValue.Text).value,
        )
        assertTrue(eventFields.all { it.adoptedValue == null && it.boundingBox != null })
        assertTrue(
            result.candidates.single().fieldEvidenceIds.containsAll(
                eventFields.map { it.id },
            ),
        )
        assertEquals(
            document.rawText,
            result.longTexts.single().rawText,
        )
    }

    @Test
    fun `基本排盘表按四柱拆出可定位且默认不采用的结构化证据`() {
        val image = image("basic-chart", WenzhenPageType.BASIC_CHART)
        val document = document(
            imageId = image.id,
            blocks = listOf(
                block("pillars", "壬申 戊申 壬申 丙午", 110, 70, 720, 110),
                block("main-star", "主星比肩七杀元男偏财", 20, 130, 720, 170),
                block("hidden-label", "藏干", 20, 190, 90, 230),
                block("hidden-year", "庚金\n壬水\n戊土", 120, 190, 220, 280),
                block("hidden-month", "庚金\n壬水\n戊土", 260, 190, 360, 280),
                block("hidden-day", "庚金\n壬水\n戊土", 400, 190, 500, 280),
                block("hidden-hour", "丁火\n己土", 550, 190, 650, 260),
                block("fortune", "星运长生长生长生胎", 20, 300, 720, 340),
                block("self", "自坐 长生 病 长生 帝旺", 20, 360, 720, 400),
                block("void", "空亡 戌亥 寅卯 戌亥 寅卯", 20, 420, 720, 460),
                block("nayin", "纳音 剑锋金 大驿土 剑锋金 天河水", 20, 480, 720, 520),
                block("terminator", "原局天干：丙壬相冲", 20, 560, 720, 600),
            ),
        )

        val result = parser.parse(
            images = listOf(image),
            documents = listOf(document),
            groupedCandidates = listOf(candidate(image.id)),
        )

        val chartFields = result.fields.filter {
            it.fieldKey.startsWith("chart.") && it.fieldKey != "chart.four_pillars"
        }
        assertEquals(24, chartFields.size)
        assertEquals(
            "比肩",
            (chartFields.single { it.fieldKey == "chart.year.main_star" }.normalizedValue as
                TypedFieldValue.Text).value,
        )
        assertEquals(
            "庚金\n壬水\n戊土",
            (chartFields.single { it.fieldKey == "chart.month.hidden_stems" }.normalizedValue as
                TypedFieldValue.Text).value,
        )
        assertEquals(
            "天河水",
            (chartFields.single { it.fieldKey == "chart.hour.nayin" }.normalizedValue as
                TypedFieldValue.Text).value,
        )
        assertTrue(chartFields.all { it.adoptedValue == null && it.boundingBox != null })
        assertTrue(
            result.candidates.single().fieldEvidenceIds.containsAll(
                chartFields.map { it.id },
            ),
        )
    }

    @Test
    fun `专业细盘按九列提取干支和观察时刻且默认只作为来源证据`() {
        val image = image("professional-chart", WenzhenPageType.PROFESSIONAL_CHART)
        val labels = listOf(
            "流时",
            "流日",
            "流月",
            "流年",
            "大运",
            "年柱",
            "月柱",
            "日柱",
            "时柱",
        )
        val pillars = listOf(
            "丙子",
            "乙未",
            "乙未",
            "丙午",
            "辛亥",
            "壬申",
            "戊申",
            "壬申",
            "丙午",
        )
        val blocks = buildList {
            labels.forEachIndexed { index, label ->
                val left = 20 + index * 80
                add(block("header-$index", label, left, 80, left + 58, 112))
                add(block("star-$index", "偏财", left, 125, left + 58, 153))
                add(block("stem-$index", pillars[index].take(1), left, 165, left + 58, 195))
                add(block("branch-$index", pillars[index].takeLast(1), left, 205, left + 58, 235))
            }
            add(
                block(
                    "observed",
                    "已选日期：2026年7月30日 星期四 子时",
                    20,
                    520,
                    600,
                    560,
                ),
            )
        }
        val document = document(image.id, blocks)

        val result = parser.parse(
            images = listOf(image),
            documents = listOf(document),
            groupedCandidates = listOf(candidate(image.id)),
        )

        val professional = result.fields.filter { it.fieldKey.startsWith("professional.") }
        assertEquals(10, professional.size)
        assertEquals(
            CivilDateTime(2026, 7, 30, 23, 0, 0),
            (professional.single {
                it.fieldKey == "professional.observed_at"
            }.normalizedValue as TypedFieldValue.DateTimeValue).value,
        )
        listOf(
            "flow_hour",
            "flow_day",
            "flow_month",
            "flow_year",
            "decade",
            "natal_year",
            "natal_month",
            "natal_day",
            "natal_hour",
        ).forEachIndexed { index, key ->
            assertEquals(
                pillars[index],
                (professional.single {
                    it.fieldKey == "professional.$key"
                }.normalizedValue as TypedFieldValue.Text).value,
            )
        }
        assertTrue(professional.all { it.adoptedValue == null && it.boundingBox != null })
        assertTrue(
            result.candidates.single().fieldEvidenceIds.containsAll(
                professional.map(CaseFieldEvidence::id),
            ),
        )
    }

    @Test
    fun `专业细盘九列表头不完整时不推断错位干支`() {
        val image = image("incomplete-professional-chart", WenzhenPageType.PROFESSIONAL_CHART)
        val document = document(
            imageId = image.id,
            blocks = listOf(
                block("flow-hour", "流时", 20, 80, 78, 112),
                block("flow-day", "流日", 100, 80, 158, 112),
                block("flow-year", "流年", 260, 80, 318, 112),
                block("stem", "丙", 20, 165, 78, 195),
                block("branch", "子", 20, 205, 78, 235),
                block(
                    "observed",
                    "已选日期：2026年7月30日 23:00",
                    20,
                    325,
                    520,
                    360,
                ),
            ),
        )

        val result = parser.parse(
            images = listOf(image),
            documents = listOf(document),
            groupedCandidates = listOf(candidate(image.id)),
        )

        val professional = result.fields.filter { it.fieldKey.startsWith("professional.") }
        assertEquals(listOf("professional.observed_at"), professional.map { it.fieldKey })
        assertEquals(
            CivilDateTime(2026, 7, 30, 23, 0, 0),
            (professional.single().normalizedValue as TypedFieldValue.DateTimeValue).value,
        )
    }

    @Test
    fun `专业细盘表头被OCR合并为一行时仍按空间拆回九列`() {
        val image = image("merged-professional-chart", WenzhenPageType.PROFESSIONAL_CHART)
        val labels = "流时流日流月流年大运年柱月柱日杜时柱"
        val pillars = listOf("丙子", "乙未", "乙未", "丙午", "辛亥", "壬申", "戊申", "壬申", "丙午")
        val blocks = buildList {
            add(block("merged-headers", labels, 20, 80, 920, 120))
            add(
                block(
                    "merged-stems",
                    pillars.joinToString("") { it.take(1) },
                    20,
                    165,
                    920,
                    195,
                ),
            )
            add(
                block(
                    "merged-branches",
                    pillars.joinToString("") { it.takeLast(1) },
                    20,
                    205,
                    920,
                    235,
                ),
            )
            add(
                block(
                    "observed-ocr-confusion",
                    "已迷日期：2026年7月30日 23:00",
                    20,
                    300,
                    520,
                    340,
                ),
            )
        }

        val result = parser.parse(
            images = listOf(image),
            documents = listOf(document(image.id, blocks)),
            groupedCandidates = listOf(candidate(image.id)),
        )

        val professional = result.fields.filter { it.fieldKey.startsWith("professional.") }
        assertEquals(10, professional.size)
        assertEquals(
            CivilDateTime(2026, 7, 30, 23, 0, 0),
            (professional.single {
                it.fieldKey == "professional.observed_at"
            }.normalizedValue as TypedFieldValue.DateTimeValue).value,
        )
        assertEquals(
            pillars,
            listOf(
                "flow_hour",
                "flow_day",
                "flow_month",
                "flow_year",
                "decade",
                "natal_year",
                "natal_month",
                "natal_day",
                "natal_hour",
            ).map { key ->
                (professional.single {
                    it.fieldKey == "professional.$key"
                }.normalizedValue as TypedFieldValue.Text).value
            },
        )
    }

    @Test
    fun `无法识别用户列表行时保留原待核对候选而不制造空命例`() {
        val image = image("unreadable-list", WenzhenPageType.USER_LIST)
        val original = candidate(image.id)

        val result = parser.parse(
            images = listOf(image),
            documents = listOf(document(image.id, "用户列表\n局部文字")),
            groupedCandidates = listOf(original),
        )

        assertEquals(listOf(original), result.candidates)
        assertTrue(result.fields.isEmpty())
        assertTrue(result.longTexts.isEmpty())
        assertNull(result.candidates.single().suggestedAlias)
    }

    private fun image(id: String, pageType: WenzhenPageType) = ImportImageRef(
        id = id,
        originalFileName = "$id.png",
        mimeType = "image/png",
        relativePath = "session/$id.png",
        sha256 = id.first().code.toString(16).padStart(64, '0').takeLast(64),
        byteSize = 1,
        pageType = pageType,
        pageConfidence = 0.95f,
        createdAt = NOW,
    )

    private fun candidate(imageId: String) = ImportCaseCandidate(
        id = "candidate-$imageId",
        imageIds = listOf(imageId),
        groupingConfidence = null,
        requiresReview = true,
    )

    private fun document(
        imageId: String,
        text: String,
    ) = OcrDocument(
        imageId = imageId,
        rawText = text,
        blocks = emptyList(),
        engineId = "fixture",
        engineVersion = "1",
        recognizedAt = NOW,
    )

    private fun document(
        imageId: String,
        blocks: List<OcrTextBlock>,
    ) = OcrDocument(
        imageId = imageId,
        rawText = blocks.joinToString("\n") { it.text },
        blocks = blocks,
        engineId = "fixture",
        engineVersion = "1",
        recognizedAt = NOW,
    )

    private fun block(
        id: String,
        text: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) = OcrTextBlock(
        id = id,
        text = text,
        confidence = 0.98f,
        boundingBox = EvidenceBoundingBox(left, top, right, bottom),
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
