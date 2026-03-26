package com.numberniceic.https

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Dns
import java.net.InetAddress
import java.net.Inet4Address
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = BackendHosts.CONTENT_BASE + "/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE // Disable logging to prevent truncation
    }

    // Expose as 'okHttpClient' for MainApplication compatibility
    val okHttpClient: OkHttpClient
        get() = unsafeOkHttpClient

    // Unsafe Client to bypass SSL issues on old Android (S8)
    private val unsafeOkHttpClient: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })

            // Use "TLS" instead of "SSL" for better compatibility with modern servers on older devices
            val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
                .hostnameVerifier { _, _ -> true } // Trust all hostnames
                
                .dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        return try {
                            Dns.SYSTEM.lookup(hostname).sortedBy { if (it is Inet4Address) 0 else 1 }
                        } catch (e: Exception) {
                            throw e
                        }
                    }
                })

                // Support both for better compatibility, fallback to 1.1 if needed
                .protocols(listOf(Protocol.HTTP_1_1, Protocol.HTTP_2))
                
                .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))

                // Shorten timeouts from 120s to 30s so we can recover/retry faster
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    val instance: Retrofit by lazy {
        val gson = com.google.gson.GsonBuilder()
            .setLenient()
            .serializeNulls()
            // Register custom deserializer for flexible type handling
            .registerTypeAdapter(String::class.java, object : com.google.gson.JsonDeserializer<String> {
                override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): String {
                    return json?.asString ?: ""
                }
            })
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(unsafeOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    val api: ApiService by lazy {
        instance.create(ApiService::class.java)
    }

    // Compatibility method if needed
    fun getInstance(): RetrofitClient {
        return this
    }
}
