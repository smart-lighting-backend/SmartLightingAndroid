package com.smartlighting.app.data.repository

import com.smartlighting.app.data.api.ApiService
import com.smartlighting.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(private val api: ApiService) {

    suspend fun getStats(): Result<DashboardStats> = apiCall { api.getDashboardStats() }
    suspend fun getEnergyTrend(): Result<EnergyTrend> = apiCall { api.getEnergyTrend() }
    suspend fun getDistricts(): Result<List<DistrictStatus>> = apiCall { api.getDistricts() }

    private suspend fun <T> apiCall(call: suspend () -> ApiResponse<T>): Result<T> {
        return try {
            val response = call()
            if (response.code == 200 && response.data != null) Result.success(response.data)
            else Result.failure(Exception(response.msg))
        } catch (e: Exception) { Result.failure(e) }
    }
}
