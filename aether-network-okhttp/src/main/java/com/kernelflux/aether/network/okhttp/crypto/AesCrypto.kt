package com.kernelflux.aether.network.impl.okhttp.crypto

import com.kernelflux.aether.network.api.Crypto
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES 加密解密实现
 * 支持 AES-128、AES-192、AES-256
 * 
 * 注意：生产环境使用时，密钥应该安全存储，不要硬编码
 */
class AesCrypto(
    private val key: ByteArray,
    private val algorithm: String = "AES/CBC/PKCS5Padding"
) : Crypto {
    
    private val keySpec: SecretKeySpec = SecretKeySpec(key, "AES")
    private val ivLength = 16 // AES block size
    
    init {
        require(key.size in listOf(16, 24, 32)) {
            "AES key must be 16, 24, or 32 bytes (128, 192, or 256 bits)"
        }
    }
    
    override fun encrypt(data: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance(algorithm)
            val iv = ByteArray(ivLength)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encrypted = cipher.doFinal(data)
            
            // 将 IV 和加密数据拼接：IV + EncryptedData
            ByteArray(iv.size + encrypted.size).apply {
                System.arraycopy(iv, 0, this, 0, iv.size)
                System.arraycopy(encrypted, 0, this, iv.size, encrypted.size)
            }
        } catch (e: Exception) {
            throw RuntimeException("AES encryption failed", e)
        }
    }
    
    override fun decrypt(encryptedData: ByteArray): ByteArray {
        return try {
            if (encryptedData.size < ivLength) {
                throw IllegalArgumentException("Encrypted data too short")
            }
            
            // 提取 IV 和加密数据
            val iv = ByteArray(ivLength)
            System.arraycopy(encryptedData, 0, iv, 0, ivLength)
            val encrypted = ByteArray(encryptedData.size - ivLength)
            System.arraycopy(encryptedData, ivLength, encrypted, 0, encrypted.size)
            
            val cipher = Cipher.getInstance(algorithm)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            
            cipher.doFinal(encrypted)
        } catch (e: Exception) {
            throw RuntimeException("AES decryption failed", e)
        }
    }
    
    override fun getAlgorithm(): String {
        return "AES"
    }
    
    companion object {
        /**
         * 生成随机 AES 密钥
         * @param keySize 密钥长度（128, 192, 256）
         * @return 密钥字节数组
         */
        @JvmStatic
        fun generateKey(keySize: Int = 256): ByteArray {
            return try {
                val keyGenerator = KeyGenerator.getInstance("AES")
                keyGenerator.init(keySize)
                keyGenerator.generateKey().encoded
            } catch (e: Exception) {
                throw RuntimeException("Failed to generate AES key", e)
            }
        }
        
        /**
         * 从字符串生成密钥（使用 SHA-256 哈希）
         * @param password 密码字符串
         * @param keySize 密钥长度（128, 192, 256）
         * @return 密钥字节数组
         */
        @JvmStatic
        fun keyFromPassword(password: String, keySize: Int = 256): ByteArray {
            return try {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(password.toByteArray())
                when (keySize) {
                    128 -> ByteArray(16).apply { System.arraycopy(hash, 0, this, 0, 16) }
                    192 -> ByteArray(24).apply { System.arraycopy(hash, 0, this, 0, 24) }
                    256 -> hash
                    else -> throw IllegalArgumentException("Invalid key size: $keySize")
                }
            } catch (e: Exception) {
                throw RuntimeException("Failed to generate key from password", e)
            }
        }
    }
}
