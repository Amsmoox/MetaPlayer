package com.metaplayer.iptv.data.util

import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.*

/**
 * Device Manager for handling MAC address generation and storage.
 * Generates a unique MAC address for the device and stores it in SharedPreferences.
 */
object DeviceManager {
    private const val PREFS_NAME = "metplayer_prefs"
    private const val KEY_MAC_ADDRESS = "device_mac_address"
    private const val KEY_DEVICE_NAME = "device_name"
    
    /**
     * Get or generate MAC address for this device.
     * Tries to get real MAC address, falls back to generated one stored in SharedPreferences.
     */
    fun getMacAddress(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Try to get stored MAC address first
        val storedMac = prefs.getString(KEY_MAC_ADDRESS, null)
        if (!storedMac.isNullOrBlank()) {
            return storedMac
        }
        
        // Try to get real MAC address
        val realMac = tryGetRealMacAddress(context)
        if (!realMac.isNullOrBlank()) {
            // Store it
            prefs.edit().putString(KEY_MAC_ADDRESS, realMac).apply()
            return realMac
        }
        
        // Generate a new MAC address
        val generatedMac = generateMacAddress(context)
        prefs.edit().putString(KEY_MAC_ADDRESS, generatedMac).apply()
        return generatedMac
    }
    
    /**
     * Try to get real MAC address from device.
     * Returns null if not available (Android 6.0+ restrictions).
     */
    private fun tryGetRealMacAddress(context: Context): String? {
        return try {
            // Method 1: Try WiFi MAC (requires permissions on Android 6.0+)
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.connectionInfo?.macAddress?.takeIf { it != "02:00:00:00:00:00" }
                ?: run {
                    // Method 2: Try network interface
                    getMacFromNetworkInterface()
                }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get MAC address from network interface.
     */
    private fun getMacFromNetworkInterface(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (interface_ in interfaces) {
                val mac = interface_.hardwareAddress
                if (mac != null && mac.isNotEmpty()) {
                    val sb = StringBuilder()
                    for (b in mac) {
                        sb.append(String.format("%02X:", b))
                    }
                    if (sb.isNotEmpty()) {
                        sb.deleteCharAt(sb.length - 1)
                    }
                    val macAddress = sb.toString()
                    // Filter out invalid MACs
                    if (macAddress != "02:00:00:00:00:00" && macAddress.length == 17) {
                        return macAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate a random MAC address in format XX:XX:XX:XX:XX:XX
     * Uses Android ID as seed for consistency across app reinstalls.
     */
    private fun generateMacAddress(context: Context? = null): String {
        val random = SecureRandom()
        
        // Use Android ID as seed for consistency if context is available
        if (context != null) {
            try {
                val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                if (!androidId.isNullOrBlank()) {
                    // Seed with Android ID hash for consistency
                    random.setSeed(androidId.hashCode().toLong())
                }
            } catch (e: Exception) {
                // If we can't get Android ID, use default random
            }
        }
        
        val macBytes = ByteArray(6)
        random.nextBytes(macBytes)
        
        // Set locally administered bit (second least significant bit of first octet)
        macBytes[0] = (macBytes[0].toInt() or 0x02).toByte()
        // Clear multicast bit (least significant bit of first octet)
        macBytes[0] = (macBytes[0].toInt() and 0xFE).toByte()
        
        return String.format(
            "%02X:%02X:%02X:%02X:%02X:%02X",
            macBytes[0],
            macBytes[1],
            macBytes[2],
            macBytes[3],
            macBytes[4],
            macBytes[5]
        )
    }
    
    /**
     * Get device name (stored or default).
     */
    fun getDeviceName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_NAME, null) 
            ?: android.os.Build.MODEL.takeIf { it.isNotBlank() }
            ?: "Android Device"
    }
    
    /**
     * Set device name.
     */
    fun setDeviceName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
    }
}
