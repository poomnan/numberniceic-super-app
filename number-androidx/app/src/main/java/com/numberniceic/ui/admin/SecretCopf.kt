package com.numberniceic.ui.admin


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment

import android.widget.Button
import android.widget.TextView
import com.numberniceic.R
import com.numberniceic.ui.AppActivity
import com.numberniceic.ui.auth.UserLoginAct
import com.numberniceic.ui.auth.UserLogoutAct


class SecretCopf : DialogFragment() {

    companion object {
        fun newInstance(codeName: String): SecretCopf {
            val args = Bundle()
            args.putString("codename", codeName)
            val fragment = SecretCopf()
            fragment.arguments = args
            return fragment
        }
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_secret_copf, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txt_codename_copy = view.findViewById<TextView>(R.id.txt_codename_copy)
        val btn_secretcode_cancel = view.findViewById<Button>(R.id.btn_secretcode_cancel)
        val btn_secretcode_copy = view.findViewById<Button>(R.id.btn_secretcode_copy)

        txt_codename_copy.text = arguments!!.getString("codename")

        btn_secretcode_cancel.setOnClickListener {
            dismiss()
            val intent = Intent(context, UserLogoutAct::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)

        }

        btn_secretcode_copy.setOnClickListener {

            val clipboard = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipdata = ClipData.newPlainText("codename", txt_codename_copy.text)
            clipboard.setPrimaryClip(clipdata)
            Toast.makeText(context, "คัดลอก CODE เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
            dismiss()
            val intent = Intent(context, UserLogoutAct::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)


        }

    }




}
