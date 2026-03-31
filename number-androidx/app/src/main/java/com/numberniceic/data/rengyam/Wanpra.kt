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
    @SerializedName("is_loy") var isLoy: Boolean = false,
    @SerializedName("is_jom") var isJom: Boolean = false,
    @SerializedName("is_fu") var isFu: Boolean = false,
    @SerializedName("display_tags") var displayTags: List<String>? = null,
    @SerializedName("display_tags_prioritized") var displayTagsPrioritized: List<String>? = null,
    @SerializedName("calendar_display_tags") var calendarDisplayTags: List<String>? = null,
    @SerializedName("kal_tags") var kalTags: List<String>? = null,
    @SerializedName("dithi_tags") var dithiTags: List<String>? = null,
    @SerializedName("day_type_tags") var dayTypeTags: List<String>? = null,
    @SerializedName("warning_tags") var warningTags: List<String>? = null,
    @SerializedName("tag_details") var tagDetails: List<TagDetail>? = null,
    @SerializedName("myhora_display_tags") var myhoraDisplayTags: List<String>? = null,
    @SerializedName("myhora_display_tags_prioritized") var myhoraDisplayTagsPrioritized: List<String>? = null,
    @SerializedName("myhora_tag_details") var myhoraTagDetails: List<TagDetail>? = null,
    @SerializedName("mahamodo_display_tags") var mahamodoDisplayTags: List<String>? = null,
    @SerializedName("mahamodo_display_tags_prioritized") var mahamodoDisplayTagsPrioritized: List<String>? = null,
    @SerializedName("mahamodo_tag_details") var mahamodoTagDetails: List<TagDetail>? = null,
    var isFoo: Boolean = false,
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
        source.createStringArrayList(),
        source.createStringArrayList(),
        source.createStringArrayList(),
        source.createStringArrayList(),
        source.createStringArrayList(),
        source.createStringArrayList(),
        source.createStringArrayList(),
        source.createTypedArrayList(TagDetail.CREATOR),
        source.createStringArrayList(),
        source.createStringArrayList(),
        source.createTypedArrayList(TagDetail.CREATOR),
        source.createStringArrayList(),
        source.createStringArrayList(),
        source.createTypedArrayList(TagDetail.CREATOR),
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
        source.readByte() != 0.toByte(), // isBestDay
        source.readByte() != 0.toByte(), // isHighlighted
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
        writeByte(if (isLoy) 1 else 0)
        writeByte(if (isJom) 1 else 0)
        writeByte(if (isFu) 1 else 0)
        writeStringList(displayTags)
        writeStringList(displayTagsPrioritized)
        writeStringList(calendarDisplayTags)
        writeStringList(kalTags)
        writeStringList(dithiTags)
        writeStringList(dayTypeTags)
        writeStringList(warningTags)
        writeTypedList(tagDetails)
        writeStringList(myhoraDisplayTags)
        writeStringList(myhoraDisplayTagsPrioritized)
        writeTypedList(myhoraTagDetails)
        writeStringList(mahamodoDisplayTags)
        writeStringList(mahamodoDisplayTagsPrioritized)
        writeTypedList(mahamodoTagDetails)
        writeByte(if (isFoo) 1 else 0)
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
