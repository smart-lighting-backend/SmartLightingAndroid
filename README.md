# 📱 SmartLightingAndroid — 智慧路灯移动端

智慧路灯 IoT 管理平台的 Android 移动端应用，使用 **Kotlin + Jetpack Compose** 现代 Android 技术栈开发，覆盖设备管理、实时控制、告警、能耗报表、AI 智能运维全流程。

配套后端：[SmartLightingExp](https://github.com/smart-lighting-backend/SmartLightingExp)（Spring Boot）· Web 前端：[smart_lighting_vue](https://github.com/smart-lighting-backend/smart_lighting_vue)

## 🚀 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin（JVM 17） |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM（data / di / ui / util 分层） |
| 依赖注入 | Hilt (Dagger 2.51) |
| 网络 | Retrofit 2.11 + OkHttp 4.12 + Moshi |
| 导航 | Navigation Compose 2.8 |
| 本地存储 | DataStore Preferences |
| 地图 | 高德地图 3D Map SDK 10.0 |
| 构建 | Gradle KTS，minSdk 24 / targetSdk 35 |

## ✨ 功能页面（11 个 Screen）

| 页面 | 说明 |
|------|------|
| Dashboard | 设备概览、在线率、告警统计 |
| DeviceList / DeviceForm | 设备台账、增删改、批量导入 |
| DeviceDetail | 设备详情、实时遥测、健康评分、手动控制 |
| BatchImport | Excel 批量导入设备 |
| AlarmList / AlarmDetail | 告警列表、详情处理 |
| Analytics | 能耗统计、节能率、碳减排报表 |
| Assistant | AI 维修诊断对话（RAG） |
| Event | AI 视觉/语音事件 |
| Login | JWT 登录认证 |

## 🏗️ 项目结构

```
android-app/app/src/main/java/com/smartlighting/app/
├── data/
│   ├── api/          # Retrofit 接口（ApiService，33 个接口）+ NetworkModule
│   ├── local/        # DataStore 本地缓存
│   ├── model/        # 数据模型（Moshi 解析）
│   └── repository/   # 仓库层（网络数据统一出口）
├── di/               # Hilt 依赖注入模块
├── ui/               # Compose 页面（navigation/theme/components + 11 个 Screen）
└── util/             # 工具类
```

## 🛠️ 构建与运行

### 环境要求

- Android Studio（建议最新稳定版）
- JDK 17
- Android SDK 35

### 配置

```bash
# 1. 复制 secrets 模板并填入高德 Key
cp secrets.properties.example secrets.properties
# 填写 AMAP_KEY 和 AMAP_SECURITY_CODE（高德开放平台申请）

# 2. 配置后端地址（Constants.kt 中 BASE_URL）
```

### 构建

```bash
# 调试包
./gradlew assembleDebug

# 正式包
./gradlew assembleRelease
```

## 🔗 接口对接

移动端通过 33 个 REST 接口对接后端（详见后端仓库 `docs/移动端接口文档.md`），使用 JWT Bearer Token 认证，支持 natapp 内网穿透 / ECS 公网两种环境。

## 📄 许可证

MIT License
