package com.numberniceic.data.admin

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class UserZ(
    @SerializedName("member_id") val memberId: String?,
    @SerializedName("username") val userName: String?,
    @SerializedName("realname") val realName: String?,
    @SerializedName("surname") val surName: String?,
    @SerializedName("birthday") val birthDat: String?,
    @SerializedName("fcm_token") val fcmToken: String?
) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(memberId)
        writeString(userName)
        writeString(realName)
        writeString(surName)
        writeString(birthDat)
        writeString(fcmToken)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<UserZ> = object : Parcelable.Creator<UserZ> {
            override fun createFromParcel(source: Parcel): UserZ = UserZ(source)
            override fun newArray(size: Int): Array<UserZ?> = arrayOfNulls(size)
        }
    }
}