package com.kernelflux.aether.network.api

import java.security.cert.X509Certificate

/**
 * 证书校验器
 */
interface CertificateValidator {
    /**
     * 验证证书
     * @param certificates 证书链
     * @param hostname 主机名
     * @return 是否验证通过
     */
    fun validate(certificates: Array<X509Certificate>, hostname: String): Boolean
}

/**
 * 默认证书校验器（使用系统默认校验）
 */
object DefaultCertificateValidator : CertificateValidator {
    override fun validate(certificates: Array<X509Certificate>, hostname: String): Boolean {
        // 默认实现应该委托给系统校验
        // 具体实现由各个网络库的实现类提供
        return true
    }
}

/**
 * 固定证书校验器（固定信任某些证书）
 */
class FixedCertificateValidator(
    private val trustedCertificates: Set<X509Certificate>
) : CertificateValidator {
    override fun validate(certificates: Array<X509Certificate>, hostname: String): Boolean {
        return certificates.any { it in trustedCertificates }
    }
}

/**
 * 自定义证书校验器（通过回调函数）
 */
class CustomCertificateValidator(
    private val validator: (Array<X509Certificate>, String) -> Boolean
) : CertificateValidator {
    override fun validate(certificates: Array<X509Certificate>, hostname: String): Boolean {
        return validator(certificates, hostname)
    }
}
