---
title: "ADR-008: 崩溃日志多后端并行存储"
type: decision
status: accepted
phase: 4
updated: 2026-06-19
summary: "CrashLogBackend 抽象；hook root 优先并行写入；模块 root ingest 管理 relay；ADR-007 canonical 仍有效"
---

# ADR-008: 崩溃日志多后端并行存储

## 背景

[ADR-007](007-crash-log-cross-process-storage.md) 确立 JSONL canonical + ContentProvider fallback 的 **串行** 主备路径。后续分析明确：

1. **Xposed 用户通常已 root**（Magisk / KernelSU + LSPosed），模块进程可借鉴 [AppSnapShotor](/home/clarence/Projects/Android/AppSnapShotor) 的 libsu + RootService 读私有目录
2. hook 运行在 **目标 UID**，设备有 root ≠ 目标进程能 `su`（DenyList）
3. 单一路径在 SELinux / 包可见性 / DenyList 下无法保证覆盖率

需决策：是否在 ADR-007 之上引入 **多后端抽象、并行 IPC、root 优先**，以及 **模块侧 root ingest** 管理各 app 私有 relay。

方案详述见 [crash-log-backends.md](../architecture/crash-log-backends.md)。

## 决策

### 1. 引入 CrashLogBackend 抽象

每种 IPC / 特权路径实现统一接口（`probe` + `append`），由注册表集中管理。hook 侧与模块侧 **分表注册**，不得混进程 bind RootService。

### 2. hook 侧：root 优先 + 并行 fallback

```
Phase 1: RootSuBackend (tier 0, ≤1500ms)
Phase 2: ProviderBackend ∥ DirectFsBackend ∥ TargetRelayBackend (≤2000ms)
```

- 独立后台线程；失败 **silent**
- 不与 Toast / 通知同 try（避免 `System.exit`）
- `TargetRelayBackend` 写目标 app 私有 `files/crashcenter_relay/` 作 **同 UID 兜底**

### 3. 模块侧：root ingest + canonical 管理

- **Canonical SSOT 不变**：`files/crash_logs/events.jsonl`（ADR-007）
- 模块进程 `RootFsBackend`（libsu，参考 AppSnapShotor）**优先**扫描各 app relay → merge → dedupe
- UI 列表 / 清空 / retention / 导出读 **同 UID canonical**，不依赖每次 root
- hook 进程 **不** 依赖 libsu AAR

### 4. ADR-007 组件保留

| 组件 | 状态 |
|------|------|
| canonical JSONL | 保留 |
| CrashLogProvider（无 signature permission） | 保留，降为 tier 1 后端 |
| DirectFs append | 保留，tier 2 |
| MVP 不上 Room | 保留 |

### 5. 配置开关

`crash_log_backend_*`、`crash_log_relay_always`、`crash_log_ingest_on_start` 等经 prefs 暴露；hook 经 XSharedPreferences **只读**（ADR-003）。

## 后果

- **正面**：覆盖率高于串行 A→B；root 用户充分利用已有特权；relay + ingest 覆盖 DenyList
- **负面**：实现与测试矩阵复杂度上升；需 dedupe；hook / 模块双 root 路径须严格分进程
- **跟进**：Phase 4B 分 α（Provider + relay + Coordinator）、β（root_su + ingest）；验收 IS-R1~IS-R5

## 备选方案

| 方案 | 不选 / 延后的原因 |
|------|-------------------|
| 仅 ADR-007 串行 A→B | 覆盖率不足；未利用 root 用户群 |
| hook 内 bind RootService | 目标进程不宜链 libsu；与 AppSnapShotor 进程模型不符 |
| 仅模块 root、无 hook 多后端 | hook 不写 relay 则模块无可 harvest（模块未运行） |
| 公开 /sdcard/ 主路径 | 见 [crash-log-ipc FAQ](../architecture/crash-log-ipc.md#为何不用公开文件系统sdcard) |
| Framework 代写 | 见 [framework-injection-feasibility.md](../architecture/framework-injection-feasibility.md) |

## 与 ADR-007 的关系

- ADR-007 **不废止**：canonical 格式、Provider 权限模型、silent 失败哲学不变
- ADR-008 是 **编排层与后端扩展**，取代「仅 Primary A 失败再 B」的单一叙事

## 相关

- [crash-log-backends.md](../architecture/crash-log-backends.md)
- [crash-log-ipc.md](../architecture/crash-log-ipc.md)
- [crash-logging.md](../architecture/crash-logging.md)
- [ADR-007](007-crash-log-cross-process-storage.md)
- [ADR-003](003-xsharedpreferences-cross-process.md)
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md)
