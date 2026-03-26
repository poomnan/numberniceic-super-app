package com.numberniceic.data.phone

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class DataSortByTypeMiracle(@SerializedName("number") val number: String?, @SerializedName("description") val description: String?, @SerializedName("percentile") val percentile: Int, @SerializedName("type") val type: String?, @SerializedName("point") val point: Int, @SerializedName("detail") val detail: String?) : Parcelable {
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readInt(),
            source.readString(),
            source.readInt(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(number)
        writeString(description)
        writeInt(percentile)
        writeString(type)
        writeInt(point)
        writeString(detail)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<DataSortByTypeMiracle> = object : Parcelable.Creator<DataSortByTypeMiracle> {
            override fun createFromParcel(source: Parcel): DataSortByTypeMiracle = DataSortByTypeMiracle(source)
            override fun newArray(size: Int): Array<DataSortByTypeMiracle?> = arrayOfNulls(size)
        }
    }
}