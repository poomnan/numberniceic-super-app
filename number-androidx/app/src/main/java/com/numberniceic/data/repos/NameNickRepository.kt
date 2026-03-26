package com.numberniceic.data.repos

import com.numberniceic.data.apicollectiondao.HomeCollectionDao
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao
import com.numberniceic.data.nickname.CharNum
import com.numberniceic.data.nickname.PairFang
import com.numberniceic.data.tabian.PairsMiracle

object NameNickRepository {

    private var dao:NickNameCollectionDao? = null

    fun addNickNameDao(dao: NickNameCollectionDao) {
        this.dao = dao
    }

    fun birthDay():String?{
        return dao!!.birthDay
    }

    fun nickname():String?{
        return dao!!.nickname
    }

    fun sumSatNickName(): Int? {
        return dao!!.sumSatNickName
    }

    fun sumShaNickName(): Int? {
        return dao!!.sumShaNickName
    }

    fun pairSatNickName(): PairFang? {
        return dao!!.pairSatNickName
    }

    fun pairShaNickName(): PairFang? {
        return dao!!.pairShaNickName
    }


    fun kName(): ArrayList<String>? {
        return dao!!.kName
    }

    fun ayadNickName(): String? {
        return dao!!.ayadNickName
    }

    fun pairsUnique(): ArrayList<String>? {
        return dao!!.pairsUnique
    }

    fun satNickName(): ArrayList<CharNum>? {
        return dao!!.satNickName
    }

    fun shaNickName(): ArrayList<CharNum>? {
        return dao!!.shaNickName
    }

    fun pairsMiracle(): ArrayList<PairsMiracle>? {
        return dao!!.pairsMiracle
    }
}