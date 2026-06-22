---
title: "Design System 架构"
type: architecture
status: accepted
phase: 4
updated: 2026-06-22
summary: "桥接 docs/design/ 视觉语言与 CrashCenter res/ 实现：Fluent token、共享 UI 组件、域复用规则；M3 静态主题见 material3-migration"
---

# Design System 架构

> 适用模块：`:app` 全域 UI
> 视觉规范：`docs/design/visual-language.md`、`docs/design/ui-modes.md`
> Shell 层：[navigation-ia.md](navigation-ia.md)、[ADR-009](../decisions/009-ui-shell-design-system.md)
> 主题决策：[ADR-022](../decisions/022-material3-static-theme-minsdk26.md)（M3 静态 Fluent）；[material3-migration.md](material3-migration.md)

## 概述

CrashCenter Design System 将 **Clarence 生态视觉语言**（Fluent token、信息流/工具密度 UI 模式）与 **Android 实现**（Material 3 静态主题 + Fluent token override）桥接，并定义可跨域复用的共享组件集合。

**当前 as-built**：Material Components 2.x（`Theme.MaterialComponents.DayNight`）。**目标**（ADR-022）：`Theme.Material3.DayNight` + 静态 Fluent 映射，**无** dynamic color。

**定位**：

| 层级 | 所有者 | 内容 |
|------|--------|------|
| `docs/design/` | Clarence 全局 | 视觉语言、色板、交互模式、组件族定义（平台无关） |
| **本文档** | CrashCenter | Android XML/Kotlin 实现映射、共享组件接口、域复用规则 |
| `res/` | `:app` | 实际 color/shape/style/layout XML |

## Token 映射

从 `docs/design/visual-language.md` 与 AppSnapShotor `Theme.AppSnapshot` 抽出的核心 token：

| Design Token | Android 实现 | 值 |
|--------------|-------------|-----|
| `color.primary` | `@color/primary` | `#0078D4`（Communication Blue） |
| `color.canvas` | `@color/background` | `#FAFAFA` |
| `color.surface` | `@color/surface` | `#FFFFFF` |
| `color.text.primary` | `@color/text_primary` | `#242424` |
| `color.text.secondary` | `@color/text_secondary` | `#616161` |
| `color.text.tertiary` | `@color/text_tertiary` | `#707070` |
| `color.border` | `@color/outline` | `#E0E0E0` |
| `color.semantic.success` | `@color/status_active` | `#107C10` |
| `color.semantic.warning` | `@color/status_inactive` | `#8A6116` |
| `shape.control` | `@dimen/fluent_corner_radius_control` | `4dp` |
| `shape.overlay` | `@dimen/corner_radius_overlay` | `8dp` |
| `density.chip_height` | `@dimen/chip_height` | `28dp` |
| `density.list_icon` | `@dimen/list_icon_size` | `36dp` |
| `density.search_height` | `@dimen/search_field_height` | Dense variant |

### 主题基类

**As-built**（M2）：

```xml
<style name="AppTheme" parent="Theme.MaterialComponents.DayNight.NoActionBar">
    <!-- Fluent aliases: colorPrimary, colorSurface, values-night/ -->
</style>
```

**目标**（M3 静态 Fluent，[material3-migration.md](material3-migration.md)）：

```xml
<style name="AppTheme" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="colorPrimary">@color/fluent_accent</item>
    <item name="colorSurface">@color/fluent_layer</item>
    <item name="colorOnSurface">@color/fluent_text_primary</item>
    <!-- 完整映射见 material3-migration §Fluent → M3 -->
</style>
```

**禁止 dynamic color**：不使用 `DynamicColors`、`harmonize*` 或壁纸取色（[ADR-022](../decisions/022-material3-static-theme-minsdk26.md)）。

## 共享组件清单

以下组件从 Phase 3 配置域抽出，供配置、观测、详情多域复用：

| 组件 | 类/布局 | 复用域 | 说明 |
|------|---------|--------|------|
| `StatusBanner` | `common/ui/StatusBanner.kt` | Shell | Xposed 激活/未激活状态条；点击跳转管理器 |
| `PermissionBanner` | `common/ui/PermissionBanner.kt` | 配置、观测 | 可配文案 + 操作按钮；包可见性 / 记录关闭提示 |
| `FilterChipRow` | `common/ui/FilterChipRow.kt` | 配置、统计 | 横向可滚动 Chip 组；支持计数、单选/多选态 |
| `DenseSearchField` | `common/ui/DenseSearchField.kt` | 配置、历史 | TextInputLayout Dense 变体 + 清除按钮 |
| `AppToggleRow` | `config/ManagedAppRow.kt` | 配置 | 受管 app 行 + 状态角标 + Switch；点击行体进编辑页；**不跨域** |
| `AddManagedAppBottomSheet` | `config/AddManagedAppBottomSheet.kt` | 配置 | Draggable Half Sheet 添加 Picker；**不跨域** |
| `CrashEventRow` | `common/ui/CrashEventRow.kt` | 历史、单应用观测 | 崩溃行：图标 + 名称 + 异常 + 时间 + source badge |
| `CrashDetailBottomSheet` | `observe/CrashDetailBottomSheet.kt` | 观测 | 壳内半屏 stack trace；Draggable Half Sheet + `CrashLogViewerClient`；对齐 `AddManagedAppBottomSheet` |
| `CrashLogViewerClient` | `view/CrashLogViewerClient.kt` | 观测详情 | CodeEditor 只读适配层；Sheet **与** Activity 双载体共用 |
| `EmptyState` | `common/ui/EmptyState.kt` | 所有域 | 图标 + 标题 + 副文案 + 可选操作按钮 |
| `LoadingState` | `common/ui/LoadingState.kt` | 所有域 | 居中 `CircularProgressIndicator` + 文案 |
| `ToolbarHeaderInsets` | `common/ui/ToolbarHeaderInsets.kt` | Shell、详情 | 统一 status bar + Toolbar inset padding |

### 组件边界规则

| 规则 | 说明 |
|------|------|
| 无业务副作用 | 共享组件不直接写 prefs、不调用 Repository；通过回调/Flow 通信 |
| 配置域专有组件不外流 | `ManagedAppRow`、`AddManagedAppBottomSheet`、scope FilterChip 仅配置域 |
| 观测域专有组件不外流 | `CrashStatsCard`（4D）、`CrashDetailBottomSheet` 仅观测域 |
| Half Sheet 成对参考 | `AddManagedAppBottomSheet`（配置 picker）与 `CrashDetailBottomSheet`（观测详情）共用 Draggable Half Sheet 规范；**不**混用 settings-card-detail-sheet |
| 命名约定 | `common/ui/` 包下组件须 `@JvmStatic` 或 `@Composable` 兼容（当前 View 体系） |

## 与 docs/design/ 的关系

```
docs/design/                          # Clarence 生态通用
├── visual-language.md               # 色板、排版、间距
├── ui-modes.md                      # 信息流 vs 工具密度
├── interaction-language.md          # 交互模式
└── components/
    ├── floating-chrome.md           # FloatingToolbar 族
    ├── form-controls.md             # Switch, Chip, Input
    └── draggable-half-sheet.md      # BottomSheet 规范

docs/architecture/design-system.md   # ← 本文档（CrashCenter 落地）
    └── res/ 实现 + 共享组件 API
```

**映射规则**：

1. `docs/design/` 定义 **what**（视觉与交互语义）
2. 本文档定义 **how**（Android 实现、资源命名、组件接口）
3. 新增视觉 token 须先更新 `docs/design/visual-language.md`，再更新本文档映射

## 实施节奏

| 阶段 | 工作 |
|------|------|
| Phase 3（已完成） | Fluent token 落地 `res/`；配置域组件就绪 |
| Phase 4C-α | 抽出 common UI 包；Shell 复用 StatusBanner、Insets |
| Phase 4C-β | `CrashEventRow` 在历史列表复用；`CrashDetailBottomSheet` + `CrashLogViewerClient` |
| Phase 4D | `FilterChipRow` 在统计页时间筛选复用；新增 `CrashStatsCard` |

## 相关文档

- [app-management-ui.md](app-management-ui.md) — Phase 3G 受管列表 / ManagedAppRow / Half Sheet
- [configuration-ui.md](configuration-ui.md) — 配置域 UI（Design System 消费方）
- [crash-history-ui.md](crash-history-ui.md) — 历史列表与半屏详情
- [code-editor-porting.md](code-editor-porting.md) — `CrashLogViewerClient` 双载体
- [ui-routing.md](ui-routing.md) — `crash_detail_sheet` 路由
- [crash-event-timeline-ui.md](crash-event-timeline-ui.md) — 时间线中的 `CrashEventRow` 与状态组件使用
- [crash-stats-ui.md](crash-stats-ui.md) — 统计页
- [navigation-ia.md](navigation-ia.md) — 观测/配置 tab IA
- [ADR-009](../decisions/009-ui-shell-design-system.md) — Shell + Design System 决策
- [ADR-022](../decisions/022-material3-static-theme-minsdk26.md) — M3 静态 Fluent + minSdk 26
- [material3-migration.md](material3-migration.md) — 分阶段实施
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md) — 4C-α 任务
