package com.numberniceic.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.numberniceic.data.member.BagColorDaoCollection
import com.numberniceic.data.persons.DressColorCollection
import com.numberniceic.data.persons.MiracleDoV2
import com.numberniceic.data.persons.MiracleDo
import com.numberniceic.data.rengyam.LuckyNumber
import com.numberniceic.data.rengyam.WanpraDao

object PersonNewsCacheManager {
    private const val PREF_NAME = "person_news_cache"
    private const val KEY_LUCKY = "lucky_number"
    private const val KEY_WANPRA = "wan_pra"
    private const val KEY_BAG = "bag_color"
    private const val KEY_DRESS_D = "dress_d"
    private const val KEY_DRESS_R = "dress_r"
    private const val KEY_MIRA_SAPOM = "mira_sapom"
    private const val KEY_MIRA_NAIL = "mira_nail"
    private const val KEY_MIRA_HAIR = "mira_hair"
    private const val KEY_MIRA_PARMAI = "mira_parmai"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private val gson = Gson()
    
    // L1 Memory Cache
    private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, Any>()

    fun clearCache(context: Context) {
        memoryCache.clear()
        getPrefs(context).edit().clear().apply()
        android.util.Log.d("PersonNewsCache", "Cache Cleared")
    }

    // Generic helper to save object
    private fun <T> save(context: Context, key: String, data: T?) {
        if (data == null) return
        
        // Save to L1 (Memory)
        memoryCache[key] = data as Any
        
        // Save to L2 (Disk/Prefs)
        try {
            val json = gson.toJson(data)
            getPrefs(context).edit().putString(key, json).apply()
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Generic helper to load object
    private fun <T> load(context: Context, key: String, clazz: Class<T>): T? {
        // Try L1 (Memory)
        if (memoryCache.containsKey(key)) {
            try {
                return clazz.cast(memoryCache[key])
            } catch (e: Exception) {
               // Cast failed, fall through to disk
               e.printStackTrace()
            }
        }
    
        // Try L2 (Disk/Prefs)
        val json = getPrefs(context).getString(key, null) ?: return null
        return try {
            val obj = gson.fromJson(json, clazz)
            // Populate L1
            if (obj != null) memoryCache[key] = obj as Any
            obj
        } catch (e: Exception) {
            null
        }
    }

    // --- Lucky Number ---
    fun saveLuckyNumber(context: Context, data: LuckyNumber?) = save(context, KEY_LUCKY, data)
    fun loadLuckyNumber(context: Context): LuckyNumber? = load(context, KEY_LUCKY, LuckyNumber::class.java)

    // --- Wan Pra ---
    fun saveWanPra(context: Context, data: WanpraDao?) = save(context, KEY_WANPRA, data)
    fun loadWanPra(context: Context): WanpraDao? = load(context, KEY_WANPRA, WanpraDao::class.java)

    // --- Bag Color ---
    fun saveBagColor(context: Context, data: BagColorDaoCollection?) = save(context, KEY_BAG, data)
    fun loadBagColor(context: Context): BagColorDaoCollection? = load(context, KEY_BAG, BagColorDaoCollection::class.java)

    // --- Dress Color (D/R) ---
    fun saveDressD(context: Context, data: DressColorCollection?) = save(context, KEY_DRESS_D, data)
    fun loadDressD(context: Context): DressColorCollection? = load(context, KEY_DRESS_D, DressColorCollection::class.java)

    fun saveDressR(context: Context, data: DressColorCollection?) = save(context, KEY_DRESS_R, data)
    fun loadDressR(context: Context): DressColorCollection? = load(context, KEY_DRESS_R, DressColorCollection::class.java)

    // --- Miracles ---
    fun saveMiraSapom(context: Context, data: MiracleDoV2?) = save(context, KEY_MIRA_SAPOM, data)
    fun loadMiraSapom(context: Context): MiracleDoV2? = load(context, KEY_MIRA_SAPOM, MiracleDoV2::class.java)

    fun saveMiraNail(context: Context, data: MiracleDoV2?) = save(context, KEY_MIRA_NAIL, data)
    fun loadMiraNail(context: Context): MiracleDoV2? = load(context, KEY_MIRA_NAIL, MiracleDoV2::class.java)

    fun saveMiraHair(context: Context, data: MiracleDoV2?) = save(context, KEY_MIRA_HAIR, data)
    fun loadMiraHair(context: Context): MiracleDoV2? = load(context, KEY_MIRA_HAIR, MiracleDoV2::class.java)

    fun saveMiraParmai(context: Context, data: MiracleDo?) = save(context, KEY_MIRA_PARMAI, data)
    fun loadMiraParmai(context: Context): MiracleDo? = load(context, KEY_MIRA_PARMAI, MiracleDo::class.java)

    // --- News Articles ---
    private const val KEY_NEWS_PREFIX = "news_list_"
    fun saveNews(context: Context, type: String, data: com.numberniceic.data.news.NewsHeadlineCollection?) = save(context, KEY_NEWS_PREFIX + type, data)
    fun loadNews(context: Context, type: String): com.numberniceic.data.news.NewsHeadlineCollection? = load(context, KEY_NEWS_PREFIX + type, com.numberniceic.data.news.NewsHeadlineCollection::class.java)

    // --- News24 (Home Articles) ---
    private const val KEY_NEWS24 = "news_24_home"
    fun saveNews24(context: Context, data: com.numberniceic.data.news.News24?) = save(context, KEY_NEWS24, data)
    fun loadNews24(context: Context): com.numberniceic.data.news.News24? = load(context, KEY_NEWS24, com.numberniceic.data.news.News24::class.java)

    // --- Phone Sell VIP List ---
    private const val KEY_PHONE_SELL = "phone_sell_vip"
    fun savePhoneSell(context: Context, data: com.numberniceic.data.apicollectiondao.PhoneSellNumberCollectionDao?) = save(context, KEY_PHONE_SELL, data)
    fun loadPhoneSell(context: Context): com.numberniceic.data.apicollectiondao.PhoneSellNumberCollectionDao? = load(context, KEY_PHONE_SELL, com.numberniceic.data.apicollectiondao.PhoneSellNumberCollectionDao::class.java)

    // --- Assigned Buddha & Temple ---
    private const val KEY_ASSIGNED_BUDDHA = "assigned_buddha"
    private const val KEY_ASSIGNED_TEMPLE = "assigned_temple"
    fun saveAssignedBuddha(context: Context, data: com.google.gson.JsonObject?) = save(context, KEY_ASSIGNED_BUDDHA, data)
    fun loadAssignedBuddha(context: Context): com.google.gson.JsonObject? = load(context, KEY_ASSIGNED_BUDDHA, com.google.gson.JsonObject::class.java)
    fun saveAssignedTemple(context: Context, data: List<com.numberniceic.data.admin.SacredTemple>?) = save(context, KEY_ASSIGNED_TEMPLE, data)
    fun loadAssignedTemple(context: Context): List<com.numberniceic.data.admin.SacredTemple>? {
        val json = getPrefs(context).getString(KEY_ASSIGNED_TEMPLE, null) ?: return null
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<com.numberniceic.data.admin.SacredTemple>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { null }
    }

    // --- Assigned Inauspicious Data ---
    private const val KEY_ASSIGNED_INAUSPICIOUS = "assigned_inauspicious"
    fun saveAssignedInauspicious(context: Context, data: List<com.numberniceic.data.admin.InauspiciousData>?) = save(context, KEY_ASSIGNED_INAUSPICIOUS, data)
    fun loadAssignedInauspicious(context: Context): List<com.numberniceic.data.admin.InauspiciousData>? {
        val json = getPrefs(context).getString(KEY_ASSIGNED_INAUSPICIOUS, null) ?: return null
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<com.numberniceic.data.admin.InauspiciousData>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { null }
    }
}
