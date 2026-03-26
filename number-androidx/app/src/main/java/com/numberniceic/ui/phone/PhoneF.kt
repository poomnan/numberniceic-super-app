package com.numberniceic.ui.phone

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ProgressBar
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.material.snackbar.Snackbar
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao
import com.numberniceic.databinding.FragmentPhoneBinding
import com.numberniceic.utils.PhoneContextManager
import com.numberniceic.utils.SnackContextManager



class PhoneF : Fragment(), View.OnClickListener {
    private var daoPhone: PhoneCollectionDao? = null
    private var imm: InputMethodManager? = null
    private lateinit var phoneViewModel: PhoneViewModel
    private var fragmentView: View? = null
    private var phoneNumber:String? = null

    private lateinit var binding: FragmentPhoneBinding



    companion object {
        fun newInstance(phoneNumber: String): PhoneF {
            val args = Bundle()
            args.putString("phoneNumber", phoneNumber)
            val fragment = PhoneF()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        this.imm = context!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?

        phoneViewModel = ViewModelProvider(this)[PhoneViewModel::class.java]

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_phone, container, false)
        binding.apply {
            lifecycleOwner = this@PhoneF
            phoneObs = phoneViewModel.phoneObserve
        }

        fragmentView = binding.root
        return fragmentView
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ProgressBar>(R.id.progressbar_phone).isVisible = false

        binding.xhonex.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val regexStr = "^[0-9]*$".toRegex()
                val numPosition0 = if(s.toString().isNotEmpty()) s!![0].toString() else "0"
                
                Log.d("DEBUG_VIP", "afterTextChanged: $s, len=${s?.length}, pos0=$numPosition0")

                if (numPosition0 == "0" && s.toString().trim().matches(regexStr) && s.toString().length == 10){
                    Log.d("DEBUG_VIP", "Calling setPhoneApi from TextWatcher")
                    setPhoneApi()
                    imm!!.hideSoftInputFromWindow(view.windowToken, 0)

                }

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        binding.btnMiracle.setOnClickListener(this)
        binding.btnReset.setOnClickListener(this)


        view.findViewById<View>(R.id.chip_add_line_cal).setOnClickListener(this)


        if (arguments?.getString("phoneNumber")!!.length > 1){
            val phoneNumberX = arguments!!.getString("phoneNumber")
            Log.d("DEBUG_VIP", "PhoneF received: $phoneNumberX")
            val xhonex = view.findViewById<EditText>(R.id.xhonex)
            val txtPhoneNumX = view.findViewById<TextView>(R.id.txtPhoneNumX)
            xhonex.setText(phoneNumberX)
            txtPhoneNumX.text = PhoneContextManager.getFormatPhoneNumber(phoneNumberX!!)

        }

        view.findViewById<View>(R.id.btn_buy_tabian).setOnClickListener {
            val intent = Intent(context, PhoneBuyAllAct::class.java)
            startActivity(intent)
        }

        phoneViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            val progressbar_phone = view.findViewById<ProgressBar>(R.id.progressbar_phone)
            if (progressbar_phone != null) progressbar_phone.isVisible = isLoading
        })

    }



    override fun onClick(v: View?) {

        if (view?.findViewById<View>(R.id.chip_add_line_cal) == v){
            val userId = getString(R.string.line_id)
            val sentText = "line://ti/p/~$userId"
            val intent: Intent?
            intent = Intent.parseUri(sentText, Intent.URI_INTENT_SCHEME)
            startActivity(intent)
        }

        if (v == binding.btnReset){
            binding.xhonex.text.clear()
            binding.nestPhoneScroll.fullScroll(ScrollView.FOCUS_UP)
            imm!!.showSoftInput(binding.xhonex, InputMethodManager.SHOW_FORCED)

        }

        if (v == binding.btnMiracle){
            if (this.daoPhone != null){
                val intent = Intent(context, PhoneMiraActivity::class.java)
                intent.putExtra("DAO", this.daoPhone)
                intent.putExtra("PHONENUMBER", this.phoneNumber)
                startActivity(intent)
            }else{
                SnackContextManager.setSnack("ข้อมูลไม่ถูกต้อง!!", binding.coordinatorPhone)

            }

        }


    }

    private fun setPhoneApi() {

        val progressbar_phone = view?.findViewById<ProgressBar>(R.id.progressbar_phone)
        if (progressbar_phone != null) progressbar_phone.isVisible = true
        this.phoneNumber = binding.xhonex.text.toString()
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getPhoneDetail(phoneNumber!!).enqueue(object : Callback<PhoneCollectionDao> {
            override fun onResponse(call: Call<PhoneCollectionDao>, response: Response<PhoneCollectionDao>) {
                // Loading hide handled by ViewModel
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    Log.d("DEBUG_VIP", "API Success! Data Recv: ${data.scoreTotalOfTotal}")
                    this@PhoneF.daoPhone = data
                    phoneViewModel.addPhoneData(data)

                    val txtTitleTelephone = view?.findViewById<TextView>(R.id.txtTitleTelephone)
                    val xhonex = view?.findViewById<EditText>(R.id.xhonex)
                    if (txtTitleTelephone != null && xhonex != null) {
                        txtTitleTelephone.text = PhoneContextManager.getFormatPhoneNumber(xhonex.text.toString())
                    }
                } else {
                    SnackContextManager.setSnack("อาจมีปัญหาเกี่ยวกับเครื่อข่าย โปรดตรวจสอบ!!", binding.coordinatorPhone)
                }
            }

            override fun onFailure(call: Call<PhoneCollectionDao>, t: Throwable) {
                if (progressbar_phone != null) progressbar_phone.isVisible = false
                SnackContextManager.setSnack("อาจมีปัญหาเกี่ยวกับเครื่อข่าย โปรดตรวจสอบ!!", binding.coordinatorPhone)
                Log.d("FROMSERVER", "error" + t.message)
            }
        })
    }

    private fun snackDup(data: PhoneCollectionDao) {
        var messageSnackX = ""
        var messageSnackY = ""

        var statusMessagex = 0
        var statusMessagey = 0


        if(data.scoreDupMi!! > 0){
            statusMessagex = 1
            messageSnackX = getString(R.string.dup_miracle)
        }

        if(data.countPairZero!! > 1){
            statusMessagey = 1
            messageSnackY = getString(R.string.str_countPairZero)
        }

        if (statusMessagex == 1 && statusMessagey == 1){

            val snack = Snackbar.make(binding.coordinatorPhone, "$messageSnackX และ$messageSnackY", 15000)
            snack.setAction("OK") { snack.dismiss()  }
            val tv = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            tv.setTextColor(Color.YELLOW)
            tv.maxLines = 5

            snack.show()


        }else if (statusMessagex == 1){
            val snack = Snackbar.make(binding.coordinatorPhone, messageSnackX, 9000)
            snack.setAction("OK") { snack.dismiss()  }
            val tv = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            tv.setTextColor(Color.YELLOW)
            tv.maxLines = 5

            snack.show()

        }else if (statusMessagey == 1){
            val snack = Snackbar.make(binding.coordinatorPhone, messageSnackY, 9000)
            snack.setAction("OK") { snack.dismiss()  }
            val tv = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            tv.setTextColor(Color.YELLOW)
            tv.maxLines = 5

            snack.show()

        }
    }


}
