---
title: "ADR-009: UI Shell 与 Design System 架构"
type: decision
status: accepted
phase: 4
updated: 2026-06-19
summary: "确立 MainShellActivity + Fluent Design System + domain pages + feature state 四层 UI 架构；ActivityMain 降级为 ConfigFragment，详情路由兼容旧 Exception extra"
---

# ADR-009: UI Shell 与 Design System 架构

## 背景

Phase 3 已完成 Fluent/AppSnapShotor 视觉 token、透明状态栏 + inset、紧凑 Toolbar、Chip、搜索、列表行和包可见性提示；[ADR-005](005-settings-information-architecture.md) 已将配置 IA 修订为「配置 tab 单屏高密度」。[ADR-006](006-material3-toolchain.md) 仅说明 Material 3 主题 defer，未定义应用级 UI 壳层。

当前源码仍以 `ActivityMain` 单体承载：Launcher 入口、Toolbar、Xposed 状态条、包可见性提示、应用列表加载、prefs 写入、搜索过滤和菜单动作。Phase 4C 起将新增观测历史 UI，Phase 4D 会加入统计与单应用观测页。如果继续让 `ActivityMain` 演进为多 tab 容器，会把配置域、观测域、状态条、全局菜单和详情阅读器混在同一个 Activity 中。

需要在编码前确立 UI 架构边界，使 Phase 3 的视觉成果可复用，并避免 Phase 4C 壳层、历史 UI、统计 UI 一次性混杂实现。

## 决策

采用 **Shell + Design System + Domain Page + Feature State** 四层 UI 架构。

### 1. Shell 顶层

新增 `MainShellActivity` 作为 Phase 4C 起的 Launcher 唯一入口，负责：

- 应用级 `MaterialToolbar`
- Xposed 激活状态条与全局状态提示
- Phase 4C+ 的 2 项 BottomNavigation：**配置 | 观测**
- `FragmentContainerView` / NavHost
- status bar / navigation bar inset
- 按当前 domain 注入 Toolbar 菜单

`ShellViewModel` 只保存 Shell 级状态，例如底栏选中项、Xposed 激活状态和一次性全局事件。Shell 不直接读写 `package_list`，也不实现应用列表过滤或崩溃统计聚合。

### 2. Design System / common ui

把 Phase 3 已落地的 Fluent/AppSnapShotor 视觉 token 固化为 CrashCenter Design System。当前仍使用 Material Components + Fluent token；Material 3 / dynamic color 不作为 Shell 前置条件。

共享组件范围：

| 组件 | 用途 |
|------|------|
| `StatusBanner` | Xposed 激活/未激活状态 |
| `PermissionBanner` | 包可见性、记录关闭、ingest 提示 |
| `FilterChipRow` | 配置全局开关、hook 过滤、统计时间范围 |
| `DenseSearchField` | 应用列表和崩溃历史搜索 |
| `AppToggleRow` | 配置域 per-app hook Switch |
| `CrashEventRow` | 观测历史与单应用崩溃列表 |
| `EmptyState` / `LoadingState` | 列表空态与加载态 |
| `ToolbarHeaderInsets` | Shell 与详情 Activity 的统一 inset 处理 |

Design System 不持有业务状态，不拥有路由，不直接访问 prefs 或 `events.jsonl`。

### 3. Domain Page

`ActivityMain` 的配置页面内容迁为 `ConfigFragment`：

- `ConfigFragment` 负责绑定状态、搜索/Chip/列表视图、配置页菜单事件
- `ConfigViewModel` + `ConfigUiState` 管理加载、过滤、排序、scope mode、系统应用显示与包可见性
- `AppListRepository` 封装 `PackageManager`、`SharedPreferences` 和 `PackageVisibilityHelper`
- `AppToggleAdapter` 只渲染 `AppToggleRow` 并回调 toggle 事件

观测域由 `ObserveHostFragment` 承载：

- Phase 4C：`CrashHistoryFragment`
- Phase 4D：`CrashStatsFragment`
- 单应用观测：`PerAppCrashActivity`
- 详情：`ActivityCrashInfo` 扩展或 `CrashLogDetailActivity`
- 阅读器：`CrashLogViewerClient`

### 4. 路由与兼容

Phase 3 / 4B 仍保持 **0 tab 单屏配置**，不提前展示空底栏。Phase 4C 起引入 **2 bottom tab**（配置 | 观测），观测内使用页级历史/统计 TabLayout；不新增第三个 bottom tab。

Launcher 入口迁至 `MainShellActivity`。`ActivityMain` 不再作为长期主入口，可在迁移期保留为内部兼容 wrapper 或直接拆除页面职责。

详情路由统一为 `crash_detail`：

1. `crash_id`：Phase 4C+ 历史/单应用列表主路径，从 `events.jsonl` 读取 `CrashEvent.stackTrace`
2. `Exception`：兼容 Phase 3 通知直传 stack extra

保留 `Exception` 兼容不改变旧通知路径；未来 Phase 4E 通知优先传 `crash_id`，避免 Binder 传输大 stack。

## 后果

- **正面**：Shell、配置域、观测域、详情阅读器职责清晰；Phase 3 Fluent 视觉可复用到历史/统计/详情；Phase 4C 可先落 Shell 再接历史列表。
- **正面**：ADR-005 的配置单屏约束不被误读为「整个应用不能有壳层」；ADR-006 的 M3 defer 不阻塞壳层建设。
- **负面**：需要从 `ActivityMain` 抽出 `ConfigFragment`、state 和 repository；短期会有迁移期类名兼容。
- **负面**：Toolbar 菜单从 Activity 事件改为 Shell 分发，需要定义配置/观测菜单 ownership。
- **跟进**：Phase 4C roadmap 拆分为 Shell/Design System 迁移（α）与历史 UI（β），避免壳层、历史、统计混在一个任务中。
- **跟进（主题）**：Shell 与 Design System 组件须随系统 `UI_MODE_NIGHT` 切换，**v1 仅跟随系统 DayNight**（无应用内浅色/深色/跟随三态 toggle）。语义色 SSOT 见 [visual-language.md](../design/visual-language.md)；`values-night/colors.xml` 覆盖同名别名，**禁止**在深色模式复用浅色 status 容器 tint（须成对定义 bg/fg）。`MainShellActivity` 及详情 Activity 共用 `SystemBars`：除 status bar 外，**API 26+** 须同步 `isAppearanceLightNavigationBars`。CodeEditor `setDark(night)` 归属 **4C-β** 详情页，不纳入 Phase 3C。完整方案见 [dark-mode-theming.md](../architecture/dark-mode-theming.md)；若日后增加用户主题 override，须先写 **ADR-016**（预留编号，尚未创建）。

## 备选方案

| 方案 | 不选原因 |
|------|----------|
| 保持 `ActivityMain` 单体并在其中加底栏 | 配置、观测、全局状态与路由混杂；Phase 4D 统计会继续扩大单体 |
| 4C 先做独立历史 Activity，4D 再加 Shell | 会产生二次导航重构；历史/统计返回栈和 Toolbar 行为不稳定 |
| 直接照搬 AppSnapShotor 3-tab 壳层 | CrashCenter 顶级任务只有配置与观测；统计是观测内聚合视图，不应成为第三个 bottom tab |
| 先迁 Material 3 再做 Shell | M3 已由 ADR-006 defer；Shell 架构不依赖 dynamic color 或 M3 主题 |

## 相关文档

- [configuration-ui.md](../architecture/configuration-ui.md)
- [navigation-ia.md](../architecture/navigation-ia.md)
- [ui-routing.md](../architecture/ui-routing.md)
- [crash-stats-ui.md](../architecture/crash-stats-ui.md)
- [code-editor-porting.md](../architecture/code-editor-porting.md)
- [ADR-005](005-settings-information-architecture.md)
- [ADR-006](006-material3-toolchain.md)
- [phase3_ui_redesign.md](../../dev/roadmap/active/phase3_ui_redesign.md)
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md)
- [dark-mode-theming.md](../architecture/dark-mode-theming.md)
- [visual-language.md](../design/visual-language.md)
