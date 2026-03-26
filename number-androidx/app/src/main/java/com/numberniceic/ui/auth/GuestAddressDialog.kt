package com.numberniceic.ui.auth

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.https.RetrofitClient
import com.numberniceic.utils.GuestManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GuestAddressDialog : DialogFragment() {
    
    private lateinit var edtAddress: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnSkip: Button
    private lateinit var guestManager: GuestManager
    private var onAddressSaved: (() -> Unit)? = null
    private var onSkip: (() -> Unit)? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_guest_address, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        guestManager = GuestManager(requireContext())
        
        edtAddress = view.findViewById(R.id.edt_guest_address)
        btnSave = view.findViewById(R.id.btn_save_address)
        btnSkip = view.findViewById(R.id.btn_skip_address)
        
        // แสดงที่อยู่เดิมถ้ามี
        val existingAddress = guestManager.getTemporaryAddress()
        if (!existingAddress.isNullOrEmpty()) {
            edtAddress.setText(existingAddress)
        }
        
        btnSave.setOnClickListener {
            saveAddress()
        }
        
        btnSkip.setOnClickListener {
            onSkip?.invoke()
            dismiss()
        }
    }
    
    private fun saveAddress() {
        val address = edtAddress.text.toString().trim()
        
        if (address.isEmpty()) {
            edtAddress.error = "กรุณากรอกที่อยู่จัดส่ง"
            return
        }
        
        // แสดง loading state
        btnSave.isEnabled = false
        btnSave.text = "กำลังบันทึก..."
        
        // บันทึกลง local ก่อน
        guestManager.saveTemporaryAddress(address)
        
        // ส่งไปบันทึกที่ server
        val addressData = JsonObject().apply {
            addProperty("guest_id", guestManager.getGuestId())
            addProperty("address", address)
            addProperty("created_at", System.currentTimeMillis())
        }
        
        Log.d("GuestAddressDialog", "Saving address: $address")
        Log.d("GuestAddressDialog", "Guest ID: ${guestManager.getGuestId()}")
        
        RetrofitClient.api.saveGuestAddress(addressData).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                // คืนค่าปุ่มให้ใช้งานได้
                btnSave.isEnabled = true
                btnSave.text = "บันทึก"
                
                Log.d("GuestAddressDialog", "Response code: ${response.code()}")
                Log.d("GuestAddressDialog", "Response body: ${response.body()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val serverResponse = response.body()!!
                    if (serverResponse.get("success")?.asBoolean == true) {
                        Toast.makeText(requireContext(), "บันทึกที่อยู่สำเร็จ", Toast.LENGTH_SHORT).show()
                        onAddressSaved?.invoke()
                        dismiss()
                    } else {
                        val message = serverResponse.get("message")?.asString ?: "บันทึกไม่สำเร็จ"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("GuestAddressDialog", "HTTP Error: ${response.code()}")
                    Toast.makeText(requireContext(), "เกิดข้อผิดพลาด (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                // คืนค่าปุ่มให้ใช้งานได้
                btnSave.isEnabled = true
                btnSave.text = "บันทึก"
                
                Log.e("GuestAddressDialog", "Network Error", t)
                Toast.makeText(requireContext(), "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
    
    fun setOnAddressSaved(callback: () -> Unit): GuestAddressDialog {
        this.onAddressSaved = callback
        return this
    }
    
    fun setOnSkip(callback: () -> Unit): GuestAddressDialog {
        this.onSkip = callback
        return this
    }
}
