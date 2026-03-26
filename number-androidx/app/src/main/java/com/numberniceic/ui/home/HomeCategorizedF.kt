package com.numberniceic.ui.home

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.numberniceic.databinding.FragmentHomeCategorizedBinding
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.news.NewsAct
import com.numberniceic.ui.news.NewsAllAct
import com.numberniceic.utils.ImageUrlResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.numberniceic.R
import okhttp3.Request

class HomeCategorizedF : Fragment() {

    private lateinit var binding: FragmentHomeCategorizedBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeCategorizedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initNewsData()
        initClickListeners()
    }

    private fun initClickListeners() {
        // Read More Buttons
        binding.layoutReviews.btnNewsFeedback.setOnClickListener { startNewsAll("1") }
        binding.layoutPhone.btnNewsPhonenum.setOnClickListener { startNewsAll("2") }
        binding.layoutNamesur.btnNewsNamesur.setOnClickListener { startNewsAll("3") }
        binding.layoutTabian.btnNewsTabian.setOnClickListener { startNewsAll("4") }
        binding.layoutHomenum.btnNewsHomenum.setOnClickListener { startNewsAll("5") }
        binding.layoutConcept.btnNewsNumberconcept.setOnClickListener { startNewsAll("6") }
    }

    private fun startNewsAll(type: String) {
        val intent = Intent(context, NewsAllAct::class.java)
        intent.putExtra("newsIdType", type)
        startActivity(intent)
    }

    private fun callActNews(news: com.numberniceic.data.news.NewsHeadline) {
        val intent = Intent(context, NewsAct::class.java)
        // Pass ID for legacy/fallback
        intent.putExtra("newsId", news.newsId.toString())
        // Pass Full Object for instant load
        intent.putExtra("newsObject", news)
        startActivity(intent)
    }

    private fun initNewsData() {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getNewsTopic24().execute()
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        com.numberniceic.utils.PersonNewsCacheManager.saveNews24(requireContext(), data)
                        launch(Dispatchers.Main) {
                            updateUI(data)
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeCategorizedF", "Error loading news", e)
            }
            try {
                val cachedData = com.numberniceic.utils.PersonNewsCacheManager.loadNews24(requireContext())
                if (cachedData != null) {
                    launch(Dispatchers.Main) {
                        updateUI(cachedData)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeCategorizedF", "Error loading cache fallback", e)
            }
        }
    }

    private fun updateUI(news24: com.numberniceic.data.news.News24) {
        // 0: Hot News
        news24.newsHot?.let { list ->
            binding.headerHotNews.visibility = View.VISIBLE
            if (list.isNotEmpty()) {
                 val cat = list[0].category
                 if (!cat.isNullOrEmpty()) {
                     binding.headerHotNews.findViewById<android.widget.TextView>(com.numberniceic.R.id.tv_header_hot_news)?.text = cat
                 }
            }
            if (list.size > 0) bindItem(list[0], binding.layoutHotNews.imgNews1, binding.layoutHotNews.txtHeaderNews1, binding.layoutHotNews.txtDescNews1, binding.layoutHotNews.linearNewsItem1)
            if (list.size > 1) bindItem(list[1], binding.layoutHotNews.imgNews01, binding.layoutHotNews.txtHeaderNews01, binding.layoutHotNews.txtDescNews01, binding.layoutHotNews.linearNewsItem2)
            if (list.size > 2) bindItem(list[2], binding.layoutHotNews.imgNews02, binding.layoutHotNews.txtHeaderNews02, binding.layoutHotNews.txtDescNews02, binding.layoutHotNews.linearNewsItem3)
            if (list.size > 3) bindItem(list[3], binding.layoutHotNews.imgNews03, binding.layoutHotNews.txtHeaderNews03, binding.layoutHotNews.txtDescNews03, binding.layoutHotNews.linearNewsItem4)
            if (list.size > 4) bindItem(list[4], binding.layoutHotNews.imgNews04, binding.layoutHotNews.txtHeaderNews04, binding.layoutHotNews.txtDescNews04, binding.layoutHotNews.linearNewsItem5)
        }

        // 1: Reviews
        news24.newsFeedback?.let { list ->
            binding.headerReviews.visibility = View.VISIBLE
            if (list.isNotEmpty()) {
                 val cat = list[0].category
                 if (!cat.isNullOrEmpty()) {
                     binding.headerReviews.findViewById<android.widget.TextView>(com.numberniceic.R.id.tv_header_reviews)?.text = cat
                 }
            }
            if (list.size > 0) bindItem(list[0], binding.layoutReviews.imgNewsFeedback01, binding.layoutReviews.txtHeaderNewsFeedback01, binding.layoutReviews.txtDescNewsFeedback01, binding.layoutReviews.linearNewsFeedbackItem1)
            if (list.size > 1) bindItem(list[1], binding.layoutReviews.imgNewsFeedback02, binding.layoutReviews.txtHeaderNewsFeedback02, binding.layoutReviews.txtDescNewsFeedback02, binding.layoutReviews.linearNewsFeedbackItem2)
            if (list.size > 2) bindItem(list[2], binding.layoutReviews.imgNewsFeedback03, binding.layoutReviews.txtHeaderNewsFeedback03, binding.layoutReviews.txtDescNewsFeedback03, binding.layoutReviews.linearNewsFeedbackItem3)
            if (list.size > 3) bindItem(list[3], binding.layoutReviews.imgNewsFeedback04, binding.layoutReviews.txtHeaderNewsFeedback04, binding.layoutReviews.txtDescNewsFeedback04, binding.layoutReviews.linearNewsItemFeedback4)
        }

        // 2: Phone
        news24.newsPhonenum?.let { list ->
            binding.headerPhone.visibility = View.VISIBLE
            if (list.isNotEmpty()) {
                 val cat = list[0].category
                 if (!cat.isNullOrEmpty()) {
                     binding.headerPhone.findViewById<android.widget.TextView>(com.numberniceic.R.id.tv_header_phone)?.text = cat
                 }
            }
            if (list.size > 0) bindItem(list[0], binding.layoutPhone.imgNewsPhonenum01, binding.layoutPhone.txtHeaderNewsPhonenum01, binding.layoutPhone.txtDescNewsPhonenum01, binding.layoutPhone.linearNewsPhonenumItem1)
            if (list.size > 1) bindItem(list[1], binding.layoutPhone.imgNewsPhonenum02, binding.layoutPhone.txtHeaderNewsPhonenum02, binding.layoutPhone.txtDescNewsPhonenum02, binding.layoutPhone.linearNewsPhonenumItem2)
            if (list.size > 2) bindItem(list[2], binding.layoutPhone.imgNewsPhonenum03, binding.layoutPhone.txtHeaderNewsPhonenum03, binding.layoutPhone.txtDescNewsPhonenum03, binding.layoutPhone.linearNewsPhonenumItem3)
            if (list.size > 3) bindItem(list[3], binding.layoutPhone.imgNewsPhonenum04, binding.layoutPhone.txtHeaderNewsPhonenum04, binding.layoutPhone.txtDescNewsPhonenum04, binding.layoutPhone.linearNewsItemPhonenum4)
        }

        // 3: Name Surname
        news24.newsNameSur?.let { list ->
            binding.headerNamesur.visibility = View.VISIBLE
            if (list.isNotEmpty()) {
                 val cat = list[0].category
                 if (!cat.isNullOrEmpty()) {
                     binding.headerNamesur.findViewById<android.widget.TextView>(com.numberniceic.R.id.tv_header_namesur)?.text = cat
                 }
            }
            if (list.size > 0) bindItem(list[0], binding.layoutNamesur.imgNewsNamesur01, binding.layoutNamesur.txtHeaderNewsNamesur01, binding.layoutNamesur.txtDescNewsNamesur01, binding.layoutNamesur.linearNewsNamesurItem1)
            if (list.size > 1) bindItem(list[1], binding.layoutNamesur.imgNewsNamesur02, binding.layoutNamesur.txtHeaderNewsNamesur02, binding.layoutNamesur.txtDescNewsNamesur02, binding.layoutNamesur.linearNewsNamesurItem2)
            if (list.size > 2) bindItem(list[2], binding.layoutNamesur.imgNewsNamesur03, binding.layoutNamesur.txtHeaderNewsNamesur03, binding.layoutNamesur.txtDescNewsNamesur03, binding.layoutNamesur.linearNewsNamesurItem3)
            if (list.size > 3) bindItem(list[3], binding.layoutNamesur.imgNewsNamesur04, binding.layoutNamesur.txtHeaderNewsNamesur04, binding.layoutNamesur.txtDescNewsNamesur04, binding.layoutNamesur.linearNewsItemNamesur4)
        }

        // 4: Tabian
        news24.newsTabian?.let { list ->
            binding.headerTabian.visibility = View.VISIBLE
            if (list.isNotEmpty()) {
                 val cat = list[0].category
                 if (!cat.isNullOrEmpty()) {
                     binding.headerTabian.findViewById<android.widget.TextView>(com.numberniceic.R.id.tv_header_tabian)?.text = cat
                 }
            }
            if (list.size > 0) bindItem(list[0], binding.layoutTabian.imgNewsTabian01, binding.layoutTabian.txtHeaderNewsTabian01, binding.layoutTabian.txtDescNewsTabian01, binding.layoutTabian.linearNewsTabianItem1)
            if (list.size > 1) bindItem(list[1], binding.layoutTabian.imgNewsTabian02, binding.layoutTabian.txtHeaderNewsTabian02, binding.layoutTabian.txtDescNewsTabian02, binding.layoutTabian.linearNewsTabianItem2)
            if (list.size > 2) bindItem(list[2], binding.layoutTabian.imgNewsTabian03, binding.layoutTabian.txtHeaderNewsTabian03, binding.layoutTabian.txtDescNewsTabian03, binding.layoutTabian.linearNewsTabianItem3)
            if (list.size > 3) bindItem(list[3], binding.layoutTabian.imgNewsTabian04, binding.layoutTabian.txtHeaderNewsTabian04, binding.layoutTabian.txtDescNewsTabian04, binding.layoutTabian.linearNewsItemTabian4)
        }

        // 5: Home Num
        news24.newsHome?.let { list ->
            binding.headerHomenum.visibility = View.VISIBLE
            if (list.isNotEmpty()) {
                 val cat = list[0].category
                 if (!cat.isNullOrEmpty()) {
                     binding.headerHomenum.findViewById<android.widget.TextView>(com.numberniceic.R.id.tv_header_homenum)?.text = cat
                 }
            }
            if (list.size > 0) bindItem(list[0], binding.layoutHomenum.imgNewsHomenum01, binding.layoutHomenum.txtHeaderNewsHomenum01, binding.layoutHomenum.txtDescNewsHomenum01, binding.layoutHomenum.linearNewsHomenumItem1)
            if (list.size > 1) bindItem(list[1], binding.layoutHomenum.imgNewsHomenum02, binding.layoutHomenum.txtHeaderNewsHomenum02, binding.layoutHomenum.txtDescNewsHomenum02, binding.layoutHomenum.linearNewsHomenumItem2)
            if (list.size > 2) bindItem(list[2], binding.layoutHomenum.imgNewsHomenum03, binding.layoutHomenum.txtHeaderNewsHomenum03, binding.layoutHomenum.txtDescNewsHomenum03, binding.layoutHomenum.linearNewsHomenumItem3)
            if (list.size > 3) bindItem(list[3], binding.layoutHomenum.imgNewsHomenum04, binding.layoutHomenum.txtHeaderNewsHomenum04, binding.layoutHomenum.txtDescNewsHomenum04, binding.layoutHomenum.linearNewsItemHomenum4)
        }

        // 6: Concept
        news24.newsConcept?.let { list ->
            binding.headerConcept.visibility = View.VISIBLE
            if (list.isNotEmpty()) {
                 val cat = list[0].category
                 if (!cat.isNullOrEmpty()) {
                     binding.headerConcept.findViewById<android.widget.TextView>(com.numberniceic.R.id.tv_header_concept)?.text = cat
                 }
            }
            if (list.size > 0) bindItem(list[0], binding.layoutConcept.imgNewsNumberconcept01, binding.layoutConcept.txtHeaderNewsNumberconcept01, binding.layoutConcept.txtDescNewsNumberconcept01, binding.layoutConcept.linearNewsNumberconceptItem1)
            if (list.size > 1) bindItem(list[1], binding.layoutConcept.imgNewsNumberconcept02, binding.layoutConcept.txtHeaderNewsNumberconcept02, binding.layoutConcept.txtDescNewsNumberconcept02, binding.layoutConcept.linearNewsNumberconceptItem2)
            if (list.size > 2) bindItem(list[2], binding.layoutConcept.imgNewsNumberconcept03, binding.layoutConcept.txtHeaderNewsNumberconcept03, binding.layoutConcept.txtDescNewsNumberconcept03, binding.layoutConcept.linearNewsNumberconceptItem3)
            if (list.size > 3) bindItem(list[3], binding.layoutConcept.imgNewsNumberconcept04, binding.layoutConcept.txtHeaderNewsNumberconcept04, binding.layoutConcept.txtDescNewsNumberconcept04, binding.layoutConcept.linearNewsItemNumberconcept4)
        }
    }

    private fun bindItem(news: com.numberniceic.data.news.NewsHeadline, img: android.widget.ImageView, title: android.widget.TextView, desc: android.widget.TextView, container: View) {
        val imageCandidates = ImageUrlResolver.resolveCandidates(news.newsImg)
        img.setImageResource(com.numberniceic.R.drawable.gold)
        loadImageWithHttpCandidates(img, imageCandidates)

        title.text = news.newsHeader
        desc.text = news.newsDesc
        
        container.setOnClickListener { 
            callActNews(news) 
        }
        container.visibility = View.VISIBLE
    }

    private fun loadImageWithHttpCandidates(img: android.widget.ImageView, candidates: List<String>) {
        val requestKey = candidates.joinToString("|")
        img.tag = requestKey
        if (candidates.isEmpty()) {
            img.setImageResource(com.numberniceic.R.drawable.gold)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = RetrofitClient.okHttpClient
                for (url in candidates) {
                    try {
                        val req = Request.Builder().url(url).get().build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) continue
                            val bytes = resp.body?.bytes() ?: continue
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: continue
                            withContext(Dispatchers.Main) {
                                if (img.tag == requestKey) {
                                    img.setImageBitmap(bitmap)
                                }
                            }
                            return@launch
                        }
                    } catch (_: Exception) {
                    }
                }
                withContext(Dispatchers.Main) {
                    if (img.tag == requestKey) {
                        img.setImageResource(com.numberniceic.R.drawable.gold)
                    }
                }
            } catch (_: CancellationException) {
            }
        }
    }
}
