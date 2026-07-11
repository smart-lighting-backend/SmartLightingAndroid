package com.smartlighting.app.data.repository

import com.smartlighting.app.data.api.ApiService
import com.smartlighting.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(private val api: ApiService) {

    suspend fun getVisionEvents(page: Int, size: Int, deviceId: String?, eventType: String?): Result<PageData<VisionEvent>> = apiCall {
        api.getVisionEvents(page, size, deviceId, eventType)
    }

    suspend fun getVoiceEvents(page: Int, size: Int, deviceId: String?, type: String?, source: String?): Result<PageData<VoiceEvent>> = apiCall {
        api.getVoiceEvents(page, size, deviceId, type, source)
    }

    suspend fun chat(message: String): Result<ChatResponse> = apiCall {
        api.chat(ChatRequest(message))
    }

    suspend fun diagnose(deviceId: String, question: String?): Result<ChatResponse> = apiCall {
        api.diagnose(DiagnoseRequest(deviceId, question))
    }

    private suspend fun <T> apiCall(call: suspend () -> ApiResponse<T>): Result<T> {
        return try {
            val response = call()
            if (response.code == 200 && response.data != null) Result.success(response.data)
            else Result.failure(Exception(response.msg))
        } catch (e: Exception) { Result.failure(e) }
    }
}
