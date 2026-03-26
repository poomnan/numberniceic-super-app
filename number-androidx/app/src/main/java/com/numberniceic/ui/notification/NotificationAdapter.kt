package com.numberniceic.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R

data class NotiItem(val title: String, val body: String, val date: String, val type: String? = null, val url: String? = null)

class NotificationAdapter(
    private val items: List<NotiItem>,
    private val onItemClick: (NotiItem) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitle: TextView = view.findViewById(R.id.txt_noti_title)
        val txtBody: TextView = view.findViewById(R.id.txt_noti_body)
        val txtDate: TextView = view.findViewById(R.id.txt_noti_date)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_notification_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtTitle.text = item.title
        holder.txtBody.text = item.body
        holder.txtDate.text = item.date
        
        // 🆕 Set Icon based on type
        val iconRes = when (item.type) {
            "chat", "admin_message" -> R.drawable.ic_chat_bubble
            else -> R.drawable.notification_w
        }
        val imgIcon = holder.itemView.findViewById<android.widget.ImageView>(R.id.img_noti_icon)
        imgIcon.setImageResource(iconRes)
        
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}
