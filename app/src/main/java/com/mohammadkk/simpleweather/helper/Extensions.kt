package com.mohammadkk.simpleweather.helper

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.TypedValue
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
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
    val simpleDateFormat = SimpleDateFormat("EEEE", Locale(context.getString(R.string.lang)))
    simpleDateFormat.timeZone = TimeZone.getTimeZone(timezone)
    return simpleDateFormat.format(Date(this * 1000))
}
fun Context.getColorRes(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}
fun Context.dipDimension(size: Float): Int {
    val dm = resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, dm).toInt()
}
fun Cursor.getString(columnName: String): String? = getString(getColumnIndexOrThrow(columnName))
fun Cursor.getLong(columnName: String) = getLong(getColumnIndexOrThrow(columnName))
fun createHtml(source: String): Spanned? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(source)
    }
}
const val REQUEST_LOCATION_PERMISSION = 105