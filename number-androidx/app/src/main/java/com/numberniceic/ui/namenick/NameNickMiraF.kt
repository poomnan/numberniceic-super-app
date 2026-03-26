package com.numberniceic.ui.namenick

import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.adapters.NameNickMiraAdapter
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao
import com.numberniceic.data.namesur.HeadMiraNameNick
import com.numberniceic.data.tabian.PairsMiracle


class NameNickMiraF : Fragment() {

    private lateinit var dao: NickNameCollectionDao
    companion object {
        fun newInstance(dao: NickNameCollectionDao): NameNickMiraF {
            val args = Bundle()
            args.putParcelable("DAO", dao)
            val fragment = NameNickMiraF()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        initInstances()

        return inflater.inflate(R.layout.fragment_name_nick_mira, container, false)
    }


    private fun initInstances() {
        this.dao = arguments!!.get("DAO") as NickNameCollectionDao
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name_sat = view.findViewById<TextView>(R.id.name_sat)
        val sumnumsat_all = view.findViewById<TextView>(R.id.sumnumsat_all)
        val sumall_sha = view.findViewById<TextView>(R.id.sumall_sha)
        val recycler_mira_nickname = view.findViewById<RecyclerView>(R.id.recycler_mira_nickname)
        val txt_grade_sat_report_satnamex = view.findViewById<TextView>(R.id.txt_grade_sat_report_satnamex)
        val txt_grade_sat_report_shanamex = view.findViewById<TextView>(R.id.txt_grade_sat_report_shanamex)

        val anyList: ArrayList<Any> = arrayListOf()
        val listD: ArrayList<PairsMiracle> = arrayListOf()
        val listR: ArrayList<PairsMiracle> = arrayListOf()

        for (item in this.dao.pairsMiracle!!){
            if (item.pairtype!![0].toString() == "R"){

                if (item.pairnumber != "0"){
                listR.add(item)

                }

            }
            if (item.pairtype[0].toString() == "D"){
                listD.add(item)
            }
        }

        val satNick = if(dao.sumSatNickName >= 100) "${dao.sumSatNickName}(${dao.pairSatNickName!!.fang})(${dao.pairSatNickName!!.pair})" else dao.sumSatNickName.toString()
        val shaNick = if(dao.sumShaNickName >= 100) "${dao.sumShaNickName}(${dao.pairShaNickName!!.fang})(${dao.pairShaNickName!!.pair})" else dao.sumShaNickName.toString()


        name_sat.text = dao.nickname
        sumnumsat_all.text = satNick
        sumall_sha.text = if(shaNick == "0") "-" else shaNick

        Log.d("shaNick", shaNick)

        anyList.add(HeadMiraNameNick(dao.nickname!!,satNick, shaNick))
        anyList.addAll(listD)
        anyList.addAll(listR)
        anyList.add("footer")

        recycler_mira_nickname.adapter = NameNickMiraAdapter(anyList)
        recycler_mira_nickname.layoutManager = LinearLayoutManager(context)


        if (txt_grade_sat_report_satnamex != null && txt_grade_sat_report_shanamex != null){

            txt_grade_sat_report_satnamex.text = if(dao.sumSatNickName <= 100) this.gradePairSat(satNick,dao) else ""
            txt_grade_sat_report_shanamex.text = if(dao.sumShaNickName <= 100 && dao.sumShaNickName != 0) this.gradePairSat(shaNick,dao) else ""

            if(dao.sumSatNickName <= 100) txt_grade_sat_report_satnamex.setTextColor(ContextCompat.getColor(view.context,this.colorPairSat(satNick, dao)))
            if(dao.sumShaNickName <= 100) txt_grade_sat_report_shanamex.setTextColor(ContextCompat.getColor(view.context,this.colorPairSat(shaNick, dao)))


        }


    }


    private fun colorPairSat(pair:String, dao: NickNameCollectionDao):Int{

        for (item in dao.pairsMiracle!!){
            if (item.pairnumber == pair){
               val colorx = if(item.pairnumber == "26" || item.pairnumber == "62" || item.pairnumber == "23" || item.pairnumber == "32" || item.pairnumber == "40" || item.pairnumber == "04") R.color.color_txt_nocircle_tabian
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
    }

    private fun gradePairSat(pair:String, dao: NickNameCollectionDao):String{

        for (item in dao.pairsMiracle!!){
            if (item.pairnumber == pair){
                val grade = when (item.pairtype) {
                    "D10" -> "ดีเยี่ยม"
                    "D8" -> "ดีมาก"
                    "D5" -> "ดี"
                    "R10" -> "อันตรายที่สุด"
                    "R7" -> "อันตราย"
                    "R5" -> "อันตราย"
                    else -> ""

                }

                return grade
            }
        }

        return ""
    }




}
