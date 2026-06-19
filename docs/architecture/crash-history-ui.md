---
title: "崩溃历史 UI 架构"
type: architecture
status: accepted
phase: 4
updated: 2026-06-19
summary: "Phase 4C-β CrashHistoryFragment：时间倒序列表、字段、筛选、空态、CrashLogRepository 读取契约"
---

# 崩溃历史 UI 架构

> 适用模块：`:app` 观测域 UI（Phase 4C-β）
> 数据来源：[crash-data-layer.md](crash-data-layer.md) — `CrashLogRepository`
> Shell 承载：[navigation-ia.md](navigation-ia.md) — 观测 tab → 历史子 tab
> 路由：[ui-routing.md](ui-routing.md) — L2 `CrashHistoryFragment`、壳内 `CrashDetailBottomSheet`、外部 `CrashLogDetailActivity`
> 相关决策：[ADR-009](../decisions/009-ui-shell-design-system.md)

## 概述

崩溃历史 UI 是观测 tab 的首要子页面，向用户呈现 **所有被 CrashHandler 拦截的崩溃事件**（`events.jsonl`），按时间倒序排列。用户可搜索、筛选、点击进入 **半屏详情 Sheet** 查看完整 stack trace。

**Hybrid dual-carrier**：壳内列表点击 → `CrashDetailBottomSheet`；通知 / 深链等外部入口 → 全屏 `ActivityCrashInfo` / `CrashLogDetailActivity`。两载体共用 `CrashLogViewerClient`（CodeEditor 只读）。

**信息架构位置**：

```
MainShellActivity → 观测 tab → ObserveHostFragment
    └── TabLayout: [历史] | 统计(4D)
         └── CrashHistoryFragment (本文档)
```

## 列表 IA

### 列表行字段

| 字段 | 来源 | 展示 |
|------|------|------|
| 时间 | `CrashEvent.timestampMs` | 相对时间（"3 分钟前"）+ 点击/长按显示绝对时间 |
| 应用名 | `CrashEvent.appLabel` | 主标题 |
| 包名 | `CrashEvent.packageName` | 副标题 |
| 异常类 | `CrashEvent.exceptionClass` | 简短类名（不含包前缀） |
| 异常消息 | `CrashEvent.message` | 单行截断 |
| 来源标记 | `CrashEvent.source` | Chip badge: `looper` / `uncaught` |

### 列表行组件

复用 Design System `CrashEventRow`（见 [design-system.md](design-system.md)）：

- 左侧：应用图标（36dp，从 PackageManager 按 `packageName` 加载；未安装则占位图标）
- 中间：应用名 + 异常简称 / 包名 + 时间 + message 截断
- 右侧：`source` Chip badge

### 排序

- **默认**：`timestampMs` 降序（最新在顶部）
- 不提供排序切换（历史本质为时间线）

## 筛选

| 筛选维度 | 控件 | 说明 |
|----------|------|------|
| 搜索 | `DenseSearchField` | 按 appLabel / packageName / exceptionClass / message 模糊匹配 |
| 时间范围 | `FilterChipRow` | 全部 / 今天 / 7 天 / 30 天 |
| 应用 | 可选 Chip（4D+）| 从统计页下钻传入 packageName pre-filter |

## 空态与加载态

| 状态 | 条件 | UI |
|------|------|-----|
| **加载中** | Repository 首次读取 `events.jsonl` | `LoadingState` 骨架/进度 |
| **无记录** | `events.jsonl` 为空或不存在 | `EmptyState`：图标 + "暂无崩溃记录" + 说明文案 |
| **无匹配** | 搜索/筛选结果为空 | `EmptyState`："无匹配结果" + 重置按钮 |
| **记录关闭** | `crash_log_enabled == false` | `PermissionBanner`：提示开启观测 |

## 详情导航

点击列表行 → 打开 **`CrashDetailBottomSheet`**（壳内 Draggable Half Sheet）：

| 项 | 规范 |
|----|------|
| 载体 | `CrashDetailBottomSheet` — `BottomSheetDialogFragment`；**非** 独立 Activity |
| 参数 | `crash_id`（UUID）via Fragment arguments |
| 数据 | `CrashLogRepository.getById(id)` 加载完整 `CrashEvent` |
| 展示 | `CrashLogViewerClient`（CodeEditor 只读）— [code-editor-porting.md](code-editor-porting.md) |
| Sheet 规范 | 复用 `AddManagedAppBottomSheet` 模式：28dp 顶圆角、`peekHeight` 50%、**DragHandle-only** 拖曳；见 [draggable-half-sheet.md](../design/components/draggable-half-sheet.md) |
| 禁止 | **不** 使用 [settings-card-detail-sheet.md](../design/components/settings-card-detail-sheet.md) 承载 stack trace（该组件面向 metadata / 设置 Card，16dp 顶缘、装饰把手） |

**外部入口**（不在本 Fragment 触发）仍走全屏 Activity：

- 通知 PendingIntent → `ActivityCrashInfo` / `CrashLogDetailActivity`（`Exception` extra 或 Phase 4E 后 `crash_id`）
- 深链 `crashcenter://crash/{crash_id}` → Activity

**PerAppCrashActivity**、统计页下钻列表行与历史列表 **同一 Sheet 路由**（`crash_detail_sheet`）。

## Repository 契约

`CrashHistoryFragment` 通过 ViewModel 调用 `CrashLogRepository`：

| 方法 | 返回 | 说明 |
|------|------|------|
| `getAll(filter, limit, offset)` | `List<CrashEvent>` | 分页加载；filter 含搜索词、时间范围、packageName |
| `getById(id: String)` | `CrashEvent?` | Sheet / Activity 详情用 |
| `getCount(filter)` | `Int` | 列表顶部计数 badge |
| `observeChanges()` | `Flow<Unit>` | ingest 新增时通知 UI 刷新 |

详见 [crash-data-layer.md](crash-data-layer.md)。

## Toolbar 操作（观测 tab 级）

由 Shell Toolbar 呈现、事件转发到 `ObserveHostFragment`：

| 操作 | Phase | 说明 |
|------|-------|------|
| 清空历史 | 4D | 确认对话框 → `Repository.clear()` |
| 导出 | 4E | SAF JSONL / zip |
| 记录设置 | 4D | `crash_log_enabled`、retention |

## 性能考虑

- `events.jsonl` 全文件扫描；MVP 500 条上限下可接受（<100ms）
- 列表使用 `RecyclerView` + `DiffUtil`
- 应用图标按 `packageName` 缓存到 `LruCache`
- 超限时考虑 sidecar 索引（4E）

## 相关文档

- [crash-data-layer.md](crash-data-layer.md) — Repository 与 StatsAggregator
- [crash-logging.md](crash-logging.md) — CrashEvent 数据模型
- [crash-stats-ui.md](crash-stats-ui.md) — 统计子 tab（4D）
- [navigation-ia.md](navigation-ia.md) — 观测 tab IA
- [ui-routing.md](ui-routing.md) — 路由与 Intent 参数
- [design-system.md](design-system.md) — `CrashEventRow`、`CrashDetailBottomSheet`
- [code-editor-porting.md](code-editor-porting.md) — Sheet vs Activity 双载体与 `CrashLogViewerClient`
- [ADR-009](../decisions/009-ui-shell-design-system.md) — UI Shell 架构
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md) — 4C-β 任务
