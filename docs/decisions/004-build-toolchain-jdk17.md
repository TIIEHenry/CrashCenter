---
title: "ADR-004: 构建工具链策略"
type: decision
status: accepted
phase: 3
updated: 2026-06-19
summary: "JDK 17 + Gradle 9.2.1 / AGP 9.0.0 / compileSdk 36；与 AppSnapShotor 对齐除 Java 版本"
---

# ADR-004: 构建工具链策略

## 背景

Phase 3 UI 验收需要稳定 `./gradlew :app:assembleDebug`。历史环境 Java 21 + Gradle 7.2 报错 `Unsupported class file major version 65`；`app/build.gradle` 曾含 Windows 硬编码签名路径，阻塞非 Windows 构建。

## 决策

**当前工具链（2026-06-19 定稿）**：

| 项 | 值 |
|---|---|
| Gradle | 9.2.1（wrapper） |
| AGP | 9.0.0 |
| Kotlin | 2.3.0 |
| compileSdk / targetSdk | 36 |
| minSdk | 21 |
| Java / Kotlin JVM | **17**（AppSnapShotor 用 21，CrashCenter 保留 17） |
| 依赖管理 | `gradle/libs.versions.toml` version catalog |

**JDK 策略**：开发机与 CI 使用 JDK 17 运行 Gradle（`org.gradle.java.home` 或 toolchain）。应用 bytecode 目标 JVM 17。

**签名**：Release 当前使用 debug 签名（`signingConfig signingConfigs.debug`），便于 CI 无 keystore 出包。生产签名通过环境变量 / GitHub secrets 可选配置，仓库内不硬编码绝对路径。

## 后果

| 正面 | 负面 |
|------|------|
| 与 AppSnapShotor 构建脚本结构一致 | 开发机须 JDK 17（非 21） |
| compileSdk 36 + 现代 AGP | Material 3 完整主题仍 defer（见 ADR-006） |
| 非 Windows 可构建 release | 单模块无 ABI split（与 AppSnapShotor 不同） |

## 相关文档

- [build-and-install.md](../guides/build-and-install.md)
- [release.md](../guides/release.md)
- [phase3_ui_redesign.md](../../dev/roadmap/active/phase3_ui_redesign.md)
- [ADR-006](006-material3-toolchain.md)
