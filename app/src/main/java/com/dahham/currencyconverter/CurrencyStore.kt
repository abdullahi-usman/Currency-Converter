package com.dahham.currencyconverter

import android.content.Context
import androidx.room.*

/**
 * Created by dahham on 7/19/18.
 * This file is part of CurrencyConverter licensed under GNU Public License
 *
 */
@Entity
data class Rate(@PrimaryKey val conversion_name: String, val conversion_rate: Double)

@Dao
interface StoreManager{
    @Query("SELECT * FROM rate")
    fun getAll(): List<Rate>

    @Query("SELECT * FROM rate where conversion_name IS :conversion_id")
    fun getRateFor(conversion_id: String): Rate?

    @Update
    fun update(vararg rate: Rate)

    @Insert
    fun add(vararg rate: Rate)
}

@Database(entities = arrayOf(Rate::class), version = 1)
abstract class CurrencyStore: RoomDatabase() {
    abstract fun manager(): StoreManager;

    companion object {
        @JvmStatic
        private var instance : CurrencyStore? = null
        fun getInstance(context: Context): CurrencyStore{
            if(instance == null) {
                instance = Room.databaseBuilder(context.applicationContext, CurrencyStore::class.java, "conversion_rates.db").allowMainThreadQueries().build()
            }

            return instance!!
        }
    }
}

