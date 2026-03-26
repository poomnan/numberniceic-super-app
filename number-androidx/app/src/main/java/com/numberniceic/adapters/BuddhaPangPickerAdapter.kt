package com.numberniceic.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.numberniceic.R
import com.numberniceic.data.admin.BuddhaPang
import com.numberniceic.utils.ImageUrlResolver

class BuddhaPangPickerAdapter(
    private val items: List<BuddhaPang>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<BuddhaPangPickerAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(item: BuddhaPang)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgBuddha: ImageView = view.findViewById(R.id.img_buddha_pang)
        val txtName: TextView = view.findViewById(R.id.txt_pang_name)
        val txtDesc: TextView = view.findViewById(R.id.txt_pang_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_buddha_pang_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtName.text = item.pangName
        holder.txtDesc.visibility = View.GONE

        val imageUrl = ImageUrlResolver.resolve(item.imageUrl)

        if (imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.buddha)
                .error(R.drawable.pra_preang)
                .into(holder.imgBuddha)
        } else {
            holder.imgBuddha.setImageResource(R.drawable.pra_preang)
        }

        holder.itemView.setOnClickListener {
            listener.onItemClick(item)
        }
    }

    override fun getItemCount() = items.size
}
