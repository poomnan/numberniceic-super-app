package com.numberniceic.ui.phone

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.view.MenuItem
import com.numberniceic.R

class PhoneBuyAllAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_buy_all)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_phone_buy_all))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        supportActionBar!!.title = "เลือกเบอร์โทรดีมีมงคลทั้งหมด"

        supportFragmentManager.beginTransaction().replace(R.id.container_phone_buy_allf,
                PhoneBuyAllF(), "PhoneBuyAllF").commit()

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }




}