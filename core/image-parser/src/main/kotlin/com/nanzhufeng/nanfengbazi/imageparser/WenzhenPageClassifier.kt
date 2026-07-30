package com.nanzhufeng.nanfengbazi.imageparser

import com.nanzhufeng.nanfengbazi.domain.model.OcrDocument
import com.nanzhufeng.nanfengbazi.domain.model.WenzhenPageType

data class PageClassification(
    val pageType: WenzhenPageType,
    val confidence: Float,
    val matchedAnchors: Set<String>,
    val classifierVersion: String,
)

interface WenzhenPageClassifier {
    val classifierVersion: String

    fun classify(document: OcrDocument): PageClassification
}

class AnchorBasedWenzhenPageClassifier : WenzhenPageClassifier {
    override val classifierVersion: String = "wenzhen-anchor-v1"

    override fun classify(document: OcrDocument): PageClassification {
        val text = document.rawText.normalizedForAnchors()
        val candidates = templates.mapNotNull { template ->
            val primary = template.primary.firstOrNull(text::contains) ?: return@mapNotNull null
            val support = template.support.filterTo(linkedSetOf(), text::contains)
            if (support.isEmpty()) return@mapNotNull null
            val confidence = (0.62f + support.size.coerceAtMost(3) * 0.1f)
                .coerceAtMost(0.92f)
            PageClassification(
                pageType = template.pageType,
                confidence = confidence,
                matchedAnchors = linkedSetOf(primary).apply { addAll(support) },
                classifierVersion = classifierVersion,
            )
        }
        return candidates.maxByOrNull(PageClassification::confidence)
            ?: PageClassification(
                pageType = WenzhenPageType.UNKNOWN,
                confidence = 0f,
                matchedAnchors = emptySet(),
                classifierVersion = classifierVersion,
            )
    }

    private fun String.normalizedForAnchors(): String =
        replace(Regex("\\s+"), "")

    private data class Template(
        val pageType: WenzhenPageType,
        val primary: Set<String>,
        val support: Set<String>,
    )

    private companion object {
        val templates = listOf(
            Template(
                WenzhenPageType.USER_LIST,
                setOf("用户列表"),
                setOf("名人库", "筛选", "阳历"),
            ),
            Template(
                WenzhenPageType.BASIC_INFO,
                setOf("基本信息"),
                setOf("出生地区", "真太阳时", "五行能量", "经度", "纬度"),
            ),
            Template(
                WenzhenPageType.FEEDBACK,
                setOf("命主反馈"),
                setOf("关键事件反馈记录", "职业", "学历", "财务", "婚姻", "健康"),
            ),
            Template(
                WenzhenPageType.COMMENTARY,
                setOf("师傅点评"),
                setOf("旺衰", "格局", "总结", "喜", "忌"),
            ),
            Template(
                WenzhenPageType.BASIC_CHART,
                setOf("基本排盘"),
                setOf("天干", "地支", "藏干", "纳音", "空亡"),
            ),
            Template(
                WenzhenPageType.PROFESSIONAL_CHART,
                setOf("专业细盘"),
                setOf("大运", "流年", "流月", "流日", "流时"),
            ),
            Template(
                WenzhenPageType.HOME_INPUT,
                setOf("首页排盘"),
                setOf("出生时间", "出生地区", "开始排盘"),
            ),
        )
    }
}
