package com.numberniceic.ui.namenick

import androidx.lifecycle.ViewModel
import com.numberniceic.data.apicollectiondao.HomeCollectionDao
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao
import com.numberniceic.data.repos.HomeRepository
import com.numberniceic.data.repos.NameNickRepository

class NameNickViewModel : ViewModel() {

    val nickNameObs = NameNickObs()


    fun addNickNameDao(dao: NickNameCollectionDao) {
        NameNickRepository.addNickNameDao(dao)
        this.showUI()
    }

    private fun showUI() {

        setBirthDay()
        setPairSatNickName()
        setKName()
        setSatNickName()
        setShaNickName()

        setGradeSatNickname()
        setGradeShaNickname()

    }

    private fun setGradeSatNickname() {

        var grade = ""
        val pairs = NameNickRepository.pairsMiracle()

        var bgColor = 0



        for (pair in pairs!!) {

            if (pair.pairnumber!!.toInt() < 100) {
                if (pair.pairnumber == NameNickRepository.sumSatNickName().toString()) {

                    bgColor = if (pair.pairnumber == "26" || pair.pairnumber == "62" || pair.pairnumber == "23" || pair.pairnumber == "32" || pair.pairnumber == "40" || pair.pairnumber == "04") 0
                    else when (pair.pairtype) {
                        "D10" -> 1
                        "D8" -> 1
                        "D5" -> 1
                        "R10" -> 2
                        "R7" -> 2
                        "R5" -> 2
                        else -> 3

                    }

                    grade = if (pair.pairnumber == "26" || pair.pairnumber == "62" || pair.pairnumber == "23" || pair.pairnumber == "32" || pair.pairnumber == "40" || pair.pairnumber == "04") "ดีแต่เสี่ยง"
                    else when (pair.pairtype) {
                        "D10" -> "(ดีเยี่ยม)"
                        "D8" -> "(ดีมาก)"
                        "D5" -> "(ดี)"
                        "R10" -> "(อันตรายที่สุด)"
                        "R7" -> "(อันตราย)"
                        "R5" -> "(อันตราย)"
                        else -> ""

                    }
                }
            }

        }

        nickNameObs.gradeSatNickname = grade
        nickNameObs.bgColorSatSum = bgColor
    }

    private fun setGradeShaNickname() {
        var bgColor = 0
        var grade = ""
        val pairs = NameNickRepository.pairsMiracle()

        for (pair in pairs!!) {


            if (pair.pairnumber!!.toInt() < 100) {
                if (pair.pairnumber == NameNickRepository.sumShaNickName().toString()) {

                    bgColor = if (pair.pairnumber == "26" || pair.pairnumber == "62" || pair.pairnumber == "23" || pair.pairnumber == "32" || pair.pairnumber == "40" || pair.pairnumber == "04") 0
                    else when (pair.pairtype) {
                        "D10" -> 1
                        "D8" -> 1
                        "D5" -> 1
                        "R10" -> 2
                        "R7" -> 2
                        "R5" -> 2
                        else -> 3

                    }

                    grade = if (pair.pairnumber == "0") {
                        ""
                    } else {
                        when (pair.pairtype) {
                            "D10" -> "(ดีเยี่ยม)"
                            "D8" -> "(ดีมาก)"
                            "D5" -> "(ดี)"
                            "R10" -> "(อันตรายที่สุด)"
                            "R7" -> "(อันตราย)"
                            "R5" -> "(อันตราย)"
                            else -> ""

                        }
                    }


                }
            }
        }

        nickNameObs.gradeShaNickname = grade
        nickNameObs.bgColorShaSum = bgColor
    }

    private fun setShaNickName() {


        if (NameNickRepository.sumShaNickName() == 0) {
            nickNameObs.pairShaNickName = "-"
        } else {
            val mStringBuilder = StringBuilder()
            mStringBuilder.append("${NameNickRepository.nickname()} = ")
            mStringBuilder.append("${NameNickRepository.sumShaNickName()}")

            if (NameNickRepository.pairShaNickName()!!.fang != null) mStringBuilder.append(" แฝง ${NameNickRepository.pairShaNickName()!!.fang}")

            // mStringBuilder.append(" อายตนะ ${NameNickRepository.ayadNickName()}")

            nickNameObs.pairShaNickName = mStringBuilder.toString()
        }


    }

    private fun setSatNickName() {
        val mStringBuilder = StringBuilder()
        for (pair in NameNickRepository.satNickName()!!) {
            mStringBuilder.append("  ${pair.xNum.toString()}")

        }

        nickNameObs.satNickName = mStringBuilder.toString()

    }

    private fun setKName() {
        val mStringBuilder = StringBuilder()


        if (NameNickRepository.kName()!!.isEmpty()) mStringBuilder.append("ไม่มีตัวกาลกิณี")
        else {
            for (pair in NameNickRepository.kName()!!) {
                mStringBuilder.append("{   $pair } ")

            }

        }

        nickNameObs.kName = mStringBuilder.toString()
    }

    private fun setPairSatNickName() {
        val mStringBuilder = StringBuilder()

        mStringBuilder.append(" ${NameNickRepository.nickname()} = ")
        mStringBuilder.append("${NameNickRepository.sumSatNickName()}")
        if (NameNickRepository.pairSatNickName()!!.fang != null) mStringBuilder.append(" แฝง ${NameNickRepository.pairSatNickName()!!.fang}")

        nickNameObs.pairSatNickName = mStringBuilder.toString()
    }

    private fun setBirthDay() {
        nickNameObs.birthDay = when (NameNickRepository.birthDay()) {
            "monday" -> "จันทร์"
            "tuesday" -> "อังคาร"
            "wednesday1" -> "พุธกลางวัน"
            "wednesday2" -> "พุธกลางคืน"
            "thursday" -> "พฤหัสบดี"
            "friday" -> "ศุกร์"
            "saturday" -> "เสาร์"
            "sunday" -> "อาทิตย์"
            else -> "ไม่เลือกวัน"
        }
    }
}