package com.mohammadkk.simpleweather.helper

import android.content.Context

class BaseConfig(val context: Context) {
    private val prefs = context.getSharedPrefs()

    companion object {
        fun newInstance(context: Context) = BaseConfig(context)
    }
    var lastLocation: String
        get() = prefs.getString(LAST_LOCATION, "tehran") ?: ""
        set(location) = prefs.edit().putString(LAST_LOCATION, location).apply()
    var appLanguage: Int
        get() = prefs.getInt(APP_LANG, 0)
        set(lang) = prefs.edit().putInt(APP_LANG, lang).apply()
}