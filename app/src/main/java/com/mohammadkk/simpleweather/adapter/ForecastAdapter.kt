package com.mohammadkk.simpleweather.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mohammadkk.simpleweather.databinding.ForecastItemBinding
import com.mohammadkk.simpleweather.helper.createHtml
import com.mohammadkk.simpleweather.model.Forecast

class ForecastAdapter(private val context: Context, private val aForecast: MutableList<Forecast>) : RecyclerView.Adapter<ForecastAdapter.ForecastHolder>() {
    class ForecastHolder(binding: ForecastItemBinding) : RecyclerView.ViewHolder(binding.root) {
        internal val itemDayForecast = binding.itemDayForecast
        internal val itemIconForecast = binding.itemIconForecast
        internal val itemDescriptionForecast = binding.itemDescriptionForecast
        internal val itemTempForecast = binding.itemTempForecast
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastHolder {
        val inflater = LayoutInflater.from(context)
        return ForecastHolder(ForecastItemBinding.inflate(inflater, parent, false))
    }
    override fun onBindViewHolder(holder: ForecastHolder, position: Int) {
        val forecast = aForecast[position]
        holder.itemDayForecast.text = forecast.day
        val iconSrc = context.resources.getIdentifier("@drawable/w${forecast.icon}", null, context.packageName)
        val iconResult = ContextCompat.getDrawable(context, iconSrc)
        holder.itemIconForecast.setImageDrawable(iconResult)
        holder.itemDescriptionForecast.text = forecast.description
        val tempMax = forecast.maxTemp
        val tempMin = forecast.minTemp
        holder.itemTempForecast.text = createHtml("<b>$tempMax°</b>/<span>$tempMin°</span>")
    }
    override fun getItemCount(): Int {
        return aForecast.size
    }
}