package com.mohammadkk.simpleweather.helper

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.mohammadkk.simpleweather.R
import kotlin.math.roundToInt

class Toasty constructor(private val context: Context) {
    @SuppressLint("InflateParams")
    fun show(message: String) {
        val toast = Toast(context)
        val view = LayoutInflater.from(context).inflate(R.layout.toast_layout, null)
        val titleToast: TextView = view.findViewById(R.id.titleToast)
        titleToast.text = message
        val metrics: Float = context.resources.displayMetrics.density
        toast.setGravity(Gravity.BOTTOM, 0, (metrics * 18).roundToInt())
        toast.duration = Toast.LENGTH_SHORT
        toast.view = view
        toast.show()
    }
}