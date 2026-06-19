---
title: "配置界面设计方案"
type: architecture
status: accepted
phase: N/A
updated: 2026-06-19
summary: "ActivityMain 单屏高密度 Fluent/Material UI：AppSnapShotor 视觉对齐、透明状态栏 + inset、Chip 全局设置、搜索与列表、包可见性手动授权"
---

# 配置界面设计方案

> 适用模块：`:app`
> 源码：`ActivityMain.kt`、`ActivityCrashInfo.java`、`recyclerhelper/`

## 概述

配置 UI 基于 **Material Components**，视觉语言对齐 sibling 项目 **AppSnapShotor** 的 Fluent Design token（Communication Blue、canvas 背景、4dp 控件圆角、扁平 Toolbar），在 **单一主屏** 完成全部配置：per-app hook 开关、作用域/系统应用全局设置、搜索与 Chip 过滤、Toolbar 排序与批量操作，以及 Xposed 激活状态条与崩溃测试。

**信息架构决策（2026-06-19）**：用户要求「在一个界面配置所有应用」，故 **不** 将全局设置迁出至独立 Activity/BottomSheet（见 [ADR-005](../decisions/005-settings-information-architecture.md) 修订）。通过提高信息密度释放列表垂直空间。

**导航（Phase 4+ defer）**：当前 Phase 3/4B 为单 `ActivityMain`（0 tab）。观测 UI 落地后计划 **2 个 bottom tab**（配置 \| 观测），单屏约束 **仅适用于配置 tab** — 见 [navigation-ia.md](navigation-ia.md)。

## ActivityMain

### 布局结构

| 区域 | 组件 | 说明 |
|------|------|------|
| 顶栏 | 白底 `MaterialToolbar` + 1dp 分隔线 | 标题 17sp medium + 溢出菜单（排序、全选、关于、测试） |
| 状态条 | 单行：盾牌图标 + 文案 | 激活/未激活；约 32dp 高；**点击**打开 LSPosed / EdXposed / 经典 Xposed 管理器（多框架回退，失败时弹 `xposed_hint`） |
| 包可见性条 | 单行：应用网格图标 + 文案 + 「去授权」 | **Android 11+** 且 `QUERY_ALL_PACKAGES` 未生效或列表被过滤时显示；点击或按钮 → 说明对话框 → 跳转本应用 **App 信息**；`onResume` 返回后自动重载列表 |
| 全局设置 | 横向 `FilterChip` ×3 | 作用域模式、处理系统应用、显示系统应用；长按「作用域」Chip 显示说明 |
| 搜索 | `TextInputLayout` Dense | 按名称/包名实时过滤 |
| 过滤 | 横向 `ChipGroup` + 应用计数 | 全部 / 已应用 / 未应用；计数右对齐同行 |
| 列表 | `RecyclerView` 扁平项 | 36dp 图标、名称+包名、系统标签、内联 Switch |

### 信息密度策略

| 项 | 原 Material v1 | 当前 |
|----|----------------|------|
| 全局设置 | 大卡片 + 3× Switch + 说明段落 | 单行可滚动 FilterChip，scope 说明改长按 |
| 状态条 | 双行标题+副文案 | 单行合并文案 |
| 列表项 | `MaterialCardView` 48dp 图标 | 扁平行 36dp 图标、更小 padding |
| 搜索 | 标准 OutlinedBox | Dense 变体 |
| Chip | 32dp 高 | 28dp 高、12sp 文案 |

### 应用列表

- 后台线程加载已安装包（`applicationContext` + `Handler`）；`PackageManager` 调用包在 try/catch 内
- 加载中显示列表区 `CircularProgressIndicator`；无匹配结果时显示空状态文案
- `App` 数据类：label、icon、get_it、packageName、isSystem、时间戳
- 列表项不再展示安装/更新时间（排序仍可用）
- 点击行切换 hook → `updatePref()` 写入 `package_list`

### 包可见性（QUERY_ALL_PACKAGES）

Manifest 声明 `android.permission.QUERY_ALL_PACKAGES`（Android 11+ 正常权限，**无** `requestPermissions()` 系统对话框）。实现见 `PackageVisibilityHelper`：

| 检测 | 说明 |
|------|------|
| API &lt; 30 | 视为完整可见，不显示授权条 |
| `checkSelfPermission(QUERY_ALL_PACKAGES)` | 未授予 → 需用户手动操作 |
| 探测包 `com.android.settings` | 不可见 → 列表被 package visibility 过滤 |
| 加载后启发式 | 可见包数明显少于 Launcher Activity 数 → 提示授权 |

**用户流程（手动申请）**：

1. 启动时检测；受限则显示 **包可见性条**（仍可加载部分列表）
2. 用户点击条或「去授权」→ `AlertDialog` 说明原因与步骤
3. 「打开设置」→ `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`（本应用 App 信息）
4. 用户在系统 UI 中开启「查询所有软件包」等效权限（OEM 文案可能不同）
5. 返回主界面 `onResume` → 重新检测并重载应用列表

不追加 dangerous 权限；不静默假定已授权。

### Toolbar 菜单 (`menu_main.xml`)

| 菜单项 | 功能 |
|--------|------|
| Sort | 按名称/安装时间/更新时间排序 |
| Select all / Deselect all | 批量切换 hook |
| About | 显示 `using_warning` |
| Test | 触发测试崩溃 |

> 作用域模式、系统应用相关开关在主屏 **设置 Chip 行**；搜索与 hook 状态过滤在主屏控件区。

### Xposed 状态检测

- 顶部 **状态条** 常驻展示激活/未激活（单行）
- 未激活时额外弹出 `AlertDialog` 提醒（保留原 `xposed_hint` 全文）
- `isModuleActived()` 由 self hook 注入 true（见 [xposed-entry.md](xposed-entry.md)）

## ActivityCrashInfo

展示单次崩溃 stack trace。

- 当前：`TextView` + `NestedScrollView`；计划以 celestailruler `CodeEditor` 替换为可查找/滚动编辑器 — [code-editor-porting.md](code-editor-porting.md)
- Intent extra：`Exception`（`Log.getStackTraceString`）— [crash-notification.md](crash-notification.md)
- `MaterialToolbar` + 返回导航；与主屏相同的透明状态栏 + `toolbarHeader` inset（`SystemBars`）

## 视觉设计（AppSnapShotor 对齐）

参考 AppSnapShotor `Theme.AppSnapshot` / Fluent token，映射至 Material Components 1.6（完整 M3 见 [ADR-006](../decisions/006-material3-toolchain.md) defer）。

| Token | 值 / 用法 |
|-------|-----------|
| Primary | `#0078D4`（Fluent Communication Blue） |
| Canvas | `#FAFAFA`（`background` / 窗口底） |
| Surface | `#FFFFFF`（Toolbar、卡片底） |
| 文本 | Primary `#242424`、Secondary `#616161`、Tertiary `#707070` |
| 描边 | `#E0E0E0`（Chip、搜索框、Toolbar 分隔） |
| 圆角 | Control 4dp（Chip、状态条、搜索）；Overlay 8dp（卡片） |
| 状态栏 | 透明 + edge-to-edge；`toolbarHeader` 应用 status bar inset；浅色主题 dark icons（`SystemBars`） |
| Chip 选中 | `primary_container` 底 + primary 描边 + accent 文案 |
| Switch | primary 轨道 tint（`switch_track_color`） |
| 状态条 | success `#107C10` / warning `#8A6116` 语义色 |

**与 AppSnapShotor 的差异（刻意保留）**：单屏 FilterChip 全局设置、36dp 紧凑列表图标、Xposed 状态条、per-app Switch。

迭代记录：[appsnapshot-style-alignment-2026-06-19.md](../../dev/iterations/configuration-ui/appsnapshot-style-alignment-2026-06-19.md)

## 主题与资源

- `Theme.MaterialComponents.Light.NoActionBar` + `AppTheme.*` 控件样式
- 色板 / 形状：`colors.xml`、`shapes.xml`、`color/chip_*.xml`
- 矢量图标：`drawable/ic_*.xml`
- 密度尺寸：`dimens.xml`（`filter_horizontal_padding`、`fluent_corner_radius_control` 等）

ActivityMain / ActivityCrashInfo 使用 **ViewBinding**（`buildFeatures.viewBinding`）。

## 相关文档

- [navigation-ia.md](navigation-ia.md)
- [scope-and-prefs.md](scope-and-prefs.md)
- [overview.md](overview.md)
- [guides/usage.md](../guides/usage.md)
- [ADR-005: 设置信息架构](../decisions/005-settings-information-architecture.md)
- [density-optimization-2026-06-19.md](../../dev/iterations/configuration-ui/density-optimization-2026-06-19.md)
- [appsnapshot-style-alignment-2026-06-19.md](../../dev/iterations/configuration-ui/appsnapshot-style-alignment-2026-06-19.md)
- [permission-flow-2026-06-19.md](../../dev/iterations/configuration-ui/permission-flow-2026-06-19.md)
