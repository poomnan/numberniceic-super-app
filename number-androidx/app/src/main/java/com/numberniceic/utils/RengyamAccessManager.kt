package com.numberniceic.utils

import android.content.Context
import com.numberniceic.data.admin.RengyamAccess
import org.joda.time.DateTime

object RengyamAccessManager {
    private const val PREFS = "rengyam_usage_prefs"
    private const val KEY_GRANTED = "rengyam_access_granted"
    private const val KEY_EXPIRE_AT = "rengyam_access_expire_at"

    private fun grantedKey(userId: String): String = "${KEY_GRANTED}_$userId"
    private fun expireKey(userId: String): String = "${KEY_EXPIRE_AT}_$userId"

    fun persistFromServer(context: Context, userId: String?, access: RengyamAccess?) {
        val safeUserId = userId?.trim().orEmpty()
        if (safeUserId.isEmpty() || access == null || !access.granted) return

        val expireAtMillis = try {
            val expireAt = access.expireAt ?: return
            DateTime.parse(expireAt).plusDays(1).minusMillis(1).millis
        } catch (_: Exception) {
            return
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(grantedKey(safeUserId), true)
            .putLong(expireKey(safeUserId), expireAtMillis)
            .apply()
    }
}
