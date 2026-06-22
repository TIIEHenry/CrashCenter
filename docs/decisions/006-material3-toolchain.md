---
title: "ADR-006: Material 3 Defer — 沿用 Material Components 2.x Fluent"
type: decision
status: archived
phase: 3
updated: 2026-06-22
summary: "已取代：Phase 3/4 曾 defer M3；2026-06-22 由 ADR-022 启动 minSdk 26 + 静态 M3 Fluent 迁移"
---

# ADR-006: Material 3 Defer — 沿用 Material Components 2.x Fluent

> **已归档**。2026-06-22 由 [ADR-022](022-material3-static-theme-minsdk26.md) 取代；实施见 [material3-migration.md](../architecture/material3-migration.md)。

## 状态（历史）

**Accepted**（2026-06-19）。构建工具链已于 ADR-004 升级至 compileSdk 36 / AGP 9.0.0 / Gradle 9.2.1；Phase 3/4 **正式沿用** Material Components **2.x** + Fluent 视觉（对齐 AppSnapShotor 色板与密度），**不** 迁移 Material 3 dynamic color / `Theme.Material3.*`。

## 背景

原草案假设 compileSdk 31、AGP 7.1、Gradle 7.2。2026-06-19 Gradle sync 后 toolchain 已现代；**剩余决策**仅为是否迁移 M3 主题 token / dynamic color。

## 当时 stack（ADR-004 后）

| 项 | 值 |
|---|---|
| compileSdk / targetSdk | 36 |
| AGP / Gradle | 9.0.0 / 9.2.1 |
| Kotlin | 2.3.0 |
| Material | 1.13.0（`libs.versions.toml`；1.14.0 需 minSdk 23，CrashCenter minSdk 21） |
| UI 范式 | ViewBinding + Material Components 2.x |

## 相关文档

- [ADR-022](022-material3-static-theme-minsdk26.md) — 取代本 ADR
- [material3-migration.md](../architecture/material3-migration.md)
- [ADR-004](004-build-toolchain-jdk17.md)
- [configuration-ui.md](../architecture/configuration-ui.md)
- [phase3_ui_redesign.md](../../dev/roadmap/active/phase3_ui_redesign.md)
