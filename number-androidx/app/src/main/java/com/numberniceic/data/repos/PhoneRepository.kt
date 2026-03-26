package com.numberniceic.data.repos

import com.numberniceic.data.apicollectiondao.PhoneCollectionDao

object PhoneRepository {


    private var phoneDaoDao: PhoneCollectionDao? = null

    fun addPhoneData(dao: PhoneCollectionDao) {
        phoneDaoDao = dao
    }

    fun getScoreD(): Int {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.scoreTotalOfTotal != null) {
                return if(phoneDaoDao!!.scoreTotalOfTotal!!.scoreTotalD<0) 0 else phoneDaoDao!!.scoreTotalOfTotal!!.scoreTotalD
            }
        }
        return 0
    }

    fun getScoreR(): Int {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.scoreTotalOfTotal != null) {
                return phoneDaoDao!!.scoreTotalOfTotal!!.scoreTotalR
            }
        }
        return 0
    }

    fun getPercentD(): Int {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.percentTotalOfTotal != null) {
                return phoneDaoDao!!.percentTotalOfTotal!!.percentTotalD
            }
        }
        return 0
    }

    fun getPercentR(): Int {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.percentTotalOfTotal != null) {
                return phoneDaoDao!!.percentTotalOfTotal!!.percentTotalR
            }
        }
        return 0
    }

    fun getPairAp1(): String {
        if (phoneDaoDao != null) {
            if (!phoneDaoDao!!.pairsA.isNullOrEmpty()) {
                return phoneDaoDao!!.pairsA!![0] ?: "?"
            }
        }
        return "?"
    }

    fun getPairAp2(): String {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.pairsA != null && phoneDaoDao!!.pairsA!!.size > 1) {
                return phoneDaoDao!!.pairsA!![1] ?: "?"
            }
        }
        return "?"
    }

    fun getPairAp3(): String {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.pairsA != null && phoneDaoDao!!.pairsA!!.size > 2) {
                return phoneDaoDao!!.pairsA!![2] ?: "?"
            }
        }
        return "?"
    }

    fun getPairAp4(): String {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.pairsA != null && phoneDaoDao!!.pairsA!!.size > 3) {
                return phoneDaoDao!!.pairsA!![3] ?: "?"
            }
        }
        return "?"
    }

    fun getPairAp5(): String {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.pairsA != null && phoneDaoDao!!.pairsA!!.size > 4) {
                return phoneDaoDao!!.pairsA!![4] ?: "?"
            }
        }
        return "?"
    }

    fun getPairBp1(): String {
        if (phoneDaoDao != null) {
            if (!phoneDaoDao!!.pairsB.isNullOrEmpty()) {
                return phoneDaoDao!!.pairsB!![0] ?: "?"
            }
        }
        return "?"
    }

    fun getPairBp2(): String {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.pairsB != null && phoneDaoDao!!.pairsB!!.size > 1) {
                return phoneDaoDao!!.pairsB!![1] ?: "?"
            }
        }
        return "?"
    }

    fun getPairBp3(): String {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.pairsB != null && phoneDaoDao!!.pairsB!!.size > 2) {
                return phoneDaoDao!!.pairsB!![2] ?: "?"
            }
        }
        return "?"
    }

    fun getPairBp4(): String {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.pairsB != null && phoneDaoDao!!.pairsB!!.size > 3) {
                return phoneDaoDao!!.pairsB!![3] ?: "?"
            }
        }
        return "?"
    }

    fun getPairSum(): String {
        if (phoneDaoDao != null) {
            if (phoneDaoDao!!.pairSum != null) {
                return phoneDaoDao!!.pairSum ?: "?"
            }
        }
        return "?"
    }
}