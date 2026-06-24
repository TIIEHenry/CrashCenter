---
title: "崩溃日志采集与统计"
type: architecture
status: accepted
phase: 4
updated: 2026-06-24
summary: "CrashEvent JSONL；4B-δ 分布式 cache 写 + root 聚合读（ADR-024）"
---

# 崩溃日志采集与统计

> 适用模块：`:app`（`CrashCapturePipeline`、`CrashLogCoordinator`、`LocalCacheBackend`、`DistributedCrashLogRepository`）
> 源码入口：`XposedEntry` → `CrashHandler.insert` → `CrashCapturePipeline.onException`
> 存储决策：[ADR-024](../decisions/024-distributed-cache-crash-storage.md) — 详见 [crash-log-distributed-storage.md](crash-log-distributed-storage.md)
> 历史 ADR：[ADR-007](../decisions/007-crash-log-cross-process-storage.md)（superseded）、[ADR-008](../decisions/008-multi-backend-crash-log-storage.md)（partially superseded）

## 产品定位

CrashCenter 可演进为 **基于 Xposed 的应用稳定性分析中心**：在无需修改目标 APK 的前提下，于目标进程内观测 Java 层未捕获异常。

| 层级 | 职责 | 状态 |
|------|------|------|
| **干预层** | 吞掉异常、Looper 续命，防止进程退出 | 已实现（[CrashHandler](crash-handler.md)） |
| **观测层** | 记录每次被捕获的 Java 崩溃（含仅观测与已拦截） | **4B-δ as-built** |
| **分析层** | 分类、聚类、诊断建议 | Phase 4G — [crash-intelligent-analysis.md](crash-intelligent-analysis.md) |
| **观测 UI** | 按应用 / 异常类型 / 时间聚合、导出 | Phase 4C–4E |

观测层**不改变**干预层语义：日志写入失败须静默，不得阻塞续命 loop 或触发 `System.exit`。

术语见 [glossary.md](../glossary.md)（**Observation Layer**、**CrashLogCoordinator**）。

---

## As-built（4B-δ，2026-06-24）

> 完整路径、扫描、迁移见 **[crash-log-distributed-storage.md](crash-log-distributed-storage.md)**。

### 捕获时的数据流（ADR-023 + ADR-024）

```
Application.onCreate (目标进程)
  → CrashHandler.install(INTERCEPT | OBSERVE)
       ├─ INTERCEPT: Looper 续命 + 吞 UEH
       └─ OBSERVE: 捕获 → 转发系统处理（进程可退出）
  → CrashCapturePipeline.onException(...)
       ├─ shouldIntercept ? logAsync : logSync
       │     └─ LocalCacheBackend → 本 app cache/crash_logs/events.jsonl；失败 silent
       ├─ XposedBridge.log(throwable)          // 调试，非持久化 SSOT
       └─ CrashFeedbackFacade.show（若 scopeDecision.showNotify）
            ├─ Toast
            └─ Notification → ActivityCrashInfo（Intent extra 单次 stack）
```

详见 [crash-capture-pipeline.md](crash-capture-pipeline.md)、[xposed-entry.md](xposed-entry.md)、[crash-handler.md](crash-handler.md)。

### 当前存储

| 存储 | 用途 | 崩溃历史 |
|------|------|----------|
| SharedPreferences `crash` | scope / 禁用列表 / `crash_log_*` 开关 | 否 |
| [XSharedPreferences](../decisions/003-xsharedpreferences-cross-process.md) | hook 侧**只读**配置 | 否 |
| `{app}/cache/crash_logs/events.jsonl` | **分布式 SSOT** — 各 app 私有 JSONL | 写：hook 同 UID；读：root 聚合 |
| `DistributedCrashLogRepository` | 模块侧 root 扫描合并（**须 root**） | 4C UI 读路径 |
| `ActivityCrashInfo` | Intent extra 展示**单次** stack | 不保留历史 |

**结论**：`crash_log_enabled == true` 时每次捕获均尝试写入**目标 app 自身 cache**（与 Toast / 是否拦截无关）。CrashCenter 历史 UI **须 root** 才能聚合全机记录；无 root 时列表为空（无降级）。

### CrashEvent 字段（已实现 vs defer）

| 字段 | 4B-δ | 说明 |
|------|------|------|
| `id` | ✅ | UUID |
| `timestampMs` | ✅ | `System.currentTimeMillis()` |
| `packageName` | ✅ | `lpparam.packageName` |
| `appLabel` | ✅ | `ApplicationInfo.loadLabel()` |
| `processName` | ✅ | `Application.getProcessName()`（API 28+）或包名 fallback |
| `exceptionClass` / `message` | ✅ | root cause |
| `stackTrace` | ✅ | 截断 64KB |
| `source` | ✅ | `"looper"` / `"uncaught"` |
| `intercepted` | ✅ | 必填；`true` = 拦截续命，`false` = 仅监测 |
| `backendWritten` | ✅ | 成功 backend 的 wire name 列表 |
| `pid` / `uid` | defer | 未写入 JSONL |
| `threadName` | defer | 未写入 JSONL |
| `causeClasses` | defer | 未写入 JSONL |
| `isSystemApp` | defer | 未写入 JSONL |
| `moduleVersion` | defer | 未写入 JSONL |
| `ingestedFrom` | **移除** | ADR-024 后无 relay ingest |

边界：仅 **Java 层**异常写入 `events.jsonl` SSOT；Native crash、ANR **不入**主 JSONL 统计（与 [crash-handler.md](crash-handler.md) 一致）。ANR **诊断**见 [anr-observation.md](anr-observation.md)（路径 A logcat / 路径 B `ApplicationExitInfo`），[ADR-025](../decisions/025-anr-observation-no-framework-hook.md)。

---

## 历史架构（4B-α/β，已由 ADR-024 取代）

<details>
<summary>canonical + relay + ingest 模型（归档参考）</summary>

曾用模块 `files/crash_logs/events.jsonl` 作 canonical SSOT；hook 经 Provider / DirectFs / relay 多路径写入，模块 `CrashLogIngestCoordinator` merge relay。详见 [crash-log-backends.md](crash-log-backends.md) 历史章节与 [ADR-024](../decisions/024-distributed-cache-crash-storage.md)。

</details>

---

## 数据模型

每条记录（`CrashEvent`）建议 JSON 结构：

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "timestampMs": 1750300800000,
  "packageName": "com.example.app",
  "appLabel": "Example",
  "processName": "com.example.app",
  "pid": 12345,
  "uid": 10123,
  "threadName": "main",
  "exceptionClass": "java.lang.RuntimeException",
  "message": "just for test",
  "stackTrace": "java.lang.RuntimeException: ...\n\tat ...",
  "causeClasses": ["java.lang.IllegalStateException"],
  "isSystemApp": false,
  "moduleVersion": "26.06.19",
  "source": "uncaught"
}
```

| 字段 | 说明 |
|------|------|
| `id` | UUID，供通知 Intent 引用（替代整段 stack，避免 Binder 限制） |
| `source` | `uncaught` \| `looper` — 区分 UEH 与主线程 loop catch |
| `stackTrace` | 可设单条上限（如 64KB）防止磁盘膨胀 |

**统计索引字段**：`timestampMs`、`packageName`、`exceptionClass`（可选 message 哈希用于去重计数）。

---

## 跨进程写入与读取

```
目标 app 进程 (hook)
  └── CrashCapturePipeline.onException
        └── CrashLogCoordinator.logAsync / logSync
              └── LocalCacheBackend → 本 app cache/crash_logs/events.jsonl

模块进程 (nota.android.crash.xp.app)
  ├── DistributedCrashLogRepository（root 扫描聚合，须 root）
  │     └── CrashLogCacheScanner → 各 app cache JSONL
  │           ├── CrashHistoryFragment — [crash-history-ui.md](crash-history-ui.md)
  │           └── StatsAggregator — [crash-data-layer.md](crash-data-layer.md)
  └── CrashLogMigrationCoordinator（一次性 legacy → per-app cache）
```

路径 SSOT：`CrashLogPaths`；单文件 I/O：`CrashLogJsonlStore`（retention 500/8MB）。

与 [ADR-003](../decisions/003-xsharedpreferences-cross-process.md) 的区别：

- ADR-003：**小配置、hook 只读**（UI 写 prefs → hook `reload()`）
- ADR-024：**大 append 流、hook 写本 app cache**（同 UID，无需跨包 IPC）

并发：多目标 app 同时崩溃时，各写各文件；模块读路径按 `id` 去重（`maxBy timestampMs`）。

---

## Retention 与隐私

| 策略 | 默认值 | 配置项 |
|------|--------|--------|
| 最大条数 | 500 | `CrashLogJsonlStore.MAX_ENTRIES`（pref `crash_log_max_entries` defer 4D） |
| 最大容量 | 8 MB | `CrashLogJsonlStore.MAX_BYTES`（硬编码，pref defer） |
| 启用记录 | true | `crash_log_enabled`（`PrefManager`，hook XSharedPreferences 只读） |

- 数据**仅本地**；stack 可能含路径 / token，导出前须用户确认
- `allowBackup="false"` → 崩溃日志默认不进系统备份
- 轮转：超限删除最旧记录（按 `timestampMs` 或文件顺序）

---

## 分阶段实施

与 [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md) 对齐：

| 阶段 | 内容 | 交付 |
|------|------|------|
| **0 文档** | 本文档 + ADR-007 + Phase 4 roadmap | 方案 commit（无代码） |
| **MVP（4B-δ）** | `LocalCacheBackend` + `DistributedCrashLogRepository`；失败 silent | ✅ 全量记录；UI 须 root |
| **P1 历史 UI** | 主屏入口「崩溃历史」→ 列表 → 详情（复用 crash trace 样式） | 可浏览全部记录 |
| **P2 统计** | 按包名 / 异常类 / 日计数；清空与 retention pref | [crash-stats-ui.md](crash-stats-ui.md) |
| **P3 扩展** | SAF 导出；通知传 `crash_id`；可选 Room 迁移 | 运维与扩展 |

**hook 改动点（实施时）**：在 [xposed-entry.md](xposed-entry.md) 所述 handler 内、`showNotify` **之外**调用 logger — **无论是否 Toast，都记录**（在 `crash_log_enabled` 为 true 时）。

---

## 方案取舍（FAQ）

常见「能否用 XSharedPreferences / 公开目录 / framework 注入」等问题，统一见 [crash-log-ipc.md § 方案取舍与常见疑问](crash-log-ipc.md#方案取舍与常见疑问)。摘要：

| 方案 | 能否作 IPC 主路径 | 说明 |
|------|-------------------|------|
| JSONL 分布式 cache（ADR-024） | ✅ **是** | 各 app 同 UID 写 cache；模块 root 聚合读 |
| XSharedPreferences 存事件体 | ❌ | 只读配置；方向相反、无写 API |
| 公开文件系统 `/sdcard/` | ❌ | 权限、Scoped Storage、隐私；仅 P3 导出 |
| Framework / system_server | ❌ | 不能替 app 级续命与主采集路径 |
| 仅 Logcat | ❌ | 无持久化、无 UI 统计；见 [adb-logcat-analysis.md](adb-logcat-analysis.md) 补充诊断 |

---

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 无 root 无法读全机历史 | 设计如此（ADR-024）；UI 空态提示 |
| `cache` 被系统清理 | 接受；崩溃归属 app 沙箱语义 |
| 崩溃路径阻塞 | 严格后台线程；logger 不得与 Toast/通知同 try 块 |
| 磁盘打满 | per-app 硬上限 + 轮转；单条 stack 截断 |
| `showNotify` 静态变量错乱 | 日志用闭包内 `lpparam`，与通知开关解耦 |
| 吞异常后 app 状态不一致 | 日志仅观测，不改变 [CrashHandler](crash-handler.md) 行为 |

---

## 相关文档

- [crash-log-distributed-storage.md](crash-log-distributed-storage.md) — 4B-δ 分布式 cache 存储（as-built）
- [ADR-024](../decisions/024-distributed-cache-crash-storage.md) — 存储架构决策
- [anr-observation.md](anr-observation.md) — ANR 观测路径 A/B
- [ADR-025](../decisions/025-anr-observation-no-framework-hook.md) — ANR 观测路径
- [logcat-multi-source.md](logcat-multi-source.md) — logcat ANR 线索（system / events）
- [crash-log-backends.md](crash-log-backends.md) — 多后端编排与 ingest
- [crash-log-filesystem.md](crash-log-filesystem.md) — canonical FileLock、读序、FS 验收
- [crash-log-ipc.md](crash-log-ipc.md) — 跨进程通信机制对比与 FAQ
- [overview.md](overview.md) — 系统总览
- [crash-event-timeline-ui.md](crash-event-timeline-ui.md) — CrashEvent 时间线呈现
- [crash-handler.md](crash-handler.md) — 拦截机制（未涵盖持久化）
- [xposed-entry.md](xposed-entry.md) — 通知与单次详情
- [scope-and-prefs.md](scope-and-prefs.md) — 配置模型
- [ADR-007](../decisions/007-crash-log-cross-process-storage.md) — 跨进程存储决策
- [glossary.md](../glossary.md) — 术语
- [crash-intelligent-analysis.md](crash-intelligent-analysis.md) — 分析层 schema 与规则引擎
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md) — 实施任务
