package com.numberniceic.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.numberniceic.data.admin.Userx

class AppContextManager {

    companion object {


        fun checkPermisNickname(sharedRef: SharedPreferences?, sharedRefUserdata: SharedPreferences?): Boolean {

            if (sharedRef != null) {
                val countNickname = sharedRef.getInt("countNickname", 0)
                if (countNickname > 5) {
                    val userJson = sharedRefUserdata!!.getString("json", null)
                    val userx = Gson().fromJson(userJson, Userx::class.java)

                    if (userx != null) {
                        return when (userx.vipcode) {
                            null -> true
                            "" -> true
                            "normal" -> true
                            "SpecialP" -> true
                            "silver" -> true
                            "gold" -> true
                            "diamond" -> true
                            "admin" -> true
                            else -> false
                        }
                    }

                    return false

                } else {
                    return true
                }
            }

            return false
        }

        fun checkPermisSurname(sharedRef: SharedPreferences?, sharedRefUserdata: SharedPreferences?): Boolean {

            if (sharedRef != null) {
                val countSurname = sharedRef.getInt("countSurname", 0)
                if (countSurname > 5) {
                    val userJson = sharedRefUserdata!!.getString("json", null)
                    val userx = Gson().fromJson(userJson, Userx::class.java)

                    if (userx != null) {
                        return when (userx.vipcode) {
                            null -> true
                            "" -> true
                            "normal" -> true
                            "SpecialP" -> true
                            "silver" -> true
                            "gold" -> true
                            "diamond" -> true
                            "admin" -> true
                            else -> false
                        }
                    }

                    return false

                } else {
                    return true
                }
            }

            return false
        }


        fun countNicknameCal(sharedRef: SharedPreferences?) {
            if (sharedRef != null) {
                val countNickname = sharedRef.getInt("countNickname", 0)
                val editor: SharedPreferences.Editor = sharedRef.edit()
                editor.putInt("countNickname", countNickname + 1)
                editor.apply()

            }
        }

        fun countSurnameCal(sharedRef: SharedPreferences?) {
            if (sharedRef != null) {
                val countSurname = sharedRef.getInt("countSurname", 0)
                val editor: SharedPreferences.Editor = sharedRef.edit()
                editor.putInt("countSurname", countSurname + 1)
                editor.apply()

            }
        }


        fun checkPermisTabianVip(sharedf: SharedPreferences?, sharedRefUserdata: SharedPreferences?): Boolean {

            if (sharedf != null) {
                val countTabian = sharedf.getInt("countTabian", 0)

                if (countTabian > 3) {
                    val userJson = sharedRefUserdata!!.getString("json", null)
                    val userx = Gson().fromJson(userJson, Userx::class.java)

                    if (userx != null) {
                        return when (userx.vipcode) {
                            "normal" -> false
                            "SpecialP" -> false
                            "silver" -> true
                            "gold" -> false
                            "diamond" -> true
                            "admin" -> true
                            else -> false
                        }
                    }

                    return false


                } else {
                    return true
                }
            }

            return false
        }

        fun countCalTatian(sharedf: SharedPreferences?) {
            if (sharedf != null) {
                val countTabian = sharedf.getInt("countTabian", 0)
                val editor: SharedPreferences.Editor = sharedf.edit()
                editor.putInt("countTabian", countTabian + 1)
                editor.apply()
            }
        }


        fun countPhoneCal(shared: SharedPreferences?) {
            if (shared != null) {
                val countPhone = shared.getInt("countPhone", 0)
                val editor: SharedPreferences.Editor = shared.edit()
                editor.putInt("countPhone", countPhone + 1)
                editor.apply()
            }
        }

        fun checkPermisPhoneVip(shared: SharedPreferences?, sharedRefUserdata: SharedPreferences?): Boolean {
            if (shared != null) {
                val countPhone = shared.getInt("countPhone", 0)
                if (countPhone > 5) {


                    val userJson = sharedRefUserdata!!.getString("json", null)
                    val userx = Gson().fromJson(userJson, Userx::class.java)

                    if (userx != null) {
                        return when (userx.vipcode) {
                            null -> true
                            "" -> true
                            "normal" -> true
                            "SpecialP" -> true
                            "silver" -> true
                            "gold" -> true
                            "diamond" -> true
                            "admin" -> true
                            else -> false
                        }
                    }

                    return false

                } else {
                    return true
                }

            }
            return false
        }


        fun countHomeCal(shared: SharedPreferences?) {
            if (shared != null) {
                val countHome = shared.getInt("countHome", 0)
                val editor: SharedPreferences.Editor = shared.edit()
                editor.putInt("countHome", countHome + 1)
                editor.apply()
            }
        }

        fun checkPermisHomeVip(shared: SharedPreferences?, sharedUser: SharedPreferences): Boolean {
            if (shared != null) {
                val countHome = shared.getInt("countHome", 0)
                if (countHome > 5) {

                    val userJson = sharedUser.getString("json", null)
                    val userx = Gson().fromJson(userJson, Userx::class.java)

                    if (userx != null) {
                        return when (userx.vipcode) {
                            null -> true
                            "" -> true
                            "normal" -> true
                            "SpecialP" -> true
                            "silver" -> true
                            "gold" -> true
                            "diamond" -> true
                            "admin" -> true
                            else -> false
                        }
                    }

                    return false
                } else {
                    return true
                }

            }
            return false
        }


        fun checkPermisListNickname(context: Context): Boolean {
            val userx = UserContextManager.userX(context)
            if (userx != null) {
                return when (userx.vipcode) {
                    "normal" -> false
                    "SpecialP" -> false
                    "silver" -> false
                    "gold" -> true
                    "diamond" -> true
                    "admin" -> true
                    else -> false
                }
            }

            return false
        }

        fun checkPermisAdminAddNickname(context: Context): Boolean {
            val userx = UserContextManager.userX(context)
            if (userx != null) {
                return when (userx.vipcode) {
                    "normal" -> false
                    "SpecialP" -> false
                    "silver" -> false
                    "gold" -> false
                    "diamond" -> false
                    "admin" -> true
                    "administrator" -> true
                    "admin7" -> true
                    else -> false
                }
            }

            return false
        }

        fun checkPermisListRealname(context: Context): Boolean {
            val userx = UserContextManager.userX(context)
            if (userx != null) {
                return when (userx.vipcode) {
                    "normal" -> false
                    "SpecialP" -> false
                    "silver" -> false
                    "gold" -> true
                    "diamond" -> true
                    "admin" -> true
                    else -> false
                }
            }

            return false
        }

        fun checkPermisAdminAddRealname(context: Context): Boolean {
            val userx = UserContextManager.userX(context)
            if (userx != null) {
                return when (userx.vipcode) {
                    "normal" -> false
                    "SpecialP" -> false
                    "silver" -> false
                    "gold" -> false
                    "diamond" -> false
                    "admin" -> true
                    "administrator" -> true
                    "admin7" -> true
                    else -> false
                }
            }

            return false
        }


    }
}