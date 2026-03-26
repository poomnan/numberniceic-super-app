package com.numberniceic.data.repos

import com.numberniceic.data.apicollectiondao.TabianCollectionDao
import com.numberniceic.data.tabian.PercentTabianDR

object TabianRepository {

    private var tabianDao: TabianCollectionDao? = null

    fun addTabianDao(dao: TabianCollectionDao) {
        this.tabianDao = dao
    }

    fun getCarId() = this.tabianDao!!.cairId

    fun getCarPairA() = this.tabianDao!!.carPairsA
    fun getCarPairB() = this.tabianDao!!.carPairsB
    fun getCarPairSumAll() = this.tabianDao!!.carPairSumAll
    fun getCarPairSumSecond() = this.tabianDao!!.carPairSumSecond
    fun getCarPairUnigue() = this.tabianDao!!.carPairUnique
    fun getPairMiracle() = this.tabianDao!!.pairsMiracle


    fun getGradeNumMudSingle(numMud: String): Int {

        if (numMud.length == 2 || numMud.length == 1) {
            return when (pairCheckType(numMud)) {
                "D10" -> 10
                "D8" -> 8
                "D5" -> 5

                else -> when (pairCheckType(numMud)) {
                    "R10" -> -10
                    "R7" -> -7
                    "R5" -> -5
                    else -> 0
                }
            }


        }


        return 0
    }

    fun getGradeNumMudDuo(pairx: ArrayList<String>): Int {

        val pairTypeFirst = StringBuilder()
        val pairTypeSecond = StringBuilder()

        pairTypeFirst.append(pairCheckType(pairx[0])[0].toString())
        pairTypeSecond.append(pairCheckType(pairx[1])[0].toString())


        if (pairTypeFirst.toString() == "D" && pairTypeSecond.toString() == "D") {
            return 4
        }


        if (pairTypeFirst.toString() == "R" && pairTypeSecond.toString() == "D") {
            return 1
        }

        if (pairTypeFirst.toString() == "D" && pairTypeSecond.toString() == "R") {
            return 1
        }

        if (pairTypeFirst.toString() == "R" && pairTypeSecond.toString() == "R") {
            return 0
        }

        return 0
    }


    private fun pairCheckType(pair: String): String {

        for (dao in this.tabianDao!!.pairsMiracle!!) {
            if (dao.pairnumber == pair) {
                return dao.pairtype!!
            }


        }

        return "?"
    }


    fun getScoreD(): Int {
        var score = 0

        for (value in tabianDao!!.pairsMiracle!!) {
            if (value.pairtype!![0].toString() == "D") {
                score += value.pairpoint
            }
        }

        return score
    }

    fun getScoreR(): Int {
        var score = 0

        for (value in tabianDao!!.pairsMiracle!!) {
            if (value.pairtype!![0].toString() == "R") {
                score += value.pairpoint
            }
        }

        return score
    }


    fun getScoreDV2(): Int {
        return tabianDao!!.scoreTotalD!!
    }

    fun getScoreRV2(): Int {
        return tabianDao!!.scoreTotalR!!
    }



    fun getTxtNumRoamAll(): String {
        var txt = ""

        for (item in tabianDao!!.pairsMiracle!!) {
            if (this.tabianDao!!.carPairSumAll == item.pairnumber) {
                txt = when {
                    item.pairtype == "D10" -> "ดีเยี่ยม"
                    item.pairtype == "D8" -> "ดีมาก"
                    item.pairtype == "D5" -> "ดี"
                    else -> when {
                        item.pairtype == "R10" -> "อันตรายมาก"
                        item.pairtype == "R7" -> "อันตราย"
                        item.pairtype == "R5" -> "เสี่ยงมาก"
                        else -> "?"
                    }
                }


            }
        }


        return txt
    }

    fun getTxtNumSecond(): String {
        var txt = ""

        for (item in tabianDao!!.pairsMiracle!!) {
            if (this.tabianDao!!.carPairSumSecond == item.pairnumber) {
                txt = when {
                    item.pairtype == "D10" -> "ดีเยี่ยม"//17
                    item.pairtype == "D8" -> "ดีมาก"//11
                    item.pairtype == "D5" -> "ดี"//5
                    else -> when {
                        item.pairtype == "R10" -> "อันตรายมาก"
                        item.pairtype == "R7" -> "อันตราย"
                        item.pairtype == "R5" -> "เสี่ยงมาก"
                        else -> "?"
                    }
                }


            }
        }


        return txt
    }

    fun getPairConA(): Int {
        var conut = 0
        loop@ for (x in (tabianDao!!.carPairsA!!.size downTo 1)) {

            for (n in tabianDao!!.pairsMiracle!!) {
                if (tabianDao!!.carPairsA!![x - 1] == n.pairnumber) {
                    if (n.pairtype!![0].toString() == "D") {
                        conut++

                    } else if (n.pairtype[0].toString() == "R") break@loop


                }


            }
        }

        return conut
    }

    fun getPairConB(): Int {
        var conut = 0
        loop@ for (x in (tabianDao!!.carPairsB!!.size downTo 1)) {

            for (n in tabianDao!!.pairsMiracle!!) {
                if (tabianDao!!.carPairsB!![x - 1] == n.pairnumber) {
                    if (n.pairtype!![0].toString() == "D") {
                        conut++

                    } else if (n.pairtype[0].toString() == "R") break@loop


                }


            }
        }

        return conut
    }

    private fun getPercentTabian(): PercentTabianDR? {

        var percentD = 0
        var percentR = 0




        return PercentTabianDR(percentD, percentR)
    }
}

