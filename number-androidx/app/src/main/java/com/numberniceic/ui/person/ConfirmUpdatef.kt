package com.numberniceic.ui.person


import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment


import com.numberniceic.R
import com.numberniceic.ui.apersonnews.BirthDayF


class ConfirmUpdatef : DialogFragment() {


    interface OnUserUpdateListener {
        fun userUpdate(message: String)
    }

    private lateinit var onUserUpdateListener: OnUserUpdateListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_confirm_updatef, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onUserUpdateListener = parentFragment as BirthDayF

        view.findViewById<View>(R.id.btn_update_cf).setOnClickListener {
            onUserUpdateListener.userUpdate("cf")
            dismiss()
        }

        view.findViewById<View>(R.id.btn_update_cancel).setOnClickListener {
            dismiss()
        }


    }


}
