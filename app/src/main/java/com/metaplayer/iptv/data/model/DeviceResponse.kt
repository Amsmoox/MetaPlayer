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
    val m3u_url: String? = null,
    val created: Boolean
)

data class DeviceInfoResponse(
    val mac_address: String,
    val device_name: String,
    val m3u_url: String? = null,
    val is_active: Boolean,
    val created_at: String,
    val last_seen: String,
    val activation_status: ActivationStatus? = null,
    val warning: String? = null,
    val message: String? = null
)

data class PlaylistResponse(
    val m3u_url: String,
    val mac_address: String,
    val device_name: String? = null
)

data class ActivationStatus(
    val is_active: Boolean,
    val activation_type: String, // FREE_TRIAL, YEARLY, LIFETIME
    val expires_at: String?,
    val activated_at: String?,
    val created_at: String,
    val last_activity: String?,
    val days_remaining: Int?,
    val expired: Boolean
)

data class DeviceActivityResponse(
    val success: Boolean,
    val is_active: Boolean,
    val last_activity: String,
    val activation_status: ActivationStatus,
    val error: String? = null,
    val message: String? = null
)

data class DeviceStatusResponse(
    val success: Boolean,
    val mac_address: String,
    val device_name: String?,
    val is_active: Boolean,
    val activation_type: String,
    val expires_at: String?,
    val activated_at: String?,
    val created_at: String,
    val last_activity: String?,
    val days_remaining: Int?,
    val expired: Boolean,
    val error: String? = null,
    val message: String? = null
)

data class ActivateDeviceRequest(
    val activation_type: String // "YEARLY" or "LIFETIME"
)
