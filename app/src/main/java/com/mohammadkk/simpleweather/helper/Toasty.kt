package com.mohammadkk.simpleweather.helper

import android.app.Activity
import android.view.Gravity
import android.widget.Toast
import com.mohammadkk.simpleweather.databinding.ToastLayoutBinding
import kotlin.math.roundToInt

class Toasty constructor() {
    companion object {
        fun show(activity: Activity, message: String) {
            val toast = Toast(activity.applicationContext)
            val binding = ToastLayoutBinding.inflate(activity.layoutInflater)
            binding.titleToast.text = message
            val metrics: Float = activity.resources.displayMetrics.density
            toast.setGravity(Gravity.BOTTOM, 0, (metrics * 18).roundToInt())
            toast.duration = Toast.LENGTH_SHORT
            toast.view = binding.root
            toast.show()
        }
    }
}