package com.numberniceic.ui.phone
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao
import com.numberniceic.utils.PhoneContextManager

class PhoneMiraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_phone_mira)


        setSupportActionBar(findViewById<Toolbar>(R.id.mira_toolbar))
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        val dao = intent.getParcelableExtra<PhoneCollectionDao>("DAO")

        supportActionBar!!.title = "ทำนายเบอร์โทร " + PhoneContextManager.getFormatPhoneNumber(intent.getStringExtra("PHONENUMBER")!!)

        supportFragmentManager.beginTransaction().replace(R.id.phoneMiracleContainer,
                PhoneMiraF.newInstance(dao!!), "PhoneMiraF").commit()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
