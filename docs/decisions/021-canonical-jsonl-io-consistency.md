---
title: "ADR-021: Canonical JSONL I/O 一致性"
type: decision
status: accepted
phase: 4
updated: 2026-06-23
summary: "events.jsonl 变异统一 FileLock；Repository 读口 timestampMs 降序；删改经 CanonicalJsonlStore"
---

# ADR-021: Canonical JSONL I/O 一致性

## 状态

**Accepted** (2026-06-23) — CanonicalJsonlStore FileLock 统一已落地（`deleteById`/`clear` 委托 `CanonicalJsonlStore`）。配套 [crash-log-filesystem.md](../architecture/crash-log-filesystem.md)。与 [ADR-017](017-root-ingest-and-dedupe.md)（dedupe / ingest）正交：本 ADR 管 **单文件 I/O 契约**，ADR-017 管 **多后端语义与 merge**。

## 背景

4B-α as-built：

- **写**：`CanonicalJsonlWriter` 对 append / retention 使用 `RandomAccessFile` + `FileLock`
- **删改**：`FileCrashLogRepository.deleteById` / `clear` 使用进程内 `ReentrantReadWriteLock` + 无锁 temp 文件重写
- **读**：`streamEvents` 按文件行序输出，文档要求 [crash-history-ui.md](../architecture/crash-history-ui.md) **`timestampMs` 降序**

跨进程 hook append 与模块 UI 删改可并发，导致 **竞态**；读序与产品规格 **不一致**。

## 决策

### 1. 所有 canonical 变异经统一 Store + FileLock

引入或升格 **`CanonicalJsonlStore`**（可由 `CanonicalJsonlWriter` 演进）：

- `append`、`applyRetention`、`deleteById`、`clear` **必须**在 `events.jsonl` 的 **`FileLock`** 内完成
- `DirectFsBackend`、`CrashLogProvider`、`FileCrashLogRepository`（删改）**仅**调用 Store，不各自实现 rewrite

进程内 `ReentrantReadWriteLock` 保留，作为 **模块内读/删序列化** 辅助，**不替代**跨进程 `FileLock`。

### 2. 物理顺序与逻辑顺序分离

| 维度 | 规则 |
|------|------|
| 文件物理行序 | **append-only 时间升序**（写入简单、retention 删最旧行） |
| Repository 读口 | 扫描后 **`sortedByDescending { timestampMs }`**，再 offset/limit |
| Paging | `CrashEventPagingSource` 不二次排序 |

### 3. 读路径防御性 dedupe（与 ADR-017 协同）

在 ADR-017 ingest merge **未落地前**，`getAll` **应**对结果 `distinctBy { id }`（保留 `timestampMs` 最大行）。

ingest merge 完成后，读路径 dedupe 可保留为 cheap 安全网（ADR-017 §4）。

### 4. DirectFs 失败可观测

`mkdirs` 或 append 失败时：

- 返回 `AppendResult.Failure`（经 `appendWithSafeWrite`）
- `XposedBridge.log` 一行摘要

**不**改变观测层 silent 对用户干预层的影响。

## 后果

### 正面

- hook 写与 UI 删改 **可串行化**，降低 JSONL 损坏概率
- 历史列表与 [crash-history-ui.md](../architecture/crash-history-ui.md) 排序一致
- IS / FS 验收可复现

### 负面

- `deleteById` 持锁时间随文件增大而增加（受 500 条 / 8 MB 上限约束）
- 每次 `getAll` 全量 sort（≤500 行可接受；大文件需 4E sidecar）

## 备选方案（未采纳）

| 方案 | 原因 |
|------|------|
| 仅进程内锁 | 无法防护 hook ∥ 模块删改 |
| 文件倒序存储（新行插头部） | append 需全文件 rewrite，崩溃路径更慢 |
| UI 层排序 | Paging offset 语义错误；统计层重复实现 |
| WAL / 双文件 | 复杂度超 MVP；违背极简哲学 |

## 验收

- [crash-log-filesystem.md](../architecture/crash-log-filesystem.md) §6.1 FS-1~FS-5
- 单测：同文件并发 append + `deleteById` 后 JSONL 可解析且条数正确

## 相关文档

- [crash-log-filesystem.md](../architecture/crash-log-filesystem.md)
- [ADR-007](007-crash-log-cross-process-storage.md)
- [ADR-017](017-root-ingest-and-dedupe.md)
- [crash-data-layer.md](../architecture/crash-data-layer.md)
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md)
