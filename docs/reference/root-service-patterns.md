---
title: "Root 服务参考模式"
type: reference
status: accepted
phase: 4
updated: 2026-06-22
summary: "从 AppSnapShotor 提炼的 libsu RootService 模式；CrashCenter 统一方案见 unified-root-service.md"
---

# Root 服务参考模式

> **参考项目**：[AppSnapShotor](sibling-projects.md#appsnapshotor)（GitHub: [TIIEHenry/AppSnapshoter](https://github.com/TIIEHenry/AppSnapshoter)）
>
> CrashCenter **不**复制 AppSnapShotor 的双 Root 服务与 AIDL 压缩管线；本文提炼上游参考模式。
>
> **CrashCenter 目标架构**（单 `CrashCenterRootService` + `RootBroker` 子 Binder）：[unified-root-service.md](../architecture/unified-root-service.md)（`proposed`）。

## 适用边界

| 场景 | CrashCenter 是否采用 root | 说明 |
|------|---------------------------|------|
| 模块 UI 读 canonical JSONL | 否 | 同 UID `files/crash_logs/` |
| 模块 ingest 读各 app `crashcenter_relay/` | **是（优先）** | 模块进程 libsu，参考 AppSnapShotor |
| hook 目标进程写模块目录 | 视 DenyList | 目标 UID 内 `su`，可靠性低于模块侧 |
| prefs 一次性迁移 | 可选 | `PrefMigrator` 已用 `su cat` 读旧包 prefs |

**关键区分**（与 [crash-log-ipc.md](../architecture/crash-log-ipc.md) 一致）：

- 设备有 root ≠ hook 进程能 su（Magisk DenyList、目标 app 策略）
- AppSnapShotor root 在 **自家 Application 进程**；CrashCenter hook 在 **被注入的目标进程**

## AppSnapShotor 模式摘要

AppSnapShotor 为 root-required 备份工具，同一 APK 内 **App UID 进程 ↔ Root UID 子进程**：

```
App 进程（模块 UID）
  ProvidersImpl
    ├─ bindRootService() → SnapshotRootService (AIDL, 30+ RPC)
    └─ bindRootService() → FileSystemManagerRootService (libsu-nio)
Root 子进程（uid 0）
  Handler 层：PackageManager / FileSystem / Permission …
```

启动序列（`SnapshotApp.onCreate`）：MMKV → `ProvidersImpl` → `Shell.getShell().isRoot` → 双 RootService bind。

详见 [sibling-projects.md](sibling-projects.md) 登记的上游路径 `docs/architecture/root-service.md` 与 `cross-cutting/security.md`。

## CrashCenter 映射（Phase 4 设计）

CrashCenter 为 **单模块 `:app`**，ingest 侧拟采用 **轻量变体**（无独立 `:provider` APK）：

```
模块 app 进程 (nota.android.crash.xp.app)
  CrashLogIngestCoordinator
    RootFsBackend (tier 0)
      libsu RootService 或 ProcessShell（读 */files/crashcenter_relay/）
    RelayMergeBackend + LocalFsBackend → events.jsonl (SSOT)
```

| AppSnapShotor 概念 | CrashCenter 对应 | 差异 |
|--------------------|------------------|------|
| `FileSystemManagerRootService` | `RootFsBackend` | 只读 relay + 可选删已合并文件 |
| `ProvidersImpl` 门面 | `CrashLogIngestCoordinator` | 无 AIDL 压缩、无 PackageManager RPC |
| `Shell.getShell().isRoot` | ingest 启动前探测 | 无 root 时降级 RelayMerge / Provider |
| SELinux `su` 域 | 同 libsu 约束 | hook 侧 `RootSuBackend` 另评估 DenyList |

实现 checklist 见 [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md) 与 [ADR-008](../decisions/008-multi-backend-crash-log-storage.md)。

## 实现约束

1. **RootService 生命周期**：绑定在 Application / 单例协调器；避免 Activity 泄漏（AppSnapShotor 用进程级 `ProvidersImpl`）。
2. **超时**：hook 侧 root 写入 ≤1.5s；ingest 批量扫描可更长但须后台线程。
3. **失败 silent**：root 不可用不得影响 [CrashHandler](../architecture/crash-handler.md) 吞异常语义。
4. **不引入 libsu 到 hook 热路径除非必要**：优先模块侧 root harvest，hook 侧 Provider / relay 兜底。

## 相关文档

- [unified-root-service.md](../architecture/unified-root-service.md) — CrashCenter 单 RootService + Broker 方案
- [crash-log-backends.md](../architecture/crash-log-backends.md) — 多后端编排与 SSOT
- [crash-log-ipc.md](../architecture/crash-log-ipc.md) — IPC 机制对比与 FAQ
- [ADR-008](../decisions/008-multi-backend-crash-log-storage.md) — root 优先并行决策
- [scope-and-prefs.md](../architecture/scope-and-prefs.md) — `PrefMigrator` root 读旧 prefs
- [glossary.md](../glossary.md) — CrashLogBackend / crashcenter_relay 术语
