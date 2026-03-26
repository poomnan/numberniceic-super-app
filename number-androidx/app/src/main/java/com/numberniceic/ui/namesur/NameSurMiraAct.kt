package com.numberniceic.ui.namesur

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.view.MenuItem
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.NameSurnameCollectionDao


class NameSurMiraAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_name_sur_mira)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_namesur_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val dao = intent.getParcelableExtra<NameSurnameCollectionDao>("DAO")

        //supportActionBar!!.title = "ชื่อ ${dao.name} ${dao.surname}"
        supportActionBar!!.title = "ทำนายชื่อจริงนามสกุล"

        supportFragmentManager.beginTransaction().replace(R.id.namesurMiracleContainer,
                NameSurMiraF.newInstance(dao!!), "NameSurMiraF").commit()
    }





    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
