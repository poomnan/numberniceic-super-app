package com.numberniceic.data.apicollectiondao

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.numberniceic.data.phone.*

data class PhoneCollectionDao(@SerializedName("scoreTotalOfTotal") val scoreTotalOfTotal: ScoreTotal?, @SerializedName("pairSpecialX") val pairSpecialX: PairSpecialX?, @SerializedName("countPairZero") val countPairZero: Int?, @SerializedName("scoreDupMi") val scoreDupMi: Int?, @SerializedName("percentTotalOfTotal") val percentTotalOfTotal: PercentTotalOfTotal?, @SerializedName("scoreByContinueCountPairsA") val scoreByContinueCountPairsA: PairContinue?, @SerializedName("scoreByContinueCountPairsB") val scoreByContinueCountPairsB: PairContinue?, @SerializedName("miracleSummary") val miracleSummary: Miracle?, @SerializedName("specialGoal") val specialGoal: SpecialGoal?, @SerializedName("pairsA") val pairsA: ArrayList<String>? = arrayListOf(), @SerializedName("pairsB") val pairsB: ArrayList<String>? = arrayListOf(), @SerializedName("pairSum") val pairSum: String?, @SerializedName("scorePairByPositionA") val scorePairByPositionA: ArrayList<PairItem>? = arrayListOf(), @SerializedName("scorePairByPositionB") val scorePairByPositionB: ArrayList<PairItem>? = arrayListOf(), @SerializedName("scorePairBySum") val scorePairBySum: PairItem?, @SerializedName("dataSortByType") val dataSortByType: ArrayList<DataSortByTypeMiracle>? = arrayListOf()) :
        Parcelable {
    

    constructor(source: Parcel) : this(
            source.readParcelable<ScoreTotal>(ScoreTotal::class.java.classLoader),
            source.readParcelable<PairSpecialX>(PairSpecialX::class.java.classLoader),
            source.readValue(Int::class.java.classLoader) as Int?,
            source.readValue(Int::class.java.classLoader) as Int?,
            source.readParcelable<PercentTotalOfTotal>(PercentTotalOfTotal::class.java.classLoader),
            source.readParcelable<PairContinue>(PairContinue::class.java.classLoader),
            source.readParcelable<PairContinue>(PairContinue::class.java.classLoader),
            source.readParcelable<Miracle>(Miracle::class.java.classLoader),
            source.readParcelable<SpecialGoal>(SpecialGoal::class.java.classLoader),
            source.createStringArrayList(),
            source.createStringArrayList(),
            source.readString(),
            source.createTypedArrayList(PairItem.CREATOR),
            source.createTypedArrayList(PairItem.CREATOR),
            source.readParcelable<PairItem>(PairItem::class.java.classLoader),
            source.createTypedArrayList(DataSortByTypeMiracle.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(scoreTotalOfTotal, 0)
        writeParcelable(pairSpecialX, 0)
        writeValue(countPairZero)
        writeValue(scoreDupMi)
        writeParcelable(percentTotalOfTotal, 0)
        writeParcelable(scoreByContinueCountPairsA, 0)
        writeParcelable(scoreByContinueCountPairsB, 0)
        writeParcelable(miracleSummary, 0)
        writeParcelable(specialGoal, 0)
        writeStringList(pairsA)
        writeStringList(pairsB)
        writeString(pairSum)
        writeTypedList(scorePairByPositionA)
        writeTypedList(scorePairByPositionB)
        writeParcelable(scorePairBySum, 0)
        writeTypedList(dataSortByType)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PhoneCollectionDao> = object : Parcelable.Creator<PhoneCollectionDao> {
            override fun createFromParcel(source: Parcel): PhoneCollectionDao = PhoneCollectionDao(source)
            override fun newArray(size: Int): Array<PhoneCollectionDao?> = arrayOfNulls(size)
        }
    }
}
