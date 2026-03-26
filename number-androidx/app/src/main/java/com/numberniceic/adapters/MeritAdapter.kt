package com.numberniceic.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.numberniceic.R
import com.numberniceic.data.admin.MeritItem
import com.numberniceic.utils.ImageUrlResolver

class MeritAdapter(
    private val items: List<MeritItem>,
    private val listener: ActionListener
) : RecyclerView.Adapter<MeritAdapter.ViewHolder>() {

    interface ActionListener {
        fun onViewClick(item: MeritItem)
        fun onSendClick(item: MeritItem)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgMerit: ImageView = view.findViewById(R.id.img_merit_thumb)
        val txtName: TextView = view.findViewById(R.id.txt_merit_title)
        val btnView: android.widget.Button = view.findViewById(R.id.btn_merit_view)
        val btnSend: android.widget.Button = view.findViewById(R.id.btn_merit_send)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_merit_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtName.text = item.title
        val imageUrl = ImageUrlResolver.resolve(item.photoUrl)
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.lotus_placeholder)
            .error(R.drawable.lotus_placeholder)
            .centerCrop()
            .into(holder.imgMerit)
        
        holder.itemView.setOnClickListener(null)

        holder.btnView.setOnClickListener {
            listener.onViewClick(item)
        }

        holder.btnSend.setOnClickListener {
            listener.onSendClick(item)
        }
    }

    override fun getItemCount() = items.size
}
