package com.numberniceic.ui.news

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.numberniceic.data.news.NewsHeadlineCollection
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.utils.PersonNewsCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    val newsCollection = MutableLiveData<NewsHeadlineCollection>()
    val isLoading = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()


    fun loadNews(newsIdType: String) {
        val context = getApplication<Application>()
        
        // 1. Show Placeholder Data IMMEDIATELY (Skeleton Loading)
        val dummyList = ArrayList<com.numberniceic.data.news.NewsHeadline>()
        for (i in 0..5) {
            dummyList.add(com.numberniceic.data.news.NewsHeadline("0", null, null, "กำลังโหลดข้อมูล...", null, "กรุณารอสักครู่...", null, null))
        }
        val dummyCollection = NewsHeadlineCollection("0", dummyList)
        
        // Only show dummy if we don't have data yet
        if (newsCollection.value == null) {
            newsCollection.value = dummyCollection
        }

        // Cache First Strategy
        viewModelScope.launch(Dispatchers.IO) {
            var cacheFound = false
            try {

                val cached = PersonNewsCacheManager.loadNews(context, newsIdType)
                if (cached != null && !cached.newsAll.isNullOrEmpty()) {
                    newsCollection.postValue(cached)
                    isLoading.postValue(false)
                    cacheFound = true

                } 
            } catch (e: Exception) { e.printStackTrace() }

            // If Cache was found, we are good. But we can still update in background if needed.
            // If Cache NOT found, we MUST fetch network.
            
            if (!cacheFound) {
                 isLoading.postValue(true)
                 
                 try {
                    val apiService = RetrofitClient.instance.create(ApiService::class.java)
                    val response = apiService.getNewsAllByType(newsIdType).execute()

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            newsCollection.value = response.body()

                            // Save to Cache
                            PersonNewsCacheManager.saveNews(context, newsIdType, response.body())
                        } else {
                            errorMessage.value = "Failed: ${response.message()}"

                        }
                        isLoading.value = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage.value = "Error: ${e.message}"

                        isLoading.value = false
                    }
                }
            }
        }
    }
}
