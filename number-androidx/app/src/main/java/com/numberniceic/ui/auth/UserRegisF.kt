package com.numberniceic.ui.auth


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.*
import com.google.gson.JsonObject
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.utils.GuestManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.numberniceic.R
import com.numberniceic.adapters.DayNumAdapter
import com.numberniceic.adapters.ProvinceAdapter
import com.numberniceic.data.admin.ServerMessage
import com.numberniceic.ui.person.ConfirmRegisF
import java.util.*
import kotlin.collections.ArrayList


class UserRegisF : androidx.fragment.app.Fragment(), AdapterView.OnItemSelectedListener, ConfirmRegisF.OnUsercfListener {


    private lateinit var sGenger: String
    private var sDay: String? = null
    private var sMonth: String? = null
    private var sYear: String? = null
    private var sProvince: String? = null
    private var sHour: String? = null
    private var sMinute: String? = null
    private var sAvatar: String = "10"

    private var gengers = arrayListOf<String>()
    private var years = arrayListOf<String>()
    private var months = arrayListOf<String>()
    private var days = arrayListOf<String>()
    private var hour = arrayListOf<String>()
    private var minute = arrayListOf<String>()
    private var provinces = arrayListOf<String>()

    private lateinit var rad_gender_group: RadioGroup
    private lateinit var btn_user_register_full: Button
    private lateinit var spin_year: Spinner
    private lateinit var spin_day: Spinner
    private lateinit var spin_month: Spinner
    private lateinit var spin_time_hour: Spinner
    private lateinit var spin_time_minute: Spinner
    private lateinit var spin_province: Spinner
    private lateinit var edt_realname: EditText
    private lateinit var edt_surname: EditText
    private lateinit var edt_username: EditText
    private lateinit var edt_password: EditText
    private lateinit var edt_passwordf: EditText
    private lateinit var imgAvatar10: com.google.android.material.imageview.ShapeableImageView
    private lateinit var imgAvatar11: com.google.android.material.imageview.ShapeableImageView
    private lateinit var imgAvatar12: com.google.android.material.imageview.ShapeableImageView
    private lateinit var imgAvatar13: com.google.android.material.imageview.ShapeableImageView
    private lateinit var btn_toggle_address: com.google.android.material.button.MaterialButton
    private lateinit var layout_address_input: LinearLayout
    private lateinit var edt_shipping_address: EditText


    companion object {
        fun newInstance(): UserRegisF {

            val fragment = UserRegisF()

            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_regis, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rad_gender_group = view.findViewById(R.id.rad_gender_group)
        btn_user_register_full = view.findViewById(R.id.btn_user_register_full)
        spin_year = view.findViewById(R.id.spin_year)
        spin_day = view.findViewById(R.id.spin_day)
        spin_month = view.findViewById(R.id.spin_month)
        spin_time_hour = view.findViewById(R.id.spin_time_hour)
        spin_time_minute = view.findViewById(R.id.spin_time_minute)
        spin_province = view.findViewById(R.id.spin_province)
        edt_realname = view.findViewById(R.id.edt_realname)
        edt_surname = view.findViewById(R.id.edt_surname)
        edt_username = view.findViewById(R.id.edt_username)
        edt_password = view.findViewById(R.id.edt_password)
        edt_password = view.findViewById(R.id.edt_password)
        edt_passwordf = view.findViewById(R.id.edt_passwordf)
        imgAvatar10 = view.findViewById(R.id.img_avatar_10)
        imgAvatar11 = view.findViewById(R.id.img_avatar_11)
        imgAvatar12 = view.findViewById(R.id.img_avatar_12)
        imgAvatar13 = view.findViewById(R.id.img_avatar_13)
        btn_toggle_address = view.findViewById(R.id.btn_toggle_address)
        layout_address_input = view.findViewById(R.id.layout_address_input)
        edt_shipping_address = view.findViewById(R.id.edt_shipping_address)

        btn_toggle_address.setOnClickListener {
            if (layout_address_input.visibility == View.VISIBLE) {
                layout_address_input.visibility = View.GONE
                btn_toggle_address.text = "เพิ่มข้อมูลที่อยู่จัดส่ง (ไม่บังคับ)"
            } else {
                layout_address_input.visibility = View.VISIBLE
                btn_toggle_address.text = "ซ่อนข้อมูลที่อยู่จัดส่ง"
            }
        }
        
        setupAvatarSelection()

        initRegisForm(view)
        rad_gender_group.clearCheck()

        btn_user_register_full.setOnClickListener {

            if (completelyDataUser()) {
                sendChkDataServerComplete()
            }
        }
    }


    private fun initRegisForm(view: View) {
        gengers.clear()
        months.clear()
        days.clear()
        hour.clear()
        minute.clear()
        provinces.clear()
        years.clear()

        gengers.addAll(resources.getStringArray(R.array.gender_array))
        months.addAll(resources.getStringArray(R.array.months_array))
        days.addAll(resources.getStringArray(R.array.days_array))
        hour.addAll(resources.getStringArray(R.array.time_array))
        minute.addAll(resources.getStringArray(R.array.minute_array))
        provinces.addAll(resources.getStringArray(R.array.province_array))


        val cyear = Calendar.getInstance().get(Calendar.YEAR)
        years.add("ปี")
        for (i in (cyear + 543) downTo 2499) {
            years.add(i.toString())
        }

        val yearNumAdapter = DayNumAdapter(view.context, years)
        val dayNumAdapter = DayNumAdapter(view.context, days)
        val monthNumAdapter = DayNumAdapter(view.context, months)

        val hourAdapter = DayNumAdapter(view.context, hour)
        val minuteAdapter = DayNumAdapter(view.context, minute)
        val provinceAdapter = ProvinceAdapter(view.context, provinces)

        spin_year.adapter = yearNumAdapter
        spin_day.adapter = dayNumAdapter
        spin_month.adapter = monthNumAdapter

        spin_time_hour.adapter = hourAdapter
        spin_time_minute.adapter = minuteAdapter

        spin_province.adapter = provinceAdapter


        spin_day.onItemSelectedListener = this
        spin_month.onItemSelectedListener = this
        spin_year.onItemSelectedListener = this

        spin_time_hour.onItemSelectedListener = this
        spin_time_minute.onItemSelectedListener = this
        spin_province.onItemSelectedListener = this

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
        
        // Select default
        selectAvatar("10")
    }

    private fun selectAvatar(id: String) {
        sAvatar = id
        
        // Reset all strokes
        val avatars = listOf(imgAvatar10, imgAvatar11, imgAvatar12, imgAvatar13)
        for (avatar in avatars) {
            avatar.strokeWidth = 0f
        }
        
        // Set stroke for selected avatar
        val selectedColor = resources.getColor(R.color.colorAccent)
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

    private fun sendChkDataServerComplete() {

        val userdata = JsonObject()

        userdata.addProperty("realname", edt_realname.text.toString())
        userdata.addProperty("surname", edt_surname.text.toString())
        userdata.addProperty("username", edt_username.text.toString())
        userdata.addProperty("password", edt_password.text.toString())

        userdata.addProperty("sday", this.sDay)
        userdata.addProperty("smonth", this.sMonth)
        userdata.addProperty("syear", this.sYear)
        userdata.addProperty("shour", this.sHour)
        userdata.addProperty("sminute", this.sMinute)
        userdata.addProperty("sprovince", this.sProvince)
        userdata.addProperty("sgender", this.sGenger)
        userdata.addProperty("avatar", this.sAvatar)
        
        // Handle guest data if coming from guest payment
        val guestManager = GuestManager(requireContext())
        if (guestManager.isGuestUser()) {
            guestManager.upgradeToMember(userdata)
        }
        
        val shipping_address = if (layout_address_input.visibility == View.VISIBLE) {
            edt_shipping_address.text.toString()
        } else {
            guestManager.getTemporaryAddress() ?: ""
        }
        userdata.addProperty("address", shipping_address) // Use 'address' instead of 'shipping_address'

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.userRegister(userdata).enqueue(object : Callback<ServerMessage> {
            override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                if (response.isSuccessful && response.body() != null) {
                    val sermessage = response.body()!!
                    Log.d("server message", "message: ${sermessage.message}, activity: ${sermessage.activity}")

                    if (sermessage.message == "dup") {
                        Toast.makeText(context, "ไม่สามารถใข้ User Name นี้ได้ กรุณาใช้ชื่ออื่น!!", Toast.LENGTH_LONG).show()
                    }

                    if (sermessage.message == "presuccess") {
                         val dilogFragmentCf = ConfirmRegisF()
                         dilogFragmentCf.show(childFragmentManager, "ConfirmRegisF")
                    }
                } else {
                    Toast.makeText(context, "เซิร์ฟเวอร์ขัดข้อง: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun insertTableServer() {

        val userdata = JsonObject()

        userdata.addProperty("realname", edt_realname.text.toString())
        userdata.addProperty("surname", edt_surname.text.toString())
        userdata.addProperty("username", edt_username.text.toString())
        userdata.addProperty("password", edt_password.text.toString())

        userdata.addProperty("sday", this.sDay)
        userdata.addProperty("smonth", this.sMonth)
        userdata.addProperty("syear", this.sYear)
        userdata.addProperty("shour", this.sHour)
        userdata.addProperty("sminute", this.sMinute)
        userdata.addProperty("sprovince", this.sProvince)
        userdata.addProperty("sgender", this.sGenger)
        userdata.addProperty("avatar", this.sAvatar)
        val shipping_address = if (layout_address_input.visibility == View.VISIBLE) edt_shipping_address.text.toString() else ""
        userdata.addProperty("shipping_address", shipping_address)

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.userRegisInsert(userdata).enqueue(object : Callback<ServerMessage> {
            override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                Log.d("USERCF", "insertTableServer response code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val sermessage = response.body()!!
                    Log.d("USERCF", "insertTableServer response message: ${sermessage.message}")

                    if (sermessage.message == "success") {
                        performAutoLogin()
                    } else {
                        val debugInfo = if (sermessage.debug != null) " (Debug: ${sermessage.debug})" else ""
                        Toast.makeText(context, "ลงทะเบียนไม่สำเร็จ: ${sermessage.message}$debugInfo", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Workaround for server bug: check if error body contains success even if 500
                    val errorBody = response.errorBody()?.string()
                    Log.d("USERCF", "Error body: $errorBody")
                    if (errorBody != null && errorBody.contains("\"message\":\"success\"")) {
                        Log.d("USERCF", "Detected 'success' in error body (500), proceeding with auto-login")
                        performAutoLogin()
                    } else {
                        Toast.makeText(context, "Server Error (Regis): ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            private fun performAutoLogin() {
                // Auto-login since the user is now registered
                val username = edt_username.text.toString()
                val password = edt_password.text.toString()
                
                val loginData = JsonObject()
                loginData.addProperty("username", username)
                loginData.addProperty("password", password)
                
                val apiServiceLogin = RetrofitClient.instance.create(ApiService::class.java)
                apiServiceLogin.userLogin(loginData).enqueue(object : Callback<com.numberniceic.data.admin.Serverx> {
                    override fun onResponse(call: Call<com.numberniceic.data.admin.Serverx>, loginResponse: Response<com.numberniceic.data.admin.Serverx>) {
                        Log.d("USERCF", "auto-login response code: ${loginResponse.code()}")
                        if (activity != null) {
                            if (loginResponse.isSuccessful && loginResponse.body()?.userx != null) {
                                Log.d("USERCF", "auto-login SUCCESS")
                                val serverx = loginResponse.body()!!
                                // Save user session
                                val sharedf = activity!!.getSharedPreferences("userdata", Context.MODE_PRIVATE)
                                sharedf.edit().putString("json", com.google.gson.Gson().toJson(serverx.userx)).apply()
                                
                                Toast.makeText(context, "ลงทะเบียนและเข้าสู่ระบบสำเร็จ!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "ลงทะเบียนสำเร็จแล้ว โปรดเข้าสู่ระบบ", Toast.LENGTH_LONG).show()
                            }
                            
                            val intent = Intent(context, com.numberniceic.ui.AppActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            intent.putExtra("show_dashboard", true)
                            startActivity(intent)
                            activity!!.finish()
                        }
                    }

                    override fun onFailure(call: Call<com.numberniceic.data.admin.Serverx>, t: Throwable) {
                        Log.e("USERCF", "auto-login Failure: ${t.message}")
                        if (activity != null) {
                            Toast.makeText(context, "ลงทะเบียนสำเร็จแล้ว โปรดเข้าสู่ระบบ", Toast.LENGTH_LONG).show()
                            val intent = Intent(context, com.numberniceic.ui.AppActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            activity!!.finish()
                        }
                    }
                })
            }

            override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                Log.e("USERCF", "insertTableServer Failure: ${t.message}", t)
                Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun completelyDataUser(): Boolean {

        sGenger = when (rad_gender_group.checkedRadioButtonId) {
            R.id.radio_man -> "ช"
            R.id.radio_woman -> "ญ"
            else -> ""
        }



        when {
            edt_realname.text.toString().isEmpty() -> {
                Toast.makeText(context, "ชื่อจริงห้ามว่าง!!", Toast.LENGTH_SHORT).show()
                edt_realname.requestFocus()
                return false
            }
            edt_surname.text.toString().isEmpty() -> {
                Toast.makeText(context, "นามสกุลห้ามว่าง!!", Toast.LENGTH_SHORT).show()
                edt_surname.requestFocus()
                return false
            }

            sDay == null || sDay == "วัน" -> {
                Toast.makeText(context, "โปรดวเลือกันเกิด!!", Toast.LENGTH_SHORT).show()
                spin_day.performClick()
                return false
            }

            sMonth == null || sMonth == "0" -> {
                Toast.makeText(context, "โปรดเลือกเดือนเกิด!!", Toast.LENGTH_SHORT).show()
                spin_month.performClick()
                return false
            }

            sYear == null || sYear == "ปี" -> {
                Toast.makeText(context, "โปรดเลือกปีเกิด!!", Toast.LENGTH_SHORT).show()
                spin_year.performClick()
                return false
            }

            sHour == null || sHour == "เวลา" -> {
                Toast.makeText(context, "โปรดเลือกเวลาเกิด!!", Toast.LENGTH_SHORT).show()
                spin_time_hour.performClick()
                return false
            }
            sMinute == null || sMinute == "นาที" -> {
                Toast.makeText(context, "โปรดเลือกนาทีเกิด!!", Toast.LENGTH_SHORT).show()
                spin_time_minute.performClick()
                return false
            }

            sProvince == null || sProvince == "จังหวัด" -> {
                Toast.makeText(context, "โปรดเลือกจังหวัดเกิด!!", Toast.LENGTH_SHORT).show()
                spin_province.performClick()
                return false
            }


            sGenger.isEmpty() -> {
                Toast.makeText(context, "โปรดเลือกเพศเกิด!!", Toast.LENGTH_SHORT).show()
                return false
            }

            edt_username.text.toString().isEmpty() -> {
                Toast.makeText(context, "User Name ห้ามว่าง!!", Toast.LENGTH_SHORT).show()
                edt_username.requestFocus()
                return false
            }


            edt_password.text.toString().isEmpty() -> {
                Toast.makeText(context, "Password ห้ามว่าง!!", Toast.LENGTH_SHORT).show()
                edt_username.requestFocus()
                return false
            }
            edt_password.text.toString() != edt_passwordf.text.toString() -> {
                Toast.makeText(context, "Password ไม่ตรงกัน!!", Toast.LENGTH_SHORT).show()
                edt_passwordf.requestFocus()
                return false
            }
        }


        val username = edt_username.text.toString()

        for (s in username) {
            if (s.isWhitespace()) {
                Toast.makeText(context, "username ห้ามมีวรรคตอน!!", Toast.LENGTH_SHORT).show()
                edt_username.requestFocus()
                return false
            }

        }


        return true

    }


    override fun onNothingSelected(parent: AdapterView<*>?) {

    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent != null) {

            when (parent.id) {
                R.id.spin_day -> {
                    sDay = getItemSelected(position, days)

                }
                R.id.spin_month -> {
                    // Force 2-digit month string to avoid any server-side ambiguity
                    sMonth = if (position > 0) String.format("%02d", position) else "0"
                }
                R.id.spin_year -> {
                    sYear = getItemSelected(position, years)

                }

                R.id.spin_province -> {
                    sProvince = getItemSelected(position, provinces)

                }

                R.id.spin_time_hour -> {
                    sHour = getItemSelected(position, hour)

                }
                R.id.spin_time_minute -> {
                    sMinute = getItemSelected(position, minute)

                }

            }

        }


    }

    private fun getItemSelected(position: Int, temList: ArrayList<String>): String? {

        for ((i, item) in temList.withIndex()) {
            if (i == position) {
                return item
            }
        }

        return null

    }
    override fun usercf(message: String) {
        Log.d("USERCF", message)

        if (message == "cf"){
            insertTableServer()

        }


    }

}
