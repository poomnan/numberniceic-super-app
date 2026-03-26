package com.numberniceic.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class GuestManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("GUEST_SESSION", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val TAG = "GuestManager"
        private const val KEY_GUEST_ID = "guest_id"
        private const val KEY_SERVER_GUEST_ID = "server_guest_id"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_TEMP_ADDRESS = "temp_address"
        private const val KEY_TEMP_ORDER_ID = "temp_order_id"
    }
    
    /**
     * เรียกครั้งแรกตอนติดตั้งแอป
     */
    fun initializeGuestIfNeeded(): String {
        val existingGuestId = prefs.getString(KEY_GUEST_ID, null)
        
        if (existingGuestId != null) {
            Log.d(TAG, "Guest ID already exists: $existingGuestId")
            return existingGuestId
        }
        
        val newGuestId = createGuestId()
        
        // บันทึกลง local
        prefs.edit()
            .putString(KEY_GUEST_ID, newGuestId)
            .putLong(KEY_CREATED_AT, System.currentTimeMillis())
            .putBoolean(KEY_IS_REGISTERED, false)
            .apply()
        
        Log.d(TAG, "Created new Guest ID: $newGuestId")
        
        // ส่งไปบันทึกที่ server แบบ async
        registerGuestToServer(newGuestId)
        
        return newGuestId
    }
    
    /**
     * สร้าง Guest ID ใหม่
     */
    private fun createGuestId(): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: UUID.randomUUID().toString()
        
        return "guest_${androidId}_${System.currentTimeMillis()}"
    }
    
    /**
     * ดึง Guest ID ปัจจุบัน
     */
    fun getGuestId(): String? {
        return prefs.getString(KEY_GUEST_ID, null)
    }
    
    /**
     * ตรวจสอบว่าเป็น guest หรือไม่
     */
    fun isGuestUser(): Boolean {
        return !prefs.getBoolean(KEY_IS_REGISTERED, false) && getGuestId() != null
    }
    
    /**
     * บันทึกข้อมูล guest ไป server
     */
    private fun registerGuestToServer(guestId: String) {
        try {
            Log.d(TAG, "Registering guest to server: $guestId")
            
            val guestData = JsonObject().apply {
                addProperty("guest_id", guestId)
                addProperty("android_id", getAndroidId())
                addProperty("device_info", getDeviceInfo())
                addProperty("app_version", com.numberniceic.BuildConfig.VERSION_NAME)
                addProperty("created_at", System.currentTimeMillis())
            }
            
            Log.d(TAG, "Guest data: $guestData")
            
            RetrofitClient.api.registerGuest(guestData).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    Log.d(TAG, "Register response code: ${response.code()}")
                    Log.d(TAG, "Register response body: ${response.body()}")
                    
                    if (response.isSuccessful && response.body() != null) {
                        val serverResponse = response.body()!!
                        if (serverResponse.get("success")?.asBoolean == true) {
                            val serverGuestId = serverResponse.get("server_guest_id")?.asString
                            if (serverGuestId != null) {
                                prefs.edit()
                                    .putString(KEY_SERVER_GUEST_ID, serverGuestId)
                                    .apply()
                                Log.d(TAG, "Guest registered to server: $serverGuestId")
                            }
                        } else {
                            Log.e(TAG, "Guest registration failed: ${serverResponse.get("message")}")
                        }
                    } else {
                        Log.e(TAG, "Guest registration HTTP error: ${response.code()}")
                    }
                }
                
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Log.e(TAG, "Failed to register guest to server", t)
                    Log.e(TAG, "Network error: ${t.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error registering guest", e)
        }
    }
    
    /**
     * บันทึกที่อยู่ชั่วคราวสำหรับ guest
     */
    fun saveTemporaryAddress(address: String) {
        prefs.edit()
            .putString(KEY_TEMP_ADDRESS, address)
            .apply()
        Log.d(TAG, "Saved temporary address: $address")
    }
    
    /**
     * ดึงที่อยู่ชั่วคราว
     */
    fun getTemporaryAddress(): String? {
        return prefs.getString(KEY_TEMP_ADDRESS, null)
    }
    
    /**
     * บันทึก order ID ชั่วคราว
     */
    fun saveTemporaryOrderId(orderId: String) {
        prefs.edit()
            .putString(KEY_TEMP_ORDER_ID, orderId)
            .apply()
        Log.d(TAG, "Saved temporary order ID: $orderId")
    }
    
    /**
     * ดึง order ID ชั่วคราว
     */
    fun getTemporaryOrderId(): String? {
        return prefs.getString(KEY_TEMP_ORDER_ID, null)
    }
    
    /**
     * อัปเกรดจาก guest เป็นสมาชิก
     */
    fun upgradeToMember(memberData: JsonObject) {
        val guestId = getGuestId()
        if (guestId != null) {
            memberData.addProperty("guest_id", guestId)
            memberData.addProperty("temp_address", getTemporaryAddress() ?: "")
            memberData.addProperty("temp_order_id", getTemporaryOrderId() ?: "")
        }
        
        Log.d(TAG, "Upgrading guest to member with data: ${memberData.toString()}")
    }
    
    /**
     * ล้างข้อมูล guest หลังสมัครสมาชิกสำเร็จ
     */
    fun clearGuestData() {
        prefs.edit()
            .remove(KEY_GUEST_ID)
            .remove(KEY_SERVER_GUEST_ID)
            .remove(KEY_TEMP_ADDRESS)
            .remove(KEY_TEMP_ORDER_ID)
            .putBoolean(KEY_IS_REGISTERED, true)
            .apply()
        Log.d(TAG, "Guest data cleared after registration")
    }
    
    /**
     * ดึง Android ID
     */
    private fun getAndroidId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
    
    /**
     * ดึงข้อมูลอุปกรณ์
     */
    private fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
    }
    
    /**
     * ดึงข้อมูล guest ทั้งหมด (สำหรับ debug)
     */
    fun getGuestInfo(): String {
        return """
            Guest ID: ${getGuestId()}
            Server Guest ID: ${prefs.getString(KEY_SERVER_GUEST_ID, "null")}
            Is Registered: ${prefs.getBoolean(KEY_IS_REGISTERED, false)}
            Created At: ${prefs.getLong(KEY_CREATED_AT, 0)}
            Temporary Address: ${getTemporaryAddress()}
            Temporary Order ID: ${getTemporaryOrderId()}
        """.trimIndent()
    }
}
