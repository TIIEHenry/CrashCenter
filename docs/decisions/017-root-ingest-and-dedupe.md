---
title: "ADR-017: 4B-β Root Ingest 与 JSONL Dedupe"
type: decision
status: accepted
phase: 4
updated: 2026-06-23
summary: "落实 ADR-008 Phase 1 RootSu + 模块 ingest merge；canonical 按 crash_id 去重；读路径可选防御性 dedupe"
---

# ADR-017: 4B-β Root Ingest 与 JSONL Dedupe

## 状态

**Accepted** (2026-06-23) — RelayMergeBackend ingest + id dedupe 已落地。不修改 [ADR-008](008-multi-backend-crash-log-storage.md) 已 accepted 原则，仅 operationalize 4B-β 实现细节。

## 背景

[ADR-008](008-multi-backend-crash-log-storage.md) 已决定：

- hook Phase 1 `RootSuBackend` + Phase 2 三后端并行
- 模块 `CrashLogIngestCoordinator` harvest relay → merge canonical
- canonical SSOT 不变

**4B-α as-built** 仅实现 Phase 2（Provider + DirectFs + TargetRelay）。Phase 2 并行时，各 backend 可能 **各自 append canonical**，导致 **同一 `id` 多行**（见 [crash-log-backends.md](../architecture/crash-log-backends.md) §backendWritten）。

若无 dedupe：

- 历史列表 / 统计重复计数
- retention 按行截断行为失真

本 ADR 在 4B-β 编码前锁定 **dedupe 位置与 merge 算法**，避免 UI 与 ingest 各写一套逻辑。

## 决策（accepted）

### 1. 4B-β 范围：完整落实 ADR-008 两阶段

| 组件 | 进程 | 职责 |
|------|------|------|
| `RootSuBackend` | hook | Phase 1，`su` append canonical，≤1500ms |
| `CrashLogIngestCoordinator` | 模块 | 启动 / Provider 回调 / 定时触发 harvest |
| `RootFsBackend` | 模块 | libsu 扫描各 app `crashcenter_relay/` |
| `RelayMergeBackend` | 模块 | 将 relay 行 merge 进 canonical |

**不选**「永久仅 Phase 2、放弃 hook RootSu」——除非 IS 矩阵证明 root 路径无收益且 relay+Provider 覆盖率 ≥ 目标（见 §验收闸门）。

### 2. Dedupe SSOT：ingest merge 时按 `id` 合并

**主路径**（模块进程 `CrashLogIngestCoordinator` / `RelayMergeBackend`）：

```
merge(relayLines, canonicalLines):
  index canonical by id (last wins by file order)
  for each relay event:
    if id not in canonical → append
    if id exists → skip append; optionally union backendWritten in meta (4B-β+)
  rewrite or append-only per performance (≤500 lines: full rewrite acceptable)
```

- **唯一键**：`CrashEvent.id`（UUID v4，hook 生成）
- **禁止**按 stack 文本 dedupe（同 stack 不同次 crash 须保留）
- merge 后 canonical **至多一行 / id**

### 3. Phase 2 并行写入：收敛为「单 canonical append + relay 副本」

4B-β 实施时调整 backend 职责，减少 ingest 压力：

| Backend | canonical | relay |
|---------|-----------|-------|
| `RootSuBackend` | append | — |
| `ProviderBackend` | append（模块 Provider 写 canonical） | — |
| `DirectFsBackend` | **优先写 relay**；canonical 仅当同 UID 直写成功 | `files/crashcenter_relay/` |
| `TargetRelayBackend` | 不写 canonical | relay only |

目标：Phase 2 常态下 **canonical 单行 / 事件**；relay 供模块 harvest 补漏。

若短期无法改 DirectFs 行为，则 **ingest dedupe（§2）为必做**，不可省略。

### 4. 读路径：防御性 dedupe（可选、cheap）

`FileCrashLogRepository.getAll` / PagingSource **可**在 4B-β 增加 `distinctBy { id }`（保留 timestamp 最大者），作为 merge bug 的安全网。

- **不替代** ingest dedupe（SSOT 仍须 canonical 文件干净）
- 默认 **开启**（成本低，500 条内 O(n) 可接受）

### 5. 新字段（merge 时写入）

| 字段 | 说明 |
|------|------|
| `backendWritten` | 已有；merge 时 union 各 backend id |
| `ingestedFrom` | relay 路径或 packageName；4B-β 新增 |
| `ingestedAtMs` | 模块 merge 时间；可选 |

破坏性：仅 append 新行字段；旧行无 `ingestedFrom` 时 UI 隐藏。

## 验收闸门

**Accepted 条件**（写入 `dev/verification/crash_log_is_*.md`）：

| # | 条件 |
|---|------|
| G1 | IS-1 或 IS-2 至少一条写入路径成功 |
| G2 | force-stop 模块后 relay harvest → canonical 可见 |
| G3 | 同一 crash 触发后 canonical **≤1 行 / id**（抽样 10 次） |
| G4 | 历史列表计数与 `getCount` 一致（无 duplicate 行） |

若 G1 失败且仅 Provider 可用：仍 accept ADR-017 §2+§4，**defer §1 RootSu** 并更新 roadmap 4B-β scope（须在 verification 报告显式记录）。

## 后果

- **正面**：统计与历史 SSOT 一致；落实 ADR-008 完整叙事
- **负面**：ingest 逻辑 + libsu 体积；merge rewrite 短暂 IO 峰值
- **跟进**：`CrashLogIngestCoordinator` 单元测试（temp dir JSONL）；IS-R 矩阵

## 备选方案

| 方案 | 不选原因 |
|------|----------|
| 仅读路径 dedupe，ingest 不 merge | canonical 文件仍膨胀；retention 按行失效 |
| 按 stack hash dedupe | 合并不同次相同 bug，丢失频次 |
| hook 协调器全局锁只 append 一次 | 跨 backend 失败域复杂；与 ADR-008 并行哲学冲突 |
| Room 主存储 | ADR-007 明确 JSONL 先行 |

## 相关文档

- [ADR-008](008-multi-backend-crash-log-storage.md) — 多后端总决策
- [crash-log-backends.md](../architecture/crash-log-backends.md) — Tier 与 wire name
- [crash-log-ipc.md](../architecture/crash-log-ipc.md) — IS 矩阵
- [crash-data-layer.md](../architecture/crash-data-layer.md) — Repository 读口
- [dev/roadmap/active/phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md) — 4B-β checklist
- [dev/plans/architecture-decision-backlog.md](../../dev/plans/architecture-decision-backlog.md) — D-01、D-02
