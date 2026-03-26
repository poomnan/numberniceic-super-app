package com.numberniceic.ui.namenick


import android.content.Intent
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.*
import androidx.fragment.app.Fragment

import com.numberniceic.R
import com.numberniceic.adapters.DayAdapter
import com.numberniceic.data.nickname.Day


class NameNickIndexF : Fragment(), AdapterView.OnItemSelectedListener {
    private var dayPosition: Int? = 0

    private val dayList: ArrayList<Day> = arrayListOf()

    private lateinit var btn_nickname_cal_index: Button
    private lateinit var edt_nickname_index: EditText
    private lateinit var btn_nickname_clear_index: Button
    private lateinit var days_spinner_index_nickname: Spinner
    private lateinit var chip_nickname_add_line: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_name_nick_index, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_nickname_cal_index = view.findViewById(R.id.btn_nickname_cal_index)
        edt_nickname_index = view.findViewById(R.id.edt_nickname_index)
        btn_nickname_clear_index = view.findViewById(R.id.btn_nickname_clear_index)
        days_spinner_index_nickname = view.findViewById(R.id.days_spinner_index_nickname)
        chip_nickname_add_line = view.findViewById(R.id.chip_nickname_add_line)

        initInstances()


        btn_nickname_cal_index.setOnClickListener {
            if (this.dayPosition != 0 && !edt_nickname_index.text.isNullOrEmpty()) {
                val intent = Intent(context, NameNiceCalAct::class.java)
                intent.putExtra("dayPosition", this.dayPosition!!)
                intent.putExtra("nickname", edt_nickname_index.text.toString())
                startActivity(intent)
            } else {
                Toast.makeText(context, "กรุณาพิมพ์ชื่อ และ เลือกวันเกิด!!", Toast.LENGTH_SHORT).show()
            }
        }

        btn_nickname_clear_index.setOnClickListener {
            edt_nickname_index.text!!.clear()
            days_spinner_index_nickname.setSelection(0)
        }


    }


    private fun initInstances() {
        dayList.clear()
        dayList.add(Day("--วันเกิด--", "BirthDay", R.drawable.happy))
        dayList.add(Day("จันทร์", "monday", R.drawable.d_moon))
        dayList.add(Day("อังคาร", "tuesday", R.drawable.d_mars))
        dayList.add(Day("พุธ (กลางวัน)", "wednesday1", R.drawable.d_mercury))
        dayList.add(Day("พุธ (กลางคืน)", "wednesday2", R.drawable.d_mercury))
        dayList.add(Day("พฤหัสบดี", "thursday", R.drawable.d_jupiter))
        dayList.add(Day("ศุกร์", "friday", R.drawable.d_asteroid))
        dayList.add(Day("เสาร์", "saturday", R.drawable.d_saturn))
        dayList.add(Day("อาทิตย์", "sunday", R.drawable.d_sun))


        val spinnterAdapter = DayAdapter(context!!, dayList)

        days_spinner_index_nickname.adapter = spinnterAdapter
        days_spinner_index_nickname.onItemSelectedListener = this

        chip_nickname_add_line.setOnClickListener {
            val userId = "@n956364599"
            val sentText = "line://ti/p/~$userId"
            val intent: Intent?
            intent = Intent.parseUri(sentText, Intent.URI_INTENT_SCHEME)
            startActivity(intent)
        }


    }


    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        this.dayPosition = position


    }


}
