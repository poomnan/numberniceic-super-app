package com.numberniceic.data.apicollectiondao

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.numberniceic.data.home.ContinueDR
import com.numberniceic.data.home.HomeReport
import com.numberniceic.data.home.ScoreRD

import com.numberniceic.data.tabian.PairsMiracle

data class HomeCollectionDao(@SerializedName("scoreV2") val scoreV2: ScoreRD?, @SerializedName("scoreRD") val scoreRD: ScoreRD?, @SerializedName("homeId") val homeId: String?, @SerializedName("pairsA") val pairsA: ArrayList<String>? = arrayListOf(), @SerializedName("pairsB") val pairsB: ArrayList<String>? = arrayListOf(), @SerializedName("continueDR") val continueDR: ContinueDR?, @SerializedName("pairSumNumber") val pairSumNumber: String?, @SerializedName("pairUnique") val pairUnique: ArrayList<String>? = arrayListOf(), @SerializedName("homeReport") val homeReport: HomeReport?, @SerializedName("pairMiracle") val pairMiracle: ArrayList<PairsMiracle>?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readParcelable<ScoreRD>(ScoreRD::class.java.classLoader),
            source.readParcelable<ScoreRD>(ScoreRD::class.java.classLoader),
            source.readString(),
            source.createStringArrayList(),
            source.createStringArrayList(),
            source.readParcelable<ContinueDR>(ContinueDR::class.java.classLoader),
            source.readString(),
            source.createStringArrayList(),
            source.readParcelable<HomeReport>(HomeReport::class.java.classLoader),
            source.createTypedArrayList(PairsMiracle.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(scoreV2, 0)
        writeParcelable(scoreRD, 0)
        writeString(homeId)
        writeStringList(pairsA)
        writeStringList(pairsB)
        writeParcelable(continueDR, 0)
        writeString(pairSumNumber)
        writeStringList(pairUnique)
        writeParcelable(homeReport, 0)
        writeTypedList(pairMiracle)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<HomeCollectionDao> = object : Parcelable.Creator<HomeCollectionDao> {
            override fun createFromParcel(source: Parcel): HomeCollectionDao = HomeCollectionDao(source)
            override fun newArray(size: Int): Array<HomeCollectionDao?> = arrayOfNulls(size)
        }
    }
}