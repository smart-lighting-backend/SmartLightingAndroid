package com.smartlighting.app.data.api

import com.smartlighting.app.data.model.*
import retrofit2.http.*

interface ApiService {

    // ── Auth ──
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @GET("/api/auth/me")
    suspend fun getCurrentUser(): ApiResponse<LoginResponse>

    // ── Dashboard ──
    @GET("/api/dashboard/stats")
    suspend fun getDashboardStats(): ApiResponse<DashboardStats>

    @GET("/api/dashboard/energy-trend")
    suspend fun getEnergyTrend(): ApiResponse<EnergyTrend>

    @GET("/api/dashboard/districts")
    suspend fun getDistricts(): ApiResponse<List<DistrictStatus>>

    // ── Devices ──
    @GET("/api/devices/page")
    suspend fun getDevicePage(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 30,
        @Query("keyword") keyword: String? = null,
        @Query("area") area: String? = null
    ): ApiResponse<PageData<Device>>

    @GET("/api/devices/{deviceId}")
    suspend fun getDeviceDetail(@Path("deviceId") deviceId: String): ApiResponse<Device>

    @POST("/api/devices")
    suspend fun createDevice(@Body device: CreateDeviceRequest): ApiResponse<Device>

    @PUT("/api/devices/{deviceId}")
    suspend fun updateDevice(
        @Path("deviceId") deviceId: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): ApiResponse<Device>

    @DELETE("/api/devices/{deviceId}")
    suspend fun deleteDevice(@Path("deviceId") deviceId: String): retrofit2.Response<Unit>

    @GET("/api/devices/map-locations")
    suspend fun getMapLocations(): MapLocationsResponse

    @GET("/api/devices/health/summary")
    suspend fun getHealthSummary(): ApiResponse<HealthSummary>

    @GET("/api/devices/{deviceId}/health")
    suspend fun getDeviceHealth(@Path("deviceId") deviceId: String): ApiResponse<DeviceHealth>

    @GET("/api/devices/{deviceId}/perception")
    suspend fun getDevicePerception(@Path("deviceId") deviceId: String): ApiResponse<Map<String, @JvmSuppressWildcards Any>>

    @POST("/api/devices/{deviceId}/control")
    suspend fun controlDevice(
        @Path("deviceId") deviceId: String,
        @Body request: ControlRequest
    ): retrofit2.Response<Unit>

    @GET("/api/devices/{deviceId}/control-history")
    suspend fun getControlHistory(
        @Path("deviceId") deviceId: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): ApiResponse<PageData<ControlHistoryItem>>

    @DELETE("/api/devices/{deviceId}/manual-lock")
    suspend fun unlockDevice(@Path("deviceId") deviceId: String): retrofit2.Response<Unit>

    // ── Batch Operations ──
    @POST("/api/devices/batch")
    suspend fun batchCreateDevices(@Body devices: List<CreateDeviceRequest>): ApiResponse<BatchCreateResult>

    @PUT("/api/devices/batch-turn-on")
    suspend fun batchTurnOn(@Body request: BatchDeviceRequest): ApiResponse<BatchOperationResult>

    @PUT("/api/devices/batch-turn-off")
    suspend fun batchTurnOff(@Body request: BatchDeviceRequest): ApiResponse<BatchOperationResult>

    @PUT("/api/devices/batch-enable")
    suspend fun batchEnable(@Body request: BatchDeviceRequest): ApiResponse<BatchOperationResult>

    @PUT("/api/devices/batch-disable")
    suspend fun batchDisable(@Body request: BatchDeviceRequest): ApiResponse<BatchOperationResult>

    // ── Telemetry ──
    @GET("/api/telemetry/latest/{deviceId}")
    suspend fun getLatestTelemetry(@Path("deviceId") deviceId: String): ApiResponse<LatestTelemetry>

    @POST("/api/telemetry/history")
    suspend fun getTelemetryHistory(@Body params: Map<String, @JvmSuppressWildcards Any>): ApiResponse<PageData<Map<String, Any?>>>

    // ── Alarms ──
    @GET("/api/alarms/page")
    suspend fun getAlarmPage(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
        @Query("status") status: String? = null,
        @Query("level") level: String? = null,
        @Query("type") type: String? = null,
        @Query("startTime") startTime: String? = null,
        @Query("endTime") endTime: String? = null
    ): ApiResponse<PageData<AlarmRecord>>

    @GET("/api/alarms/{id}")
    suspend fun getAlarmDetail(@Path("id") id: Long): ApiResponse<AlarmRecord>

    @PUT("/api/alarms/{id}/handle")
    suspend fun handleAlarm(
        @Path("id") id: Long,
        @Body request: HandleAlarmRequest
    ): retrofit2.Response<Unit>

    @GET("/api/alarms/stats")
    suspend fun getAlarmStats(): ApiResponse<AlarmStats>

    @GET("/api/alarms/trend")
    suspend fun getAlarmTrend(@Query("days") days: Int = 7): ApiResponse<List<AlarmTrendItem>>

    // ── Events ──
    @GET("/api/vision-events/page")
    suspend fun getVisionEvents(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("deviceId") deviceId: String? = null,
        @Query("eventType") eventType: String? = null
    ): ApiResponse<PageData<VisionEvent>>

    @GET("/api/voice-events/page")
    suspend fun getVoiceEvents(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("deviceId") deviceId: String? = null,
        @Query("type") type: String? = null,
        @Query("source") source: String? = null
    ): ApiResponse<PageData<VoiceEvent>>

    // ── Assistant ──
    @POST("/api/assistant/chat")
    suspend fun chat(@Body request: ChatRequest): ApiResponse<ChatResponse>

    @POST("/api/assistant/diagnose")
    suspend fun diagnose(@Body request: DiagnoseRequest): ApiResponse<ChatResponse>
}
