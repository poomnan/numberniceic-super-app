package com.numberniceic.ui.tabian

import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.numberniceic.R
import com.numberniceic.ui.phone.PhoneF

class TabianCalActivity : AppCompatActivity() {

    private var tabianNumber:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tabian_cal)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_tabian_cal))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        tabianNumber = intent.getStringExtra("TABAINNUMBER")


        supportActionBar!!.title = "ถอดรหัสทะเบียน $tabianNumber"


    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(name, context, attrs)

    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)


        supportFragmentManager.beginTransaction().replace(R.id.tabian_cal_container,
                TabianF.newInstance(this.tabianNumber!!), "tabainNumber").commit()

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
