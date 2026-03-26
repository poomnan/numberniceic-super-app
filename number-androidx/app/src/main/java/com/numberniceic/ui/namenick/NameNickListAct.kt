package com.numberniceic.ui.namenick

import android.os.Bundle
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.MenuItem
import com.numberniceic.R
import com.numberniceic.adapters.DayAdapter
import com.numberniceic.data.nickname.Day

class NameNickListAct : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var selectedBundle: SelectBuddle

    private var txtBtn: String = ""
    private var charx = ""
    private var day = ""
    private var lastid = 0
    private val dayList: ArrayList<Day> = arrayListOf()

    private lateinit var days_spinner_vip: Spinner
    private lateinit var btn_prefix: Button
    private lateinit var toolbar_namenick_list: Toolbar
    private lateinit var edt_charx: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_name_nick_list)

        days_spinner_vip = findViewById(R.id.days_spinner_vip)
        btn_prefix = findViewById(R.id.btn_prefix)
        toolbar_namenick_list = findViewById(R.id.toolbar_namenick_list)
        edt_charx = findViewById(R.id.edt_charx)

        setSupportActionBar(toolbar_namenick_list)
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


    }

    private fun innitInstance() {
        dayList.clear()
        dayList.add(Day("--วันเกิด--", "BirthDay", R.drawable.ic_clover))
        dayList.add(Day("อาทิตย์", "1", R.drawable.d_sun))
        dayList.add(Day("จันทร์", "2", R.drawable.d_moon))
        dayList.add(Day("อังคาร", "3", R.drawable.d_mars))
        dayList.add(Day("พุธ (กลางวัน)", "4", R.drawable.d_mercury))
        dayList.add(Day("พุธ (กลางคืน)", "8", R.drawable.d_mercury))
        dayList.add(Day("พฤหัสบดี", "5", R.drawable.d_jupiter))
        dayList.add(Day("ศุกร์", "6", R.drawable.d_asteroid))
        dayList.add(Day("เสาร์", "7", R.drawable.d_saturn))


        val spinnterAdapter = DayAdapter(applicationContext!!, dayList)

        days_spinner_vip.adapter = spinnterAdapter
        days_spinner_vip.onItemSelectedListener = this
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)


        this.txtBtn = "ทั้งหมด"
        btn_prefix.text = "ทั้งหมด"
        this.charx = ""

        btn_prefix.setOnClickListener {

            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialogbox_prefix, null)
            val rad_all = dialogView.findViewById<RadioButton>(R.id.rad_all)
            val rad_girl = dialogView.findViewById<RadioButton>(R.id.rad_girl)
            val rad_man = dialogView.findViewById<RadioButton>(R.id.rad_man)
            val rad_mangirl = dialogView.findViewById<RadioButton>(R.id.rad_mangirl)

            val mBuilder = AlertDialog.Builder(this)
            mBuilder.setView(dialogView)
            mBuilder.setTitle("เลือกดูหญิงหรือชาย")

            val dialogAlert = mBuilder.show()
            if (btn_prefix.text.toString() == "ทั้งหมด") rad_all.setChecked(true)
            if (btn_prefix.text.toString() == "หญิง") rad_girl.setChecked(true)
            if (btn_prefix.text.toString() == "ชาย") rad_man.setChecked(true)
            if (btn_prefix.text.toString() == "ชายหญิง") rad_mangirl.setChecked(true)

            rad_all.setOnClickListener {
                Toast.makeText(it.context, "แสดงเพศทั้งหมด", Toast.LENGTH_SHORT).show()
                btn_prefix.text = "ทั้งหมด"
                this.txtBtn = "ทั้งหมด"
                dialogAlert.dismiss()
                selectedBundle.onBundleSeclect(this.day, this.charx, this.txtBtn)


            }


            rad_girl.setOnClickListener {
                Toast.makeText(it.context, "แสดงเฉพาะหญิง", Toast.LENGTH_SHORT).show()
                btn_prefix.text = "หญิง"
                this.txtBtn = "หญิง"
                dialogAlert.dismiss()
                selectedBundle.onBundleSeclect(this.day, this.charx, this.txtBtn)


            }

            rad_man.setOnClickListener {
                Toast.makeText(it.context, "แสดงเฉพาะชาย", Toast.LENGTH_SHORT).show()
                btn_prefix.text = "ชาย"
                this.txtBtn = "ชาย"
                dialogAlert.dismiss()
                selectedBundle.onBundleSeclect(this.day, this.charx, this.txtBtn)
            }

            rad_mangirl.setOnClickListener {
                Toast.makeText(it.context, "แสดงทั้งหมด", Toast.LENGTH_SHORT).show()
                btn_prefix.text = "ชายหญิง"
                this.txtBtn = "ชายหญิง"
                dialogAlert.dismiss()
                selectedBundle.onBundleSeclect(this.day, this.charx, this.txtBtn)
            }


        }


        val manager = supportFragmentManager
        val ft = manager.beginTransaction()

        val callFragment = intent.getStringExtra("call_fragmentx")

        toolbar_namenick_list.title = if (callFragment == "realname") "VIP เลือกชื่อจริงตาม: " else "VIP เลือกชื่อเล่น : "

        when (callFragment) {
            "realname" -> ft.replace(R.id.namenick_container_vip, NameNickVipF.newInstance(callFragment), "NameNickVipF")
            "nickname" -> ft.replace(R.id.namenick_container_vip, NameNickVipF.newInstance(callFragment), "NameNickVipF")
        }

        ft.commit()

        manager.executePendingTransactions()

        innitInstance()



        edt_charx.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null) {

                    if (s.toString() != "") {
                        setCharx(s.toString())
                    }else{
                        setCharx("x")
                    }


                }else{
                    setCharx("x")
                }


            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {


            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })


    }


    private fun setCharx(s: String) {
        this.charx = s
        selectedBundle.onBundleSeclect(this.day, this.charx, this.txtBtn)
    }

    interface SelectBuddle {
        fun onBundleSeclect(day: String, charx: String, prefix: String)
    }

    fun setOnBundleSelected(selectBuddle: SelectBuddle) {
        this.selectedBundle = selectBuddle
    }


    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        this.day = when (position) {
            1 -> dayList[1].nameEng
            2 -> dayList[2].nameEng
            3 -> dayList[3].nameEng
            4 -> dayList[4].nameEng
            5 -> dayList[5].nameEng
            6 -> dayList[6].nameEng
            7 -> dayList[7].nameEng
            8 -> dayList[8].nameEng
            else -> "x"
        }

        if (this.charx == "") charx = "x"
        if (this.txtBtn == "") charx = "x"

        selectedBundle.onBundleSeclect(this.day, this.charx, this.txtBtn)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }



}


