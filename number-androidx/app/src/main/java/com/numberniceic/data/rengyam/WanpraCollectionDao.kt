package com.numberniceic.data.rengyam

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class WanpraCollectionDao(@SerializedName("wanpra_collection") val wanpraCollectionDao: List<Wanpra>?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.createTypedArrayList(Wanpra.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeTypedList(wanpraCollectionDao)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<WanpraCollectionDao> = object : Parcelable.Creator<WanpraCollectionDao> {
            override fun createFromParcel(source: Parcel): WanpraCollectionDao = WanpraCollectionDao(source)
            override fun newArray(size: Int): Array<WanpraCollectionDao?> = arrayOfNulls(size)
        }
    }
}