package com.numberniceic.ui.tambon


import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager

import com.numberniceic.R
import com.numberniceic.adapters.PraPreangAdapter
import com.numberniceic.data.news.News
import androidx.recyclerview.widget.RecyclerView

class Tambonf : Fragment() {

    private lateinit var recycler_tambon: RecyclerView

    companion object {
        fun newInstance(): Tambonf {

            val args = Bundle()
            //args.putString("mira", miraDairy)
            val fragment = Tambonf()
            fragment.arguments = args

            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_tambonf, container, false)
        recycler_tambon = root.findViewById(R.id.recycler_tambon)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sunday = News("1", "", "ปางวันอาทิตย์","ปางวันอาทิตย์", "")
        val monday = News("2", "","ปางวันจันทร์","ปางวันจันทร์", "")
        val tuesday = News("3", "","ปางวันอังคาร", "ปางวันอังคาร", "")
        val wednesday = News("4", "","ปางวันพุธ", "ปางวันพุธ", "")
        val thursday = News("5", "","ปางวันพฤหัสบดี", "ปางวันพฤหัสบดี", "")
        val friday = News("6", "","ปางวันศุกร์", "ปางวันศุกร์", "")
        val saturday = News("7", "","ปางวันเสาร์", "ปางวันเสาร์", "")
        val rahuu = News("8", "","ปางวันพุธกลางคืน", "ปางพระวันพุธกลางคืน", "")
        val chataa = News("chataa", "","วิธีการปรับดวงเรื่องความรักเงินงาน", "", "")
        val love = News("love", "","การทำบุญช่วยส่งเสริมชะตาอาภัพคู่", "", "")

        val listTambon = arrayListOf("header", sunday, monday, tuesday, wednesday, thursday, friday, saturday, rahuu,chataa, love)


        val newsAdapter = PraPreangAdapter(listTambon, this)
        recycler_tambon.adapter = newsAdapter
        recycler_tambon.layoutManager = LinearLayoutManager(context)







    }


}
