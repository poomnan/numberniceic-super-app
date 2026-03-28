package com.numberniceic.ui.auth


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonObject
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import com.numberniceic.R
import com.numberniceic.data.admin.Serverx
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.AppActivity
import com.numberniceic.utils.RengyamAccessManager
import retrofit2.Response
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class UserLoginF : androidx.fragment.app.Fragment() {

    private lateinit var edt_login_username: EditText
    private lateinit var edt_login_password: EditText
    private lateinit var progress_login: ProgressBar




    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {


        return inflater.inflate(R.layout.fragment_user_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edt_login_username = view.findViewById(R.id.edt_login_username)
        edt_login_password = view.findViewById(R.id.edt_login_password)
        progress_login = view.findViewById<ProgressBar>(R.id.progress_login)
        val btn_user_login = view.findViewById<Button>(R.id.btn_user_login)
        val btn_user_register = view.findViewById<Button>(R.id.btn_user_register)

        btn_user_login.setOnClickListener {

            if(completelyDataUser()){
                btn_user_login.isEnabled = false
                sendToDatabase()
            }
        }

        btn_user_register.setOnClickListener {
            val intent = Intent(context, UserRegisAct::class.java)
            startActivity(intent)
        }

    }

    private fun sendToDatabase() {
        val usernameStr = edt_login_username.text.toString()
        val passwordStr = edt_login_password.text.toString()
        
        val userdata = JsonObject()
        userdata.addProperty("username", usernameStr)
        userdata.addProperty("password", passwordStr)

        progress_login.visibility = View.VISIBLE
        val btnLogin = view?.findViewById<Button>(R.id.btn_user_login)
        
        Log.d("UserLoginF", "sendToDatabase: Requesting login for $usernameStr")

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiService = RetrofitClient.instance.create(ApiService::class.java)
                val response = apiService.userLogin(userdata).execute()

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    progress_login.visibility = View.GONE
                    btnLogin?.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val serverx = response.body()!!
                         if (serverx.serverx != null) {
                            if (serverx.serverx.message == "wrong") {
                                Toast.makeText(requireContext(), "User Name หรือ Password ไม่ถูกต้อง!!", Toast.LENGTH_LONG).show()
                            } else if (serverx.serverx.message == "success") {
                                handleLoginSuccess(serverx)
                            } else {
                                Toast.makeText(requireContext(), "Login Warning: Msg='${serverx.serverx.message}'", Toast.LENGTH_LONG).show()
                            }
                         } else {
                             Toast.makeText(requireContext(), "Server Error: serverx object is NULL", Toast.LENGTH_SHORT).show()
                         }
                    } else {
                        Toast.makeText(requireContext(), "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    progress_login.visibility = View.GONE
                    btnLogin?.isEnabled = true
                    Toast.makeText(context, "Connect Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("UserLoginF", "Error: ", e)
                }
            }
        }
    }

    private fun handleLoginSuccess(serverx: Serverx) {
         if (serverx.userx != null) {
            // val u = serverx.userx
            // Toast.makeText(requireContext(), "Login Debug: ID=${u.userId}, Name=${u.realName}, User=${u.username}", Toast.LENGTH_LONG).show()
            Toast.makeText(requireContext(), "ยินดีต้อนรับท่านได้เข้าสู่ระบบแล้ว", Toast.LENGTH_LONG).show()

            val userdataSharedf: SharedPreferences = requireContext().getSharedPreferences("userdata", Context.MODE_PRIVATE)
            val editorUserdata: SharedPreferences.Editor = userdataSharedf.edit()
            /* 
            // ⚠️ Removing this block as it can overwrite server-side "admin" status with local "normal" cache
            val codevip = userdataSharedf.getString("codevip", null)
            val userid = userdataSharedf.getString("userid", null)

            if (codevip != null && userid != null) {
                if (serverx.userx.userId == userid) {
                    serverx.userx.vipcode = codevip
                }
            }
            */

            val jsonToSave = Gson().toJson(serverx.userx)
            Log.d("UserLoginF", "Saving User JSON: $jsonToSave")
            editorUserdata.putString("json", jsonToSave)
            
            // Backup: Save individual fields explicitly to avoid JSON parsing issues
            editorUserdata.putString("saved_userid", serverx.userx.userId)
            editorUserdata.putString("saved_realname", serverx.userx.realName)
            editorUserdata.putString("saved_username", serverx.userx.username)
            editorUserdata.putString("vipcode", serverx.userx.vipcode)
            editorUserdata.putString("status", serverx.userx.status)
            
            editorUserdata.putBoolean("just_logged_in", true) 
            editorUserdata.commit() // Use commit for immediate availability

            RengyamAccessManager.persistFromServer(
                requireContext(),
                serverx.userx.userId,
                serverx.rengyamAccess
            )
            
            // 🚀 Trigger UI Refresh immediately after login before dismissal
            try {
                val refreshIntent = Intent("com.numberniceic.REFRESH_DASHBOARD")
                requireContext().sendBroadcast(refreshIntent)
                Log.d("UserLoginF", "Refresh broadcast sent")
            } catch (e: Exception) {
                Log.e("UserLoginF", "Error sending refresh broadcast", e)
            }

            // Clear chat session so a new one is created with the correct user name
            val chatPrefs = requireContext().getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
            chatPrefs.edit().remove("session_id").apply()
            Log.d("UserLoginF", "Chat session cleared for new login")
            
            // 🧹 Clear Person News Cache to ensure fresh data for new user
            com.numberniceic.utils.PersonNewsCacheManager.clearCache(requireContext())

            // Check for pending FCM token (Fire and forget in background)
            // Always check and update FCM token
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrEmpty() && serverx.userx.userId != null) {
                        try {
                            // Verify context is still valid before Toast
                            if (context != null) {
                                // Toast.makeText(requireContext(), "กำลังอัปเดต Token...", Toast.LENGTH_SHORT).show()
                            }
                            
                            val json = JsonObject()
                            json.addProperty("memberid", serverx.userx.userId)
                            json.addProperty("token", token)

                            val apiService = RetrofitClient.instance.create(ApiService::class.java)
                            apiService.updateFcmToken(json).enqueue(object : retrofit2.Callback<JsonObject> {
                                override fun onResponse(call: retrofit2.Call<JsonObject>, response: retrofit2.Response<JsonObject>) {
                                    if (isAdded) {
                                        Log.d("UserLoginF", "FCM Token update response: ${response.code()}")
                                    }
                                }
                                override fun onFailure(call: retrofit2.Call<JsonObject>, t: Throwable) {
                                    Log.e("UserLoginF", "Error updating FCM token", t)
                                     if (isAdded) Toast.makeText(requireContext(), "Update Token Fail: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            Log.d("UserLoginF", "Login SUCCESS. User: ${serverx.userx.userId}")

            val goHomeIntent = Intent(requireContext(), AppActivity::class.java).apply {
                putExtra("open_home_after_login", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(goHomeIntent)
            activity?.finish()
        } else {
            Toast.makeText(requireContext(), "User Name หรือ Password ไม่ถูกต้อง!!", Toast.LENGTH_LONG).show()
        }
    }

    private fun completelyDataUser() : Boolean {

        when {
            edt_login_username.text.toString().isEmpty() -> {
                Toast.makeText(context, "User Name ห้ามว่าง!!", Toast.LENGTH_SHORT).show()
                edt_login_username.requestFocus()
                return false
            }

            edt_login_password.text.toString().isEmpty() -> {
                Toast.makeText(context, "Password ห้ามว่าง!!", Toast.LENGTH_SHORT).show()
                edt_login_username.requestFocus()
                return false
            }

        }

        val username = edt_login_username.text.toString()

                for (s in username){
                    if (s.isWhitespace()){
                        Toast.makeText(context, "username มีวรรคตอน!!", Toast.LENGTH_SHORT).show()
                        edt_login_username.requestFocus()
                        return false
                    }

                }

        return true

    }


}
