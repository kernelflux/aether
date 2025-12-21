package com.kernelflux.aether.network.impl.okhttp.converter

import com.google.protobuf.Message
import com.google.protobuf.MessageLite
import com.kernelflux.aether.network.api.DataConverter
import com.kernelflux.aether.network.api.DataFormat
import com.kernelflux.aether.network.api.ProtobufConverter
import java.lang.reflect.Method

/**
 * Protobuf 数据转换器实现
 * 支持 Protobuf 消息的序列化和反序列化
 * 
 * 注意：使用此转换器需要依赖 protobuf-java 库
 */
class ProtobufDataConverter : ProtobufConverter {
    
    override fun toBytes(data: Any): ByteArray {
        return try {
            when (data) {
                is Message -> data.toByteArray()
                is MessageLite -> data.toByteArray()
                is ByteArray -> data
                else -> {
                    // 尝试调用 toByteArray() 方法
                    val method = data.javaClass.getMethod("toByteArray")
                    method.invoke(data) as ByteArray
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to convert object to Protobuf bytes", e)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T> fromBytes(bytes: ByteArray, clazz: Class<T>): T {
        return try {
            if (bytes.isEmpty()) {
                throw IllegalArgumentException("Empty bytes cannot be converted")
            }
            
            // 尝试使用 parseFrom 方法
            val parseFromMethod = clazz.getMethod("parseFrom", ByteArray::class.java)
            parseFromMethod.invoke(null, bytes) as T
        } catch (e: Exception) {
            throw RuntimeException("Failed to convert Protobuf bytes to object", e)
        }
    }
    
    override fun toProtobuf(message: Any): ByteArray {
        return toBytes(message)
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T> fromProtobuf(bytes: ByteArray, messageClass: Class<T>): T {
        return fromBytes(bytes, messageClass)
    }
    
    override fun getSupportedFormat(): DataFormat {
        return DataFormat.PROTOBUF
    }
    
    override fun getContentType(): String {
        return "application/x-protobuf"
    }
    
    companion object {
        /**
         * 创建 Protobuf 转换器
         */
        @JvmStatic
        fun create(): ProtobufDataConverter {
            return ProtobufDataConverter()
        }
    }
}
