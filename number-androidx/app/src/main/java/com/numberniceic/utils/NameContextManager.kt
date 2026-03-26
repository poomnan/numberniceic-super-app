package com.numberniceic.utils

import com.numberniceic.data.nickname.Day

class NameContextManager {

    companion object {


        fun getDayThaiTitle(dayNum: Int): String {



                when (dayNum) {
                    1 -> return "วันอาทิตย์"
                    2 -> return "วันจันทร์"
                    3 -> return "วันอังคาร"
                    4 -> return "วันพุธ(กลางวัน)"
                    5 -> return "วันพุธ(กลางคืน)"
                    6 -> return "วันพฤหัสบดี"
                    7 -> return "วันศุกร์"
                    8 -> return "วันเสาร์"

                }



            return ""
        }


    }
}