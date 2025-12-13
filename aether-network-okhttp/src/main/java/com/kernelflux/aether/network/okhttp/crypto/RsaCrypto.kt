package com.kernelflux.aether.network.impl.okhttp.crypto

import com.kernelflux.aether.network.api.Crypto
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * RSA 加密解密实现
 * 支持 RSA/ECB/PKCS1Padding
 * 
 * 注意：
 * - RSA 加密有长度限制，大数据需要分段加密
 * - 生产环境使用时，密钥应该安全存储，不要硬编码
 */
class RsaCrypto(
    private val publicKey: PublicKey? = null,
    private val privateKey: PrivateKey? = null
) : Crypto {
    
    private val algorithm = "RSA/ECB/PKCS1Padding"
    private val maxEncryptBlockSize: Int
    private val maxDecryptBlockSize: Int
    
    init {
        require(publicKey != null || privateKey != null) {
            "At least one key (public or private) must be provided"
        }
        
        // RSA 密钥长度（假设 2048 位）
        val keySize = (publicKey ?: privateKey)?.let {
            when {
                it is java.security.interfaces.RSAPublicKey -> it.modulus.bitLength()
                it is java.security.interfaces.RSAPrivateKey -> it.modulus.bitLength()
                else -> 2048
            }
        } ?: 2048
        
        // PKCS1Padding 的最大块大小
        maxEncryptBlockSize = (keySize / 8) - 11
        maxDecryptBlockSize = keySize / 8
    }
    
    override fun encrypt(data: ByteArray): ByteArray {
        require(publicKey != null) { "Public key is required for encryption" }
        
        return try {
            if (data.size <= maxEncryptBlockSize) {
                // 数据小于最大块大小，直接加密
                val cipher = Cipher.getInstance(algorithm)
                cipher.init(Cipher.ENCRYPT_MODE, publicKey)
                cipher.doFinal(data)
            } else {
                // 数据大于最大块大小，分段加密
                encryptLargeData(data)
            }
        } catch (e: Exception) {
            throw RuntimeException("RSA encryption failed", e)
        }
    }
    
    override fun decrypt(encryptedData: ByteArray): ByteArray {
        require(privateKey != null) { "Private key is required for decryption" }
        
        return try {
            if (encryptedData.size <= maxDecryptBlockSize) {
                // 数据小于最大块大小，直接解密
                val cipher = Cipher.getInstance(algorithm)
                cipher.init(Cipher.DECRYPT_MODE, privateKey)
                cipher.doFinal(encryptedData)
            } else {
                // 数据大于最大块大小，分段解密
                decryptLargeData(encryptedData)
            }
        } catch (e: Exception) {
            throw RuntimeException("RSA decryption failed", e)
        }
    }
    
    /**
     * 分段加密大数据
     */
    private fun encryptLargeData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        
        val output = java.io.ByteArrayOutputStream()
        var offset = 0
        
        while (offset < data.size) {
            val blockSize = minOf(maxEncryptBlockSize, data.size - offset)
            val block = ByteArray(blockSize)
            System.arraycopy(data, offset, block, 0, blockSize)
            
            val encrypted = cipher.doFinal(block)
            output.write(encrypted)
            
            offset += blockSize
        }
        
        return output.toByteArray()
    }
    
    /**
     * 分段解密大数据
     */
    private fun decryptLargeData(encryptedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        
        val output = java.io.ByteArrayOutputStream()
        var offset = 0
        
        while (offset < encryptedData.size) {
            val blockSize = minOf(maxDecryptBlockSize, encryptedData.size - offset)
            val block = ByteArray(blockSize)
            System.arraycopy(encryptedData, offset, block, 0, blockSize)
            
            val decrypted = cipher.doFinal(block)
            output.write(decrypted)
            
            offset += blockSize
        }
        
        return output.toByteArray()
    }
    
    override fun getAlgorithm(): String {
        return "RSA"
    }
    
    companion object {
        /**
         * 从字节数组创建公钥
         * @param keyBytes 公钥字节数组（X.509 格式）
         * @return PublicKey
         */
        @JvmStatic
        fun publicKeyFromBytes(keyBytes: ByteArray): PublicKey {
            return try {
                val keySpec = X509EncodedKeySpec(keyBytes)
                val keyFactory = KeyFactory.getInstance("RSA")
                keyFactory.generatePublic(keySpec)
            } catch (e: Exception) {
                throw RuntimeException("Failed to create public key from bytes", e)
            }
        }
        
        /**
         * 从字节数组创建私钥
         * @param keyBytes 私钥字节数组（PKCS#8 格式）
         * @return PrivateKey
         */
        @JvmStatic
        fun privateKeyFromBytes(keyBytes: ByteArray): PrivateKey {
            return try {
                val keySpec = PKCS8EncodedKeySpec(keyBytes)
                val keyFactory = KeyFactory.getInstance("RSA")
                keyFactory.generatePrivate(keySpec)
            } catch (e: Exception) {
                throw RuntimeException("Failed to create private key from bytes", e)
            }
        }
        
        /**
         * 生成 RSA 密钥对
         * @param keySize 密钥长度（1024, 2048, 4096）
         * @return 密钥对
         */
        @JvmStatic
        fun generateKeyPair(keySize: Int = 2048): java.security.KeyPair {
            return try {
                val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
                keyPairGenerator.initialize(keySize)
                keyPairGenerator.generateKeyPair()
            } catch (e: Exception) {
                throw RuntimeException("Failed to generate RSA key pair", e)
            }
        }
    }
}
