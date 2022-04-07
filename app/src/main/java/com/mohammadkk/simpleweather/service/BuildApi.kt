package com.mohammadkk.simpleweather.service

object BuildApi {
    fun getMainGPSAddress(lat: Double, lon: Double): String {
        val url = "http://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&lang=fa&units=metric&appId=a17639e2d998a74bd1dc8aa859c64f95"
        return url.lowercase()
    }
    fun getMainWeather(location: String): String {
        val url = "http://api.openweathermap.org/data/2.5/weather?lang=fa&units=metric&q=$location&appId=a17639e2d998a74bd1dc8aa859c64f95"
        return url.lowercase()
    }
    fun getMainForecast(lat: String, lon: String, lang: String): String {
        val url = "http://api.openweathermap.org/data/2.5/onecall?lat=$lat&lon=$lon&lang=$lang&units=metric&exclude=current&appId=a17639e2d998a74bd1dc8aa859c64f95"
        return url.lowercase()
    }
}