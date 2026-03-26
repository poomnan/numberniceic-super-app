package com.numberniceic.ui.namesur

import android.util.Log
import androidx.lifecycle.ViewModel
import com.numberniceic.data.apicollectiondao.NameSurnameCollectionDao
import com.numberniceic.data.nickname.CharNum

import com.numberniceic.data.repos.NameSurRepository


class NameSurViewModel : ViewModel() {

    val nameSurObs = NameSurObs()


    fun addNameSurObs(dao: NameSurnameCollectionDao) {
        NameSurRepository.addNameSurDao(dao)
        this.showUI()
    }

    private fun showUI() {
        setBirthDay()
        setSatName()
        setSatSurname()
        setKalakini()

        setSumNameSurname()
        setSumShaName()
        setSumShaSurname()
        setSumShaNameSurname()

    }


    private fun setSumShaName() {


        if (NameSurRepository.sumShaName() == 0) {
            nameSurObs.pairSumShaName = "-"
            nameSurObs.ayantanaName = ""
        } else {
            val mStringBuilder = StringBuilder()
            mStringBuilder.append("${NameSurRepository.name()}")
            mStringBuilder.append("(${NameSurRepository.sumShaName()})")
            if (NameSurRepository.sumShaName().toString().length == 3) {
                mStringBuilder.append(" (${NameSurRepository.pairShaName()!!.fang}, ${NameSurRepository.pairShaName()!!.pair})")

            }

            nameSurObs.pairSumShaName = mStringBuilder.toString()

            val ayanStringBuilder = StringBuilder()
            ayanStringBuilder.append("อายตนะ ${NameSurRepository.ayadName()}")
            nameSurObs.ayantanaName = ayanStringBuilder.toString()
        }


    }

    private fun setSumShaSurname() {

        if (NameSurRepository.sumShaSurName() == 0) {
            nameSurObs.pairSumShaSurname = "-"
            nameSurObs.ayantanaSurName = ""
        } else {
            val mStringBuilder = StringBuilder()
            mStringBuilder.append("${NameSurRepository.surname()}")
            mStringBuilder.append("(${NameSurRepository.sumShaSurName()})")
            if (NameSurRepository.sumShaSurName().toString().length == 3) mStringBuilder.append("(${NameSurRepository.pairShaSurName()!!.fang}, ${NameSurRepository.pairShaSurName()!!.pair})")

            nameSurObs.pairSumShaSurname = mStringBuilder.toString()

            val ayanStringBuilder = StringBuilder()

            ayanStringBuilder.append("อายตนะ ${NameSurRepository.ayadSurName()}")

            nameSurObs.ayantanaSurName = ayanStringBuilder.toString()
        }

    }


    private fun setSumShaNameSurname() {

        if (NameSurRepository.sumShaNameSurName() == 0) {
            nameSurObs.pairSumShaNameSurname = "-"
            nameSurObs.ayantanaNameSurname = ""
        } else {

            val mStringBuilder = StringBuilder()
            mStringBuilder.append("[ชื่อ + สกุล]")
            mStringBuilder.append("(${NameSurRepository.sumShaNameSurName()})")
            if (NameSurRepository.sumShaNameSurName().toString().length == 3) mStringBuilder.append("(${NameSurRepository.pairShaNameSurName()!!.fang}, ${NameSurRepository.pairShaNameSurName()!!.pair})")

            nameSurObs.pairSumShaNameSurname = mStringBuilder.toString()

            val ayanStringBuilder = StringBuilder()

            ayanStringBuilder.append("อายตนะ ${NameSurRepository.ayadNameSurName()}")

            nameSurObs.ayantanaNameSurname = ayanStringBuilder.toString()
        }
    }


    private fun setSumNameSurname() {
        val mStringBuilder = StringBuilder()

        mStringBuilder.append(" ${NameSurRepository.name()}")
        if (NameSurRepository.pairSatName()!!.fang != null) mStringBuilder.append("(แฝง ${NameSurRepository.pairSatName()!!.fang})")
        mStringBuilder.append("(${NameSurRepository.pairSatName()!!.pair})")
        mStringBuilder.append(" + ")
        mStringBuilder.append("${NameSurRepository.surname()}")
        if (NameSurRepository.pairSatSurName()!!.fang != null) mStringBuilder.append("(แฝง ${NameSurRepository.pairSatSurName()!!.fang})")
        mStringBuilder.append("(${NameSurRepository.pairSatSurName()!!.pair})")
        mStringBuilder.append(" = ")
        mStringBuilder.append("${NameSurRepository.pairSatNameSurName()!!.pair}")
        if (NameSurRepository.pairSatNameSurName()!!.fang != null) mStringBuilder.append("(แฝง ${NameSurRepository.pairSatNameSurName()!!.fang})")

        nameSurObs.pairSumSatNameSurname = mStringBuilder.toString()
    }

    private fun setKalakini() {
        val mStringBuilder = StringBuilder()

        if (NameSurRepository.kName()!!.isEmpty()) mStringBuilder.append("ไม่มีตัวกาลกิณี")
        else {
            for (pair in NameSurRepository.kName()!!) {
                mStringBuilder.append("{   $pair } ")
            }

        }

        nameSurObs.kName = mStringBuilder.toString()
    }

    private fun setSatSurname() {
        val mStringBuilder = StringBuilder()

        for (item: CharNum in NameSurRepository.satSurName()!!) {
            mStringBuilder.append("  ")
            mStringBuilder.append(item.xNum)
        }
        nameSurObs.satSurName = mStringBuilder.toString()
    }

    private fun setSatName() {
        val mStringBuilder = StringBuilder()
        for (item: CharNum in NameSurRepository.satName()!!) {
            mStringBuilder.append("  ")
            mStringBuilder.append(item.xNum)
        }
        nameSurObs.satName = mStringBuilder.toString()
    }

    private fun setBirthDay() {
        nameSurObs.birthDay = when (NameSurRepository.birthDay()) {
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