package com.smartlighting.app.data.repository

import com.smartlighting.app.data.api.ApiService
import com.smartlighting.app.data.local.TokenManager
import com.smartlighting.app.data.model.LoginRequest
import com.smartlighting.app.data.model.LoginResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(username, password))
            if (response.code == 200 && response.data != null) {
                tokenManager.saveAuth(response.data.token, response.data.username)
                tokenManager.savePermissions(response.data.permissions.joinToString(","))
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<LoginResponse> {
        return try {
            val response = api.getCurrentUser()
            if (response.code == 200 && response.data != null) {
                tokenManager.saveAuth(response.data.token, response.data.username)
                tokenManager.savePermissions(response.data.permissions.joinToString(","))
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getToken(): String? = tokenManager.getToken()

    suspend fun logout() {
        tokenManager.clearAll()
    }
}
