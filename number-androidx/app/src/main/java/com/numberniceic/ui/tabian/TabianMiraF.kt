package com.numberniceic.ui.tabian


import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager

import androidx.recyclerview.widget.RecyclerView

import com.numberniceic.R
import com.numberniceic.adapters.TabianMiraAdapter
import com.numberniceic.data.apicollectiondao.TabianCollectionDao
import com.numberniceic.data.phone.DataSortByTypeMiracle
import com.numberniceic.data.tabian.PairsMiracle
import com.numberniceic.data.tabian.SiangTabian
import com.numberniceic.data.tabian.SumTotalPercent
import com.numberniceic.data.tabian.TabianHeaderReport


class TabianMiraF : Fragment() {

    private lateinit var daoTabian: TabianCollectionDao

    companion object {
        fun newInstance(daoTabian: TabianCollectionDao): TabianMiraF {
            val args = Bundle()
            args.putParcelable("DAO", daoTabian)
            val fragment = TabianMiraF()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        initInstances()
        return inflater.inflate(R.layout.fragment_tabian_mira, container, false)
    }

    private fun initInstances() {
        this.daoTabian = arguments!!.get("DAO") as TabianCollectionDao
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val anyList: ArrayList<Any> = arrayListOf()
        val listMiracleD = arrayListOf<PairsMiracle>()
        val listMiracleR = arrayListOf<PairsMiracle>()

        for (pair in this.daoTabian.pairsMiracle!!) {
            if (pair.pairtype!![0].toString() == "D") {
                listMiracleD.add(pair)
            }
            if (pair.pairtype[0].toString() == "R") {
                listMiracleR.add(pair)
            }
        }

        listMiracleD.sortByDescending { selector(it) }
        listMiracleR.sortByDescending { selector(it) }

        anyList.add(TabianHeaderReport(this.daoTabian.sumTotalPercent!!.sumPercentD, this.daoTabian.sumTotalPercent!!.sumPercentR, this.daoTabian.scoreTotalD!!, this.daoTabian.scoreTotalR!!, this.daoTabian.miracleSummary!!.miracleD!!, this.daoTabian.miracleSummary!!.miracleR!!))

        anyList.addAll(listMiracleD)
        anyList.addAll(listMiracleR)


        val percentSiang = this.percentSiag(this.daoTabian, this.daoTabian.sumTotalPercent!!)

        if (percentSiang > 0) {
            anyList.add(SiangTabian(percentSiang, "มีความเสี่ยง!! $percentSiang%"))
        }

        anyList.add("footer")

        val recycler_mira_tabian = view.findViewById<RecyclerView>(R.id.recycler_mira_tabian)
        recycler_mira_tabian.adapter = TabianMiraAdapter(anyList)
        recycler_mira_tabian.layoutManager = LinearLayoutManager(context)

    }


    private fun percentSiag(daoTabian: TabianCollectionDao, sumTotalPercent: SumTotalPercent): Int {

        var percentR = 0

        for (pairMiracle in daoTabian.pairsMiracle!!) {

            if (pairMiracle.pairtype!![0].toString() == "R") {
                percentR += pairMiracle.percent

            }

        }

        val saig = sumTotalPercent.sumPercentR - percentR

        return saig
    }


    private fun selector(p: PairsMiracle): Int = p.percent


}
