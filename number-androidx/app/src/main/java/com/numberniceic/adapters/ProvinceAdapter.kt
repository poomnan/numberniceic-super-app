package com.numberniceic.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.numberniceic.R

class ProvinceAdapter(val context: Context, var listItem: ArrayList<String>): BaseAdapter() {



        private val mInflater: LayoutInflater = LayoutInflater.from(context)


        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val vh: ItemRowHolder

            if (convertView == null){
                view = mInflater.inflate(R.layout.item_spinner_provinces, parent, false)
                vh = ItemRowHolder(view)
                view.tag = vh
            }else{
                view = convertView
                vh = view.tag as ItemRowHolder
            }

            val params = view.layoutParams
            params.height = 100
            view.layoutParams = params
            vh.label!!.text = listItem[position]
            vh.reletiveLayout!!.setBackgroundColor(ContextCompat.getColor(parent!!.context, if (position%2 == 0) R.color.colorBGWhite else R.color.colorBGDay))



            return view
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getCount(): Int {
            return listItem.size
        }


        private class ItemRowHolder(row: View?){
            var label: TextView? = null
            var reletiveLayout: RelativeLayout? = null

            init {
                this.label = row?.findViewById(R.id.txtDay) as TextView
                this.reletiveLayout = row.findViewById(R.id.reSpinnerProvince) as RelativeLayout
            }
        }

    }
