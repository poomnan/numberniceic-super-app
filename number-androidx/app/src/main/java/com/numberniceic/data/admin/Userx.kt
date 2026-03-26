package com.numberniceic.data.admin

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Userx(
    @SerializedName(value="memberid", alternate=["memberId", "id", "ID", "member_id"]) var userId: String?,
    @SerializedName(value="realname", alternate=["realName", "RealName", "Realname", "name"]) val realName: String?,
    @SerializedName("surname") val surname: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("birthday") var birthDay: String?,
    @SerializedName("shour") var sHour: Int,
    @SerializedName("sminute") var sMinute: Int,
    @SerializedName("sgender") val sGender: String?,
    @SerializedName("ageyear") val ageYear: Int,
    @SerializedName("agemonth") val ageMonth: Int,
    @SerializedName("ageweek") val ageWeek: Int,
    @SerializedName("ageday") val ageDay: Int,
    @SerializedName("day_of_birth") var dayOfBirth: Int = 0,
    @SerializedName("password") val password: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("vipcode") var vipcode: String?,
    @SerializedName("sprovince") val sProvince: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("shipping_address") val shippingAddress: String? = null,
    @SerializedName("address") val address: String? = null
) : Parcelable {
    
    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readInt(),
            source.readInt(),
            source.readString(),
            source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(userId)
        writeString(realName)
        writeString(surname)
        writeString(username)
        writeString(birthDay)
        writeInt(sHour)
        writeInt(sMinute)
        writeString(sGender)
        writeInt(ageYear)
        writeInt(ageMonth)
        writeInt(ageWeek)
        writeInt(ageDay)
        writeInt(dayOfBirth)
        writeString(password)
        writeString(status)
        writeString(vipcode)
        writeString(sProvince)
        writeString(avatar)
        writeString(shippingAddress)
        writeString(address)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Userx> = object : Parcelable.Creator<Userx> {
            override fun createFromParcel(source: Parcel): Userx = Userx(source)
            override fun newArray(size: Int): Array<Userx?> = arrayOfNulls(size)
        }
    }
}
