package com.numberniceic.ui.auth

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.adapters.DayNumAdapter
import com.numberniceic.adapters.ProvinceAdapter
import com.numberniceic.data.admin.Serverx
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.utils.UserContextManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class UserEditProfileF : androidx.fragment.app.Fragment(), AdapterView.OnItemSelectedListener {

    private var sGender: String = ""
    private var sDay: String? = null
    private var sMonth: String? = null
    private var sYear: String? = null
    private var sProvince: String? = null
    private var sHour: String? = null
    private var sMinute: String? = null
    private var sAvatar: String = "10"

    private val genders = arrayListOf<String>()
    private val years = arrayListOf<String>()
    private val months = arrayListOf<String>()
    private val days = arrayListOf<String>()
    private val hours = arrayListOf<String>()
    private val minutes = arrayListOf<String>()
    private val provinces = arrayListOf<String>()

    private lateinit var radGenderGroup: RadioGroup
    private lateinit var btnSaveProfile: Button
    private lateinit var btnCancel: Button
    private lateinit var spinYear: Spinner
    private lateinit var spinDay: Spinner
    private lateinit var spinMonth: Spinner
    private lateinit var spinTimeHour: Spinner
    private lateinit var spinTimeMinute: Spinner
    private lateinit var spinProvince: Spinner
    private lateinit var edtRealName: EditText
    private lateinit var edtSurname: EditText
    private lateinit var txtUsername: TextView
    private lateinit var imgAvatar10: com.google.android.material.imageview.ShapeableImageView
    private lateinit var imgAvatar11: com.google.android.material.imageview.ShapeableImageView
    private lateinit var imgAvatar12: com.google.android.material.imageview.ShapeableImageView
    private lateinit var imgAvatar13: com.google.android.material.imageview.ShapeableImageView
    private lateinit var edtShippingAddress: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        radGenderGroup = view.findViewById(R.id.rad_gender_group)
        btnSaveProfile = view.findViewById(R.id.btn_save_profile)
        btnCancel = view.findViewById(R.id.btn_cancel_edit)
        spinYear = view.findViewById(R.id.spin_year)
        spinDay = view.findViewById(R.id.spin_day)
        spinMonth = view.findViewById(R.id.spin_month)
        spinTimeHour = view.findViewById(R.id.spin_time_hour)
        spinTimeMinute = view.findViewById(R.id.spin_time_minute)
        spinProvince = view.findViewById(R.id.spin_province)
        edtRealName = view.findViewById(R.id.edt_realname)
        edtSurname = view.findViewById(R.id.edt_surname)
        txtUsername = view.findViewById(R.id.txt_username_readonly)
        imgAvatar10 = view.findViewById(R.id.img_avatar_10)
        imgAvatar11 = view.findViewById(R.id.img_avatar_11)
        imgAvatar12 = view.findViewById(R.id.img_avatar_12)
        imgAvatar13 = view.findViewById(R.id.img_avatar_13)
        edtShippingAddress = view.findViewById(R.id.edt_shipping_address)
        
        setupAvatarSelection()

        initForm(view)
        loadCurrentUserData()

        btnSaveProfile.setOnClickListener {
            if (validateData()) {
                updateProfile()
            }
        }

        btnCancel.setOnClickListener {
            closeSession()
        }
    }

    private fun initForm(view: View) {
        genders.clear()
        months.clear()
        days.clear()
        hours.clear()
        minutes.clear()
        provinces.clear()
        years.clear()

        genders.addAll(resources.getStringArray(R.array.gender_array))
        months.addAll(resources.getStringArray(R.array.months_array))
        days.addAll(resources.getStringArray(R.array.days_array))
        hours.addAll(resources.getStringArray(R.array.time_array))
        minutes.addAll(resources.getStringArray(R.array.minute_array))
        provinces.addAll(resources.getStringArray(R.array.province_array))

        val cyear = Calendar.getInstance().get(Calendar.YEAR)
        years.add("ปี")
        for (i in (cyear + 543) downTo 2499) {
            years.add(i.toString())
        }

        spinYear.adapter = DayNumAdapter(view.context, years)
        spinDay.adapter = DayNumAdapter(view.context, days)
        spinMonth.adapter = DayNumAdapter(view.context, months)
        spinTimeHour.adapter = DayNumAdapter(view.context, hours)
        spinTimeMinute.adapter = DayNumAdapter(view.context, minutes)
        spinProvince.adapter = ProvinceAdapter(view.context, provinces)

        spinDay.onItemSelectedListener = this
        spinMonth.onItemSelectedListener = this
        spinYear.onItemSelectedListener = this
        spinTimeHour.onItemSelectedListener = this
        spinTimeMinute.onItemSelectedListener = this
        spinProvince.onItemSelectedListener = this
    }

    private fun setupAvatarSelection() {
        val listener = View.OnClickListener { v ->
            when (v.id) {
                R.id.img_avatar_10 -> selectAvatar("10")
                R.id.img_avatar_11 -> selectAvatar("11")
                R.id.img_avatar_12 -> selectAvatar("12")
                R.id.img_avatar_13 -> selectAvatar("13")
            }
        }
        imgAvatar10.setOnClickListener(listener)
        imgAvatar11.setOnClickListener(listener)
        imgAvatar12.setOnClickListener(listener)
        imgAvatar13.setOnClickListener(listener)
    }

    private fun selectAvatar(id: String) {
        sAvatar = id
        
        // Reset all strokes
        val avatars = listOf(imgAvatar10, imgAvatar11, imgAvatar12, imgAvatar13)
        for (avatar in avatars) {
            avatar.strokeWidth = 0f
        }
        
        // Set stroke for selected avatar
        val selectedColor = resources.getColor(R.color.colorAccent) // หรือใช้สีที่ต้องการ
        when (id) {
            "10" -> {
                imgAvatar10.strokeWidth = 6f
                imgAvatar10.strokeColor = android.content.res.ColorStateList.valueOf(selectedColor)
            }
            "11" -> {
                imgAvatar11.strokeWidth = 6f
                imgAvatar11.strokeColor = android.content.res.ColorStateList.valueOf(selectedColor)
            }
            "12" -> {
                imgAvatar12.strokeWidth = 6f
                imgAvatar12.strokeColor = android.content.res.ColorStateList.valueOf(selectedColor)
            }
            "13" -> {
                imgAvatar13.strokeWidth = 6f
                imgAvatar13.strokeColor = android.content.res.ColorStateList.valueOf(selectedColor)
            }
            else -> {
                imgAvatar10.strokeWidth = 6f
                imgAvatar10.strokeColor = android.content.res.ColorStateList.valueOf(selectedColor)
            }
        }
    }

    private fun loadCurrentUserData() {
        val user = UserContextManager.userX(requireContext()) ?: return

        edtRealName.setText(user.realName)
        edtSurname.setText(user.surname)
        txtUsername.text = user.username

        // Handle Birthday (YYYY-MM-DD)
        user.birthDay?.let { bday ->
            val parts = bday.split("-")
            if (parts.size == 3) {
                val yearAD = parts[0].toIntOrNull() ?: 0
                val month = parts[1].toIntOrNull() ?: 0
                val day = parts[2].toIntOrNull() ?: 0

                if (yearAD > 0) {
                    val yearBE = yearAD + 543
                    val yearPos = years.indexOf(yearBE.toString())
                    if (yearPos >= 0) {
                        spinYear.setSelection(yearPos)
                        sYear = years[yearPos]
                    }
                }
                
                if (month in 1..12) {
                    spinMonth.setSelection(month) // Assuming index matches month number (0 is "Month")
                    if (month < months.size) sMonth = months[month]
                }
                
                val dayPos = days.indexOf(day.toString())
                if (dayPos >= 0) {
                    spinDay.setSelection(dayPos)
                    sDay = days[dayPos]
                } else {
                     // Try with 0 padding if formatted that way in array
                    val dayPosPad = days.indexOf(String.format("%02d", day))
                    if (dayPosPad >= 0) {
                        spinDay.setSelection(dayPosPad)
                        sDay = days[dayPosPad]
                    }
                }
            }
        }

        // Time
        val hourStr = String.format("%02d", user.sHour)
        val hourPos = hours.indexOf(hourStr)
        if (hourPos >= 0) {
            spinTimeHour.setSelection(hourPos)
            sHour = hours[hourPos]
        }

        val minuteStr = String.format("%02d", user.sMinute)
        val minutePos = minutes.indexOf(minuteStr)
        if (minutePos >= 0) {
            spinTimeMinute.setSelection(minutePos)
            sMinute = minutes[minutePos]
        }

        // Province
        var provExists = false
        user.sProvince?.let { userProv ->
            val provPos = provinces.indexOf(userProv)
            if (provPos >= 0) {
                spinProvince.setSelection(provPos)
                sProvince = userProv
                provExists = true
            }
        }
        
        if (!provExists) {
            Toast.makeText(requireContext(), "กรุณาเลือกจังหวัด", Toast.LENGTH_LONG).show()
        }
        
        // Gender
        if (user.sGender == "ช") {
            view?.findViewById<RadioButton>(R.id.radio_man)?.isChecked = true
            sGender = "ช"
        } else if (user.sGender == "ญ") {
            view?.findViewById<RadioButton>(R.id.radio_woman)?.isChecked = true
            sGender = "ญ"
        }
        
        // Avatar
        selectAvatar(user.avatar ?: "1")

        // Shipping Address
        edtShippingAddress.setText(user.address ?: user.shippingAddress ?: "")
    }

    private fun validateData(): Boolean {
        sGender = when (radGenderGroup.checkedRadioButtonId) {
            R.id.radio_man -> "ช"
            R.id.radio_woman -> "ญ"
            else -> ""
        }

        if (edtRealName.text.isBlank()) {
            Toast.makeText(context, "กรุณากรอกชื่อจริง", Toast.LENGTH_SHORT).show()
            return false
        }
        if (edtSurname.text.isBlank()) {
            Toast.makeText(context, "กรุณากรอกนามสกุล", Toast.LENGTH_SHORT).show()
            return false
        }
        if (sDay == null || sDay == "วัน") {
            Toast.makeText(context, "กรุณาเลือกวันเกิด", Toast.LENGTH_SHORT).show()
            return false
        }
        if (sMonth == null || sMonth == "0") {
            Toast.makeText(context, "กรุณาเลือกเดือนเกิด", Toast.LENGTH_SHORT).show()
            return false
        }
        if (sYear == null || sYear == "ปี") {
            Toast.makeText(context, "กรุณาเลือกปีเกิด", Toast.LENGTH_SHORT).show()
            return false
        }
        if (sProvince == null || sProvince == "จังหวัด") {
             Toast.makeText(context, "กรุณาเลือกจังหวัด", Toast.LENGTH_SHORT).show()
             return false
        }
        if (sGender.isEmpty()) {
            Toast.makeText(context, "กรุณาเลือกเพศ", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun updateProfile() {
        val user = UserContextManager.userX(requireContext()) ?: return
        val json = JsonObject()
        json.addProperty("memberid", user.userId)
        json.addProperty("realname", edtRealName.text.toString())
        json.addProperty("surname", edtSurname.text.toString())
        json.addProperty("sday", sDay)
        json.addProperty("smonth", sMonth)
        json.addProperty("syear", sYear)
        json.addProperty("shour", sHour ?: "00")
        json.addProperty("sminute", sMinute ?: "00")
        json.addProperty("sprovince", sProvince ?: "กรุงเทพมหานคร")
        json.addProperty("sgender", sGender)
        json.addProperty("avatar", sAvatar)
        json.addProperty("address", edtShippingAddress.text.toString())

        RetrofitClient.api.userUpdateData(json).enqueue(object : Callback<Serverx> {
            override fun onResponse(call: Call<Serverx>, response: Response<Serverx>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.serverx?.message == "success") {
                        // Update local storage
                        val editor = requireContext().getSharedPreferences("userdata", Context.MODE_PRIVATE).edit()
                        editor.putString("json", Gson().toJson(body.userx))
                        editor.apply()
                        
                        Toast.makeText(context, "แก้ไขข้อมูลสำเร็จ", Toast.LENGTH_LONG).show()
                        closeSession()
                    } else {
                        Toast.makeText(context, "ผิดพลาด: ${body.serverx?.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "เกิดข้อผิดพลาดจากเซิร์ฟเวอร์", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Serverx>, t: Throwable) {
                Toast.makeText(context, "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun closeSession() {
        if (parentFragment is DialogFragment) {
            (parentFragment as DialogFragment).dismiss()
        } else {
            // If it's a regular fragment in a container
            parentFragmentManager.popBackStack()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            R.id.spin_day -> sDay = days.getOrNull(position)
            R.id.spin_month -> {
                // If position 0 is "Month" placeholder
                 sMonth = if (position > 0) String.format("%02d", position) else "0"
            }
            R.id.spin_year -> sYear = years.getOrNull(position)
            R.id.spin_time_hour -> sHour = hours.getOrNull(position)
            R.id.spin_time_minute -> sMinute = minutes.getOrNull(position)
            R.id.spin_province -> sProvince = provinces.getOrNull(position)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
