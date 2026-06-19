---
title: "ADR-007: 崩溃日志跨进程存储"
type: decision
status: accepted
phase: 4
updated: 2026-06-19
summary: "JSONL canonical + Provider；编排扩展见 ADR-008"
---

# ADR-007: 崩溃日志跨进程存储

## 背景

hook 运行在**目标 app 进程**，崩溃日志须写入**模块进程**私有存储以供历史 UI 与统计读取。这与 [ADR-003](003-xsharedpreferences-cross-process.md) 的配置同步方向相反、数据量级不同：

| 维度 | ADR-003（prefs） | 崩溃日志 |
|------|------------------|----------|
| 方向 | UI 写 → hook 读 | hook 写 → UI 读 |
| 体量 | 小（boolean / Set） | 大（stack trace append 流） |
| 频率 | 用户改设置时 | 每次拦截崩溃 |
| ADR-003 结论 | ContentProvider **过度设计** | **不适用**于 crash log |

需单独决策 crash log 的跨进程持久化方式。方案分析见 [crash-logging.md](../architecture/crash-logging.md)。

## 决策

### 1. 主路径：JSONL append-only 文件

hook 侧通过 `createPackageContext(MODULE_PKG, CONTEXT_IGNORE_SECURITY).getFilesDir()` 异步 append 到：

```
files/crash_logs/events.jsonl
```

- 每行一条 JSON（[数据模型](../architecture/crash-logging.md#数据模型)）
- 后台线程写入；失败 **silent**（不阻塞 [CrashHandler](../architecture/crash-handler.md)、不 `System.exit`）
- 零新依赖，与项目极简哲学一致；便于 debug 与 export

### 2. Fallback：极简 ContentProvider（修订：无 signature permission）

若真机验证跨包直写失败（部分 Android 10+ / OEM），增加 **`CrashLogProvider`**：

- 仅暴露 `insert`（可选 `delete` 供 UI 清空）
- 实现 append 同一 JSONL 或内存队列 + 模块进程 flush
- **`exported="true"` 且不得使用 `protectionLevel="signature"` 的 `android:permission`** — hook 运行在**目标 app UID**，与模块不同签名，`checkCallingPermission` 会拒绝（见 [crash-log-ipc.md § signature 权限悖论](../architecture/crash-log-ipc.md#signature-权限悖论fallback-b-关键缺陷)）
- 安全改由 Provider 内实现：`callingUid` ↔ payload `packageName` 一致、rate limit、单条 stack 上限、retention 硬顶
- 模块进程未运行时 AM **按需拉起** Provider 所在进程 — 不依赖 UI 曾打开
- **不**在 MVP 引入 Room

Provider 在此场景**不是**过度设计：它是跨 ROM 写入模块存储的兼容层，而非 prefs 的替代。

### 3. MVP 不上 Room

| 选项 | 不选 / 延后的原因 |
|------|-------------------|
| **Room MVP** | 当前无历史 UI、无查询需求；JSONL 足够验证「全量记录」 |
| **Room 第二阶段** | 当 JSONL 全量扫描成为统计瓶颈时，再迁移或维护 sidecar 索引 |
| **仅 Logcat** | 不满足持久化与 UI 统计（见分析会话） |
| **XSharedPreferences 存事件体** | ADR-003 为 UI→hook **只读**；无写 API；XML 不适合 append / 大 stack；见 [crash-log-ipc FAQ](../architecture/crash-log-ipc.md#为何不用-xsharedpreferences-存崩溃日志) |
| **公开文件系统主路径** | 目标 UID 常无写权限；Scoped Storage；隐私与伪造风险；不优于 Provider；见 [crash-log-ipc FAQ](../architecture/crash-log-ipc.md#为何不用公开文件系统sdcard) |
| **外部存储主路径** | 同上；仅作 P3 SAF **导出**补充 |
| **Framework 注入代写** | 不能替代 app 级 hook；见 [framework-injection-feasibility.md](../architecture/framework-injection-feasibility.md) |

Room + Provider 仍是长期演进选项（统计查询强、单进程串行写），但以 **JSONL 先行、Provider 按需** 降低 MVP 成本。

## 演进（ADR-008）

[ADR-008](008-multi-backend-crash-log-storage.md) 在本决策之上增加：

- `CrashLogBackend` 多后端抽象
- hook 侧 root 优先 + 并行 fallback
- 模块侧 root ingest（参考 AppSnapShotor）
- target relay 同 UID 兜底

**本 ADR 的 canonical JSONL、Provider 权限模型、silent 失败原则仍然有效。**

## 后果

- **正面**：MVP 实现快；与 ADR-003 职责清晰分离；文件可人工 inspect
- **负面**：JSONL 复杂统计需扫描或 sidecar；跨包直写兼容性待真机验证；Provider 无 signature permission 时须防伪造写入
- **跟进**：Phase 4 MVP 真机验证写入路径（含**模块 force-stop / 从未打开 UI**）；失败则启用 Provider fallback（无 signature permission 设计）

## 与 ADR-003 的关系

- ADR-003 **仍然有效**：scope / 禁用列表继续 XSharedPreferences 只读
- 崩溃日志 **不**写入 `grapcrash` prefs（避免大字符串、无结构化查询）
- 崩溃 retention 开关（`crash_log_enabled` 等）可走 prefs，但 **事件体**走 JSONL / Provider

## 相关

- 方案文档：[crash-logging.md](../architecture/crash-logging.md)
- IPC 与独立启动：[crash-log-ipc.md](../architecture/crash-log-ipc.md)
- [xposed-entry.md](../architecture/xposed-entry.md) — hook 回调写入点
- [ADR-003](003-xsharedpreferences-cross-process.md) — 配置跨进程（只读）
- [ADR-008](008-multi-backend-crash-log-storage.md) — 多后端并行编排
- [crash-log-backends.md](../architecture/crash-log-backends.md)
