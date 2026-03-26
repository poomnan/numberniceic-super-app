package com.numberniceic.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.numberniceic.R
import com.numberniceic.data.admin.Article
import com.numberniceic.https.NetworkConfig

class AdminArticleAdapter(
    private var articles: List<Article>,
    private val onEdit: (Article) -> Unit,
    private val onDelete: (Article) -> Unit
) : RecyclerView.Adapter<AdminArticleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgArticle: ImageView = view.findViewById(R.id.img_article)
        val txtTitle: TextView = view.findViewById(R.id.txt_title)
        val txtCategory: TextView = view.findViewById(R.id.txt_category)
        val txtStatus: TextView = view.findViewById(R.id.txt_status)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_article, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articles[position]
        holder.txtTitle.text = article.title
        holder.txtCategory.text = article.category
        
        val isPublished = article.isPublished == 1
        holder.txtStatus.text = if (isPublished) "Published" else "Draft"
        holder.txtStatus.setBackgroundResource(if (isPublished) R.color.green_bg else R.color.gray_bg)
        holder.txtStatus.setTextColor(if (isPublished) R.color.green_text else R.color.gray_text)

        val imageUrl = com.numberniceic.utils.ImageUrlResolver.resolve(article.imageUrl)

        Glide.with(holder.imgArticle.context)
            .load(imageUrl)
            .placeholder(R.drawable.article)
            .into(holder.imgArticle)

        holder.itemView.setOnClickListener { onEdit(article) }
        holder.btnDelete.setOnClickListener { onDelete(article) }
    }

    override fun getItemCount() = articles.size

    fun updateData(newData: List<Article>) {
        this.articles = newData
        notifyDataSetChanged()
    }
}
