package com.numberniceic.data.rengyam

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class LengYamDao(@SerializedName("leng_yam") val lengYam: WanSpecial?, @SerializedName("next_wanpra") val nextWanpra: String?, @SerializedName("wan_pras") val wanPras: List<Wanpra>?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readParcelable<WanSpecial>(WanSpecial::class.java.classLoader),
            source.readString(),
            source.createTypedArrayList(Wanpra.CREATOR)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(lengYam, 0)
        writeString(nextWanpra)
        writeTypedList(wanPras)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<LengYamDao> = object : Parcelable.Creator<LengYamDao> {
            override fun createFromParcel(source: Parcel): LengYamDao = LengYamDao(source)
            override fun newArray(size: Int): Array<LengYamDao?> = arrayOfNulls(size)
        }
    }
}