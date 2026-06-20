---
title: "导航与信息架构"
type: architecture
status: accepted
phase: 4
updated: 2026-06-20
summary: "分阶段导航：Phase 3/4B 无 tab；Phase 4C+ 双底栏（配置 | 观测），观测内 TabLayout（历史 | 统计）；路由表见 ui-routing.md"
---

# 导航与信息架构

> 适用模块：`:app` 配置与观测 UI  
> 分析来源：Tab IA 分析（2026-06-19，会话 a0a7a9db）  
> 相关决策：[ADR-005](../decisions/005-settings-information-architecture.md)（配置 tab 单屏高密度）、[ADR-009](../decisions/009-ui-shell-design-system.md)（UI Shell 与 Design System）

## 概述

CrashCenter 产品叙事分为 **干预层**（hook 配置 + 吞异常）与 **观测层**（崩溃记录 + 统计），见 [crash-logging.md](crash-logging.md)。顶级导航按 **任务域** 划分，而非按设置项数量机械拆 tab。

**Activity / Fragment 路由表、Intent 参数、返回栈**见 **[ui-routing.md](ui-routing.md)**。

UI 架构按四层组织：

| 层 | IA 职责 |
|----|---------|
| **Shell** | 承载顶级导航：Toolbar、Xposed 状态条、2-tab BottomNavigation、WindowInsets 与全局菜单 |
| **Design System / common ui** | 固化 Fluent token 与通用列表/搜索/Chip/banner 组件，避免配置页和观测页各自造样式 |
| **Domain Page** | 配置、观测历史、统计、单应用观测、详情等页面各自组织内容 |
| **Feature State** | 每个页面只持有本域状态；Shell 状态不下沉到配置/观测列表 |

这一分层意味着：**tab 是 Shell 级结构，历史/统计是观测域内结构，设置项不是顶级导航结构**。

| 阶段 | 顶级 tab 数 | 形态 |
|------|-------------|------|
| Phase 3 / 4B | **0** | 单 `ActivityMain`，全部配置同屏 |
| Phase 4C+ | **2** | 底栏：**配置** \| **观测** |

---

## Phase 3 / 4B：0 tab（当前）

- **载体**：单一 `ActivityMain`（见 [configuration-ui.md](configuration-ui.md)）
- **内容**：Xposed 状态条、包可见性 banner、全局 FilterChip、搜索与过滤、per-app 列表、Toolbar 排序/批量/About/Test
- **4B**：`CrashLogger` MVP 仅后台写 JSONL，**无 UI**，导航不变
- **迁移约束**：Phase 3/4B 不提前引入空 bottom nav；可在代码内部预拆 `ConfigFragment` / state，但用户可见 IA 仍为 0 tab

此阶段完全符合 [ADR-005](../decisions/005-settings-information-architecture.md) 修订后的单屏高密度配置 IA。

---

## Phase 4C+：2 个 Bottom Tab

观测 UI 落地后引入 **2-tab 壳层**（非 3 tab）。推荐在 **4C-α** 建立 Shell 与 Design System 迁移，**4C-β** 接入历史列表；**4D** 在观测 tab 内增加「统计」子 tab，避免后续再加顶级 tab 的重构。

```
┌─────────────────────────────────┐
│ Toolbar + 溢出菜单               │
│ [壳层] Xposed 状态条（两 tab 共享）│
├─────────────────────────────────┤
│                                 │
│   Fragment 内容区                │
│                                 │
├─────────────────────────────────┤
│  [配置]  │  [观测]               │  ← Bottom Nav（2 项）
└─────────────────────────────────┘
```

### Shell 顶层

Shell 拥有所有跨 tab chrome：

- `MainShellActivity`：Launcher 入口；`FragmentContainerView` / NavHost；BottomNavigation 仅配置、观测两项
- `ShellViewModel`：Xposed 激活状态、底栏选中 tab、全局菜单事件、状态条提示
- `ToolbarHeaderInsets`：统一透明状态栏与 header inset
- Toolbar 菜单按当前 tab 注入：配置 tab 显示排序/全选/About/Test；观测 tab 显示清空、导出、记录设置

### Tab 1 — 配置

`ActivityMain` 内容迁为 `ConfigFragment`。**Phase 3G 前**保留当前紧凑布局；**3G 起**列表改为受管应用模型（[app-management-ui.md](app-management-ui.md)），仍符合 ADR-005 单屏高密度：

**当前（Legacy）**：

- Dense 搜索 + 全部/已应用/未应用 Chip + 应用计数
- RecyclerView 全量 per-app Switch

**Phase 3G 目标**：

- 包可见性 banner（条件显示）
- 全局 FilterChip：`scope_mode` / `handle_system` / `show_system_ui`
- Dense 搜索 + **全部/已启用/待配置** Chip + 受管应用计数
- RecyclerView **受管列表** + 行内 Switch + 状态角标
- Toolbar：**添加应用**（Half Sheet）、排序；移除全选/全不选
- L3：`AppInterventionEditActivity` 干预规则编辑

### Tab 2 — 观测

`ObserveHostFragment` 是观测 tab 的域宿主。内层 **`TabLayout`（页级，非 bottom nav）**：

| 子 tab | Phase | 内容 |
|--------|-------|------|
| **历史** | 4C | 时间倒序列表 → 按 `crash_id` 打开 **`CrashDetailBottomSheet`**（壳内半屏） |
| **统计** | 4D | 按包名/异常类 TOP N、日/周摘要；清空、retention — 详见 [crash-stats-ui.md](crash-stats-ui.md) |
| （Toolbar 菜单） | 4E | SAF 导出、`crash_log_enabled` 等运维项 |

观测域设置挂在观测 tab 的 Toolbar 或统计页脚，**不**增设第三个 bottom tab。

### Design System 复用约束

Design System 来自 Phase 3 已落地的 Fluent/AppSnapShotor token，但应从配置页面中抽出：

| 组件 | 配置 | 观测 | 详情 |
|------|------|------|------|
| `StatusBanner` | Shell 共享 | Shell 共享 | — |
| `PermissionBanner` | 包可见性 | 记录关闭 / ingest 提示 | — |
| `FilterChipRow` | scope / show system / hook filter | 时间范围 / 异常过滤 | — |
| `DenseSearchField` | app 搜索 | 历史搜索 | 可选查找入口仍用 CodeEditor |
| `AppToggleRow` | per-app Switch | — | — |
| `CrashEventRow` | 可选角标，不作主行 | 历史 / 单应用列表 | — |
| `CrashDetailBottomSheet` | — | 历史 / 单应用 / 统计下钻 | — |
| `CrashLogViewerClient` | — | — | 半屏 Sheet **与** 全屏 Activity 共用 |
| `EmptyState` / `LoadingState` | 列表 | 历史 / 统计 | Sheet / Activity 加载 |

### 壳层实现

视觉与 sibling 项目 **AppSnapShotor** 对齐时，可参考其主壳层模式（非 1:1 照搬 tab 数量）：

- 固定 `MaterialToolbar` + `FragmentContainerView` + 底栏
- 自定义 **`FloatingBottomNav`**（`BlurView` 悬浮胶囊、等宽 `ImageButton`），见 AppSnapShotor `docs/guides/getting-started/ui-shell.md`
- CrashCenter 底栏为 **2 项**（约 120dp 宽），列表区 `paddingBottom` 避让底栏
- 使用 `NavController` + `launchSingleTop` / `restoreState` 绑定 tab

完整 M3 主题见 [ADR-006](../decisions/006-material3-toolchain.md) defer；Shell/Design System 决策见 [ADR-009](../decisions/009-ui-shell-design-system.md)。Phase 4C 可用 Material Components + 现有 Fluent token 先行。

---

## 不应放进 Bottom Tab 的内容

| 内容 | 推荐载体 | 理由 |
|------|----------|------|
| Stack trace 详情（壳内） | **`CrashDetailBottomSheet`**（`crash_id`） | 历史 / 单应用 / 统计下钻；半屏 + CodeEditor；复用 [Draggable Half Sheet](../design/components/draggable-half-sheet.md) 规范 |
| Stack trace 详情（外部） | `ActivityCrashInfo` / `CrashLogDetailActivity`（`crash_id` / `Exception` extra） | 通知 PendingIntent、深链、跨任务栈入口；全屏深度阅读 |
| About / 使用警告 | Toolbar 对话框 | 低频；AppSnapShotor 同理不进 tab |
| 测试崩溃 | Toolbar 菜单 | 开发/验收，非用户主路径 |
| Xposed 管理器跳转 | 状态条点击 | 系统外链 |
| 包可见性授权 | 条件 banner + 对话框 | 阻塞型提示 |
| SAF 导出 | 系统 picker + 确认对话框 | 瞬时流程 |
| 排序 / 批量操作 | Toolbar 子菜单 | 配置域工具 |

---

## 与 AppSnapShotor 对比

| 维度 | AppSnapShotor | CrashCenter（推荐） |
|------|---------------|-------------------|
| Bottom tab 数 | **3**（存档 \| 时间线 \| 应用） | **2**（配置 \| 观测） |
| 设置 | 独立 `SettingsActivity`，Toolbar 进入 | 不进 tab；全局 hook 设置在 **配置 tab** 同屏 Chip（ADR-005） |
| Tab 内再分 tab | 应用 tab 内 `TabLayout`（用户/系统筛选） | **观测 tab** 内 `TabLayout`（历史 \| 统计） |
| 底栏组件 | `FloatingBottomNav` + 3× `ImageButton` | 可选同款壳层，**2 项** |
| 产品主轴 | 三条独立工作流（存档、时间线、应用配置） | 干预 vs 观测两层；统计为历史的聚合视图，不必独立顶级 tab |

**可借鉴**：壳层布局、悬浮底栏、内联 TabLayout、Toolbar 固定不随列表收起。  
**不照搬**：3 tab 数量；CrashCenter 观测层早期较轻，独立「统计」顶级 tab 价值不足（见 Phase 4D「Chip + 简单列表，无重型图表」）。

---

## ADR-005 澄清

[ADR-005](../decisions/005-settings-information-architecture.md) 的 **单屏高密度** 约束 **仅适用于配置 tab**：

- 全局设置与 per-app 列表须 **同屏可达**（FilterChip + RecyclerView）
- **不** 表示整个应用只能有一个 Activity 或禁止 Phase 4 观测域顶级导航
- Phase 4C+ 增加「观测」bottom tab 与 ADR-005 **不冲突**；历史/统计 **不应** 塞进配置 tab 下方（会占用列表空间并混淆干预/观测语义）

---

## 分阶段 rollout

| 阶段 | Tab 数 | 导航形态 | 备注 |
|------|--------|----------|------|
| **Phase 3**（当前） | 0 | 单 `ActivityMain` | ADR-005 单屏配置 |
| **Phase 4B** | 0 | 无 UI 变更 | `CrashLogger` 后台写盘 |
| **Phase 4C-α** | 0 → **2** | 引入 Shell + Design System + `ConfigFragment` | 不交付统计；观测可先空态 |
| **Phase 4C-β** | 2 | 观测 tab 接入 **历史** | 历史列表 → `CrashDetailBottomSheet`；通知仍走 Activity |
| **Phase 4D** | 2 | 观测 tab 内增加 **统计** 子 tab | 不增 top-level tab |
| **Phase 4E** | 2 | 导出 / retention 挂观测 Toolbar | 仍 2 tab |

**不采用的 4C 最小改动**：Toolbar 菜单 → 独立历史 Activity（0 tab），4D 再一次性上 2-tab 壳层。该方案会把历史 UI、统计 UI、Shell 迁移混在一起，且导致二次导航重构。

---

## 相关文档

- [ui-routing.md](ui-routing.md) — Activity/Fragment 路由与 NavGraph
- [configuration-ui.md](configuration-ui.md) — 配置 tab 布局与密度策略
- [crash-logging.md](crash-logging.md) — 观测层数据与 UI 需求
- [ADR-009: UI Shell 与 Design System 架构](../decisions/009-ui-shell-design-system.md)
- [ADR-005: 设置信息架构](../decisions/005-settings-information-architecture.md)
- [ADR-006: Material 3 工具链](../decisions/006-material3-toolchain.md)
- [crash-history-ui.md](crash-history-ui.md) — 历史列表与半屏详情导航
- [crash-event-timeline-ui.md](crash-event-timeline-ui.md) — 历史子页的 CrashEvent 时间线呈现
- [code-editor-porting.md](code-editor-porting.md) — `CrashLogViewerClient` 与 Sheet/Activity 双载体
- [design-system.md](design-system.md) — `CrashDetailBottomSheet` 观测域组件
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md)
- [phase3_ui_redesign.md](../../dev/roadmap/active/phase3_ui_redesign.md)
- AppSnapShotor 参考：`/home/clarence/Projects/Android/AppSnapShotor/docs/guides/getting-started/ui-shell.md`
