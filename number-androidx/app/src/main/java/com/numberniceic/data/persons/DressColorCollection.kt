package com.numberniceic.data.persons

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class DressColorCollection(@SerializedName("cloth_color") val clothColors: List<DressColor>?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.createTypedArrayList(DressColor.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeTypedList(clothColors)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<DressColorCollection> = object : Parcelable.Creator<DressColorCollection> {
            override fun createFromParcel(source: Parcel): DressColorCollection = DressColorCollection(source)
            override fun newArray(size: Int): Array<DressColorCollection?> = arrayOfNulls(size)
        }
    }
}