package com.numberniceic.ui.payment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.numberniceic.utils.GuestManager

class PaymentTestFragment : Fragment() {
    
    private lateinit var guestManager: GuestManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        guestManager = GuestManager(requireContext())
        
        // จำลองการชำระเงินสำเร็จ
        simulatePaymentSuccess()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // สร้าง view ว่างๆ เพื่อทดสอบ
        return View(requireContext())
    }
    
    private fun simulatePaymentSuccess() {
        Log.d("PaymentTest", "=== Simulating Payment Success ===")
        
        // จำลอง order ID
        val testOrderId = "TEST_ORDER_${System.currentTimeMillis()}"
        
        // บันทึก order ID ชั่วคราว
        guestManager.saveTemporaryOrderId(testOrderId)
        Log.d("PaymentTest", "Saved temporary order ID: $testOrderId")
        
        // ตรวจสอบว่าเป็น guest หรือสมาชิก
        val isGuest = guestManager.isGuestUser()
        val guestId = guestManager.getGuestId()
        
        Log.d("PaymentTest", "Is Guest: $isGuest")
        Log.d("PaymentTest", "Guest ID: $guestId")
        
        if (isGuest) {
            Log.d("PaymentTest", "Showing address dialog for guest...")
            showAddressDialog()
        } else {
            Log.d("PaymentTest", "User is already a member, skipping address...")
        }
    }
    
    private fun showAddressDialog() {
        val addressDialog = com.numberniceic.ui.auth.GuestAddressDialog()
        
        addressDialog.setOnAddressSaved {
            Log.d("PaymentTest", "✅ Address saved successfully in payment flow!")
        }
        
        addressDialog.setOnSkip {
            Log.d("PaymentTest", "⏭️ User skipped address in payment flow")
        }
        
        addressDialog.show(parentFragmentManager, "PaymentGuestAddressDialog")
        Log.d("PaymentTest", "Payment Guest Address Dialog shown")
    }
    
    companion object {
        fun newInstance(): PaymentTestFragment {
            return PaymentTestFragment()
        }
    }
}
