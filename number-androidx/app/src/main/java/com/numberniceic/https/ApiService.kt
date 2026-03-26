package com.numberniceic.https

import com.google.gson.JsonObject
import com.numberniceic.data.admin.Serverx
import com.numberniceic.data.admin.ServerVip
import com.numberniceic.data.admin.ServerMessage
import okhttp3.ResponseBody
import okhttp3.MultipartBody
import com.numberniceic.data.member.BagColorDaoCollection
import com.numberniceic.data.member.VipCodeDao
import com.numberniceic.data.news.News
import com.numberniceic.data.news.News24
import com.numberniceic.data.news.NewsHeadline
import com.numberniceic.data.news.NewsHeadlineCollection
import com.numberniceic.data.apicollectiondao.HomeCollectionDao
import com.numberniceic.data.admin.Article
import com.numberniceic.data.persons.CusTime
import com.numberniceic.data.persons.DressColorCollection
import com.numberniceic.data.persons.MiracleDo
import com.numberniceic.data.persons.MiracleDoV2
import com.numberniceic.data.persons.OutfitMiracleColorSetsRequest
import com.numberniceic.data.persons.OutfitMiracleColorSetsResponse
import com.numberniceic.data.persons.OutfitDayPalettesResponse
import com.numberniceic.data.persons.OutfitDayPalettesUpsertRequest
import com.numberniceic.data.rengyam.LengYamDao
import com.numberniceic.data.rengyam.LuckyNumber
import com.numberniceic.data.rengyam.WanpraDao
import com.numberniceic.data.rengyam.WanSpecialCollection
import com.numberniceic.data.member.MemberVipCollectionDao
import com.numberniceic.data.topic.Topic
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao
import com.numberniceic.data.apicollectiondao.NameSurnameCollectionDao
import com.numberniceic.data.nickname.NameNickVipCollection
import com.numberniceic.data.product.Product
import com.numberniceic.data.product.ProductCategory
import com.numberniceic.data.admin.Users
import com.numberniceic.data.apicollectiondao.TabianCollectionDao
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao
import com.numberniceic.data.apicollectiondao.PhoneSellNumberCollectionDao
import com.numberniceic.data.admin.MsgUpdateColor
import com.numberniceic.data.admin.MsgAddBagColor
import com.numberniceic.data.admin.ColorSix
import com.numberniceic.data.admin.Users as AdminUsers
import com.numberniceic.data.nickname.NameSuggestion
import com.numberniceic.data.admin.BuddhaPang
import retrofit2.Call
import retrofit2.http.*

import com.numberniceic.data.notification.NotificationListResponse
import com.numberniceic.data.notification.UnreadCountResponse
import com.numberniceic.data.notification.MarkReadRequest

interface ApiService {

    // --- Notification API v2 (Hybrid Approach) ---
    @GET("api/v2/notifications/by-type")
    fun getNotificationsByType(
        @Query("memberid") memberId: String,
        @Query("type") type: String,
        @Query("limit") limit: Int = 50
    ): Call<NotificationListResponse>

    @GET("api/v2/notifications")
    fun getNotifications(
        @Query("memberid") memberId: String,
        @Query("limit") limit: Int = 50
    ): Call<NotificationListResponse>

    @POST("api/v2/notifications/mark-read")
    fun markAsRead(@Body body: MarkReadRequest): Call<ServerMessage>

    @GET("api/v2/notifications/unread-count")
    fun getUnreadCount(
        @Query("memberid") memberId: String,
        @Query("type") type: String? = null
    ): Call<UnreadCountResponse>

    @POST("api/v2/notifications/save")
    fun saveNotificationToServer(@Body body: JsonObject): Call<JsonObject>

    // --- User & Auth ---

    @POST("member/login")
    fun userLogin(@Body body: JsonObject): Call<Serverx>

    @POST("member/updateToken")
    fun updateFcmToken(@Body body: JsonObject): Call<JsonObject>

    @POST("member/register/v2")
    fun userRegister(@Body body: JsonObject): Call<ServerMessage>

    @POST("member/update")
    fun userUpdateData(@Body body: JsonObject): Call<Serverx>

    // ✅ ดึงข้อมูล user ล่าสุดจาก DB โดยตรง (ใช้ refresh vipcode/status)
    @GET("member/info/{memberid}")
    fun getMemberInfo(@Path("memberid") memberId: String): Call<Serverx>

    @POST("guest/register")
    fun registerGuest(@Body body: JsonObject): Call<JsonObject>

    @POST("guest/save-address")
    fun saveGuestAddress(@Body body: JsonObject): Call<JsonObject>

    @POST("guest/save-order")
    fun saveGuestOrder(@Body body: JsonObject): Call<JsonObject>

    // --- Admin Guest Address Management ---
    @GET("http://43.228.85.200:8095/admin/guest-addresses")
    fun getGuestAddresses(): Call<JsonObject>

    @POST("http://43.228.85.200:8095/admin/guest-addresses/toggle-shipping")
    fun toggleShippingStatus(@Body body: JsonObject): Call<JsonObject>

    @POST("http://43.228.85.200:8095/admin/guest-addresses/update-address")
    fun updateOrderAddress(@Body body: JsonObject): Call<JsonObject>

    @GET("admin/guest-addresses/{guestId}")
    fun getGuestAddressById(@Path("guestId") guestId: String): Call<JsonObject>

    @GET("admin/guest-addresses/search/{searchTerm}")
    fun searchGuestAddresses(@Path("searchTerm") searchTerm: String): Call<JsonObject>

    @GET("admin/guest-addresses/stats")
    fun getGuestAddressStats(): Call<JsonObject>

    @POST("member/register/successcf")
    fun userRegisInsert(@Body body: JsonObject): Call<ServerMessage>

    @GET("admin/finduser/bagcolor/{username}")
    fun findUsersByUsername(@Path("username") username: String): Call<Users>

    @POST("member/vipcode")
    fun userVipcodeUpgrade(@Body body: JsonObject): Call<ServerVip>

    @GET("member/currenttime")
    fun currentTime(): Call<CusTime>

    // --- Person & Fortune ---
    @GET("member/bagcolor/{memberid}/{age1}/{age2}")
    fun getBagColor(
        @Path("memberid") memberId: String,
        @Path("age1") age1: String,
        @Path("age2") age2: String
    ): Call<BagColorDaoCollection>

    @GET("member/wanpra/{wandate}")
    fun getWanPra(@Path("wandate") wanDate: String): Call<WanpraDao>

    @GET("member/wanspecial/{wandate}")
    fun getWanSpecial(@Path("wandate") wanDate: String): Call<WanSpecialCollection>

    @GET("vipcode/{apicode}")
    fun checkVipCodeApi(@Path("apicode") apiCode: String): Call<MemberVipCollectionDao>

    @GET("lucky/number")
    fun getLuckyNumber(): Call<LuckyNumber>

    @POST("lucky/number")
    fun addLuckyNumber(@Body body: JsonObject): Call<ServerMessage>

    @POST("lucky/number/v2")
    fun addLuckyNumberV2(@Body body: JsonObject): Call<ServerMessage>

    @GET("member/dresscolor/{birthDay}")
    fun getDressColor(@Path("birthDay") birthDay: String): Call<DressColorCollection>
    
    @POST(BackendHosts.NAMING_BASE + "/api/v1/outfit-miracle/color-sets")
    fun getOutfitMiracleColorSets(@Body body: OutfitMiracleColorSetsRequest): Call<OutfitMiracleColorSetsResponse>

    @GET(BackendHosts.NAMING_BASE + "/api/v1/outfit-miracle/day-palettes")
    fun getOutfitDayPalettes(): Call<OutfitDayPalettesResponse>

    @POST(BackendHosts.NAMING_BASE + "/api/v1/outfit-miracle/day-palettes")
    fun upsertOutfitDayPalettes(@Body body: OutfitDayPalettesUpsertRequest): Call<OutfitDayPalettesResponse>

    @GET("member/miradoV2/{activity}/{birthDay}/{today}/{currentDate}")
    fun getMiraDoV2(
        @Path("activity") activity: String,
        @Path("birthDay") birthDay: String,
        @Path("today") today: String,
        @Path("currentDate") currentDate: String
    ): Call<MiracleDoV2>

    @GET("member/mirado/{activity}/{birthDay}/{today}")
    fun getMiraDo(
        @Path("activity") activity: String,
        @Path("birthDay") birthDay: String,
        @Path("today") today: String
    ): Call<MiracleDo>

    @GET("member/lengyam")
    fun getLengYam(): Call<LengYamDao>

    @GET("http://43.228.85.200:8095/api/kalagni")
    fun getKalagniDay(@Query("birth_day") birthDay: String): Call<com.google.gson.JsonObject>
    
    @GET("http://43.228.85.200:8095/api/kalagni/age")
    fun getKalagniAge(@Query("birth_day") birthDay: String, @Query("age") age: Int): Call<com.google.gson.JsonObject>

    @GET("http://43.228.85.200:8095/api/foo-days")
    fun getFooDays(@Query("year") year: Int? = null): Call<com.google.gson.JsonObject>

    @GET("http://43.228.85.200:8095/api/sitti-chok")
    fun getSittiChok(@Query("year") year: Int? = null): Call<com.google.gson.JsonObject>

    @GET("http://43.228.85.200:8095/api/ubath")
    fun getUbathDays(@Query("year") year: Int? = null): Call<com.google.gson.JsonObject>

    @GET("http://43.228.85.200:8095/api/lokawinat")
    fun getLokawinatDays(@Query("year") year: Int? = null): Call<com.google.gson.JsonObject>

    // --- Wanpra API ---
    @POST("http://43.228.85.200:8095/api/v1/wanpra/calculate")
    fun calculateWanpra(@Body body: com.google.gson.JsonObject): Call<com.google.gson.JsonObject>

    // --- News ---
    @GET("news/topicall/{newsIdType}")
    fun getNewsAllByType(@Path("newsIdType") newsIdType: String): Call<NewsHeadlineCollection>

    @GET("news/topic/{newsIdType}")
    fun getNewsByType(@Path("newsIdType") newsIdType: String): Call<NewsHeadlineCollection>

    @GET("news/topic24/stable")
    fun getNewsTopic24(): Call<News24>

    @GET("news/api/article/{newsId}")
    fun getNewsHeadlineDetail(@Path("newsId") newsId: String): Call<NewsHeadline>

    @GET("home/main/{homeNum}")
    fun getHomeDetail(@Path("homeNum") homeNum: String): Call<HomeCollectionDao>

    @GET("tabian/main/{tabianNum}")
    fun getTabianDetail(@Path("tabianNum") tabianNum: String): Call<TabianCollectionDao>

    @GET("tabian/sell/all")
    fun getTabianSellList(): Call<List<com.numberniceic.data.tabian.TabianSellItem>>

    @GET("phone/{phoneNum}")
    fun getPhoneDetail(@Path("phoneNum") phoneNum: String): Call<PhoneCollectionDao>

    @GET("shopsell/main")
    fun getPhoneSellList(): Call<PhoneSellNumberCollectionDao>

    @POST("admin/topic")
    fun uploadTopic(@Body body: JsonObject): Call<Topic>

    @PUT("admin/topic")
    fun updateTopic(@Body body: JsonObject): Call<Topic>

    @PUT("admin/bagcolor")
    fun updateBagColor(@Body body: JsonObject): Call<MsgUpdateColor>

    @POST("admin/bagcolor")
    fun addBagColor(@Body body: JsonObject): Call<MsgAddBagColor>

    @GET("admin/bagcolor/{userId}")
    fun getColorSixByUserId(@Path("userId") userId: String): Call<ColorSix>

    @GET("nickname/main/{name}/{birthDay}")
    fun getNicknameDetail(@Path("name") name: String, @Path("birthDay") birthDay: String): Call<NickNameCollectionDao>

    @GET("name/main/{name}/{surname}/{birthDay}")
    fun getNameSurnameDetail(@Path("name") name: String, @Path("surname") surname: String, @Path("birthDay") birthDay: String): Call<NameSurnameCollectionDao>

    @GET("admin/finduser/bagcolor/{username}")
    fun findUserBagColor(@Path("username") username: String): Call<Users>

    @GET("admin/notifications/send-bag-colors")
    fun sendBagColorNoti(@Query("memberid") userId: String): Call<JsonObject>
    @GET("nickname/list/{usetable}/{day}/{charx}/{txtPrefix}")
    fun getNicknameList(@Path("usetable") usetable: String, @Path("day") day: String, @Path("charx") charx: String, @Path("txtPrefix") txtPrefix: String): Call<NameNickVipCollection>

    @GET("nickname/list/{usetable}/{day}/{charx}/{txtPrefix}/{lastid}")
    fun getNicknameVipList(@Path("usetable") usetable: String, @Path("day") day: String, @Path("charx") charx: String, @Path("txtPrefix") txtPrefix: String, @Path("lastid") lastid: String): Call<NameNickVipCollection>

    @POST("admin/secretcode/list")
    fun addSecretcodeList(@Body body: JsonObject): Call<ServerMessage>

    @POST("admin/nickname")
    fun addNickname(@Body body: JsonObject): Call<ServerMessage>

    @POST("admin/realname")
    fun addRealname(@Body body: JsonObject): Call<ServerMessage>
    
    @POST("admin/secretcode/list")
    fun addSecretCode(@Body body: JsonObject): Call<ServerMessage>

    // --- Admin Articles ---
    @GET("admin/articles/list")
    fun getAdminArticles(): Call<List<Article>>

    @POST("admin/articles/save")
    fun saveAdminArticle(@Body body: JsonObject): Call<ServerMessage>

    @POST("admin/articles/delete")
    fun deleteAdminArticle(@Body body: JsonObject): Call<ServerMessage>

    @POST("admin/articles/upload-image")
    fun uploadArticleImage(@Body body: JsonObject): Call<JsonObject>

    @POST("admin/notifications/custom/send")
    fun sendCustomNotify(@Body body: JsonObject): Call<ResponseBody>

    @GET("api/names/suggestions")
    fun getNameSuggestions(@Query("q") query: String): Call<List<NameSuggestion>>

    // --- Buddha Pang ---
    @GET("admin/buddha/pangs")
    fun getBuddhaPangs(): Call<List<BuddhaPang>>

    @POST("admin/buddha/assign")
    fun assignBuddhaPang(@Body body: JsonObject): Call<JsonObject>

    @GET("api/buddha/assigned/{memberid}")
    fun getAssignedBuddhaPang(@Path("memberid") memberId: String): Call<com.google.gson.JsonObject>

    @GET("admin/temple/assigned/{memberid}")
    fun getAssignedSacredTemple(@Path("memberid") memberId: String): Call<List<com.numberniceic.data.admin.SacredTemple>>

    @POST("admin/temple/assign")
    fun assignSacredTemple(@Body body: com.google.gson.JsonObject): Call<com.google.gson.JsonObject>
    
    @GET("admin/temple/api")
    fun getSacredTemples(): Call<List<com.numberniceic.data.admin.SacredTemple>>

    @POST("admin/merit/assign")
    fun assignMerit(@Body body: com.google.gson.JsonObject): Call<com.google.gson.JsonObject>

    @GET("member/merit/assigned/{memberid}")
    fun getAssignedMerit(@Path("memberid") memberId: String, @Query("type") type: String? = null): Call<List<com.numberniceic.data.notification.NotificationResponse>>

    @GET("api/spell/latest")
    fun getLatestSpell(@Query("memberid") memberid: String?): Call<com.google.gson.JsonObject>

    @GET("api/spell/assigned/{memberid}")
    fun getAssignedSpells(@Path("memberid") memberid: String): Call<com.google.gson.JsonObject>

    @GET("admin/spell/api")
    fun getAllSpells(@Query("memberid") memberid: String?): Call<com.numberniceic.data.admin.SpellListResponse>

    @GET("admin/spell/api/{id}")
    fun getSpellById(@Path("id") id: String, @Query("memberid") memberid: String?): Call<com.google.gson.JsonObject>

    @POST("admin/spell/assign")
    fun assignSpell(@Body body: com.google.gson.JsonObject): Call<com.google.gson.JsonObject>

    @POST("admin/spell/update-note")
    fun updateSpellNote(@Body body: com.google.gson.JsonObject): Call<com.google.gson.JsonObject>

    // --- Tabian Management ---
    @POST("admin/tabian/save")
    fun saveTabian(@Body body: JsonObject): Call<ServerMessage>

    @POST("admin/tabian/delete")
    fun deleteTabian(@Body body: JsonObject): Call<ServerMessage>

    // --- Assignment Deletion ---
    @POST("api/merit/delete")
    fun deleteMeritAssignment(@Body body: JsonObject): Call<JsonObject>

    @POST("api/buddha/delete")
    fun deleteBuddhaAssignment(@Body body: JsonObject): Call<JsonObject>

    @POST("api/temple/delete")
    fun deleteTempleAssignment(@Body body: JsonObject): Call<JsonObject>

    @GET("api/inauspicious/assigned/{memberid}")
    fun getAssignedInauspicious(@Path("memberid") memberId: String): Call<List<com.numberniceic.data.admin.InauspiciousData>>

    @GET("admin/inauspicious/history/{memberid}")
    fun getInauspiciousHistoryAll(@Path("memberid") memberId: String): Call<List<com.numberniceic.data.admin.InauspiciousData>>

    @POST("admin/inauspicious/assign-api")
    fun assignInauspicious(@Body body: JsonObject): Call<JsonObject>

    @POST("api/inauspicious/delete")
    fun deleteInauspiciousAssignment(@Body body: JsonObject): Call<JsonObject>

    @GET("api/auspicious/assigned/{memberid}")
    fun getAssignedAuspicious(@Path("memberid") memberId: String): Call<List<com.numberniceic.data.admin.InauspiciousData>>

    @GET("admin/auspicious/history/{memberid}")
    fun getAuspiciousHistoryAll(@Path("memberid") memberId: String): Call<List<com.numberniceic.data.admin.InauspiciousData>>

    @POST("admin/auspicious/assign-api")
    fun assignAuspicious(@Body body: JsonObject): Call<JsonObject>

    @POST("api/auspicious/delete")
    fun deleteAuspiciousAssignment(@Body body: JsonObject): Call<JsonObject>

    @POST("api/spell/delete")
    fun deleteSpellAssignment(@Body body: JsonObject): Call<JsonObject>

    // --- Go Backend Chat System ---
    @POST(BackendHosts.NAMING_BASE + "/chat/init")
    fun initChat(@Body body: JsonObject): Call<JsonObject>

    @POST(BackendHosts.NAMING_BASE + "/chat/send")
    fun sendChatMessage(@Body body: JsonObject): Call<com.numberniceic.data.chat.ChatMessage>

    @GET(BackendHosts.NAMING_BASE + "/chat/poll")
    fun pollMessages(@Query("session_id") sessionId: String): Call<List<com.numberniceic.data.chat.ChatMessage>>

    @GET(BackendHosts.NAMING_BASE + "/chat/history")
    fun getChatHistory(@Query("session_id") sessionId: String, @Query("limit") limit: Int = 50): Call<List<com.numberniceic.data.chat.ChatMessage>>

    @GET(BackendHosts.NAMING_BASE + "/chat/admin/poll")
    fun adminPollMessages(): Call<List<com.numberniceic.data.chat.ChatMessage>>

    @GET(BackendHosts.NAMING_BASE + "/chat/admin/recent")
    fun getAdminRecentMessages(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Call<List<com.numberniceic.data.chat.ChatMessage>>

    @DELETE(BackendHosts.NAMING_BASE + "/chat/delete")
    fun deleteMessage(@Query("message_id") messageId: Long): Call<JsonObject>

    @DELETE(BackendHosts.NAMING_BASE + "/chat/admin/session/delete")
    fun deleteChatSession(@Query("session_id") sessionId: String): Call<JsonObject>

    @GET(BackendHosts.NAMING_BASE + "/chat/unread-count")
    fun getUnreadChatCountGo(@Query("member_id") memberId: String): Call<UnreadCountResponse>

    @POST(BackendHosts.NAMING_BASE + "/chat/mark-read")
    fun markChatAsReadGo(@Query("session_id") sessionId: String): Call<JsonObject>

    @POST(BackendHosts.NAMING_BASE + "/chat/admin/mark-read")
    fun markChatAsReadAdmin(@Query("session_id") sessionId: String): Call<JsonObject>

    @Multipart
    @POST(BackendHosts.NAMING_BASE + "/chat/upload")
    fun uploadChatImage(@Part file: MultipartBody.Part): Call<JsonObject>

    @GET(BackendHosts.NAMING_BASE + "/recommend-spelling")
    fun recommendSpelling(
        @Query("name") name: String,
        @Query("day") day: String,
        @Query("limit") limit: Int = 10
    ): Call<List<com.google.gson.JsonObject>>

    @GET(BackendHosts.NAMING_BASE + "/recommend")
    fun recommendNames(
        @Query("intent") intent: String,
        @Query("surname") surname: String,
        @Query("day") day: String,
        @Query("limit") limit: Int = 10
    ): Call<List<com.google.gson.JsonObject>>

    // --- Ninin Dream Chat ---
    @POST(BackendHosts.NAMING_BASE + "/api/v1/ninin/chat")
    fun sendNininChat(@Body body: com.numberniceic.data.ninin.NininChatRequest): Call<com.numberniceic.data.ninin.NininChatResponse>

    @POST(BackendHosts.NAMING_BASE + "/api/v1/ninin/redeem-access")
    fun redeemNininAccess(@Body body: com.numberniceic.data.ninin.NininRedeemAccessRequest): Call<com.numberniceic.data.ninin.NininRedeemAccessResponse>

    // --- Naming Assistant Chat ---
    @POST(BackendHosts.NAMING_BASE + "/api/v1/naming/chat")
    fun sendNamingChat(@Body body: com.numberniceic.data.naming.NamingChatRequest): Call<com.numberniceic.data.naming.NamingChatResponse>

    // --- Admin Dream Management ---
    @POST(BackendHosts.NAMING_BASE + "/add-dream")
    fun addDream(@Body body: com.numberniceic.data.admin.AddDreamRequest): Call<com.numberniceic.data.admin.AddDreamResponse>

    @POST(BackendHosts.NAMING_BASE + "/update-dream")
    fun updateDream(@Body body: com.numberniceic.data.admin.UpdateDreamRequest): Call<com.numberniceic.data.admin.AddDreamResponse>

    @POST(BackendHosts.NAMING_BASE + "/delete-dream")
    fun deleteDream(@Body body: com.google.gson.JsonObject): Call<com.numberniceic.data.admin.AddDreamResponse>

    @GET(BackendHosts.NAMING_BASE + "/get-all-dreams")
    fun getAllDreams(): Call<List<com.numberniceic.data.admin.DreamAdminItem>>

    // --- Product Management APIs ---
    @GET(BackendHosts.NAMING_BASE + "/api/v1/products")
    fun getProducts(@Query("category_id") categoryId: Int? = null): Call<List<Product>>

    @GET(BackendHosts.NAMING_BASE + "/api/v1/product-categories")
    fun getProductCategories(): Call<List<ProductCategory>>

    @POST(BackendHosts.NAMING_BASE + "/api/v1/admin/products/add")
    fun addProduct(@Body product: Product): Call<Product>

    @POST(BackendHosts.NAMING_BASE + "/api/v1/admin/products/update")
    fun updateProduct(@Body product: Product): Call<JsonObject>

    @POST(BackendHosts.NAMING_BASE + "/api/v1/admin/products/delete")
    fun deleteProduct(@Query("id") id: Int): Call<JsonObject>

    @POST(BackendHosts.NAMING_BASE + "/api/v1/admin/products/update-shipping")
    fun updateShippingStatus(@Body body: JsonObject): Call<JsonObject>

    @POST(BackendHosts.NAMING_BASE + "/api/v1/admin/categories/add")
    fun addCategory(@Body category: ProductCategory): Call<ProductCategory>

    @POST(BackendHosts.NAMING_BASE + "/api/v1/admin/categories/update")
    fun updateCategory(@Body category: ProductCategory): Call<JsonObject>

    @POST(BackendHosts.NAMING_BASE + "/api/v1/admin/categories/delete")
    fun deleteCategory(@Query("id") id: Int): Call<JsonObject>

    // --- Admin User Management (Go Backend) ---
    @GET("http://43.228.85.200:8095/admin/users/list")
    fun listUsers(@Query("search") search: String?): Call<com.google.gson.JsonObject>

    @POST("http://43.228.85.200:8095/admin/member/update-status")
    fun updateUserStatus(@Body body: com.google.gson.JsonObject): Call<com.google.gson.JsonObject>

    @POST("http://43.228.85.200:8095/admin/member/edit")
    fun editMember(@Body body: com.google.gson.JsonObject): Call<com.google.gson.JsonObject>

    @POST("http://43.228.85.200:8095/admin/member/delete")
    fun deleteMember(@Body body: com.google.gson.JsonObject): Call<com.google.gson.JsonObject>

    // --- Payment APIs (PaySolutions) ---
    @POST(BackendHosts.NAMING_BASE + "/api/v1/payment/qr")
    fun createPaymentQR(@Body body: com.google.gson.JsonObject): Call<com.google.gson.JsonObject>

    @GET(BackendHosts.NAMING_BASE + "/api/v1/payment/status")
    fun getPaymentStatus(@Query("ref_no") refNo: String): Call<com.google.gson.JsonObject>

    @GET(BackendHosts.NAMING_BASE + "/admin/zircon-orders")
    fun getZirconOrders(): Call<List<com.google.gson.JsonObject>>

    // --- Application Privileges ---
    @GET("https://numberniceic.online/api/v2/application/privileges")
    fun getApplicationPrivileges(): Call<List<com.numberniceic.data.admin.ApplicationPrivilege>>
}
