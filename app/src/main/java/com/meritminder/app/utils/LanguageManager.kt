package com.meritminder.app.utils

import android.content.Context
import java.util.Locale

object LanguageManager {

    private const val PREF_NAME = "app_settings"
    private const val KEY_LANGUAGE = "language"

    const val SYSTEM = ""
    const val ENGLISH = "en"
    const val CHINESE_SIMPLIFIED = "zh"
    const val CHINESE_TRADITIONAL = "zh-TW"

    fun getLanguageCode(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SYSTEM) ?: SYSTEM

    fun setLanguageCode(context: Context, code: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, code)
            .apply()
    }

    fun applyLocale(context: Context): Context {
        val code = getLanguageCode(context)
        if (code.isEmpty()) return context
        val locale = Locale.forLanguageTag(code)
        val config = context.resources.configuration.also { it.setLocale(locale) }
        return context.createConfigurationContext(config)
    }
}
