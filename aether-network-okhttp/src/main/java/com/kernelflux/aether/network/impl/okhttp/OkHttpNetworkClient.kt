package com.kernelflux.aether.network.impl.okhttp

import com.google.gson.Gson
import com.kernelflux.aether.network.spi.INetworkClient
import com.kernelflux.aether.network.spi.NetworkCallback
import com.kernelflux.aether.network.spi.NetworkConfig
import com.kernelflux.fluxrouter.annotation.FluxService
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OkHttp网络客户端实现
 *
 * @author Aether Framework
 */
@FluxService(interfaceClass = INetworkClient::class)
class OkHttpNetworkClient : INetworkClient {
    private var okHttpClient: OkHttpClient? = null
    private var baseUrl: String = ""
    private val gson = Gson()


    override fun init(config: NetworkConfig) {
        this.baseUrl = config.baseUrl

        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(config.writeTimeout, TimeUnit.MILLISECONDS)
            .build()
    }

    override fun <T> get(
        url: String,
        params: Map<String, Any>,
        headers: Map<String, String>,
        responseType: Class<T>,
        callback: NetworkCallback<T>
    ) {
        val fullUrl = buildUrl(url, params)
        val request = Request.Builder()
            .url(fullUrl)
            .apply {
                headers.forEach { (key, value) ->
                    addHeader(key, value)
                }
            }
            .get()
            .build()

        okHttpClient?.newCall(request)?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string()
                    val data = gson.fromJson(body, responseType)
                    callback.onSuccess(data)
                } catch (e: Exception) {
                    callback.onError(e)
                }
            }
        })
    }

    override fun <T> post(
        url: String,
        body: Any?,
        headers: Map<String, String>,
        responseType: Class<T>,
        callback: NetworkCallback<T>
    ) {
        val fullUrl = buildUrl(url, emptyMap())
        val jsonBody = if (body != null) {
            gson.toJson(body).toRequestBody("application/json".toMediaType())
        } else {
            "{}".toRequestBody("application/json".toMediaType())
        }

        val request = Request.Builder()
            .url(fullUrl)
            .apply {
                headers.forEach { (key, value) ->
                    addHeader(key, value)
                }
            }
            .post(jsonBody)
            .build()

        okHttpClient?.newCall(request)?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    val data = gson.fromJson(responseBody, responseType)
                    callback.onSuccess(data)
                } catch (e: Exception) {
                    callback.onError(e)
                }
            }
        })
    }

    override fun <T> getSync(
        url: String,
        params: Map<String, Any>,
        headers: Map<String, String>,
        responseType: Class<T>
    ): T? {
        val fullUrl = buildUrl(url, params)
        val request = Request.Builder()
            .url(fullUrl)
            .apply {
                headers.forEach { (key, value) ->
                    addHeader(key, value)
                }
            }
            .get()
            .build()

        return try {
            val response = okHttpClient?.newCall(request)?.execute()
            val body = response?.body?.string()
            gson.fromJson(body, responseType)
        } catch (e: Exception) {
            null
        }
    }

    override fun <T> postSync(
        url: String,
        body: Any?,
        headers: Map<String, String>,
        responseType: Class<T>
    ): T? {
        val fullUrl = buildUrl(url, emptyMap())
        val jsonBody = if (body != null) {
            gson.toJson(body).toRequestBody("application/json".toMediaType())
        } else {
            "{}".toRequestBody("application/json".toMediaType())
        }

        val request = Request.Builder()
            .url(fullUrl)
            .apply {
                headers.forEach { (key, value) ->
                    addHeader(key, value)
                }
            }
            .post(jsonBody)
            .build()

        return try {
            val response = okHttpClient?.newCall(request)?.execute()
            val responseBody = response?.body?.string()
            gson.fromJson(responseBody, responseType)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildUrl(url: String, params: Map<String, Any>): HttpUrl {
        val httpUrl = if (url.startsWith("http")) {
            url.toHttpUrlOrNull()!!
        } else {
            "$baseUrl$url".toHttpUrlOrNull()!!
        }

        val urlBuilder = httpUrl.newBuilder()
        params.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value.toString())
        }

        return urlBuilder.build()
    }
}

