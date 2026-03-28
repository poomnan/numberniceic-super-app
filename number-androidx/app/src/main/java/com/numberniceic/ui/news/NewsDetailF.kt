package com.numberniceic.ui.news

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.numberniceic.R
import com.numberniceic.data.news.NewsHeadline
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewsDetailF : Fragment() {
    companion object {
        // Global Cache for News Details
        private val detailCache = mutableMapOf<String, String>()

        fun newInstance(newsId: String?, newsHeadline: NewsHeadline?): NewsDetailF {
            val args = Bundle()
            if (newsId != null) args.putString("newsId", newsId)
            if (newsHeadline != null) args.putParcelable("newsObject", newsHeadline)
            val fragment = NewsDetailF()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var webView: android.webkit.WebView
    private lateinit var progressLoading: android.widget.ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webview_news_detail)
        progressLoading = view.findViewById(R.id.progress_loading)

        val newsObject = arguments?.getParcelable<NewsHeadline>("newsObject")
        val newsId = arguments?.getString("newsId") ?: newsObject?.newsId
        
        setupWebView()

        if (newsId != null) {
            // 1. Check Memory Cache first
            val cachedContent = detailCache[newsId]
            if (!cachedContent.isNullOrEmpty()) {
                // Use cached content + header info from object (or empty if deep link)
                loadContent(cachedContent, newsObject?.newsHeader, newsObject?.newsImg, newsObject?.category)
                // Always refresh from API so stale/malformed image URLs are corrected.
                fetchAndLoadContent(newsId)
                return
            }

            // 2. Check if the passed object already has details (rare but possible)
            if (newsObject != null && !newsObject.newsDetail.isNullOrEmpty()) {
                detailCache[newsId] = newsObject.newsDetail
                loadContent(newsObject.newsDetail, newsObject.newsHeader, newsObject.newsImg, newsObject.category)
                // Refresh for latest image/url normalization from server.
                fetchAndLoadContent(newsId)
                return
            }

            // 3. Optimistic Loading: Show Header/Image immediately with a "Loading" placeholder
            if (newsObject != null) {
                // Show what we have instantly
                val loadingPlaceholder = """
                    <div style='text-align:center; padding-top:50px; padding-bottom:50px;'>
                        <h3 style='color:#999; font-weight:normal;'>กำลังโหลดเนื้อหา...</h3>
                    </div>
                """.trimIndent()
                loadContent(loadingPlaceholder, newsObject.newsHeader, newsObject.newsImg, newsObject.category)
                // Hide spinner because we are showing optimistic UI
                progressLoading.visibility = View.GONE 
            } else {
                 // Deep link case: We have nothing, so show spinner
                 progressLoading.visibility = View.VISIBLE
            }

            // 4. Fetch full content from API
            fetchAndLoadContent(newsId)

        } else {
            android.widget.Toast.makeText(context, "Error: No News Data", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupWebView() {
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.loadsImagesAutomatically = true
        webSettings.blockNetworkImage = false
        webSettings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }

    private fun loadContent(htmlContent: String, title: String?, imgUrl: String?, category: String?) {
        // Enforce readable content width & Add Beautiful Header
        
        val headerHtml = StringBuilder()
        val resolvedImageUrl = resolveNewsImageUrl(imgUrl)
        
        // 1. Header Image
        if (!resolvedImageUrl.isNullOrEmpty()) {
            headerHtml.append("""
                <div class="header-img-container">
                    <img src="$resolvedImageUrl" class="header-img" />
                </div>
            """.trimIndent())
        }

        // 2. Category & Title
        headerHtml.append("""
            <div class="header-text-container">
                ${if (!category.isNullOrEmpty()) "<span class='category-badge'>$category</span>" else ""}
                <h1 class="news-title">${title ?: ""}</h1>
            </div>
            <hr class="divider"/>
        """.trimIndent())

        val styledHtml = """
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #333; line-height: 1.6; }
                img { display: inline; height: auto; max-width: 100%; border-radius: 8px; }
                
                .header-img-container { width: 100%; height: 250px; overflow: hidden; position: relative; }
                .header-img { width: 100%; height: 100%; object-fit: cover; border-radius: 0 0 24px 24px; }
                
                .header-text-container { padding: 20px 20px 0 20px; }
                .category-badge { background-color: #FFD600; color: #000; padding: 4px 10px; border-radius: 12px; font-size: 13px; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px; }
                .news-title { margin-top: 12px; margin-bottom: 8px; font-size: 24px; font-weight: 700; color: #000; line-height: 1.3; }
                
                .divider { border: 0; height: 1px; background: #eee; margin: 20px; }
                
                /* Content Padding */
                .content-body { padding: 0 20px 40px 20px; font-size: 16px; }
                p { margin-bottom: 16px; }
            </style>
            </head>
            <body>
            
            $headerHtml
            
            <div class="content-body">
                $htmlContent
            </div>
            
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL("https://numberniceic.online/", styledHtml, "text/html", "utf-8", null)
        progressLoading.visibility = View.GONE
    }

    private fun resolveNewsImageUrl(rawUrl: String?): String? {
        var clean = rawUrl?.trim().orEmpty()
        if (clean.isEmpty()) return null

        // Handle values stored as <img ... src="..."> in legacy content.
        if (clean.contains("<img", ignoreCase = true)) {
            val srcMatch = Regex("""src\s*=\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE).find(clean)
            if (srcMatch != null && srcMatch.groupValues.size > 1) {
                clean = srcMatch.groupValues[1].trim()
            }
        }

        clean = clean.trim().trim('"', '\'').replace("\\/", "/")
        if (clean.isEmpty()) return null
        if (clean.equals("null", ignoreCase = true) || clean.equals("undefined", ignoreCase = true)) return null

        // If URL accidentally double-prefixed e.g. https://host/https://...
        val nestedHttps = clean.indexOf("https://", startIndex = 8)
        val nestedHttp = clean.indexOf("http://", startIndex = 7)
        if (nestedHttps > 0) clean = clean.substring(nestedHttps)
        else if (nestedHttp > 0) clean = clean.substring(nestedHttp)

        // Never rely on legacy host.
        clean = clean.replace("https://www.ananya.in.th", "https://numberniceic.online", ignoreCase = true)
            .replace("https://ananya.in.th", "https://numberniceic.online", ignoreCase = true)
            .replace("http://www.ananya.in.th", "https://numberniceic.online", ignoreCase = true)
            .replace("http://ananya.in.th", "https://numberniceic.online", ignoreCase = true)

        return when {
            clean.startsWith("//") -> "https:$clean"
            clean.startsWith("http://") -> clean
            clean.startsWith("https://") -> clean
            clean.startsWith("/") -> "https://numberniceic.online$clean"
            else -> "https://numberniceic.online/$clean"
        }
    }

    private fun fetchAndLoadContent(newsId: String) {
        // If we didn't show optimistic UI (progress is visible), keep it visible
        // If we showed given UI (progress GONE), keep it GONE
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Assuming API returns NewsHeadline object
                val response = apiService.getNewsHeadlineDetail(newsId).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val item = response.body()
                         val detail = item?.newsDetail
                         
                         // Pass all details to loadContent
                         if (!detail.isNullOrEmpty()) {
                             // Cache it!
                             detailCache[newsId] = detail
                             loadContent(detail, item?.newsHeader, item?.newsImg, item?.category)
                         } else {
                             android.widget.Toast.makeText(context, "No content found for this news.", android.widget.Toast.LENGTH_SHORT).show()
                             progressLoading.visibility = View.GONE
                         }
                    } else {
                        android.widget.Toast.makeText(context, "Error fetching news detail.", android.widget.Toast.LENGTH_SHORT).show()
                        progressLoading.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("NewsDetailF", "Error", e)
                    android.widget.Toast.makeText(context, "Connection Error", android.widget.Toast.LENGTH_SHORT).show()
                    progressLoading.visibility = View.GONE
                }
            }
        }
    }
}
