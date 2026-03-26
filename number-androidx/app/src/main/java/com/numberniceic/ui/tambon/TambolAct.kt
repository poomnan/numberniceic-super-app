package com.numberniceic.ui.tambon

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.view.MenuItem
import com.numberniceic.R
import com.numberniceic.utils.PersonContextManager

class TambolAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambol)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_tambon_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val id = intent.getStringExtra("tambonId")
        val type = intent.getStringExtra("type")
        val typeTitle = intent.getStringExtra("type_title")

        supportFragmentManager.beginTransaction().replace(R.id.tambol_container,
                TambonDetailF.newInstance(id!!, type!!), "TambonDetailF").commit()


        if(type == "change_num"){
            supportActionBar!!.title = typeTitle
        }else{
        supportActionBar!!.title = typeTitle

        }



    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
