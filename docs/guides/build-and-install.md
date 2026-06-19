---
title: "构建与安装指南"
type: guide
status: accepted
phase: N/A
updated: 2026-06-19
summary: "Gradle 9.2.1 构建、version catalog、签名与 APK 安装"
---

# 构建与安装指南

## 前置条件

- JDK 17（见 [ADR-004](../decisions/004-build-toolchain-jdk17.md)；AppSnapShotor 用 21，CrashCenter 保留 17）
- Android SDK（compileSdk / targetSdk 36）
- Gradle 9.2.1（wrapper）；依赖版本见 `gradle/libs.versions.toml`
- Xposed 框架（LSPosed / EdXposed / 经典 Xposed）

## 构建

```bash
cd /path/to/CrashCenter
./gradlew :app:assembleRelease
# 或
./gradlew :app:assembleDebug
```

输出 APK 命名：`CrashCenter_v{versionName}_{buildType}.apk`（见 `app/build.gradle` `androidComponents`）。

版本号在 `app/build.gradle` 的 `versionCode`（递增整数）与 `versionName`（semver，如 `0.1.0`）。发布流程见 [release.md](release.md)。

## 签名配置

Release 构建当前使用 **debug 签名**（`signingConfig signingConfigs.debug`），与 CI 无 keystore 出包一致。本地 `assembleRelease` 无需额外 keystore。

若需生产签名，在 `app/build.gradle` 配置 `signingConfigs.release` 并通过环境变量或 CI secrets 注入。

## 安装

```bash
adb install -r app/build/outputs/apk/release/CrashCenter_v*.apk
```

无线调试示例（项目含 `mi10.ps1`）：

```powershell
adb connect 192.168.2.154:5555
```

## Xposed 激活

1. 在 LSPosed / EdXposed 管理器中启用模块
2. 选择作用域（或在 app 内配置 scope）
3. 重启目标 app
4. 打开 CrashCenter 确认「模块已激活」

## 相关文档

- [getting-started/INDEX.md](getting-started/INDEX.md) — 指南导航
- [usage.md](usage.md)
- [release.md](release.md) — GitHub Release 与 CHANGELOG
- [dev/DEV_GUIDE.md](../../dev/DEV_GUIDE.md)
