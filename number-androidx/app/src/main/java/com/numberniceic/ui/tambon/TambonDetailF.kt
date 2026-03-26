package com.numberniceic.ui.tambon


import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment

import com.numberniceic.R
import com.numberniceic.https.NetworkConfig


class TambonDetailF : Fragment() {

    companion object {
        fun newInstance(tambonId:String, type:String): TambonDetailF {
            val args = Bundle()
            args.putString("tambonId", tambonId)
            args.putString("type", type)
            val fragment = TambonDetailF()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tambon_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webview_tambon = view.findViewById<WebView>(R.id.webview_tambon)
        val type = arguments!!.getString("type")

        if (type == "change_num"){
            val baseUrl = "${NetworkConfig.BASE_URL}/changenum/view?id="
            val id = arguments!!.getString("tambonId")
            webview_tambon.webViewClient = WebViewClient()
            webview_tambon.loadUrl("$baseUrl$id")

            Log.d("BASEURLCHANGE", "$baseUrl$id")
        }

        if (type == "tambon"){
            val baseUrl = "${NetworkConfig.BASE_URL}/merit/view?id="
            val tambonId = arguments!!.getString("tambonId")

            webview_tambon.webViewClient = WebViewClient()
            webview_tambon.loadUrl("$baseUrl$tambonId")
        }



    }


}
