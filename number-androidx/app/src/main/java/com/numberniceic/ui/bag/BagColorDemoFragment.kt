package com.numberniceic.ui.bag

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.numberniceic.R

/**
 * Fragment สำหรับแสดง Demo การใช้งาน BagColorView
 */
class BagColorDemoFragment : Fragment() {

    private lateinit var bagColorView: BagColorView
    private lateinit var txtAgeRange: TextView
    private lateinit var txtLuckyNumbers: TextView
    private lateinit var colorIndicator1: View
    private lateinit var colorIndicator2: View
    private lateinit var colorIndicator3: View
    private lateinit var colorIndicator4: View
    private lateinit var txtColor1: TextView
    private lateinit var txtColor2: TextView
    private lateinit var txtColor3: TextView
    private lateinit var txtColor4: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bag_color_demo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        bagColorView = view.findViewById(R.id.bag_color_view)
        txtAgeRange = view.findViewById(R.id.txt_age_range)
        txtLuckyNumbers = view.findViewById(R.id.txt_lucky_numbers)
        colorIndicator1 = view.findViewById(R.id.color_indicator_1)
        colorIndicator2 = view.findViewById(R.id.color_indicator_2)
        colorIndicator3 = view.findViewById(R.id.color_indicator_3)
        colorIndicator4 = view.findViewById(R.id.color_indicator_4)
        txtColor1 = view.findViewById(R.id.txt_color_1)
        txtColor2 = view.findViewById(R.id.txt_color_2)
        txtColor3 = view.findViewById(R.id.txt_color_3)
        txtColor4 = view.findViewById(R.id.txt_color_4)

        // Test buttons
        view.findViewById<Button>(R.id.btn_test_colors_1).setOnClickListener {
            setColorSet1()
        }

        view.findViewById<Button>(R.id.btn_test_colors_2).setOnClickListener {
            setColorSet2()
        }

        view.findViewById<Button>(R.id.btn_test_colors_3).setOnClickListener {
            setColorSet3()
        }

        // Set default colors
        setColorSet1()
    }

    /**
     * ชุดสีที่ 1: เขียว-น้ำเงิน-ส้ม-น้ำตาล
     */
    private fun setColorSet1() {
        val color1 = "#2E7D32" // เขียว
        val color2 = "#1565C0" // น้ำเงิน
        val color3 = "#EF6C00" // ส้ม
        val color4 = "#5D4037" // น้ำตาล

        bagColorView.setBagColors(color1, color2, color3, color4)

        colorIndicator1.setBackgroundColor(Color.parseColor(color1))
        colorIndicator2.setBackgroundColor(Color.parseColor(color2))
        colorIndicator3.setBackgroundColor(Color.parseColor(color3))
        colorIndicator4.setBackgroundColor(Color.parseColor(color4))

        txtColor1.text = "เขียว - โชคลาภ การเงิน"
        txtColor2.text = "น้ำเงิน - สุขภาพ ความสงบ"
        txtColor3.text = "ส้ม - ความรัก ความสัมพันธ์"
        txtColor4.text = "น้ำตาล - เสถียรภาพ ความมั่นคง"

        txtAgeRange.text = "อายุ 43 ปี ยาง 44 ปี"
        txtLuckyNumbers.text = "41 09 42 59 97 23"
    }

    /**
     * ชุดสีที่ 2: แดง-ทอง-ม่วง-ชมพู
     */
    private fun setColorSet2() {
        val color1 = "#C62828" // แดง
        val color2 = "#F9A825" // ทอง
        val color3 = "#6A1B9A" // ม่วง
        val color4 = "#EC407A" // ชมพู

        bagColorView.setBagColors(color1, color2, color3, color4)

        colorIndicator1.setBackgroundColor(Color.parseColor(color1))
        colorIndicator2.setBackgroundColor(Color.parseColor(color2))
        colorIndicator3.setBackgroundColor(Color.parseColor(color3))
        colorIndicator4.setBackgroundColor(Color.parseColor(color4))

        txtColor1.text = "แดง - พลังงาน ความกล้าหาญ"
        txtColor2.text = "ทอง - ความมั่งคั่ง โชคลาภ"
        txtColor3.text = "ม่วง - ภูมิปัญญา ความสง่างาม"
        txtColor4.text = "ชมพู - ความรัก ความอบอุ่น"

        txtAgeRange.text = "อายุ 35 ปี ยาง 36 ปี"
        txtLuckyNumbers.text = "12 24 36 48 60 72"
    }

    /**
     * ชุดสีที่ 3: ดำ-ขาว-เทา-เงิน
     */
    private fun setColorSet3() {
        val color1 = "#212121" // ดำ
        val color2 = "#FAFAFA" // ขาว
        val color3 = "#616161" // เทา
        val color4 = "#9E9E9E" // เงิน

        bagColorView.setBagColors(color1, color2, color3, color4)

        colorIndicator1.setBackgroundColor(Color.parseColor(color1))
        colorIndicator2.setBackgroundColor(Color.parseColor(color2))
        colorIndicator3.setBackgroundColor(Color.parseColor(color3))
        colorIndicator4.setBackgroundColor(Color.parseColor(color4))

        txtColor1.text = "ดำ - ความลึกลับ พลังงาน"
        txtColor2.text = "ขาว - ความบริสุทธิ์ ความสะอาด"
        txtColor3.text = "เทา - ความสมดุล ความเป็นกลาง"
        txtColor4.text = "เงิน - ความทันสมัย ความหรูหรา"

        txtAgeRange.text = "อายุ 50 ปี ยาง 51 ปี"
        txtLuckyNumbers.text = "05 10 15 20 25 30"
    }

    companion object {
        fun newInstance(): BagColorDemoFragment {
            return BagColorDemoFragment()
        }

        /**
         * สร้าง instance พร้อมข้อมูลสีจาก API
         */
        fun newInstance(
            age: Int,
            color1: String,
            color2: String,
            color3: String,
            color4: String,
            luckyNumbers: String
        ): BagColorDemoFragment {
            val fragment = BagColorDemoFragment()
            val args = Bundle().apply {
                putInt("age", age)
                putString("color1", color1)
                putString("color2", color2)
                putString("color3", color3)
                putString("color4", color4)
                putString("luckyNumbers", luckyNumbers)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
