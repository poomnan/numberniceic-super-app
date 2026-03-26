package com.numberniceic.data.apicollectiondao

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.numberniceic.data.nickname.CharNum
import com.numberniceic.data.nickname.PairFang
import com.numberniceic.data.tabian.PairsMiracle

data class NameSurnameCollectionDao(@SerializedName("birthDay") val birthDay: String?, @SerializedName("name") val name: String?, @SerializedName("surname") val surname: String?, @SerializedName("sumSatName") val sumSatName: Int, @SerializedName("sumSatSurName") val sumSatSurName: Int, @SerializedName("sumSatNameSurName") val sumSatNameSurName: Int, @SerializedName("pairSatName") val pairSatName: PairFang?, @SerializedName("pairSatSurName") val pairSatSurName: PairFang?, @SerializedName("pairSatNameSurName") val pairSatNameSurName: PairFang?, @SerializedName("kName") val kName: ArrayList<String>? = arrayListOf(), @SerializedName("sumShaName") val sumShaName: Int, @SerializedName("sumShaSurName") val sumShaSurName: Int, @SerializedName("sumShaNameSurName") val sumShaNameSurName: Int, @SerializedName("pairShaName") val pairShaName: PairFang?, @SerializedName("pairShaSurName") val pairShaSurName: PairFang?, @SerializedName("pairShaNameSurName") val pairShaNameSurName: PairFang?, @SerializedName("ayadName") val ayadName: String?, @SerializedName("ayadSurName") val ayadSurName: String?, @SerializedName("ayadNameSurName") val ayadNameSurName: String?, @SerializedName("pairsUnique") val pairsUnique: ArrayList<String>? = arrayListOf(), @SerializedName("satName") val satName: ArrayList<CharNum>? = arrayListOf(), @SerializedName("satSurName") val satSurName: ArrayList<CharNum>? = arrayListOf(), @SerializedName("shaName") val shaName: ArrayList<CharNum>? = arrayListOf(), @SerializedName("shaSurName") val shaSurName: ArrayList<CharNum>? = arrayListOf(), @SerializedName("pairsMiracle") val pairsMiracle: ArrayList<PairsMiracle>? = arrayListOf()) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readParcelable<PairFang>(PairFang::class.java.classLoader),
            source.readParcelable<PairFang>(PairFang::class.java.classLoader),
            source.readParcelable<PairFang>(PairFang::class.java.classLoader),
            source.createStringArrayList(),
            source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readParcelable<PairFang>(PairFang::class.java.classLoader),
            source.readParcelable<PairFang>(PairFang::class.java.classLoader),
            source.readParcelable<PairFang>(PairFang::class.java.classLoader),
            source.readString(),
            source.readString(),
            source.readString(),
            source.createStringArrayList(),
            source.createTypedArrayList(CharNum.CREATOR),
            source.createTypedArrayList(CharNum.CREATOR),
            source.createTypedArrayList(CharNum.CREATOR),
            source.createTypedArrayList(CharNum.CREATOR),
            source.createTypedArrayList(PairsMiracle.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(birthDay)
        writeString(name)
        writeString(surname)
        writeInt(sumSatName)
        writeInt(sumSatSurName)
        writeInt(sumSatNameSurName)
        writeParcelable(pairSatName, 0)
        writeParcelable(pairSatSurName, 0)
        writeParcelable(pairSatNameSurName, 0)
        writeStringList(kName)
        writeInt(sumShaName)
        writeInt(sumShaSurName)
        writeInt(sumShaNameSurName)
        writeParcelable(pairShaName, 0)
        writeParcelable(pairShaSurName, 0)
        writeParcelable(pairShaNameSurName, 0)
        writeString(ayadName)
        writeString(ayadSurName)
        writeString(ayadNameSurName)
        writeStringList(pairsUnique)
        writeTypedList(satName)
        writeTypedList(satSurName)
        writeTypedList(shaName)
        writeTypedList(shaSurName)
        writeTypedList(pairsMiracle)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<NameSurnameCollectionDao> = object : Parcelable.Creator<NameSurnameCollectionDao> {
            override fun createFromParcel(source: Parcel): NameSurnameCollectionDao = NameSurnameCollectionDao(source)
            override fun newArray(size: Int): Array<NameSurnameCollectionDao?> = arrayOfNulls(size)
        }
    }
}