package com.smartlighting.app.data.repository

import com.smartlighting.app.data.api.ApiService
import com.smartlighting.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(private val api: ApiService) {

    suspend fun getDevicePage(pageNum: Int, pageSize: Int, keyword: String?, area: String? = null): Result<PageData<Device>> = apiCall {
        api.getDevicePage(pageNum, pageSize, keyword, area)
    }

    suspend fun getDeviceDetail(deviceId: String): Result<Device> = apiCall {
        api.getDeviceDetail(deviceId)
    }

    suspend fun createDevice(device: CreateDeviceRequest): Result<Device> = apiCall {
        api.createDevice(device)
    }

    suspend fun updateDevice(deviceId: String, updates: Map<String, Any>): Result<Device> = apiCall {
        api.updateDevice(deviceId, updates)
    }

    suspend fun deleteDevice(deviceId: String): Result<Unit> {
        return try {
            val response = api.deleteDevice(deviceId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("删除失败: ${response.code()} ${response.message()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMapLocations(): Result<List<DeviceMapLocation>> {
        return try {
            val response = api.getMapLocations()
            if (response.code == 200) Result.success(response.data)
            else Result.failure(Exception(response.msg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHealthSummary(): Result<HealthSummary> = apiCall {
        api.getHealthSummary()
    }

    suspend fun getDeviceHealth(deviceId: String): Result<DeviceHealth> = apiCall {
        api.getDeviceHealth(deviceId)
    }

    suspend fun getLatestTelemetry(deviceId: String): Result<LatestTelemetry> = apiCall {
        api.getLatestTelemetry(deviceId)
    }

    suspend fun controlDevice(deviceId: String, action: String, brightness: Int? = null): Result<Unit> {
        return try {
            val response = api.controlDevice(deviceId, ControlRequest(action, brightness))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("控制失败: ${response.code()} ${response.message()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getControlHistory(deviceId: String): Result<PageData<ControlHistoryItem>> = apiCall {
        api.getControlHistory(deviceId)
    }

    suspend fun unlockDevice(deviceId: String): Result<Unit> {
        return try {
            val response = api.unlockDevice(deviceId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("操作失败: ${response.code()} ${response.message()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDevicePerception(deviceId: String): Result<Map<String, Any?>> = apiCall {
        api.getDevicePerception(deviceId)
    }

    suspend fun batchCreateDevices(devices: List<CreateDeviceRequest>): Result<BatchCreateResult> = apiCall {
        api.batchCreateDevices(devices)
    }

    suspend fun batchTurnOn(ids: List<Long>): Result<BatchOperationResult> = apiCall {
        api.batchTurnOn(BatchDeviceRequest(deviceIds = ids))
    }

    suspend fun batchTurnOff(ids: List<Long>): Result<BatchOperationResult> = apiCall {
        api.batchTurnOff(BatchDeviceRequest(deviceIds = ids))
    }

    suspend fun batchEnable(ids: List<Long>): Result<BatchOperationResult> = apiCall {
        api.batchEnable(BatchDeviceRequest(deviceIds = ids))
    }

    suspend fun batchDisable(ids: List<Long>): Result<BatchOperationResult> = apiCall {
        api.batchDisable(BatchDeviceRequest(deviceIds = ids))
    }

    private suspend fun <T> apiCall(call: suspend () -> ApiResponse<T>): Result<T> {
        return try {
            val response = call()
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
