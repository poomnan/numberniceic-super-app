package com.numberniceic.ui.namesur


import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.numberniceic.R
import com.numberniceic.adapters.NameNickMiraAdapter

import com.numberniceic.adapters.NameSurMiraAdapter
import com.numberniceic.data.apicollectiondao.NameSurnameCollectionDao
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao
import com.numberniceic.data.namesur.HeadMiraNameSur
import com.numberniceic.data.tabian.PairsMiracle

class NameSurMiraF : Fragment() {

    private lateinit var dao: NameSurnameCollectionDao

    companion object {
        fun newInstance(dao: NameSurnameCollectionDao): NameSurMiraF {
            val args = Bundle()
            args.putParcelable("DAO", dao)
            val fragment = NameSurMiraF()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        initInstances()

        return inflater.inflate(R.layout.fragment_name_sur_mira, container, false)
    }

    private fun initInstances() {
        this.dao = arguments!!.get("DAO") as NameSurnameCollectionDao


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val allList: ArrayList<Any> = arrayListOf()

        val listD: ArrayList<PairsMiracle> = arrayListOf()
        val listR: ArrayList<PairsMiracle> = arrayListOf()

        for (item in dao.pairsMiracle!!){
            if (item.pairtype!![0].toString() == "D"){
                listD.add(item)
            }

            if (item.pairtype[0].toString() == "R"){
                if(item.pairnumber != "0"){
                listR.add(item)

                }
            }
        }

        val sumSatName = if(dao.sumSatName >= 100) "${dao.sumSatName}(${dao.pairSatName!!.fang})(${dao.pairSatName!!.pair})" else dao.sumSatName.toString()
        val sumSatSurName = if(dao.sumSatSurName >= 100) "${dao.sumSatSurName}(${dao.pairSatSurName!!.fang})(${dao.pairSatSurName!!.pair})" else dao.sumSatSurName.toString()
        val sumSatAll = if(dao.sumSatNameSurName >= 100) "${dao.sumSatNameSurName}(${dao.pairSatNameSurName!!.fang})(${dao.pairSatNameSurName!!.pair})" else dao.sumSatNameSurName.toString()

        val sumShaName = if(dao.sumShaName >= 100) "${dao.sumShaName}(${dao.pairShaName!!.fang})(${dao.pairShaName!!.pair})" else dao.sumShaName.toString()
        val sumShaSurName = if(dao.sumShaSurName >= 100) "${dao.sumShaSurName}(${dao.pairShaSurName!!.fang})(${dao.pairShaSurName!!.pair})" else dao.sumShaSurName.toString()
        val sumShaAll = if(dao.sumShaNameSurName >= 100) "${dao.sumShaNameSurName}(${dao.pairShaNameSurName!!.fang})(${dao.pairShaNameSurName!!.pair})" else dao.sumShaNameSurName.toString()

        val name_sat = view.findViewById<TextView>(R.id.name_sat)
        val surname_sat = view.findViewById<TextView>(R.id.surname_sat)
        val sumnumsat_name = view.findViewById<TextView>(R.id.sumnumsat_name)
        val sumnumsat_surname = view.findViewById<TextView>(R.id.sumnumsat_surname)
        val sumnumsat_all = view.findViewById<TextView>(R.id.sumnumsat_all)
        val sumnumsha_name = view.findViewById<TextView>(R.id.sumnumsha_name)
        val sumnumsha_surname = view.findViewById<TextView>(R.id.sumnumsha_surname)
        val sumall_sha = view.findViewById<TextView>(R.id.sumall_sha)
        val recycle_mira_namesur = view.findViewById<RecyclerView>(R.id.recycle_mira_namesur)

        name_sat.text = dao.name
        surname_sat.text = dao.surname

        sumnumsat_name.text = sumSatName
        sumnumsat_surname.text = sumSatSurName
        sumnumsat_all.text = sumSatAll

        sumnumsha_name.text = if(sumShaName != "0") sumShaName else "-"
        sumnumsha_surname.text = if(sumShaSurName != "0") sumShaSurName else "-"
        sumall_sha.text = if(sumShaAll != "0") sumShaAll else "-"


        allList.add(HeadMiraNameSur(dao.name!!, dao.surname!!, sumSatName, sumSatSurName, sumSatAll, sumShaName, sumShaAll, sumShaSurName))

        allList.addAll(listD)
        allList.addAll(listR)

        allList.add("footer")

        recycle_mira_namesur.adapter = NameSurMiraAdapter(allList)
        recycle_mira_namesur.layoutManager = LinearLayoutManager(context)


    }


    /*private fun colorPairSat(pair: String, dao: NameSurnameCollectionDao): Int {

        for (item in dao.pairsMiracle!!) {
            if (item.pairnumber == pair) {
                val colorx = if (item.pairnumber == "26" || item.pairnumber == "62" || item.pairnumber == "23" || item.pairnumber == "32" || item.pairnumber == "40" || item.pairnumber == "04") R.color.color_txt_nocircle_tabian
                else when (item.pairtype) {
                    "D10" -> R.color.color_bg_shasatD
                    "D8" -> R.color.color_bg_shasatD
                    "D5" -> R.color.color_bg_shasatD
                    "R10" -> R.color.color_bg_shasatR
                    "R7" -> R.color.color_bg_shasatR
                    "R5" -> R.color.color_bg_shasatR
                    else -> R.color.color_txt_nocircle_tabian

                }

                return colorx
            }
        }

        return R.color.color_txt_nocircle_tabian
    }*/
}
