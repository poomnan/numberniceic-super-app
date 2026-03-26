package com.numberniceic.ui.admin

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.data.admin.Article
import com.numberniceic.data.admin.ServerMessage
import com.numberniceic.https.NetworkConfig
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import jp.wasabeef.richeditor.RichEditor
import java.io.ByteArrayOutputStream

class AdminArticleEditF : Fragment() {

    private var article: Article? = null
    
    // Views
    private lateinit var edtTitle: EditText
    private lateinit var edtTitleShort: EditText
    private lateinit var edtSlug: EditText
    private lateinit var edtCategory: EditText
    private lateinit var edtExcerpt: EditText
    private lateinit var edtImageUrl: EditText
    private lateinit var richEditor: RichEditor
    private lateinit var swPublished: SwitchMaterial
    private lateinit var imgPreview: ImageView
    private lateinit var progUpload: ProgressBar
    private lateinit var btnSave: MaterialButton
    private lateinit var btnPick: MaterialButton
    private lateinit var btnCancel: MaterialButton

    // Modern ActivityResult API for image picking
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }
    
    // Image picker for content
    private val pickContentImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadContentImage(it) }
    }

    companion object {
        fun newInstance(article: Article?): AdminArticleEditF {
            val f = AdminArticleEditF()
            val b = Bundle()
            b.putParcelable("article", article)
            f.arguments = b
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        article = arguments?.getParcelable("article")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_admin_article_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupEditorToolbar(view)
        
        article?.let {
            populateData(it)
            view.findViewById<TextView>(R.id.txt_edit_header).text = "แก้ไขบทความ"
        } ?: run {
            view.findViewById<TextView>(R.id.txt_edit_header).text = "สร้างบทความใหม่"
        }

        btnPick.setOnClickListener { pickImage() }
        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
        btnSave.setOnClickListener { saveArticle() }
        
        // ปิดแป้นพิมพ์เมื่อแตะนอก input
        setupHideKeyboardOnTouch(view)
    }
    
    private fun setupHideKeyboardOnTouch(view: View) {
        view.findViewById<View>(R.id.root_layout).setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
                v.clearFocus()
            }
            false
        }
        
        view.findViewById<View>(R.id.scroll_view).setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
                v.clearFocus()
            }
            false
        }
    }
    
    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        val currentFocus = activity?.currentFocus
        if (currentFocus != null) {
            imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    private fun initViews(view: View) {
        edtTitle = view.findViewById(R.id.edt_title)
        edtTitleShort = view.findViewById(R.id.edt_title_short)
        edtSlug = view.findViewById(R.id.edt_slug)
        edtCategory = view.findViewById(R.id.edt_category)
        edtExcerpt = view.findViewById(R.id.edt_excerpt)
        edtImageUrl = view.findViewById(R.id.edt_image_url)
        richEditor = view.findViewById(R.id.rich_editor)
        swPublished = view.findViewById(R.id.sw_published)
        imgPreview = view.findViewById(R.id.img_cover_preview)
        progUpload = view.findViewById(R.id.prog_upload)
        btnSave = view.findViewById(R.id.btn_save_article)
        btnPick = view.findViewById(R.id.btn_pick_image)
        btnCancel = view.findViewById(R.id.btn_cancel)
        
        // Setup RichEditor
        setupRichEditor()
    }
    
    private fun setupRichEditor() {
        richEditor.setEditorHeight(300)
        richEditor.setEditorFontSize(16)
        richEditor.setEditorFontColor(Color.BLACK)
        richEditor.setPadding(12, 12, 12, 12)
        richEditor.setPlaceholder("เขียนเนื้อหาบทความที่นี่...")
        richEditor.setBackgroundColor(Color.WHITE)
    }
    
    private fun setupEditorToolbar(view: View) {
        // Bold
        view.findViewById<ImageButton>(R.id.btn_bold).setOnClickListener {
            richEditor.setBold()
        }
        
        // Italic
        view.findViewById<ImageButton>(R.id.btn_italic).setOnClickListener {
            richEditor.setItalic()
        }
        
        // Underline
        view.findViewById<ImageButton>(R.id.btn_underline).setOnClickListener {
            richEditor.setUnderline()
        }
        
        // Bullet List
        view.findViewById<ImageButton>(R.id.btn_bullet).setOnClickListener {
            richEditor.setBullets()
        }
        
        // Number List
        view.findViewById<ImageButton>(R.id.btn_number).setOnClickListener {
            richEditor.setNumbers()
        }
        
        // Text Color
        view.findViewById<ImageButton>(R.id.btn_text_color).setOnClickListener {
            showColorPicker()
        }
        
        // Insert Image
        view.findViewById<ImageButton>(R.id.btn_insert_image).setOnClickListener {
            pickContentImageLauncher.launch("image/*")
        }
        
        // Insert Link
        view.findViewById<ImageButton>(R.id.btn_insert_link).setOnClickListener {
            showInsertLinkDialog()
        }
    }
    
    private fun showColorPicker() {
        ColorPickerDialog.Builder(requireContext())
            .setTitle("เลือกสีตัวอักษร")
            .setPositiveButton("เลือก", ColorEnvelopeListener { envelope, _ ->
                richEditor.setTextColor(envelope.color)
            })
            .setNegativeButton("ยกเลิก") { dialog, _ -> dialog.dismiss() }
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true)
            .show()
    }
    
    private fun showInsertLinkDialog() {
        val dialogView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null)
        val editText = EditText(context).apply {
            hint = "กรอก URL"
            setPadding(50, 30, 50, 30)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("แทรกลิงก์")
            .setView(editText)
            .setPositiveButton("แทรก") { _, _ ->
                val url = editText.text.toString()
                if (url.isNotEmpty()) {
                    richEditor.insertLink(url, url)
                }
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun populateData(art: Article) {
        edtTitle.setText(art.title)
        edtTitleShort.setText(art.titleShort)
        edtSlug.setText(art.slug)
        edtCategory.setText(art.category)
        edtExcerpt.setText(art.excerpt)
        edtImageUrl.setText(art.imageUrl)
        swPublished.isChecked = art.isPublished == 1
        
        // Set content to RichEditor
        richEditor.html = art.content
        
        var imageUrl = art.imageUrl ?: ""
        if (imageUrl.isNotEmpty() && !imageUrl.startsWith("http")) {
            imageUrl = NetworkConfig.BASE_URL + if (imageUrl.startsWith("/")) imageUrl else "/$imageUrl"
        }
        Glide.with(this).load(imageUrl).placeholder(R.drawable.article).into(imgPreview)
    }

    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }

    private fun uploadImage(uri: Uri) {
        progUpload.visibility = View.VISIBLE
        btnPick.isEnabled = false
        
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Calculate scale factor
            val maxSide = 1024
            var scale = 1
            if (options.outWidth > maxSide || options.outHeight > maxSide) {
                scale = Math.pow(2.0, Math.ceil(Math.log(maxSide.toDouble() / Math.max(options.outWidth, options.outHeight)) / Math.log(0.5)).toInt().toDouble()).toInt()
            }

            // Decode with inSampleSize
            val uploadOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            val inputStream2 = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, uploadOptions)
            inputStream2?.close()

            if (bitmap == null) {
                throw Exception("Failed to decode image")
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream) // Use JPEG 80% for smaller size
            val byteArray = stream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            val json = JsonObject()
            json.addProperty("base64Img", base64)

            apiService.uploadArticleImage(json).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    progUpload.visibility = View.GONE
                    btnPick.isEnabled = true
                    if (response.isSuccessful && response.body() != null) {
                        val obj = response.body()!!
                        try {
                            if (obj.get("status").asString == "success") {
                                val url = obj.get("url").asString
                                edtImageUrl.setText(url)
                                Glide.with(this@AdminArticleEditF).load(uri).into(imgPreview)
                                Toast.makeText(context, "อัปโหลดรูปสำเร็จ", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: ${obj.get("message").asString}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("Upload", "Error logic: ${obj}")
                        }
                    } else {
                        Toast.makeText(context, "Upload Fail: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    progUpload.visibility = View.GONE
                    btnPick.isEnabled = true
                    Toast.makeText(context, "Upload Fail: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            progUpload.visibility = View.GONE
            btnPick.isEnabled = true
            Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadContentImage(uri: Uri) {
        Toast.makeText(context, "กำลังอัปโหลดรูป...", Toast.LENGTH_SHORT).show()
        
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val uploadOptions = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, uploadOptions)
            inputStream?.close()

            if (bitmap == null) {
                Toast.makeText(context, "ไม่สามารถอ่านรูปได้", Toast.LENGTH_SHORT).show()
                return
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            val json = JsonObject()
            json.addProperty("base64Img", base64)

            apiService.uploadArticleImage(json).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful && response.body() != null) {
                        val obj = response.body()!!
                        try {
                            if (obj.get("status").asString == "success") {
                                val url = obj.get("url").asString
                                val fullUrl = NetworkConfig.BASE_URL + if (url.startsWith("/")) url else "/$url"
                                richEditor.insertImage(fullUrl, "รูปภาพ", 300)
                                Toast.makeText(context, "แทรกรูปสำเร็จ", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: ${obj.get("message").asString}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("Upload", "Error logic: ${obj}")
                        }
                    } else {
                        Toast.makeText(context, "อัปโหลดรูปไม่ได้: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(context, "อัปโหลดรูปไม่ได้: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveArticle() {
        val htmlContent = richEditor.html ?: ""
        
        val json = JsonObject()
        article?.let { json.addProperty("art_id", it.artId) }
        json.addProperty("title", edtTitle.text.toString())
        json.addProperty("title_short", edtTitleShort.text.toString())
        json.addProperty("slug", edtSlug.text.toString())
        json.addProperty("category", edtCategory.text.toString())
        json.addProperty("excerpt", edtExcerpt.text.toString())
        json.addProperty("image_url", edtImageUrl.text.toString())
        json.addProperty("content", htmlContent)
        json.addProperty("is_published", if (swPublished.isChecked) 1 else 0)

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.saveAdminArticle(json).enqueue(object : Callback<ServerMessage> {
            override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "บันทึกบทความเรียบร้อย", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "บันทึกไม่ได้: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                Toast.makeText(context, "บันทึกไม่ได้: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
