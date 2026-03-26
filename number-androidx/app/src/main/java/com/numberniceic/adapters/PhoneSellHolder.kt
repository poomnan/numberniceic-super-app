package com.numberniceic.adapters


import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.home.ScoreRD

import com.numberniceic.data.phone.DataSortByTypeMiracle
import com.numberniceic.data.phone.Miracle
import com.numberniceic.data.phone.PhoneNumberItem
import com.numberniceic.data.tabian.PairsMiracle
import com.numberniceic.ui.phone.PhoneCalAct
import com.numberniceic.utils.PhoneContextManager
import android.widget.TextView
import android.widget.ImageView


class PhoneSellHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!){

        fun bind(part: Any){

                if (part is PhoneNumberItem){
                        val txtPhoneNumber = itemView.findViewById<TextView>(R.id.txt_phone_number)
                        val txtPhoneSum = itemView.findViewById<TextView>(R.id.txt_phone_sum)
                        val txtPhonePrice = itemView.findViewById<TextView>(R.id.txt_phone_price)
                        val iconGroupPhone = itemView.findViewById<ImageView>(R.id.icon_group_phone)

                        val phoneNumber = part.phoneNumber
                        val phoneSum = part.phoneSum
                        val phonePrice = part.phonePrice
                        txtPhoneNumber.text = PhoneContextManager.getFormatPhoneNumber(phoneNumber!!)
                        txtPhoneSum.text = "($phoneSum)"
                        txtPhonePrice.text = "ราคา $phonePrice บ."


                        if (part.phone_group == "viptop4" || part.phone_group == "vip"){
                                iconGroupPhone.setImageResource(R.drawable.ic_vip02)
                        }



                        if (part.phone_group == "d"){
                                iconGroupPhone.setImageResource(R.drawable.icon_diamond02)
                        }


                        if (part.phone_group == "g"){
                                iconGroupPhone.setImageResource(R.drawable.icon_gold01)
                        }

                        if (part.phone_group == "s" || part.phone_group == "b"){
                                iconGroupPhone.setImageResource(R.drawable.ic_clover)
                        }






                        itemView.setOnClickListener {

                                val intent = Intent(itemView.context, PhoneCalAct::class.java)

                                        intent.putExtra("PHONENUMBER", part.phoneNumber)
                                        intent.putExtra("PHONEPRICE", part.phonePrice)
                                        itemView.context.startActivity(intent)


                        }
                }



        }



}
