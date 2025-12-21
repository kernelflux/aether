package com.kernelflux.aether.network.api

/**
 * 网络工具类
 * 提供常用的辅助函数
 */
object NetworkUtils {
    /**
     * 合并请求头
     */
    fun mergeHeaders(
        vararg headerMaps: Map<String, String>
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        headerMaps.forEach { result.putAll(it) }
        return result
    }
    
    /**
     * 合并查询参数
     */
    fun mergeParams(
        vararg paramMaps: Map<String, Any>
    ): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        paramMaps.forEach { result.putAll(it) }
        return result
    }
    
    /**
     * 构建查询字符串
     */
    fun buildQueryString(params: Map<String, Any>): String {
        if (params.isEmpty()) return ""
        
        return params.entries.joinToString("&") { (key, value) ->
            "${java.net.URLEncoder.encode(key, "UTF-8")}=${java.net.URLEncoder.encode(value.toString(), "UTF-8")}"
        }
    }
    
    /**
     * 解析查询字符串
     */
    fun parseQueryString(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        
        val params = mutableMapOf<String, String>()
        query.split("&").forEach { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                params[java.net.URLDecoder.decode(parts[0], "UTF-8")] = 
                    java.net.URLDecoder.decode(parts[1], "UTF-8")
            }
        }
        return params
    }
    
    /**
     * 检查 URL 是否有效
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 从 URL 中提取主机名
     */
    fun extractHostname(url: String): String? {
        return try {
            java.net.URL(url).host
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 从 URL 中提取路径
     */
    fun extractPath(url: String): String? {
        return try {
            java.net.URL(url).path
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Request 扩展函数：从现有 Request 创建新的 RequestBuilder
 */
fun Request.toBuilder(): RequestBuilder {
    return RequestBuilder.create().apply {
        url(this@toBuilder.url)
        method(this@toBuilder.method)
        headers(this@toBuilder.headers)
        params(this@toBuilder.params)
        body?.let { body(it) }
        tag?.let { tag(it) }
        timeout?.let { timeout(it) }
        retryStrategy?.let { retryStrategy(it) }
        cacheStrategy?.let { cacheStrategy(it) }
        encryptor?.let { encryptor(it) }
        decryptor?.let { decryptor(it) }
        dataConverter?.let { dataConverter(it) }
        weakNetworkHandler?.let { weakNetworkHandler(it) }
        networkStrategy?.let { networkStrategy(it) }
    }
}

/**
 * RequestBuilder 扩展函数：快速设置常用配置
 */
fun RequestBuilder.get(url: String) = apply {
    this.url(url).method(HttpMethod.GET)
}

fun RequestBuilder.post(url: String) = apply {
    this.url(url).method(HttpMethod.POST)
}

fun RequestBuilder.put(url: String) = apply {
    this.url(url).method(HttpMethod.PUT)
}

fun RequestBuilder.delete(url: String) = apply {
    this.url(url).method(HttpMethod.DELETE)
}

fun RequestBuilder.patch(url: String) = apply {
    this.url(url).method(HttpMethod.PATCH)
}

/**
 * 快速设置 JSON Content-Type
 */
fun RequestBuilder.json() = apply {
    header("Content-Type", "application/json; charset=utf-8")
}

/**
 * 快速设置表单 Content-Type
 */
fun RequestBuilder.form() = apply {
    header("Content-Type", "application/x-www-form-urlencoded")
}

/**
 * 快速设置 Protobuf Content-Type
 */
fun RequestBuilder.protobuf() = apply {
    header("Content-Type", "application/x-protobuf")
}

/**
 * 快速设置认证头
 */
fun RequestBuilder.auth(token: String, type: String = "Bearer") = apply {
    header("Authorization", "$type $token")
}

/**
 * 快速设置用户代理
 */
fun RequestBuilder.userAgent(ua: String) = apply {
    header("User-Agent", ua)
}
