package com.numberniceic.ui.apersonnews


import android.content.Intent
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.gson.JsonObject
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.numberniceic.R
import com.numberniceic.adapters.DayNumAdapter
import com.numberniceic.adapters.ProvinceAdapter
import com.numberniceic.data.admin.ServerMessage
import com.numberniceic.data.admin.Serverx
import com.numberniceic.data.admin.Userx
import com.numberniceic.ui.AppActivity
import com.numberniceic.ui.person.ConfirmUpdatef
import com.numberniceic.utils.UserContextManager



import java.util.*


class BirthDayF : Fragment(), AdapterView.OnItemSelectedListener, ConfirmUpdatef.OnUserUpdateListener {

    private lateinit var sGenger: String
    private var sDay: String? = null
    private var sMonth: String? = null
    private var sYear: String? = null
    private var sProvince: String? = null
    private var sHour: String? = null
    private var sMinute: String? = null

    private var gengers = arrayListOf<String>()
    private var years = arrayListOf<String>()
    private var months = arrayListOf<String>()
    private var days = arrayListOf<String>()
    private var hour = arrayListOf<String>()
    private var minute = arrayListOf<String>()
    private var provinces = arrayListOf<String>()

    private lateinit var btn_user_update: Button
    private lateinit var spin_year: Spinner
    private lateinit var spin_day: Spinner
    private lateinit var spin_month: Spinner
    private lateinit var spin_time_hour: Spinner
    private lateinit var spin_time_minute: Spinner
    private lateinit var spin_province: Spinner
    private lateinit var rad_gender_group: RadioGroup


    companion object {
        fun newInstance(): BirthDayF {

            val fragment = BirthDayF()

            return fragment
        }
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_birth_day, container, false)
    }

    private var userxId: String = ""
    private var userx: Userx? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_user_update = view.findViewById(R.id.btn_user_update)
        spin_year = view.findViewById(R.id.spin_year)
        spin_day = view.findViewById(R.id.spin_day)
        spin_month = view.findViewById(R.id.spin_month)
        spin_time_hour = view.findViewById(R.id.spin_time_hour)
        spin_time_minute = view.findViewById(R.id.spin_time_minute)
        spin_province = view.findViewById(R.id.spin_province)
        rad_gender_group = view.findViewById(R.id.rad_gender_group)

        initRegisForm(view)


        this.userx = UserContextManager.userX(view.context)
        if (this.userx != null){
           this.userxId = this.userx!!.userId!!

            Log.d("USERX", this.userx.toString())

        }


        btn_user_update.setOnClickListener {

            if (completelyDataUser()) {
                val dilogConfirmUpdatef = ConfirmUpdatef()
                dilogConfirmUpdatef.show(childFragmentManager, "ConfirmUpdatef")

            }
        }

    }


    private fun sendChkDataServerComplete() {

        val userdata = JsonObject()
        userdata.addProperty("memberid", this.userxId)
        userdata.addProperty("sday", this.sDay)
        userdata.addProperty("smonth", this.sMonth)
        userdata.addProperty("syear", this.sYear)
        userdata.addProperty("shour", this.sHour)
        userdata.addProperty("sminute", this.sMinute)
        userdata.addProperty("sprovince", this.sProvince)
        userdata.addProperty("sgender", this.sGenger)

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        // Changed to expect Serverx which contains both message and updated user object
        apiService.userUpdateData(userdata).enqueue(object : Callback<Serverx> {
            override fun onResponse(call: Call<Serverx>, response: Response<Serverx>) {
                if (response.isSuccessful && response.body() != null) {
                    val serverResponse = response.body()!!
                    val serverMsg = serverResponse.serverx

                    if (serverMsg != null && serverMsg.message == "success") {
                        val updatedUser = serverResponse.userx

                        if (updatedUser != null) {
                            // Update local session data using SharedPreferences
                            val context = requireContext()
                            val userdataSharedf = context.getSharedPreferences("userdata", android.content.Context.MODE_PRIVATE)
                            val editorUserdata = userdataSharedf.edit()
                            
                            // Preserve VIP code if it exists locally
                            val codevip = userdataSharedf.getString("codevip", null)
                            val userid = userdataSharedf.getString("userid", null)
                            
                            if (codevip != null && userid != null) {
                                if (updatedUser.userId == userid) {
                                    updatedUser.vipcode = codevip
                                }
                            }

                            // Save updated user json
                            editorUserdata.putString("json", com.google.gson.Gson().toJson(updatedUser))
                            editorUserdata.apply()

                            Toast.makeText(context, "ยินดีด้วยค่ะ คุณมีข้อมูลที่สมบูรณ์แล้ว", Toast.LENGTH_LONG).show()

                            activity!!.finish()

                            val intent = Intent(context, AppActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                        } else {
                            Toast.makeText(context, "Update success but no user data returned", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Update Failed: ${serverMsg?.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Serverx>, t: Throwable) {
                Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
            }
        })
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

    private fun completelyDataUser(): Boolean {

        sGenger = when (rad_gender_group.checkedRadioButtonId) {
            R.id.radio_man -> "ช"
            R.id.radio_woman -> "ญ"
            else -> ""
        }



        when {
            sDay == "วัน" -> {
                Toast.makeText(context, "โปรดเลือกวันเกิด!!", Toast.LENGTH_SHORT).show()
                spin_day.performClick()
                return false
            }

            sMonth == null || sMonth == "0" -> {
                Toast.makeText(context, "โปรดเลือกเดือนเกิด!!", Toast.LENGTH_SHORT).show()
                spin_month.performClick()
                return false
            }

            sYear == "ปี" -> {
                Toast.makeText(context, "โปรดเลือกปีเกิด!!", Toast.LENGTH_SHORT).show()
                spin_year.performClick()
                return false
            }

            sHour == "เวลา" -> {
                Toast.makeText(context, "โปรดเลือกเวลาเกิด!!", Toast.LENGTH_SHORT).show()
                spin_time_hour.performClick()
                return false
            }
            sMinute == "นาที" -> {
                Toast.makeText(context, "โปรดเลือกนาทีเกิด!!", Toast.LENGTH_SHORT).show()
                spin_time_minute.performClick()
                return false
            }

            sProvince == "จังหวัด" -> {
                Toast.makeText(context, "โปรดเลือกจังหวัดเกิด!!", Toast.LENGTH_SHORT).show()
                spin_province.performClick()
                return false
            }


            sGenger.isEmpty() -> {
                Toast.makeText(context, "โปรดเลือกเพศเกิด!!", Toast.LENGTH_SHORT).show()
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

    override fun userUpdate(message: String) {
        Log.d("MessageConfirm", message)
        if (message == "cf"){
        sendChkDataServerComplete()

        }
    }



}
