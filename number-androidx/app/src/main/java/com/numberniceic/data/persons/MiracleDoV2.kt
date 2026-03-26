package com.numberniceic.data.persons

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class MiracleDoV2(@SerializedName("wanpra") val wanpra: Boolean, @SerializedName("domira") val domira: MiracleDo?) : Parcelable {
    

    constructor(source: Parcel) : this(
            1 == source.readInt(),
            source.readParcelable<MiracleDo>(MiracleDo::class.java.classLoader)
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt((if (wanpra) 1 else 0))
        writeParcelable(domira, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MiracleDoV2> = object : Parcelable.Creator<MiracleDoV2> {
            override fun createFromParcel(source: Parcel): MiracleDoV2 = MiracleDoV2(source)
            override fun newArray(size: Int): Array<MiracleDoV2?> = arrayOfNulls(size)
        }
    }
}