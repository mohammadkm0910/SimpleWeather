package com.mohammadkk.simpleweather.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.mohammadkk.simpleweather.helper.getLong
import com.mohammadkk.simpleweather.helper.getString
import com.mohammadkk.simpleweather.model.City
import java.util.*

class HistoryCity(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_NAME TEXT, $COLUMN_DATE TEXT);")
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME;")
        onCreate(db)
    }
    fun insertData(name: String): Boolean {
        var result = false
        val db = writableDatabase
        val values = ContentValues()
        val query = db?.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COLUMN_NAME=?", arrayOf(name))
        if (query?.count == 0) {
            try {
                values.put(COLUMN_NAME, name)
                values.put(COLUMN_DATE, Date().time.toString())
                db.use {
                    val insert: Long = db.insert(TABLE_NAME, null, values)
                    result = insert != -1L
                }
            } catch (e: Exception) {}
        }
        query?.close()
        return result
    }
    fun getAllData(): ArrayList<City> {
        val city = arrayListOf<City>()
        val cursor = readData()
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    val mId = cursor.getLong(COLUMN_ID)
                    val mName = cursor.getString(COLUMN_NAME) ?: ""
                    val mDate = cursor.getLong(COLUMN_DATE)
                    city.add(City(mId, mName, mDate))
                } while (cursor.moveToNext())
            }
        }
        return city
    }
    fun getAllCity(): Array<String> {
        val city = arrayListOf<String>()
        val cursor = readData()
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    val mName = cursor.getString(COLUMN_NAME) ?:""
                    city.add(mName)
                } while (cursor.moveToNext())
            }
        }
        return city.toTypedArray()
    }
    fun getLastDate() = getAllData()[getAllData().size-1]
    fun getLastCity() = getAllCity()[getAllCity().size-1]
    fun destroyHistory(): Boolean {
        var isDestroyable = false
        val db = writableDatabase
        try {
            db?.use {
                val destroy = db.delete(TABLE_NAME, null, null)
                isDestroyable = destroy != -1
            }
        } catch (e: Exception) {}
        return isDestroyable
    }
    private fun readData(): Cursor? {
        val db = writableDatabase
        val query = db?.rawQuery("SELECT * FROM $TABLE_NAME;", null)
        try {
            return query
        } catch (e: Exception) {}
        return null
    }
    companion object {
        private const val DATABASE_NAME = "history_city"
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "cities"
        private const val COLUMN_ID = "ID"
        private const val COLUMN_NAME = "NAME"
        private const val COLUMN_DATE = "DATE"
    }

}