package com.numberniceic.ui.person


import android.content.Context
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment

import com.numberniceic.R
import com.numberniceic.ui.auth.UserRegisF

class ConfirmRegisF : DialogFragment() {

    interface OnUsercfListener{
        fun usercf(message:String)
    }

    private var onUserCfListener: OnUsercfListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_confirm_regis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog?.dismiss()
        }

        view.findViewById<View>(R.id.btn_cf).setOnClickListener {
            Log.d("USERCF", "Confirm button clicked. listener is null: ${onUserCfListener == null}")
            onUserCfListener?.usercf("cf")
            dialog?.dismiss()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            Log.d("USERCF", "onAttach: parentFragment is ${parentFragment?.javaClass?.simpleName}")
            onUserCfListener = parentFragment as? OnUsercfListener
            if (onUserCfListener == null) {
                // Try parent activity if fragment didn't work (though it should be the fragment)
                onUserCfListener = context as? OnUsercfListener
            }
            Log.d("USERCF", "onAttach: onUserCfListener set: ${onUserCfListener != null}")
        } catch (e: Exception) {
            Log.e("USERCF", "Error in onAttach: ${e.message}")
        }
    }


}
