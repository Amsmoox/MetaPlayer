package com.metaplayer.iptv.data.repository

import android.content.Context
import com.metaplayer.iptv.data.api.ApiClient
import com.metaplayer.iptv.data.model.*
import com.metaplayer.iptv.data.util.DeviceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Repository for device registration and backend communication.
 */
class DeviceRepository(private val context: Context) {
    private val api = ApiClient.api
    
    /**
     * Register device with backend.
     */
    suspend fun registerDevice(m3uUrl: String? = null): Result<DeviceInfo> = withContext(Dispatchers.IO) {
        try {
            val macAddress = DeviceManager.getMacAddress(context)
            val deviceName = DeviceManager.getDeviceName(context)
            
            val request = RegisterDeviceRequest(
                mac_address = macAddress,
                device_name = deviceName,
                m3u_url = m3uUrl
            )
            
            val response = api.registerDevice(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val deviceInfo = response.body()!!.device
                if (deviceInfo != null) {
                    Result.success(deviceInfo)
                } else {
                    Result.failure(Exception("Device info is null"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Registration failed: $errorBody"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP error: ${e.code()} - ${e.message()}"))
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get device information from backend.
     */
    suspend fun getDeviceInfo(): Result<DeviceInfoResponse> = withContext(Dispatchers.IO) {
        try {
            val macAddress = DeviceManager.getMacAddress(context)
            val response = api.getDeviceInfo(macAddress)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Failed to get device info: $errorBody"))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("HTTP error: ${e.code()} - ${e.message()}"))
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get M3U playlist URL from backend.
     */
    suspend fun getM3UUrl(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val deviceInfo = getDeviceInfo()
            deviceInfo.fold(
                onSuccess = { info ->
                    if (info.m3u_url.isNotBlank()) {
                        Result.success(info.m3u_url)
                    } else {
                        Result.failure(Exception("M3U URL not set for this device"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update M3U URL for device.
     */
    suspend fun updateM3UUrl(m3uUrl: String): Result<DeviceInfo> = withContext(Dispatchers.IO) {
        registerDevice(m3uUrl)
    }
}
