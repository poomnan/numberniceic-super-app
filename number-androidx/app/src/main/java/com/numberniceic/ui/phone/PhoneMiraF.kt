package com.numberniceic.ui.phone


import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.numberniceic.R
import com.numberniceic.adapters.MiraclePhoneAdapter
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao
import com.numberniceic.data.phone.DataSortByTypeMiracle
import com.numberniceic.data.phone.PhoneWarningMsgMira


class PhoneMiraF : Fragment() {
    private lateinit var daoPhone: PhoneCollectionDao

    companion object {
        fun newInstance(daoPhone: PhoneCollectionDao): PhoneMiraF {
            val args = Bundle()
            args.putParcelable("DAO", daoPhone)
            val fragment = PhoneMiraF()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        initInstances()
        return inflater.inflate(R.layout.fragment_phone_mira, container, false)
    }

    private fun initInstances() {
        this.daoPhone = arguments!!.get("DAO") as PhoneCollectionDao
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listMiracleD = arrayListOf<DataSortByTypeMiracle>()
        val listMiracleR = arrayListOf<DataSortByTypeMiracle>()

        for (pair in this.daoPhone.dataSortByType!!) {
            if (pair.type!![0].toString() == "D") {
                listMiracleD.add(pair)
            }
            if (pair.type[0].toString() == "R") {
                listMiracleR.add(pair)
            }
        }



        listMiracleD.sortByDescending { selector(it) }
        listMiracleR.sortByDescending { selector(it) }


        val anyList: ArrayList<Any> = arrayListOf()
        anyList.add(this.daoPhone)
        anyList.add(this.daoPhone.percentTotalOfTotal!!)
        anyList.addAll(listMiracleD)
        anyList.addAll(listMiracleR)


        if (this.daoPhone.countPairZero != 0 && this.daoPhone.scoreDupMi != 0) {

            if (this.daoPhone.percentTotalOfTotal!!.percentTotalR != this.daoPhone.pairSpecialX!!.percentOriginR) {
                anyList.add(this.daoPhone.pairSpecialX!!)

            }

        }else {

            if (this.daoPhone.percentTotalOfTotal!!.percentTotalR > daoPhone.pairSpecialX!!.percentOriginR) {
                val percentWarning = this.daoPhone.percentTotalOfTotal!!.percentTotalR - daoPhone.pairSpecialX!!.percentOriginR
                var msg69 = ""
                var msg44 = ""
                for (pair in daoPhone.pairsA!!) {
                    if (pair == "69" || pair == "96") {
                        msg69 = "ค่าความเสี่ยงจากเลขในคู่หลักคือ 96 69 ที่แม้จะไม่ใช่เลขร้ายแรงแต่ก็ไม่ควรมีในคู่หลัก เพราะ 96 69 มีข้อเสียคือ ใจร้อน หงุดหงิดง่าย เจ้าอารมณ์ มีความแปรปรวนในอารมณ์และชีวิตสูง เขามักดึงหน่วงชีวิตให้ขาดความก้าวหน้า และต้องพบเจอกับเรื่องเดิมๆ สถานการณ์เดิมๆ จึงไม่ควรมีในคู่หลัก (แต่สามารถมีในคู่แฝงได้ เพราะเค้ายังมีพลังด้านดีอยู่) แต่ถ้าเลี่ยงไม่ได้ในคู่หลักก็ไม่ควรมีมากกว่า 1 คู่..."
                    }

                    if (pair == "44") {
                        msg44 = "ค่าความเสี่ยงของคู่ 44 เลข 44 แม้จะเป็นเลขดีมีมงคล แต่ไม่ควรมีมากเกินไป เพราะจะทำให้มีอารมณ์หงุดหงิดง่าย ขี้น้อยใจ คิดมาก เจ้าอารมณ์ และมุทะลุดื้อดึงในบางจังหวะ ถ้ามีเยอะให้ระวังคดีความ อุบัติเหตุและความวุ่นวายเกิด ถ้าจะใช้ควรวางเลขกำกับให้ดี"
                    }
                }

                anyList.add(PhoneWarningMsgMira(percentWarning, "$msg69 $msg44"))
            }
        }

        anyList.add("footer")

        val recycleview_mira_phone = view.findViewById<RecyclerView>(R.id.recycleview_mira_phone)
        recycleview_mira_phone.adapter = MiraclePhoneAdapter(anyList)
        recycleview_mira_phone.layoutManager = LinearLayoutManager(context)

    }

    private fun selector(p: DataSortByTypeMiracle): Int = p.percentile


}
