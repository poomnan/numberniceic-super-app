package com.numberniceic.data.persons

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class MiracleDo(@SerializedName("activity") val activity: String?, @SerializedName("dayx") val dayX: String?, @SerializedName("dayy") val dayY: String?, @SerializedName("action") val action: String?, @SerializedName("mira_desc") val miraDesc: String?) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(activity)
        writeString(dayX)
        writeString(dayY)
        writeString(action)
        writeString(miraDesc)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MiracleDo> = object : Parcelable.Creator<MiracleDo> {
            override fun createFromParcel(source: Parcel): MiracleDo = MiracleDo(source)
            override fun newArray(size: Int): Array<MiracleDo?> = arrayOfNulls(size)
        }
    }
}