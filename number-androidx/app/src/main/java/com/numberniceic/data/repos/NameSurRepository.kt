package com.numberniceic.data.repos

import com.numberniceic.data.apicollectiondao.NameSurnameCollectionDao
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao
import com.numberniceic.data.nickname.CharNum
import com.numberniceic.data.nickname.PairFang
import com.numberniceic.data.tabian.PairsMiracle

object NameSurRepository {

    private var dao: NameSurnameCollectionDao? = null

    fun addNameSurDao(dao: NameSurnameCollectionDao) {
        this.dao = dao
    }

    fun birthDay():String?{
        return dao!!.birthDay
    }

    fun name():String?{
        return dao!!.name
    }

    fun surname():String?{
        return dao!!.surname
    }

    fun sumSatName():Int?{
        return dao!!.sumSatName
    }

    fun sumSatSurName():Int?{
        return dao!!.sumSatSurName
    }

    fun sumSatNameSurName():Int?{
        return dao!!.sumSatNameSurName
    }

    fun pairSatName():PairFang?{
        return dao!!.pairSatName
    }

    fun pairSatSurName():PairFang?{
        return dao!!.pairSatSurName
    }

    fun pairSatNameSurName():PairFang?{
        return dao!!.pairSatNameSurName
    }

    fun kName():ArrayList<String>?{
        return dao!!.kName
    }

    fun sumShaName():Int?{
        return dao!!.sumShaName
    }

    fun sumShaSurName():Int?{
        return dao!!.sumShaSurName
    }

    fun sumShaNameSurName():Int?{
        return dao!!.sumShaNameSurName
    }

    fun pairShaName():PairFang?{
        return dao!!.pairShaName
    }

    fun pairShaSurName():PairFang?{
        return dao!!.pairShaSurName
    }

    fun pairShaNameSurName():PairFang?{
        return dao!!.pairShaNameSurName
    }

    fun ayadName():String?{
        return dao!!.ayadName
    }

    fun ayadSurName():String?{
        return dao!!.ayadSurName
    }

    fun ayadNameSurName():String?{
        return dao!!.ayadNameSurName
    }


    fun satName(): ArrayList<CharNum>? {
        return dao!!.satName
    }

    fun satSurName(): ArrayList<CharNum>? {
        return dao!!.satSurName
    }

    fun shaName(): ArrayList<CharNum>? {
        return dao!!.shaName
    }

    fun shaSurName(): ArrayList<CharNum>? {
        return dao!!.shaSurName
    }

    fun pairsMiracle(): ArrayList<PairsMiracle>? {
        return dao!!.pairsMiracle
    }


























}