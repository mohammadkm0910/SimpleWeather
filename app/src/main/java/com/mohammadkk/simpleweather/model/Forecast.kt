package com.mohammadkk.simpleweather.model

data class Forecast(
    internal val day: String,
    internal val description: String,
    internal val minTemp: String,
    internal val maxTemp: String,
    internal val icon: String

)