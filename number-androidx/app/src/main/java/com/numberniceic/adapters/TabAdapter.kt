package com.numberniceic.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.numberniceic.ui.apersonnews.PersonNewsF
import com.numberniceic.ui.home.HomeF
import com.numberniceic.ui.namenick.NameNickF
import com.numberniceic.ui.namesur.NameSurF
import com.numberniceic.ui.phone.PhoneHomeF
import com.numberniceic.ui.tabian.TabianHomeF
import com.numberniceic.ui.home.HomeArticleF
import com.numberniceic.ui.home.HomeCategorizedF

class TabAdapter(fm: FragmentManager, private val tabCount: Int) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position){
            0 -> HomeCategorizedF() 
            1 -> PhoneHomeF()
            2 -> TabianHomeF()
            3 -> NameNickF()
            4 -> NameSurF()
            5 -> HomeF()
            else -> HomeCategorizedF()
        }
    }

    override fun getCount(): Int {
        return tabCount
    }
}