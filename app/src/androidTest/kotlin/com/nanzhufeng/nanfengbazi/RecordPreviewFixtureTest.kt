package com.nanzhufeng.nanfengbazi

import androidx.test.platform.app.InstrumentationRegistry
import com.nanzhufeng.nanfengbazi.domain.CaseWriteResult
import com.nanzhufeng.nanfengbazi.domain.model.AnalysisCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseEvent
import com.nanzhufeng.nanfengbazi.domain.model.CaseEventCategory
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecord
import com.nanzhufeng.nanfengbazi.domain.model.CaseTextRecordType
import com.nanzhufeng.nanfengbazi.domain.model.EventDatePrecision
import com.nanzhufeng.nanfengbazi.domain.model.ExplicitText
import com.nanzhufeng.nanfengbazi.domain.model.SexForFortuneDirection
import com.nanzhufeng.nanfengbazi.domain.model.TextRecordSourceType
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * 仅在显式传入 seedPreviewCases=true 时写入本地模拟器，用于记录列表视觉验收。
 * 正常测试、正式构建和首次启动均不会内置或生成这些数据。
 */
class RecordPreviewFixtureTest {
    @Test
    fun seedNamedPreviewCasesOnExplicitRequest() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("seedPreviewCases") == "true")
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as NanfengBaziApplication
        val createCase = CreateCaseUseCase(
            baziEngine = application.container.baziEngine,
            caseRepository = application.container.caseRepository,
        )
        previewCases.forEach { fixture ->
            val result = createCase(
                form = CaseFormState(
                    alias = fixture.name,
                    name = fixture.name,
                    sex = fixture.sex,
                    year = fixture.year.toString(),
                    month = fixture.month.toString(),
                    day = fixture.day.toString(),
                    hour = "12",
                    minute = "0",
                    locationName = "北京市东城区",
                    longitude = "116.4167",
                    latitude = "39.9167",
                    timeZoneId = "Asia/Shanghai",
                ),
                allowDuplicate = true,
            )
            assertTrue(
                "${fixture.name} 写入失败：$result",
                result is CreateCaseResult.Created || result is CreateCaseResult.AlreadyExists,
            )
            if (fixture.name == "席瑞") {
                val caseId = when (result) {
                    is CreateCaseResult.Created -> result.caseId
                    is CreateCaseResult.AlreadyExists -> result.caseId
                    else -> error("预览命例创建失败")
                }
                val repository = application.container.caseRepository
                val current = requireNotNull(repository.findById(caseId))
                if (current.textRecords.isEmpty() && current.events.isEmpty()) {
                    val now = Instant.parse("2026-08-08T12:00:00Z")
                    val enriched = current.copy(
                        profile = current.profile.copy(
                            occupation = ExplicitText.present("产品与技术管理"),
                            education = ExplicitText.present("本科"),
                            finance = ExplicitText.present("求稳健，关注长期配置"),
                            marriage = ExplicitText.present("未填写"),
                            health = ExplicitText.present("作息不稳定，需持续记录"),
                        ),
                        textRecords = listOf(
                            CaseTextRecord(
                                id = "preview-owner-feedback-$caseId",
                                type = CaseTextRecordType.OWNER_FEEDBACK,
                                content = "近两年工作方向有调整，希望重点核对事业节奏与长期财务安排。",
                                sourceType = TextRecordSourceType.USER,
                                createdAt = now,
                                updatedAt = now,
                            ),
                            CaseTextRecord(
                                id = "preview-master-commentary-$caseId",
                                type = CaseTextRecordType.MASTER_COMMENTARY,
                                content = "此处为模拟器视觉验收用点评正文。排版应当直接、清晰，长内容自然换行，不使用层层卡片包裹。",
                                sourceType = TextRecordSourceType.USER,
                                createdAt = now,
                                updatedAt = now,
                            ),
                            CaseTextRecord(
                                id = "preview-analysis-$caseId",
                                type = CaseTextRecordType.ANALYSIS,
                                content = "事业与财富主题的人工复盘草稿。",
                                analysisCategory = AnalysisCategory.CAREER,
                                sourceType = TextRecordSourceType.USER,
                                createdAt = now,
                                updatedAt = now,
                            ),
                        ),
                        events = listOf(
                            previewEvent("preview-event-1-$caseId", 1999, "进入新的学习阶段，开始适应环境变化。", now),
                            previewEvent("preview-event-2-$caseId", 2004, "参加重要项目并承担更多责任。", now),
                            previewEvent("preview-event-3-$caseId", 2017, "工作方向发生明显调整。", now),
                        ),
                    )
                    val saved = repository.save(enriched, expectedRevision = current.revision)
                    assertTrue("预览笔记写入失败：$saved", saved is CaseWriteResult.Updated)
                }
            }
        }
    }
}

private fun previewEvent(
    id: String,
    year: Int,
    text: String,
    now: Instant,
) = CaseEvent(
    id = id,
    category = CaseEventCategory.GENERAL,
    year = year,
    datePrecision = EventDatePrecision.YEAR,
    rawText = text,
    createdAt = now,
)

private data class PreviewCaseFixture(
    val name: String,
    val sex: SexForFortuneDirection,
    val year: Int,
    val month: Int,
    val day: Int,
)

private val previewCases = listOf(
    PreviewCaseFixture("席瑞", SexForFortuneDirection.MAN, 1992, 8, 24),
    PreviewCaseFixture("安倍", SexForFortuneDirection.MAN, 1954, 9, 21),
    PreviewCaseFixture("林清娟", SexForFortuneDirection.WOMAN, 1967, 1, 22),
    PreviewCaseFixture("苏晚晴", SexForFortuneDirection.WOMAN, 1995, 3, 18),
    PreviewCaseFixture("程墨", SexForFortuneDirection.MAN, 2000, 8, 5),
    PreviewCaseFixture("沈星遥", SexForFortuneDirection.WOMAN, 2012, 12, 12),
    PreviewCaseFixture("顾南乔", SexForFortuneDirection.WOMAN, 1982, 9, 8),
    PreviewCaseFixture("江予安", SexForFortuneDirection.MAN, 1990, 1, 1),
)
