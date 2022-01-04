package com.mohammadkk.simpleweather.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.mohammadkk.simpleweather.R
import com.mohammadkk.simpleweather.model.City

class HistoryAdapter(private val context: Context, private val onclick:(name:String)->Unit) : RecyclerView.Adapter<HistoryAdapter.ForecastHolder>() {
    private val callback = object : SortedListAdapterCallback<City>(this) {
        override fun compare(o1: City?, o2: City?): Int {
            if (o1 != null && o2 != null) {
                return o2.date.compareTo(o1.date)
            }
            return 0
        }
        override fun areContentsTheSame(oldItem: City?, newItem: City?): Boolean {
            return oldItem?.equals(newItem) ?: false
        }
        override fun areItemsTheSame(item1: City?, item2: City?): Boolean {
            return item1?.equals(item2) ?: false
        }
    }
    private val items = SortedList(City::class.java, callback)

    class ForecastHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val itemCityHistory: TextView = itemView.findViewById(R.id.itemCityHistory)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastHolder {
        return ForecastHolder(LayoutInflater.from(context).inflate(R.layout.history_item, parent, false))
    }
    override fun onBindViewHolder(holder: ForecastHolder, position: Int) {
        holder.itemCityHistory.text = items[position].name
        holder.itemCityHistory.setOnClickListener {
            onclick(items[position].name)
            Log.d("long", items[position].date.toString())
        }

    }
    override fun getItemCount(): Int {
        return items.size()
    }
    private fun refresh() {
        for (i in 0 until items.size()) {
            notifyItemChanged(i)
        }
    }
    fun add(city: City) {
        items.add(city)
        refresh()
    }
    fun allAll(cities: List<City>) {
        items.addAll(cities)
    }
    fun clear() {
        while (items.size() > 0) items.removeItemAt(items.size() -1)
        refresh()
    }
}