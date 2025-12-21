package com.kernelflux.aether.network.impl.okhttp.converter

import com.google.gson.Gson
import com.kernelflux.aether.network.api.DataConverter
import com.kernelflux.aether.network.api.DataFormat

/**
 * 数据转换器工厂
 * 用于创建各种格式的数据转换器
 */
object DataConverterFactory {
    
    /**
     * 创建数据转换器
     * @param format 数据格式
     * @param gson 自定义 Gson 实例（仅用于 JSON 格式，可选）
     * @return 数据转换器实例
     */
    @JvmStatic
    fun create(format: DataFormat, gson: Gson? = null): DataConverter {
        return when (format) {
            DataFormat.JSON -> {
                if (gson != null) {
                    JsonDataConverter.create(gson)
                } else {
                    JsonDataConverter.create()
                }
            }
            DataFormat.PROTOBUF -> {
                ProtobufDataConverter.create()
            }
            DataFormat.XML -> {
                // TODO: 实现 XML 转换器
                throw UnsupportedOperationException("XML converter not yet implemented")
            }
            DataFormat.CUSTOM -> {
                throw IllegalArgumentException("CUSTOM format requires custom implementation")
            }
        }
    }
    
    /**
     * 创建 JSON 转换器
     * @param gson 自定义 Gson 实例（可选）
     * @return JSON 转换器
     */
    @JvmStatic
    fun createJson(gson: Gson? = null): DataConverter {
        return create(DataFormat.JSON, gson)
    }
    
    /**
     * 创建 Protobuf 转换器
     * @return Protobuf 转换器
     */
    @JvmStatic
    fun createProtobuf(): DataConverter {
        return create(DataFormat.PROTOBUF)
    }
}
