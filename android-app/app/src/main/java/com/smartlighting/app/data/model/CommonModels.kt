package com.smartlighting.app.data.model

import com.squareup.moshi.Json

data class ApiResponse<T>(
    @Json(name = "code") val code: Int,
    @Json(name = "msg") val msg: String,
    @Json(name = "data") val data: T? = null
)

data class PageData<T>(
    @Json(name = "records") val records: List<T> = emptyList(),
    @Json(name = "total") val total: Long = 0,
    @Json(name = "size") val size: Long = 0,
    @Json(name = "current") val current: Long = 0,
    @Json(name = "pages") val pages: Long = 0
)
