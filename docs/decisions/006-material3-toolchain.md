---
title: "ADR-006: Material 3 Defer — 沿用 Material Components 2.x Fluent"
type: decision
status: accepted
phase: 3
updated: 2026-06-19
summary: "正式决策：Phase 3/4 沿用 Material Components 2.x + Fluent token；Material 3 完整主题明确 defer，不作为 Shell/Design System 前置"
---

# ADR-006: Material 3 Defer — 沿用 Material Components 2.x Fluent

## 状态

**Accepted**。构建工具链已于 ADR-004 升级至 compileSdk 36 / AGP 9.0.0 / Gradle 9.2.1；Phase 3/4 **正式沿用** Material Components **2.x** + Fluent 视觉（对齐 AppSnapShotor 色板与密度），**不** 迁移 Material 3 dynamic color / `Theme.Material3.*`。

## 背景

原草案假设 compileSdk 31、AGP 7.1、Gradle 7.2。2026-06-19 Gradle sync 后 toolchain 已现代；**剩余决策**仅为是否迁移 M3 主题 token / dynamic color。

## 当前 stack（ADR-004 后）

| 项 | 值 |
|---|---|
| compileSdk / targetSdk | 36 |
| AGP / Gradle | 9.0.0 / 9.2.1 |
| Kotlin | 2.3.0 |
| Material | 1.13.0（`libs.versions.toml`；1.14.0 需 minSdk 23，CrashCenter minSdk 21） |
| UI 范式 | ViewBinding + Material Components 2.x |

## 待决事项（启动 3E 时）

- M3 `Theme.Material3.*` 还是保留 Fluent M2 扩展
- dynamic color / WindowInsets 与现有 `SystemBars` 衔接
- minSdk 21 下 M3 组件兼容矩阵

## 相关文档

- [ADR-004](004-build-toolchain-jdk17.md)
- [configuration-ui.md](../architecture/configuration-ui.md)
- [phase3_ui_redesign.md](../../dev/roadmap/active/phase3_ui_redesign.md)
