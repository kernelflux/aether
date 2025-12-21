package com.kernelflux.aether.network.impl.okhttp.crypto

import com.kernelflux.aether.network.api.Crypto
import com.kernelflux.aether.network.api.CryptoAlgorithm

/**
 * 加密解密器工厂
 * 用于创建各种加密算法的实现
 */
object CryptoFactory {
    
    /**
     * 创建加密解密器
     * @param algorithm 加密算法类型
     * @param key 密钥（AES 使用）
     * @param publicKey RSA 公钥（可选）
     * @param privateKey RSA 私钥（可选）
     * @return 加密解密器实例
     */
    @JvmStatic
    fun create(
        algorithm: CryptoAlgorithm,
        key: ByteArray? = null,
        publicKey: java.security.PublicKey? = null,
        privateKey: java.security.PrivateKey? = null
    ): Crypto {
        return when (algorithm) {
            CryptoAlgorithm.AES -> {
                require(key != null) { "AES requires a key" }
                AesCrypto(key)
            }
            CryptoAlgorithm.RSA -> {
                require(publicKey != null || privateKey != null) {
                    "RSA requires at least one key (public or private)"
                }
                RsaCrypto(publicKey, privateKey)
            }
            CryptoAlgorithm.DES -> {
                // TODO: 实现 DES 加密
                throw UnsupportedOperationException("DES encryption not yet implemented")
            }
            CryptoAlgorithm.DES3 -> {
                // TODO: 实现 3DES 加密
                throw UnsupportedOperationException("3DES encryption not yet implemented")
            }
            CryptoAlgorithm.SM4 -> {
                // TODO: 实现 SM4 国密加密
                throw UnsupportedOperationException("SM4 encryption not yet implemented")
            }
            CryptoAlgorithm.CUSTOM -> {
                throw IllegalArgumentException("CUSTOM algorithm requires custom implementation")
            }
        }
    }
    
    /**
     * 创建 AES 加密解密器（从字符串密码）
     * @param password 密码字符串
     * @param keySize 密钥长度（128, 192, 256）
     * @return AES 加密解密器
     */
    @JvmStatic
    fun createAesFromPassword(password: String, keySize: Int = 256): Crypto {
        val key = AesCrypto.keyFromPassword(password, keySize)
        return AesCrypto(key)
    }
    
    /**
     * 创建 RSA 加密解密器（从字节数组）
     * @param publicKeyBytes 公钥字节数组（X.509 格式）
     * @param privateKeyBytes 私钥字节数组（PKCS#8 格式，可选）
     * @return RSA 加密解密器
     */
    @JvmStatic
    fun createRsaFromBytes(
        publicKeyBytes: ByteArray? = null,
        privateKeyBytes: ByteArray? = null
    ): Crypto {
        val publicKey = publicKeyBytes?.let { RsaCrypto.publicKeyFromBytes(it) }
        val privateKey = privateKeyBytes?.let { RsaCrypto.privateKeyFromBytes(it) }
        return RsaCrypto(publicKey, privateKey)
    }
    
    /**
     * 创建空操作加密解密器（用于测试）
     * @return 空操作加密解密器
     */
    @JvmStatic
    fun createNoOp(): Crypto {
        return NoOpCrypto.INSTANCE
    }
}
