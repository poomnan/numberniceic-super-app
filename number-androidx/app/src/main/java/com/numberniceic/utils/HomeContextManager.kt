package com.numberniceic.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.numberniceic.R

class HomeContextManager {

    companion object {

        private val num = "0123456789/"
        fun checkString(homeNum: String): Boolean {

            if (homeNum.isNotEmpty() && homeNum.length <= 7) {

                for (s in homeNum) {

                    var x = 0
                    for (n in this.num) {
                        if (n.toString() == s.toString()) {
                            x++

                        }

                    }

                    if (x == 0) return true

                }

                return false
            }
            return true
        }

        fun meeSlat(homeNumber: String): String? {
            var homeNumN: String? = null

            for (num in homeNumber) {
                if (num.toString() == "/") {
                    homeNumN = homeNumber.replace(num.toString(), "")

                }

            }
            return homeNumN
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

    }


    }