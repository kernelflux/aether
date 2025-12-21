package com.kernelflux.aether.network.impl.okhttp.converter

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kernelflux.aether.network.api.DataConverter
import com.kernelflux.aether.network.api.DataFormat
import java.nio.charset.StandardCharsets

/**
 * JSON 数据转换器实现
 * 基于 Gson 实现 JSON 序列化和反序列化
 */
class JsonDataConverter(
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .setPrettyPrinting()
        .create()
) : DataConverter {
    
    override fun toBytes(data: Any): ByteArray {
        return try {
            val json = gson.toJson(data)
            json.toByteArray(StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Failed to convert object to JSON bytes", e)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T> fromBytes(bytes: ByteArray, clazz: Class<T>): T {
        return try {
            if (bytes.isEmpty()) {
                throw IllegalArgumentException("Empty bytes cannot be converted")
            }
            val json = String(bytes, StandardCharsets.UTF_8)
            gson.fromJson(json, clazz) ?: throw IllegalStateException("Failed to parse JSON: null result")
        } catch (e: Exception) {
            throw RuntimeException("Failed to convert JSON bytes to object", e)
        }
    }
    
    override fun getSupportedFormat(): DataFormat {
        return DataFormat.JSON
    }
    
    override fun getContentType(): String {
        return "application/json; charset=utf-8"
    }
    
    companion object {
        /**
         * 创建默认的 JSON 转换器
         */
        @JvmStatic
        fun create(): JsonDataConverter {
            return JsonDataConverter()
        }
        
        /**
         * 创建自定义 Gson 的 JSON 转换器
         */
        @JvmStatic
        fun create(gson: Gson): JsonDataConverter {
            return JsonDataConverter(gson)
        }
    }
}
