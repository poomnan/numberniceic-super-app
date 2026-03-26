package com.numberniceic.ui.merit

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.numberniceic.R

class MeritWebViewAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merit_webview)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val title = intent.getStringExtra("title") ?: "รายละเอียด"
        supportActionBar?.title = title

        val webView = findViewById<WebView>(R.id.webview_merit)
        val url = intent.getStringExtra("url") ?: com.numberniceic.https.NetworkConfig.BASE_URL

        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
