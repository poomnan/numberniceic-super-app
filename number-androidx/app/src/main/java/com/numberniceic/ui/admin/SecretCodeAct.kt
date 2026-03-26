package com.numberniceic.ui.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.numberniceic.R

import androidx.appcompat.widget.Toolbar

class SecretCodeAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secret_code)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_secret_code_act))

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "รายการ Secret Code"


        supportFragmentManager.beginTransaction().replace(R.id.secret_code_container,
                SecretCodeF(), "SecretCodeF").commit()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
