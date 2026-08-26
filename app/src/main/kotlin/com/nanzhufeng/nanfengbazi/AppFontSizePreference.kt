package com.nanzhufeng.nanfengbazi

import android.content.Context

/**
 * App-owned type scale. Standard deliberately means the font size users see before changing
 * this setting, rather than mirroring a device-wide accessibility setting.
 */
enum class AppFontSizePreference(
    val id: String,
    val displayName: String,
    internal val adjustmentSp: Float,
) {
    SMALL("small", "小号", -2f),
    STANDARD("standard", "标准", 0f),
    LARGE("large", "大号", 1f),
    ;

    companion object {
        fun fromStoredId(id: String?): AppFontSizePreference =
            entries.firstOrNull { it.id == id } ?: STANDARD
    }
}

class AppFontSizePreferenceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): AppFontSizePreference =
        AppFontSizePreference.fromStoredId(preferences.getString(KEY_FONT_SIZE, null))

    fun write(preference: AppFontSizePreference) {
        preferences.edit().putString(KEY_FONT_SIZE, preference.id).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "bazi_display_preferences"
        const val KEY_FONT_SIZE = "font_size"
    }
}
