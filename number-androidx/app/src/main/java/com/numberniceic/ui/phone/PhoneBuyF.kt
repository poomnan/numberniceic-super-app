package com.numberniceic.ui.phone


import android.content.Intent
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao
import com.numberniceic.ui.AppActivity
import com.numberniceic.ui.tabian.TabianMiraActivity


class PhoneBuyF : Fragment(), View.OnClickListener {

    private lateinit var chip_0951424515: View
    private lateinit var chip_0956515454: View
    private lateinit var chip_0959546364: View
    private lateinit var chip_show_phone_all_number: View


    companion object {
        fun newInstance(): PhoneBuyF {
            //val args = Bundle()

            val fragment = PhoneBuyF()

            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

       val view = inflater.inflate(R.layout.fragment_phone_buy, container, false)
        return view
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chip_0951424515 = view.findViewById(R.id.chip_0951424515)
        chip_0956515454 = view.findViewById(R.id.chip_0956515454)
        chip_0959546364 = view.findViewById(R.id.chip_0959546364)
        chip_show_phone_all_number = view.findViewById(R.id.chip_show_phone_all_number)

        chip_0951424515.setOnClickListener(this)
        chip_0956515454.setOnClickListener(this)
        chip_0959546364.setOnClickListener(this)
        chip_show_phone_all_number.setOnClickListener(this)

    }


    override fun onClick(v: View?) {
        val intent = Intent(context, AppActivity::class.java)
        val intentPhneBuyAll = Intent(context, PhoneBuyAllAct::class.java)
        if (v == chip_0951424515) {
            intent.putExtra("PHONENUMBER", "0951424515")
            startActivity(intent)

        }
        if (v == chip_0956515454) {
            intent.putExtra("PHONENUMBER", "0956515454")
            startActivity(intent)

        }
        if (v == chip_0959546364) {
            intent.putExtra("PHONENUMBER", "0959546364")
            startActivity(intent)

        }

        if (v == chip_show_phone_all_number) {
            startActivity(intentPhneBuyAll)

        }


    }


}
