package com.numberniceic.ui.renkyam

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.view.MenuItem
import com.numberniceic.R

class RengYam : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reng_yam)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_rengyam_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.rengyam_container,
                RengYamF.newInstance(), "RengYamF").commit()
        }


        supportActionBar!!.title = "ฤกษ์ยามประจำวัน"

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
