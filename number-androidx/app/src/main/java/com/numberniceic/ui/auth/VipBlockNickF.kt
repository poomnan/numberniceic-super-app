package com.numberniceic.ui.auth


import android.content.Context
import android.content.Intent
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

import com.numberniceic.R

class VipBlockNickF : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vip_block_nick, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btn_nickname_block_regis = view.findViewById<Button>(R.id.btn_nickname_block_regis)
        btn_nickname_block_regis.setOnClickListener {
            val intent = Intent(view.context, UserLoginAct::class.java)
            startActivity(intent)
        }

    }


}
