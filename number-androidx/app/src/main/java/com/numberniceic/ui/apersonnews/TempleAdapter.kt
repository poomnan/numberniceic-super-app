package com.numberniceic.ui.apersonnews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.numberniceic.R
import com.numberniceic.databinding.ItemDynamicNotificationBinding

class TempleAdapter(
    private var items: MutableList<PersonNewsF.TempleItem>,
    private val onItemClick: (PersonNewsF.TempleItem) -> Unit
) : RecyclerView.Adapter<TempleAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDynamicNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDynamicNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.txtItemTitle.text = item.name
        holder.binding.txtItemBody.text = item.description
        holder.binding.txtItemNote.visibility = View.GONE

        Glide.with(holder.itemView.context)
            .load(item.photoUrl)
            .placeholder(R.drawable.ic_temple_gold)
            .error(R.drawable.thai_temple_placeholder)
            .into(holder.binding.imgItemIcon)

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun removeItem(position: Int): PersonNewsF.TempleItem {
        val item = items.removeAt(position)
        notifyItemRemoved(position)
        return item
    }

    fun updateItems(newItems: List<PersonNewsF.TempleItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}
