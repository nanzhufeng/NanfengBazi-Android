package com.nanzhufeng.nanfengbazi

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.serialization.Serializable

/**
 * Token-only local estimate used when the provider did not return a settlement amount.
 *
 * The supported price table is deliberately limited to model IDs with a verified public price.
 * Unknown models and automatic routing remain unpriced rather than being presented as a bill.
 */
@Serializable
data class AiCommentaryEstimatedCost(
    val priceVersion: String,
    val currencyCode: String,
    val totalMicros: Long,
)

internal object AiCommentaryCostEstimator {
    const val OPENROUTER_PRICE_VERSION = "openrouter-public-prices-2026-08-v1"
    const val QWEN_PRICE_VERSION = "qwen-cn-beijing-standard-2026-08-v2"
    const val DEEPSEEK_LEGACY_PRICE_VERSION = "deepseek-public-prices-before-2026-08-17-v1"
    const val DEEPSEEK_OFF_PEAK_PRICE_VERSION = "deepseek-off-peak-2026-08-v2"
    const val DEEPSEEK_PEAK_PRICE_VERSION = "deepseek-peak-2026-08-v2"

    private data class Price(
        val version: String,
        val inputPerMillion: BigDecimal,
        val outputPerMillion: BigDecimal,
        val currencyCode: String = "USD",
    )

    private data class InputTier(val upperInclusiveInputTokens: Long, val price: Price)

    private fun price(
        version: String,
        inputPerMillion: String,
        outputPerMillion: String,
        currencyCode: String = "USD",
    ) = Price(
        version = version,
        inputPerMillion = BigDecimal(inputPerMillion),
        outputPerMillion = BigDecimal(outputPerMillion),
        currencyCode = currencyCode,
    )

    private val fixedPrices = mapOf(
        "anthropic/claude-fable-5" to price(OPENROUTER_PRICE_VERSION, "10", "50"),
        "anthropic/claude-opus-5" to price(OPENROUTER_PRICE_VERSION, "5", "25"),
        "anthropic/claude-sonnet-5" to price(OPENROUTER_PRICE_VERSION, "2", "10"),
        "anthropic/claude-haiku-4.5" to price(OPENROUTER_PRICE_VERSION, "1", "5"),
        "qwen/qwen3.8-max" to price(QWEN_PRICE_VERSION, "12", "36", "CNY"),
    )

    private val inputTieredPrices = mapOf(
        "openai/gpt-5.6-sol" to listOf(
            InputTier(271_999, price(OPENROUTER_PRICE_VERSION, "2", "10")),
            InputTier(Long.MAX_VALUE, price(OPENROUTER_PRICE_VERSION, "4", "15")),
        ),
        "openai/gpt-5.6-terra" to listOf(
            InputTier(271_999, price(OPENROUTER_PRICE_VERSION, "2", "12")),
            InputTier(Long.MAX_VALUE, price(OPENROUTER_PRICE_VERSION, "4", "18")),
        ),
        "openai/gpt-5.6-luna" to listOf(
            InputTier(271_999, price(OPENROUTER_PRICE_VERSION, "0.2", "1.2")),
            InputTier(Long.MAX_VALUE, price(OPENROUTER_PRICE_VERSION, "0.4", "1.8")),
        ),
        "qwen3.7-plus" to listOf(
            InputTier(256_000, price(QWEN_PRICE_VERSION, "0.276", "1.101", "CNY")),
            InputTier(1_000_000, price(QWEN_PRICE_VERSION, "0.826", "3.301", "CNY")),
        ),
    )

    private val deepSeekPeakPricingEffectiveAt = Instant.parse("2026-08-16T16:00:00Z")

    fun estimate(
        model: String,
        inputTokens: Long?,
        outputTokens: Long?,
        requestedAt: Instant,
    ): AiCommentaryEstimatedCost? {
        val input = inputTokens ?: return null
        val output = outputTokens ?: return null
        if (input < 0 || output < 0) return null
        val modelId = model.lowercase()
        val price = deepSeekPrice(modelId, requestedAt)
            ?: fixedPrices[modelId]
            ?: inputTieredPrices[modelId]?.firstOrNull { input <= it.upperInclusiveInputTokens }?.price
            ?: return null
        val micros = BigDecimal.valueOf(input).multiply(price.inputPerMillion)
            .add(BigDecimal.valueOf(output).multiply(price.outputPerMillion))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
        return AiCommentaryEstimatedCost(
            priceVersion = price.version,
            currencyCode = price.currencyCode,
            totalMicros = micros,
        )
    }

    private fun deepSeekPrice(modelId: String, at: Instant): Price? {
        val normalized = when (modelId) {
            "deepseek/deepseek-v4-pro" -> "deepseek-v4-pro"
            "deepseek/deepseek-v4-flash" -> "deepseek-v4-flash"
            else -> modelId
        }
        if (normalized !in setOf("deepseek-v4-pro", "deepseek-v4-flash")) return null
        if (at.isBefore(deepSeekPeakPricingEffectiveAt)) {
            return if (normalized == "deepseek-v4-pro") {
                price(DEEPSEEK_LEGACY_PRICE_VERSION, "0.435", "0.87")
            } else {
                price(DEEPSEEK_LEGACY_PRICE_VERSION, "0.14", "0.28")
            }
        }
        val peak = deepSeekPeakPeriodAt(at)
        return when (normalized) {
            "deepseek-v4-pro" -> if (peak) {
                price(DEEPSEEK_PEAK_PRICE_VERSION, "1.32", "3.96")
            } else {
                price(DEEPSEEK_OFF_PEAK_PRICE_VERSION, "0.66", "1.98")
            }
            else -> if (peak) {
                price(DEEPSEEK_PEAK_PRICE_VERSION, "0.44", "1.32")
            } else {
                price(DEEPSEEK_OFF_PEAK_PRICE_VERSION, "0.22", "0.66")
            }
        }
    }

    private fun deepSeekPeakPeriodAt(instant: Instant): Boolean {
        val secondOfDay = instant.atOffset(ZoneOffset.UTC).toLocalTime().toSecondOfDay()
        return secondOfDay in 1 * 60 * 60 until 4 * 60 * 60 ||
            secondOfDay in 6 * 60 * 60 until 10 * 60 * 60
    }
}

internal object AiCommentaryCnyMoneyDisplay {
    const val USD_REFERENCE_LABEL = "美元按 1 美元 ≈ ¥6.7203 换算（2026-08-27）"

    private val microsPerUnit = BigDecimal("1000000")
    private val usdToCny = BigDecimal("6.7203091455560326320")

    fun label(cost: AiCommentaryEstimatedCost): String? {
        val sourceAmount = BigDecimal.valueOf(cost.totalMicros).divide(microsPerUnit)
        val amount = when (cost.currencyCode) {
            "CNY" -> sourceAmount
            "USD" -> sourceAmount.multiply(usdToCny)
            else -> return null
        }.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return "≈ ¥$amount（估算）"
    }
}

internal fun AiCommentaryCallRecord.estimatedCostOrNull(): AiCommentaryEstimatedCost? =
    estimatedCost ?: AiCommentaryCostEstimator.estimate(
        model = model,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        requestedAt = Instant.ofEpochMilli(requestedAtEpochMillis),
    )
