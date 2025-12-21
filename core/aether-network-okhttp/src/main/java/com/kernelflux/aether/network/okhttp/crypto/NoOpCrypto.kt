package com.kernelflux.aether.network.impl.okhttp.crypto

import com.kernelflux.aether.network.api.Crypto

/**
 * 空操作加密解密实现（用于测试或禁用加密）
 * 直接返回原始数据，不进行任何加密解密操作
 */
class NoOpCrypto : Crypto {
    
    override fun encrypt(data: ByteArray): ByteArray {
        return data.copyOf()
    }
    
    override fun decrypt(encryptedData: ByteArray): ByteArray {
        return encryptedData.copyOf()
    }
    
    override fun getAlgorithm(): String {
        return "NO_OP"
    }
    
    companion object {
        @JvmStatic
        val INSTANCE = NoOpCrypto()
    }
}
