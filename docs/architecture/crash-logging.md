---
title: "崩溃日志采集与统计"
type: architecture
status: accepted
phase: 4
updated: 2026-06-19
summary: "hook 侧异步持久化全量拦截崩溃；4B-α 部分 MVP 已实现；多后端编排见 crash-log-backends.md"
---

# 崩溃日志采集与统计

> 适用模块：`:app`（4B-α：`CrashCapturePipeline`、`CrashLogCoordinator`、`CrashLogProvider`）
> 源码入口：`XposedEntry` → `CrashHandler.insert` → `CrashCapturePipeline.onException`
> 相关 ADR：[ADR-007](../decisions/007-crash-log-cross-process-storage.md)、[ADR-008](../decisions/008-multi-backend-crash-log-storage.md)
> 多后端编排：[crash-log-backends.md](crash-log-backends.md)

## 产品定位

CrashCenter 可演进为 **基于 Xposed 的应用稳定性分析中心**：在无需修改目标 APK 的前提下，于目标进程内观测 Java 层未捕获异常。

| 层级 | 职责 | 状态 |
|------|------|------|
| **干预层** | 吞掉异常、Looper 续命，防止进程退出 | 已实现（[CrashHandler](crash-handler.md)） |
| **观测层** | 记录每次被拦截的崩溃（结构化字段 + 本地持久化） | **4B-α 部分 MVP**（无 UI 统计 / root ingest） |
| **分析层** | 分类、聚类、诊断建议 | Phase 4G — [crash-intelligent-analysis.md](crash-intelligent-analysis.md) |
| **观测 UI** | 按应用 / 异常类型 / 时间聚合、导出 | Phase 4C–4E |

观测层**不改变**干预层语义：日志写入失败须静默，不得阻塞续命 loop 或触发 `System.exit`。

术语见 [glossary.md](../glossary.md)（**Observation Layer**、**CrashLogCoordinator**）。

---

## As-built（4B-α，2026-06-19）

### 拦截时的数据流

```
Application.onCreate (目标进程)
  → CrashHandler.insert(exceptionHandler)
       ├─ 主线程: while(true) { Looper.loop() } catch → handler
       └─ UncaughtExceptionHandler → handler（不转发系统 handler）
  → CrashCapturePipeline.onException(...)
       ├─ CrashLogCoordinator.logAsync（单线程 executor，与 showNotify 无关）
       │     └─ Phase 2 并行：ProviderBackend ∥ DirectFsBackend ∥ TargetRelayBackend
       │           → canonical JSONL / relay 副本；失败 silent
       ├─ XposedBridge.log(throwable)          // 调试，非持久化 SSOT
       └─ CrashFeedbackFacade.show（若 scopeDecision.showNotify）
            ├─ Toast
            └─ Notification → ActivityCrashInfo（Intent extra 单次 stack）
```

详见 [crash-capture-pipeline.md](crash-capture-pipeline.md)、[xposed-entry.md](xposed-entry.md)、[crash-handler.md](crash-handler.md)。

**4B-α 范围**：hook 侧 Phase 2 多后端并行写入；`RootSuBackend` 与模块侧 `CrashLogIngestCoordinator` **defer 至 4B-β**。

### 当前存储

| 存储 | 用途 | 崩溃历史 |
|------|------|----------|
| SharedPreferences `crash` | scope / 禁用列表 / `crash_log_*` 开关 | 否 |
| [XSharedPreferences](../decisions/003-xsharedpreferences-cross-process.md) | hook 侧**只读**配置 | 否 |
| `files/crash_logs/events.jsonl` | canonical append-only JSONL | **是**（模块 UID 写） |
| `files/crashcenter_relay/{id}.json` | 目标 app 同 UID relay 副本 | 待 4B-β ingest merge |
| `FileCrashLogRepository` | 模块侧读 canonical（4C UI 用） | 读路径已实现 |
| `ActivityCrashInfo` | Intent extra 展示**单次** stack | 不保留历史 |

**结论**：`crash_log_enabled == true` 时每次拦截均尝试持久化（与 Toast 无关）；历史 UI / 统计 / root harvest 仍属 4C–4D / 4B-β backlog。

### CrashEvent 字段（已实现 vs defer）

| 字段 | 4B-α | 说明 |
|------|------|------|
| `id` | ✅ | UUID |
| `timestampMs` | ✅ | `System.currentTimeMillis()` |
| `packageName` | ✅ | `lpparam.packageName` |
| `appLabel` | ✅ | `ApplicationInfo.loadLabel()` |
| `processName` | ✅ | `Application.getProcessName()`（API 28+）或包名 fallback |
| `exceptionClass` / `message` | ✅ | root cause |
| `stackTrace` | ✅ | 截断 64KB |
| `source` | ✅ | `"looper"` / `"uncaught"` |
| `backendWritten` | ✅ | 成功 backend 的 wire name 列表 |
| `pid` / `uid` | defer | 未写入 JSONL |
| `threadName` | defer | 未写入 JSONL |
| `causeClasses` | defer | 未写入 JSONL |
| `isSystemApp` | defer | 未写入 JSONL |
| `moduleVersion` | defer | 未写入 JSONL |
| `ingestedFrom` | defer | 4B-β ingest 字段 |

边界：仅 **Java 层**异常；Native crash、ANR 不在 scope（与 [crash-handler.md](crash-handler.md) 一致）。

---

## 推荐架构（4B-β 补全 root / ingest）

多后端并行 + root 优先 + 模块 ingest 见 **[crash-log-backends.md](crash-log-backends.md)**（[ADR-008](../decisions/008-multi-backend-crash-log-storage.md)）。**4B-α 已落地 Phase 2 并行**；Phase 1 `RootSuBackend` 与 ingest 见该文档 § As-built 4B-α。

### 主路径：append-only JSONL

hook 侧在 `handlerException` 内（**与 `showNotify` 无关**）异步 append 一行 JSON 到模块私有目录：

```
/data/data/nota.android.crash.xp.app/files/crash_logs/
  events.jsonl          # append-only，每行一条 CrashEvent
  meta.json             # 可选：totalCount、byteSize、oldestId
```

写入 API（概念）：

```
createPackageContext(MODULE_PKG, CONTEXT_IGNORE_SECURITY)
  → getFilesDir() / crash_logs / events.jsonl
  → 后台线程 FileOutputStream append + newline
```

### Fallback：ContentProvider

部分 Android 10+ / OEM 上跨包直写 `filesDir` 可能失败（SELinux EACCES；API 30+ 包可见性）。**4B-α 已实现**极简 **`CrashLogProvider`**（仅 `insert`，内部 `CanonicalJsonlWriter.append` 同一 JSONL）。详见 [crash-log-ipc.md § As-built](crash-log-ipc.md#as-builtcrashlogprovider4b-α)。

**权限设计**：`exported="true"`，**不得**使用 `protectionLevel="signature"` 的 manifest permission — hook 侧 `Binder.getCallingUid()` 为目标 app UID，与模块异签名，会导致 `SecurityException`。安全依赖 Provider 内 `callingUid` ↔ `packageName` 校验与 rate limit。详见 [crash-log-ipc.md](crash-log-ipc.md#signature-权限悖论fallback-b-关键缺陷)。

模块进程未运行时，AM 按需拉起 Provider 进程；不依赖 CrashCenter UI 曾运行。

**MVP 不引入 Room**；统计查询成为瓶颈时再评估 Room 或 sidecar 索引文件。

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

## 跨进程写入路径

```
目标 app 进程 (hook)
  └── CrashCapturePipeline.onException
        └── CrashLogCoordinator.logAsync
              └── Phase2（4B-α）：ProviderBackend ∥ DirectFsBackend ∥ TargetRelayBackend
              └── Phase1 RootSuBackend — defer 4B-β

模块进程 (nota.android.crash.xp.app)
  ├── CrashLogProvider.insert → CanonicalJsonlWriter → events.jsonl
  ├── FileCrashLogRepository（读 canonical，4C UI）
  └── CrashLogIngestCoordinator — defer 4B-β
        └── root harvest relay → merge canonical
              ├── CrashHistoryFragment — [crash-history-ui.md](crash-history-ui.md)
              └── StatsAggregator — [crash-data-layer.md](crash-data-layer.md)
```

详见 [crash-log-backends.md](crash-log-backends.md)。单后端 IPC 对比见 [crash-log-ipc.md](crash-log-ipc.md)。

与 [ADR-003](../decisions/003-xsharedpreferences-cross-process.md) 的区别：

- ADR-003：**小配置、hook 只读**（UI 写 prefs → hook `reload()`）
- 本方案：**大 append 流、hook 写回模块存储**（方向相反，数据量大）

并发：多目标 app 同时崩溃时，需 `FileLock`、Provider 串行化，或临时文件 merge。

---

## Retention 与隐私

| 策略 | 默认值 | 配置项 |
|------|--------|--------|
| 最大条数 | 500 | `CanonicalJsonlWriter.MAX_ENTRIES`（pref `crash_log_max_entries` defer 4D） |
| 最大容量 | 8 MB | `CanonicalJsonlWriter.MAX_BYTES`（硬编码，pref defer） |
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
| **MVP（4B-α）** | `CrashCapturePipeline` + `CrashLogCoordinator` Phase 2 并行；失败 silent | ✅ 全量记录；无统计 UI |
| **P1 历史 UI** | 主屏入口「崩溃历史」→ 列表 → 详情（复用 crash trace 样式） | 可浏览全部记录 |
| **P2 统计** | 按包名 / 异常类 / 日计数；清空与 retention pref | [crash-stats-ui.md](crash-stats-ui.md) |
| **P3 扩展** | SAF 导出；通知传 `crash_id`；可选 Room 迁移 | 运维与扩展 |

**hook 改动点（实施时）**：在 [xposed-entry.md](xposed-entry.md) 所述 handler 内、`showNotify` **之外**调用 logger — **无论是否 Toast，都记录**（在 `crash_log_enabled` 为 true 时）。

---

## 方案取舍（FAQ）

常见「能否用 XSharedPreferences / 公开目录 / framework 注入」等问题，统一见 [crash-log-ipc.md § 方案取舍与常见疑问](crash-log-ipc.md#方案取舍与常见疑问)。摘要：

| 方案 | 能否作 IPC 主路径 | 说明 |
|------|-------------------|------|
| JSONL + 多后端（ADR-008） | ✅ **是** | root 优先 + Provider + relay + ingest |
| XSharedPreferences 存事件体 | ❌ | 只读配置；方向相反、无写 API |
| 公开文件系统 `/sdcard/` | ❌ | 权限、Scoped Storage、隐私；仅 P3 导出 |
| Framework / system_server | ❌ | 不能替 app 级续命与主采集路径 |
| 仅 Logcat | ❌ | 无持久化、无 UI 统计；见 [adb-logcat-analysis.md](adb-logcat-analysis.md) 补充诊断 |

---

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 跨包写文件失败 | Provider fallback（无 signature permission）；写失败仅 log，不 `System.exit` |
| 模块未运行 / force-stop | Primary A 不依赖进程；Fallback B 由 AM 拉起；见 [crash-log-ipc.md](crash-log-ipc.md) |
| 崩溃路径阻塞 | 严格后台线程；logger 不得与 Toast/通知同 try 块 |
| 磁盘打满 | 硬上限 + 轮转；单条 stack 截断 |
| `showNotify` 静态变量错乱 | 日志用闭包内 `lpparam`，与通知开关解耦 |
| 吞异常后 app 状态不一致 | 日志仅观测，不改变 [CrashHandler](crash-handler.md) 行为 |

---

## 相关文档

- [crash-log-backends.md](crash-log-backends.md) — 多后端编排与 ingest
- [crash-log-ipc.md](crash-log-ipc.md) — 跨进程通信机制对比与 FAQ
- [overview.md](overview.md) — 系统总览
- [crash-handler.md](crash-handler.md) — 拦截机制（未涵盖持久化）
- [xposed-entry.md](xposed-entry.md) — 通知与单次详情
- [scope-and-prefs.md](scope-and-prefs.md) — 配置模型
- [ADR-007](../decisions/007-crash-log-cross-process-storage.md) — 跨进程存储决策
- [glossary.md](../glossary.md) — 术语
- [crash-intelligent-analysis.md](crash-intelligent-analysis.md) — 分析层 schema 与规则引擎
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md) — 实施任务
