package com.numberniceic.ui.admin

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.data.admin.UserZ
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object UserManagementHelper {

    fun showDeleteConfirmation(context: Context, userZ: UserZ, onSuccess: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("ยืนยันการลบ")
            .setMessage("คุณแน่ใจหรือไม่ว่าต้องการลบผู้ใช้งาน ${userZ.realName} (#${userZ.memberId})?")
            .setPositiveButton("ลบ") { _, _ ->
                deleteUserFromServer(context, userZ, onSuccess)
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun deleteUserFromServer(context: Context, userZ: UserZ, onSuccess: () -> Unit) {
        val body = JsonObject()
        body.addProperty("memberid", userZ.memberId)
        
        RetrofitClient.instance.create(ApiService::class.java).deleteMember(body).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "ลบสำเร็จ", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "ลบไม่สำเร็จ: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(context, "เกิดข้อผิดพลาด: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun showEditDialog(context: Context, layoutInflater: LayoutInflater, userZ: UserZ, onSuccess: () -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_user, null)
        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()
        
        val edtRealName = view.findViewById<EditText>(R.id.edt_realname)
        val edtSurName = view.findViewById<EditText>(R.id.edt_surname)
        val edtBirthday = view.findViewById<EditText>(R.id.edt_birthday)
        val btnSave = view.findViewById<Button>(R.id.btn_save_user)
        
        edtRealName.setText(userZ.realName)
        edtSurName.setText(userZ.surName)
        edtBirthday.setText(userZ.birthDat)
        
        btnSave.setOnClickListener {
            val body = JsonObject()
            body.addProperty("memberid", userZ.memberId)
            body.addProperty("realname", edtRealName.text.toString())
            body.addProperty("surname", edtSurName.text.toString())
            body.addProperty("birthday", edtBirthday.text.toString())
            
            RetrofitClient.instance.create(ApiService::class.java).editMember(body).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "อัพเดทสำเร็จ", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        onSuccess()
                    } else {
                        Toast.makeText(context, "อัพเดทไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(context, "เกิดข้อผิดพลาด: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
        
        dialog.show()
    }
}
