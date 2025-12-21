package com.kernelflux.aether.network.api

/**
 * 加密器接口
 */
interface Encryptor {
    /**
     * 加密数据
     * @param data 原始数据
     * @return 加密后的数据
     */
    fun encrypt(data: ByteArray): ByteArray
    
    /**
     * 获取加密算法名称
     */
    fun getAlgorithm(): String
}

/**
 * 解密器接口
 */
interface Decryptor {
    /**
     * 解密数据
     * @param encryptedData 加密的数据
     * @return 解密后的数据
     */
    fun decrypt(encryptedData: ByteArray): ByteArray
    
    /**
     * 获取解密算法名称
     */
    fun getAlgorithm(): String
}

/**
 * 加解密器（同时支持加密和解密）
 */
interface Crypto : Encryptor, Decryptor

/**
 * 加密算法类型
 */
enum class CryptoAlgorithm {
    /**
     * AES 加密
     */
    AES,
    
    /**
     * RSA 加密
     */
    RSA,
    
    /**
     * DES 加密
     */
    DES,
    
    /**
     * 3DES 加密
     */
    DES3,
    
    /**
     * SM4 加密（国密）
     */
    SM4,
    
    /**
     * 自定义加密
     */
    CUSTOM
}
