package com.numberniceic.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.numberniceic.data.tabian.PairCharNumber
import com.numberniceic.R

class TabianContextManager {
    companion object {


            fun getVip(scoreTotalD: Int?) : Int{
            return when{
                scoreTotalD!! >= 1800 -> 9
                scoreTotalD >= 1500 -> 0
                scoreTotalD >= 1000 -> 0
                else -> 0

            }
        }


        fun getColorBG(type: String, context: Context): Int? {
            var color: Int? = null

            if (type == "D") {
                color = ContextCompat.getColor(context, R.color.colorPairD)
            }
            if (type == "R") {
                color = ContextCompat.getColor(context, R.color.colorPairR)

            }
            return color
        }


        private fun getDaoCharTabian(): ArrayList<PairCharNumber> {

            val mNumbeCharList: ArrayList<PairCharNumber> = arrayListOf()

            mNumbeCharList.add(PairCharNumber("ก", "1"))
            mNumbeCharList.add(PairCharNumber("ฃ", "3"))
            mNumbeCharList.add(PairCharNumber("ข", "2"))
            mNumbeCharList.add(PairCharNumber("ฆ", "3"))
            mNumbeCharList.add(PairCharNumber("ค", "4"))
            mNumbeCharList.add(PairCharNumber("ฉ", "5"))
            mNumbeCharList.add(PairCharNumber("จ", "6"))
            mNumbeCharList.add(PairCharNumber("ซ", "7"))
            mNumbeCharList.add(PairCharNumber("ผ", "8"))
            mNumbeCharList.add(PairCharNumber("ฏ", "9"))
            mNumbeCharList.add(PairCharNumber("a", "1"))
            mNumbeCharList.add(PairCharNumber("A", "1"))
            mNumbeCharList.add(PairCharNumber("b", "2"))
            mNumbeCharList.add(PairCharNumber("B", "2"))
            mNumbeCharList.add(PairCharNumber("c", "3"))
            mNumbeCharList.add(PairCharNumber("C", "3"))
            mNumbeCharList.add(PairCharNumber("d", "4"))
            mNumbeCharList.add(PairCharNumber("D", "4"))
            mNumbeCharList.add(PairCharNumber("e", "5"))
            mNumbeCharList.add(PairCharNumber("E", "5"))
            mNumbeCharList.add(PairCharNumber("u", "6"))
            mNumbeCharList.add(PairCharNumber("U", "6"))
            mNumbeCharList.add(PairCharNumber("o", "7"))
            mNumbeCharList.add(PairCharNumber("O", "7"))
            mNumbeCharList.add(PairCharNumber("f", "8"))
            mNumbeCharList.add(PairCharNumber("F", "8"))

            mNumbeCharList.add(PairCharNumber("ด", "1"))
            mNumbeCharList.add(PairCharNumber("ง", "2"))
            mNumbeCharList.add(PairCharNumber("ต", "3"))
            mNumbeCharList.add(PairCharNumber("ธ", "4"))
            mNumbeCharList.add(PairCharNumber("ฌ", "5"))
            mNumbeCharList.add(PairCharNumber("ล", "6"))
            mNumbeCharList.add(PairCharNumber("ศ", "7"))
            mNumbeCharList.add(PairCharNumber("ฝ", "8"))
            mNumbeCharList.add(PairCharNumber("ฐ", "9"))
            mNumbeCharList.add(PairCharNumber("i", "1"))
            mNumbeCharList.add(PairCharNumber("I", "1"))
            mNumbeCharList.add(PairCharNumber("k", "2"))
            mNumbeCharList.add(PairCharNumber("K", "2"))
            mNumbeCharList.add(PairCharNumber("g", "3"))
            mNumbeCharList.add(PairCharNumber("G", "3"))
            mNumbeCharList.add(PairCharNumber("m", "4"))
            mNumbeCharList.add(PairCharNumber("M", "4"))
            mNumbeCharList.add(PairCharNumber("h", "5"))
            mNumbeCharList.add(PairCharNumber("H", "5"))
            mNumbeCharList.add(PairCharNumber("v", "6"))
            mNumbeCharList.add(PairCharNumber("V", "6"))
            mNumbeCharList.add(PairCharNumber("z", "7"))
            mNumbeCharList.add(PairCharNumber("Z", "7"))
            mNumbeCharList.add(PairCharNumber("p", "8"))
            mNumbeCharList.add(PairCharNumber("P", "8"))

            mNumbeCharList.add(PairCharNumber("ถ", "1"))
            mNumbeCharList.add(PairCharNumber("ช", "2"))
            mNumbeCharList.add(PairCharNumber("ฑ", "3"))
            mNumbeCharList.add(PairCharNumber("ญ", "4"))
            mNumbeCharList.add(PairCharNumber("ณ", "5"))
            mNumbeCharList.add(PairCharNumber("ว", "6"))
            mNumbeCharList.add(PairCharNumber("ส", "7"))
            mNumbeCharList.add(PairCharNumber("พ", "8"))
            mNumbeCharList.add(PairCharNumber("ไ", "9"))
            mNumbeCharList.add(PairCharNumber("j", "1"))
            mNumbeCharList.add(PairCharNumber("J", "1"))
            mNumbeCharList.add(PairCharNumber("r", "2"))
            mNumbeCharList.add(PairCharNumber("R", "2"))
            mNumbeCharList.add(PairCharNumber("l", "3"))
            mNumbeCharList.add(PairCharNumber("L", "3"))
            mNumbeCharList.add(PairCharNumber("t", "4"))
            mNumbeCharList.add(PairCharNumber("T", "4"))
            mNumbeCharList.add(PairCharNumber("n", "5"))
            mNumbeCharList.add(PairCharNumber("N", "5"))
            mNumbeCharList.add(PairCharNumber("w", "6"))
            mNumbeCharList.add(PairCharNumber("W", "6"))

            mNumbeCharList.add(PairCharNumber("ท", "1"))
            mNumbeCharList.add(PairCharNumber("บ", "2"))
            mNumbeCharList.add(PairCharNumber("ฒ", "3"))
            mNumbeCharList.add(PairCharNumber("ร", "4"))
            mNumbeCharList.add(PairCharNumber("น", "5"))
            mNumbeCharList.add(PairCharNumber("อ", "6"))
            mNumbeCharList.add(PairCharNumber("๊", "7"))
            mNumbeCharList.add(PairCharNumber("ฟ", "8"))
            mNumbeCharList.add(PairCharNumber("์", "9"))
            mNumbeCharList.add(PairCharNumber("q", "1"))
            mNumbeCharList.add(PairCharNumber("Q", "1"))

            mNumbeCharList.add(PairCharNumber("s", "3"))
            mNumbeCharList.add(PairCharNumber("S", "3"))
            mNumbeCharList.add(PairCharNumber("x", "5"))
            mNumbeCharList.add(PairCharNumber("X", "5"))

            mNumbeCharList.add(PairCharNumber("ภ", "1"))
            mNumbeCharList.add(PairCharNumber("ป", "2"))
            mNumbeCharList.add(PairCharNumber("๋", "3"))
            mNumbeCharList.add(PairCharNumber("ษ", "4"))
            mNumbeCharList.add(PairCharNumber("ม", "5"))
            mNumbeCharList.add(PairCharNumber("ใ", "6"))
            mNumbeCharList.add(PairCharNumber("ี", "7"))
            mNumbeCharList.add(PairCharNumber("ย", "8"))

            mNumbeCharList.add(PairCharNumber("ฤ", "1"))
            mNumbeCharList.add(PairCharNumber("เ", "2"))

            mNumbeCharList.add(PairCharNumber("ะ", "4"))
            mNumbeCharList.add(PairCharNumber("ห", "5"))

            mNumbeCharList.add(PairCharNumber("ื", "7"))
            mNumbeCharList.add(PairCharNumber("็", "8"))

            mNumbeCharList.add(PairCharNumber("ฦ", "1"))
            mNumbeCharList.add(PairCharNumber("แ", "2"))

            mNumbeCharList.add(PairCharNumber("โ", "4"))
            mNumbeCharList.add(PairCharNumber("ฎ", "5"))


            mNumbeCharList.add(PairCharNumber("ำ", "1"))
            mNumbeCharList.add(PairCharNumber("้", "2"))

            mNumbeCharList.add(PairCharNumber("ิ", "4"))
            mNumbeCharList.add(PairCharNumber("ฬ", "5"))
            mNumbeCharList.add(PairCharNumber("ฮ", "5"))


            mNumbeCharList.add(PairCharNumber("ุ", "1"))

            mNumbeCharList.add(PairCharNumber("ึ", "5"))
            mNumbeCharList.add(PairCharNumber("่", "1"))


            return mNumbeCharList
        }

        fun convertFormatTabian(tabianNum: String): String {

            val nThai = "กขฃคฅฆงจฉชซฌญฎฏฐฑฒณดตถทธนบปผฝพฟภมยรลวศษสหฬอฮ"
            val nNumber = "0123456789"

            var status = 0

            var a = ""
            val mid = StringBuilder()
            var b = ""

            //6กฮ6459

            for ((i, t) in tabianNum.withIndex()) {
                //i = 0


                for (n in nThai) {
                    if (t == n) {

                        // a = 6-ก
                        a = if (i == 0) tabianNum.substring(0, i) else if (i == 1) tabianNum.substring(0, i - 1) else if (i == 2) tabianNum.substring(0, i - 1) + "-" else tabianNum.substring(0, i)

                        // find middle
                        t@ for (x in i..(tabianNum.length - 1)) {
                            num@ for (number in nNumber) {
                                if (tabianNum[x] == number && status == 0) {
                                    status = 1
                                    b = "-" + tabianNum.substring(x, tabianNum.length)
                                    break@num
                                }
                            }

                            if (status == 1) break@t
                        }
                    } else {

                    }
                }
            }


            for (tabianC in tabianNum) {
                for (thai in nThai) {
                    if (thai == tabianC) {
                        mid.append(thai)
                    }
                }
            }

            val stringCompNum = a + mid.toString() + b
            // 6-กฮ-6459

            val tabianNumPeal = StringBuilder()

            for (strC in stringCompNum) {

                var mee = 0

                loop@ for (cObj in this.getDaoCharTabian()) {
                    if (strC == cObj.mChar[0]) {

                        mee = 1
                        tabianNumPeal.append(cObj.mNumber[0])

                        break@loop

                    }
                }

                if (mee == 0) tabianNumPeal.append(strC)
            }



            return tabianNumPeal.toString()

        }

        fun converFormatTabianMud(tabianNum: String): String {

            val realRean = StringBuilder()


            val nThai = "กขฃคฅฆงจฉชซฌญฎฏฐฑฒณดตถทธนบปผฝพฟภมยรลวศษสหฬอฮ"
            for ((n, str) in tabianNum.withIndex()) {

                strThai@ for (nthai in nThai) {
                    if (str == nthai) {
                        realRean.append(getStrToNum(str))
                        break@strThai

                    } else {
                        if (n == 0 && getStrToNum(str) == "x") {
                            realRean.append("$str-")
                            break@strThai
                        }

                    }
                }
            }

            return realRean.toString()

        }

        fun converFormatTabianLast(tabianNum: String): String {

            val realReal = StringBuilder()

            val nNumber = "0123456789"

            for (n in tabianNum.length - 1 downTo 1) {

                listnumber@ for (number in nNumber) {
                    if (number == tabianNum[n]) {
                        realReal.append(number)
                        break@listnumber
                    }
                }


            }

            val sortNumDesc = StringBuilder()

            for (poNumber in realReal.length - 1 downTo 0) {
                sortNumDesc.append(realReal[poNumber])
            }

            return sortNumDesc.toString()

        }


        fun firstNumber(mud: String): String {

            var fNum = ""
            for ((i, m) in mud.withIndex()) {
                if (m.toString() == "-") {
                    fNum = mud[i - 1].toString()
                }
            }

            return fNum
        }


        fun affterNumDat(mud: String): String {
            val afNum = StringBuilder()

            var meeDat: Int? = null

            for ((i, item) in mud.withIndex()) {
                if (item.toString() == "-") {
                    meeDat = i

                    break
                }
            }

            if (meeDat != null) {
                for (m in (meeDat + 1)..(mud.length - 1)) {
                    afNum.append(mud[m])

                }
            } else {
                for (m in mud) {
                    afNum.append(m)
                }
            }


            return afNum.toString()
        }


        fun pairNumMud(numMud: String): ArrayList<String> {
            val pairNumList = arrayListOf<String>()
            val pairFirst = StringBuilder()
            val pairSecond = StringBuilder()

            if (numMud.length == 3) {

                for ((n, num) in numMud.withIndex()) {

                    if (n == 0) {
                        pairFirst.append(num)
                    }

                    if (n == 1) {
                        pairFirst.append(num)
                        pairSecond.append(num)
                    }

                    if (n == 2) {

                        pairSecond.append(num)
                    }
                }

            }

            pairNumList.add(pairFirst.toString())
            pairNumList.add(pairSecond.toString())

            return pairNumList
        }

        private fun getStrToNum(str: Char): String {
            for (charThai in getDaoCharTabian()) {
                if (str.toString() == charThai.mChar) {
                    return charThai.mNumber
                }
            }

            return "x"
        }


        fun checkTabianNumber(tabian: String): Boolean {
            val nThai = "กขคฆงจฉชซฌญฎฏฐฑฒณดตถทธนบปผฝพฟภมยรลวศษสหฬอฮ"
            val nNumber = "0123456789"
            var checkBolt1 = false
            var checkBolt2 = false

            var countStr = 0



            if (tabian.length > 1) {

                if (tabian.length == 2) {
                    for (n in nNumber) {
                        if (n == tabian[0]) {
                            return false
                        }
                    }
                }


                if (tabian.length == 3) {
                    for (t in nThai) {
                        if (tabian[2] == t) {
                            return false

                        }
                    }
                }
                if (tabian.length == 4) {
                    for (t in nThai) {
                        if (tabian[3] == t) {
                            return false

                        }
                    }
                }

                if (tabian.length == 5) {
                    for (t in nThai) {
                        if (tabian[4] == t) {
                            return false

                        }
                    }


                }

                if (tabian.length == 7) {
                    for (t in nThai) {
                        if (tabian[6] == t || tabian[5] == t || tabian[4] == t || tabian[3] == t) {

                            return false

                        }
                    }

                }

                if (tabian.length == 6) {
                    for (t in nThai) {
                        if (tabian[5] == t || tabian[4] == t || tabian[3] == t) {
                            return false

                        }
                    }

                }


                if (tabian.length >= 2) {




                    for (ct in nThai){
                        for (ctb in tabian){
                            if (ct == ctb){
                                countStr++
                            }
                        }
                    }


                    ta@ for (t in nThai) {
                        str@ for (c in tabian) {
                            if (c == t) {
                                checkBolt1 = true

                                break@str
                            }


                        }

                        if (checkBolt1) break@ta
                    }

                    nn@ for (n in nNumber) {
                        num@ for (c in tabian) {
                            if (c == n) {
                                checkBolt2 = true
                                break@num
                            }

                        }

                        if (checkBolt2) break@nn
                    }


                }


            }

            if (checkBolt1 && checkBolt2 && countStr==2) {
                return true
            }


            return false

        }


    }
}