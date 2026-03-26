package com.numberniceic.ui.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.utils.OutfitApiColorEntry

sealed class ApiColorMappingRow {
    data class DayHeader(val dayName: String, val dayNumber: Int?, val palette: List<String>) : ApiColorMappingRow()
    data class ColorItem(val entry: OutfitApiColorEntry) : ApiColorMappingRow()
}

class ApiColorMappingAdapter(
    private var items: List<ApiColorMappingRow>,
    private val onEdit: (OutfitApiColorEntry) -> Unit,
    private val onReset: (OutfitApiColorEntry) -> Unit,
    private val onEditDayShade: (Int, String, Int, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DAY_HEADER = 1
        private const val TYPE_COLOR_ITEM = 2
    }

    fun submitList(newItems: List<ApiColorMappingRow>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ApiColorMappingRow.DayHeader -> TYPE_DAY_HEADER
            is ApiColorMappingRow.ColorItem -> TYPE_COLOR_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_DAY_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_api_color_day_header, parent, false)
            DayHeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_api_color_mapping, parent, false)
            ColorMappingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ApiColorMappingRow.DayHeader -> (holder as DayHeaderViewHolder).bind(item)
            is ApiColorMappingRow.ColorItem -> (holder as ColorMappingViewHolder).bind(item.entry)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class DayHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtDayHeader: TextView = itemView.findViewById(R.id.txt_day_header)
        private val shadeButtons: List<Button> = listOf(
            itemView.findViewById(R.id.btn_day_shade_1),
            itemView.findViewById(R.id.btn_day_shade_2),
            itemView.findViewById(R.id.btn_day_shade_3),
            itemView.findViewById(R.id.btn_day_shade_4),
            itemView.findViewById(R.id.btn_day_shade_5)
        )

        fun bind(item: ApiColorMappingRow.DayHeader) {
            val dayNo = item.dayNumber
            txtDayHeader.text = if (dayNo != null) {
                "กลุ่มวัน ${item.dayName} (เลข $dayNo)"
            } else {
                "กลุ่มวัน ${item.dayName}"
            }
            val palette = item.palette
            shadeButtons.forEachIndexed { index, button ->
                val shade = palette.getOrNull(index) ?: "#FFFFFF"
                runCatching { Color.parseColor(shade) }
                    .onSuccess { parsed ->
                        button.backgroundTintList = ColorStateList.valueOf(parsed)
                    }
                    .onFailure {
                        button.backgroundTintList = ColorStateList.valueOf(Color.LTGRAY)
                    }
                button.setOnClickListener {
                    val number = item.dayNumber ?: return@setOnClickListener
                    onEditDayShade(number, item.dayName, index, shade)
                }
            }
        }
    }

    inner class ColorMappingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtColorName: TextView = itemView.findViewById(R.id.txt_color_name)
        private val txtHexValue: TextView = itemView.findViewById(R.id.txt_hex_value)
        private val txtDefaultHex: TextView = itemView.findViewById(R.id.txt_default_hex)
        private val txtDayInfo: TextView = itemView.findViewById(R.id.txt_day_info)
        private val previewColor: View = itemView.findViewById(R.id.view_color_preview)
        private val btnEdit: Button = itemView.findViewById(R.id.btn_edit_color)
        private val btnReset: Button = itemView.findViewById(R.id.btn_reset_color)

        fun bind(item: OutfitApiColorEntry) {
            txtColorName.text = item.colorName
            txtHexValue.text = item.activeHex
            txtDefaultHex.text = "ค่าเริ่มต้น: ${item.defaultHex}"
            val dayNo = item.dayNumber
            val dayName = item.dayName
            txtDayInfo.text = if (dayNo != null && !dayName.isNullOrBlank()) {
                "วันประจำชุดสี: $dayName (เลข $dayNo)"
            } else {
                "วันประจำชุดสี: -"
            }
            btnReset.isEnabled = item.isOverridden

            runCatching { Color.parseColor(item.activeHex) }
                .onSuccess { previewColor.setBackgroundColor(it) }
                .onFailure { previewColor.setBackgroundColor(Color.LTGRAY) }

            btnEdit.setOnClickListener { onEdit(item) }
            btnReset.setOnClickListener { onReset(item) }
        }
    }
}
