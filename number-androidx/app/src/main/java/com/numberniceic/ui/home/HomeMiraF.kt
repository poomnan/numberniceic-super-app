package com.numberniceic.ui.home
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.adapters.HomeMiraAdapter
import com.numberniceic.adapters.TabianMiraAdapter
import com.numberniceic.data.apicollectiondao.HomeCollectionDao
import com.numberniceic.data.home.HomeHeaderMira


class HomeMiraF : Fragment() {

    private lateinit var dao: HomeCollectionDao
    companion object {
        fun newInstance(dao: HomeCollectionDao): HomeMiraF {
            val args = Bundle()
            args.putParcelable("DAO", dao)
            val fragment = HomeMiraF()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        this.dao = arguments!!.get("DAO") as HomeCollectionDao
        return inflater.inflate(R.layout.fragment_home_mira, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val anyList: ArrayList<Any> = arrayListOf()
        anyList.add(HomeHeaderMira(this.dao.scoreV2!!.scoreD, this.dao.scoreV2!!.scoreR, this.dao.homeReport!!.miracleD!!, this.dao.homeReport!!.miracleR!!))
        anyList.addAll(this.dao.pairMiracle!!)
        anyList.add("footer")

        val recycler_mira_home = view.findViewById<RecyclerView>(R.id.recycler_mira_home)
        recycler_mira_home.adapter = HomeMiraAdapter(anyList)
        recycler_mira_home.layoutManager = LinearLayoutManager(context)

    }


}
