package com.kernelflux.aether.network.okhttp

import com.kernelflux.aether.network.api.*
import okhttp3.Interceptor as OkHttpInterceptor
import okhttp3.Request as OkHttpRequest
import okhttp3.Response as OkHttpResponse
import java.io.IOException

/**
 * OkHttp 拦截器适配器
 * 将 Aether 的 Interceptor 适配到 OkHttp 的 Interceptor
 *
 * 注意：拦截器应该只观察和修改请求/响应，不应该替换整个响应流
 * 如果拦截器需要修改响应，应该使用 Response.newBuilder() 来创建新响应
 */
class OkHttpInterceptorAdapter(
    private val interceptor: Interceptor,
    private val config: NetworkConfig
) : OkHttpInterceptor {

    override fun intercept(chain: OkHttpInterceptor.Chain): OkHttpResponse {
        val okHttpRequest = chain.request()
        val request = convertRequest(okHttpRequest, config)

        val chainAdapter = object : Chain {
            override fun request(): Request = request

            override fun proceed(request: Request): Response<*> {
                val newOkHttpRequest = convertToOkHttpRequest(request, okHttpRequest, config)
                val okHttpResponse = chain.proceed(newOkHttpRequest)
                return convertResponse(okHttpResponse, request)
            }
        }

        val response = interceptor.intercept(chainAdapter)
        if (response.request != request) {
            // 请求被修改了，需要重新执行
            val modifiedOkHttpRequest =
                convertToOkHttpRequest(response.request, okHttpRequest, config)
            return chain.proceed(modifiedOkHttpRequest)
        }
        return chain.proceed(okHttpRequest)
    }

    private fun convertRequest(
        okHttpRequest: OkHttpRequest,
        config: NetworkConfig
    ): Request {
        val url = okHttpRequest.url.toString()
        val method = when (okHttpRequest.method) {
            "GET" -> HttpMethod.GET
            "POST" -> HttpMethod.POST
            "PUT" -> HttpMethod.PUT
            "DELETE" -> HttpMethod.DELETE
            "PATCH" -> HttpMethod.PATCH
            "HEAD" -> HttpMethod.HEAD
            "OPTIONS" -> HttpMethod.OPTIONS
            else -> HttpMethod.GET
        }

        val headers = okHttpRequest.headers.toMultimap().mapValues { it.value.firstOrNull() ?: "" }
        val params = okHttpRequest.url.queryParameterNames.associateWith {
            okHttpRequest.url.queryParameter(it) ?: ""
        }

        return Request(
            url = url,
            method = method,
            headers = headers,
            params = params,
            body = null, // 拦截器中不解析 body，避免性能问题
            tag = okHttpRequest.tag()
        )
    }

    private fun convertToOkHttpRequest(
        request: Request,
        originalRequest: OkHttpRequest,
        config: NetworkConfig
    ): OkHttpRequest {
        val builder = originalRequest.newBuilder()

        // 更新 URL（如果变化）
        val newUrl = request.buildUrl(config.baseUrl)
        if (newUrl != originalRequest.url.toString()) {
            builder.url(newUrl)
        }

        // 更新方法（如果变化）
        if (request.method.name != originalRequest.method) {
            builder.method(request.method.name, originalRequest.body)
        }

        // 更新请求头（合并）
        request.headers.forEach { (key, value) ->
            builder.header(key, value)
        }

        return builder.build()
    }

    private fun convertResponse(
        okHttpResponse: OkHttpResponse,
        request: Request
    ): Response<*> {
        val code = okHttpResponse.code
        val message = okHttpResponse.message
        val headers = okHttpResponse.headers.toMultimap()
        val isFromCache = okHttpResponse.cacheResponse != null

        // 重要：使用 peekBody 来读取 body，这样不会消耗原始的 body
        // peekBody 会创建一个新的 ResponseBody，原始 body 仍然可用
        val bodyBytes = try {
            okHttpResponse.peekBody(Long.MAX_VALUE).bytes()
        } catch (_: IOException) {
            // 如果读取失败，返回空数组
            ByteArray(0)
        }

        return Response(
            data = bodyBytes,
            code = code,
            message = message,
            headers = headers,
            request = request,
            isFromCache = isFromCache,
            isSuccessful = code in 200..299
        )
    }
}
