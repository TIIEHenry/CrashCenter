---
title: "作用域与偏好模型"
type: architecture
status: accepted
phase: N/A
updated: 2026-06-23
summary: "SharedPreferences 键、ScopePolicy install/intercept；ADR-023 全量观测"
---

# 作用域与偏好模型

> 适用模块：`:app`
> 源码：`PrefManager.kt`、`ConfigFragment.kt`、`XposedEntry.kt`、`ScopePolicy.kt`
> 相关 ADR：[ADR-002](../decisions/002-inverted-package-toggle.md)、[ADR-003](../decisions/003-xsharedpreferences-cross-process.md)、[ADR-023](../decisions/023-injection-observe-intercept-split.md)

## 概述

CrashCenter 通过 SharedPreferences 在 UI 进程写入配置，Xposed hook 侧通过 XSharedPreferences 读取，驱动 `ScopePolicy` 输出 `ScopeDecision`（**安装捕获** / **是否拦截** / 通知 / 日志）。

**ADR-023（as-built）**：

| 字段 | 含义 |
|------|------|
| `shouldInstall` | 是否 hook `Application.onCreate` 并安装 `CrashHandler`（默认对 LSPosed 已加载包为 true） |
| `shouldIntercept` | Switch ON / `CATCH_ALL.enabled` → Looper 续命 + 吞异常 |
| `showNotify` | 拦截模式下 Toast/通知；观测模式默认 false |
| `crashLogEnabled` | 是否写 `events.jsonl`；观测/拦截均可 |

`managed_packages` 为**策展配置集**（per-app 拦截偏好），**不再**门控是否注入。外层门控仍为 **LSPosed 作用域**。

## 偏好键

| Key | 类型 | 默认值 | 含义 |
|-----|------|--------|------|
| `crash` | — | — | SharedPreferences 文件名（`PREF_NAME`） |
| `scope_mode` | boolean | `false` | 作用域模式开关 |
| `handle_system` | boolean | `false` | scope 模式下是否包含系统应用 |
| `show_system_ui` | boolean | `false` | **UI 侧**是否在列表中显示系统应用（不影响 hook 行为） |
| `package_list` | Set\<String\> | 空 | **禁用**包名列表（见 ADR-002；**Legacy**，新模型见 ADR-015） |
| `managed_packages` | Set\<String\> | **`null`** | 受管应用策展集；`null` = Legacy；**不**门控注入（ADR-023） |
| `intervention_rules` | String（JSON） | `{}` | 包名 → 干预规则；`CATCH_ALL.enabled` → `shouldIntercept` |
| `managed_model_migrated` | boolean | `false` | 一次性从 ADR-002 模型迁移标记 |
| `observe_intercept_split_migrated` | boolean | `false` | ADR-023 行为迁移标记（无 prefs 重写） |

### `show_system_ui` 语义

`show_system_ui` 是纯 UI 侧过滤键，**不参与 hook 决策**：

| `show_system_ui` | `handle_system` | UI 列表 | 捕获安装（hook 侧） |
|-------------------|-----------------|---------|---------------------|
| `false` | `false` | 仅第三方 app | scope=true 时不安装系统 app 捕获 |
| `false` | `true` | 仅第三方 app | scope=true 时仍安装系统 app 捕获 |
| `true` | `true` | 全部 app | scope=true 时安装系统 app 捕获 |
| `true` | `false` | 全部 app | scope=true 时不安装系统 app 捕获 |

源码：`PrefManager.PREF_SHOW_SYSTEM_UI`；UI 读取位于 `ConfigFragment.kt` / `BaseConfigViewModel.kt` FilterChip（"显示系统应用"）。

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

## Scope Mode 行为（ADR-023）

| scope_mode | shouldInstall | shouldIntercept |
|------------|---------------|-----------------|
| 任意 | 对 LSPosed 已加载包默认 true（除忽略包） | Switch / `CATCH_ALL.enabled`；Legacy `package_list` 内为 false |
| `true` + 系统 app + `!handle_system` | false | — |

## UI 与存储的映射

### Legacy（`managed_packages == null`）

Switch **开启** = 拦截；**关闭** = 仅观测（写入 `package_list`）：

```
Switch ON  → shouldIntercept=true（包名 NOT IN package_list）
Switch OFF → shouldIntercept=false（包名 IN package_list）
```

初始化时：`prefWhiteList == null`（首次使用）→ 所有 app 默认 checked（全部拦截）。

### 受管模型（ADR-015，`managed_packages != null`）

| UI 操作 | 存储 |
|---------|------|
| Picker 添加 | `managed_packages += pkg` |
| Switch ON（无规则） | `intervention_rules[pkg]` append `CATCH_ALL` enabled | `shouldIntercept=true` |
| Switch OFF | 全部 rules `enabled=false` | `shouldIntercept=false`（仍 `shouldInstall=true`） |
| 编辑页改规则 | 更新 `intervention_rules` JSON |
| 移除应用 | 删 pkg + profile |

Hook 侧由 `ScopePolicy` 读 `managed_packages` + `intervention_rules`；**不读** `package_list`。详见 [app-management-ui.md](app-management-ui.md)。

### `intervention_rules` JSON 结构（摘要）

```json
{
  "com.example.app": {
    "rules": [
      {
        "id": "uuid",
        "type": "CATCH_ALL",
        "enabled": true,
        "showNotify": null,
        "crashLogEnabled": null
      }
    ],
    "updatedAt": 1718812800000
  }
}
```

v1 仅支持 `type: CATCH_ALL`。`null` 字段表示继承全局 prefs。

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

- [injection-observe-intercept-split.md](injection-observe-intercept-split.md)
- [ADR-023](../decisions/023-injection-observe-intercept-split.md)
- [ADR-015](../decisions/015-managed-apps-intervention-rules.md)
- [configuration-ui.md](configuration-ui.md)
- [xposed-entry.md](xposed-entry.md)
- [glossary.md](../glossary.md)
