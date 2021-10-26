package com.mohammadkk.simpleweather.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mohammadkk.simpleweather.R
import com.mohammadkk.simpleweather.model.City

class HistoryAdapter(private val context: Context, private val aCity: MutableList<City>, private val onclick:(name:String)->Unit) : RecyclerView.Adapter<HistoryAdapter.ForecastHolder>() {
    class ForecastHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val itemCityHistory: TextView = itemView.findViewById(R.id.itemCityHistory)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastHolder {
        return ForecastHolder(LayoutInflater.from(context).inflate(R.layout.history_item, parent, false))
    }
    @SuppressLint("RtlHardcoded")
    override fun onBindViewHolder(holder: ForecastHolder, position: Int) {
        holder.itemCityHistory.gravity = if (context.getString(R.string.lang) == "en") Gravity.LEFT else
            Gravity.RIGHT
        holder.itemCityHistory.text = aCity[position].name
        holder.itemCityHistory.setOnClickListener {
            onclick(aCity[position].name)
        }
    }
    override fun getItemCount(): Int {
        return aCity.size
    }
}