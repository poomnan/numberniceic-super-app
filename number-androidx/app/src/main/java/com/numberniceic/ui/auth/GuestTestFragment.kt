package com.numberniceic.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.numberniceic.R
import com.numberniceic.utils.GuestManager

class GuestTestFragment : Fragment() {
    
    private lateinit var guestManager: GuestManager
    private lateinit var btnTestDialog: Button
    private lateinit var btnTestDirect: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guest_test, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        guestManager = GuestManager(requireContext())
        btnTestDialog = view.findViewById(R.id.btn_test_dialog)
        btnTestDirect = view.findViewById(R.id.btn_test_direct)
        
        setupClickListeners()
        showGuestInfo()
    }
    
    private fun setupClickListeners() {
        btnTestDialog.setOnClickListener {
            testGuestAddressDialog()
        }
        
        btnTestDirect.setOnClickListener {
            testDirectAPI()
        }
    }
    
    private fun showGuestInfo() {
        val guestId = guestManager.getGuestId()
        val isGuest = guestManager.isGuestUser()
        val tempAddress = guestManager.getTemporaryAddress()
        
        Log.d("GuestTest", "=== Guest Info ===")
        Log.d("GuestTest", "Guest ID: $guestId")
        Log.d("GuestTest", "Is Guest: $isGuest")
        Log.d("GuestTest", "Temp Address: $tempAddress")
    }
    
    private fun testGuestAddressDialog() {
        Log.d("GuestTest", "Testing Guest Address Dialog...")
        
        val addressDialog = GuestAddressDialog()
        
        addressDialog.setOnAddressSaved {
            Log.d("GuestTest", "✅ Address saved successfully!")
            showGuestInfo()
        }
        
        addressDialog.setOnSkip {
            Log.d("GuestTest", "⏭️ User skipped address")
        }
        
        addressDialog.show(parentFragmentManager, "GuestAddressDialog")
    }
    
    private fun testDirectAPI() {
        Log.d("GuestTest", "Testing Direct API Call...")
        
        val guestId = guestManager.getGuestId()
        if (guestId == null) {
            Log.e("GuestTest", "❌ No Guest ID found!")
            return
        }
        
        val testAddress = "123 ทดสอบ ถนนสุขุมวิท กรุงเทพมหานคร"
        
        // บันทึกลง local ก่อน
        guestManager.saveTemporaryAddress(testAddress)
        
        // ทดสอบ API call
        val addressData = com.google.gson.JsonObject().apply {
            addProperty("guest_id", guestId)
            addProperty("address", testAddress)
            addProperty("created_at", System.currentTimeMillis())
        }
        
        Log.d("GuestTest", "Sending: $addressData")
        
        com.numberniceic.https.RetrofitClient.api.saveGuestAddress(addressData)
            .enqueue(object : retrofit2.Callback<com.google.gson.JsonObject> {
                override fun onResponse(
                    call: retrofit2.Call<com.google.gson.JsonObject>,
                    response: retrofit2.Response<com.google.gson.JsonObject>
                ) {
                    Log.d("GuestTest", "✅ API Response Code: ${response.code()}")
                    Log.d("GuestTest", "✅ API Response Body: ${response.body()}")
                    
                    if (response.isSuccessful && response.body() != null) {
                        val serverResponse = response.body()!!
                        if (serverResponse.get("success")?.asBoolean == true) {
                            Log.d("GuestTest", "✅ API Success!")
                        } else {
                            Log.e("GuestTest", "❌ API Failed: ${serverResponse.get("message")}")
                        }
                    } else {
                        Log.e("GuestTest", "❌ HTTP Error: ${response.code()}")
                    }
                }
                
                override fun onFailure(
                    call: retrofit2.Call<com.google.gson.JsonObject>,
                    t: Throwable
                ) {
                    Log.e("GuestTest", "❌ Network Error", t)
                }
            })
    }
}
