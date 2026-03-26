package com.numberniceic.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.numberniceic.R
import com.numberniceic.data.admin.SpellItem
import com.numberniceic.utils.ImageUrlResolver

class SpellAdapter(
    private val items: List<SpellItem>,
    private val listener: ActionListener
) : RecyclerView.Adapter<SpellAdapter.ViewHolder>() {

    interface ActionListener {
        fun onSendClick(item: SpellItem)
        fun onViewClick(item: SpellItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_spell_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTitle: TextView = itemView.findViewById(R.id.txt_spell_title)
        private val txtDesc: TextView = itemView.findViewById(R.id.txt_spell_desc)
        private val txtNote: TextView = itemView.findViewById(R.id.txt_spell_note)
        private val imgThumb: android.widget.ImageView = itemView.findViewById(R.id.img_spell_thumb)
        private val btnSend: View = itemView.findViewById(R.id.btn_spell_send)
        private val btnView: View = itemView.findViewById(R.id.btn_spell_view)

        fun bind(item: SpellItem) {
            val prefix = if (item.type == "warning") "[คำเตือน] " else "[คาถา] "
            txtTitle.text = prefix + item.title
            
            txtDesc.text = item.content ?: ""

            if (!item.note.isNullOrEmpty()) {
                txtNote.text = "คำเตือนพิเศษ: ${item.note}"
                txtNote.visibility = View.VISIBLE
            } else {
                txtNote.visibility = View.GONE
            }

            if (!item.photoUrl.isNullOrEmpty()) {
                val imageUrl = ImageUrlResolver.resolve(item.photoUrl)

                com.bumptech.glide.Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.lotus_placeholder)
                    .error(R.drawable.lotus_placeholder)
                    .into(imgThumb)
            } else {
                imgThumb.setImageResource(R.drawable.lotus_placeholder)
            }

            btnSend.setOnClickListener {
                listener.onSendClick(item)
            }
            
            btnView.setOnClickListener {
                listener.onViewClick(item)
            }
        }
    }
}
