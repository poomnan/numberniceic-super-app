package com.numberniceic.ui.home


import android.content.Intent
import android.graphics.Color
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

import android.widget.Button
import android.widget.EditText
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.numberniceic.R
import com.numberniceic.utils.HomeContextManager


class HomeIndexF : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home_index, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btn_homeindex_cal = view.findViewById<Button>(R.id.btn_homeindex_cal)
        val edt_homeindex_num = view.findViewById<EditText>(R.id.edt_homeindex_num)
        val coordinator_home_index = view.findViewById<CoordinatorLayout>(R.id.coordinator_home_index)

        btn_homeindex_cal.setOnClickListener {
            if (HomeContextManager.checkString(edt_homeindex_num.text.toString())) {
                val snack = Snackbar.make(coordinator_home_index, "ข้อมูลไม่ถูกต้อง!!", Snackbar.LENGTH_LONG)
                val tv = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                tv.setTextColor(Color.YELLOW)
                snack.show()
            } else {
                val intent = Intent(context, HomeCalAct::class.java)
                intent.putExtra("HOMENUMBER", edt_homeindex_num.text.toString())
                startActivity(intent)
            }
        }




    }


}
