package com.numberniceic.data.rengyam

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Wanpra(
    @SerializedName("wanpra_id") val wanpraId: String?,
    @SerializedName("wanpra_date") val wanpraDate: String?,
    @SerializedName("is_wanpra") var isWanpra: Any? = "0",
    @SerializedName("is_tongchai") var isTongchai: Any? = "0",
    @SerializedName("is_atipbadee") var isAtipbadee: Any? = "0",
    @SerializedName("is_kating") var isKating: Any? = "0",
    var isFoo: Boolean = false,
    var isLoy: Boolean = false,
    var isJom: Boolean = false,
    var isFu: Boolean = false,
    @SerializedName("is_riangmon") var isRiangMon: Boolean = false,
    @SerializedName("is_sittichok") var isSittichok: Boolean = false,
    @SerializedName("is_ammarit") var isAmmarit: Boolean = false,
    @SerializedName("is_mahasittichok") var isMahaSittichok: Boolean = false,
    @SerializedName("is_rachachok") var isRachaChok: Boolean = false,
    @SerializedName("is_chaichok") var isChaiChok: Boolean = false,
    @SerializedName("is_ubath") var isUbath: Boolean = false,
    @SerializedName("is_lokawinat") var isLokawinat: Boolean = false,
    var isKalagni: Boolean = false,
    var kalagniBirth: Boolean = false,
    var kalagniAge: Boolean = false,
    var isBestDay: Boolean = false,
    var isHighlighted: Boolean = false,
    var isOtherMonth: Boolean = false,
    var lunarPhase: String? = null,
    var lunarMonth: String? = null
) : Parcelable {
    

    constructor(source: Parcel) : this(
        source.readString(),
        source.readString(),
        source.readValue(String::class.java.classLoader),
        source.readValue(String::class.java.classLoader),
        source.readValue(String::class.java.classLoader),
        source.readValue(String::class.java.classLoader),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(),
        source.readByte() != 0.toByte(), // isOtherMonth
        source.readString(),
        source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(wanpraId)
        writeString(wanpraDate)
        writeValue(isWanpra)
        writeValue(isTongchai)
        writeValue(isAtipbadee)
        writeValue(isKating)
        writeByte(if (isFoo) 1 else 0)
        writeByte(if (isLoy) 1 else 0)
        writeByte(if (isJom) 1 else 0)
        writeByte(if (isFu) 1 else 0)
        writeByte(if (isRiangMon) 1 else 0)
        writeByte(if (isSittichok) 1 else 0)
        writeByte(if (isAmmarit) 1 else 0)
        writeByte(if (isMahaSittichok) 1 else 0)
        writeByte(if (isRachaChok) 1 else 0)
        writeByte(if (isChaiChok) 1 else 0)
        writeByte(if (isUbath) 1 else 0)
        writeByte(if (isLokawinat) 1 else 0)
        writeByte(if (isKalagni) 1 else 0)
        writeByte(if (kalagniBirth) 1 else 0)
        writeByte(if (kalagniAge) 1 else 0)
        writeByte(if (isBestDay) 1 else 0)
        writeByte(if (isHighlighted) 1 else 0)
        writeByte(if (isOtherMonth) 1 else 0)
        writeString(lunarPhase)
        writeString(lunarMonth)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Wanpra> = object : Parcelable.Creator<Wanpra> {
            override fun createFromParcel(source: Parcel): Wanpra = Wanpra(source)
            override fun newArray(size: Int): Array<Wanpra?> = arrayOfNulls(size)
        }
    }
}