package com.numberniceic.ui.namesur

import android.os.Bundle

import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.numberniceic.R
import com.numberniceic.ui.namenick.NameNickF

class NameSurCalAct : AppCompatActivity() {

    private var day: Int? = null
    private var name: String? = null
    private var surname: String? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_name_sur_cal)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_namesur_cal))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        this.day = intent.getIntExtra("dayPosition", 0)
        this.name = intent.getStringExtra("name")
        this.surname = intent.getStringExtra("surname")

        supportActionBar!!.title = "ถอดรหัสชื่อ $name $surname"


    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        supportFragmentManager.beginTransaction().replace(R.id.namesur_cal_container,
                //NameSurF.newInstance(this.day!!, this.name!!, this.surname!!), "NameSurF").commit()
                NameSurF.newInstance(), "NameSurF").commit()

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}