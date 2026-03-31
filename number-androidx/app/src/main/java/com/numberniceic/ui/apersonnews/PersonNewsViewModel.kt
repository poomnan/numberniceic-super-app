package com.numberniceic.ui.apersonnews

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.numberniceic.data.member.BagColorDaoCollection
import com.numberniceic.data.persons.DressColor
import com.numberniceic.data.persons.DressColorCollection
import com.numberniceic.data.persons.MiracleDo
import com.numberniceic.data.persons.MiracleDoV2
import com.numberniceic.data.persons.OutfitMiracleColorSetItem
import com.numberniceic.data.persons.OutfitMiracleColorSetsRequest
import com.numberniceic.data.persons.OutfitMiracleColorSetsResponse
import com.numberniceic.data.rengyam.LuckyNumber
import com.numberniceic.data.rengyam.WanpraDao
import com.numberniceic.data.admin.BuddhaPang
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.utils.PersonContextManager
import com.numberniceic.utils.UserContextManager
import com.numberniceic.utils.PersonNewsCacheManager
import com.numberniceic.utils.OutfitApiColorMappingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import retrofit2.Response

class PersonNewsViewModel(application: Application) : AndroidViewModel(application) {

    private fun <T> logHttpFailure(tag: String, response: Response<T>) {
        Log.w(
            "PersonNewsVM",
            "$tag failed code=${response.code()} message=${response.message()} errorBody=${try { response.errorBody()?.string() } catch (_: Exception) { null }}"
        )
    }

    val luckyNumber = MutableLiveData<LuckyNumber?>()
    val wanPra = MutableLiveData<WanpraDao?>()
    val bagColorCollection = MutableLiveData<BagColorDaoCollection?>()
    
    // Dress Colors
    val dressColorD = MutableLiveData<DressColorCollection?>()
    val dressColorR = MutableLiveData<DressColorCollection?>()
    val outfitApiAuspiciousThai = MutableLiveData<String?>()
    val outfitApiInauspiciousThai = MutableLiveData<String?>()
    val outfitApiReferenceThai = MutableLiveData<String?>()
    
    // Miracle Do
    val miracleSapom = MutableLiveData<MiracleDoV2?>()
    val miracleCutNail = MutableLiveData<MiracleDoV2?>()
    val miracleCutHair = MutableLiveData<MiracleDoV2?>()
    val miracleParmai = MutableLiveData<MiracleDo?>()
    val assignedBuddhaPang = MutableLiveData<com.google.gson.JsonObject?>()
    val assignedSacredTemple = MutableLiveData<List<com.numberniceic.data.admin.SacredTemple>?>()
    val meritNotifications = MutableLiveData<List<com.numberniceic.data.notification.NotificationResponse>>()
    val changeNotifications = MutableLiveData<List<com.numberniceic.data.notification.NotificationResponse>>()
    val spellNotifications = MutableLiveData<List<com.numberniceic.data.notification.NotificationResponse>>()
    val assignedInauspicious = MutableLiveData<List<com.numberniceic.data.admin.InauspiciousData>?>()
    val assignedAuspicious = MutableLiveData<List<com.numberniceic.data.admin.InauspiciousData>?>()

    val apiStatusMessage = MutableLiveData<String?>()

    val isLoading = MutableLiveData<Boolean>()

    private var lastLoadAllDataAtMs: Long = 0L
    private val loadAllDataThrottleMs: Long = 3_000L

    fun loadAllData(context: Context, forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && (now - lastLoadAllDataAtMs) < loadAllDataThrottleMs) {
            Log.d(
                "PersonNewsVM",
                "loadAllData throttled deltaMs=${now - lastLoadAllDataAtMs} forceRefresh=$forceRefresh"
            )
            return
        }
        lastLoadAllDataAtMs = now

        apiStatusMessage.value = null

        isLoading.value = true
        val userx = UserContextManager.userX(context)
        Log.d("PersonNewsVM", "loadAllData forceRefresh=$forceRefresh userId=${userx?.userId} vip=${userx?.vipcode}")

        if (forceRefresh) {
            // We no longer clear LiveDatas here to avoid flickers or empty states
            // if the network is slow or failing. The new data will replace 
            // the old data once the network call completes successfully.
        }

        viewModelScope.launch(Dispatchers.IO) {
            val api = RetrofitClient.instance.create(ApiService::class.java)
            val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

            // 1. Independent Requests (Async)
            val luckyJob = async { try { api.getLuckyNumber().execute().body() } catch (e: Exception) { null } }
            val wanPraJob = async { try { api.getWanPra(currentDateStr).execute().body() } catch (e: Exception) { null } }

            // 2. User Dependent Requests
            var bagJob: kotlinx.coroutines.Deferred<BagColorDaoCollection?>? = null
            var dressDJob: kotlinx.coroutines.Deferred<DressColorCollection?>? = null
            var dressRJob: kotlinx.coroutines.Deferred<DressColorCollection?>? = null
            // var bagJob: kotlinx.coroutines.Deferred<BagColorDaoCollection?>? = null // Removed
            // var dressDJob: kotlinx.coroutines.Deferred<DressColorCollection?>? = null // Removed
            // var dressRJob: kotlinx.coroutines.Deferred<DressColorCollection?>? = null // Removed
            
            var miraSapomJob: kotlinx.coroutines.Deferred<MiracleDoV2?>? = null
            var miraNailJob: kotlinx.coroutines.Deferred<MiracleDoV2?>? = null
            var miraHairJob: kotlinx.coroutines.Deferred<MiracleDoV2?>? = null
            var miraParmaiJob: kotlinx.coroutines.Deferred<MiracleDo?>? = null

            // Progressive Loading: Launch separate coroutines for each data source
            // This ensures that slow APIs do not block fast APIs from updating the UI.

            val context = getApplication<android.app.Application>()

            // 1. Lucky Number
            viewModelScope.launch(Dispatchers.IO) {
                // Cache First (Skip if forceRefresh)
                if (!forceRefresh) {
                    try {
                        val cached = PersonNewsCacheManager.loadLuckyNumber(context)
                        if (cached != null) luckyNumber.postValue(cached)
                    } catch (e: Exception) { e.printStackTrace() }
                }

                // Network
                try {
                    val response = api.getLuckyNumber().execute()
                    if (response.isSuccessful) {
                        val body = response.body()
                        luckyNumber.postValue(body)
                        PersonNewsCacheManager.saveLuckyNumber(context, body)
                    } else {
                        logHttpFailure("getLuckyNumber", response)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            // 2. Wan Pra
            viewModelScope.launch(Dispatchers.IO) {
                // Cache First
                try {
                    val cached = PersonNewsCacheManager.loadWanPra(context)
                    if (cached != null) wanPra.postValue(cached)
                } catch (e: Exception) { e.printStackTrace() }

                // Network
                try {
                    val dt = DateTime.now()
                    val wanDate = "${dt.dayOfMonth}-${dt.monthOfYear}-${dt.year + 543}"
                    val response = api.getWanPra(wanDate).execute()
                    if (response.isSuccessful) {
                        val body = response.body()
                        wanPra.postValue(body)
                        PersonNewsCacheManager.saveWanPra(context, body)
                    } else {
                        logHttpFailure("getWanPra($wanDate)", response)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            // Calculate Age & Birthday (Local Logic)
            val birthday: DateTime? = PersonContextManager.parseBirthdayOrNull(userx?.birthDay)
            var ageCurrent = 0
            var ageYang = 0
            var dayBirthEng = ""
            var dayBirthNum = 0

            if (birthday != null) {
                ageCurrent = PersonContextManager.ageCurrent(birthday.year, birthday.monthOfYear, birthday.dayOfMonth) ?: 0
                ageYang = PersonContextManager.ageYang(birthday.year, birthday.monthOfYear, birthday.dayOfMonth) ?: 0
                val d = birthday.dayOfWeek()
                val dayBirth = d.getAsText(Locale.ENGLISH)
                val dayNumBirthRaw = PersonContextManager.convertDayEngToNum(dayBirth)
                
                dayBirthNum = if ((userx?.sHour ?: 0) <= 4) {
                    if (dayNumBirthRaw == 1) 7 else dayNumBirthRaw - 1
                } else {
                    dayNumBirthRaw
                }
                dayBirthEng = PersonContextManager.convertDayNumToEng(dayBirthNum) ?: "Sunday"
            } else if (userx?.vipcode?.lowercase()?.contains("admin") == true) {
                // Fallback for Admin: Default to Sunday to ensure dress colors display
                dayBirthEng = "Sunday"
                dayBirthNum = 1
                ageYang = 45
            }

            // 3. Bag Color
            if (ageCurrent > 0 && userx?.userId != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    // Cache First: Always show something immediately, then update from network
                    try {
                        val cached = PersonNewsCacheManager.loadBagColor(context)
                        if (cached != null) bagColorCollection.postValue(cached)
                    } catch (e: Exception) { e.printStackTrace() }

                    // Network
                    try {
                        val response = api.getBagColor(userx.userId!!, ageCurrent.toString(), (ageCurrent + 1).toString()).execute()
                        if (response.isSuccessful) {
                            val body = response.body()
                            bagColorCollection.postValue(body)
                            PersonNewsCacheManager.saveBagColor(context, body)
                        } else {
                            logHttpFailure("getBagColor", response)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }

            // 3.5 Assigned Buddha Pang & Sacred Temple
            if (userx?.userId != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    // Cache First
                    try {
                        val cached = PersonNewsCacheManager.loadAssignedBuddha(context)
                        if (cached != null) assignedBuddhaPang.postValue(cached)
                    } catch (e: Exception) { e.printStackTrace() }

                    try {
                        val response = api.getAssignedBuddhaPang(userx.userId!!).execute()
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()
                            assignedBuddhaPang.postValue(body)
                            PersonNewsCacheManager.saveAssignedBuddha(context, body)
                        } else {
                            if (!response.isSuccessful) {
                                logHttpFailure("getAssignedBuddhaPang", response)
                            }
                            assignedBuddhaPang.postValue(null)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        assignedBuddhaPang.postValue(null)
                    }
                }
                
                viewModelScope.launch(Dispatchers.IO) {
                    // Cache First
                    try {
                        val cached = PersonNewsCacheManager.loadAssignedTemple(context)
                        if (cached != null) assignedSacredTemple.postValue(cached)
                    } catch (e: Exception) { e.printStackTrace() }

                    try {
                        val response = api.getAssignedSacredTemple(userx.userId!!).execute()
                        var temples = if (response.isSuccessful) response.body() else null

                        if (!response.isSuccessful) {
                            logHttpFailure("getAssignedSacredTemple", response)
                        }
                        
                        // If no specific assigned temple, fetch latest one from system
                        if (temples.isNullOrEmpty()) {
                            val allTemplesResponse = api.getSacredTemples().execute()
                            if (!allTemplesResponse.isSuccessful) {
                                logHttpFailure("getSacredTemples", allTemplesResponse)
                            }
                            if (allTemplesResponse.isSuccessful && !allTemplesResponse.body().isNullOrEmpty()) {
                                val latestTemple = allTemplesResponse.body()!!.last()
                                // Wrap it in a list
                                temples = listOf(latestTemple)
                            }
                        }
                        
                        if (!temples.isNullOrEmpty()) {
                             assignedSacredTemple.postValue(temples)
                             PersonNewsCacheManager.saveAssignedTemple(context, temples)
                        } else {
                             assignedSacredTemple.postValue(emptyList())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Don't clear valid cache on network error if possible, but for simplicity here we assume if failing we might show nothing or keep old value? 
                        // LiveData isn't cleared automatically, so just logging error.
                    }
                }
            }

            // 4. Dress Color
            if (dayBirthEng.isNotEmpty() && ageYang > 0) {
                viewModelScope.launch(Dispatchers.IO) {
                    // Cache First
                    try {
                        val cachedD = PersonNewsCacheManager.loadDressD(context)
                        if (cachedD != null) dressColorD.postValue(cachedD)
                        val cachedR = PersonNewsCacheManager.loadDressR(context)
                        if (cachedR != null) dressColorR.postValue(cachedR)
                    } catch (e: Exception) { e.printStackTrace() }

                // Network / Calculation
                val currentDayEngx = ViewModelHelper.resolveOutfitCurrentDayEng(DateTime())
                try {
                    val reqBody = OutfitMiracleColorSetsRequest(
                        currentDayName = ViewModelHelper.dayEngToOutfitDayName(currentDayEngx),
                        birthDayName = ViewModelHelper.dayEngToOutfitDayName(dayBirthEng),
                            ageYears = ageYang
                        )

                        OutfitApiColorMappingManager.syncDayPalettesFromServer(context)
                        val response = api.getOutfitMiracleColorSets(reqBody).execute()
                        if (response.isSuccessful && response.body() != null) {
                            val outfitResponse = response.body()
                            
                            val inauspiciousSets = outfitResponse?.inauspiciousSets ?: emptyList()
                            val inauspiciousNumbers = inauspiciousSets.mapNotNull { it.number }.toSet()
                            
                            var effectiveAuspicious = ViewModelHelper.resolveAuspiciousSetsWithKaliCheck(
                                outfitResponse?.auspiciousSets,
                                inauspiciousNumbers
                            )
                            if (effectiveAuspicious.isNullOrEmpty()) {
                                effectiveAuspicious = ViewModelHelper.fallbackAuspiciousFromLegacy(
                                    currentDayEng = currentDayEngx,
                                    dayBirthEng = dayBirthEng,
                                    dayBirthNum = dayBirthNum,
                                    context = context
                                )
                            }
                            
                            val dCollection = ViewModelHelper.toDressColorCollection(
                                effectiveAuspicious,
                                context,
                                ViewModelHelper.DressDisplayMode.AUSPICIOUS
                            )
                            val rCollection = ViewModelHelper.toDressColorCollection(
                                inauspiciousSets,
                                context,
                                ViewModelHelper.DressDisplayMode.INAUSPICIOUS
                            )
                            dressColorD.postValue(dCollection)
                            dressColorR.postValue(rCollection)
                            outfitApiAuspiciousThai.postValue(ViewModelHelper.formatThaiAuspiciousOrdered(effectiveAuspicious))
                            outfitApiInauspiciousThai.postValue(ViewModelHelper.formatThaiInauspiciousOrdered(inauspiciousSets))
                            outfitApiReferenceThai.postValue(ViewModelHelper.formatOutfitReference(outfitResponse))
                            PersonNewsCacheManager.saveDressD(context, dCollection)
                            PersonNewsCacheManager.saveDressR(context, rCollection)
                        } else {
                            logHttpFailure("getOutfitMiracleColorSets", response)
                            val fallbackD = ViewModelHelper.fallbackDressCollectionFromLegacy(
                                currentDayEng = currentDayEngx,
                                dayBirthEng = dayBirthEng,
                                dayBirthNum = dayBirthNum,
                                type = "d",
                                context = context
                            )
                            val fallbackR = ViewModelHelper.fallbackDressCollectionFromLegacy(
                                currentDayEng = currentDayEngx,
                                dayBirthEng = dayBirthEng,
                                dayBirthNum = dayBirthNum,
                                type = "r",
                                context = context
                            )
                            dressColorD.postValue(fallbackD)
                            dressColorR.postValue(fallbackR)
                            PersonNewsCacheManager.saveDressD(context, fallbackD)
                            PersonNewsCacheManager.saveDressR(context, fallbackR)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val fallbackD = ViewModelHelper.fallbackDressCollectionFromLegacy(
                            currentDayEng = currentDayEngx,
                            dayBirthEng = dayBirthEng,
                            dayBirthNum = dayBirthNum,
                            type = "d",
                            context = context
                        )
                        val fallbackR = ViewModelHelper.fallbackDressCollectionFromLegacy(
                            currentDayEng = currentDayEngx,
                            dayBirthEng = dayBirthEng,
                            dayBirthNum = dayBirthNum,
                            type = "r",
                            context = context
                        )
                        dressColorD.postValue(fallbackD)
                        dressColorR.postValue(fallbackR)
                        PersonNewsCacheManager.saveDressD(context, fallbackD)
                        PersonNewsCacheManager.saveDressR(context, fallbackR)
                    }
                }
            }

            // 5. Miracles (Heavy APIs)
            if (dayBirthNum > 0) {
                // Cache First (All Miracles)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val cSapom = PersonNewsCacheManager.loadMiraSapom(context)
                        if (cSapom != null) miracleSapom.postValue(cSapom)
                        val cNail = PersonNewsCacheManager.loadMiraNail(context)
                        if (cNail != null) miracleCutNail.postValue(cNail)
                        val cHair = PersonNewsCacheManager.loadMiraHair(context)
                        if (cHair != null) miracleCutHair.postValue(cHair)
                        val cParmai = PersonNewsCacheManager.loadMiraParmai(context)
                        if (cParmai != null) miracleParmai.postValue(cParmai)
                    } catch (e: Exception) { e.printStackTrace() }
                }

                val dt = DateTime.now()
                val currentDateStr = "${dt.dayOfMonth}-${dt.monthOfYear}-${dt.year + 543}"
                val currentDayEngV2 = PersonContextManager.convertDayNumToEngV2(dt.dayOfWeek().asString.toInt())?.lowercase() ?: "sunday"

                // Network Requests (Parallel)
                // Mira Do V2 (General)
                viewModelScope.launch(Dispatchers.IO) {
                    try { 
                        val response = api.getMiraDoV2("สระผม", dayBirthEng, currentDayEngV2, currentDateStr).execute()
                        if (response.isSuccessful) {
                            val body = response.body()
                            miracleSapom.postValue(body)
                            PersonNewsCacheManager.saveMiraSapom(context, body)
                        } else {
                            logHttpFailure("getMiraDoV2(สระผม)", response)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                viewModelScope.launch(Dispatchers.IO) {
                    try { 
                        val response = api.getMiraDoV2("ตัดเล็บ", dayBirthEng, currentDayEngV2, currentDateStr).execute()
                        if (response.isSuccessful) {
                            val body = response.body()
                            miracleCutNail.postValue(body)
                            PersonNewsCacheManager.saveMiraNail(context, body)
                        } else {
                            logHttpFailure("getMiraDoV2(ตัดเล็บ)", response)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                
                // Mira Do V2 (Hair)
                viewModelScope.launch(Dispatchers.IO) {
                    try { 
                        val response = api.getMiraDoV2("ตัดผม", dayBirthEng, currentDayEngV2, currentDateStr).execute()
                        if (response.isSuccessful) {
                            val body = response.body()
                            miracleCutHair.postValue(body)
                            PersonNewsCacheManager.saveMiraHair(context, body)
                        } else {
                            logHttpFailure("getMiraDoV2(ตัดผม)", response)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                
                // Mira Do V1 (Parmai - New Cloth)
                viewModelScope.launch(Dispatchers.IO) {
                    try { 
                        val response = api.getMiraDo("ผ้าใหม่", dayBirthEng, currentDayEngV2).execute()
                        if (response.isSuccessful) {
                            val body = response.body()
                            miracleParmai.postValue(body)
                            PersonNewsCacheManager.saveMiraParmai(context, body)
                        } else {
                            logHttpFailure("getMiraDo(ผ้าใหม่)", response)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }

            // 6. Fetch Assigned Merit, Change, Spells (Direct Table Source)
            if (userx?.userId != null) {
                // Merit
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val response = api.getAssignedMerit(userx.userId!!, "merit").execute()
                        if (response.isSuccessful) {
                            meritNotifications.postValue(response.body() ?: emptyList())
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                // Change Number
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val response = api.getAssignedMerit(userx.userId!!, "changenum").execute()
                        if (response.isSuccessful) {
                            changeNotifications.postValue(response.body() ?: emptyList())
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                
                // Spells
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val response = api.getAssignedMerit(userx.userId!!, "spell").execute()
                        if (response.isSuccessful) {
                            spellNotifications.postValue(response.body() ?: emptyList())
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                // Inauspicious Data
                viewModelScope.launch(Dispatchers.IO) {
                    // Cache First: Always show something immediately
                    try {
                        val cached = PersonNewsCacheManager.loadAssignedInauspicious(context)
                        if (cached != null && cached.isNotEmpty()) {
                            assignedInauspicious.postValue(cached)
                        }
                    } catch (e: Exception) { e.printStackTrace() }

                    // Network
                    try {
                        val response = api.getAssignedInauspicious(userx.userId!!).execute()
                        if (response.isSuccessful) {
                            val list = response.body()
                            
                            if (!list.isNullOrEmpty()) {
                                assignedInauspicious.postValue(list)
                                PersonNewsCacheManager.saveAssignedInauspicious(context, list)
                            } else {
                                val currentCache = PersonNewsCacheManager.loadAssignedInauspicious(context)
                                if (currentCache.isNullOrEmpty()) {
                                    assignedInauspicious.postValue(emptyList())
                                } else {
                                    Log.w("PersonNewsVM", "Server returned empty Inauspicious data, but keeping Cache to prevent 1-min bug.")
                                }
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                // Auspicious Data
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val response = api.getAssignedAuspicious(userx.userId!!).execute()
                        if (response.isSuccessful) {
                            val list = response.body()
                            if (!list.isNullOrEmpty()) {
                                assignedAuspicious.postValue(list)
                            } else {
                                assignedAuspicious.postValue(emptyList())
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            
            withContext(Dispatchers.Main) {
                isLoading.value = false
            }

            // --- PREFETCH NEWS IN BACKGROUND ---
            // While the user is enjoying the fast Dashboard, we silently load the News for them.
            // This ensures that when they click the "News" tab, it will be instant (Cache Hit).
            // --- PREFETCH NEWS IN BACKGROUND (Optimized Parallel) ---
            // While the user is enjoying the fast Dashboard, we silently load ALL News types for them.
            // --- PREFETCH NEWS IN BACKGROUND (Sequential & Safe) ---
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val apiService = RetrofitClient.instance.create(ApiService::class.java)
                    val newsTypes = listOf("1", "2", "3") // Reduced scope for stability
                    
                    for (type in newsTypes) {
                        try {
                            val response = apiService.getNewsAllByType(type).execute()
                            if (response.isSuccessful && response.body() != null) {
                                PersonNewsCacheManager.saveNews(context, type, response.body())
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }
}

// Helper to access ClothColor3dModel logic without Fragment instance (assuming logic is static or context-based)
// But ClothColor3dModel is a ViewModel... we need to instantiate it or copy logic.
// For now, I will assume Client has 'ViewModelHelper' or I'll copy 'getColorCloth' logic if possible.
// Wait, 'clothColorModel' inside Fragment is a ViewModel. 
object ViewModelHelper {
    enum class DressDisplayMode {
        AUSPICIOUS,
        INAUSPICIOUS
    }
    fun resolveOutfitCurrentDayEng(dateTime: DateTime): String {
        var currentDayEng = dateTime.dayOfWeek().getAsText(java.util.Locale.ENGLISH)
        if (currentDayEng == "Wednesday" && dateTime.hourOfDay >= 18) {
            currentDayEng = "Wednesday2"
        } else if (currentDayEng == "Thursday" && dateTime.hourOfDay < 6) {
            currentDayEng = "Wednesday2"
        }
        return currentDayEng
    }

    fun getColorCloth(currentDay: String, dayBirth: String, dayBirthNum: Int, type: String, context: Context): String? {
         val model = Clothcolor3dModel() 
         val sb = model.getColorCloth(currentDay, dayBirth, dayBirthNum, type, context)
         return sb?.toString()
    }

    private fun formatThaiColorSetLine(index: Int, item: OutfitMiracleColorSetItem?): String {
        if (item == null) {
            return "$index) ไม่มีข้อมูล"
        }
        val label = item.label ?: "-"
        val number = item.number?.toString() ?: "-"
        val day = item.day ?: "-"
        val colors = if (item.colorGroup.isNullOrEmpty()) "-" else item.colorGroup.joinToString(", ")
        return "$index) $label (เลข $number $day) = $colors"
    }

    fun formatThaiAuspiciousOrdered(items: List<OutfitMiracleColorSetItem>?): String {
        val list = items ?: emptyList()
        val line1 = formatThaiColorSetLine(1, list.getOrNull(0))
        val line2 = formatThaiColorSetLine(2, list.getOrNull(1))
        val line3 = formatThaiColorSetLine(3, list.getOrNull(2))
        return "API มงคล (เรียงลำดับ):\n$line1\n$line2\n$line3"
    }

    fun formatThaiInauspiciousOrdered(items: List<OutfitMiracleColorSetItem>?): String {
        val list = items ?: emptyList()
        val line1 = formatThaiColorSetLine(1, list.getOrNull(0))
        val line2 = formatThaiColorSetLine(2, list.getOrNull(1))
        val line3 = formatThaiColorSetLine(3, list.getOrNull(2))
        return "API อัปมงคล (เรียงลำดับ):\n$line1\n$line2\n$line3"
    }

    fun dayEngToOutfitNumber(dayEng: String): String {
        return when (dayEng) {
            "Sunday" -> "1"
            "Monday" -> "2"
            "Tuesday" -> "3"
            "Wednesday" -> "4"
            "Thursday" -> "5"
            "Friday" -> "6"
            "Saturday" -> "7"
            "Wednesday2" -> "8"
            else -> "1"
        }
    }

    fun dayEngToOutfitDayName(dayEng: String): String {
        return when (dayEng) {
            "Sunday" -> "อาทิตย์"
            "Monday" -> "จันทร์"
            "Tuesday" -> "อังคาร"
            "Wednesday" -> "พุธ"
            "Thursday" -> "พฤหัสบดี"
            "Friday" -> "ศุกร์"
            "Saturday" -> "เสาร์"
            "Wednesday2" -> "พุธ กลางคืน"
            else -> "อาทิตย์"
        }
    }

    fun toDressColorCollection(
        items: List<OutfitMiracleColorSetItem>?,
        context: Context,
        mode: DressDisplayMode = DressDisplayMode.AUSPICIOUS
    ): DressColorCollection? {
        val list = items ?: return null
        val finalNine = buildMergedNineShades(list, context, mode)
        return toDressColorCollectionFromNine(finalNine)
    }

    private fun toDressColorCollectionFromNine(finalNine: List<String>): DressColorCollection? {
        if (finalNine.isEmpty()) {
            return null
        }
        val visibleColors = finalNine.filter { it.isNotBlank() }
        if (visibleColors.isEmpty()) {
            return null
        }
        val dresses = mutableListOf<DressColor>()
        val chunked = visibleColors.chunked(3)
        chunked.forEachIndexed { groupIndex, chunk ->
            val c1 = chunk.getOrNull(0) ?: ""
            val c2 = chunk.getOrNull(1) ?: ""
            val c3 = chunk.getOrNull(2) ?: ""
            dresses.add(
                DressColor(
                    colorId = (groupIndex + 1).toString(),
                    dayEng = "ชุดรวม",
                    colorCode1 = c1,
                    colorCode2 = c2,
                    colorCode3 = c3,
                    colorCode4 = null
                )
            )
        }
        return DressColorCollection(dresses)
    }



    fun resolveAuspiciousSetsWithKaliCheck(
        auspicious: List<OutfitMiracleColorSetItem>?,
        inauspiciousNumbers: Set<Int>
    ): List<OutfitMiracleColorSetItem>? {
        val sriList = auspicious ?: return null
        val seenNumbers = linkedSetOf<Int>()
        val out = mutableListOf<OutfitMiracleColorSetItem>()

        sriList.forEach { sriItem ->
            val sriNumber = sriItem.number
            if (sriNumber == null || sriNumber !in 1..8) {
                out.add(sriItem)
                return@forEach
            }
            
            // ตรวจสอบว่าสีซ้ำกับชุดอัปมงคล (kali) หรือไม่ หรือซ้ำกับศรีที่ออกไปก่อนหน้า
            if (inauspiciousNumbers.contains(sriNumber) || !seenNumbers.add(sriNumber)) {
                val montriNumber = resolveMontriNumber(sriNumber, sriItem)
                if (montriNumber != null && montriNumber in 1..8) {
                    seenNumbers.add(montriNumber)
                    val originalLabel = sriItem.label ?: "ศรี"
                    val newLabel = if (originalLabel.contains("ศรี")) originalLabel.replace("ศรี", "มนตรี") else "มนตรี"
                    
                    out.add(
                        sriItem.copy(
                            label = newLabel,
                            number = montriNumber,
                            day = numberToDayName[montriNumber] ?: sriItem.day,
                            colorGroup = numberToColorGroup[montriNumber] ?: sriItem.colorGroup
                        )
                    )
                } else {
                    out.add(sriItem)
                }
            } else {
                out.add(sriItem)
            }
        }
        return out
    }

    fun resolveAuspiciousSetsWithMontriFallback(
        auspicious: List<OutfitMiracleColorSetItem>?
    ): List<OutfitMiracleColorSetItem>? {
        return resolveAuspiciousSetsWithKaliCheck(auspicious, emptySet())
    }

    fun fallbackAuspiciousFromLegacy(
        currentDayEng: String,
        dayBirthEng: String,
        dayBirthNum: Int,
        context: Context
    ): List<OutfitMiracleColorSetItem>? {
        val encoded = getColorCloth(
            currentDay = currentDayEng,
            dayBirth = dayBirthEng,
            dayBirthNum = dayBirthNum,
            type = "d",
            context = context
        ) ?: return null

        val numbers = encoded.mapNotNull { ch ->
            ch.digitToIntOrNull()?.takeIf { it in 1..8 }
        }.distinct()

        if (numbers.isEmpty()) return null

        return numbers.take(3).map { number ->
            OutfitMiracleColorSetItem(
                label = "ศรี",
                sourceDay = null,
                sourceAge = null,
                number = number,
                day = numberToDayName[number],
                dualNumbers = null,
                dualDays = null,
                colorGroup = numberToColorGroup[number]
            )
        }
    }

    fun fallbackDressCollectionFromLegacy(
        currentDayEng: String,
        dayBirthEng: String,
        dayBirthNum: Int,
        type: String,
        context: Context
    ): DressColorCollection? {
        val encoded = getColorCloth(
            currentDay = currentDayEng,
            dayBirth = dayBirthEng,
            dayBirthNum = dayBirthNum,
            type = type,
            context = context
        ) ?: return null

        val numbers = encoded.mapNotNull { ch ->
            ch.digitToIntOrNull()?.takeIf { it in 1..8 }
        }.distinct()

        if (numbers.isEmpty()) return null

        val items = numbers.take(3).map { number ->
            OutfitMiracleColorSetItem(
                label = if (type == "d") "ศรี" else "กาลี",
                sourceDay = null,
                sourceAge = null,
                number = number,
                day = numberToDayName[number],
                dualNumbers = null,
                dualDays = null,
                colorGroup = numberToColorGroup[number]
            )
        }
        return toDressColorCollection(
            items = items,
            context = context,
            mode = if (type == "d") DressDisplayMode.AUSPICIOUS else DressDisplayMode.INAUSPICIOUS
        )
    }

    private val dayCounterClockwiseSequence = listOf(1, 6, 8, 5, 7, 4, 3, 2)
    private val ageCounterClockwiseSequence = listOf(6, 8, 5, 7, 4, 3, 2, 1)
    private val numberToDayName = mapOf(
        1 to "อาทิตย์",
        2 to "จันทร์",
        3 to "อังคาร",
        4 to "พุธ",
        5 to "พฤหัสบดี",
        6 to "ศุกร์",
        7 to "เสาร์",
        8 to "พุธ กลางคืน"
    )
    private val numberToColorGroup = mapOf(
        1 to listOf("สีแดง", "สีแดงเลือดนก", "แดงอ่อน"),
        2 to listOf("สีขาว", "สีเหลืองอ่อน", "สีเหลืองอ่อน"),
        3 to listOf("สีชมพู", "สีชมพูอ่อน", "สีชมพูเข้ม"),
        4 to listOf("สีเขียวเข้ม", "สีเขียวอ่อน", "สีเขียวอ่อน"),
        5 to listOf("สีเหลืองเข้ม", "สีแสด", "สีส้มเข้ม"),
        6 to listOf("สีฟ้า", "สีฟ้าอ่อน", "สีน้ำเงิน"),
        7 to listOf("สีดำ", "สีม่วง", "สีน้ำตาลเข้ม"),
        8 to listOf("สีเทา", "สีเทาอ่อน", "สีเทาอ่อนไล่ตามเฉด")
    )

    private fun resolveMontriNumber(sriNumber: Int, item: OutfitMiracleColorSetItem): Int? {
        val sequence = if (item.sourceAge != null) ageCounterClockwiseSequence else dayCounterClockwiseSequence
        val sriIndex = sequence.indexOf(sriNumber)
        if (sriIndex < 0) return null
        val montriIndex = (sriIndex - 3 + sequence.size) % sequence.size
        return sequence[montriIndex]
    }

    private fun buildMergedNineShades(
        items: List<OutfitMiracleColorSetItem>,
        context: Context,
        mode: DressDisplayMode
    ): List<String> {
        val merged = mutableListOf<String>()
        items.forEach { item ->
            val number = item.number ?: return@forEach
            if (number in 1..8) {
                val palette = OutfitApiColorMappingManager.getDayPalette(context, number)
                merged.addAll(palette.take(3))
            } else {
                merged.addAll(item.colorGroup.orEmpty().take(3))
            }
        }

        if (merged.isEmpty()) {
            val fallback = mutableListOf<String>()
            items.forEach { item ->
                fallback.addAll(item.colorGroup.orEmpty())
            }
            if (fallback.isEmpty()) return emptyList()
            while (fallback.size < 9) {
                fallback.add(fallback.last())
            }
            return fallback.take(9)
        }

        val out = merged.take(9).toMutableList()
        while (out.size < 9) {
            out.add(out.lastOrNull() ?: "#FFFFFF")
        }
        return out
    }



    private fun resolveFiveShades(item: OutfitMiracleColorSetItem, context: Context): List<String> {
        val number = item.number
        if (number != null && number in 1..8) {
            return OutfitApiColorMappingManager.getDayPalette(context, number).take(5)
        }
        val fallback = item.colorGroup.orEmpty().toMutableList()
        while (fallback.size < 5) {
            fallback.add(fallback.lastOrNull() ?: "#FFFFFF")
        }
        return fallback.take(5)
    }

    fun formatThaiColorSet(item: OutfitMiracleColorSetItem?, prefix: String): String {
        if (item == null) {
            return "API $prefix: ไม่มีข้อมูล"
        }
        val label = item.label ?: "-"
        val number = item.number?.toString() ?: "-"
        val day = item.day ?: "-"
        val colors = if (item.colorGroup.isNullOrEmpty()) "-" else item.colorGroup.joinToString(", ")
        return "API $prefix: $label (เลข $number $day) = $colors"
    }

    fun formatOutfitReference(response: OutfitMiracleColorSetsResponse?): String {
        val doc = response?.referenceSkillDoc ?: "/Users/tayap/project-naming/outfit-miracle/SKILL.md"
        return "อ้างอิงการคำนวณชุดสีจาก: $doc"
    }
}
