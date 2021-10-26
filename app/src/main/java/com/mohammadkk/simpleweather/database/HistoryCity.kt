package com.mohammadkk.simpleweather.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.mohammadkk.simpleweather.model.City

class HistoryCity(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_NAME TEXT);");
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME;")
        onCreate(db)
    }
    fun insertData(name: String, onInsert: (isCan:Boolean)->Unit) {
        val db = writableDatabase
        val values = ContentValues()
        val query = db?.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COLUMN_NAME=?", arrayOf(name))
        if (query?.count == 0) {
            try {
                values.put(COLUMN_NAME, name)
                db.use {
                    val insert: Long = db.insert(TABLE_NAME, null, values)
                    if (insert != -1L) {
                        onInsert(true)
                    } else {
                        onInsert(false)
                    }
                }
            } catch (e: Exception) {}
        }
        query?.close()
    }
    fun getAllData(): ArrayList<City> {
        val city = arrayListOf<City>()
        val cursor = readData()
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    val mId = cursor.getLong(cursor.getColumnIndex(COLUMN_ID))
                    val mName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
                    city.add(City(mId, mName))
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
                    val mName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
                    city.add(mName)
                } while (cursor.moveToNext())
            }
        }
        return city.toTypedArray()
    }
    fun destoryHistory(onDestroy: (isCan:Boolean)->Unit) {
        val db = writableDatabase
        try {
            db?.use {
                val destroy = db.delete(TABLE_NAME, null, null)
                onDestroy(destroy != -1)
            }
        } catch (e: Exception) {}
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
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "cities"
        private const val COLUMN_ID = "ID"
        private const val COLUMN_NAME = "NAME"
    }

}