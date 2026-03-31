package com.numberniceic.utils

import android.util.Log
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.Years

class PersonContextManager {

    companion object {


        fun ageYang(year:Int, month:Int, day:Int):Int? {
            val ageNext = this.getUsersAge(year, month, day)

            /*Log.d("AGEXYear", ageNext.year.toString())
            Log.d("AGEXMonth", ageNext.monthOfYear.toString())
            Log.d("AGEXDay", ageNext.dayOfMonth.toString())*/

            /*if (ageNext.monthOfYear >= 0 && ageNext.dayOfMonth >= 0) {
                return ageNext.year + 1
            }*/

            return ageNext + 1

        }

        fun ageCurrent(year:Int, month:Int, day:Int):Int? {
            return getUsersAge(year, month, day)

        }

        fun parseBirthdayOrNull(rawBirthDay: String?): DateTime? {
            if (rawBirthDay.isNullOrBlank()) return null

            return try {
                DateTime.parse(rawBirthDay.trim())
            } catch (e: Exception) {
                Log.w("PersonContextManager", "Invalid birthday format: $rawBirthDay", e)
                null
            }
        }




        private fun getUsersAge(years: Int, months: Int, days: Int): Int {

            val birthDay = LocalDate(years, months, days)


            val now = LocalDate()

            val age = Years.yearsBetween(birthDay, now)

            //return LocalDate.now().minusYears(years).minusMonths(months).minusDays(days)
            return age.years


        }



        fun convertDayEngToNum(dayBirth: String?):Int{
            return when (dayBirth) {
                "Sunday" -> 1
                "Monday" -> 2
                "Tuesday" -> 3
                "Wednesday" -> 4
                "Thursday" -> 5
                "Friday" -> 6
                "Saturday" -> 7
                else -> 0
            }
        }

        fun convertDayThToEng(dayBirth: String?):String{
            return when (dayBirth) {
                "วันอาทิตย์" -> "Sunday"
                "วันจันทร์" -> "Monday"
                "วันอังคาร" -> "Tuesday"
                "วันพุธ" -> "Wednesday"
                "วันพฤหัสบดี" -> "Thursday"
                "วันศุกร์" -> "Friday"
                "วันเสาร์" -> "Saturday"
                else -> ""
            }
        }

        fun convertDayNumToEng(dayBirthNumber:Int):String?{
            return when (dayBirthNumber) {
                1 -> "Sunday"
                2 -> "Monday"
                3 -> "Tuesday"
                4 -> "Wednesday"
                5 -> "Thursday"
                6 -> "Friday"
                7 -> "Saturday"
                else -> null

            }
        }

        fun convertDayNumToThai(dayBirthNumber:Int):String?{
            return when (dayBirthNumber) {
                7 -> "วันอาทิตย์"
                1 -> "วันจันทร์"
                2 -> "วันอังคาร"
                3 -> "วันพุธ"
                4 -> "วันพฤหัสบดี"
                5 -> "วันศุกร์"
                6 -> "วันเสาร์"
                else -> null

            }
        }

        fun convertDayNumToEngJoda(dayBirthNumber:Int):String?{
            return when (dayBirthNumber) {
                0 -> "Sunday"
                1 -> "Monday"
                2 -> "Tuesday"
                3 -> "Wednesday"
                4 -> "Thursday"
                5 -> "Friday"
                6 -> "Saturday"
                else -> null

            }
        }

        fun convertDayNumToTH(dayBirthNumber:Int):String?{
            return when (dayBirthNumber) {
                1 -> "ปางวันอาทิตย์"
                2 -> "ปางวันจันทร์"
                3 -> "ปางวันอังคาร"
                4 -> "ปางวันพุธ"
                5 -> "ปางวันพฤหัส"
                6 -> "ปางวันศุกร์"
                7 -> "ปางวันเสาร์"
                8 -> "ปางวันพุธกลางคืน"
                else -> null

            }
        }

        fun convertDayNumToEngV2(numDay:Int):String?{

            return when (numDay) {
                1 -> "Monday"
                2 -> "Tuesday"
                3 -> "Wednesday"
                4 -> "Thursday"
                5 -> "Friday"
                6 -> "Saturday"
                7 -> "Sunday"
                else -> null

            }
        }


        fun dupArrayNumber(numClothColorDistinct: ArrayList<String>, mapDayAnti: HashMap<String, String>): Int? {
            for ((n, numday) in numClothColorDistinct.withIndex()) {
                for ((antiK, _) in mapDayAnti) {
                    if (numday == antiK) {
                        return n

                    }
                }
            }

            return null
        }

        fun dupArrayNumberYang(numClothColorDistinct: ArrayList<String>, dayAnti:  String): Int? {
            for ((n, numday) in numClothColorDistinct.withIndex()) {

                if (numday == dayAnti) {
                    return n

                }

            }

            return null
        }

        fun listNumberDayColorx(dayOfWeekEng:String, dayBirth:String?, positonList: Int):List<String>{

            val listDays = arrayListOf<String>()

            val sundays = listOf("4", "3", "8", "7", "2", "1", "5")

            val mondays = listOf("7", "4", "6", "5", "3", "2", "8")

            val tuesdays = listOf("5", "7", "1", "8", "4", "3", "6")

            val wednesdays = listOf("8", "5", "2", "6", "7", "4", "1")

            val thursdays = listOf("1", "6", "4", "2", "8", "5", "3")

            val fridays = listOf("3", "2", "5", "4", "1", "6", "7")

            val saturdays = listOf("6", "8", "3", "1", "5", "7", "2")

            val wednesdays2 = listOf("2", "1", "7", "3", "6", "8", "4")


            //Log.d("dayOfWeekx", dayOfWeekx)

            when (dayOfWeekEng) {
                "Sunday", "วันอาทิตย์" -> {

                    listDays.add(sundays[positonList])

                }
                "Monday", "วันจันทร์" -> {

                    listDays.add(mondays[positonList])

                }
                "Tuesday", "วันอังคาร" -> {

                    listDays.add(tuesdays[positonList])

                }
                "Wednesday", "วันพุธ" -> {

                    listDays.add(wednesdays[positonList])

                }
                "Thursday", "วันพฤหัสบดี" -> {

                    listDays.add(thursdays[positonList])

                }
                "Friday", "วันศุกร์" -> {

                    listDays.add(fridays[positonList])

                }
                "Saturday", "วันเสาร์" -> {

                    listDays.add(saturdays[positonList])

                }
            }

            when (dayBirth) {
                "Sunday" -> {

                    listDays.add(sundays[positonList])

                }

                "Monday" -> {

                    listDays.add(mondays[positonList])

                }

                "Tuesday" -> {

                    listDays.add(tuesdays[positonList])

                }

                "Wednesday" -> {
                    listDays.add(wednesdays[positonList])

                }

                "Thursday" -> {

                    listDays.add(thursdays[positonList])

                }

                "Friday" -> {

                    listDays.add(fridays[positonList])

                }

                "Saturday" -> {

                    listDays.add(saturdays[positonList])

                }

                "Wednesday2" -> {

                    listDays.add(wednesdays2[positonList])

                }

            }



            return listDays

        }

        fun toThaiDay(dayEng: String):String {

            return when(dayEng){
                "Sunday" -> "วันอาทิตย์"
                "Monday" -> "วันจันทร์"
                "Tuesday" -> "วันอังคาร"
                "Wednesday" -> "วันพุธ"
                "Thursday" -> "วันพฤหัสบดี"
                "Friday" -> "วันศุกร์"
                "Saturday" -> "วันเสาร์"
                else -> ""
            }

        }

        fun dayThaiToEng(dayTH: String):String {

            return when(dayTH){
                "วันอาทิตย์" -> "Sunday"
                "วันจันทร์" -> "Monday"
                "วันอังคาร" -> "Tuesday"
                "วันพุธ" -> "Wednesday"
                "วันพฤหัสบดี" -> "Thursday"
                "วันศุกร์" -> "Friday"
                "วันเสาร์" -> "Saturday"
                else -> ""
            }

        }

        fun toThaiMonth(monthEng: String):List<String> {
            return when(monthEng){
                "January" -> listOf("มกราคม", "ม.ค")
                "February" -> listOf("กุมภาพันธ์", "ก.พ")
                "March" -> listOf("มีนาคม", "มี.ค")
                "April" -> listOf("เมษายน", "เม.ย")
                "May" -> listOf("พฤษภาคม", "พ.ค")
                "June" -> listOf("มิถุนายน", "มิ.ย")
                "July" -> listOf("กรกฎาคม", "ก.ค")
                "August" -> listOf("สิงหาคม", "ส.ค")
                "September" -> listOf("กันยายน", "ก.ย")
                "October" -> listOf("ตุลาคม", "ต.ค")
                "November" -> listOf("พฤศจิกายน", "พ.ย")
                "December" -> listOf("ธันวาคม", "ธ.ค")
                else -> listOf("","")
            }

        }
        fun monthTH2FullTH(monthEng: String):List<String> {
            return when(monthEng){
                "มกราคม" -> listOf("มกราคม", "ม.ค")
                "กุมภาพันธ์" -> listOf("กุมภาพันธ์", "ก.พ")
                "มีนาคม" -> listOf("มีนาคม", "มี.ค")
                "เมษายน" -> listOf("เมษายน", "เม.ย")
                "พฤษภาคม" -> listOf("พฤษภาคม", "พ.ค")
                "มิถุนายน" -> listOf("มิถุนายน", "มิ.ย")
                "กรกฎาคม" -> listOf("กรกฎาคม", "ก.ค")
                "สิงหาคม" -> listOf("สิงหาคม", "ส.ค")
                "กันยายน" -> listOf("กันยายน", "ก.ย")
                "ตุลาคม" -> listOf("ตุลาคม", "ต.ค")
                "พฤศจิกายน" -> listOf("พฤศจิกายน", "พ.ย")
                "ธันวาคม" -> listOf("ธันวาคม", "ธ.ค")
                else -> listOf("","")
            }

        }
    }
}
