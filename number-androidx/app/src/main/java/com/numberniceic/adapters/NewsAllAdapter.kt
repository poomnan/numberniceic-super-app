package com.numberniceic.adapters

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.numberniceic.R
import com.numberniceic.data.news.NewsHeadline
import com.numberniceic.ui.news.NewsAct
import com.numberniceic.utils.ImageUrlResolver

class NewsAllAdapter : ListAdapter<NewsHeadline, NewsAllAdapter.NewsHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsHolder(view)
    }

    override fun onBindViewHolder(holder: NewsHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class NewsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgNews: ImageView = itemView.findViewById(R.id.img_news)
        private val txtHeaderNews: TextView = itemView.findViewById(R.id.txt_header_news)
        private val txtDescNews: TextView = itemView.findViewById(R.id.txt_desc_news)
        private val linearNewsItem: View = itemView.findViewById(R.id.linear_news_item)

        fun bind(item: NewsHeadline, position: Int) {
            val candidates = ImageUrlResolver.resolveCandidates(item.newsImg)
            val glide = Glide.with(itemView.context)
            var chain: com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable>? = null
            for (candidate in candidates.asReversed()) {
                val request = glide
                    .load(candidate)
                    .placeholder(R.drawable.gold)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                chain = if (chain == null) request.error(R.drawable.gold) else request.error(chain)
            }
            if (chain != null) {
                chain.into(imgNews)
            } else {
                imgNews.setImageResource(R.drawable.gold)
            }

            // Use Short Title as requested, fallback to full header
            txtHeaderNews.text = item.newsTitleShort ?: item.newsHeader
            
            // Description is hidden in XML for grid view
            // txtDescNews.text = item.newsDesc
            
            // Disable background color logic for Grid View as the image covers the card
            // val bgColorRes = if (position % 2 == 0) R.color.colorBGWhite else R.color.colorBGDay
            // linearNewsItem.setBackgroundColor(ContextCompat.getColor(itemView.context, bgColorRes))

            itemView.setOnClickListener {
                Log.d("NewsAllAdapter", "Clicked Item ID: ${item.newsId}")
                val intent = Intent(itemView.context, NewsAct::class.java)
                intent.putExtra("newsId", item.newsId)
                intent.putExtra("newsObject", item) // Pass full object
                itemView.context.startActivity(intent)
            }
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<NewsHeadline>() {
        override fun areItemsTheSame(oldItem: NewsHeadline, newItem: NewsHeadline): Boolean {
            return oldItem.newsId == newItem.newsId
        }

        override fun areContentsTheSame(oldItem: NewsHeadline, newItem: NewsHeadline): Boolean {
            return oldItem == newItem
        }
    }
}
