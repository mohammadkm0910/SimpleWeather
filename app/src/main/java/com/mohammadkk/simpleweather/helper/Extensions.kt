package com.mohammadkk.simpleweather.helper

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Spanned
import com.mohammadkk.simpleweather.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

fun Long.dateTimePattern(timezone: String, lang: String="fa"): String {
    val dateFormat: DateFormat = SimpleDateFormat("hh:mm a", Locale(lang))
    dateFormat.timeZone = TimeZone.getTimeZone(timezone)
    return dateFormat.format(Date(this * 1000))
}
fun Long.getDay(timezone: String, context: Context): String {
    runCatching {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = this * 1000
        calendar.timeZone = TimeZone.getTimeZone(timezone)
        val today = calendar.get(Calendar.DAY_OF_WEEK)
        return when(today % 7) {
            0-> context.getString(R.string.saturday)
            Calendar.SUNDAY-> context.getString(R.string.sunday)
            Calendar.MONDAY-> context.getString(R.string.monday)
            Calendar.TUESDAY-> context.getString(R.string.thursday)
            Calendar.WEDNESDAY-> context.getString(R.string.wednesday)
            Calendar.THURSDAY-> context.getString(R.string.thursday)
            else-> context.getString(R.string.friday)
        }
    }
    return ""
}
fun createHtml(source: String): Spanned? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(source)
    }
}