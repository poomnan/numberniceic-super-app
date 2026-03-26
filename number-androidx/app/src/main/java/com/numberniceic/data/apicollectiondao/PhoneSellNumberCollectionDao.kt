package com.numberniceic.data.apicollectiondao

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.numberniceic.data.home.ContinueDR
import com.numberniceic.data.home.ScoreRD
import com.numberniceic.data.phone.PhoneNumberItem

import com.numberniceic.data.tabian.PairsMiracle

data class PhoneSellNumberCollectionDao(@SerializedName("phonenumberSell") val phonenumberSell: ArrayList<PhoneNumberItem>?, @SerializedName("phonenumTop4") val phonenumTop4: ArrayList<PhoneNumberItem>?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.createTypedArrayList(PhoneNumberItem.CREATOR),
            source.createTypedArrayList(PhoneNumberItem.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeTypedList(phonenumberSell)
        writeTypedList(phonenumTop4)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PhoneSellNumberCollectionDao> = object : Parcelable.Creator<PhoneSellNumberCollectionDao> {
            override fun createFromParcel(source: Parcel): PhoneSellNumberCollectionDao = PhoneSellNumberCollectionDao(source)
            override fun newArray(size: Int): Array<PhoneSellNumberCollectionDao?> = arrayOfNulls(size)
        }
    }
}