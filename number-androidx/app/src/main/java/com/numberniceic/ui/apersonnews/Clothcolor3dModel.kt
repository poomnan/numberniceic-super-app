package com.numberniceic.ui.apersonnews

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.ViewModel
import com.numberniceic.R
import com.numberniceic.data.persons.DressColorCollection
import com.numberniceic.utils.OutfitApiColorMappingManager
import com.numberniceic.utils.PersonContextManager
import com.numberniceic.utils.UserContextManager
import org.joda.time.DateTime
import java.util.Locale

class Clothcolor3dModel: ViewModel() {
    var clothColor3dObs = ClothColor3dObs()

    private lateinit var sri: String


    init {
        initUi()
    }

    private fun initUi() {
        clothColor3dObs.clothColorTitle = getCurrentDatePlus(1)
        clothColor3dObs.clothColorTitleX1 = getCurrentDatePlus(2)
        clothColor3dObs.clothColorTitleX2 = getCurrentDatePlus(3)

    }

    fun getStatusWednesday():Boolean{
        val presentDateTime = DateTime()


        val dayEng = presentDateTime.dayOfWeek().getAsText(java.util.Locale.ENGLISH)
        val datex = PersonContextManager.convertDayThToEng(presentDateTime.dayOfWeek().asText)
        val presentDateConvertToEng = if(datex != "") datex else dayEng

        return presentDateConvertToEng == "Wednesday"

    }

    fun getColorSortxD(dressColorCollection: DressColorCollection, context: Context): ArrayList<ColorStateList>? {

        var colorx: ArrayList<ColorStateList>? = null

        if (dressColorCollection.clothColors != null && dressColorCollection.clothColors.isNotEmpty()) {
            val fallback = arrayListOf<ColorStateList>()
            
            // 🎨 Step 1: Collect unique shades in the sorted order (Item first, then Shade)
            val allCodes = mutableListOf<String>()
            for (item in dressColorCollection.clothColors) {
                for (i in 1..4) {
                    val code = when (i) {
                        1 -> item.colorCode1
                        2 -> item.colorCode2
                        3 -> item.colorCode3
                        4 -> item.colorCode4
                        else -> null
                    }
                    if (!code.isNullOrBlank()) allCodes.add(code)
                }
            }

            if (allCodes.isNotEmpty()) {
                // 🎨 Step 2: Render only available colors, leave remaining slots empty in UI
                for (code in allCodes) {
                    parseColorCodeFlexible(code, context)?.let { parsedColor ->
                        fallback.add(ColorStateList.valueOf(parsedColor))
                    }
                }
                colorx = fallback
            }
        }
        return colorx
    }

    fun getColorSortxR(dressColorCollection: DressColorCollection, context: Context): ArrayList<ColorStateList>? {
        var colorx: ArrayList<ColorStateList>? = null
        if (dressColorCollection.clothColors != null && dressColorCollection.clothColors.isNotEmpty()) {
            val fallback = arrayListOf<ColorStateList>()
            
            // 🎨 Step 1: Collect unique shades in the sorted order
            val allCodes = mutableListOf<String>()
            for (item in dressColorCollection.clothColors) {
                for (i in 1..4) {
                    val code = when (i) {
                        1 -> item.colorCode1
                        2 -> item.colorCode2
                        3 -> item.colorCode3
                        4 -> item.colorCode4
                        else -> null
                    }
                    if (!code.isNullOrBlank()) allCodes.add(code)
                }
            }

            if (allCodes.isNotEmpty()) {
                for (code in allCodes) {
                    parseColorCodeFlexible(code, context)?.let { parsedColor ->
                        fallback.add(ColorStateList.valueOf(parsedColor))
                    }
                }
                colorx = fallback
            }
        }
        return colorx

    }

    private fun parseColorCodeFlexible(code: String, context: Context): Int? {
        return try {
            Color.parseColor(code)
        } catch (_: Exception) {
            val hex = OutfitApiColorMappingManager.resolveHex(context, code.trim()) ?: return null
            try {
                Color.parseColor(hex)
            } catch (_: Exception) {
                null
            }
        }
    }


    fun getColorCloth(dayOfWeeEng: String, dayBirth: String?, dayBirthNumber: Int, typeColor: String, context:Context): StringBuilder? {


        var kaliBirthDayx: String? = null

        var ageYang: Int? = null
        val userx = UserContextManager.userX(context)
        if (userx != null) {
            val birthday: DateTime? = PersonContextManager.parseBirthdayOrNull(userx.birthDay)
            if (birthday != null) {
                ageYang = PersonContextManager.ageYang(birthday.year, birthday.monthOfYear, birthday.dayOfMonth)

            }
        }

        if (ageYang != null) {
            //หากาลีปีเกิด
            kaliBirthDayx = this.kaliBirth(ageYang, dayBirthNumber)

            if (kaliBirthDayx != null) {
                Log.d("kaliBirthDayx", kaliBirthDayx)

                val kaliSri = hashMapOf(
                    "1" to "7", "2" to "5", "3" to "8", "4" to "6",
                    "5" to "2", "6" to "4", "7" to "1", "8" to "3"
                )

                for ((kali, sri) in kaliSri) {
                    if (kali == kaliBirthDayx) {
                        this.sri = sri
                    }
                }
                Log.d("SRI", this.sri)
            } else {
                return null
            }
        }
        val dayMasters = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Saturday", "Thursday", "Wednesday2", "Friday")
        val dayNumPositionsMap = hashMapOf("1" to "Sunday", "2" to "Monday", "3" to "Tuesday", "4" to "Wednesday", "7" to "Saturday", "5" to "Thursday", "8" to "Wednesday2", "6" to "Friday")
        val dayAntis = arrayListOf<String>()

        for ((n, dayMaster) in dayMasters.withIndex()) {

            //หากาลีวันเกิด
            if (dayMaster == dayBirth) {
                if (dayMaster == "Sunday") dayAntis.add(dayMasters.last()) else dayAntis.add(dayMasters[n - 1])
            }

            Log.d("dayOfWeeEng", dayOfWeeEng)
            //หากาลีวันปัจุบัน
            if (dayMaster == dayOfWeeEng) {

                if (dayMaster == "Sunday") dayAntis.add(dayMasters.last()) else dayAntis.add(dayMasters[n - 1])
            }
        }


        val mapDayAnti = hashMapOf<String, String>()
        for ((key, value) in dayNumPositionsMap) {
            for (dayAnti in dayAntis) {
                if (dayAnti == value) {
                    mapDayAnti[key] = value
                }
            }
        }


        val listColorCloth = arrayListOf<String>()
        //ศรี
        listColorCloth.addAll(PersonContextManager.listNumberDayColorx(dayOfWeeEng, dayBirth, 0))
        //เดช
        listColorCloth.addAll(PersonContextManager.listNumberDayColorx(dayOfWeeEng, dayBirth, 1))
        //มนตรี
        listColorCloth.addAll(PersonContextManager.listNumberDayColorx(dayOfWeeEng, dayBirth, 2))
        //มูล
        listColorCloth.addAll(PersonContextManager.listNumberDayColorx(dayOfWeeEng, dayBirth, 3))
        //อายุ
        listColorCloth.addAll(PersonContextManager.listNumberDayColorx(dayOfWeeEng, dayBirth, 4))
        //บริวาร
        listColorCloth.addAll(PersonContextManager.listNumberDayColorx(dayOfWeeEng, dayBirth, 5))
        //อุตสาหะ
        listColorCloth.addAll(PersonContextManager.listNumberDayColorx(dayOfWeeEng, dayBirth, 6))

        val numClothColorDistinct = arrayListOf<String>()

        numClothColorDistinct.addAll(listColorCloth.distinct())


        val arrStrRDistint = arrayListOf<String>()

        arrStrRDistint.add(kaliBirthDayx!!)

        for ((k, _) in mapDayAnti) {
            arrStrRDistint.add(k)
        }

        // Explicitly remove all inauspicious colors from the positive list to ensure no overlaps
        numClothColorDistinct.removeAll(arrStrRDistint.toSet())

        if (userx != null) {
            val listNumberDayStr = StringBuilder()
            val sridee = arrayListOf<String>()

            sridee.addAll(numClothColorDistinct)

            when (typeColor) {
                "d" -> {
                    for (s in sridee.distinct()) {
                        listNumberDayStr.append(s)
                    }
                    return listNumberDayStr
                }

                "r" -> {
                    arrStrRDistint.distinct()
                    val listNumberDayStrAnti = StringBuilder()
                    for (arr in arrStrRDistint.distinct()) {
                        listNumberDayStrAnti.append(arr)
                    }
                    return listNumberDayStrAnti
                }
            }
        }

        return null
    }


    private fun kaliBirth(ageYang: Int, dayBirthNumber: Int): String? {


        val dayYangAntis = mapOf("1" to "6", "0" to "7", "2" to "1", "3" to "2", "4" to "3", "7" to "4", "5" to "7", "8" to "5", "6" to "8")
        val dayYangPos = arrayListOf("1", "0", "2", "3", "4", "7", "5", "8", "6", "1", "0", "2", "3", "4", "7", "5", "8", "6")


        var antifinal: String? = null
        val arrNumStr = arrayListOf<String>()

        Log.d("ageYang", ageYang.toString())

        for (n in ageYang.toString()) {
            arrNumStr.add(n.toString())
        }

        var numPlus = 0
        for (x in arrNumStr) {
            numPlus += x.toInt()
        }

        val arrNumStrf = arrayListOf<String>()
        for (n in numPlus.toString()) {
            arrNumStrf.add(n.toString())
        }

        var numPlusf = 0
        for (x in arrNumStrf) {
            numPlusf += x.toInt()
        }

        //จำนวนอายุที่ต้องนับ
        Log.d("numPlusf", numPlusf.toString())
        Log.d("dayBirthNumber", dayBirthNumber.toString())

        var posMapAnti: String? = null
        loop@ for ((n, v) in dayYangPos.withIndex()) {
            if (v == dayBirthNumber.toString()) {
                posMapAnti = dayYangPos[(n - 1) + numPlusf]

                break@loop
            }
        }


        if (posMapAnti == "0") {
            antifinal = "7"
        } else {
            for ((k, v) in dayYangAntis) {

                if (k == posMapAnti) {
                    antifinal = v
                }
            }
        }

        Log.d("kaliBirth", antifinal!!)

        return antifinal
    }


    fun getCurrentDatePlus(daynum:Int):String {
        val dt = DateTime()
        val dtTarget = dt.plusDays(daynum)
        val dayLabelEng = dtTarget.dayOfWeek().getAsText(java.util.Locale.ENGLISH)
        val thaiDay = PersonContextManager.toThaiDay(dayLabelEng)
        val day = if (thaiDay == "") dtTarget.dayOfWeek().asText else thaiDay
        val dtWithOffset = dt.plusDays(daynum)
        val monthLabelEng = dtWithOffset.monthOfYear().getAsText(java.util.Locale.ENGLISH)
        val month = if (PersonContextManager.toThaiMonth(monthLabelEng)[1] == "") PersonContextManager.monthTH2FullTH(dtWithOffset.monthOfYear().asText)[1] else PersonContextManager.toThaiMonth(monthLabelEng)[1]
        val dayStringBuilder = StringBuilder()

        dayStringBuilder.append("สีเสื้อผ้า ")
        dayStringBuilder.append("$day ที่ ")
        dayStringBuilder.append("${dt.plusDays(daynum).dayOfMonth().asText} ")
        dayStringBuilder.append("$month ")
        dayStringBuilder.append((dt.plusDays(daynum).year().asText.toInt() + 543).toString())

        return dayStringBuilder.toString()

    }

    fun getLogicExplanation(context: Context): String {
        try {
            val userx = UserContextManager.userX(context) ?: return ""
            val birthday = PersonContextManager.parseBirthdayOrNull(userx.birthDay) ?: return ""
            val ageYang = PersonContextManager.ageYang(birthday.year, birthday.monthOfYear, birthday.dayOfMonth) ?: return ""

            val sHour = userx.sHour
            val dayNumBirth = PersonContextManager.convertDayEngToNum(birthday.dayOfWeek().getAsText(Locale.ENGLISH))
            val dayBirthNumber = if (sHour <= 4) {
                if (dayNumBirth == 1) 7 else dayNumBirth - 1
            } else {
                dayNumBirth
            }

            val kaliBirthDayx = kaliBirth(ageYang, dayBirthNumber) ?: "1"
            val dayBirthEng = PersonContextManager.convertDayNumToEng(dayBirthNumber) ?: "Sunday"
            
            val dayMasters = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Saturday", "Thursday", "Wednesday2", "Friday")
            val dayNumPositionsMap = hashMapOf("1" to "Sunday", "2" to "Monday", "3" to "Tuesday", "4" to "Wednesday", "7" to "Saturday", "5" to "Thursday", "8" to "Wednesday2", "6" to "Friday")
            
            var kaliBirthDayEng = "Friday"
            for ((n, dayMaster) in dayMasters.withIndex()) {
                if (dayMaster == dayBirthEng) {
                    kaliBirthDayEng = if (dayMaster == "Sunday") dayMasters.last() else dayMasters[n - 1]
                    break
                }
            }
            
            var kaliBirthDayNum = "1"
            for ((k, v) in dayNumPositionsMap) {
                if (v == kaliBirthDayEng) {
                    kaliBirthDayNum = k
                    break
                }
            }

            val birthDayThai = getThaiDayName(dayBirthEng)

            val explanation = StringBuilder()
            explanation.append("วิธีคิดการร่อนสีเฉพาะคุณ (หลักทักษา):\n")
            explanation.append("1. กาลีอายุย่าง ($ageYang ปี): ห้ามใช้สีของดาว").append(getThaiStarColor(kaliBirthDayx)).append("\n")
            explanation.append("2. กาลีวันเกิด (คนเกิดวัน").append(birthDayThai).append("): ห้ามใช้สีของดาว").append(getThaiStarColor(kaliBirthDayNum)).append("\n")
            explanation.append("3. กาลีประจำวันแต่งตัว: ห้ามใช้สีของดาวที่เป็นกาลีในวันนั้นๆ\n")
            explanation.append("💡 ระบบจะนำสีมงคลของแต่ละวัน มาหักลบด้วยสีกาลีทั้ง 3 ข้อด้านบน เพื่อให้ได้ \"สีมงคลที่ปลอดภัย 100%\" สำหรับคุณ")
            
            return explanation.toString()
        } catch (e: Exception) {
            return ""
        }
    }

    private fun getThaiDayName(dayEng: String): String {
        return when (dayEng) {
            "Sunday" -> "อาทิตย์"
            "Monday" -> "จันทร์"
            "Tuesday" -> "อังคาร"
            "Wednesday" -> "พุธ(กลางวัน)"
            "Wednesday2" -> "พุธ(กลางคืน)"
            "Thursday" -> "พฤหัสบดี"
            "Friday" -> "ศุกร์"
            "Saturday" -> "เสาร์"
            else -> ""
        }
    }

    private fun getThaiStarColor(starNum: String): String {
        return when (starNum) {
            "1" -> "อาทิตย์ (สีแดง)"
            "2" -> "จันทร์ (สีเหลือง/ขาว)"
            "3" -> "อังคาร (สีชมพู)"
            "4" -> "พุธ (สีเขียว)"
            "5" -> "พฤหัสบดี (สีส้ม/น้ำตาล)"
            "6" -> "ศุกร์ (สีฟ้า/น้ำเงิน)"
            "7" -> "เสาร์ (สีม่วง/ดำ)"
            "8" -> "ราหู (สีเทาเข้ม)"
            else -> ""
        }
    }


}
