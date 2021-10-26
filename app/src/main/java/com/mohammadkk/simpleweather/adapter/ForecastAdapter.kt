package com.mohammadkk.simpleweather.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mohammadkk.simpleweather.R
import com.mohammadkk.simpleweather.helper.createHtml
import com.mohammadkk.simpleweather.model.Forecast

class ForecastAdapter(private val context: Context, private val aForecast: MutableList<Forecast>) : RecyclerView.Adapter<ForecastAdapter.ForecastHolder>() {
    class ForecastHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val itemDayForecast: TextView = itemView.findViewById(R.id.itemDayForecast)
        internal val itemIconForecast: ImageView = itemView.findViewById(R.id.itemIconForecast)
        internal val itemDescriptionForecast: TextView = itemView.findViewById(R.id.itemDescriptionForecast)
        internal val itemTempForecast: TextView = itemView.findViewById(R.id.itemTempForecast)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastHolder {
        return ForecastHolder(LayoutInflater.from(context).inflate(R.layout.forecast_item, parent, false))
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