package com.numberniceic.ui.tambon


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

import com.numberniceic.R
import com.numberniceic.adapters.PraPreangAdapter
import com.numberniceic.data.news.News


class TambonDiaf : DialogFragment() {
    companion object {
        fun newInstance(): TambonDiaf {

            val args = Bundle()
            //args.putString("mira", miraDairy)
            val fragment = TambonDiaf()
            fragment.arguments = args

            return fragment
        }
    }

    private var tablayoutTambon: TabLayout? = null
    private var pagerMaster: ViewPager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_tambon_diaf, container, false)

        initInstances(rootView)


        return rootView
    }



    private fun initInstances(rootView: View?) {
        if (rootView != null){
            this.tablayoutTambon = rootView.findViewById(R.id.tablayout_tambon)
            this.pagerMaster = rootView.findViewById(R.id.pager_layout_master)

            val btnClose = rootView.findViewById<View>(R.id.btn_close_tambon)
            btnClose.setOnClickListener {
                dismiss()
            }

            val pagerAdapter = FragmentTambonPagerAdapter(childFragmentManager)
            pagerAdapter.addFragment("วิธีการทำบุญ", Tambonf.newInstance())
            pagerAdapter.addFragment("ขั้นตอนการเปลี่ยนแปลง", ChangeNumf.newInstance())

            pagerMaster?.adapter = pagerAdapter
            tablayoutTambon?.setupWithViewPager(pagerMaster)


        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(true) // 🚀 Force click outside to dismiss
    }

    override fun onStart() {
        super.onStart()
        // Ensure the dialog window isn't forcefully taking over the touch events of the backdrop
        dialog?.window?.apply {
             // Keep default params which usually allow outside touches, 
             // or explicitly ensure WRAP_CONTENT/MATCH_PARENT behavior maintains the 'dim' area interactiveness.
             // If previously full-screened elsewhere, this creates safety.
             setBackgroundDrawableResource(android.R.color.transparent)
        }
    }


}
