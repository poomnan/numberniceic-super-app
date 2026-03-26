package com.numberniceic.ui.news

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.adapters.NewsAllAdapter

class NewsAllF : Fragment() {

    private lateinit var viewModel: NewsViewModel

    companion object {
        fun newInstance(newsIdType: String): NewsAllF {
            val args = Bundle()
            args.putString("newsIdType", newsIdType)
            val fragment = NewsAllF()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_news_all, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(NewsViewModel::class.java)
        val newsIdType = arguments?.getString("newsIdType") ?: return

        val recyclerview_newsall = view.findViewById<RecyclerView>(R.id.recyclerview_newsall)
        val progressbar_news = view.findViewById<ProgressBar>(R.id.progressbar_news)

        // PERFORMANCE TUNING (To match Flutter speed)
        // Switch to Grid Layout (2 columns) as requested
        recyclerview_newsall.layoutManager = GridLayoutManager(context, 2)
        recyclerview_newsall.setHasFixedSize(true) // Crucial for performance
        recyclerview_newsall.setItemViewCacheSize(20) // Keep items in memory
        
        val adapter = NewsAllAdapter()
        recyclerview_newsall.adapter = adapter

        viewModel.newsCollection.observe(viewLifecycleOwner, Observer { newsCollection ->
            if (newsCollection?.newsAll != null) {
                adapter.submitList(newsCollection.newsAll)
                progressbar_news?.isVisible = false
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            // Only show spinner if list is empty
            if (adapter.itemCount == 0) {
                 progressbar_news?.isVisible = isLoading
            } else {
                 progressbar_news?.isVisible = false
            }
        })

        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { msg ->
             Log.e("NewsAllF", "Error loading news: $msg")
             android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
        })
        

        // Initial Load
        if (viewModel.newsCollection.value == null) {
            viewModel.loadNews(newsIdType)
        }
    }
}
