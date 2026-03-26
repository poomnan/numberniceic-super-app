package com.numberniceic.utils

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

object NetworkUtils {

    fun fetchUrl(urlStr: String, useSslBypass: Boolean = true): String? {
        // Try HTTPS first
        var result = fetchInternal(urlStr, useSslBypass)
        
        // If HTTPS fails and we have cleartext enabled, try HTTP as fallback
        if (result == null && urlStr.startsWith("https://")) {
            val httpUrl = urlStr.replace("https://", "http://")
            Log.w("NetworkUtils", "HTTPS failed, trying HTTP fallback: $httpUrl")
            result = fetchInternal(httpUrl, false)
        }
        
        return result
    }

    private fun fetchInternal(urlStr: String, useSslBypass: Boolean): String? {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            
            if (conn is HttpsURLConnection && useSslBypass) {
                setupUnsafeSsl(conn)
            }
            
            conn.requestMethod = "GET"
            conn.connectTimeout = 7000 // Reduced from 15s to 7s
            conn.readTimeout = 7000    // Reduced from 15s to 7s
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Connection", "close")
            
            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                return sb.toString()
            }
        } catch (e: Exception) {
            Log.e("NetworkUtils", "Fetch Error ($urlStr): ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return null
    }

    private fun setupUnsafeSsl(conn: HttpsURLConnection) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
            })

            // Use TLS instead of SSL for better compatibility
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, java.security.SecureRandom())
            conn.sslSocketFactory = sc.socketFactory
            conn.setHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            Log.e("NetworkUtils", "SSL Bypass Error: ${e.message}")
        }
    }
}
