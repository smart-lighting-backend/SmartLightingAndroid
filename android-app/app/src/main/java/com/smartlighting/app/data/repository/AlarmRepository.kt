package com.smartlighting.app.data.repository

import com.smartlighting.app.data.api.ApiService
import com.smartlighting.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(private val api: ApiService) {

    suspend fun getAlarmPage(
        pageNum: Int, pageSize: Int,
        status: String?, level: String?, type: String?,
        startTime: String?, endTime: String?
    ): Result<PageData<AlarmRecord>> = apiCall {
        api.getAlarmPage(pageNum, pageSize, status, level, type, startTime, endTime)
    }

    suspend fun getAlarmDetail(id: Long): Result<AlarmRecord> = apiCall {
        api.getAlarmDetail(id)
    }

    suspend fun handleAlarm(id: Long, remark: String? = null): Result<Unit> {
        return try {
            val response = api.handleAlarm(id, HandleAlarmRequest(remark))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("操作失败: ${response.code()} ${response.message()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAlarmStats(): Result<AlarmStats> = apiCall {
        api.getAlarmStats()
    }

    suspend fun getAlarmTrend(days: Int = 7): Result<List<AlarmTrendItem>> = apiCall {
        api.getAlarmTrend(days)
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
