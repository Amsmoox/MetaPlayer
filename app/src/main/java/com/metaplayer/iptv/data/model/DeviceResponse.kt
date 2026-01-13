package com.metaplayer.iptv.data.model

/**
 * API response models for device registration and info.
 */
data class RegisterDeviceRequest(
    val mac_address: String,
    val device_name: String? = null,
    val m3u_url: String? = null
)

data class RegisterDeviceResponse(
    val success: Boolean,
    val device: DeviceInfo?,
    val error: String? = null
)

data class DeviceInfo(
    val mac_address: String,
    val device_name: String,
    val m3u_url: String,
    val created: Boolean
)

data class DeviceInfoResponse(
    val mac_address: String,
    val device_name: String,
    val m3u_url: String,
    val is_active: Boolean,
    val created_at: String,
    val last_seen: String,
    val cache: CacheInfo
)

data class CacheInfo(
    val has_cache: Boolean,
    val cache_valid: Boolean,
    val cache_updated: String?,
    val cache_expires_in_hours: Int,
    val cache_size_bytes: Long
)
