package com.numberniceic.data.admin

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class RengyamAccess(
    @SerializedName("granted") val granted: Boolean = false,
    @SerializedName("viptype") val viptype: String? = null,
    @SerializedName("codename") val codename: String? = null,
    @SerializedName("dateadd") val dateadd: String? = null,
    @SerializedName("expire_at") val expireAt: String? = null,
    @SerializedName("expired") val expired: Boolean = false
) : Parcelable {

    constructor(source: Parcel) : this(
        source.readByte() != 0.toByte(),
        source.readString(),
        source.readString(),
        source.readString(),
        source.readString(),
        source.readByte() != 0.toByte()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeByte(if (granted) 1 else 0)
        writeString(viptype)
        writeString(codename)
        writeString(dateadd)
        writeString(expireAt)
        writeByte(if (expired) 1 else 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<RengyamAccess> = object : Parcelable.Creator<RengyamAccess> {
            override fun createFromParcel(source: Parcel): RengyamAccess = RengyamAccess(source)
            override fun newArray(size: Int): Array<RengyamAccess?> = arrayOfNulls(size)
        }
    }
}
