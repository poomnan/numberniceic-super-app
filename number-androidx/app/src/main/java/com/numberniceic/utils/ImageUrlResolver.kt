package com.numberniceic.utils

import android.net.Uri
import com.numberniceic.https.NetworkConfig
import java.net.URLEncoder

object ImageUrlResolver {
    fun resolve(rawUrl: String?): String {
        var value = rawUrl?.trim().orEmpty()
        if (value.isEmpty()) return ""
        android.util.Log.d("ImageUrlResolver", "Resolving: $value")
        if (value.startsWith("http://") || value.startsWith("https://")) {
            val uri = android.net.Uri.parse(value)
            val host = uri.host?.lowercase().orEmpty()
            
            // Check if host is one of our servers (Active or Retired)
            val isActiveDomain = host == "numberniceic.online" || host == "www.numberniceic.online"
            val isRetiredDomain = host == "ananya.in.th" || host == "www.ananya.in.th"
            val isNamingDomain = host == "xn--b3cu8e7ah6h.com" || host == "www.xn--b3cu8e7ah6h.com"
            val isDirectIp = host == "43.228.85.200"
            
            val isOurServer = isActiveDomain || isRetiredDomain || isDirectIp || isNamingDomain
            
            android.util.Log.d("ImageUrlResolver", "Host: $host, isOurServer: $isOurServer")
            if (!isOurServer) return value
            val path = uri.encodedPath.orEmpty()
            val query = uri.encodedQuery?.let { "?$it" }.orEmpty()
            value = path + query
        }
        // 1. Determine which server it belongs to (Go vs PHP)
        val isGoBackend = (value.contains("/uploads/") && !value.contains("/public/")) || value.contains("/chat/")
        val namingBase = "http://43.228.85.200:8095" // Run Go directly to avoid Nginx proxy issues
        val phpBase = NetworkConfig.BASE_URL // PHP Backend (:81)

        // 2. Clean up the URL (Preserve /public/ for PHP backend as it's required for correct Nginx routing)
        // Stripping /public/ here would make the path hit the Nginx /uploads/ alias pointing to the Go backend instead of the PHP backend
        
        // Fix path: Move from /uploads/temple (broken) to /uploads/buddha/temple (working)
        if (value.contains("/uploads/temple/")) {
            value = value.replace("/uploads/temple/", "/uploads/buddha/temple/")
        }

        // 3. Resolve with appropriate base
        val targetBase = if (isGoBackend) namingBase else phpBase
        val result = targetBase + if (value.startsWith("/")) value else "/$value"
        
        android.util.Log.d("ImageUrlResolver", "Result: $result (Go: $isGoBackend)")
        return result
    }

    /**
     * Specialized resolver for Chat Images from the Go Naming Backend.
     * Tries to use the Naming Domain on Port 80 (Nginx) for maximum reliability.
     */
    fun resolveNamingChat(rawUrl: String?): String {
        val raw = rawUrl?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        
        val namingBase = "http://43.228.85.200:8095" 
        
        // 1. If it's already a full http URL, handle it safely
        if (raw.startsWith("http")) {
            // If it's already correct, just return it
            if (raw.startsWith(namingBase)) return raw
            
            // If it's from our server (either by domain or IP), reconstruct it to use namingBase
            if (raw.contains("xn--b3cu8e7ah6h.com") || raw.contains("43.228.85.200")) {
                val uri = android.net.Uri.parse(raw)
                val path = uri.path.orEmpty()
                return namingBase + if (path.startsWith("/")) path else "/$path"
            }
            return raw
        }
        
        // 2. Resolve Relative Path
        var path = raw
        if (path.startsWith("/public/")) {
            path = path.substring(7)
        } else if (path.startsWith("/public")) {
            path = path.substring(7)
        }
        
        // Ensure path starts with /uploads/ or /chat/ etc.
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        
        // Use Naming Domain on Port 80 (Nginx static serving)
        return "$namingBase$normalizedPath"
    }

    fun resolveCandidates(rawUrl: String?): List<String> {
        val raw = rawUrl?.trim().orEmpty()
        if (raw.isEmpty()) return emptyList()
        val primary = resolve(raw)
        val candidates = linkedSetOf<String>()
        if (primary.isNotEmpty()) candidates.add(primary)
        candidates.add(raw)
        if (raw.startsWith("https://")) {
            candidates.add("http://" + raw.removePrefix("https://"))
        }
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            val uri = Uri.parse(raw)
            val host = uri.host?.lowercase().orEmpty()
            val isOurServer = host == "numberniceic.online" ||
                    host == "www.numberniceic.online" ||
                    host == "ananya.in.th" ||
                    host == "www.ananya.in.th" ||
                    host == "xn--b3cu8e7ah6h.com" ||
                    host == "www.xn--b3cu8e7ah6h.com" ||
                    host == "43.228.85.200"
            if (!isOurServer) {
                val normalized = raw.removePrefix("https://").removePrefix("http://")
                val encoded = URLEncoder.encode(normalized, "UTF-8")
                candidates.add("https://wsrv.nl/?url=$encoded")
            }
        }
        return candidates.filter { it.isNotBlank() }
    }

}
