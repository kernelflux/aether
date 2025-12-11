package com.kernelflux.aether.network.spi


/**
 * 网络请求回调
 */
interface NetworkCallback<T> {
    fun onSuccess(data: T)
    fun onError(error: Throwable)
}

/**
 * 网络请求配置
 */
data class NetworkConfig(
    val baseUrl: String,
    val connectTimeout: Long = 30_000,
    val readTimeout: Long = 30_000,
    val writeTimeout: Long = 30_000,
    val headers: Map<String, String> = emptyMap()
)

/**
 * 网络客户端服务接口
 * 
 * @author Aether Framework
 */
interface INetworkClient {
    
    /**
     * 初始化网络客户端
     */
    fun init(config: NetworkConfig)
    
    /**
     * GET请求
     */
    fun <T> get(
        url: String,
        params: Map<String, Any> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        responseType: Class<T>,
        callback: NetworkCallback<T>
    )
    
    /**
     * POST请求
     */
    fun <T> post(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap(),
        responseType: Class<T>,
        callback: NetworkCallback<T>
    )
    
    /**
     * 同步GET请求
     */
    fun <T> getSync(
        url: String,
        params: Map<String, Any> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        responseType: Class<T>
    ): T?
    
    /**
     * 同步POST请求
     */
    fun <T> postSync(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap(),
        responseType: Class<T>
    ): T?
}

