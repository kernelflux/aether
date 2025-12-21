package com.kernelflux.aether.network.api

/**
 * 数据格式类型
 */
enum class DataFormat {
    /**
     * JSON 格式
     */
    JSON,
    
    /**
     * Protobuf 格式
     */
    PROTOBUF,
    
    /**
     * XML 格式
     */
    XML,
    
    /**
     * 自定义格式
     */
    CUSTOM
}

/**
 * 数据转换器接口
 * 用于将对象转换为字节数组，或将字节数组转换为对象
 */
interface DataConverter {
    /**
     * 将对象转换为字节数组
     * @param data 对象
     * @return 字节数组
     */
    fun toBytes(data: Any): ByteArray
    
    /**
     * 将字节数组转换为对象
     * @param bytes 字节数组
     * @param clazz 目标类型
     * @return 对象
     */
    fun <T> fromBytes(bytes: ByteArray, clazz: Class<T>): T
    
    /**
     * 获取支持的数据格式
     */
    fun getSupportedFormat(): DataFormat
    
    /**
     * 获取 Content-Type
     */
    fun getContentType(): String
}

/**
 * Protobuf 数据转换器接口
 * 注意：使用此接口需要依赖 protobuf 库
 */
interface ProtobufConverter : DataConverter {
    /**
     * 将 Protobuf 消息转换为字节数组
     * @param message Protobuf 消息对象（需要实现 Message 接口）
     */
    fun toProtobuf(message: Any): ByteArray
    
    /**
     * 将字节数组转换为 Protobuf 消息
     * @param bytes 字节数组
     * @param messageClass 消息类型
     */
    fun <T> fromProtobuf(bytes: ByteArray, messageClass: Class<T>): T
}
