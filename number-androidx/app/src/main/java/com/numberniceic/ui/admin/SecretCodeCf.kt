package com.numberniceic.ui.admin


import android.content.Context
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment

import com.numberniceic.R
import com.numberniceic.ui.auth.UserRegisF
import com.numberniceic.ui.namesur.NamesurAdF
import android.widget.TextView
import android.widget.Button
import java.lang.ClassCastException


class SecretCodeCf : DialogFragment() {

    companion object {
        fun newInstance(codeType:String, codeName:String): SecretCodeCf {
            val args = Bundle()
            args.putString("codeType", codeType)
            args.putString("codeName", codeName)
            val fragment = SecretCodeCf()
            fragment.arguments = args
            return fragment
        }
    }



    private lateinit var onConfListener: OnConfListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_secret_code_cf, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txt_codetype = view.findViewById<TextView>(R.id.txt_codetype)
        val txt_codename = view.findViewById<TextView>(R.id.txt_codename)
        val btn_secret_code_cf = view.findViewById<Button>(R.id.btn_secret_code_cf)
        val btn_secret_code_cencel = view.findViewById<Button>(R.id.btn_secret_code_cencel)

        txt_codetype.text = arguments!!.getString("codeType")
        txt_codename.text = arguments!!.getString("codeName")

        if (btn_secret_code_cf != null){
            btn_secret_code_cf.setOnClickListener {
            onConfListener.onConf(txt_codename.text.toString())
            dismiss()

            }
        }

        if (btn_secret_code_cencel != null){
            btn_secret_code_cencel.setOnClickListener {
            dismiss()

            }
        }

    }




    interface OnConfListener{
        fun onConf(cf: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onConfListener = parentFragment as SecretCodeF
        }catch (c: ClassCastException){
            Log.d("CLASSCAST", c.message!!)
        }
    }

}
