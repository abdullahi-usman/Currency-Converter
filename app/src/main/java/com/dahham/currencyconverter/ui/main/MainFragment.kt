package com.dahham.currencyconverter.ui.main

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.dahham.currencyconverter.Currency
import com.dahham.currencyconverter.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.currency_details.*
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var currencies: List<Currency>
    private var first_currency: Currency? = null
    private var second_currency: Currency? = null

    private var conversion_rate1: Double = 0.00
    private var conversion_rate2: Double = 0.00


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel

        val dialog = AlertDialog.Builder(context!!).setMessage("Getting list of currencies, please wait...").setCancelable(false).create()
        dialog.show()
        viewModel.getCountries(context!!).observe(this, object : Observer<List<Currency>> {
            override fun onChanged(t: List<Currency>?) {

                dialog.dismiss()

                if (t != null) {
                    currencies = t
                }
            }
        })

        first_country_container.setOnClickListener {
            setSelectedCurrency(button_first_country, 1)
        }

        second_country_container.setOnClickListener {
            setSelectedCurrency(button_second_country, 2)
        }


        amount.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(p0: View?, p1: Int, keyEvent: KeyEvent?): Boolean {

                if (keyEvent?.keyCode == KeyEvent.KEYCODE_DEL && amount.text.toString().trim() == "${first_currency?.currencySymbol ?: "$"}"){
                    return true
                }

                if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_UP) {
                    formatAmountInputSymbol()
                    calculate()
                }

                return false
            }
        })

        amount.setOnEditorActionListener { textView, i, keyEvent ->
            if (keyEvent == null && conversion_rate1 == 0.00) {
                fetchConversionRates()
            }

            return@setOnEditorActionListener false
        }

        toggle_conversions.setOnClickListener {

            val animator = ObjectAnimator.ofFloat(toggle_conversions, "rotation", toggle_conversions.rotation, -toggle_conversions.rotation)
            animator.setDuration(250)
            animator.interpolator = LinearInterpolator()
            animator.start()

            if (conversion_rate1 == 0.00 && conversion_rate2 == 0.00) {
                fetchConversionRates()
            }

            amount.text.clearSpans()
            amount.setText(amount.text.toString().replace(first_currency?.currencySymbol ?: "$", "", true))

            val conversion_rate_temp = conversion_rate1
            conversion_rate1 = conversion_rate2
            conversion_rate2 = conversion_rate_temp

            val currency_temp = first_currency
            first_currency = second_currency
            second_currency = currency_temp

            val btn_name_tmp = button_first_country.text
            button_first_country.text = button_second_country.text
            button_second_country.text = btn_name_tmp

            formatAmountInputSymbol()

            calculate()

            setConvertRate()
        }

        toggle_conversions.isEnabled = false
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.main_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item?.itemId){
            R.id.clear -> {
                reset()
                Toast.makeText(context!!, "Cleared", Toast.LENGTH_LONG).show()
            }
            R.id.about -> {
                AlertDialog.Builder(context!!).setView(R.layout.about).show()
            }
            else -> return false
        }

        return true
    }

    private fun reset() {
        amount.setText("")
        result.setText(R.string.zero_dollars)
        convert_rate1.setText(R.string.zero_dollars)
        convert_rate2.setText(R.string.zero_dollars)
        button_first_country.setText(R.string.first_country_button_text)
        button_second_country.setText(R.string.second_country_button_text)
        first_currency = null
        second_currency = null
    }

    private fun setConvertRate(){
        convert_rate1.text = "${first_currency?.currencySymbol} 1.00"
        convert_rate2.text = "${second_currency?.currencySymbol} ${conversion_rate1}"
    }

    private fun formatAmountInputSymbol(){

        if (amount.text.length > 0 && amount.text.startsWith(first_currency?.currencySymbol ?: "$", true).not()) {
            val cur = first_currency?.currencySymbol ?: "$"

            amount.text.clearSpans()
            amount.setText("${cur}${amount.text.toString().replace(cur, "").trim()}")

            val num_sta = amount.text.indexOf(first_currency?.currencySymbol ?: "$")

            var color = resources.getColor(R.color.colorAccent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                color = resources.getColor(R.color.colorAccent, context!!.theme)
            }
            amount.text.setSpan(ForegroundColorSpan(color), num_sta, num_sta + cur.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//            amount.text.setSpan(StyleSpan(BOLD), num_sta, num_sta + cur.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//            amount.text.setSpan(ResourcesCompat.getFont(context!!, R.font.berkshire_swash), num_sta, num_sta + cur.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            amount.setSelection(amount.length())
        }
    }

    fun firstCountryClicked(view: TextView) {
        setSelectedCurrency(view, 1)
    }

    fun secondCountryClicked(view: TextView) {
        setSelectedCurrency(view, 2)
    }

    private fun formatDecimal(double: Double, roundUpDecimals: Boolean = true): String {
        val d = double.toString()

        var main: String
        var deci: String
        var expo = ""

        if (d.contains(".", ignoreCase = true)) {
            val splits: List<String>
            splits = d.split(Regex("\\."), 2)
            main = splits[0]
            deci = splits[1]
        } else {
            main = d;
            deci = ""
            expo = ""
        }

        if (deci.contains("E", ignoreCase = true)) {
            expo = deci.substring(deci.indexOf("E", ignoreCase = true))
            deci = deci.replace(expo, "")
        }

        val rem = main.length % 3
        val div = main.length / 3

        val string = StringBuilder()
        if (rem != 0) {
            string.append(main.take(rem))
            main = main.drop(rem)
        }

        if (string.isEmpty().not() && main.isEmpty().not()) {
            string.append(",")
        }

        for (i in 0 until div) {
            string.append(main.take(3))
            main = main.drop(3)

            if (i != div - 1) {
                string.append(",")
            }
        }

        if (deci.isEmpty().not() && roundUpDecimals) {

            if (expo.isEmpty().not()) {
                val e = expo.take(1)
                val frac = expo.drop(1).toInt()

                if (expo.drop(1).toInt() > 0) {
                    expo = "$e${frac.plus(deci.length - 3)}"
                }
//                else {
//                    expo = "$e${frac.minus(deci.length - 3)}"
//                }
            }

            deci = deci.take(3)

            if (deci.drop(2).toInt() > 5) {
                deci = deci.take(2) + deci.drop(2).toInt().inc()
            } else {
                deci = deci.take(2)
            }
        }

        if (deci.isEmpty().not()) {
            string.append(".")
        }

        string.append(deci)

        string.append(expo)

        return string.toString()
    }

    private fun calculate() {

        val amount =  amount.text.toString().replace(first_currency?.currencySymbol ?: "$", "", true)

        if (conversion_rate1 != 0.00 && amount.isEmpty().not()) {

            var res = (amount.toDouble() * conversion_rate1).toString()

            if (res.length > 20) {
                res = formatDecimal(res.toDouble())
            } else {
                res = formatDecimal(res.toDouble(), false)
            }


            if (res.length > 20) {
                result.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            } else if (res.length >= 17) {
                result.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            } else if (res.length >= 15) {
                result.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            }else if (res.length >= 13){
                result.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            } else if (res.length >= 10) {
                result.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            }

            //val sym = "${second_currency?.currencySymbol}"

            result.text = "${second_currency?.currencySymbol} $res"

            if (toggle_conversions.isEnabled.not()) {
                toggle_conversions.isEnabled = true
            }
        }
    }

    private fun setSelectedCurrency(textView: TextView, pos: Int) {
        val currencies_dialog: androidx.appcompat.app.AlertDialog

        currencies_dialog = androidx.appcompat.app.AlertDialog.Builder(context!!).setAdapter(CurrenciesAdapter(context!!)) { di, i ->

            val currency = currencies[i]
            textView.setText(currencyDisplayStringAt(i))

            if (pos == 1) {
                amount.text.clearSpans()
                amount.setText(amount.text.toString().replace(first_currency?.currencySymbol ?: "$", "", true))

                first_currency = currency

                formatAmountInputSymbol()
            } else if (pos == 2) {
                second_currency = currency
                result.text = "${currency.currencySymbol} 0.00"
            }

            di.dismiss()

            fetchConversionRates()
        }.create()

        currencies_dialog.listView.setOnItemLongClickListener { adapterView, view, i, l ->

            val currency = currencies[i]
            val currency_details = AlertDialog.Builder(context!!).setView(R.layout.currency_details).create()
            currency_details.show()

            currency_details.cur_id.text = currency.id ?: ""
            currency_details.cur_name.text = currency.name ?: ""
            currency_details.cur_currency_id.text = currency.currencyId ?: ""
            currency_details.cur_currency_name.text = currency.currencyName ?: ""
            currency_details.cur_currency_symbol.text = currency.currencySymbol ?: ""

            return@setOnItemLongClickListener true;
        }

        currencies_dialog.show()
    }

    fun fetchConversionRates() {

        if (first_currency == null || second_currency == null) return

        button_first_country.isEnabled = false
        button_second_country.isEnabled = false

        first_country_container.isEnabled = false
        second_country_container.isEnabled = false

        var snackbar = Snackbar.make(this.view!!, "Getting conversion rates...", Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction("Cancel") {
            viewModel.cancelFetchRequest()
        }
        snackbar.show()

        viewModel.fetchConversionRates(context!!, first_currency!!, second_currency!!).observe(this, object : Observer<Pair<Double, Double>> {
            override fun onChanged(t: Pair<Double, Double>?) {

                snackbar.dismiss()

                first_country_container.isEnabled = true
                second_country_container.isEnabled = true

                button_first_country.isEnabled = true
                button_second_country.isEnabled = true

                if (t == null) return;

                conversion_rate1 = t.first
                conversion_rate2 = t.second

                if (conversion_rate1 == 0.00 && conversion_rate2 == 0.00) {
                    snackbar = Snackbar.make(view!!, "Failed to get conversion rates...", Snackbar.LENGTH_LONG)
                    snackbar.setAction("Retry") {
                        fetchConversionRates()
                    }

                    snackbar.duration = 5000

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        snackbar.setActionTextColor(resources.getColor(R.color.colorAccent, context!!.theme))
                    } else {
                        snackbar.setActionTextColor(resources.getColor(R.color.colorAccent))
                    }

                    snackbar.show()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        toggle_conversions.backgroundTintList = resources.getColorStateList(R.color.countries_background_toggle_error, context!!.theme)
                    } else {
                        toggle_conversions.backgroundTintList = resources.getColorStateList(R.color.countries_background_toggle_error)
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        toggle_conversions.backgroundTintList = resources.getColorStateList(R.color.countries_background_toggle_success, context!!.theme)
                    } else {
                        toggle_conversions.backgroundTintList = resources.getColorStateList(R.color.countries_background_toggle_success)
                    }

                    calculate()

                    setConvertRate()
                }
            }
        })
    }

    fun currencyDisplayStringAt(pos: Int): String {
        val cur = currencies[pos]
        return "${cur.name} (${cur.currencyId})"
    }

    inner class CurrenciesAdapter(context: Context) : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1) {
        override fun getCount(): Int {
            return currencies.size
        }

        override fun getItem(position: Int): String {
            return currencyDisplayStringAt(position)
        }
    }
}
