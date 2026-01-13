package com.metaplayer.iptv.data.api

import com.metaplayer.iptv.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for MetaPlayer backend.
 */
interface MetaPlayerApi {
    
    /**
     * Register device with backend.
     */
    @POST("api/devices/register/")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): Response<RegisterDeviceResponse>
    
    /**
     * Get device information.
     */
    @GET("api/devices/{mac_address}/info/")
    suspend fun getDeviceInfo(
        @Path("mac_address") macAddress: String
    ): Response<DeviceInfoResponse>
    
    /**
     * Get M3U playlist for device.
     */
    @GET("api/devices/{mac_address}/playlist.m3u")
    suspend fun getPlaylist(
        @Path("mac_address") macAddress: String,
        @Query("refresh") refresh: Boolean = false
    ): Response<ResponseBody>
    
    /**
     * Force refresh playlist cache.
     */
    @POST("api/devices/{mac_address}/refresh/")
    suspend fun refreshPlaylist(
        @Path("mac_address") macAddress: String
    ): Response<RegisterDeviceResponse>
}
