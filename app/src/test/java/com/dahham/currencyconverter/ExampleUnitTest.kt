package com.dahham.currencyconverter

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    fun formatDecimal(double: Double): String {
        val d = double.toString()

        val splits: List<String>
        var main: String
        var deci: String
        var expo = ""

        splits = double.toString().split(Regex("\\."), 2)
        main = splits[0]
        deci = splits[1]

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

        if (deci.isEmpty().not()) {
            string.append(".")

            if (expo.isEmpty().not()){
                expo = "${expo.take(1)}${expo.drop(1).toInt().plus(deci.length - 3)}"
            }
            deci = deci.take(3)

            if (deci.drop(2).toInt() > 5) {
                deci = deci.take(2) + deci.drop(2).toInt().inc()
            } else {
                deci = deci.take(2)
            }

            string.append(deci)
        }

        string.append(expo)

        return string.toString()
    }

    @Test
    fun addition_isCorrect() {
        val d: Double = 934809804.3784372292
        print(formatDecimal(d))

        assertEquals(4, 2 + 2)
    }

    @Test
    fun te() {
        var s:String? = null

        s = s ?: "Hello"

        print(s)
    }
}
