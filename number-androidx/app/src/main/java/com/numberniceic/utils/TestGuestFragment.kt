package com.numberniceic.utils

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.numberniceic.R
import com.numberniceic.ui.auth.GuestAddressDialog

class TestGuestFragment : Fragment() {
    
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ทดสอบแสดง Guest Address Dialog
        testGuestAddressDialog()
    }
    
    private fun testGuestAddressDialog() {
        val guestManager = GuestManager(requireContext())
        val guestId = guestManager.getGuestId()
        
        Log.d("TestGuest", "Current Guest ID: $guestId")
        Log.d("TestGuest", "Is Guest User: ${guestManager.isGuestUser()}")
        
        val addressDialog = GuestAddressDialog()
        
        addressDialog.setOnAddressSaved {
            Log.d("TestGuest", "Address saved successfully!")
            // ตรวจสอบข้อมูลหลังบันทึก
            val savedAddress = guestManager.getTemporaryAddress()
            Log.d("TestGuest", "Saved address: $savedAddress")
        }
        
        addressDialog.setOnSkip {
            Log.d("TestGuest", "User skipped address input")
        }
        
        addressDialog.show(parentFragmentManager, "TestGuestAddressDialog")
    }
    
    companion object {
        fun newInstance(): TestGuestFragment {
            return TestGuestFragment()
        }
    }
}
