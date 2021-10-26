package com.mohammadkk.simpleweather.helper

import android.content.Context
import android.content.SharedPreferences

class CacheApp constructor(context: Context) {
    private var preferences: SharedPreferences = context.getSharedPreferences("config_app", Context.MODE_PRIVATE)
    fun saveLocation(city: String) {
        preferences.edit().apply {
            putString("location", city)
            apply()
        }
    }
    fun getLocation(): String {
        return preferences.getString("location", "tehran") ?: ""
    }
}