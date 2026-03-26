package com.numberniceic.ui.namenick
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao

class NameNickMiraAct : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_namenick_mira)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_nickname_act))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val dao = intent.getParcelableExtra<NickNameCollectionDao>("DAO")

        //supportActionBar!!.title = "ทำนายชื่อเล่น " + dao.nickname
        supportActionBar!!.title = "ทำนายชื่อเล่น"

        supportFragmentManager.beginTransaction().replace(R.id.nicknameMiracleContainer,
                NameNickMiraF.newInstance(dao!!), "NameNickMiraF").commit()
    }





    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
