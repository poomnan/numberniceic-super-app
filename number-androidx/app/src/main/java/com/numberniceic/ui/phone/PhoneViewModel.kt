package com.numberniceic.ui.phone

import android.graphics.Color
import androidx.lifecycle.ViewModel
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao

import com.numberniceic.data.repos.PhoneRepository
import java.text.NumberFormat
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhoneViewModel : ViewModel() {


    var phoneObserve = PhoneObs()
    val isLoading = MutableLiveData<Boolean>()


    init {
        initUI()
    }

    fun addPhoneData(dao: PhoneCollectionDao) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.Default) {
            PhoneRepository.addPhoneData(dao)
            preloadPairColors(dao)
            withContext(Dispatchers.Main) {
                showUI(dao)
                isLoading.value = false
            }
        }
    }


    private fun showUI(dao: PhoneCollectionDao) {


        getSummaraReport(dao)

        getImgReport(dao)
        getScoreD()
        getScoreR()
        getPercentD()
        getPercentR()

        getPairSum()
        getPairsAp()
        getPairsBp()

        getBgPairsAp(dao)
        getBgPairsBp(dao)
        getBgpairSum(dao)

        getTxtPairSumColor()
        getTxtPairsAColor()
        getTxtPairsBColor()

        getGradePairSum(dao)
        getGradeConA(dao)
        getGradeConB(dao)

        getImgPairSum(dao)
        getImgPairLast(dao)
        getImgPairCon(dao)

    }



    private fun getSummaraReport(dao: PhoneCollectionDao) {



        phoneObserve.reportSummaryD = dao.miracleSummary!!.miracleD!!
        phoneObserve.reportSummaryR = dao.miracleSummary.miracleR!!
    }


    private fun getImgReport(dao: PhoneCollectionDao) {
        phoneObserve.imgReportD = when {
            dao.scoreTotalOfTotal!!.scoreTotalD >= 1700 -> 9
            dao.scoreTotalOfTotal.scoreTotalD > 1000 -> 1
            dao.scoreTotalOfTotal.scoreTotalD <= 0 -> 0
            else -> 0
        }

        phoneObserve.imgReportR = when {
            dao.scoreTotalOfTotal.scoreTotalR < 0 -> 2
            dao.scoreTotalOfTotal.scoreTotalR == 0 -> 1
            else -> 0
        }


    }

    private fun getImgPairLast(dao: PhoneCollectionDao) {
        phoneObserve.txtPairLast = when (dao.specialGoal!!.specialPairLast) {
            "A" -> "ดีเยี่ยม"
            "B" -> "ดีมาก"
            "C" -> "ดี"
            "D" -> "พอใช้"
            else -> {
                "อันตราย"
            }
        }
    }

    private fun getImgPairCon(dao: PhoneCollectionDao) {
        phoneObserve.txtPairCon = when {
            dao.scoreByContinueCountPairsA!!.pairContinueD >= 3 && dao.scoreByContinueCountPairsB!!.pairContinueD >= 2 -> "ดีเยี่ยม"
            dao.scoreByContinueCountPairsA.pairContinueD >= 2  && dao.scoreByContinueCountPairsB!!.pairContinueD >= 1 -> "ปานกลาง"
            dao.scoreByContinueCountPairsA.pairContinueD == 0  && dao.scoreByContinueCountPairsB!!.pairContinueD == 0  -> "อันตราย"
            dao.scoreByContinueCountPairsA.pairContinueD == 0  || dao.scoreByContinueCountPairsB!!.pairContinueD == 0  -> "พอใช้"


            else -> {
               return
            }
        }
    }


    private fun getImgPairSum(dao: PhoneCollectionDao) {

        phoneObserve.txtPairSum = when (dao.specialGoal!!.specialPairSum) {
            "A" -> "ดีเยี่ยม"
            "B" -> "ดีมาก"
            "C" -> "ดี"
            "D" -> "พอใช้"
            else -> {
                "อันตราย"
            }
        }
    }


    private fun getGradePairSum(dao: PhoneCollectionDao) {
        for (pair in dao.dataSortByType!!) {

            val typeSpecial = if(pair.number=="60") "R4" else "x"

            if (typeSpecial == "R4"){
                phoneObserve.gradePairSum = "พอใช้"
            }else
            if (pair.number == dao.pairSum) {
                phoneObserve.gradePairSum = when (pair.type) {
                    "D10" -> "ดีเยี่ยม"
                    "D8" -> "ดีมาก"
                    "D5" -> "ดี"
                    else -> {


                        when(pair.type){
                            "R10" -> "อันตรายมาก"
                            "R7" -> "อันตราย"
                            "R5" -> "อันตราย"

                            else -> {
                                return
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getGradeConA(dao: PhoneCollectionDao) {
        phoneObserve.gradePairsA = when(dao.scoreByContinueCountPairsA!!.pairContinueD){
            1 -> "เสี่ยงมาก"
            2 -> "เสี่ยง"
            3 -> "พอใช้"
            4 -> "ดี"
            5 -> "ดีเยี่ยม"
            else -> {
                when(dao.scoreByContinueCountPairsA.pairContinueR){
                    1 -> "เสี่ยงมาก"
                    2 -> "อันตราย"
                    3 -> "อันตราย"
                    4 -> "อันตรายมาก"
                    5 -> "อันตรายมาก"
                    else -> {
                        return
                    }
                }
            }
        }

    }

    private fun getGradeConB(dao: PhoneCollectionDao) {
        phoneObserve.gradePairsB = when(dao.scoreByContinueCountPairsB!!.pairContinueD){
            1 -> "เสี่ยง"
            2 -> "พอใช้"
            3 -> "ดี"
            4 -> "ดีเยี่ยม"
            else -> {
                when(dao.scoreByContinueCountPairsB.pairContinueR){
                    1 -> "พอใช้"
                    2 -> "อันตราย"
                    3 -> "อันตรายมาก"
                    4 -> "อันตรายมาก"
                    else -> {
                        return
                    }
                }
            }
        }

    }


    private fun getScoreD() {
        phoneObserve.scoreD = NumberFormat.getInstance().format(PhoneRepository.getScoreD()).toString()

    }

    private fun getScoreR() {
        phoneObserve.scoreR = NumberFormat.getInstance().format(PhoneRepository.getScoreR()).toString()

    }

    private fun getPercentD() {
        phoneObserve.percentD = PhoneRepository.getPercentD().toString()

    }

    private fun getPercentR() {
        phoneObserve.percentR = PhoneRepository.getPercentR().toString()

    }

    private fun getPairSum() {
        phoneObserve.pairSum = PhoneRepository.getPairSum()
    }

    private fun getPairsAp() {
        phoneObserve.pairAp1 = PhoneRepository.getPairAp1()
        phoneObserve.pairAp2 = PhoneRepository.getPairAp2()
        phoneObserve.pairAp3 = PhoneRepository.getPairAp3()
        phoneObserve.pairAp4 = PhoneRepository.getPairAp4()
        phoneObserve.pairAp5 = PhoneRepository.getPairAp5()
    }

    private fun getPairsBp() {
        phoneObserve.pairBp1 = PhoneRepository.getPairBp1()
        phoneObserve.pairBp2 = PhoneRepository.getPairBp2()
        phoneObserve.pairBp3 = PhoneRepository.getPairBp3()
        phoneObserve.pairBp4 = PhoneRepository.getPairBp4()
    }

    private fun getBgPairsAp(dao: PhoneCollectionDao) {
        phoneObserve.pairAp1BgColor = getPairBgColor(dao, PhoneRepository.getPairAp1())
        phoneObserve.pairAp2BgColor = getPairBgColor(dao, PhoneRepository.getPairAp2())
        phoneObserve.pairAp3BgColor = getPairBgColor(dao, PhoneRepository.getPairAp3())
        phoneObserve.pairAp4BgColor = getPairBgColor(dao, PhoneRepository.getPairAp4())
        phoneObserve.pairAp5BgColor = getPairBgColor(dao, PhoneRepository.getPairAp5())

    }

    private fun getBgPairsBp(dao: PhoneCollectionDao) {
        phoneObserve.pairB1BgColor = getPairBgColor(dao, PhoneRepository.getPairBp1())
        phoneObserve.pairB2BgColor = getPairBgColor(dao, PhoneRepository.getPairBp2())
        phoneObserve.pairB3BgColor = getPairBgColor(dao, PhoneRepository.getPairBp3())
        phoneObserve.pairB4BgColor = getPairBgColor(dao, PhoneRepository.getPairBp4())

    }

    private fun getBgpairSum(dao: PhoneCollectionDao) {
        phoneObserve.pairSumBgColor = getPairBgColor(dao, PhoneRepository.getPairSum())
    }

    private fun getTxtPairSumColor() {
        phoneObserve.pairSumTxtColor = Color.parseColor("#FFFFFF")
    }

    private fun getTxtPairsAColor() {
        val color = "#FFFFFF"
        phoneObserve.pairAp1TxtColor = Color.parseColor(color)
        phoneObserve.pairAp2TxtColor = Color.parseColor(color)
        phoneObserve.pairAp3TxtColor = Color.parseColor(color)
        phoneObserve.pairAp4TxtColor = Color.parseColor(color)
        phoneObserve.pairAp5TxtColor = Color.parseColor(color)
    }

    private fun getTxtPairsBColor() {
        val color = "#FFFFFF"
        phoneObserve.pairBp1TxtColor = Color.parseColor(color)
        phoneObserve.pairBp2TxtColor = Color.parseColor(color)
        phoneObserve.pairBp3TxtColor = Color.parseColor(color)
        phoneObserve.pairBp4TxtColor = Color.parseColor(color)

    }


    fun initUI() {
        phoneObserve.reportSummaryD = ""
        phoneObserve.reportSummaryR = ""

        phoneObserve.imgReportD = 0
        phoneObserve.imgReportR = 0

        phoneObserve.txtPairSum = ""
        phoneObserve.txtPairCon = ""
        phoneObserve.txtPairLast = ""

        phoneObserve.gradePairSum = "?"
        phoneObserve.gradePairsA = "?"
        phoneObserve.gradePairsB = "?"

        phoneObserve.scoreD = "0"
        phoneObserve.scoreR = "0"

        phoneObserve.percentD = "0"
        phoneObserve.percentR = "0"

        phoneObserve.pairSum = ""
        phoneObserve.pairAp1 = ""
        phoneObserve.pairAp2 = ""
        phoneObserve.pairAp3 = ""
        phoneObserve.pairAp4 = ""
        phoneObserve.pairAp5 = ""

        phoneObserve.pairBp1 = ""
        phoneObserve.pairBp2 = ""
        phoneObserve.pairBp3 = ""
        phoneObserve.pairBp4 = ""

        phoneObserve.edtPhone = ""

        resetBgPairsAB()
    }


    private fun resetBgPairsAB() {

        val colorReset = "#FFFFFF"

        phoneObserve.pairAp1BgColor = Color.parseColor(colorReset)
        phoneObserve.pairAp2BgColor = Color.parseColor(colorReset)
        phoneObserve.pairAp3BgColor = Color.parseColor(colorReset)
        phoneObserve.pairAp4BgColor = Color.parseColor(colorReset)
        phoneObserve.pairAp5BgColor = Color.parseColor(colorReset)

        phoneObserve.pairB1BgColor = Color.parseColor(colorReset)
        phoneObserve.pairB2BgColor = Color.parseColor(colorReset)
        phoneObserve.pairB3BgColor = Color.parseColor(colorReset)
        phoneObserve.pairB4BgColor = Color.parseColor(colorReset)

        phoneObserve.pairSumBgColor = Color.parseColor(colorReset)

    }


    private val pairBgColorCache = HashMap<String, Int>()

    private fun preloadPairColors(dao: PhoneCollectionDao) {
        pairBgColorCache.clear()
        if (dao.dataSortByType != null) {
            for (pairMiracle in dao.dataSortByType) {
                if (pairMiracle.number != null && pairMiracle.type != null) {
                    var bgPair = Color.TRANSPARENT // Default
                    if (pairMiracle.type[0].toString() == "D") {
                        bgPair = Color.parseColor("#3a9900")
                    } else {
                        when (pairMiracle.type) {
                            "R10" -> bgPair = Color.parseColor("#D13124")
                            "R7" -> bgPair = Color.parseColor("#FF5722")
                            "R5" -> bgPair = Color.parseColor("#FF9800")
                        }
                    }
                    pairBgColorCache[pairMiracle.number] = bgPair
                }
            }
        }
    }

    //global function
    private fun getPairBgColor(dao: PhoneCollectionDao, pair: String): Int {
        return pairBgColorCache[pair] ?: Color.TRANSPARENT
    }

}