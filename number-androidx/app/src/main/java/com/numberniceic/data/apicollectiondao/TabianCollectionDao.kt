package com.numberniceic.data.apicollectiondao

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.numberniceic.data.tabian.MiracleSummary
import com.numberniceic.data.tabian.PairsMiracle
import com.numberniceic.data.tabian.SumTotalPercent


class TabianCollectionDao(@SerializedName("carid") val cairId: String?, @SerializedName("scoreTotalD") val scoreTotalD: Int?, @SerializedName("scoreTotalR") val scoreTotalR: Int?, @SerializedName("miracleSummary") val miracleSummary: MiracleSummary?, @SerializedName("sumTotalPercent") val sumTotalPercent: SumTotalPercent?, @SerializedName("carPairsA") val carPairsA: ArrayList<String>? = arrayListOf(), @SerializedName("carPairsB") val carPairsB: ArrayList<String>? = arrayListOf(), @SerializedName("carPairSumAll") val carPairSumAll: String?, @SerializedName("carPairSumSecond") val carPairSumSecond: String?, @SerializedName("carPairUnique") val carPairUnique: ArrayList<String>? = arrayListOf(), @SerializedName("pairsMiracle") val pairsMiracle: ArrayList<PairsMiracle>? = arrayListOf()) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readValue(Int::class.java.classLoader) as Int?,
            source.readValue(Int::class.java.classLoader) as Int?,
            source.readParcelable<MiracleSummary>(MiracleSummary::class.java.classLoader),
            source.readParcelable<SumTotalPercent>(SumTotalPercent::class.java.classLoader),
            source.createStringArrayList(),
            source.createStringArrayList(),
            source.readString(),
            source.readString(),
            source.createStringArrayList(),
            source.createTypedArrayList(PairsMiracle.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(cairId)
        writeValue(scoreTotalD)
        writeValue(scoreTotalR)
        writeParcelable(miracleSummary, 0)
        writeParcelable(sumTotalPercent, 0)
        writeStringList(carPairsA)
        writeStringList(carPairsB)
        writeString(carPairSumAll)
        writeString(carPairSumSecond)
        writeStringList(carPairUnique)
        writeTypedList(pairsMiracle)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TabianCollectionDao> = object : Parcelable.Creator<TabianCollectionDao> {
            override fun createFromParcel(source: Parcel): TabianCollectionDao = TabianCollectionDao(source)
            override fun newArray(size: Int): Array<TabianCollectionDao?> = arrayOfNulls(size)
        }
    }
}