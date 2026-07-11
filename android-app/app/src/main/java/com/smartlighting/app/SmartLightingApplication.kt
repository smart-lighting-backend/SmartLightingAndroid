package com.smartlighting.app

import android.app.Application
import com.amap.api.maps.MapsInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartLightingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 高德地图初始化（隐私合规：需在用户同意隐私政策后调用）
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
    }
}
