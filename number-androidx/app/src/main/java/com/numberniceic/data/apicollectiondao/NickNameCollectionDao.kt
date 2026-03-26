package com.numberniceic.data.apicollectiondao

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.numberniceic.data.nickname.CharNum
import com.numberniceic.data.nickname.PairFang


import com.numberniceic.data.tabian.PairsMiracle

data class NickNameCollectionDao(@SerializedName("birthDay") val birthDay: String?, @SerializedName("nickname") val nickname: String?, @SerializedName("sumSatNickName") val sumSatNickName: Int, @SerializedName("pairSatNickName") val pairSatNickName: PairFang?, @SerializedName("kName") val kName: ArrayList<String>? = arrayListOf(), @SerializedName("sumShaNickName") val sumShaNickName: Int, @SerializedName("pairShaNickName") val pairShaNickName: PairFang?, @SerializedName("ayadNickName") val ayadNickName: String?, @SerializedName("pairsUnique") val pairsUnique: ArrayList<String>? = arrayListOf(), @SerializedName("satNickName") val satNickName: ArrayList<CharNum>? = arrayListOf(), @SerializedName("shaNickName") val shaNickName: ArrayList<CharNum>? = arrayListOf(), @SerializedName("pairsMiracle") val pairsMiracle: ArrayList<PairsMiracle>? = arrayListOf()) : Parcelable {

    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readInt(),
            source.readParcelable<PairFang>(PairFang::class.java.classLoader),
            source.createStringArrayList(),
            source.readInt(),
            source.readParcelable<PairFang>(PairFang::class.java.classLoader),
            source.readString(),
            source.createStringArrayList(),
            source.createTypedArrayList(CharNum.CREATOR),
            source.createTypedArrayList(CharNum.CREATOR),
            source.createTypedArrayList(PairsMiracle.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(birthDay)
        writeString(nickname)
        writeInt(sumSatNickName)
        writeParcelable(pairSatNickName, 0)
        writeStringList(kName)
        writeInt(sumShaNickName)
        writeParcelable(pairShaNickName, 0)
        writeString(ayadNickName)
        writeStringList(pairsUnique)
        writeTypedList(satNickName)
        writeTypedList(shaNickName)
        writeTypedList(pairsMiracle)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<NickNameCollectionDao> = object : Parcelable.Creator<NickNameCollectionDao> {
            override fun createFromParcel(source: Parcel): NickNameCollectionDao = NickNameCollectionDao(source)
            override fun newArray(size: Int): Array<NickNameCollectionDao?> = arrayOfNulls(size)
        }
    }
}