---
title: "作用域与偏好模型"
type: architecture
status: accepted
phase: N/A
updated: 2026-06-19
summary: "SharedPreferences 键、scope 模式与跨进程同步；legacy tiiehenry.xp.grapcrash 迁移"
---

# 作用域与偏好模型

> 适用模块：`:app`
> 源码：`PrefManager.java`、`ActivityMain.kt`、`XposedEntry.java`
> 相关 ADR：[ADR-002](../decisions/002-inverted-package-toggle.md)、[ADR-003](../decisions/003-xsharedpreferences-cross-process.md)

## 概述

CrashCenter 通过 SharedPreferences 在 UI 进程写入配置，Xposed hook 侧通过 XSharedPreferences 读取，控制哪些 app 被 hook 以及通知行为。

## 偏好键

| Key | 类型 | 默认值 | 含义 |
|-----|------|--------|------|
| `crash` | — | — | SharedPreferences 文件名（`PREF_NAME`） |
| `scope_mode` | boolean | `false` | 作用域模式开关 |
| `handle_system` | boolean | `false` | scope 模式下是否包含系统应用 |
| `package_list` | Set\<String\> | 空 | **禁用**包名列表（见 ADR-002） |

### Phase 4 计划键（崩溃观测，待实现）

| Key | 类型 | 默认值 | 含义 |
|-----|------|--------|------|
| `crash_log_enabled` | boolean | `true` | hook 侧是否持久化每次拦截崩溃 |
| `crash_log_max_entries` | int | `500` | JSONL retention 上限（条数） |
| `crash_log_backend_root_su` | boolean | `true` | hook Phase1 `RootSuBackend` |
| `crash_log_backend_provider` | boolean | `true` | `ProviderBackend` |
| `crash_log_backend_direct_fs` | boolean | `true` | `DirectFsBackend` |
| `crash_log_backend_relay` | boolean | `true` | `TargetRelayBackend` |
| `crash_log_relay_always` | boolean | `false` | root 成功仍写 relay（双保险） |
| `crash_log_ingest_on_start` | boolean | `true` | 模块启动时 root ingest relay |

**注意**：`CrashEvent` 事件体（stack trace 等）**不**存入 prefs — 见 [crash-log-backends.md](crash-log-backends.md)、[ADR-008](../decisions/008-multi-backend-crash-log-storage.md)。

## Scope Mode 行为

| scope_mode | Hook 范围 | showNotify |
|------------|-----------|------------|
| `true` | 非系统包 + 可选系统包，排除禁用列表 | 始终 true（对被 hook 的包） |
| `false` | 全部包（仍排除 android、Xposed 管理器） | 仅非禁用包为 true |

## UI 与存储的映射

ActivityMain 中 Switch **开启**（checked）表示 app **被 hook**：

```
Switch ON  → 包名 NOT IN package_list
Switch OFF → 包名 IN package_list (disabled)
```

初始化时：`prefWhiteList == null`（首次使用）→ 所有 app 默认 checked（全部 hook）。

## 跨进程读取

```java
// XposedEntry
sXSharedPreferences = new XSharedPreferences(PACKAGE_NAME, PrefManager.PREF_NAME);
sXSharedPreferences.reload();  // 每次 handleLoadPackage 时
```

`PACKAGE_NAME` = `nota.android.crash.xp.app`（`PrefManager.PACKAGE_NAME`），必须与模块 applicationId 一致。

旧包 `tiiehenry.xp.grapcrash` / `grapcrash.xml` 由 `PrefMigrator` 在首次启动时一次性导入，之后仅读写 `crash.xml`。

## 注意事项

- `StringSet` 在 SharedPreferences 中是 copy-on-write，修改时需创建新 Set
- XSharedPreferences 读取有延迟，UI 修改后需重启目标 app 或等待 reload
- 崩溃历史不走 prefs，见 [crash-log-ipc.md](crash-log-ipc.md)

## 相关文档

- [crash-log-backends.md](crash-log-backends.md) — 多后端与 ingest
- [crash-log-ipc.md](crash-log-ipc.md) — 配置 vs 事件体分工

- [configuration-ui.md](configuration-ui.md)
- [xposed-entry.md](xposed-entry.md)
- [glossary.md](../glossary.md)
