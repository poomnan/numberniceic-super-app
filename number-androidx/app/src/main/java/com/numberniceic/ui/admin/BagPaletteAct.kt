package com.numberniceic.ui.admin

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.numberniceic.R
import com.numberniceic.data.admin.UserZ
import androidx.appcompat.widget.Toolbar

class BagPaletteAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bag_palette)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_bagpalette_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "เลือกสีจากถาดสี"

        val userz = intent.getParcelableExtra<UserZ>("userz")

        supportFragmentManager.beginTransaction().replace(R.id.bag_palette_container, BagColorDf.newInstance(userz!!), "BagColorDf").commit()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

}
