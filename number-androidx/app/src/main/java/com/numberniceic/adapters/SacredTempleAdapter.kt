package com.numberniceic.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.numberniceic.R
import com.numberniceic.data.admin.SacredTemple

class SacredTempleAdapter(
    private val items: List<SacredTemple>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<SacredTempleAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(item: SacredTemple)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.img_buddha_pang)
        val title: TextView = view.findViewById(R.id.txt_pang_name)
        val desc: TextView = view.findViewById(R.id.txt_pang_desc)
        // Reuse item_buddha_pang_picker.xml
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_buddha_pang_picker, parent, false) // Reuse existing item layout
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.templeName
        holder.desc.text = item.description

        val imageUrl = com.numberniceic.utils.ImageUrlResolver.resolve(item.imageUrl)
        
        if (imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.buddha)
                .error(R.drawable.pra_preang)
                .into(holder.img)
        } else {
            holder.img.setImageResource(R.drawable.pra_preang)
        }

        holder.itemView.setOnClickListener {
            listener.onItemClick(item)
        }
    }

    override fun getItemCount() = items.size
}
