package com.numberniceic.utils

import android.text.Editable

class PhoneContextManager {



    companion object {
        fun getFormatPhoneNumber(phoneNumber: String): String {

            var succesNumber = ""

            if (phoneNumber.length == 10){
                val c0 = phoneNumber[0]
                val c1 = phoneNumber[1]
                val c2 = phoneNumber[2]
                val c3 = phoneNumber[3]
                val c4 = phoneNumber[4]
                val c5 = phoneNumber[5]
                val c6 = phoneNumber[6]
                val c7 = phoneNumber[7]
                val c8 = phoneNumber[8]
                val c9 = phoneNumber[9]

                succesNumber = "$c0$c1-$c2$c3$c4$c5-$c6$c7$c8$c9"

            }


            return succesNumber

        }


    }


    }