package com.numberniceic.ui.home

import android.content.Context
import com.numberniceic.R


import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.numberniceic.data.news.News24
import com.numberniceic.databinding.FragmentHomeArticleBinding
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.numberniceic.ui.news.NewsAct
import com.numberniceic.ui.news.NewsAllAct
import com.numberniceic.ui.renkyam.RengYam
import com.numberniceic.ui.tambon.TambonDiaf
import com.numberniceic.utils.ImageUrlResolver
import com.numberniceic.utils.PersonContextManager
import com.numberniceic.utils.UserContextManager


import org.joda.time.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HomeArticleF : Fragment() {
    private lateinit var binding: FragmentHomeArticleBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home_article, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup Pull-to-Refresh
        binding.swipeRefreshHomeArticle.setOnRefreshListener {
            refreshArticles()
        }
        binding.swipeRefreshHomeArticle.setColorSchemeResources(
            android.R.color.holo_green_dark,
            android.R.color.holo_orange_dark,
            android.R.color.holo_blue_dark
        )
        
        this.initNewsTopic24()
        this.initBtnReadmoreNews()
    }












    private fun initBtnReadmoreNews() {
        binding.btnSeeAll.setOnClickListener {
            val intent = Intent(it.context, NewsAllAct::class.java)
            intent.putExtra("newsIdType", "1") // Default to type 1 or some collection
            startActivity(intent)
        }
    }

    private fun initNewsTopic24() {
        val ctx = context ?: return

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getNewsTopic24().enqueue(object : Callback<News24> {
            override fun onResponse(call: Call<News24>, response: Response<News24>) {
                if (response.isSuccessful && response.body() != null) {
                    val news24 = response.body()!!
                    Log.d("HomeArticleF", "Network Success: Received ${news24.newsHot?.size} hot news")
                    displayNews24(news24)
                    com.numberniceic.utils.PersonNewsCacheManager.saveNews24(ctx, news24)
                } else {
                    val cached = com.numberniceic.utils.PersonNewsCacheManager.loadNews24(ctx)
                    if (cached != null) {
                        Log.d("HomeArticleF", "Fallback cache due to unsuccessful response")
                        displayNews24(cached)
                    }
                }
            }

            override fun onFailure(call: Call<News24>, t: Throwable) {
                Log.e("HomeArticleF", "Network Error: ${t.message}")
                val cached = com.numberniceic.utils.PersonNewsCacheManager.loadNews24(ctx)
                if (cached != null) {
                    Log.d("HomeArticleF", "Fallback cache due to network error")
                    displayNews24(cached)
                }
            }
        })
    }
    
    private fun displayNews24(news24: News24) {
        if (news24.newsHot == null || !isAdded) return
        val articles = news24.newsHot!!

        // Featured Article
        if (articles.isNotEmpty()) {
            val n = articles[0]
            loadNewsImage(n.newsImg, binding.imgFeatured, 800, 600)

            binding.txtFeaturedTitle.text = n.newsHeader
            binding.txtFeaturedDesc.text = n.newsDesc
            binding.txtFeaturedCategory.text = n.category
            binding.layoutFeaturedContainer.setOnClickListener { callActNews(n.newsId.toString()) }
        }

        // Grid Item 1
        if (articles.size > 1) {
            val n = articles[1]
            loadNewsImage(n.newsImg, binding.imgGrid1, 400, 400)
            binding.txtGrid1.text = n.newsTitleShort ?: n.newsHeader
            binding.layoutGrid1.setOnClickListener { callActNews(n.newsId.toString()) }
        }

        // Grid Item 2
        if (articles.size > 2) {
            val n = articles[2]
            loadNewsImage(n.newsImg, binding.imgGrid2, 400, 400)
            binding.txtGrid2.text = n.newsTitleShort ?: n.newsHeader
            binding.layoutGrid2.setOnClickListener { callActNews(n.newsId.toString()) }
        }

        // Grid Item 3
        if (articles.size > 3) {
            val n = articles[3]
            loadNewsImage(n.newsImg, binding.imgGrid3, 400, 400)
            binding.txtGrid3.text = n.newsTitleShort ?: n.newsHeader
            binding.layoutGrid3.setOnClickListener { callActNews(n.newsId.toString()) }
        }

        // List Items in Purple Block
        if (articles.size > 4) {
            val n = articles[4]
            binding.txtList1.text = n.newsTitleShort ?: n.newsHeader
            binding.layoutList1.setOnClickListener { callActNews(n.newsId.toString()) }
        }
        if (articles.size > 5) {
            val n = articles[5]
            binding.txtList2.text = n.newsTitleShort ?: n.newsHeader
            binding.layoutList2.setOnClickListener { callActNews(n.newsId.toString()) }
        }
        if (articles.size > 6) {
            val n = articles[6]
            binding.txtList3.text = n.newsTitleShort ?: n.newsHeader
            binding.layoutList3.setOnClickListener { callActNews(n.newsId.toString()) }
        }
    }

    private fun loadNewsImage(rawUrl: String?, imageView: ImageView, width: Int, height: Int) {
        val candidates = ImageUrlResolver.resolveCandidates(rawUrl)
        val glide = Glide.with(this@HomeArticleF)
        var chain: com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable>? = null
        for (candidate in candidates.asReversed()) {
            val request = glide.load(candidate)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .override(width, height)
            chain = if (chain == null) request.error(android.R.drawable.stat_notify_error) else request.error(chain)
        }
        if (chain != null) {
            chain.into(imageView)
        } else {
            imageView.setImageResource(android.R.drawable.stat_notify_error)
        }
    }


    private fun callActNews(s: String) {

        val intent = Intent(context, NewsAct::class.java)
        intent.putExtra("newsId", s)
        startActivity(intent)
    }

    //set icon wanpra
    
    private fun refreshArticles() {
        val ctx = context ?: return
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getNewsTopic24().enqueue(object : Callback<News24> {
            override fun onResponse(call: Call<News24>, response: Response<News24>) {
                binding.swipeRefreshHomeArticle.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    val news24 = response.body()!!
                    displayNews24(news24)
                    com.numberniceic.utils.PersonNewsCacheManager.saveNews24(ctx, news24)
                    android.widget.Toast.makeText(context, "รีเฟรชบทความสำเร็จ", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<News24>, t: Throwable) {
                binding.swipeRefreshHomeArticle.isRefreshing = false
                android.widget.Toast.makeText(context, "ไม่สามารถรีเฟรชบทความได้", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }
}
