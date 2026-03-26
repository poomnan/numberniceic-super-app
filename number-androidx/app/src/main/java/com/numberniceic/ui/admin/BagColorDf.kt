package com.numberniceic.ui.admin

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.adapters.FindUserAdapter
import com.numberniceic.data.admin.*
import com.numberniceic.databinding.FragmentBagColorDfBinding
import com.numberniceic.utils.PersonContextManager
import org.joda.time.DateTime

class BagColorDf : Fragment(), BagPaletteColorF.OnSelectColorListener, View.OnClickListener {


    private lateinit var binding: FragmentBagColorDfBinding
    private lateinit var bagColorModel: BagColorModel
    private var userZ: UserZ? = null
    private var ageCurrent: Int? = null

    companion object {
        fun newInstance(userz: UserZ): BagColorDf {
            val args = Bundle()
            args.putParcelable("userz", userz)
            val fragment = BagColorDf()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {



        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_bag_color_df, container, false)
        bagColorModel = ViewModelProvider(this.activity!!)[BagColorModel::class.java]
        binding.apply {
            lifecycleOwner = this@BagColorDf
            binding.bagColorObs = bagColorModel.bagColorObs
        }

        this.userZ = arguments!!.getParcelable("userz")

        if (userZ != null) {
            bagColorModel.setUserName(userZ!!.userName ?: "ไม่ระบุชื่อ")
            bagColorModel.setUserId("รหัส: ${userZ!!.memberId ?: "-"}")
            
            Log.d("BagColorDf", "Loading data for user: ${userZ!!.memberId} - ${userZ!!.userName}")
            if (!userZ!!.birthDat.isNullOrEmpty()) {
                try {
                    val birthday: DateTime? = DateTime.parse(userZ!!.birthDat)
                    if (birthday != null) {

                        this.ageCurrent = PersonContextManager.ageCurrent(birthday.year, birthday.monthOfYear, birthday.dayOfMonth)
                        initUser() // Update age display
                        
                        val dayOfWeekThai = PersonContextManager.convertDayNumToThai(birthday.dayOfWeek)
                        bagColorModel.setUserDayOfWeek( "เกิดวัน: ${dayOfWeekThai ?: "ไม่ระบุ"}")

                        Log.d("BagColorDf", "Requesting colors for userId: ${userZ!!.memberId}")
                        val apiService = RetrofitClient.instance.create(ApiService::class.java)
                        fetchBagColor()

                    }
                } catch (e: Exception) {
                    Log.e("BagColorDf", "Birthday parse error: ${e.message}")
                    bagColorModel.setUserDayOfWeek("วันเกิดไม่ถูกต้อง")
                }

            } else {
                bagColorModel.setUserDayOfWeek("ไม่มีข้อมูลวันเกิด")
            }

        } else {
            Log.d("USERID", "UserZ is null")
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bagColorModel.setOnSave(true)

        initUser()
        initDateYear()
        setChipInit()
        refreshChipColors()

        binding.btnAddColor.setOnClickListener {
            val colorData = JsonObject()

            colorData.addProperty("memberid", this.userZ!!.memberId)
            colorData.addProperty("age", this.ageCurrent)


            if (bagColorModel.getColorCodeChip0() != 0) {
                val intColor = bagColorModel.getColorCodeChip0()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("color0", hexColor)
            }

            if (bagColorModel.getColorCodeChip1() != 0) {
                val intColor = bagColorModel.getColorCodeChip1()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("color1", hexColor)
            }

            if (bagColorModel.getColorCodeChip2() != 0) {
                val intColor = bagColorModel.getColorCodeChip2()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("color2", hexColor)
            }

            if (bagColorModel.getColorCodeChip3() != 0) {
                val intColor = bagColorModel.getColorCodeChip3()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("color3", hexColor)
            }

            if (bagColorModel.getColorCodeChip4() != 0) {
                val intColor = bagColorModel.getColorCodeChip4()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("color4", hexColor)
            }

            if (bagColorModel.getColorCodeChip5() != 0) {
                val intColor = bagColorModel.getColorCodeChip5()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("color5", hexColor)
            }



            if (bagColorModel.getColorCodeChipB0() != 0) {
                val intColor = bagColorModel.getColorCodeChipB0()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("colorb0", hexColor)
            }

            if (bagColorModel.getColorCodeChipB1() != 0) {
                val intColor = bagColorModel.getColorCodeChipB1()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("colorb1", hexColor)
            }

            if (bagColorModel.getColorCodeChipB2() != 0) {
                val intColor = bagColorModel.getColorCodeChipB2()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("colorb2", hexColor)
            }

            if (bagColorModel.getColorCodeChipB3() != 0) {
                val intColor = bagColorModel.getColorCodeChipB3()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("colorb3", hexColor)
            }

            if (bagColorModel.getColorCodeChipB4() != 0) {
                val intColor = bagColorModel.getColorCodeChipB4()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("colorb4", hexColor)
            }

            if (bagColorModel.getColorCodeChipB5() != 0) {
                val intColor = bagColorModel.getColorCodeChipB5()
                val hexColor = "#" + Integer.toHexString(intColor).substring(2)
                colorData.addProperty("colorb5", hexColor)
            }


            Log.d("colorData", colorData.toString())

            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            apiService.updateBagColor(colorData).enqueue(object : Callback<MsgUpdateColor> {
                override fun onResponse(call: Call<MsgUpdateColor>, response: Response<MsgUpdateColor>) {
                    if (response.isSuccessful && response.body() != null) {
                        val msgUpdateBagColor = response.body()!!
                        Log.d("msgUpdateBagColor", msgUpdateBagColor.toString())
                        Toast.makeText(context, "บันทึกข้อมูลสีกระเป๋าเรียบร้อยแล้ว", Toast.LENGTH_LONG).show()
                        bagColorModel.setOnSave(true)
                        this@BagColorDf.fetchBagColor()
                    } else {
                        Toast.makeText(context, "บันทึกข้อมูลสีกระเป๋าไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MsgUpdateColor>, t: Throwable) {
                    Log.d("RetrofitError", t.message ?: "Unknown error")
                    Toast.makeText(context, "เกิดข้อผิดพลาดในการบันทึก", Toast.LENGTH_SHORT).show()
                }
            })
        }

    }

    private fun initDateYear() {
        val dt = DateTime()
        bagColorModel.setCurrentYear(dt.year.toString())
        bagColorModel.setNextYear(dt.plusYears(1).year.toString())
    }

    private fun initUser() {
        bagColorModel.setUserName(this.userZ!!.userName ?: "ไม่ระบุชื่อ")
        if (this.ageCurrent != null) {
            bagColorModel.setUserAge("อายุ: ${this.ageCurrent} ย่าง ${this.ageCurrent!! + 1} ปี")
        } else {
            bagColorModel.setUserAge("ไม่ระบุอายุ")
        }
    }

    private fun insertBagColor(userId: String?, age: Int?) {

        val colorData = JsonObject()

        colorData.addProperty("memberid", userId)
        colorData.addProperty("age", age)
        colorData.addProperty("bag_color1a", "#FFFFFF")
        colorData.addProperty("bag_color2a", "#FFFFFF")
        colorData.addProperty("bag_color3a", "#FFFFFF")
        colorData.addProperty("bag_color4a", "#FFFFFF")
        colorData.addProperty("bag_color5a", "#FFFFFF")
        colorData.addProperty("bag_color6a", "#FFFFFF")

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.addBagColor(colorData).enqueue(object : Callback<MsgAddBagColor> {
            override fun onResponse(call: Call<MsgAddBagColor>, response: Response<MsgAddBagColor>) {
                if (response.isSuccessful && response.body() != null) {
                    val msgAddBagColor = response.body()!!
                    Log.d("msgAddBagColor", msgAddBagColor.insertColor!!)
                }
            }

            override fun onFailure(call: Call<MsgAddBagColor>, t: Throwable) {
                Log.d("RetrofitError", t.message ?: "Unknown error")
            }
        })
    }

    private fun setColorToBags(colorSix: ColorSix) {
        if (colorSix.colorSixA != null) {
            val color1 = colorSix.colorSixA.bagColor1
            val color2 = colorSix.colorSixA.bagColor2
            val color3 = colorSix.colorSixA.bagColor3
            val color4 = colorSix.colorSixA.bagColor4
            val color5 = colorSix.colorSixA.bagColor5
            val color6 = colorSix.colorSixA.bagColor6

            if (color1 != null) { bagColorModel.setColorChip0(color1) }
            if (color2 != null) { bagColorModel.setColorChip1(color2) }
            if (color3 != null) { bagColorModel.setColorChip2(color3) }
            if (color4 != null) { bagColorModel.setColorChip3(color4) }
            if (color5 != null) { bagColorModel.setColorChip4(color5) }
            if (color6 != null) { bagColorModel.setColorChip5(color6) }
            
            bagColorModel.setLastUpdateA(colorSix.colorSixA.dateColorUpdated ?: "")
        } else {
            bagColorModel.setLastUpdateA("")
        }

        if (colorSix.colorSixB != null) {
            val color1 = colorSix.colorSixB.bagColor1
            val color2 = colorSix.colorSixB.bagColor2
            val color3 = colorSix.colorSixB.bagColor3
            val color4 = colorSix.colorSixB.bagColor4
            val color5 = colorSix.colorSixB.bagColor5
            val color6 = colorSix.colorSixB.bagColor6

            if (color1 != null) { bagColorModel.setColorChipB0(color1) }
            if (color2 != null) { bagColorModel.setColorChipB1(color2) }
            if (color3 != null) { bagColorModel.setColorChipB2(color3) }
            if (color4 != null) { bagColorModel.setColorChipB3(color4) }
            if (color5 != null) { bagColorModel.setColorChipB4(color5) }
            if (color6 != null) { bagColorModel.setColorChipB5(color6) }
            
            bagColorModel.setLastUpdateB(colorSix.colorSixB.dateColorUpdated ?: "")
        } else {
            bagColorModel.setLastUpdateB("")
        }
        refreshChipColors()
    }

    private fun sendNotification() {
        if (userZ?.memberId == null) {
            Toast.makeText(requireContext(), "ไม่พบรหัสผู้ใช้", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSendNoti.isEnabled = false
        Toast.makeText(requireContext(), "กำลังส่งการแจ้งเตือน...", Toast.LENGTH_SHORT).show()
        
        Log.d("BagColorDf", "Sending notification for userId: ${userZ!!.memberId}")
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.sendBagColorNoti(userZ!!.memberId!!).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                binding.btnSendNoti.isEnabled = true
                Log.d("BagColorDf", "Notification Response: ${response.code()} ${response.body()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val resultJson = response.body()!!
                    val status = if (resultJson.has("status")) resultJson.get("status").asString else null
                    
                    if (status == "completed") {
                        val sentCount = if (resultJson.has("sent_count")) resultJson.get("sent_count").asInt else 0
                        if (sentCount > 0) {
                            Toast.makeText(requireContext(), "ส่งการแจ้งเตือนสำเร็จครับ ✅", Toast.LENGTH_SHORT).show()
                        } else {
                            // Extract Debug Info
                            var reason = "ผู้ใช้อาจไม่มี FCM Token"
                            if (resultJson.has("details")) {
                                val details = resultJson.getAsJsonArray("details")
                                if (details.size() > 0) {
                                    val detail = details[0].asJsonObject
                                    if (detail.has("debug")) {
                                        val dbg = detail.getAsJsonObject("debug")
                                        val bagFound = dbg.get("bag_found").asBoolean
                                        val hasToken = dbg.get("has_token").asBoolean
                                        val srvAcc = dbg.get("service_acc_exists").asBoolean
                                        
                                        if (!hasToken) reason = "DB: ไม่มี Token"
                                        else if (!bagFound) reason = "DB: ไม่พบข้อมูลสีกระเป๋า"
                                        else if (!srvAcc) reason = "Server: ไม่พบ service-account.json"
                                        else reason = "FCM Send Failed: ${dbg.get("fcm_response_raw")}"
                                    } else if (detail.has("status")) {
                                         reason = "Status: ${detail.get("status").asString}"
                                    }
                                }
                            }
                            Toast.makeText(requireContext(), "ส่งไม่สำเร็จ ($reason)", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val msg = if (resultJson.has("message")) resultJson.get("message").asString else "Server error"
                        Toast.makeText(requireContext(), "แจ้งเตือน: $msg", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMsg = "ส่งไม่สำเร็จ (Error: ${response.code()})"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                binding.btnSendNoti.isEnabled = true
                Log.e("BagColorDf", "Retrofit notification request failed: ${t.message}")
                Toast.makeText(requireContext(), "ข้อผิดพลาดระบบ: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun refreshChipColors() {
        val obs = bagColorModel.bagColorObs
        binding.chip00.backgroundTintList = ColorStateList.valueOf(obs.colorChip0)
        binding.chip01.backgroundTintList = ColorStateList.valueOf(obs.colorChip1)
        binding.chip02.backgroundTintList = ColorStateList.valueOf(obs.colorChip2)
        binding.chip03.backgroundTintList = ColorStateList.valueOf(obs.colorChip3)
        binding.chip04.backgroundTintList = ColorStateList.valueOf(obs.colorChip4)
        binding.chip05.backgroundTintList = ColorStateList.valueOf(obs.colorChip5)

        binding.chipB00.backgroundTintList = ColorStateList.valueOf(obs.colorChipb0)
        binding.chipB01.backgroundTintList = ColorStateList.valueOf(obs.colorChipb1)
        binding.chipB02.backgroundTintList = ColorStateList.valueOf(obs.colorChipb2)
        binding.chipB03.backgroundTintList = ColorStateList.valueOf(obs.colorChipb3)
        binding.chipB04.backgroundTintList = ColorStateList.valueOf(obs.colorChipb4)
        binding.chipB05.backgroundTintList = ColorStateList.valueOf(obs.colorChipb5)

        // Status image
        if (obs.imgSave == 0) {
            binding.imgSaveStatus.setImageResource(R.drawable.star_save)
            binding.imgSaveStatus.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFC107"))
        } else {
            binding.imgSaveStatus.setImageResource(R.drawable.check)
            binding.imgSaveStatus.imageTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        }
    }


    private fun setChipInit() {

        binding.chip00.setOnClickListener(this)
        binding.chip01.setOnClickListener(this)
        binding.chip02.setOnClickListener(this)
        binding.chip03.setOnClickListener(this)
        binding.chip04.setOnClickListener(this)
        binding.chip05.setOnClickListener(this)

        binding.chipB00.setOnClickListener(this)
        binding.chipB01.setOnClickListener(this)
        binding.chipB02.setOnClickListener(this)
        binding.chipB03.setOnClickListener(this)
        binding.chipB04.setOnClickListener(this)
        binding.chipB05.setOnClickListener(this)
        binding.btnAddColor.setOnClickListener(this)
        binding.btnSendNoti.setOnClickListener(this)

    }

    override fun onClick(v: View?) {

        var bagPalette: DialogFragment? = null

        when (v) {
            binding.chip00 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chip00.id)
                bagColorModel.setOnSave(false); refreshChipColors()

            }
            binding.chip01 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chip01.id)
                bagColorModel.setOnSave(false); refreshChipColors()

            }
            binding.chip02 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chip02.id)
                bagColorModel.setOnSave(false); refreshChipColors()

            }
            binding.chip03 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chip03.id)
                bagColorModel.setOnSave(false); refreshChipColors()

            }
            binding.chip04 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chip04.id)
                bagColorModel.setOnSave(false); refreshChipColors()

            }
            binding.chip05 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chip05.id)
                bagColorModel.setOnSave(false); refreshChipColors()
            }

            binding.chipB00 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chipB00.id)
                bagColorModel.setOnSave(false); refreshChipColors()

            }
            binding.chipB01 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chipB01.id)
                bagColorModel.setOnSave(false); refreshChipColors()

            }
            binding.chipB02 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chipB02.id)
                bagColorModel.setOnSave(false); refreshChipColors()

            }
            binding.chipB03 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chipB03.id)
                bagColorModel.setOnSave(false); refreshChipColors()

            }
            binding.chipB04 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chipB04.id)
                bagColorModel.setOnSave(false); refreshChipColors()

            }
            binding.chipB05 -> {
                bagPalette = BagPaletteColorF.newInstance(binding.chipB05.id)
                bagColorModel.setOnSave(false); refreshChipColors()
            }

            binding.btnSendNoti -> {
                sendNotification()
            }
        }

        bagPalette?.show(childFragmentManager, "BagPaletteColorF")
    }

    private fun fetchBagColor() {
        if (userZ == null || userZ!!.memberId == null) return
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getColorSixByUserId(userZ!!.memberId!!).enqueue(object : Callback<ColorSix> {
            override fun onResponse(call: Call<ColorSix>, response: Response<ColorSix>) {
                if (response.isSuccessful && response.body() != null) {
                    val colorSix = response.body()!!
                    this@BagColorDf.setColorToBags(colorSix)

                    if (colorSix.colorSixA == null) {
                        Log.d("BagColorDf", "ColorSixA is null, inserting...")
                        this@BagColorDf.insertBagColor(userZ!!.memberId, ageCurrent)
                    }

                    if (colorSix.colorSixB == null) {
                        Log.d("BagColorDf", "ColorSixB is null, inserting...")
                        this@BagColorDf.insertBagColor(userZ!!.memberId, if (ageCurrent != null) ageCurrent!! + 1 else null)
                    }
                } else {
                    Toast.makeText(context, "โหลดสีไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ColorSix>, t: Throwable) {
                Log.e("BagColorDf", "Refetch failed: ${t.message}")
                Toast.makeText(context, "โหลดสีไม่สำเร็จ: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun selectListener(defaultColor: Int, chipId: Int) {
        val hexColor = "#" + Integer.toHexString(defaultColor).substring(2)
        when (chipId) {
            //binding.chip00.id -> binding.chip00.chipBackgroundColor = ColorStateList.valueOf(defaultColor)
            binding.chip00.id -> bagColorModel.setColorChip0(hexColor)
            binding.chip01.id -> bagColorModel.setColorChip1(hexColor)
            binding.chip02.id -> bagColorModel.setColorChip2(hexColor)
            binding.chip03.id -> bagColorModel.setColorChip3(hexColor)
            binding.chip04.id -> bagColorModel.setColorChip4(hexColor)
            binding.chip05.id -> bagColorModel.setColorChip5(hexColor)

            binding.chipB00.id -> bagColorModel.setColorChipB0(hexColor)
            binding.chipB01.id -> bagColorModel.setColorChipB1(hexColor)
            binding.chipB02.id -> bagColorModel.setColorChipB2(hexColor)
            binding.chipB03.id -> bagColorModel.setColorChipB3(hexColor)
            binding.chipB04.id -> bagColorModel.setColorChipB4(hexColor)
            binding.chipB05.id -> bagColorModel.setColorChipB5(hexColor)
        }
        refreshChipColors()
    }


}
