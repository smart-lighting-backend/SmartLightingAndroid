# 智慧路灯 Android 端

原生 Android Java 实现，当前联调地址：

```text
http://d5dfca47.natappfree.cc
```

如 natapp 地址变化，只需要修改：

```text
app/src/main/java/com/smartlighting/app/Constants.java
```

## 已实现模块

- 登录认证：`/api/auth/login`、`/api/auth/me`
- 数字孪生：首页统计、能耗趋势、分区设备状态、边缘 AI 状态、最近 AI 决策、手动触发边缘模拟
- 设备管理：设备分页列表、新增/编辑/删除、批量新增、批量分配区域、健康概览、设备详情、健康评分、最新遥测、遥测历史、手动控制、控制历史
- 数据报表：年度统计、月度能耗、分区能耗占比
- 智能助手：知识问答、设备一键诊断
- 告警中心：告警分页列表、告警详情、告警统计、告警趋势、ACTIVE 告警确认处理
- UI 优化：移动端运维仪表盘风格，统一卡片、按钮、状态标签、加载态、空状态和错误态

## 构建

```powershell
$env:GRADLE_USER_HOME='D:\codex-gradle-cache'
$env:JAVA_HOME='D:\jdk21'
$env:ANDROID_HOME='D:\A_tiku\android_app\sdk'
.\gradlew.bat assembleDebug
```

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 说明

当前后端实际分页接口与文档有两处差异：

- 设备列表使用 `GET /api/devices/page?pageNum=1&pageSize=30`
- 告警列表使用 `GET /api/alarms/page?pageNum=1&pageSize=30`

这两个接口已按公网后端实际行为适配。
