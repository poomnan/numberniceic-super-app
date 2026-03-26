package com.numberniceic.data.rengyam

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class WanpraDao(
    @SerializedName("activity") val activity: String?,
    @SerializedName("tomorrow") val tomorro: Boolean,
    @SerializedName("wanpra") val wanpra: Wanpra?,
    @SerializedName("wan_special") val wanSpecial: WanSpecial?,
    @SerializedName("wan_special_tomorrow") val wanSpecialTomorrow: WanSpecial?
) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            1 == source.readInt(),
            source.readParcelable<Wanpra>(Wanpra::class.java.classLoader),
            source.readParcelable<WanSpecial>(WanSpecial::class.java.classLoader),
            source.readParcelable<WanSpecial>(WanSpecial::class.java.classLoader)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(activity)
        writeInt((if (tomorro) 1 else 0))
        writeParcelable(wanpra, 0)
        writeParcelable(wanSpecial, 0)
        writeParcelable(wanSpecialTomorrow, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<WanpraDao> = object : Parcelable.Creator<WanpraDao> {
            override fun createFromParcel(source: Parcel): WanpraDao = WanpraDao(source)
            override fun newArray(size: Int): Array<WanpraDao?> = arrayOfNulls(size)
        }
    }
}