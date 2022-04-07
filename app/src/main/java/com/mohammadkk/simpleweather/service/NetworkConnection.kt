package com.mohammadkk.simpleweather.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings

class NetworkConnection constructor(private val context: Context) {
    fun isInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val nc = cm.getNetworkCapabilities(network)
            return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || nc.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH))
        } else {
            return isOldInternet(cm)
        }
    }
    @Suppress("DEPRECATION")
    private fun isOldInternet(connectivityManager: ConnectivityManager): Boolean {
        val newInfo = connectivityManager.activeNetworkInfo
        return newInfo != null && newInfo.isConnected
    }
    fun isAirplaneMode(): Boolean {
        return Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    }
}