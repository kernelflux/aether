package com.kernelflux.aether.network.okhttp

import com.kernelflux.aether.network.api.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Request as OkHttpRequest
import okhttp3.Response as OkHttpResponse

/**
 * OkHttp 缓存适配器
 * 将 Aether 的 Cache 接口适配到 OkHttp 的 Cache
 * 注意：OkHttp 的 Cache 是 final 类，无法继承，这里使用组合方式
 */
class OkHttpCacheAdapter(
    private val cache: Cache,
    val keyGenerator: CacheKeyGenerator = DefaultCacheKeyGenerator()
) {
    
    /**
     * 从 Aether Cache 获取响应
     */
    fun get(request: OkHttpRequest): OkHttpResponse? {
        val cacheKey = keyGenerator.generate(convertRequest(request))
        val entry = cache.get(cacheKey) ?: return null
        
        if (!entry.isValid()) {
            cache.remove(cacheKey)
            return null
        }
        
        // 构建 OkHttp Response from cache
        return buildOkHttpResponse(request, entry)
    }
    
    /**
     * 将响应存储到 Aether Cache
     */
    fun put(response: OkHttpResponse) {
        val request = response.request
        val cacheKey = keyGenerator.generate(convertRequest(request))
        
        val body = response.body?.bytes() ?: return
        val headers = response.headers.toMultimap()
        
        val entry = CacheEntry(
            data = body,
            headers = headers,
            timestamp = System.currentTimeMillis(),
            ttl = getCacheTTL(response),
            etag = response.header("ETag"),
            lastModified = response.header("Last-Modified")
        )
        
        cache.put(cacheKey, entry)
    }
    
    /**
     * 从 Aether Cache 移除
     */
    fun remove(request: OkHttpRequest) {
        val cacheKey = keyGenerator.generate(convertRequest(request))
        cache.remove(cacheKey)
    }
    
    /**
     * 更新缓存条目
     */
    fun update(response: OkHttpResponse, networkResponse: OkHttpResponse) {
        // 更新缓存条目
        val request = response.request
        val cacheKey = keyGenerator.generate(convertRequest(request))
        
        val body = networkResponse.body?.bytes() ?: return
        val headers = networkResponse.headers.toMultimap()
        
        val entry = CacheEntry(
            data = body,
            headers = headers,
            timestamp = System.currentTimeMillis(),
            ttl = getCacheTTL(networkResponse),
            etag = networkResponse.header("ETag"),
            lastModified = networkResponse.header("Last-Modified")
        )
        
        cache.put(cacheKey, entry)
    }
    
    /**
     * 清空缓存
     */
    fun evictAll() {
        cache.clear()
    }
    
    /**
     * 获取缓存大小
     */
    fun size(): Long {
        return cache.size()
    }
    
    // ========== 私有方法 ==========
    
    private fun convertRequest(request: OkHttpRequest): Request {
        val url = request.url.toString()
        val method = when (request.method) {
            "GET" -> HttpMethod.GET
            "POST" -> HttpMethod.POST
            "PUT" -> HttpMethod.PUT
            "DELETE" -> HttpMethod.DELETE
            "PATCH" -> HttpMethod.PATCH
            "HEAD" -> HttpMethod.HEAD
            "OPTIONS" -> HttpMethod.OPTIONS
            else -> HttpMethod.GET
        }
        
        val headers = request.headers.toMultimap().mapValues { it.value.firstOrNull() ?: "" }
        val params = request.url.queryParameterNames.associateWith { 
            request.url.queryParameter(it) ?: "" 
        }
        
        return Request(
            url = url,
            method = method,
            headers = headers,
            params = params
        )
    }
    
    private fun buildOkHttpResponse(request: OkHttpRequest, entry: CacheEntry): OkHttpResponse {
        val contentType = entry.headers["Content-Type"]?.firstOrNull()?.toMediaTypeOrNull() 
            ?: "application/octet-stream".toMediaType()
        
        val body = entry.data.toResponseBody(contentType)
        
        val builder = OkHttpResponse.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body)
        
        entry.headers.forEach { (name, values) ->
            values.forEach { value ->
                builder.addHeader(name, value)
            }
        }
        
        return builder.build()
    }
    
    private fun getCacheTTL(response: OkHttpResponse): Long {
        val cacheControl = response.cacheControl
        if (cacheControl.maxAgeSeconds > 0) {
            return cacheControl.maxAgeSeconds * 1000L
        }
        
        // 从 Expires 头获取
        val expires = response.header("Expires")
        if (expires != null) {
            // 解析 Expires 头（简化处理）
            return 3600_000L // 默认 1 小时
        }
        
        return 0L // 永久有效
    }
}

/**
 * 默认缓存键生成器
 */
class DefaultCacheKeyGenerator : CacheKeyGenerator {
    override fun generate(request: Request): String {
        val url = request.buildUrl("")
        val method = request.method.name
        val headers = request.headers.entries.sortedBy { it.key }
            .joinToString("|") { "${it.key}=${it.value}" }
        return "$method:$url:$headers"
    }
}
