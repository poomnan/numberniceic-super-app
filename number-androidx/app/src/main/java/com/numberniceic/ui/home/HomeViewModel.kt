package com.numberniceic.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.numberniceic.data.apicollectiondao.HomeCollectionDao
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao
import com.numberniceic.data.repos.HomeRepository
import com.numberniceic.data.repos.PhoneRepository
import com.numberniceic.data.repos.TabianRepository
import com.numberniceic.data.tabian.PairsMiracle
import com.numberniceic.ui.phone.PhoneObs
import java.text.NumberFormat

class HomeViewModel : ViewModel() {
    var homeObs = HomeObs()


    init {
        initUI()
    }

    private fun initUI() {

    }

    fun addHomeDao(dao: HomeCollectionDao) {
        HomeRepository.addHomeDao(dao)
        this.showUI()
    }

    private fun showUI() {
        getHomeId()
        getScoreD()
        getScoreR()
        getPairSumNumber()
        getPairGradeSum()
        getPercentPairsA()
        getPercentPairsB()
        getGradeNumReang()

        getHomeMiracleDR()

    }

    private fun getHomeMiracleDR() {
        homeObs.miracleD = HomeRepository.getHomeMiracleD()!!
        homeObs.miracleR = HomeRepository.getHomeMiracleR()!!
    }

    private fun getGradeNumReang() {
        val percentA = getGradePair(HomeRepository.getPairA()!!.size, HomeRepository.getPairHomeConA())
        val percentB = getGradePair(HomeRepository.getPairB()!!.size, HomeRepository.getPairHomeConB())
        val percentAB = (percentA + percentB)/2

        val report = when {
            percentAB >= 80 -> "ดีเยี่ยม"
            percentAB >= 70 -> "ดีมาก"
            percentAB >= 60 -> "ดี"
            percentAB >= 50 -> "ดี"
            else -> "ร้าย"
        }


        homeObs.gradeNumReang = report
    }

    private fun getPercentPairsB() {

        homeObs.percentPairsB = "ดีต่อเนื่อง " + getGradePair(HomeRepository.getPairB()!!.size, HomeRepository.getPairHomeConB()).toString() + "%"
    }

    private fun getPercentPairsA() {
        homeObs.percentPairsA = "ดีต่อเนื่อง " + getGradePair(HomeRepository.getPairA()!!.size, HomeRepository.getPairHomeConA()).toString() + "%"
    }

    private fun getPairGradeSum() {
        for (item in HomeRepository.getPairMiracle()!!) {
            if (item.pairnumber == HomeRepository.getPairSumNumber()) {
                homeObs.pairGradeSum = when (item.pairtype) {
                    "D10" -> "ดีเยี่ยม"
                    "D8" -> "ดีมาก"
                    "D5" -> "ดี"
                    else -> "ร้าย"
                }
            }
        }
    }

    private fun getPairSumNumber() {
        homeObs.pairSumNumber = HomeRepository.getPairSumNumber().toString()

        for (item in HomeRepository.getPairMiracle()!!) {
            if (item.pairnumber == HomeRepository.getPairSumNumber()) {
                when {
                    item.pairtype!![0].toString() == "D" -> homeObs.bgPairSumNumber = 1
                    item.pairtype[0].toString() == "R" -> homeObs.bgPairSumNumber = 0
                    else -> homeObs.bgPairSumNumber = 0
                }
            }
        }

    }


    private fun getScoreR() {
        homeObs.scoreR = NumberFormat.getInstance().format(HomeRepository.getScoreRv2()).toString()

    }

    private fun getScoreD() {
        homeObs.scoreD = NumberFormat.getInstance().format(HomeRepository.getScoreDv2()).toString()

        homeObs.scoreImg = HomeRepository.getScoreDv2()
    }

    private fun getHomeId() {
        homeObs.homeId = HomeRepository.getHomeId()!!
    }

    private fun getGradePair(pairsSize: Int, countx: Int): Int {
        return ((100.00 / pairsSize) * countx).toInt()

    }
}