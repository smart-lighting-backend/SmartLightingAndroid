package com.smartlighting.app.util

import com.smartlighting.app.BuildConfig

object Constants {
    const val BASE_URL = "http://p62ea58a.natappfree.cc"
    const val DATASTORE_NAME = "smart_lighting_prefs"

    // ── 高德地图 Key ──────────────────────────────────────────
    // 实际值存储在: android-app/secrets.properties（已加入 .gitignore，不上传 Git）
    // 新开发者: 复制 secrets.properties.example → secrets.properties 并填入 Key
    // 申请地址: https://console.amap.com/
    // ──────────────────────────────────────────────────────────
    val AMAP_KEY: String get() = BuildConfig.AMAP_KEY
    val AMAP_SECURITY_CODE: String get() = BuildConfig.AMAP_SECURITY_CODE
}
