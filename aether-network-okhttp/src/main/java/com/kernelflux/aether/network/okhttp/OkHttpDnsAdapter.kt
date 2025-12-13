package com.kernelflux.aether.network.okhttp

import com.kernelflux.aether.network.api.DnsResolver
import okhttp3.Dns
import java.net.InetAddress

/**
 * OkHttp DNS 适配器
 * 将 Aether 的 DnsResolver 适配到 OkHttp 的 Dns
 */
class OkHttpDnsAdapter(
    private val dnsResolver: DnsResolver
) : Dns {
    
    override fun lookup(hostname: String): List<InetAddress> {
        return dnsResolver.lookup(hostname)
    }
}
