package com.numberniceic.data.rengyam

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class TagDetail(
    @SerializedName("tag") val tag: String? = null,
    @SerializedName("display_tag") val displayTag: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("school") val school: String? = null
) : Parcelable {
    constructor(sourceParcel: Parcel) : this(
        sourceParcel.readString(),
        sourceParcel.readString(),
        sourceParcel.readString(),
        sourceParcel.readString(),
        sourceParcel.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(tag)
        dest.writeString(displayTag)
        dest.writeString(source)
        dest.writeString(description)
        dest.writeString(school)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TagDetail> = object : Parcelable.Creator<TagDetail> {
            override fun createFromParcel(source: Parcel): TagDetail = TagDetail(source)
            override fun newArray(size: Int): Array<TagDetail?> = arrayOfNulls(size)
        }
    }
}
