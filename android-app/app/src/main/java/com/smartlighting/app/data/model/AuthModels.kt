package com.smartlighting.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "token") val token: String,
    @Json(name = "username") val username: String,
    @Json(name = "roleCode") val roleCode: String,
    @Json(name = "permissions") val permissions: List<String> = emptyList(),
    @Json(name = "menus") val menus: List<MenuItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MenuItem(
    @Json(name = "id") val id: Long,
    @Json(name = "parentId") val parentId: Long?,
    @Json(name = "name") val name: String,
    @Json(name = "permissionCode") val permissionCode: String?,
    @Json(name = "icon") val icon: String?,
    @Json(name = "path") val path: String?,
    @Json(name = "component") val component: String?,
    @Json(name = "sort") val sort: Int,
    @Json(name = "enabled") val enabled: Boolean,
    @Json(name = "children") val children: List<MenuItem> = emptyList()
)
