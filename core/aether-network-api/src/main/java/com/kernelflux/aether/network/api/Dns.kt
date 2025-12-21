package com.kernelflux.aether.network.api

import java.net.InetAddress

/**
 * DNS 解析器
 */
interface DnsResolver {
    /**
     * 解析域名
     * @param hostname 主机名
     * @return IP 地址列表
     */
    fun lookup(hostname: String): List<InetAddress>
}

/**
 * 系统默认 DNS 解析器
 */
object SystemDnsResolver : DnsResolver {
    override fun lookup(hostname: String): List<InetAddress> {
        return InetAddress.getAllByName(hostname).toList()
    }
}

/**
 * 自定义 DNS 解析器（通过映射表）
 */
class CustomDnsResolver(
    private val hostMap: Map<String, List<InetAddress>>
) : DnsResolver {
    override fun lookup(hostname: String): List<InetAddress> {
        return hostMap[hostname] ?: SystemDnsResolver.lookup(hostname)
    }
}

/**
 * 自定义 DNS 解析器（通过回调函数）
 */
class CallbackDnsResolver(
    private val resolver: (String) -> List<InetAddress>
) : DnsResolver {
    override fun lookup(hostname: String): List<InetAddress> {
        return resolver(hostname)
    }
}
