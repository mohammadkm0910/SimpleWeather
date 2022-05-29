package com.mohammadkk.simpleweather.helper

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.mohammadkk.simpleweather.R
import com.mohammadkk.simpleweather.databinding.ToastBinding
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
fun Context.hasPermission(permission: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
    return true
}
fun Activity.createCustomToast(message: String) {
    val context = this
    val toast = Toast(applicationContext)
    val toastBinding = ToastBinding.inflate(layoutInflater)
    toastBinding.root.background = GradientDrawable().apply {
        setColor(ContextCompat.getColor(context, R.color.warning))
        cornerRadius = resources.getDimension(R.dimen.corner_rounded_toast)
    }
    toastBinding.tvToast.text = message
    @Suppress("DEPRECATION")
    toast.view = toastBinding.root
    toast.duration = Toast.LENGTH_SHORT
    toast.show()
}
@Suppress("HasPlatformType")
fun Context.getSharedPrefs() = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

val Context.baseConfig: BaseConfig get() = BaseConfig.newInstance(this)

fun Cursor.getString(columnName: String): String? = getString(getColumnIndexOrThrow(columnName))
fun Cursor.getLong(columnName: String) = getLong(getColumnIndexOrThrow(columnName))

fun createHtml(source: String): Spanned? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(source)
    }
}
const val REQUEST_LOCATION_PERMISSION = 105