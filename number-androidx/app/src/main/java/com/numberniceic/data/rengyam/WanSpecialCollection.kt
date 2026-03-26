package com.numberniceic.data.rengyam

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class WanSpecialCollection(@SerializedName("activity") val activity: String?, @SerializedName("wan_special") val wanSpecial: WanSpecial?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readParcelable<WanSpecial>(WanSpecial::class.java.classLoader)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(activity)
        writeParcelable(wanSpecial, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<WanSpecialCollection> = object : Parcelable.Creator<WanSpecialCollection> {
            override fun createFromParcel(source: Parcel): WanSpecialCollection = WanSpecialCollection(source)
            override fun newArray(size: Int): Array<WanSpecialCollection?> = arrayOfNulls(size)
        }
    }
}
