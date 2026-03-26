package com.numberniceic.data.repos

import com.numberniceic.data.apicollectiondao.HomeCollectionDao
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao
import com.numberniceic.data.tabian.PairsMiracle
import java.util.*

object HomeRepository {

    private var homeDao: HomeCollectionDao? = null

    fun addHomeDao(dao: HomeCollectionDao) {
        this.homeDao = dao
    }

    fun getHomeMiracleD():String?{
        return homeDao!!.homeReport!!.miracleD
    }

    fun getHomeMiracleR():String?{
        return homeDao!!.homeReport!!.miracleR
    }

    fun getHomeId():String?{
        return homeDao!!.homeId
    }

    fun getScoreRv2(): Int {
        return homeDao!!.scoreV2!!.scoreR
    }

    fun getScoreDv2(): Int {
        return homeDao!!.scoreV2!!.scoreD
    }

    fun getScoreR(): Int {
        return homeDao!!.scoreRD!!.scoreR
    }

    fun getScoreD(): Int {
        return homeDao!!.scoreRD!!.scoreD
    }

    fun getPairA(): ArrayList<String>?{
        return homeDao!!.pairsA
    }

    fun getPairB(): ArrayList<String>?{

        return homeDao!!.pairsB
    }

    fun getContinueA():Int{
        return homeDao!!.continueDR!!.conA
    }

    fun getContinueB():Int{
        return homeDao!!.continueDR!!.conB
    }

    fun getPairSumNumber(): String? {
        return homeDao!!.pairSumNumber
    }

    fun getPairUnique(): ArrayList<String>? {
        return homeDao!!.pairUnique
    }

    fun getPairMiracle(): ArrayList<PairsMiracle>? {
        return homeDao!!.pairMiracle
    }


    fun getPairHomeConA():Int{
        var conut = 0
        
        // สร้าง HashMap เพื่อเพิ่มความเร็วในการค้นหา
        val miracleMap = homeDao!!.pairMiracle!!.associateBy { it.pairnumber }
        
        loop@ for (x in (homeDao!!.pairsA!!.size downTo 1)) {
            val pairNumber = homeDao!!.pairsA!![x - 1]
            val miracle = miracleMap[pairNumber]
            
            if (miracle != null) {
                when (miracle.pairtype?.get(0)?.toString()) {
                    "D" -> conut++
                    "R" -> break@loop
                }
            }
        }

        return conut
    }

    fun getPairHomeConB():Int{
        var conut = 0
        
        // สร้าง HashMap เพื่อเพิ่มความเร็วในการค้นหา
        val miracleMap = homeDao!!.pairMiracle!!.associateBy { it.pairnumber }
        
        loop@ for (x in (homeDao!!.pairsB!!.size downTo 1)) {
            val pairNumber = homeDao!!.pairsB!![x - 1]
            val miracle = miracleMap[pairNumber]
            
            if (miracle != null) {
                when (miracle.pairtype?.get(0)?.toString()) {
                    "D" -> conut++
                    "R" -> break@loop
                }
            }
        }

        return conut
    }

}