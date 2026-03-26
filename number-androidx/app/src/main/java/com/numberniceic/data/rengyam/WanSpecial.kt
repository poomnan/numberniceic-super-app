package com.numberniceic.data.rengyam

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class WanSpecial(
    @SerializedName("dayid") val dayId: String?,
    @SerializedName("wan_date") val wanDate: String?,
    @SerializedName("wan_desc") val wanDesc: String?,
    @SerializedName("wan_detail") val wanDetail: String?,
    @SerializedName("wan_pra") val wanPra: String?,
    @SerializedName("wan_kating") val wanKating: String?,
    @SerializedName("wan_tongchai") val wanTongchai: String?,
    @SerializedName("wan_atipbadee") val wanAtipbadee: String?,
    @SerializedName("wan_sittichok") val wanSittichok: String? = "0",
    @SerializedName("wan_mahasittichok") val wanMahaSittichok: String? = "0",
    @SerializedName("wan_ammarit") val wanAmmarit: String? = "0",
    @SerializedName("wan_rachachok") val wanRachaChok: String? = "0",
    @SerializedName("wan_chaichok") val wanChaiChok: String? = "0",
    @SerializedName("wan_riangmon") val wanRiangMon: String? = "0",
    @SerializedName("wan_lokawinat") val wanLokawinat: String? = "0"
) : Parcelable {
    

    constructor(source: Parcel) : this(
            source.readString(), // 1
            source.readString(), // 2
            source.readString(), // 3
            source.readString(), // 4
            source.readString(), // 5
            source.readString(), // 6
            source.readString(), // 7
            source.readString(), // 8
            source.readString(), // 9
            source.readString(), // 10
            source.readString(), // 11
            source.readString(), // 12
            source.readString(), // 13
            source.readString(), // 14
            source.readString()  // 15
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(dayId)
        writeString(wanDate)
        writeString(wanDesc)
        writeString(wanDetail)
        writeString(wanPra)
        writeString(wanKating)
        writeString(wanTongchai)
        writeString(wanAtipbadee)
        writeString(wanSittichok)
        writeString(wanMahaSittichok)
        writeString(wanAmmarit)
        writeString(wanRachaChok)
        writeString(wanChaiChok)
        writeString(wanRiangMon)
        writeString(wanLokawinat)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<WanSpecial> = object : Parcelable.Creator<WanSpecial> {
            override fun createFromParcel(source: Parcel): WanSpecial = WanSpecial(source)
            override fun newArray(size: Int): Array<WanSpecial?> = arrayOfNulls(size)
        }
    }
}