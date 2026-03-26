package com.numberniceic.ui.admin

import android.graphics.Color
import androidx.lifecycle.ViewModel
import com.numberniceic.R


class BagColorModel : ViewModel() {

    var bagColorObs: BagColorObs = BagColorObs()

    init {
        initUi()
    }

    private fun initUi() {

    }

    fun setOnSave(save: Boolean) {
        bagColorObs.onSave = save

        bagColorObs.msgSave = if (save) "บันทึกแล้ว" else "ยังไม่บันทึก"
        bagColorObs.imgSave = if (save) 1 else 0


    }

    fun setUserDayOfWeek(dayOfWeek: String){
        bagColorObs.userDayOfWeek = dayOfWeek
    }

    fun setNextYear(year: String) {
        bagColorObs.nextYear = year
    }


    fun setCurrentYear(year: String) {
        bagColorObs.currentYear = year
    }

    fun setUserAge(age: String) {
        bagColorObs.userAge = age
    }

    fun setUserName(userName: String) {
        bagColorObs.userName = userName
    }


    fun setColorChip0(colorStr: String) {
        bagColorObs.colorChip0 = Color.parseColor(colorStr)
    }

    fun setColorChip1(colorStr: String) {
        bagColorObs.colorChip1 = Color.parseColor(colorStr)
    }

    fun setColorChip2(colorStr: String) {
        bagColorObs.colorChip2 = Color.parseColor(colorStr)
    }

    fun setColorChip3(colorStr: String) {
        bagColorObs.colorChip3 = Color.parseColor(colorStr)
    }

    fun setColorChip4(colorStr: String) {
        bagColorObs.colorChip4 = Color.parseColor(colorStr)
    }

    fun setColorChip5(colorStr: String) {
        bagColorObs.colorChip5 = Color.parseColor(colorStr)
    }


    fun setColorChipB0(colorStr: String) {
        bagColorObs.colorChipb0 = Color.parseColor(colorStr)
    }

    fun setColorChipB1(colorStr: String) {
        bagColorObs.colorChipb1 = Color.parseColor(colorStr)
    }

    fun setColorChipB2(colorStr: String) {
        bagColorObs.colorChipb2 = Color.parseColor(colorStr)
    }

    fun setColorChipB3(colorStr: String) {
        bagColorObs.colorChipb3 = Color.parseColor(colorStr)
    }

    fun setColorChipB4(colorStr: String) {
        bagColorObs.colorChipb4 = Color.parseColor(colorStr)
    }

    fun setColorChipB5(colorStr: String) {
        bagColorObs.colorChipb5 = Color.parseColor(colorStr)
    }

    fun setUserId(userId: String) {
        bagColorObs.userId = userId
    }

    fun getColorCodeChip0(): Int {
        return bagColorObs.colorChip0
    }

    fun getColorCodeChip1(): Int {
        return bagColorObs.colorChip1
    }

    fun getColorCodeChip2(): Int {
        return bagColorObs.colorChip2
    }

    fun getColorCodeChip3(): Int {
        return bagColorObs.colorChip3
    }

    fun getColorCodeChip4(): Int {
        return bagColorObs.colorChip4
    }

    fun getColorCodeChip5(): Int {
        return bagColorObs.colorChip5
    }

    fun getColorCodeChipB0(): Int {
        return bagColorObs.colorChipb0
    }

    fun getColorCodeChipB1(): Int {
        return bagColorObs.colorChipb1
    }

    fun getColorCodeChipB2(): Int {
        return bagColorObs.colorChipb2
    }

    fun getColorCodeChipB3(): Int {
        return bagColorObs.colorChipb3
    }

    fun getColorCodeChipB4(): Int {
        return bagColorObs.colorChipb4
    }

    fun getColorCodeChipB5(): Int {
        return bagColorObs.colorChipb5
    }
    fun setLastUpdateA(date: String) {
        bagColorObs.lastUpdateA = if (date.isEmpty()) "ยังไม่ update" else "บันทึก: $date"
    }

    fun setLastUpdateB(date: String) {
        bagColorObs.lastUpdateB = if (date.isEmpty()) "ยังไม่ update" else "บันทึก: $date"
    }
}