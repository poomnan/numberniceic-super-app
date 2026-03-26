package com.numberniceic.ui.apersonnews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.numberniceic.R
import com.numberniceic.databinding.ItemDynamicNotificationBinding
import com.numberniceic.utils.ImageUrlResolver

import com.numberniceic.data.admin.SpellItem

class SpellAdapter(
    private var items: MutableList<SpellItem>,
    private val onItemClick: (SpellItem) -> Unit
) : RecyclerView.Adapter<SpellAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDynamicNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDynamicNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.txtItemTitle.text = item.title
        holder.binding.txtItemBody.text = item.content
        
        if (!item.note.isNullOrEmpty()) {
            holder.binding.txtItemNote.text = "คำเตือนพิเศษ: ${item.note}"
            holder.binding.txtItemNote.visibility = View.VISIBLE
        } else {
            holder.binding.txtItemNote.visibility = View.GONE
        }

        val content = (item.title + " " + item.content).lowercase()
        val isBuddha = content.contains("ชินบัญชร") || content.contains("พระ") || content.contains("ปาง") || content.contains("สมเด็จ") || content.contains("สวด")
        
        val photoUrl = ImageUrlResolver.resolve(item.photoUrl)

        Glide.with(holder.itemView.context)
            .load(photoUrl)
            .placeholder(if (isBuddha) R.drawable.buddha else R.drawable.lotus_placeholder)
            .error(if (isBuddha) R.drawable.buddha else R.drawable.lotus_placeholder)
            .into(holder.binding.imgItemIcon)

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun removeItem(position: Int): SpellItem {
        val item = items.removeAt(position)
        notifyItemRemoved(position)
        return item
    }

    fun updateItems(newItems: List<SpellItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}
