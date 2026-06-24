---
title: "ADR-024: 分布式 cache 崩溃日志存储"
type: decision
status: accepted
phase: 4
updated: 2026-06-24
summary: "各 app cache 写（无 root）；UI 须 root 聚合读；无无 root 降级"
---

# ADR-024: 分布式 cache 崩溃日志存储

## 状态

**Accepted** — 已实施（见 [crash-log-distributed-storage.md](../architecture/crash-log-distributed-storage.md)）。

## 背景

[ADR-007](007-crash-log-cross-process-storage.md) 与 [ADR-008](008-multi-backend-crash-log-storage.md) 采用 **模块进程 canonical JSONL**（`files/crash_logs/events.jsonl`）为主记录，hook 经 DirectFs / Provider / relay / ingest 等多路径写入同一文件。

实践中：

1. 跨 UID 写模块 `filesDir` 在 Android 10+ 上不可靠（SELinux）
2. relay 与 canonical 双份存储增加 ingest、dedupe 复杂度
3. 用户期望：**崩溃数据归属发生 app**，模块仅**聚合展示**

需决策：是否取消 canonical，改为 **每 app 私有 `cache` JSONL + 模块 root 聚合读**。

## 决策

### 1. 取消模块 canonical SSOT

- **不再**维护 `/data/data/nota.android.crash.xp.app/files/crash_logs/events.jsonl` 作为全局权威源
- **不再**使用 `files/crashcenter_relay/` per-file 副本
- **不再**运行 `RelayMergeBackend` / `CrashLogIngestCoordinator` 的 merge → canonical 流程

### 2. 分布式写：每 app 一个 JSONL（cache）

路径：

```
/data/user/{userId}/{packageName}/cache/crash_logs/events.jsonl
```

- hook 侧 **仅**通过同 UID `LocalCacheBackend` append（由原 `TargetRelayBackend` 演进，格式由 per-file JSON 改为 JSONL append）
- CrashCenter 模块自身崩溃写入 **模块自己的** cache 路径
- retention：每文件 500 条 / 8 MB（与现硬顶一致）

选用 **`cacheDir` 而非 `filesDir`**：与用户明确要求一致；接受系统可能清理 cache 的风险（设置页 / 导出作补充）。

### 3. 模块读：root 聚合（非降级）

- `DistributedCrashLogRepository` **仅在 root 可用**时工作：root 扫描各 app（**含模块自身**）`cache/crash_logs/events.jsonl`
- 全局 `groupBy { id }`，冲突保留较大 `timestampMs`；列表 `timestampMs` 降序
- 删改 / `clear` / `applyRetention` **一律经 root** 定位目标包 cache 文件
- **不实现**无 root 读路径：沙箱下模块无法读他包 cache，**非产品降级**；无 root 时历史/统计/导出为空或提示，**不**部分展示本模块 cache

### 4. 精简 hook 写后端

| 组件 | 决策 |
|------|------|
| `LocalCacheBackend` | **主写路径**；`BackendId.LOCAL_CACHE` / `"local_cache"` |
| `DirectFsBackend` | **移除** |
| `ProviderBackend` / `CrashLogProvider` | **移除** |
| `RootSuBackend` | **移除** |
| `TargetRelayBackend` | **由 LocalCacheBackend 替代** |
| `RelayMergeBackend` | **移除** |
| `RootFsBackend` | **保留**：root **读**扫描 + **远程删改**（`RootMutationClient`）；**无** append |

`CrashLogBackend` 抽象保留；hook 注册表仅 `LocalCacheBackend`；模块侧 `RootFsBackend` 不实现 `CrashLogBackend.append` 写崩溃（或 `append` 返回 Failure）。

### 5. 字段、路径与配置

**路径 SSOT**：`CrashLogPaths`（见架构 doc §路径约定）；禁止再使用 `FileCrashLogRepository.eventsFile` 指向 `filesDir`。

**BackendId 枚举**：

```kotlin
enum class BackendId(val wireName: String) {
    LOCAL_CACHE("local_cache"),  // hook 写
    ROOT_FS("root_fsm"),         // 模块 root I/O
    // 移除: ROOT_SU, PROVIDER_INSERT, DIRECT_FS, TARGET_RELAY, RELAY_MERGE
}
```

| 字段 / pref | 决策 |
|-------------|------|
| `backendWritten` | `["local_cache"]` |
| `ingestedFrom` | **废弃** |
| `crash_log_backend_local_cache` | 默认 `true` |
| `distributed_cache_migrated` | 迁移完成标记 |
| 废弃 pref | `crash_log_backend_provider`、`direct_fs`、`relay`、`relay_merge`、`root_su`、`root_fs`（读恒需 root，不单开关） |

### 6. 升级迁移

| 项 | 决策 |
|----|------|
| 触发 | `Application.onCreate`；**须 root** |
| 幂等键 | `distributed_cache_migrated` |
| 顺序 | legacy canonical 拆分 → relay merge → per-target dedupe（`maxBy timestampMs`）→ 写 cache → 删 legacy |
| 无 root | 跳过；下次启动重试 |
| 同 id 重复 | 保留 `timestampMs` 较大者 |

详见 [crash-log-distributed-storage.md §升级与迁移](../architecture/crash-log-distributed-storage.md#升级与迁移)。

## 理由

| 选项 | 优点 | 缺点 |
|------|------|------|
| **A. 维持 canonical（现状）** | 曾设想 UI 无 root 可读全量 | 跨 UID 写不可靠；relay + ingest 复杂 |
| **B. 分布式 cache（本 ADR）** | 写同 UID 可靠；读模型与沙箱一致 | 读历史 **须 root**；cache 可被系统清理 |
| **C. 分布式 files** | 比 cache 更耐久 | 仍须 root 聚合读；与用户指定 cache 不符 |

**选择 B**：写路径同 UID、无需 root；读路径与 Android 沙箱一致（跨包读须 root），**不做无 root 降级**。

## 后果

### 正面

- 删除 DirectFs / Provider / relay merge 大量代码与 IS 矩阵分支
- 观测 `logSync` 简化为单后端同步写本 app cache
- 与 Android 沙箱数据归属一致

### 负面

- 无 root 时 **无法** 使用崩溃历史/统计（预期；非降级缺陷）
- `cache` 清理可能导致丢日志
- 全机 root 扫描引入延迟（须缓存与上限）

### 对既有 ADR 的关系

| ADR | 关系 |
|-----|------|
| [ADR-007](007-crash-log-cross-process-storage.md) | **Superseded**（canonical 主路径） |
| [ADR-008](008-multi-backend-crash-log-storage.md) | **Partially superseded**（并行写 canonical + ingest） |
| [ADR-017](017-root-ingest-and-dedupe.md) | **Superseded**（relay merge dedupe） |
| [ADR-021](021-canonical-jsonl-io-consistency.md) | **Amended**（per-app 文件 FileLock） |
| [ADR-023](023-injection-observe-intercept-split.md) | **Amended**（observe `logSync` → 同步 `LocalCacheBackend`） |
| [ADR-003](003-xsharedpreferences-cross-process.md) | **不变** |

## 实施备注

- 方案 / 实施 commit **分开**（[DOCUMENTATION.md](../DOCUMENTATION.md) 规则 3a）
- 本 ADR `accepted` 后再编码；编码后更新 `status.md`、roadmap 4B-δ、`crash-logging.md` as-built

## 相关文档

- [crash-log-distributed-storage.md](../architecture/crash-log-distributed-storage.md) — 完整方案
- [crash-log-backends.md](../architecture/crash-log-backends.md)
- [crash-logging.md](../architecture/crash-logging.md)
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md)
- [glossary.md](../glossary.md)
