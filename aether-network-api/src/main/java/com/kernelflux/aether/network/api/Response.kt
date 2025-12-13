package com.kernelflux.aether.network.api

/**
 * HTTP 响应封装
 */
data class Response<T>(
    val data: T?,
    val code: Int,
    val message: String,
    val headers: Map<String, List<String>> = emptyMap(),
    val request: Request,
    val isFromCache: Boolean = false,
    val isSuccessful: Boolean = code in 200..299
) {
    /**
     * 获取响应头（单个值）
     */
    fun header(name: String): String? {
        return headers[name]?.firstOrNull()
    }
    
    /**
     * 获取响应头（所有值）
     */
    fun headers(name: String): List<String> {
        return headers[name] ?: emptyList()
    }
}

/**
 * 网络异常
 */
sealed class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause) {
/**
 * HTTP 错误（非 2xx 状态码）
     */
    class HttpException(
        val code: Int,
        message: String,
        val response: Response<*>?
    ) : NetworkException("HTTP $code: $message")
    
    /**
     * 网络连接异常
     */
    class ConnectionException(message: String, cause: Throwable? = null) : NetworkException(message, cause)
    
    /**
     * 超时异常
     */
    class TimeoutException(message: String, cause: Throwable? = null) : NetworkException(message, cause)
    
    /**
     * 解析异常
     */
    class ParseException(message: String, cause: Throwable? = null) : NetworkException(message, cause)
    
    /**
     * 加密/解密异常
     */
    class CryptoException(message: String, cause: Throwable? = null) : NetworkException(message, cause)
    
    /**
     * 取消异常
     */
    class CancelledException(message: String = "Request cancelled") : NetworkException(message)
    
    /**
     * 未知异常
     */
    class UnknownException(message: String, cause: Throwable? = null) : NetworkException(message, cause)
}
