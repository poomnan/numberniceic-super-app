package com.numberniceic.ui.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.numberniceic.R
import androidx.appcompat.widget.Toolbar

class BagColorAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bag_color)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_bagcolor_act))

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "เพิ่มสีกระเป๋าลูกค้า"


        supportFragmentManager.beginTransaction().replace(R.id.bag_color_container,
                BagColorF.newInstance(), "BagColorF").commit()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
