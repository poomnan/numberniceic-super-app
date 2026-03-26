package com.numberniceic.data.phone

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class PhoneNumberItem(@SerializedName("pnumber_num") val phoneNumber: String?, @SerializedName("pnumber_sum") val phoneSum: String?, @SerializedName("pnumber_price") val phonePrice: String?, @SerializedName("phone_group") val phone_group: String?, @SerializedName("sell_status") val sell_status: String?, @SerializedName("prefix_group") val prefix_group: String?) : Parcelable {
    

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
        writeString(phoneNumber)
        writeString(phoneSum)
        writeString(phonePrice)
        writeString(phone_group)
        writeString(sell_status)
        writeString(prefix_group)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PhoneNumberItem> = object : Parcelable.Creator<PhoneNumberItem> {
            override fun createFromParcel(source: Parcel): PhoneNumberItem = PhoneNumberItem(source)
            override fun newArray(size: Int): Array<PhoneNumberItem?> = arrayOfNulls(size)
        }
    }
}