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

class GuestDebugFragment : Fragment() {
    
    private lateinit var guestManager: GuestManager
    private lateinit var btnTestDialog: Button
    private lateinit var btnTestAPI: Button
    
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
        btnTestAPI = view.findViewById(R.id.btn_test_direct)
        
        setupClickListeners()
        showGuestInfo()
    }
    
    private fun setupClickListeners() {
        btnTestDialog.setOnClickListener {
            testGuestAddressDialog()
        }
        
        btnTestAPI.setOnClickListener {
            testDirectAPI()
        }
    }
    
    private fun showGuestInfo() {
        val guestId = guestManager.getGuestId()
        val isGuest = guestManager.isGuestUser()
        val tempAddress = guestManager.getTemporaryAddress()
        
        Log.d("GuestDebug", "=== Guest Info ===")
        Log.d("GuestDebug", "Guest ID: $guestId")
        Log.d("GuestDebug", "Is Guest: $isGuest")
        Log.d("GuestDebug", "Temp Address: $tempAddress")
    }
    
    private fun testGuestAddressDialog() {
        Log.d("GuestDebug", "=== Testing Guest Address Dialog ===")
        
        val addressDialog = GuestAddressDialog()
        
        addressDialog.setOnAddressSaved {
            Log.d("GuestDebug", "✅ Address saved successfully!")
            showGuestInfo()
        }
        
        addressDialog.setOnSkip {
            Log.d("GuestDebug", "⏭️ User skipped address")
        }
        
        addressDialog.show(parentFragmentManager, "GuestAddressDialog")
    }
    
    private fun testDirectAPI() {
        Log.d("GuestDebug", "=== Testing Direct API ===")
        
        val guestId = guestManager.getGuestId()
        if (guestId == null) {
            Log.e("GuestDebug", "❌ No Guest ID found!")
            return
        }
        
        val testAddress = "123 ทดสอบ Debug ถนนสุขุมวิท กรุงเทพมหานคร"
        
        // บันทึกลง local ก่อน
        guestManager.saveTemporaryAddress(testAddress)
        
        // ทดสอบ API call
        val addressData = com.google.gson.JsonObject().apply {
            addProperty("guest_id", guestId)
            addProperty("address", testAddress)
            addProperty("created_at", System.currentTimeMillis())
        }
        
        Log.d("GuestDebug", "Sending: $addressData")
        
        com.numberniceic.https.RetrofitClient.api.saveGuestAddress(addressData)
            .enqueue(object : retrofit2.Callback<com.google.gson.JsonObject> {
                override fun onResponse(
                    call: retrofit2.Call<com.google.gson.JsonObject>,
                    response: retrofit2.Response<com.google.gson.JsonObject>
                ) {
                    Log.d("GuestDebug", "✅ Response Code: ${response.code()}")
                    Log.d("GuestDebug", "✅ Response Body: ${response.body()}")
                    
                    if (response.isSuccessful && response.body() != null) {
                        val serverResponse = response.body()!!
                        if (serverResponse.get("success")?.asBoolean == true) {
                            Log.d("GuestDebug", "✅ API SUCCESS!")
                        } else {
                            Log.e("GuestDebug", "❌ API FAILED: ${serverResponse.get("message")}")
                        }
                    } else {
                        Log.e("GuestDebug", "❌ HTTP ERROR: ${response.code()}")
                    }
                }
                
                override fun onFailure(
                    call: retrofit2.Call<com.google.gson.JsonObject>,
                    t: Throwable
                ) {
                    Log.e("GuestDebug", "❌ NETWORK ERROR", t)
                }
            })
    }
    
    companion object {
        fun newInstance(): GuestDebugFragment {
            return GuestDebugFragment()
        }
    }
}
