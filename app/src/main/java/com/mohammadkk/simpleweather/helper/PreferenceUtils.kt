package com.mohammadkk.simpleweather.helper

import android.content.Context
import androidx.preference.PreferenceManager

object PreferenceUtils {
    internal const val KEY_LOCATION = "location"
    internal const val KEY_CHANGE_LANG = "lang_app"

    fun putString(context: Context, key: String, value: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply()
    }
    fun getString(context: Context, key: String, defaultValue: String): String {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue) ?: ""
    }
    fun putInt(context: Context, key: String, value: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply()
    }
    fun getInt(context: Context, key: String, defaultValue: Int):Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, defaultValue)
    }
}