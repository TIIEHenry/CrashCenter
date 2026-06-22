---
title: "崩溃数据层架构"
type: architecture
status: accepted
phase: 4
updated: 2026-06-22
summary: "CrashLogRepository 读口 as-built（Paging3 + LRU + clear/deleteById/applyRetention）；4B-γ FileLock 统一见 crash-log-filesystem.md"
---

# 崩溃数据层架构

> 适用模块：`:app` 模块进程域（`nota.android.crash.module.data`）
> 数据 SSOT：`events.jsonl`（[crash-logging.md](crash-logging.md)）
> 写入侧：[crash-log-backends.md](crash-log-backends.md)（hook 侧 Coordinator）、[crash-log-ipc.md](crash-log-ipc.md)（ingest）
> 消费侧：[crash-history-ui.md](crash-history-ui.md)（列表）、[crash-stats-ui.md](crash-stats-ui.md)（统计）

## 概述

`CrashLogRepository` 是模块进程内 **观测数据的唯一读口**（SSOT consumer）。历史列表、统计、详情、导出、分析层均通过 Repository 访问 `events.jsonl`，不直接操作文件。

## 组件图

**As-built（2026-06）** — 读路径已落地；clear/deleteById/applyRetention 已落地；聚合 / 变更通知 defer 4D：

```
CrashLogIngestCoordinator (defer 4B-β)
    └── merge → events.jsonl
                    ↑ append (hook 写入，4B-α)
                    │
           FileCrashLogRepository (读)
            ├── getAll / getById / getCount
            ├── deleteById / clear / applyRetention
            ├── LRU cache (200 entries, mtime 失效)
            └── 流式顺序扫描 + ReentrantReadWriteLock
                    │
                    ▼
           CrashEventPagingSource → Paging3
                    │
                    ▼
           CrashHistoryFragment / CrashHistoryPagingAdapter
```

**4D 目标** — StatsAggregator、`observeChanges` Flow 与用户可调 pref（见 §4D 目标）。`clear` / `deleteById` / `applyRetention` 已在 as-built。

## As-built（2026-06）

模块进程读口已实现，与 [crash-history-ui.md](crash-history-ui.md) 列表 UI 对齐。

| 项 | as-built |
|----|----------|
| 接口 | `getAll`、`getById`、`getCount`、`deleteById(id): Boolean`、`clear()`、`applyRetention()` |
| 实现 | `FileCrashLogRepository` — 顺序读 `events.jsonl`，逐行 JSON parse；流式扫描支持 early termination |
| 缓存 | LRU **200** 条（`CACHE_CAPACITY`）；`lastModified` + 文件长度变更时 invalidate |
| 并发 | `ReentrantReadWriteLock`；无 `FileObserver` 变更通知（defer 4D） |
| 列表 UI | `CrashHistoryViewModel` → `Pager` + **`CrashEventPagingSource`** + `CrashHistoryPagingAdapter` |
| DI | `ServiceLocator.crashLogRepository()` 单例 |

**不在 as-built**：`observeChanges()`（4D 目标，未定义于接口）、`StatsAggregator`。`clear()`、`deleteById()`、`applyRetention()` 已落地。

## CrashLogRepository

### As-built 接口

```kotlin
interface CrashLogRepository {
    fun getAll(filter: CrashFilter, limit: Int, offset: Int): List<CrashEvent>
    fun getById(id: String): CrashEvent?
    fun getCount(filter: CrashFilter): Int
    fun deleteById(id: String): Boolean
    fun clear()
    fun applyRetention()
}

data class CrashFilter(
    val query: String? = null,
    val packageName: String? = null,
    val sinceMs: Long? = null,
    val untilMs: Long? = null,
    val source: String? = null
)
```

### 实现策略（as-built）

| 策略 | 说明 |
|------|------|
| 流式扫描 | 顺序读 `events.jsonl`，逐行 JSON parse；`getAll` 支持 offset/limit，命中 limit 后 early termination |
| LRU 缓存 | `LinkedHashMap` 最多 **200** 条；`getById` 命中缓存则免扫描；扫描时自动填充缓存 |
| 失效 | 比较 `eventsFile.lastModified()` 与 `length()`；ingest 追加后下次读自动 reload |
| 删除 | `deleteById` 通过临时文件重写，atomic rename；空文件时直接删除 |
| 清空 | `clear()` 删除 `events.jsonl` 并清空缓存状态 |
| 轮转 | `applyRetention()` 委托 `CanonicalJsonlWriter.applyRetention`（500 条 / 8 MB），完成后清空缓存 |
| 分页 | UI **不**全量 `getAll`；`CrashEventPagingSource` 按页调用 Repository |
| 线程 | Repository 方法同步阻塞；Paging / ViewModel 在 `Dispatchers.IO` 调用 |

### 未来演进（4E+）

| 阶段 | 方案 |
|------|------|
| 500 条内 | JSONL 全文件 + Paging 足够（当前） |
| 500–5000 条 | sidecar `index.json`：`[{id, timestampMs, packageName, exceptionClass}]` |
| 5000+ | 可选 Room 迁移；Repository 接口扩展但保持读口单一 |

## §4D 目标（尚未实现）

以下规格为 Phase 4D+ 设计目标，**非**当前 as-built。实施前见 [dev/plans/architecture-decision-backlog.md](../../dev/plans/architecture-decision-backlog.md) D-04 / D-06。

### 扩展接口契约

```kotlin
interface CrashLogRepository {
    fun getAll(filter: CrashFilter, limit: Int, offset: Int): List<CrashEvent>
    fun getById(id: String): CrashEvent?
    fun getCount(filter: CrashFilter): Int
    fun deleteById(id: String): Boolean
    fun clear()
    fun observeChanges(): Flow<Unit>   // stub: returns emptyFlow()
    fun applyRetention()
}
```

### 变更通知（4D）

| 策略 | 说明 |
|------|------|
| 当前实现 | `observeChanges()` 返回 `emptyFlow()` — stub，UI 通过 Paging3 `refresh` 手动刷新 |
| 未来方案 | `FileObserver` 或轮询 `lastModified`；ingest/Provider 写入后通知 `observeChanges` |
| 线程 | `Dispatchers.IO`；UI 层在 ViewModel 内 `viewModelScope` 收集 |

## StatsAggregator（4D 目标）

聚合层设计，**尚未编码**；仅依赖扩展后的 `CrashLogRepository`：

```kotlin
class StatsAggregator(private val repo: CrashLogRepository) {
    suspend fun summary(filter: CrashFilter): CrashStats
    suspend fun topPackages(filter: CrashFilter, limit: Int): List<PackageStat>
    suspend fun topExceptions(filter: CrashFilter, limit: Int): List<ExceptionStat>
    suspend fun dailyCounts(filter: CrashFilter, days: Int): List<DayCount>
}

data class CrashStats(
    val totalCount: Int,
    val uniquePackages: Int,
    val uniqueExceptions: Int,
    val latestTimestamp: Long?
)
```

### 聚合策略

- MVP：每次调用扫描缓存 `List<CrashEvent>`，in-memory group-by
- 4G-V2：引入 `signatureHash` 后可做 `topClusters()` 重复崩溃聚类
- 不持久化聚合结果；clear 后自动归零

## Retention 策略（4D 目标）

> hook 侧 `CanonicalJsonlWriter` 已在 append 时执行 500 条 / 8 MB 轮转（4B-α）。模块侧 `applyRetention` / 用户可调 pref 为 4D 目标。

| 参数 | 默认 | pref key |
|------|------|----------|
| 最大条数 | 500 | `crash_log_max_entries` |
| 最大容量 | 8 MB | （硬编码） |
| 启用记录 | true | `crash_log_enabled` |

### 轮转逻辑

`applyRetention()` 委托 `CanonicalJsonlWriter.applyRetention(eventsFile)`：

```
1. 读 events.jsonl → lines[]
2. while lines.size > 500: drop oldest (first line)
3. while byteSize(lines) > 8 MB and lines.size > 1: drop oldest
4. if trimmed: rewrite events.jsonl via RandomAccessFile (file lock)
```

模块侧 `applyRetention()` 在写锁内调用，完成后清空缓存。

### 清空（as-built）

`clear()` 删除 `events.jsonl` 并清空缓存；`deleteById(id)` 通过临时文件重写，命中后 atomic rename（空文件时直接删除）。

## 文件路径

```
/data/data/nota.android.crash.xp.app/files/crash_logs/
  events.jsonl    # canonical SSOT，append-only（轮转时 rewrite）
  meta.json       # 可选：totalCount, byteSize, oldestId, newestId
```

模块进程同 UID，读写无需 root。

## 线程安全

| 场景 | 策略 |
|------|------|
| hook 写 + UI 读 | `FileLock` 或 Provider 串行化（hook 侧已处理）；删改须同锁 — 见 [crash-log-filesystem.md](crash-log-filesystem.md)、[ADR-021](../decisions/021-canonical-jsonl-io-consistency.md) |
| ingest merge + UI 读 | Repository 内部 `Mutex`；ingest 写完后通知 invalidate |
| retention rewrite + 读 | `Mutex` + atomic temp-rename |

## 相关文档

- [crash-logging.md](crash-logging.md) — CrashEvent 模型、retention 规格
- [crash-log-backends.md](crash-log-backends.md) — hook 侧写入
- [crash-history-ui.md](crash-history-ui.md) — 历史列表消费方
- [crash-event-timeline-ui.md](crash-event-timeline-ui.md) — 历史列表的时间线呈现与筛选状态
- [crash-stats-ui.md](crash-stats-ui.md) — 统计消费方
- [crash-capture-pipeline.md](crash-capture-pipeline.md) — 事件生产侧
- [scope-and-prefs.md](scope-and-prefs.md) — `crash_log_enabled` / `crash_log_max_entries` 配置
- [architecture-optimization.md](architecture-optimization.md) — §5.5 Repository 设计
- [crash-log-filesystem.md](crash-log-filesystem.md) — canonical FileLock、读序、FS 验收
- [dev/plans/architecture-decision-backlog.md](../../dev/plans/architecture-decision-backlog.md) — D-04 Repository 契约、D-10 Paging vs Flow
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md)
