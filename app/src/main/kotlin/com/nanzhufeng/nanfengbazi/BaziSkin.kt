package com.nanzhufeng.nanfengbazi

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

/**
 * 外观仅保存于本机，不参与命例、备份、恢复码或南枫云快照。
 * 六套皮肤共享同一页面树与触控几何，只改变语义色和首页材质头图。
 */
@Immutable
data class BaziSkinTokens(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val heavySurface: Color,
    val primary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
)

@Immutable
data class BaziSkinVisualRecipe(
    @DrawableRes val headerArtworkRes: Int,
    val headerContentColor: Color,
    val headerScrimColor: Color,
    val headerScrimAlpha: Float,
    val artworkAlignment: Alignment = Alignment.Center,
)

enum class BaziSkin(
    val id: String,
    val displayName: String,
    val headline: String,
    val subhead: String,
    val tokens: BaziSkinTokens,
    val visualRecipe: BaziSkinVisualRecipe,
) {
    INK_STAR_CHART(
        id = "ink_star_chart",
        displayName = "玄墨星盘",
        headline = "观势知时",
        subhead = "命盘 · 岁运 · 流年",
        tokens = tokens(
            background = Color(0xFFF4F3F0),
            surface = Color.White,
            raised = Color.White,
            heavy = Color(0xFF25282B),
            primary = Color(0xFF3D7052),
            secondary = Color(0xFFB99A58),
            text = Color(0xFF202426),
            secondaryText = Color(0xFF687073),
        ),
        visualRecipe = BaziSkinVisualRecipe(
            headerArtworkRes = R.drawable.skin_header_bazi_ink_star_chart,
            headerContentColor = Color(0xFFFFFCF7),
            headerScrimColor = Color(0xFF111417),
            headerScrimAlpha = 0.38f,
        ),
    ),
    JADE_FIVE_ELEMENTS(
        id = "jade_five_elements",
        displayName = "青玉五行",
        headline = "五行流转",
        subhead = "审时 · 观势 · 定盘",
        tokens = tokens(
            background = Color(0xFFEAF3F0),
            surface = Color.White,
            raised = Color.White,
            heavy = Color(0xFF214C47),
            primary = Color(0xFF287A70),
            secondary = Color(0xFFE08B4A),
            text = Color(0xFF203631),
            secondaryText = Color(0xFF5E726C),
        ),
        visualRecipe = BaziSkinVisualRecipe(
            headerArtworkRes = R.drawable.skin_header_bazi_jade_five_elements,
            headerContentColor = Color(0xFFFFFDF8),
            headerScrimColor = Color(0xFF0E4842),
            headerScrimAlpha = 0.28f,
        ),
    ),
    CINNABAR_CLOUD(
        id = "cinnabar_cloud",
        displayName = "朱砂云纹",
        headline = "红鸾照命",
        subhead = "格局 · 用神 · 应期",
        tokens = tokens(
            background = Color(0xFFF7F1F0),
            surface = Color.White,
            raised = Color.White,
            heavy = Color(0xFF6D2630),
            primary = Color(0xFF9C3F4C),
            secondary = Color(0xFFD88A43),
            text = Color(0xFF3A2024),
            secondaryText = Color(0xFF795B61),
        ),
        visualRecipe = BaziSkinVisualRecipe(
            headerArtworkRes = R.drawable.skin_header_bazi_cinnabar_cloud,
            headerContentColor = Color(0xFFFFFBF6),
            headerScrimColor = Color(0xFF5B1723),
            headerScrimAlpha = 0.40f,
        ),
    ),
    GOLDEN_TRIGRAM(
        id = "golden_trigram",
        displayName = "金棕卦象",
        headline = "乾坤有序",
        subhead = "四柱 · 十神 · 大运",
        tokens = tokens(
            background = Color(0xFFF5F1EA),
            surface = Color.White,
            raised = Color.White,
            heavy = Color(0xFF503C25),
            primary = Color(0xFF907038),
            secondary = Color(0xFF237B73),
            text = Color(0xFF342A1E),
            secondaryText = Color(0xFF71634D),
        ),
        visualRecipe = BaziSkinVisualRecipe(
            headerArtworkRes = R.drawable.skin_header_bazi_golden_trigram,
            headerContentColor = Color(0xFFFFFCF5),
            headerScrimColor = Color(0xFF35291B),
            headerScrimAlpha = 0.38f,
        ),
    ),
    INDIGO_GALAXY(
        id = "indigo_galaxy",
        displayName = "靛蓝星河",
        headline = "星汉垂象",
        subhead = "命局 · 流月 · 流日",
        tokens = tokens(
            background = Color(0xFFF0F2F7),
            surface = Color.White,
            raised = Color.White,
            heavy = Color(0xFF263A63),
            primary = Color(0xFF4968A8),
            secondary = Color(0xFFC99245),
            text = Color(0xFF263047),
            secondaryText = Color(0xFF626C80),
        ),
        visualRecipe = BaziSkinVisualRecipe(
            headerArtworkRes = R.drawable.skin_header_bazi_indigo_galaxy,
            headerContentColor = Color(0xFFFFFCF7),
            headerScrimColor = Color(0xFF182440),
            headerScrimAlpha = 0.36f,
        ),
    ),
    FROST_PEACH_BLOSSOM(
        id = "frost_peach_blossom",
        displayName = "霜白桃花",
        headline = "花开有期",
        subhead = "日主 · 合冲 · 神煞",
        tokens = tokens(
            background = Color(0xFFF7F4F2),
            surface = Color.White,
            raised = Color.White,
            heavy = Color(0xFF65515A),
            primary = Color(0xFF9A6575),
            secondary = Color(0xFFB99354),
            text = Color(0xFF372D31),
            secondaryText = Color(0xFF76666C),
        ),
        visualRecipe = BaziSkinVisualRecipe(
            headerArtworkRes = R.drawable.skin_header_bazi_frost_peach_blossom,
            headerContentColor = Color(0xFF322A2E),
            headerScrimColor = Color(0xFFFFFCF8),
            headerScrimAlpha = 0.40f,
        ),
    );

    companion object {
        fun fromStoredId(id: String?): BaziSkin =
            entries.firstOrNull { it.id == id } ?: INK_STAR_CHART
    }
}

private fun tokens(
    background: Color,
    surface: Color,
    raised: Color,
    heavy: Color,
    primary: Color,
    secondary: Color,
    text: Color,
    secondaryText: Color,
) = BaziSkinTokens(
    background = background,
    surface = surface,
    surfaceRaised = raised,
    heavySurface = heavy,
    primary = primary,
    secondary = secondary,
    textPrimary = text,
    textSecondary = secondaryText,
    outline = text.copy(alpha = 0.12f),
)

class BaziSkinPreferenceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): BaziSkin = BaziSkin.fromStoredId(preferences.getString(KEY_SKIN, null))

    fun write(skin: BaziSkin) {
        preferences.edit().putString(KEY_SKIN, skin.id).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "bazi_skin_preferences"
        const val KEY_SKIN = "selected_skin"
    }
}
