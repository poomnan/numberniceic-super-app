package com.numberniceic.ui.tambon


import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager

import androidx.recyclerview.widget.RecyclerView

import com.numberniceic.R
import com.numberniceic.adapters.ChangeNumAdapter

import com.numberniceic.data.news.News


class ChangeNumf : Fragment() {

    companion object {
        fun newInstance(): ChangeNumf {

            val args = Bundle()
            //args.putString("mira", miraDairy)
            val fragment = ChangeNumf()
            fragment.arguments = args

            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_changer_numf, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val phone = News("phone", "","ขั้นตอนการเปลี่ยนแปลงเบอร์โทรศัพท์","", "")
        val namenick = News("namenick", "", "ขั้นตอนการเปลี่ยนแปลงชื่อเล่น","", "")
        val namesur = News("namesur", "", "ขั้นตอนการเปลี่ยนแปลงชื่อจริง นามสกุล","", "")
        val tabian = News("tabian", "","ขั้นตอนการเปลี่ยนแปลงทะเบียนรถ", "", "")
        val home = News("home", "","ขั้นตอนการเปลี่ยนแปลงบ้านเลขที่", "", "")



        val listTambon = arrayListOf("header", phone, namenick, namesur, tabian, home)


        val newsAdapter = ChangeNumAdapter(listTambon, this)
        val recycler_changnum = view.findViewById<RecyclerView>(R.id.recycler_changnum)
        recycler_changnum.adapter = newsAdapter
        recycler_changnum.layoutManager = LinearLayoutManager(context)







    }


}
