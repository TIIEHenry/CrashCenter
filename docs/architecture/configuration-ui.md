---
title: "配置域 UI 架构"
type: architecture
status: accepted
phase: N/A
updated: 2026-06-21
summary: "配置域从 ActivityMain 单体演进为 MainShellActivity + ConfigFragment + ConfigUiState；复用 Fluent Design System 与 Phase 3 单屏 IA"
---

# 配置域 UI 架构

> 适用模块：`:app`
> 当前源码：`MainShellActivity.kt`、`ConfigFragment.kt`、`ActivityCrashInfo.kt`、`common/ui/adapter/`
> 目标源码：`MainShellActivity`、`ConfigFragment`、`ConfigViewModel`、`LegacyAppRepository` + `ManagedAppRepository`

## 概述

配置 UI 基于 **Material Components**，视觉语言对齐 sibling 项目 **AppSnapShotor** 的 Fluent Design token（Communication Blue、canvas 背景、4dp 控件圆角、扁平 Toolbar），在 **单一主屏** 完成全部配置：受管应用列表、作用域/系统应用全局设置、搜索与 Chip 过滤、Toolbar 排序，以及 Xposed 激活状态条与崩溃测试。

> **演进（ADR-015）**：列表改为 **受管应用策展** + **行内 Switch** + 干预规则编辑页 — 详见 [app-management-ui.md](app-management-ui.md)。当前源码仍为全量列表 + Switch（ADR-002）。

**信息架构决策（2026-06-19）**：用户要求「在一个界面配置所有应用」，故 **不** 将全局设置迁出至独立 Activity/BottomSheet（见 [ADR-005](../decisions/005-settings-information-architecture.md) 修订）。通过提高信息密度释放列表垂直空间。

**UI 架构决策（ADR-009）**：当前源码仍是 `ActivityMain` 单体；目标架构将其降级为配置域页面 `ConfigFragment`，由 `MainShellActivity` 承载全局 Toolbar、状态条、BottomNavigation、inset 与菜单。设计系统 token 和 common UI 组件从配置域抽出，供观测/详情域复用。

**导航阶段**：Phase 3/4B 仍为单屏配置（0 tab）。Phase 4C 起引入 **2 个 bottom tab**（配置 \| 观测），单屏约束 **仅适用于配置 tab** — 见 [navigation-ia.md](navigation-ia.md)。

---

## 目标四层架构

| 层 | 职责 | 目标类 / 包 |
|----|------|-------------|
| **Shell** | 应用级 Toolbar、Xposed 状态条、BottomNavigation、全局菜单、WindowInsets、跨 tab 状态保持 | `shell/MainShellActivity`、`ShellViewModel` |
| **Design System / common ui** | Fluent token、共享 banner、搜索、Chip、列表行、空态/加载态、Toolbar inset helper | `common/ui/*`、`common/design/*` |
| **Domain Page** | 配置域页面编排，不拥有全局壳层；只处理配置页控件与列表 | `config/ConfigFragment`、`ConfigListController`、`AppListRenderer`、`EmptyStateRenderer`、`PermissionBannerRenderer` |
| **Feature State** | 配置页可测试状态、应用列表加载、过滤、排序、prefs 写入 | `ConfigViewModel`、`ConfigViewModelDelegate`（接口）、`LegacyConfigViewModel` / `ManagedConfigViewModel`（子类）、`ConfigUiState`、`LegacyAppRepository` + `ManagedAppRepository` |

该分层只描述目标结构，不改变 Phase 3 当前行为。Phase 4C 壳层落地前，`ActivityMain` 可继续承载配置单屏，但新增实现应按目标层次拆分，避免继续扩大单体。

### Shell 边界

Shell 只处理跨域结构：

- `MainShellActivity` 是未来 Launcher 唯一入口；默认选中配置 tab
- `ShellViewModel` 管理 Xposed 激活状态、包可见性全局提示、底栏选中 tab 与一次性全局事件
- Toolbar 标题、状态条、全局 overflow 菜单和 system bar inset 由 Shell 拥有
- Shell 不读取/修改 `package_list`，也不实现应用列表过滤

### Design System / common ui 边界

共享组件应保持无业务写入副作用：

| 组件 | 复用范围 | 说明 |
|------|----------|------|
| `StatusBanner` | Shell | Xposed 激活/未激活；点击打开 Xposed 管理器 |
| `PermissionBanner` | 配置域、未来观测导入提示 | 包可见性或数据权限提示 |
| `FilterChipRow` | 配置、统计、单应用观测 | 横向 Chip 组，支持计数/选中态 |
| `DenseSearchField` | 配置、历史列表 | 名称/包名/异常过滤 |
| `AppToggleRow` | 配置 | per-app hook Switch 行 |
| `CrashEventRow` | 观测历史、单应用观测 | 崩溃事件行；配置域只可选显示角标，不复用为开关行 |
| `EmptyState` / `LoadingState` | 所有域 | 列表空态和加载态 |
| `ToolbarHeaderInsets` | Shell、详情 Activity | 统一 status bar inset 处理 |

Design System 固化 [ADR-006](../decisions/006-material3-toolchain.md) 的结论：当前沿用 Material Components + Fluent token，不把 Material 3 / dynamic color 作为 Phase 4C 壳层前置条件。

---

## 配置域目标模型

`ActivityMain` 现有职责拆为三块：

| 当前职责 | 目标归属 |
|----------|----------|
| Window / Toolbar / 状态条 / inset | `MainShellActivity` + `ShellViewModel` |
| 视图拼装、Chip/搜索/list 绑定、菜单响应 | `ConfigFragment` |
| 应用列表加载、过滤、排序、prefs 更新、包可见性状态 | `ConfigViewModel` + `LegacyAppRepository` / `ManagedAppRepository` |

### `ConfigUiState`

建议用单一状态对象驱动页面：

| 字段 | 说明 |
|------|------|
| `isLoading` | 应用列表加载中 |
| `allApps: List<AppItem>` / `visibleApps` | 原始列表与过滤后列表（Legacy 模式） |
| `managedApps` / `visibleManagedApps` | 受管列表与过滤结果（Managed 模式） |
| `managedFilter` | ALL / ENABLED / PENDING |
| `isLegacyMode` | `managed_packages == null`（由 `ManagedAppRepository.isLegacyMode()` 驱动） |
| `query` | 搜索关键字 |
| `hookFilter` | 全部 / 已应用 / 未应用（Legacy 模式） |
| `sortMode` | 名称 / 安装时间 / 更新时间 |
| `scopeMode` | `PREF_SCOPE_MODE` |
| `handleSystem` | hook 是否处理系统应用 |
| `showSystemUi` | UI 是否显示系统应用 |
| `packageVisibility` | granted / restricted / unknown |
| `emptyMessage: String?` | 无应用、无匹配、包可见性受限 |

### `LegacyAppRepository` + `ManagedAppRepository`

原单体 Repository（设计草案）已拆分为两个 Repository：

**`LegacyAppRepository`** 封装 `PackageManager`、`SharedPreferences` 和 `PackageVisibilityHelper`：

- `loadInstalledApps()`：后台读取全量已安装包
- `persistHookStates()`：写入 `package_list`
- `setScopeMode()` / `setHandleSystem()` / `setShowSystemUi()`：写入全局 prefs

**`ManagedAppRepository`** 封装受管包和干预规则：

- `readManagedPackageNames()` / `loadManagedApps()`
- `addManagedPackages()` / `removeManagedPackage()`
- `setInterventionEnabled()` — 行内 Switch
- `getProfile()` / `saveProfile()` — `intervention_rules` JSON
- `pruneUninstalled()`
- `detectPackageVisibility()` 归属 `PackageVisibilityRepository`（非 `ManagedAppRepository`）

两个 Repository 仍写模块私有 `SharedPreferences`；hook 侧继续通过 XSharedPreferences 读取（见 [scope-and-prefs.md](scope-and-prefs.md)）。

---

## 受管应用模型（Phase 3G 目标）

> **SSOT**：[app-management-ui.md](app-management-ui.md) · [ADR-015](../decisions/015-managed-apps-intervention-rules.md)  
> **当前源码**：仍为 `ActivityMain` 全量列表 + `package_list`（下文 ActivityMain 节为 **Legacy 验收基线**）。

### 与 Legacy 差异

| 项 | Legacy（ActivityMain） | Phase 3G（ConfigFragment） |
|----|------------------------|----------------------------|
| Repository | `LegacyAppRepository` | **`ManagedAppRepository`** |
| 列表数据源 | 全部已安装包 | **`managed_packages`** |
| Hook 写入 | `persistHookStates` → `package_list` | **`setInterventionEnabled` + rules JSON** |
| 添加入口 | 无 | Toolbar + **`AddManagedAppBottomSheet`** |
| 细配 | 无 | **`AppInterventionEditActivity`** |
| 过滤 | 全部 / 已应用 / 未应用 | 全部 / **已启用** / **待配置** |

### `ConfigUiState`（3G 扩展）

| 字段 | 说明 |
|------|------|
| `managedApps` / `visibleManagedApps` | 受管列表与过滤结果 |
| `managedFilter` | ALL / ENABLED / PENDING |
| `isLegacyMode` | `managed_packages == null` |

Legacy 字段 `apps` / `hookFilter` 在迁移完成后移除。

### `ManagedAppRepository`

取代 `LegacyAppRepository` 的 per-app 职责（全局 prefs 方法可保留或上提）：

- `loadManagedApps()` / `addManagedPackages()` / `removeManagedPackage()`
- `setInterventionEnabled()` — 行内 Switch
- `getProfile()` / `saveProfile()` — `intervention_rules` JSON
- `pruneUninstalled()`

---

## ActivityMain（Legacy 验收基线 — 已重构为 ConfigFragment）

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
- `isModuleActive()` 由 self hook 注入 true（见 [xposed-entry.md](xposed-entry.md)）

## 详情页关系

配置域不拥有崩溃详情页；详情属于 observe/detail domain。当前 `ActivityCrashInfo` 展示单次崩溃 stack trace，未来可演进为 `CrashLogDetailActivity` 或保留类名并扩展参数。

- 当前：`TextView` + `NestedScrollView`；计划以 celestailruler `CodeEditor` 替换为可查找/滚动编辑器 — [code-editor-porting.md](code-editor-porting.md)
- Intent extra：兼容 `Exception`（`Log.getStackTraceString`）；Phase 4E 起优先 `crash_id` — [ui-routing.md](ui-routing.md)
- `MaterialToolbar` + 返回导航；与主屏相同的透明状态栏 + `toolbarHeader` inset（`SystemBars`）

## 视觉设计（AppSnapShotor 对齐）

参考 AppSnapShotor `Theme.AppSnapshot` / Fluent token，映射至 Material Components 1.6（完整 M3 见 [ADR-006](../decisions/006-material3-toolchain.md) defer）。

| Token | 浅色 | 深色 |
|-------|------|------|
| Primary | `#0078D4` | `#479EF5` |
| Canvas | `#FAFAFA`（`background`） | `#202020` |
| Surface | `#FFFFFF`（Toolbar、卡片底） | `#2D2D2D` |
| 文本 Primary | `#242424` | `#FFFFFF` |
| 文本 Secondary | `#616161` | `#A0A0A0` |
| 文本 Tertiary | `#707070` | `#8A8A8A` |
| 描边 | `#E0E0E0` | `#3D3D3D` |
| 圆角 | Control 4dp（Chip、状态条、搜索）；Overlay 8dp（卡片） | 同左 |
| 状态栏 | 透明 + edge-to-edge；浅色 dark icons / 深色 light icons（`SystemBars`） | 同左 |
| Chip 选中 | `primary_container` 底 + primary 描边 + accent 文案 | 深色 container `#0A2E4A` |
| Switch | primary 轨道 tint（`switch_track_color`） | accent-dark 轨道 |
| 状态条 | success `#107C10` / warning `#8A6116` | success `#6CCB5F` / warning `#FCE100` |

**与 AppSnapShotor 的差异（刻意保留）**：单屏 FilterChip 全局设置、36dp 紧凑列表图标、Xposed 状态条、per-app Switch。

迭代记录：[appsnapshot-style-alignment-2026-06-19.md](../../dev/iterations/configuration-ui/appsnapshot-style-alignment-2026-06-19.md)

## 主题与资源

- `Theme.MaterialComponents.DayNight.NoActionBar` + `AppTheme.*` 控件样式；`values-night/colors.xml` 语义 token 覆盖 — 见 [dark-mode-theming.md](dark-mode-theming.md)
- 色板 / 形状：`colors.xml`、`shapes.xml`、`color/chip_*.xml`
- 矢量图标：`drawable/ic_*.xml`
- 密度尺寸：`dimens.xml`（`filter_horizontal_padding`、`fluent_corner_radius_control` 等）

ActivityMain / ActivityCrashInfo 使用 **ViewBinding**（`buildFeatures.viewBinding`）。

## 相关文档

- [navigation-ia.md](navigation-ia.md)
- [ui-routing.md](ui-routing.md)
- [scope-and-prefs.md](scope-and-prefs.md)
- [overview.md](overview.md)
- [guides/usage.md](../guides/usage.md)
- [app-management-ui.md](app-management-ui.md) — 受管应用添加/移除与干预规则编辑（ADR-015）
- [ADR-015: 受管应用与干预规则](../decisions/015-managed-apps-intervention-rules.md)
- [ADR-009: UI Shell 与 Design System 架构](../decisions/009-ui-shell-design-system.md)
- [ADR-005: 设置信息架构](../decisions/005-settings-information-architecture.md)
- [ADR-006: Material 3 与工具链升级](../decisions/006-material3-toolchain.md)
- [density-optimization-2026-06-19.md](../../dev/iterations/configuration-ui/density-optimization-2026-06-19.md)
- [appsnapshot-style-alignment-2026-06-19.md](../../dev/iterations/configuration-ui/appsnapshot-style-alignment-2026-06-19.md)
- [permission-flow-2026-06-19.md](../../dev/iterations/configuration-ui/permission-flow-2026-06-19.md)
