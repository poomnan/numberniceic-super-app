package com.numberniceic.ui.tabian

import android.os.Bundle
import android.app.Activity
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.numberniceic.R
import com.numberniceic.ui.phone.PhoneF

class TabianBuyAllAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabian_buy_all)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_tabian_buyall))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        supportActionBar!!.title = "เลือกดูรายการทะเบียนทั้งหมด"


    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)


        supportFragmentManager.beginTransaction().replace(R.id.tabian_buyall_container,
                TabianBuyAllF(), "PhoneF").commit()

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}