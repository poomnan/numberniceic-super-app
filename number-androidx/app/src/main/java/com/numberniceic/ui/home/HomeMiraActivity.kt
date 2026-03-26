package com.numberniceic.ui.home

import android.os.Bundle
import android.app.Activity
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.HomeCollectionDao


class HomeMiraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_mira)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_home_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val dao = intent.getParcelableExtra<HomeCollectionDao>("DAO")

        supportActionBar!!.title = "ทำนายบ้านเลขที่ " + dao!!.homeId
        supportFragmentManager.beginTransaction().replace(R.id.homeMiracleContainer,
                HomeMiraF.newInstance(dao), "HomeMiraF").commit()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
