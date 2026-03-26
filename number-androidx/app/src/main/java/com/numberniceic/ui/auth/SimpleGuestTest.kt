package com.numberniceic.ui.auth

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.numberniceic.utils.GuestManager

class SimpleGuestTest : Fragment() {
    
    private lateinit var guestManager: GuestManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        guestManager = GuestManager(requireContext())
        
        // ทดสอบทันทีที่สร้าง fragment
        testGuestSystem()
    }
    
    private fun testGuestSystem() {
        Log.d("SimpleGuestTest", "=== Testing Guest System ===")
        
        // 1. ตรวจสอบ Guest ID
        val guestId = guestManager.getGuestId()
        Log.d("SimpleGuestTest", "Guest ID: $guestId")
        
        // 2. ตรวจสอบสถานะ
        val isGuest = guestManager.isGuestUser()
        Log.d("SimpleGuestTest", "Is Guest User: $isGuest")
        
        // 3. ทดสอบบันทึกที่อยู่
        testSaveAddress()
    }
    
    private fun testSaveAddress() {
        val guestId = guestManager.getGuestId()
        if (guestId == null) {
            Log.e("SimpleGuestTest", "❌ No Guest ID found!")
            return
        }
        
        val testAddress = "123 ทดสอบ API ถนนสุขุมวิท กรุงเทพมหานคร"
        
        Log.d("SimpleGuestTest", "Testing save address: $testAddress")
        
        // บันทึกที่อยู่ชั่วคราว
        guestManager.saveTemporaryAddress(testAddress)
        
        // ทดสอบ API Call
        val addressData = com.google.gson.JsonObject().apply {
            addProperty("guest_id", guestId)
            addProperty("address", testAddress)
            addProperty("created_at", System.currentTimeMillis())
        }
        
        Log.d("SimpleGuestTest", "Sending API request: $addressData")
        
        com.numberniceic.https.RetrofitClient.api.saveGuestAddress(addressData)
            .enqueue(object : retrofit2.Callback<com.google.gson.JsonObject> {
                override fun onResponse(
                    call: retrofit2.Call<com.google.gson.JsonObject>,
                    response: retrofit2.Response<com.google.gson.JsonObject>
                ) {
                    Log.d("SimpleGuestTest", "✅ Response Code: ${response.code()}")
                    Log.d("SimpleGuestTest", "✅ Response Body: ${response.body()}")
                    
                    if (response.isSuccessful && response.body() != null) {
                        val serverResponse = response.body()!!
                        val success = serverResponse.get("success")?.asBoolean ?: false
                        val message = serverResponse.get("message")?.asString ?: "No message"
                        
                        if (success) {
                            Log.d("SimpleGuestTest", "✅ API SUCCESS: $message")
                        } else {
                            Log.e("SimpleGuestTest", "❌ API FAILED: $message")
                        }
                    } else {
                        Log.e("SimpleGuestTest", "❌ HTTP ERROR: ${response.code()}")
                        response.errorBody()?.let {
                            Log.e("SimpleGuestTest", "Error Body: $it")
                        }
                    }
                }
                
                override fun onFailure(
                    call: retrofit2.Call<com.google.gson.JsonObject>,
                    t: Throwable
                ) {
                    Log.e("SimpleGuestTest", "❌ NETWORK ERROR", t)
                    Log.e("SimpleGuestTest", "Error message: ${t.message}")
                }
            })
    }
    
    companion object {
        fun newInstance(): SimpleGuestTest {
            return SimpleGuestTest()
        }
    }
}
