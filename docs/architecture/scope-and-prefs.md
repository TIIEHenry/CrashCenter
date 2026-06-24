---
title: "作用域与偏好模型"
type: architecture
status: accepted
phase: N/A
updated: 2026-06-24
summary: "SharedPreferences 键、ScopePolicy install/intercept；managed_packages 单模型"
---

# 作用域与偏好模型

> 适用模块：`:app`
> 源码：`PrefManager.kt`、`ConfigFragment.kt`、`XposedEntry.kt`、`ScopePolicy.kt`
> 相关 ADR：[ADR-003](../decisions/003-xsharedpreferences-cross-process.md)、[ADR-023](../decisions/023-injection-observe-intercept-split.md)

## 概述

CrashCenter 通过 SharedPreferences 在 UI 进程写入配置，Xposed hook 侧通过 XSharedPreferences 读取，驱动 `ScopePolicy` 输出 `ScopeDecision`（**安装捕获** / **是否拦截** / 通知 / 日志）。

**ADR-023（as-built）**：

| 字段 | 含义 |
|------|------|
| `shouldInstall` | 是否 hook `Application.onCreate` 并安装 `CrashHandler`（对 LSPosed 已加载包默认 true，系统 app 受 `handle_system` 约束） |
| `shouldIntercept` | 包名在 `managed_packages` 中 → Looper 续命 + 吞异常 |
| `showNotify` | 拦截模式下 Toast/通知；观测模式 false |
| `crashLogEnabled` | 是否写 `events.jsonl`；观测/拦截均可 |

外层门控仍为 **LSPosed 作用域**；`managed_packages` 仅表示 per-app **拦截开关**（不在集合内 = 仅观测）。

## 偏好键

| Key | 类型 | 默认值 | 含义 |
|-----|------|--------|------|
| `crash` | — | — | SharedPreferences 文件名（`PREF_NAME`） |
| `handle_system` | boolean | `false` | 是否对系统 app 安装捕获 |
| `show_system_ui` | boolean | `false` | **UI 侧**是否在列表中显示系统应用（不影响 hook 行为） |
| `managed_packages` | Set\<String\> | 空 | **已启用拦截**的包名集合 |

**已移除**（2026-06-24，无运行时迁移）：`package_list`、`scope_mode`、`intervention_rules`、各类 `*_migrated` 标记、`PrefMigrator` / 旧包 `tiiehenry.xp.grapcrash` 导入。历史决策见 [ADR-014](../decisions/014-legacy-prefs-migration.md)（superseded）、[ADR-015](../decisions/015-managed-apps-intervention-rules.md)（部分 superseded）。

### `show_system_ui` 语义

`show_system_ui` 是纯 UI 侧过滤键，**不参与 hook 决策**：

| `show_system_ui` | `handle_system` | UI 列表 | 捕获安装（hook 侧） |
|-------------------|-----------------|---------|---------------------|
| `false` | `false` | 仅第三方 app | 不安装系统 app 捕获 |
| `false` | `true` | 仅第三方 app | 安装系统 app 捕获 |
| `true` | `true` | 全部 app | 安装系统 app 捕获 |
| `true` | `false` | 全部 app | 不安装系统 app 捕获 |

源码：`PrefManager.PREF_SHOW_SYSTEM_UI`；UI 读取位于 `ConfigFragment.kt` FilterChip（「显示系统应用」）。

### Phase 4 键（崩溃观测，as-built）

| Key | 类型 | 默认值 | 含义 |
|-----|------|--------|------|
| `crash_log_enabled` | boolean | `true` | hook 侧是否持久化每次捕获的崩溃 |
| `crash_log_max_entries` | int | `500` | JSONL retention 上限（条数） |
| `crash_log_backend_root_su` | boolean | `true` | hook Phase1 `RootSuBackend` |
| `crash_log_backend_provider` | boolean | `true` | `ProviderBackend` |
| `crash_log_backend_direct_fs` | boolean | `true` | `DirectFsBackend` |
| `crash_log_backend_relay` | boolean | `true` | `TargetRelayBackend` |
| `crash_log_relay_always` | boolean | `false` | root 成功仍写 relay（双保险） |
| `crash_log_ingest_on_start` | boolean | `true` | 模块启动时 root ingest relay |

**注意**：`CrashEvent` 事件体（stack trace 等）**不**存入 prefs — 见 [crash-log-backends.md](crash-log-backends.md)、[ADR-008](../decisions/008-multi-backend-crash-log-storage.md)。

## ScopePolicy 行为（as-built）

```kotlin
// ScopePolicy.evaluate — 摘要
interceptEnabled = managed_packages.contains(packageName)
shouldInstall  = passesSystemFilter && packageName !in IGNORED_PACKAGES
shouldIntercept = interceptEnabled && shouldInstall
```

| 条件 | shouldInstall | shouldIntercept |
|------|---------------|-----------------|
| LSPosed 已加载 + 第三方 app | true | 包在 `managed_packages` |
| 系统 app + `handle_system=false` | false | false |
| 忽略包（`android`、Xposed 管理器等） | false | false |
| 包不在 `managed_packages` | true（若通过系统过滤） | false（仅观测） |

## UI 与存储的映射

`ConfigFragment` 展示**已安装应用**列表（可按系统/拦截状态筛选）：

| UI 操作 | 存储 |
|---------|------|
| Switch **ON** | `managed_packages += packageName` |
| Switch **OFF** | `managed_packages -= packageName` |

无独立「受管列表」、无 `intervention_rules` JSON、无添加应用 BottomSheet。

## 跨进程读取

```java
// XposedEntry
sXSharedPreferences = new XSharedPreferences(PACKAGE_NAME, PrefManager.PREF_NAME);
sXSharedPreferences.reload();  // 每次 handleLoadPackage 时
```

`PACKAGE_NAME` = `nota.android.crash.xp.app`（`PrefManager.PACKAGE_NAME`），必须与模块 applicationId 一致。

## 注意事项

- `StringSet` 在 SharedPreferences 中是 copy-on-write，修改时需创建新 Set
- XSharedPreferences 读取有延迟，UI 修改后需重启目标 app 或等待 reload
- 崩溃历史不走 prefs，见 [crash-log-ipc.md](crash-log-ipc.md)

## 相关文档

- [app-management-ui.md](app-management-ui.md) — 配置 tab UI
- [injection-observe-intercept-split.md](injection-observe-intercept-split.md)
- [ADR-023](../decisions/023-injection-observe-intercept-split.md)
- [configuration-ui.md](configuration-ui.md)
- [xposed-entry.md](xposed-entry.md)
- [glossary.md](../glossary.md)
