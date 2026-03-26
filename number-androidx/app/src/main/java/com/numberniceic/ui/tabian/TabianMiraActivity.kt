package com.numberniceic.ui.tabian

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao
import com.numberniceic.data.apicollectiondao.TabianCollectionDao
import com.numberniceic.ui.phone.PhoneMiraF

class TabianMiraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabian_mira)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_tabian_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        val dao = intent.getParcelableExtra<TabianCollectionDao>("DAO")
        val tabian = dao!!.cairId
        supportActionBar!!.title = "ทำนายผลทะเบียน $tabian"

        supportFragmentManager.beginTransaction().replace(R.id.tabianMiracleContainer,
                TabianMiraF.newInstance(dao), "TabianMiraF").commit()
    }




    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
