package com.numberniceic.ui.home

import android.os.Bundle
import android.view.MenuItem

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.numberniceic.R
import com.numberniceic.utils.PhoneContextManager


class HomeCalAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_cal)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_home_cal))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        supportActionBar!!.title = "ถอดรหัสบ้านเลขที่ " + PhoneContextManager.getFormatPhoneNumber(intent.getStringExtra("HOMENUMBER")!!)


    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val homeNumber = intent.getStringExtra("HOMENUMBER")

        supportFragmentManager.beginTransaction().replace(R.id.home_cal_container,
                HomeF.newInstance(homeNumber!!), "HomeF").commit()

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}