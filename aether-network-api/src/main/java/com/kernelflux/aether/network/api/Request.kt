package com.kernelflux.aether.network.api

import java.io.InputStream

/**
 * HTTP 请求方法
 */
enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
}

/**
 * 请求体类型
 */
sealed class RequestBody {
    /**
     * JSON 请求体
     */
    data class JsonBody(val data: Any) : RequestBody()
    
    /**
     * 表单请求体
     */
    data class FormBody(val fields: Map<String, String>) : RequestBody()
    
    /**
     * 多部分表单请求体（文件上传）
     */
    data class MultipartBody(
        val parts: List<Part>
    ) : RequestBody() {
        data class Part(
            val name: String,
            val value: String? = null,
            val filename: String? = null,
            val contentType: String? = null,
            val content: ByteArray? = null
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Part

                if (name != other.name) return false
                if (value != other.value) return false
                if (filename != other.filename) return false
                if (contentType != other.contentType) return false
                if (!content.contentEquals(other.content)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = name.hashCode()
                result = 31 * result + (value?.hashCode() ?: 0)
                result = 31 * result + (filename?.hashCode() ?: 0)
                result = 31 * result + (contentType?.hashCode() ?: 0)
                result = 31 * result + (content?.contentHashCode() ?: 0)
                return result
            }
        }
    }
    
    /**
     * 原始字节流请求体
     */
    data class RawBody(
        val data: ByteArray,
        val contentType: String
    ) : RequestBody() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RawBody

            if (!data.contentEquals(other.data)) return false
            if (contentType != other.contentType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + contentType.hashCode()
            return result
        }
    }

    /**
     * 输入流请求体
     */
    data class StreamBody(
        val inputStream: InputStream,
        val contentType: String,
        val contentLength: Long? = null
    ) : RequestBody()
    
    /**
     * Protobuf 请求体
     */
    data class ProtobufBody(val data: ByteArray) : RequestBody() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ProtobufBody

            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    /**
     * 空请求体
     */
    object EmptyBody : RequestBody()
}

/**
 * 请求封装
 */
data class Request(
    val url: String,
    val method: HttpMethod = HttpMethod.GET,
    val headers: Map<String, String> = emptyMap(),
    val params: Map<String, Any> = emptyMap(),
    val body: RequestBody? = null,
    val tag: Any? = null,
    val timeout: Long? = null,
    val retryStrategy: RetryStrategy? = null,
    val cacheStrategy: CacheStrategy? = null,
    val encryptor: Encryptor? = null,
    val decryptor: Decryptor? = null,
    val dataConverter: DataConverter? = null,
    val weakNetworkHandler: WeakNetworkHandler? = null,
    val networkStrategy: NetworkStrategy? = null
) {
    /**
     * 构建完整 URL（包含查询参数）
     */
    fun buildUrl(baseUrl: String): String {
        val fullUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            baseUrl.trimEnd('/') + "/" + url.trimStart('/')
        }
        
        if (params.isEmpty()) {
            return fullUrl
        }
        
        val urlBuilder = StringBuilder(fullUrl)
        val hasQuery = fullUrl.contains('?')
        urlBuilder.append(if (hasQuery) '&' else '?')
        
        params.entries.joinTo(urlBuilder, separator = "&") { (key, value) ->
            "$key=${java.net.URLEncoder.encode(value.toString(), "UTF-8")}"
        }
        
        return urlBuilder.toString()
    }
}
