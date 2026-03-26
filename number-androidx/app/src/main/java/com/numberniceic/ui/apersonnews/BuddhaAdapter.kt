package com.numberniceic.ui.apersonnews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.numberniceic.R
import com.numberniceic.data.admin.BuddhaPang
import com.numberniceic.databinding.ItemDynamicNotificationBinding
import com.numberniceic.utils.ImageUrlResolver

class BuddhaAdapter(
    private var items: MutableList<BuddhaPang>,
    private var ageCurrent: Int? = null,
    private var ageNextYang: Int? = null,
    private val onItemClick: (BuddhaPang) -> Unit
) : RecyclerView.Adapter<BuddhaAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDynamicNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDynamicNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        var title = if (item.pangName.isNullOrEmpty()) "พระพุทธรูป" else item.pangName
        
        
        holder.binding.txtItemTitle.text = title
        holder.binding.txtItemBody.text = if (item.customDescription.isNullOrEmpty()) item.description else item.customDescription
        
        val imageUrl = ImageUrlResolver.resolve(item.imageUrl)

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.buddha)
            .error(R.drawable.pra_preang)
            .into(holder.binding.imgItemIcon)

        holder.binding.txtItemNote.visibility = android.view.View.GONE
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun removeItem(position: Int): BuddhaPang {
        val item = items.removeAt(position)
        notifyItemRemoved(position)
        return item
    }

    fun updateItems(newItems: List<BuddhaPang>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun updateAge(current: Int?, next: Int?) {
        this.ageCurrent = current
        this.ageNextYang = next
        notifyDataSetChanged()
    }
}
