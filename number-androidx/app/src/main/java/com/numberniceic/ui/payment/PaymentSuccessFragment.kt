package com.numberniceic.ui.payment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.data.admin.Userx
import com.numberniceic.ui.auth.GuestAddressDialog
import com.numberniceic.ui.auth.UserRegisF
import com.numberniceic.utils.GuestManager
import com.numberniceic.utils.UserContextManager

class PaymentSuccessFragment : Fragment() {
    
    private lateinit var guestManager: GuestManager
    private var paymentData: JsonObject? = null
    private var orderId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        guestManager = GuestManager(requireContext())
        
        // รับข้อมูลการชำระเงิน
        paymentData = arguments?.getSerializable("payment_data") as? JsonObject
        orderId = arguments?.getString("order_id")
        
        Log.d("PaymentSuccess", "Payment completed for order: $orderId")
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        handlePaymentSuccess()
    }
    
    private fun handlePaymentSuccess() {
        // บันทึก order ID ชั่วคราว
        orderId?.let { 
            guestManager.saveTemporaryOrderId(it)
            saveGuestOrderToServer(it)
        }
        
        // ตรวจสอบว่าเป็น guest หรือสมาชิก
        if (guestManager.isGuestUser()) {
            // แสดง dialog ให้กรอกที่อยู่
            showAddressDialog()
        } else {
            // สมาชิกแล้ว - ไปหน้าถัดไปได้เลย
            navigateToNextStep()
        }
    }
    
    private fun showAddressDialog() {
        val addressDialog = GuestAddressDialog()
        
        addressDialog.setOnAddressSaved {
            // บันทึกที่อยู่สำเร็จแล้ว
            Log.d("PaymentSuccess", "Address saved for guest")
            navigateToNextStep()
        }
        
        addressDialog.setOnSkip {
            // ข้ามการกรอกที่อยู่
            Log.d("PaymentSuccess", "User skipped address input")
            navigateToNextStep()
        }
        
        addressDialog.show(parentFragmentManager, "GuestAddressDialog")
    }
    
    private fun saveGuestOrderToServer(orderId: String) {
        val orderData = JsonObject().apply {
            addProperty("guest_id", guestManager.getGuestId())
            addProperty("order_id", orderId)
            addProperty("payment_data", paymentData?.toString())
            addProperty("created_at", System.currentTimeMillis())
        }
        
        // ส่งไปบันทึกที่ server
        com.numberniceic.https.RetrofitClient.api.saveGuestOrder(orderData)
            .enqueue(object : retrofit2.Callback<JsonObject> {
                override fun onResponse(
                    call: retrofit2.Call<JsonObject>,
                    response: retrofit2.Response<JsonObject>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val serverResponse = response.body()!!
                        if (serverResponse.get("success")?.asBoolean == true) {
                            Log.d("PaymentSuccess", "Order saved to server: $orderId")
                        }
                    }
                }
                
                override fun onFailure(call: retrofit2.Call<JsonObject>, t: Throwable) {
                    Log.e("PaymentSuccess", "Failed to save order to server", t)
                }
            })
    }
    
    private fun navigateToNextStep() {
        // ตรวจสอบว่าต้องการให้สมัครสมาชิกหรือไม่
        if (shouldShowRegistration()) {
            showRegistrationOption()
        } else {
            // ไปหน้าสรุปคำสั่งซื้อ
            navigateToOrderSummary()
        }
    }
    
    private fun shouldShowRegistration(): Boolean {
        // ตรวจสอบ logic ว่าควรให้สมัครสมาชิกหรือไม่
        // เช่น ถ้าซื้อสินค้าราคาสูง หรือมีข้อเสนอพิเศษ
        return true // ปรับตามความเหมาะสม
    }
    
    private fun showRegistrationOption() {
        // สร้าง dialog ถามว่าจะสมัครสมาชิกหรือไม่
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("สมัครสมาชิก?")
            .setMessage("สมัครสมาชิกเพื่อรับสิทธิประโยชน์พิเศษและติดตามสถานะการจัดส่ง")
            .setPositiveButton("สมัครเลย") { _, _ ->
                navigateToRegistration()
            }
            .setNegativeButton("ไม่เป็นไร") { _, _ ->
                navigateToOrderSummary()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun navigateToRegistration() {
        // ไปหน้าสมัครสมาชิก พร้อมส่งข้อมูล guest ไปด้วย
        val registrationFragment = UserRegisF()
        val bundle = Bundle()
        bundle.putBoolean("from_guest_payment", true)
        bundle.putString("guest_id", guestManager.getGuestId())
        bundle.putString("temp_address", guestManager.getTemporaryAddress())
        bundle.putString("temp_order_id", guestManager.getTemporaryOrderId())
        registrationFragment.arguments = bundle
        
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, registrationFragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToOrderSummary() {
        // ไปหน้าสรุปคำสั่งซื้อ
        // สามารถส่งข้อมูล guest ไปด้วยถ้าต้องการ
        Log.d("PaymentSuccess", "Navigating to order summary")
        // TODO: Implement navigation to order summary
    }
    
    /**
     * ฟังก์ชันสำหรับเรียกจากหน้าอื่น
     */
    companion object {
        fun newInstance(
            orderId: String,
            paymentData: JsonObject? = null
        ): PaymentSuccessFragment {
            val fragment = PaymentSuccessFragment()
            val args = Bundle()
            args.putString("order_id", orderId)
            args.putString("payment_data", paymentData?.toString())
            fragment.arguments = args
            return fragment
        }
    }
}
