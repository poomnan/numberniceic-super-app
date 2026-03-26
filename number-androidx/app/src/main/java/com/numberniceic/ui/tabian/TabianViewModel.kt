package com.numberniceic.ui.tabian

import androidx.lifecycle.ViewModel
import com.numberniceic.data.apicollectiondao.TabianCollectionDao
import com.numberniceic.data.repos.TabianRepository
import com.numberniceic.data.tabian.PairsMiracle
import com.numberniceic.data.tabian.PercentTabianDR
import com.numberniceic.utils.TabianContextManager
import java.text.NumberFormat

class TabianViewModel : ViewModel() {

    val tabianObs = TabianObs()

    fun addTabianRepo(dao: TabianCollectionDao) {
        TabianRepository.addTabianDao(dao)
        showUi(dao)

    }

    private fun showUi(dao: TabianCollectionDao) {
        getCarId()
        getTabianScoreD()
        getTabianScoreR()
        getPairSumAll()
        getPairSumSecond()
        getTxtNumRoamAll()
        getTxtNumReang()
        getTxtNumSecond()

        getConTxtA()
        getConTxtB()



        tabianObs.sumTotalPercentD = dao.sumTotalPercent!!.sumPercentD.toString()
        tabianObs.sumTotalPercentR = dao.sumTotalPercent.sumPercentR.toString()

        tabianObs.miracleD = dao.miracleSummary!!.miracleD!!
        tabianObs.miracleR = dao.miracleSummary.miracleR!!

        tabianObs.vip = TabianContextManager.getVip(dao.scoreTotalD)

    }




    private fun getConTxtB() {
        tabianObs.txtConB = "ดีต่อเนื่อง " + getGradePair(TabianRepository.getCarPairB()!!.size, TabianRepository.getPairConB()).toString() + "%"
    }

    private fun getConTxtA() {
        tabianObs.txtConA = "ดีต่อเนื่อง " + getGradePair(TabianRepository.getCarPairA()!!.size, TabianRepository.getPairConA()).toString() + "%"
    }

    private fun getGradePair(pairsSize: Int, countx: Int): Int {
        return ((100.00 / pairsSize) * countx).toInt()

    }


    fun getCarPairA(): ArrayList<String> {
        return TabianRepository.getCarPairA()!!
    }

    fun getCarPairB(): ArrayList<String> {
        return TabianRepository.getCarPairB()!!
    }

    fun getPairMiracle(): ArrayList<PairsMiracle>? {
        return TabianRepository.getPairMiracle()
    }

    private fun getTxtNumSecond() {
        tabianObs.txtNumSecond = TabianRepository.getTxtNumSecond()
    }

    private fun getTxtNumReang() {
        val percentA = getGradePair(TabianRepository.getCarPairA()!!.size, TabianRepository.getPairConA())
        val percentB = getGradePair(TabianRepository.getCarPairB()!!.size, TabianRepository.getPairConB())
        val percentAB = (percentA + percentB) / 2

        val report = when {
            percentAB >= 80 -> "ดีเยี่ยม" //d20
            percentAB >= 70 -> "ดีมาก" //17
            percentAB >= 60 -> "ดี"     //14
            percentAB >= 50 -> "เสี่ยงมาก" //11
            percentAB >= 40 -> "อันตราย" //8
            percentAB >= 30 -> "อันตรายมาก" //5
            else -> "เปลี่ยนทันที"
        }


        tabianObs.txtNumReang = report
    }

    private fun getTxtNumRoamAll() {
        tabianObs.txtNumRoam = TabianRepository.getTxtNumRoamAll()
    }


    private fun getPairSumSecond() {
        val pair = TabianRepository.getCarPairSumSecond().toString()
        tabianObs.pairSumSecond = pair
        var bg = 0
        for (item in TabianRepository.getPairMiracle()!!) {
            if (pair == item.pairnumber && item.pairtype!![0].toString() == "D") {
                bg = 1
            }
        }
        tabianObs.bgPairSecond = bg
    }

    private fun getPairSumAll() {
        val pair = TabianRepository.getCarPairSumAll().toString()
        tabianObs.pairSumAll = pair
        var bg = 0
        for (item in TabianRepository.getPairMiracle()!!) {

            if (pair == item.pairnumber && pair == "25") {
                bg = 2
            } else if (pair == item.pairnumber && pair == "26") {
                bg = 3
            } else if (pair == item.pairnumber && pair == "23" || pair == "32" || pair == "35" || pair == "53") {
                bg = 4
            } else if (pair == item.pairnumber && item.pairtype!![0].toString() == "D") {
                bg = 1
            }
        }
        tabianObs.bgPairAll = bg
    }


    private fun getTabianScoreD() {
        tabianObs.tabianScoreD = NumberFormat.getInstance().format(TabianRepository.getScoreDV2()).toString()
    }

    private fun getTabianScoreR() {
        tabianObs.tabianScoreR = NumberFormat.getInstance().format(TabianRepository.getScoreRV2()).toString()
    }

    private fun getCarId() {


        val tabianFront = TabianContextManager.converFormatTabianMud(TabianRepository.getCarId().toString())

        val tabianSecond = TabianContextManager.converFormatTabianLast(TabianRepository.getCarId().toString())

        val numFirst = TabianContextManager.firstNumber(tabianFront)
        val meeDat = TabianContextManager.affterNumDat(tabianFront)

        val pairNumm = TabianContextManager.pairNumMud(meeDat)

        tabianObs.edtTabian = "$tabianFront-$tabianSecond"
        tabianObs.edtTabianMud = tabianFront
        tabianObs.txtNumMud = numFirst
        tabianObs.acsonMud = meeDat


        var mudReportStatus = 0

        val grade: Int?
        grade = when {
            meeDat.length == 2 -> TabianRepository.getGradeNumMudSingle(meeDat)
            meeDat.length == 3 -> TabianRepository.getGradeNumMudDuo(pairNumm)
            else -> 0
        }


            tabianObs.plepudStrMud = getMessageSpecial(grade)
            tabianObs.plepudStrMudColor = grade

            if (grade >=4) mudReportStatus++


        if (!numFirst.isEmpty()){
            val gradeNumFirst = TabianRepository.getGradeNumMudSingle(numFirst)
            tabianObs.pleNumMud = getMessageSpecial(gradeNumFirst)
            tabianObs.pleNumMudColor = gradeNumFirst

            if (gradeNumFirst >=4) mudReportStatus++



        }else{
            tabianObs.pleNumMud = "ไม่มีเลขนำหมวด"
            tabianObs.pleNumMudColor = 9
            mudReportStatus++
        }


        tabianObs.imgNumReangMud = if (mudReportStatus > 1) 1 else 0


    }


    private fun getMessageSpecial(grade: Int): String {
        return when (grade) {
            0 -> "อันตรายมาก"
            1 -> "เสี่ยงมาก"
            4 -> "ดีเยี่ยม"

            10 -> "ดีเยี่ยม"
            8 -> "ดีมาก"
            5 -> "ดีเยี่ยม"
            -10 -> "อันตรายมาก"
            -7 -> "อันตราย"
            -5 -> "เสี่ยงมาก"
            else -> "x"
        }

    }





}