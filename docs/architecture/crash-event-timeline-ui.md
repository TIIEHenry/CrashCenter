---
title: "Crash Event 时间线 UI 架构"
type: architecture
status: draft
phase: 4
updated: 2026-06-20
summary: "CrashHistoryFragment 的时间线呈现规范：按时间组织 CrashEvent、复用 Repository/详情路由，不新增独立页面或第三个 tab"
---

# Crash Event 时间线 UI 架构

> 适用模块：`:app` 观测域 UI（Phase 4C+）
> 主契约：[crash-history-ui.md](crash-history-ui.md) — 历史列表、详情导航、Repository 读口
> 数据 SSOT：[crash-logging.md](crash-logging.md) — `CrashEvent` / `events.jsonl`
> 路由 SSOT：[ui-routing.md](ui-routing.md) — `crash_history` / `crash_detail_sheet`

## 概述

Crash Event 时间线不是新的顶级页面，也不是独立 Activity。它是 **`CrashHistoryFragment` 的呈现层演进**：在观测 tab 的「历史」子页中，以时间为主轴展示所有被 CrashCenter 拦截并记录的 `CrashEvent`。

时间线回答的问题是：

| 用户问题 | 页面职责 |
|----------|----------|
| 最近发生了哪些崩溃？ | 按 `timestampMs` 倒序展示事件 |
| 某次崩溃来自哪个 app、哪类异常？ | 行项展示 app、package、exception、message、source |
| 为什么这个 app 反复崩？ | 从时间线进入详情或下钻统计；不在时间线内做聚合判断 |
| 这是不是系统崩溃日志？ | 明确说明只展示被模块 hook 并拦截的 Java 层异常 |

边界：本文只定义 **时间线视觉、交互和状态机**。数据读取、筛选参数、详情载体以 [crash-history-ui.md](crash-history-ui.md) 与 [crash-data-layer.md](crash-data-layer.md) 为准。

术语引用：[CrashEvent](../glossary.md#术语表)、[Observation Layer](../glossary.md#术语表)、[events.jsonl](../glossary.md#术语表)。

---

## IA 与路由

时间线固定承载在观测域：

```
MainShellActivity
  └── ObserveHostFragment
        └── 历史 / crash_history
              └── CrashHistoryFragment
                    └── CrashDetailBottomSheet(crash_id)
```

| 层级 | routeId / 载体 | 说明 |
|------|----------------|------|
| 顶级 | `MainShellActivity` | 2-tab Shell：配置 \| 观测 |
| 观测宿主 | `ObserveHostFragment` | 4C 只挂历史；4D 增加统计内层 TabLayout |
| 时间线 | `crash_history` / `CrashHistoryFragment` | 全局事件时间线；可带预筛参数 |
| 壳内详情 | `crash_detail_sheet` / `CrashDetailBottomSheet` | 点击事件行后用 `crash_id` 加载 stack |
| 外部详情 | `crash_detail` / `ActivityCrashInfo` 或 `CrashLogDetailActivity` | 通知、深链、跨任务栈入口 |

路由参数：

| 参数 | 来源 | 行为 |
|------|------|------|
| `filter_package?` | 单应用页、配置 tab 菜单、历史包名点击 | 时间线只显示该包事件 |
| `filter_exception?` | 统计页异常类 TOP 行 | 时间线只显示该异常类 |
| `initial_page=history` | 外部跳转观测 tab | Shell 选中观测并显示历史 |

禁止新增：

- 第三个 bottom tab「时间线」
- 独立 `TimelineActivity`
- 与 `crash_history` 平行的第二个历史路由

---

## 与相邻页面的边界

| 页面 | 主轴 | 排序 | 主要数据 | 不做什么 |
|------|------|------|----------|----------|
| 配置 tab | 干预配置 | 名称 / 安装状态 | 受管应用、hook Switch | 不展示全量 crash 时间线 |
| 时间线（本文） | 单条事件 | `timestampMs` 降序 | `CrashEvent` 列表 | 不做 TOP N 聚合、不放 hook Switch |
| 统计 tab | 聚合指标 | count / 最近时间 | `StatsAggregator` 输出 | 不承载逐条 stack 阅读 |
| 单应用观测页 | `packageName` | `timestampMs` 降序 | 包级摘要 + 该包事件 | 不修改该 app 的干预规则 |
| 详情 Sheet / Activity | 单条 stack | N/A | `CrashEvent.stackTrace` | 不展示列表和筛选 |

时间线可以接受统计页下钻参数，但聚合语义仍属于 [crash-stats-ui.md](crash-stats-ui.md)。配置 tab 可提供「崩溃记录」入口，但开关与规则编辑仍停留在配置域。

---

## As-built（2026-06-20）

当前 4C-β 已具备基础历史列表：

| 能力 | 现状 |
|------|------|
| 载体 | `ObserveHostFragment` 内嵌 `CrashHistoryFragment` |
| 数据 | `FileCrashLogRepository` 读取 canonical `events.jsonl` |
| 列表 | Paging3 + `CrashEventPagingSource`，默认 `PAGE_SIZE = 50` |
| 行项 | `CrashEventBinder` + `view_crash_event_row.xml` |
| 排序 | Repository 按 `timestampMs` 降序返回 |
| 详情 | 历史行点击 → `CrashDetailBottomSheet(crash_id)` |
| 刷新 | `onResume` / 手动 adapter refresh |

尚未完成：

| 能力 | 目标阶段 |
|------|----------|
| 时间范围 Chip 与搜索 UI 接入 | 4D |
| `filter_package` / `filter_exception` 路由预筛 | 4D |
| `crash_log_enabled=false` 横幅 | 4D |
| `observeChanges()` / ingest 后自动刷新 | 4B-β / 4D |
| 日期分组、sticky header、跳日控件 | 4D+ / P2 |

因此本文的目标态需要分阶段实现，不能把未来 Repository 接口当作当前代码已完成能力。

---

## 时间线视觉模型

### V1：平铺事件流（当前基线）

V1 使用单列 `RecyclerView`：

```
[搜索框 / 时间 Chip - 4D]

Today
  App A   RuntimeException   3 分钟前   looper
          message 摘要
  App B   NullPointerException 12 分钟前 uncaught
          message 摘要

Yesterday
  App C   IllegalStateException 昨天 22:10 looper
          message 摘要
```

当前实现可先不显示日期 header，只保证 `timestampMs` 倒序。日期 header 是展示增强，不改变数据模型和排序规则。

### V2：日期分组

日期分组用于提升可读性：

| 元素 | 规则 |
|------|------|
| 分组键 | 本地时区下的 `yyyy-MM-dd` |
| Header 文案 | 今天 / 昨天 / `MM-dd` / `yyyy-MM-dd` |
| Header 行 | 不可点击；可 sticky（P2） |
| 事件行 | 仍为 Paging 列表项；header 可作为 separator item |

分组只影响 UI，不写入 `CrashEvent`。跨时区显示以设备当前时区为准，导出仍保留原始 `timestampMs`。

### V3：快速定位（可选）

当 retention 或未来索引允许超过 500 条时，可增加轻量跳转：

| 能力 | 说明 |
|------|------|
| 日期 scrubber | 右侧或底部轻量定位，不阻挡列表 |
| 跳到今天 / 最早 | Toolbar 或浮动小按钮 |
| 当前日期提示 | 随滚动更新 header 文案 |

不引入热力图、折线图或重型图表库。趋势和 TOP N 归属统计页。

---

## 行项契约

时间线行项复用 `CrashEventRow` 语义：

| 字段 | 来源 | 展示规则 |
|------|------|----------|
| 应用图标 | `PackageManager` by `packageName` | 未安装时使用默认图标 |
| 应用名 | `CrashEvent.appLabel` | 缺失时降级为 package 简名 |
| 包名 | `CrashEvent.packageName` | 次要文本或详情展开 |
| 异常类 | `CrashEvent.exceptionClass` | 默认显示短类名；详情显示完整类名 |
| message | `CrashEvent.message` | 单行截断；空时用 stack 首行 fallback |
| 时间 | `CrashEvent.timestampMs` | 相对时间 + 可访问性文本包含绝对时间 |
| 来源 | `CrashEvent.source` | Chip：`looper` / `uncaught` |

点击行为：

| 操作 | 行为 |
|------|------|
| 点击行 | 打开 `CrashDetailBottomSheet`，传 `crash_id` |
| 长按行 | P2：复制摘要 / 分享单条 / 删除单条（如 Repository 支持） |
| 点击包名或 app 区 | P2：进入单应用观测页或应用该包筛选 |

行项不得展示或修改 per-app hook Switch。时间线是观测层，不是干预层配置入口。

---

## 筛选与搜索

筛选沿用 `CrashFilter`：

| 控件 | 映射字段 | 默认 |
|------|----------|------|
| 搜索框 | `query`，匹配 `appLabel` / `packageName` / `exceptionClass` / `message` | 空 |
| 时间 Chip | `sinceMs` / `untilMs` | 全部 |
| 包名预筛 | `packageName` | 无 |
| 异常类预筛 | `exceptionClass`（目标扩展字段） | 无 |
| 来源 Chip（可选） | `source` | 全部 |

交互规则：

- 搜索与 Chip 变化后刷新 PagingSource。
- 下钻筛选应显示可清除的 filter chip，例如「异常：RuntimeException ×」。
- 清除全部筛选后回到全局时间线。
- 过滤后为空显示「无匹配结果」，不使用「暂无崩溃记录」文案。

---

## ViewModel 与数据流

目标数据流：

```
CrashHistoryFragment
  └── CrashHistoryViewModel
        ├── CrashFilter state
        ├── Pager(config = PAGE_SIZE)
        │     └── CrashEventPagingSource
        │           └── CrashLogRepository.getAll(filter, limit, offset)
        └── getById(id) for detail route
```

约束：

| 约束 | 说明 |
|------|------|
| Repository 是唯一读口 | UI / ViewModel 不直接打开 `events.jsonl` |
| 线程 | 文件扫描、JSON parse、聚合在 IO 线程 |
| 写入刷新 | 当前可在 `onResume` refresh；目标态由 `observeChanges()` 或 ingest 通知触发 |
| 分页 | 默认 50 条；日期 header 不应破坏 paging key |
| 错误行 | JSONL 坏行跳过；可在页脚提示跳过数量（P2） |

`events.jsonl` 是 canonical SSOT。hook 侧 relay、Provider、DirectFs 的差异由写入/ingest 层处理，时间线只消费合并后的 canonical 数据。

---

## 状态机

| 状态 | 条件 | UI |
|------|------|----|
| Loading | 首次加载或筛选切换 | `LoadingState` 或列表 skeleton |
| Empty | `events.jsonl` 不存在或无有效事件 | `EmptyState`：「暂无崩溃记录」 |
| No match | 筛选后无结果 | `EmptyState`：「无匹配结果」+ 清除筛选 |
| Disabled | `crash_log_enabled == false` | `PermissionBanner`：「记录已关闭，仅显示旧数据」 |
| Partial | JSONL 存在坏行 | 列表正常显示；页脚或 debug 文案提示跳过 |
| Ingesting | 4B-β relay merge 进行中 | 轻提示「正在合并崩溃记录」或静默刷新 |

空态文案必须说明数据范围：这里不是 Android 系统 crash 报告，只是 CrashCenter 已拦截并写入的 Java 层异常。

---

## Hook 与跨进程边界

时间线 UI 只在模块进程运行，不参与 hook：

| 边界 | 约束 |
|------|------|
| `XposedEntry` / `CrashHandler` | 不因时间线需求修改拦截策略 |
| `CrashCapturePipeline` | 写入与反馈保持隔离；UI 不假设显示 Toast 才会有日志 |
| `CrashLogCoordinator` | 写入失败 silent；不得为刷新 UI 阻塞 hook 线程 |
| `CrashLogProvider` | 继续无 signature permission；时间线不直接调用 Provider 写入 |
| Relay 文件 | 仅由 ingest 读取并 merge；时间线不读目标 app 私有 relay |
| 4G 分析 | 分析在模块进程 lazy 执行；禁止进入 hook 路径 |

`scope_mode`、`handle_system`、per-app 干预规则决定「哪些异常会被拦截并记录」。时间线只展示结果，不解释未被 hook 的系统异常、Native crash 或 ANR。

---

## 性能与隐私

| 主题 | 策略 |
|------|------|
| 数据量 | 当前 retention 500 条 / 8 MB，JSONL 扫描可接受 |
| 滚动 | RecyclerView + DiffUtil / Paging；日期 header 作为轻量 item |
| 图标 | 按 `packageName` LruCache；卸载 app 使用默认图标 |
| stack | 列表只显示摘要，完整 stack 进详情 |
| 敏感信息 | stack 可能含路径、账号、token；分享/导出前二次确认 |
| 后续扩展 | 5000+ 条再评估 sidecar index 或 Room，Repository 接口保持稳定 |

---

## 非目标

- 不新增顶级「时间线」tab。
- 不引入 `TimelineActivity` 替代 `CrashHistoryFragment`。
- 不绕过 `CrashLogRepository` 直接读写 `events.jsonl`。
- 不读取 target app 私有 relay 文件。
- 不在时间线行放置 hook Switch、scope 编辑或规则编辑。
- 不在 hook 路径运行统计、分析、LLM 或图表生成。
- 不引入 MPAndroidChart、热力图、折线图等重型图表。
- 不把时间线变成系统级 crash / ANR / Native crash 报告。

---

## 分阶段交付

| 阶段 | 时间线能力 |
|------|------------|
| 4C-β（已完成基线） | `CrashHistoryFragment` + Paging 列表 + 行点击详情 |
| 4D MVP | 搜索、时间 Chip、预筛参数、记录关闭 banner |
| 4D+ | 日期分组、异常类/source 筛选、单应用入口联动 |
| 4E | 导出时保留当前筛选范围；通知改 `crash_id` 后从详情回时间线 |
| 4G | 详情页 lazy 分析结果可从时间线显示轻量 category chip（不阻塞列表） |

若需要把 `CrashFilter` 扩展到 `exceptionClass`，应先更新 [crash-data-layer.md](crash-data-layer.md) 的 Repository 契约，再实施 UI。

---

## 验收标准

| # | 场景 | 期望 |
|---|------|------|
| H1 | 3 个 app 各触发 2 次 Java 崩溃 | 时间线显示 6 条，按 `timestampMs` 降序 |
| H2 | 点击任一事件 | 打开 `CrashDetailBottomSheet`，stack 与该 `crash_id` 一致 |
| H3 | 从统计页点击异常类 TOP 行 | 切到历史并应用 `filter_exception` |
| H4 | `crash_log_enabled=false` 后触发崩溃 | 新事件不增加；时间线显示记录关闭 banner |
| H5 | 模块 force-stop 后目标 app 崩溃 | Provider / ingest 成功后，回到观测 tab 可见新事件 |
| H6 | 触发 retention 超过 500 条 | 最旧事件被移除，列表不崩溃、不重复 |
| H7 | 启用日期分组 | 同日本地时间事件分组正确，滚动流畅 |
| H8 | 卸载曾产生事件的 app | 历史仍显示；图标与 label 合理降级 |

---

## 相关文档

- [crash-history-ui.md](crash-history-ui.md) — 历史列表主契约
- [crash-data-layer.md](crash-data-layer.md) — Repository、筛选、retention
- [crash-logging.md](crash-logging.md) — CrashEvent 模型与跨进程写入
- [navigation-ia.md](navigation-ia.md) — 2-tab Shell 与观测域 IA
- [ui-routing.md](ui-routing.md) — `crash_history` / `crash_detail_sheet` 路由
- [crash-stats-ui.md](crash-stats-ui.md) — 统计页与时间线下钻边界
- [design-system.md](design-system.md) — `CrashEventRow`、`EmptyState`、`CrashDetailBottomSheet`
- [code-editor-porting.md](code-editor-porting.md) — 详情阅读器与双载体
- [overview.md](overview.md) — 系统总览与观测层定位
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md) — Phase 4 任务与验收
- [ADR-005](../decisions/005-settings-information-architecture.md) — 配置 IA 边界
- [ADR-007](../decisions/007-crash-log-cross-process-storage.md) — 跨进程存储
- [ADR-008](../decisions/008-multi-backend-crash-log-storage.md) — 多后端写入
- [ADR-009](../decisions/009-ui-shell-design-system.md) — UI Shell 与 Design System
- [glossary.md](../glossary.md) — 术语
