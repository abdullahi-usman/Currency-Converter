package com.dahham.currencyconverter.ui.main

import android.content.Context
import android.util.JsonReader
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dahham.currencyconverter.Currency
import com.dahham.currencyconverter.CurrencyStore
import com.dahham.currencyconverter.Rate
import java.io.File
import java.io.StringReader
import java.net.URL
import java.util.*

class MainViewModel : ViewModel() {
    // TODO: Implement the ViewModel
    private val countriesLiveData = MutableLiveData<List<Currency>>()
    var fetchRequest : Thread? = null

    private fun formatCountries(data: String): ArrayList<Currency> {

        val arrayList = ArrayList<Currency>()
        val json_reader = JsonReader(StringReader(data))

        json_reader.beginObject()
        if (json_reader.hasNext() && json_reader.nextName() == "results") {
            json_reader.beginObject()

            while (json_reader.hasNext() && json_reader.nextName() != null) {
                json_reader.beginObject()


                val currency = Currency()
                while (json_reader.hasNext()) {
                    when (json_reader.nextName()) {
                        "currencyName" -> currency.currencyName = json_reader.nextString()
                        "currencyId" -> currency.currencyId = json_reader.nextString()
                        "currencySymbol" -> currency.currencySymbol = json_reader.nextString()
                        "id" -> currency.id = json_reader.nextString()
                        "name" -> currency.name = json_reader.nextString()
                        else -> json_reader.nextString()
                    }
                }
                arrayList.add(currency)
                json_reader.endObject()
            }

            json_reader.endObject()
        }
        json_reader.endObject()

        arrayList.sortWith(object : Comparator<Currency> {
            override fun compare(p0: Currency?, p1: Currency?): Int {
                return if ((p0!!.name > p1!!.name)) 1 else -1
            }
        })
        return arrayList;
    }

    fun getCountries(context: Context): LiveData<List<Currency>> {

        if (countriesLiveData.value == null || countriesLiveData.value!!.size <= 0) {


            Thread(Runnable {

                var bytes: ByteArray
                val previous_file = File(context.filesDir.absolutePath + "/countires.json")
                if (previous_file.exists().not() || (Calendar.getInstance().time.time - previous_file.lastModified()) >= (1000 * 60 * 60 * 24 * 3)) {
                    try {
                        val stream = URL("https://free.currencyconverterapi.com/api/v5/countries").openStream()
                        bytes = stream.readBytes()

                    } catch (ex: Exception) {
                        bytes = context.resources.assets.open("countries-offline-default.json").readBytes()
                    }

                    val output_file = context.openFileOutput("countires.json", Context.MODE_PRIVATE)
                    output_file.write(bytes)
                    output_file.flush()
                    output_file.close()
                } else {
                    bytes = previous_file.readBytes()
                }

                countriesLiveData.postValue(formatCountries(String(bytes)))
            }).start()
        }

        return countriesLiveData
    }

    fun fetchConversionRates(context: Context, first_currency: Currency, second_currency: Currency): LiveData<Pair<Double, Double>> {
        val conversionsLiveData = MutableLiveData<Pair<Double, Double>>()
        fetchRequest = Thread(Runnable {

            var conversion_rate1 = 0.00
            var conversion_rate2 = 0.00

            val con1 = "${first_currency.currencyId}_${second_currency.currencyId}"
            val con2 = "${second_currency.currencyId}_${first_currency.currencyId}"

            val store_manager = CurrencyStore.getInstance(context).manager()

            try {
                val data = URL("https://free.currencyconverterapi.com/api/v5/convert?q=${con1},${con2}&compact=ultra").openStream().readBytes()

                val s = String(data)
                val json_reader = JsonReader(StringReader(s))

                json_reader.beginObject()
                while (json_reader.hasNext()) {
                    when(json_reader.nextName()) {
                        con1 -> conversion_rate1 = json_reader.nextDouble()
                        con2 -> conversion_rate2 = json_reader.nextDouble()
                    }
                }
                json_reader.endObject()

                store_manager.add(Rate(con1, conversion_rate1), Rate(con2, conversion_rate2))
            } catch (ex: Exception) {
                ex.printStackTrace()

                var rate = store_manager.getRateFor(con1)
                if (rate != null){
                    conversion_rate1 = rate.conversion_rate
                }

                rate = store_manager.getRateFor(con2)

                if(rate != null){
                    conversion_rate2 = rate.conversion_rate
                }
            }

            conversionsLiveData.postValue(Pair(conversion_rate1, conversion_rate2))

        })
        fetchRequest?.start()

        return conversionsLiveData;
    }

    fun cancelFetchRequest(){
        fetchRequest?.interrupt()
    }
}
