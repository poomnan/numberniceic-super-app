package com.numberniceic.ui.tambon

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class FragmentTambonPagerAdapter(fm:FragmentManager): FragmentPagerAdapter(fm) {

    val mFragmentCollectionj = arrayListOf<Fragment>()
    val mTitleCollcetion = arrayListOf<String>()

    fun addFragment(title:String, fragment:Fragment){
        mFragmentCollectionj.add(fragment)
        mTitleCollcetion.add(title)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return mTitleCollcetion[position]
    }

    override fun getItem(position: Int): Fragment {
        return mFragmentCollectionj[position]
    }

    override fun getCount(): Int {
        return mFragmentCollectionj.size

    }

}