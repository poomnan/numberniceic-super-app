package com.numberniceic.ui.apersonnews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.numberniceic.R
import com.numberniceic.data.admin.InauspiciousData
import com.numberniceic.https.NetworkConfig

class AuspiciousAdapter(private val items: List<InauspiciousData>) :
    RecyclerView.Adapter<AuspiciousAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitle: TextView = view.findViewById(R.id.txt_title)
        val txtDesc: TextView = view.findViewById(R.id.txt_desc)
        val img: ImageView = view.findViewById(R.id.img_inauspicious) // Reuse ID from layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auspicious, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        // Show the content in the colored title field
        holder.txtTitle.text = item.description 
        holder.txtDesc.visibility = View.GONE

        if (!item.imageUrl.isNullOrEmpty()) {
            var finalUrl = item.imageUrl
            if (!finalUrl.startsWith("http")) {
                finalUrl = NetworkConfig.BASE_URL + (if (finalUrl!!.startsWith("/")) finalUrl else "/$finalUrl")
            }
            holder.img.visibility = View.VISIBLE
            Glide.with(holder.itemView.context).load(finalUrl).into(holder.img)
        } else {
            holder.img.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size
}
