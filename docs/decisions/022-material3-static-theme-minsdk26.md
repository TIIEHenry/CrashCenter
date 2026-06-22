---
title: "ADR-022: Material 3 静态 Fluent 主题与 minSdk 26"
type: decision
status: accepted
phase: 3
updated: 2026-06-22
summary: "minSdk 升至 26；UI 迁移 Theme.Material3.DayNight + Fluent 静态语义色；明确拒绝 dynamic color / 壁纸取色；取代 ADR-006 defer"
---

# ADR-022: Material 3 静态 Fluent 主题与 minSdk 26

## 状态

**Accepted**。取代 [ADR-006](006-material3-toolchain.md)（已归档）中的 M3 defer 决策。

## 背景

Phase 3 在 Material Components **2.x** 上完成了 Fluent token、DayNight 暗色与 Shell / Design System（[ADR-009](009-ui-shell-design-system.md)）。ADR-006 将完整 M3 迁移 defer，主因是 minSdk 21 与 dynamic color 未定。

2026-06-22 确定启动 **3E Material 3** 时：

- 需解锁 Material 1.14+ 与 M3 Widget 默认样式；
- 生态视觉 SSOT（[visual-language.md](../design/visual-language.md)）要求 **Communication Blue 固定品牌色** 与 **轻量通透**（拒绝 tonal 深 elevation、拒绝壁纸 harmonize）；
- Xposed / LSPosed 目标用户设备普遍 ≥ Android 8。

## 决策

### 1. minSdk 26（Android 8.0）

全模块统一 `minSdk 26`：

| 模块 | 路径 |
|------|------|
| `:app` | `app/build.gradle` |
| `:ui_common` | `lib/ui_common/build.gradle` |
| `:CodeEditor` / `:CodeEditorClient` / `:CodeEditorAntlr` | `lib/CodeEditor*/build.gradle` |

**理由**：

| 考量 | 说明 |
|------|------|
| Material 库 | 1.14+ 要求 minSdk ≥ 23；26 留余量 |
| 系统栏 | `SystemBars` 中 `isAppearanceLightNavigationBars` 在 API 26+ 无条件可用 |
| 通知 | 崩溃反馈通知渠道已在 API 26+ 基线假设下实现 |
| 受众 | API 21–25 在 LSPosed 用户群中占比可忽略 |

**不做**：为保留 API 21–25 维护 M2 回退主题或双轨资源。

### 2. Material 3 主题 — 静态 Fluent 映射

- `AppTheme` parent 改为 `Theme.Material3.DayNight.NoActionBar`。
- 色值来源 **仅** `values/colors.xml` 与 `values-night/colors.xml` 中的 Fluent palette；映射到 M3 语义槽（`colorPrimary`、`colorSurface`、`colorOnSurface`、`colorOutline` 等）。
- Widget 默认样式 parent 迁移为 `Widget.Material3.*`；Fluent 圆角 / 密度通过 `styles.xml`、`shapes.xml`、`dimens.xml` **显式 override**。

实施细节见 [material3-migration.md](../architecture/material3-migration.md)。

### 3. 明确拒绝 dynamic color

以下 **禁止** 出现在 CrashCenter UI 进程：

| 禁止项 | 说明 |
|--------|------|
| `DynamicColors.applyToActivitiesIfAvailable()` | 壁纸 / 系统取色 |
| `Theme.Material3.DynamicColors.*` | M3 动态主题 parent |
| `MaterialColors.harmonize()` / `harmonizeWith()` | 色板 harmonize |
| 运行时从 `WallpaperManager` / `WallpaperColors` 推导主题 | 与 Fluent SSOT 冲突 |

深浅色 **仅** 跟随系统 `uiMode`（现有 DayNight + `values-night/`），与应用内主题三态（[dark-mode-theming.md](../architecture/dark-mode-theming.md) §2.10 defer）无关。

### 4. UI 范式不变

- 保持 **ViewBinding + XML + Fragment Shell**（ADR-009）。
- **不** 引入 Jetpack Compose 作为本 ADR 范围。
- hook / scope / prefs 语义不变。

### 5. 依赖

- `com.google.android.material:material` 升至 **1.14.0+**（`gradle/libs.versions.toml`）。
- `lib/ui_common` 改由 version catalog 引用，与 `:app` 同版本。

## 后果

### 正面

- M3 Widget 默认行为与长期 Material 库维护对齐；
- minSdk 26 简化 API 分支与测试矩阵；
- Fluent 品牌色在浅/深模式下可预测、可截图回归。

### 负面

- 放弃 API 21–25 安装支持；须在 Release notes 标明。
- M3 默认 Chip / Switch 尺寸可能与 Fluent 规格略有偏差，需逐组件 override（见 migration 文档 §高风险点）。

### 回滚

实施前打 `pre-m3-theme` tag；回滚 = 恢复 M2 `Theme.MaterialComponents.*` + minSdk 21（仅紧急）。

## 相关文档

- [material3-migration.md](../architecture/material3-migration.md) — 分阶段实施与 token 对照
- [design-system.md](../architecture/design-system.md) — Design System 消费方
- [dark-mode-theming.md](../architecture/dark-mode-theming.md) — DayNight 语义 token
- [ADR-006](006-material3-toolchain.md) — 已归档（defer）
- [ADR-004](004-build-toolchain-jdk17.md) — 构建工具链
- [phase3_ui_redesign.md](../../dev/roadmap/active/phase3_ui_redesign.md) — §3E 任务清单
