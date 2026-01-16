package com.metaplayer.iptv.data.api

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * API Client configuration for MetaPlayer backend.
 */
object ApiClient {
    private const val IS_PRODUCTION = true
    
    private const val PROD_URL = "http://38.242.149.21/"
    private const val TEST_URL = "http://10.0.2.2:8000/"
    
    private val BASE_URL = if (IS_PRODUCTION) PROD_URL else TEST_URL
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // Shared DNS-over-HTTPS configuration with explicit IP bootstrapping
    private val dohClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    // Custom DNS resolver with DoH fallback to system DNS
    val dns = object : okhttp3.Dns {
        private val cloudflareDoh = DnsOverHttps.Builder()
            .client(dohClient)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(listOf(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1"),
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("8.8.4.4")
            ))
            .build()
        
        private val googleDoh = DnsOverHttps.Builder()
            .client(dohClient)
            .url("https://dns.google/dns-query".toHttpUrl())
            .bootstrapDnsHosts(listOf(
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("8.8.4.4")
            ))
            .build()
        
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                // Try Cloudflare DoH first
                cloudflareDoh.lookup(hostname)
            } catch (e: Exception) {
                // Fallback to system DNS
                try {
                    okhttp3.Dns.SYSTEM.lookup(hostname)
                } catch (e2: Exception) {
                    // If both fail, try Google DoH as last resort
                    try {
                        googleDoh.lookup(hostname)
                    } catch (e3: Exception) {
                        throw e3 // Re-throw if all methods fail
                    }
                }
            }
        }
    }
    
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .dns(dns) 
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: MetaPlayerApi = retrofit.create(MetaPlayerApi::class.java)
}
