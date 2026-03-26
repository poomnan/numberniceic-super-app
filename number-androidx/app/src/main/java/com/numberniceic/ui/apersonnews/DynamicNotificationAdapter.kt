package com.numberniceic.ui.apersonnews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.local.NotiModel
import com.numberniceic.databinding.ItemDynamicNotificationBinding
import android.util.Log

class DynamicNotificationAdapter(
    private var items: MutableList<NotiModel>,
    private val onItemClick: (NotiModel) -> Unit
) : RecyclerView.Adapter<DynamicNotificationAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDynamicNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDynamicNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        val title = item.title.orEmpty()
        val body = item.body.orEmpty()
        val content = (title + " " + body).lowercase()
        
        // Comprehensive Buddha detection
        val isBuddha = content.contains("ปาง") || 
                       content.contains("พระ") || 
                       content.contains("ชินบัญชร") || 
                       content.contains("สมเด็จ") || 
                       content.contains("สวด") ||
                       content.contains("วันเกิด") ||
                       content.contains("ประจำวัน")

        Log.d("DynamicNoti", "Position: $position, Title: $title, isBuddha: $isBuddha")
        
        if (body.contains("คุณนินแนะนำ : ")) {
            holder.binding.txtItemTitle.text = body.substringAfter("คุณนินแนะนำ : ")
            holder.binding.txtItemBody.text = "คลิกเพื่อดูรายละเอียด"
        } else {
            holder.binding.txtItemTitle.text = title
            holder.binding.txtItemBody.text = if (body.isNotEmpty()) body else "คลิกเพื่อดูรายละเอียด"
        }

        // Handle Icons & Images
        var imageUrl = item.url?.trim().orEmpty()
        
        // Smart inference for Buddha Pangs if URL is missing or is just a web link
        val isWebLink = imageUrl.startsWith("http") && !imageUrl.contains("/uploads/")
        if (isBuddha && (imageUrl.isEmpty() || isWebLink)) {
            val inferredUrl = when {
                content.contains("อาทิตย์") -> "/uploads/buddha/pang_sunday.jpg"
                content.contains("จันทร์") -> "/uploads/buddha/pang_monday.jpg"
                content.contains("อังคาร") -> "/uploads/buddha/pang_tuesday.jpg"
                content.contains("พุธกลางคืน") -> "/uploads/buddha/pang_wed_night.jpg"
                content.contains("พุธกลางวัน") || content.contains("พุธ") -> "/uploads/buddha/pang_wednesday.jpg"
                content.contains("พฤหัสบดี") -> "/uploads/buddha/pang_thursday.jpg"
                content.contains("ศุกร์") -> "/uploads/buddha/pang_friday.jpg"
                content.contains("เสาร์") -> "/uploads/buddha/pang_saturday.jpg"
                content.contains("ชินบัญชร") -> "/uploads/buddha/chinnarat.png"
                else -> ""
            }
            if (inferredUrl.isNotEmpty()) {
                imageUrl = inferredUrl
                Log.d("DynamicNoti", "Inferred URL: $imageUrl for content: $content")
            }
        }

        val resolvedUrl = if (imageUrl.isNotEmpty()) com.numberniceic.utils.ImageUrlResolver.resolve(imageUrl) else ""
        Log.d("DynamicNoti", "Final Resolved URL: $resolvedUrl")
        
        if (resolvedUrl.isNotEmpty() && (resolvedUrl.contains(".jpg") || resolvedUrl.contains(".png") || resolvedUrl.contains(".jpeg") || resolvedUrl.contains("/uploads/"))) {
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(resolvedUrl)
                .placeholder(if (isBuddha) R.drawable.buddha else R.drawable.lotus_placeholder)
                .error(if (isBuddha) R.drawable.buddha else R.drawable.lotus_placeholder)
                .into(holder.binding.imgItemIcon)
        } else {
            // Fallback to static icons
            Log.d("DynamicNoti", "Falling back to static icon for type: ${item.type}")
            if (item.type == "webview_merit") {
                if (isBuddha) {
                    holder.binding.imgItemIcon.setImageResource(R.drawable.buddha)
                } else {
                    holder.binding.imgItemIcon.setImageResource(R.drawable.lotus_placeholder)
                }
            } else if (item.type == "webview_changenum") {
                holder.binding.imgItemIcon.setImageResource(R.drawable.approval)
            } else if (item.type == "webview_spell") {
                if (isBuddha) {
                    holder.binding.imgItemIcon.setImageResource(R.drawable.buddha)
                } else {
                    holder.binding.imgItemIcon.setImageResource(R.drawable.lotus_placeholder)
                }
            } else {
                holder.binding.imgItemIcon.setImageResource(R.drawable.approval)
            }
        }

        if (item.note != null && item.note.isNotEmpty()) {
            holder.binding.txtItemNote.text = "คำเตือนพิเศษ: ${item.note}"
            holder.binding.txtItemNote.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.txtItemNote.visibility = android.view.View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun removeItem(position: Int): NotiModel {
        val item = items.removeAt(position)
        notifyItemRemoved(position)
        return item
    }

    fun updateItems(newItems: List<NotiModel>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}
