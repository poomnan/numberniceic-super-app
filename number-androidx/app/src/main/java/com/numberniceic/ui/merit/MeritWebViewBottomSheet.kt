package com.numberniceic.ui.merit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.numberniceic.R

class MeritWebViewBottomSheet : BottomSheetDialogFragment() {

    private var url: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        url = arguments?.getString("url")
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_merit_webview_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val webView = view.findViewById<WebView>(R.id.webview_merit)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url == "app://close" || url.endsWith("app://close")) {
                    dismiss()
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
            // For older devices
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, url: String?): Boolean {
                 if (url == "app://close") {
                    dismiss()
                    return true
                }
                return super.shouldOverrideUrlLoading(view, url)
            }
        }
        
        if (!url.isNullOrEmpty()) {
            webView.loadUrl(url!!)
        }
        
        // Removed close button listener as it's gone from layout
    }
    
    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialogTheme
    }
    
    override fun onStart() {
        super.onStart()
        // Expand full height
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val behavior = BottomSheetBehavior.from(bottomSheet)
            val displayMetrics = resources.displayMetrics
            val totalHeight = displayMetrics.heightPixels
            val targetHeight = (totalHeight * 0.85).toInt()

            bottomSheet.layoutParams.height = targetHeight
            behavior.isFitToContents = false
            behavior.expandedOffset = totalHeight - targetHeight
            behavior.peekHeight = targetHeight
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            bottomSheet.requestLayout()
        }
    }

    companion object {
        fun newInstance(url: String): MeritWebViewBottomSheet {
            val fragment = MeritWebViewBottomSheet()
            val args = Bundle()
            args.putString("url", url)
            fragment.arguments = args
            return fragment
        }
    }
}
