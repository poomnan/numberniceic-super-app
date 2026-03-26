package com.numberniceic.ui.home


import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide

import android.widget.ImageView
import com.numberniceic.R
import com.numberniceic.https.NetworkConfig
import com.google.android.material.chip.Chip


class HomeAdF : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home_ad, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val img_exhome01 = view.findViewById<ImageView>(R.id.img_exhome01)
        val img_exhome02 = view.findViewById<ImageView>(R.id.img_exhome02)
        val img_exhome03 = view.findViewById<ImageView>(R.id.img_exhome03)
        val img_exhome04 = view.findViewById<ImageView>(R.id.img_exhome04)
        val img_exhome05 = view.findViewById<ImageView>(R.id.img_exhome05)
        val chip_home_add_line = view.findViewById<Chip>(R.id.chip_add_line)

        // แก้ไขปัญหา Content ไม่แสดงโดยใช้ LinearLayout ธรรมดาแทน RibbonLayout
        // force แสดง content_container และ linDesc
        view.findViewById<View>(R.id.cardRe)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.content_container)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.linDesc)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.linHeader)?.visibility = View.VISIBLE

        // โหลดรูปภาพแบบ deferred เพื่อให้ UI แสดงผลข้อความก่อน
        view.post {
            loadImages(img_exhome01, img_exhome02, img_exhome03, img_exhome04, img_exhome05)
        }

        chip_home_add_line?.setOnClickListener {
            val userId = getString(R.string.line_id)
            val sentText = "line://ti/p/~$userId"
            try{
                val intentLine = Intent.parseUri(sentText, Intent.URI_INTENT_SCHEME)
                startActivity(intentLine)
                Toast.makeText(context, "กรุณารอสักครู่...", Toast.LENGTH_LONG).show()
            }catch (e : ActivityNotFoundException){
                Toast.makeText(context, "โปรดลงแอพพลิเคชั่น LINE เพื่อติดต่อกับเรา", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadImages(img1: ImageView, img2: ImageView, img3: ImageView, img4: ImageView, img5: ImageView) {
        // โหลดรูปภาพแบบ async พร้อม optimization เพื่อไม่ให้ค้างหน้า
        // ใช้ thumbnail เพื่อแสดงรูปเล็กก่อน แล้วค่อยโหลดรูปใหญ่
        // ถ้าโหลดไม่สำเร็จจะใช้รูปจาก drawable แทน
        Glide.with(this)
            .load("${NetworkConfig.BASE_URL}/views/assets/images/icon_homebox01.jpg")
            .thumbnail(0.1f) // โหลดรูปเล็ก 10% ก่อน
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .placeholder(R.drawable.icon_homebox01) // ใช้รูปจาก drawable ระหว่างโหลด
            .error(R.drawable.icon_homebox01) // ถ้าโหลดไม่สำเร็จให้ใช้รูปจาก drawable
            .fallback(R.drawable.icon_homebox01) // fallback ถ้า URL เป็น null
            .override(800, 600) // จำกัดขนาดรูปเพื่อลด memory
            .into(img1)
            
        Glide.with(this)
            .load("${NetworkConfig.BASE_URL}/views/assets/images/icon_homebox02.jpg")
            .thumbnail(0.1f)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .placeholder(R.drawable.icon_homebox02)
            .error(R.drawable.icon_homebox02)
            .fallback(R.drawable.icon_homebox02)
            .override(800, 600)
            .into(img2)
            
        Glide.with(this)
            .load("${NetworkConfig.BASE_URL}/views/assets/images/ex_home02.jpg")
            .thumbnail(0.1f)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ex_home02)
            .error(R.drawable.ex_home02)
            .fallback(R.drawable.ex_home02)
            .override(800, 600)
            .into(img3)
            
        Glide.with(this)
            .load("${NetworkConfig.BASE_URL}/views/assets/images/ex_home03.jpg")
            .thumbnail(0.1f)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ex_home03)
            .error(R.drawable.ex_home03)
            .fallback(R.drawable.ex_home03)
            .override(800, 600)
            .into(img4)
            
        Glide.with(this)
            .load("${NetworkConfig.BASE_URL}/views/assets/images/ex_home04.jpg")
            .thumbnail(0.1f)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ex_home04)
            .error(R.drawable.ex_home04)
            .fallback(R.drawable.ex_home04)
            .override(800, 600)
            .into(img5)
    }


}
