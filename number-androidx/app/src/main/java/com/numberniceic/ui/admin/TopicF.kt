package com.numberniceic.ui.admin


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.gson.JsonObject
import com.numberniceic.databinding.FragmentTopicBinding
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.numberniceic.R
import com.numberniceic.data.admin.ServerMessage
import com.numberniceic.data.topic.ServerTopicMsg
import com.numberniceic.data.topic.Topic
import java.io.ByteArrayOutputStream
import java.io.IOException


class TopicF : Fragment(), View.OnClickListener {
    private lateinit var binding: FragmentTopicBinding

    private lateinit var topicModel: TopicModel
    private var stateChipPhone = true
    private var stateChipHome = true
    private var stateChipTabian = true
    private var stateChipNameSur = true

    companion object {

        private val IMAGE_PICK_CODE = 1000
        private val IMAGE_PICK_CODE_P1 = 100
        private val IMAGE_PICK_CODE_P2 = 101


        private val PERMISSION_CODE = 1001
        private val PERMISSION_CODE_P1 = 1002
        private val PERMISSION_CODE_P2 = 1003

        fun newInstance(): TopicF {
            val args = Bundle()
            //args.putInt("chipId", chipId)
            val fragment = TopicF()
            fragment.arguments = args
            return fragment
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_topic, container, false)
        topicModel = ViewModelProvider(this.activity!!)[TopicModel::class.java]
        binding.apply {
            lifecycleOwner = this@TopicF
            binding.topicObs = topicModel.getTopicObs()
        }
        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chipPhone.setOnClickListener(this)
        binding.chipCar.setOnClickListener(this)
        binding.chipNamesurname.setOnClickListener(this)
        binding.chipHome.setOnClickListener(this)

        binding.btnTopicSave.setOnClickListener(this)
        binding.imgHeader.setOnClickListener(this)
        binding.imgParagraph1.setOnClickListener(this)
        binding.imgParagraph2.setOnClickListener(this)

        this.setNewTopic()


    }

    private fun setNewTopic() {

        val topicJson = JsonObject()
        /*topicJson.addProperty("header_text", topicModel.getTopicHeader())
        topicJson.addProperty("desc_text", topicModel.getTopicDesc())

        topicJson.addProperty("hag_phone", topicModel.getChipPhone())
        topicJson.addProperty("hag_tabian", topicModel.getChipTabian())
        topicJson.addProperty("hag_home", topicModel.getChipHome())
        topicJson.addProperty("hag_namesur", topicModel.getChipNameSur())
        topicJson.addProperty("paragraph1_text", topicModel.getParagraph1())
        topicJson.addProperty("paragraph2_text", topicModel.getParagraph2())
        topicJson.addProperty("paragraph3_text", topicModel.getParagraph3())

        topicJson.addProperty("photo1", topicModel.getBase64Photo1())
        topicJson.addProperty("photo2", topicModel.getBase64Photo2())
        topicJson.addProperty("photo3", topicModel.getBase64Photo3())*/

        topicJson.addProperty("header_text", "")
        topicJson.addProperty("desc_text", "")

        topicJson.addProperty("hag_phone", "")
        topicJson.addProperty("hag_tabian", "")
        topicJson.addProperty("hag_home", "")
        topicJson.addProperty("hag_namesur", "")
        topicJson.addProperty("paragraph1_text", "")
        topicJson.addProperty("paragraph2_text", "")
        topicJson.addProperty("paragraph3_text", "")

        topicJson.addProperty("photo1", "")
        topicJson.addProperty("photo2", "")
        topicJson.addProperty("photo3", "")
        topicJson.addProperty("auth_name", "")


        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.uploadTopic(topicJson).enqueue(object : Callback<Topic> {
            override fun onResponse(call: Call<Topic>, response: Response<Topic>) {
                if (response.isSuccessful && response.body() != null) {
                    val topic = response.body()!!
                    if (topic.topicId != "") {
                        this@TopicF.initBindingTopicView(topic)
                        Toast.makeText(activity!!.applicationContext, "พร้อมสำหรับการเขียนบทความ", Toast.LENGTH_SHORT).show()
                        Log.d("topicObj", topic.toString())
                    } else {
                        Toast.makeText(activity!!.applicationContext, "ไม่สามารถสร้างบทความได้!!!", Toast.LENGTH_SHORT).show()
                        activity!!.finish()
                    }
                } else {
                    Toast.makeText(context, "เกิดข้อผิดพลาดในการสร้างบทความ", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Topic>, t: Throwable) {
                Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
                Log.d("RetrofitError", t.message ?: "Unknown error")
            }
        })
    }

    private fun initBindingTopicView(topic: Topic) {

        if (topic.topicId != null) topicModel.setTopicId(topic.topicId)
        if (topic.authName != null) topicModel.setTopicAuthName(topic.authName)
        if (topic.topicDate != null) topicModel.setTopicDateTime(topic.topicDate)
        if (topic.headerText != null) topicModel.setTopicHeader(topic.headerText)
        if (topic.descText != null) topicModel.setTopicDesc(topic.descText)
        if (topic.paragraph1Text != null) topicModel.setParagraph1(topic.paragraph1Text)
        if (topic.paragraph2Text != null) topicModel.setParagraph2(topic.paragraph2Text)
        if (topic.paragraph3Text != null) topicModel.setParagraph3(topic.paragraph3Text)

        if (topic.photo1 != null) topicModel.setBase64Photo1(topic.photo1)
        if (topic.photo2 != null) topicModel.setBase64Photo2(topic.photo2)
        if (topic.photo3 != null) topicModel.setBase64Photo3(topic.photo3)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.chipPhone -> {
                topicModel.setChipPhone(this.stateChipPhone)
                this.stateChipPhone = !this.stateChipPhone
            }
            binding.chipCar -> {
                topicModel.setChipTabian(this.stateChipTabian)
                this.stateChipTabian = !this.stateChipTabian
            }
            binding.chipHome -> {
                topicModel.setChipHome(this.stateChipHome)
                this.stateChipHome = !this.stateChipHome
            }
            binding.chipNamesurname -> {
                topicModel.setChipNameSur(this.stateChipNameSur)
                this.stateChipNameSur = !this.stateChipNameSur
            }

            binding.imgHeader -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (activity!!.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        // Permission Denies
                        val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        requestPermissions(permissions, PERMISSION_CODE)
                    } else {
                        // Permission Ready granted
                        pickImageFromGallery(0)
                    }
                } else {
                    // System SDK < Android 6.0
                    pickImageFromGallery(0)
                }
            }
            binding.imgParagraph1 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (activity!!.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        // Permission Denies
                        val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        requestPermissions(permissions, PERMISSION_CODE)
                    } else {
                        // Permission Ready granted
                        pickImageFromGallery(1)
                    }
                } else {
                    // System SDK < Android 6.0
                    pickImageFromGallery(1)
                }
            }
            binding.imgParagraph2 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (activity!!.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        // Permission Denies
                        val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        requestPermissions(permissions, PERMISSION_CODE)
                    } else {
                        // Permission Ready granted
                        pickImageFromGallery(2)
                    }
                } else {
                    // System SDK < Android 6.0
                    pickImageFromGallery(2)
                }
            }

            binding.btnTopicSave -> {

                val topicJson = JsonObject()
                topicJson.addProperty("topic_id", topicModel.getTopicId())
                topicJson.addProperty("auth_name", binding.topicAuth.text.toString())
                topicJson.addProperty("header_text", binding.edtHeaderTopic.text.toString())
                topicJson.addProperty("desc_text", binding.edtDescTopic.text.toString())

                topicJson.addProperty("hag_phone", this.stateChipPhone.toString())
                topicJson.addProperty("hag_tabian", this.stateChipTabian.toString())
                topicJson.addProperty("hag_home", this.stateChipHome.toString())
                topicJson.addProperty("hag_namesur", this.stateChipNameSur.toString())
                topicJson.addProperty("paragraph1_text", binding.edtParagraph1.text.toString())
                topicJson.addProperty("paragraph2_text", binding.edtParagraph2.text.toString())
                topicJson.addProperty("paragraph3_text", binding.edtParagraph3.text.toString())

                topicJson.addProperty("photo1", topicModel.getBase64Photo1())
                topicJson.addProperty("photo2", topicModel.getBase64Photo2())
                topicJson.addProperty("photo3", topicModel.getBase64Photo3())

                Log.d("JSONTopic", topicJson.toString())

                val apiService = RetrofitClient.instance.create(ApiService::class.java)
                apiService.updateTopic(topicJson).enqueue(object : Callback<Topic> {
                    override fun onResponse(call: Call<Topic>, response: Response<Topic>) {
                        if (response.isSuccessful && response.body() != null) {
                            val topic = response.body()!!
                            if (topic.topicId != "") {
                                this@TopicF.initBindingTopicView(topic)
                                Toast.makeText(activity!!.applicationContext, "บันทึกบทความแล้ว", Toast.LENGTH_SHORT).show()
                                Log.d("topicUpdate", topic.toString())
                            } else {
                                Toast.makeText(activity!!.applicationContext, "ไม่สามารถบันทึกบทความได้!!!", Toast.LENGTH_SHORT).show()
                                activity!!.finish()
                            }
                        } else {
                            Toast.makeText(context, "เกิดข้อผิดพลาดในการบันทึกบทความ", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Topic>, t: Throwable) {
                        Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
                        Log.d("RetrofitError", t.message ?: "Unknown error")
                    }
                })


            }
        }
    }



    private fun pickImageFromGallery(imtPosition: Int) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"

        when (imtPosition) {
            0 -> startActivityForResult(intent, IMAGE_PICK_CODE)
            1 -> startActivityForResult(intent, IMAGE_PICK_CODE_P1)
            2 -> startActivityForResult(intent, IMAGE_PICK_CODE_P2)

        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission from popup granted
                    pickImageFromGallery(0)
                } else {
                    //permission from popup denied
                    Toast.makeText(context, "Permission denied", Toast.LENGTH_LONG).show()
                }
            }

            PERMISSION_CODE_P1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission from popup granted
                    pickImageFromGallery(1)
                } else {
                    //permission from popup denied
                    Toast.makeText(context, "Permission denied", Toast.LENGTH_LONG).show()
                }
            }

            PERMISSION_CODE_P2 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission from popup granted
                    pickImageFromGallery(2)
                } else {
                    //permission from popup denied
                    Toast.makeText(context, "Permission denied", Toast.LENGTH_LONG).show()
                }
            }


        }

    }

    @Throws(IOException::class)
    private fun readBytes(context: Context, uri: Uri): ByteArray? =
            context.contentResolver.openInputStream(uri)?.buffered()?.use { it.readBytes() }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {

            when (requestCode) {

                IMAGE_PICK_CODE -> {
                    val img = data!!.data
                    if (img != null) {
                        topicModel.setImgHeader(img)

                        if (topicModel.getImgHeader() != null){
                            val imgUri = topicModel.getImgHeader()
                            if (imgUri != null){
                                val iStream =   readBytes(activity!!.baseContext, imgUri)
                                val imgBase64 = Base64.encodeToString(iStream, Base64.DEFAULT)
                                topicModel.setBase64Photo1(imgBase64)
                                Log.d("IMG64Photo1", imgBase64)

                            }
                        }

                    }
                }

                IMAGE_PICK_CODE_P1 -> {
                    val img = data!!.data
                    if (img != null) {
                        topicModel.setImgParagraph1(img)

                        if (topicModel.getImgParagraph1() != null){
                            val imgUri = topicModel.getImgParagraph1()
                            if (imgUri != null){
                                val iStream =   readBytes(activity!!.baseContext, imgUri)
                                val imgBase64 = Base64.encodeToString(iStream, Base64.DEFAULT)
                                topicModel.setBase64Photo2(imgBase64)
                                Log.d("IMG64Photo2", imgBase64)

                            }
                        }

                    }
                }

                IMAGE_PICK_CODE_P2 -> {
                    val img = data!!.data
                    if (img != null) {
                        topicModel.setImgParagraph2(img)

                        if (topicModel.getImgParagraph2() != null){
                            val imgUri = topicModel.getImgParagraph2()
                            if (imgUri != null){
                                val iStream =   readBytes(activity!!.baseContext, imgUri)
                                val imgBase64 = Base64.encodeToString(iStream, Base64.DEFAULT)
                                topicModel.setBase64Photo3(imgBase64)
                                Log.d("IMG64Photo3", imgBase64)

                            }
                        }
                    }
                }

            }
            //binding.imgHeader.setImageURI(topicModel.getImgHeader())
        }
    }


}
