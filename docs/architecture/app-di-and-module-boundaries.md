---
title: "模块 DI 与进程边界"
type: architecture
status: accepted
phase: 4
updated: 2026-06-20
summary: "ServiceLocator 手动 DI、ViewModelFactory、hook 包禁止依赖 xp.app 的门禁与测试替身"
---

# 模块 DI 与进程边界

> 适用模块：`:app`
> 壳层 UI：[ui-routing.md](ui-routing.md)、[ADR-009](../decisions/009-ui-shell-design-system.md)
> 数据读口：[crash-data-layer.md](crash-data-layer.md)
> DI 框架决策 defer：[dev/plans/architecture-decision-backlog.md](../../dev/plans/architecture-decision-backlog.md) D-05 / ADR-020

## 概述

CrashCenter 为 **单 APK Xposed 模块**，运行时存在两类进程：

| 进程 | 入口 | 可读写的配置 | 禁止 |
|------|------|--------------|------|
| **目标 app**（hook） | `XposedEntry` | `XSharedPreferences`、目标 UID `files/`、Provider insert | import `nota.android.crash.xp.app.*`（除 Provider 契约常量） |
| **模块 app**（UI） | `MainShellActivity` | `SharedPreferences`、模块 `files/crash_logs/` | 运行 Xposed hook、libsu 在 hook 路径 |

模块进程内 UI 层采用 **手动 Service Locator** 提供 Repository 单例；ViewModel 经 `ViewModelFactory` 注入。Hilt 引入为可选演进（ADR-020），**非** as-built 前置。

## ServiceLocator

```kotlin
object ServiceLocator {
    fun legacyAppRepository(context: Context): LegacyAppRepository
    fun managedAppRepository(context: Context): ManagedAppRepository
    fun packageVisibilityRepository(context: Context): PackageVisibilityRepository
    fun crashLogRepository(context: Context): CrashLogRepository
    fun clear()  // 测试用：重置单例
}
```

| 依赖 | 实现 | 生命周期 |
|------|------|----------|
| `LegacyAppRepository` | `LegacyAppRepository` | Application 级单例 |
| `ManagedAppRepository` | `ManagedAppRepository` | Application 级单例 |
| `PackageVisibilityRepository` | `PackageVisibilityRepository` | Application 级单例 |
| `CrashLogRepository` | `FileCrashLogRepository` | Application 级单例 |

**约定**：

- 仅 `Application` / `Context.applicationContext` 传入构造，避免 Activity 泄漏
- Fragment / Activity 通过 `ServiceLocator.xxx(requireContext())` 获取；**不**在 common UI 组件内直接调用
- 单元测试在 `@Before` 调用 `ServiceLocator.clear()`，注入 fake 前须先 clear

## ViewModelFactory

`ViewModelFactory` 接受 lambda 工厂，供 `by viewModels { ViewModelFactory { ... } }` 使用：

```kotlin
CrashHistoryViewModel(ServiceLocator.crashLogRepository(requireContext()))
ConfigViewModel(
    ServiceLocator.legacyAppRepository(requireContext()),
    ServiceLocator.managedAppRepository(requireContext()),
    ServiceLocator.packageVisibilityRepository(requireContext()),
)
```

**约定**：

- ViewModel **不**持有 `Activity` / `Fragment` 引用
- IO 工作在 ViewModel 内 `viewModelScope` + `Dispatchers.IO`（或构造函数注入 dispatcher，便于测试）

## 包边界门禁

### hook 域（`nota.android.crash` 除 `xp.app`）

| 允许 | 禁止 |
|------|------|
| `capture`、`log`、`feedback`、`xp`（XposedEntry、PrefManager 常量） | `import nota.android.crash.xp.app.config.*` |
| 反射 / IPC 调用 `CrashLogProvider` URI | `import nota.android.crash.xp.app.data.*` |
| | libsu / RootService |

验收：`./gradlew :app:assembleDebug` + 代码审查；可选 CI grep hook 包无 `xp.app` import（`CrashLogProvider` 契约类除外）。

### 模块 UI 域（`nota.android.crash.xp.app`）

| 允许 | 禁止 |
|------|------|
| `shell`、`config`、`observe`、`data`、`di`、`common.ui` | 修改 hook 语义（scope 判定须在 hook 侧） |
| 读 `events.jsonl`、写 `SharedPreferences` | 在 UI 进程调用 `CrashLogCoordinator`（仅 hook 写入） |

### 共享契约

| 类型 | 位置 | 说明 |
|------|------|------|
| `CrashEvent` JSON schema | hook `log` + 模块 `data` | 字段须与 [crash-logging.md](crash-logging.md) 一致 |
| `PrefManager` keys | `xp/PrefManager.kt` | UI 写、hook 只读（ADR-003） |
| Provider URI | `CrashLogContract` | hook insert、模块 Provider 实现 |

## 与架构分层的对应

```
shell / config / observe     ← Domain Page（ADR-009）
    ↓ ServiceLocator
data (Repository)            ← 读口 SSOT
    ↓ files
events.jsonl

hook: capture / log          ← 写口（独立进程）
    ↓ IPC / relay
events.jsonl 或 relay 目录
```

Design System（`common.ui`）**不**依赖 ServiceLocator；通过 callback / 上层 Fragment 注入行为。

## 测试替身

| 测试 | 模式 |
|------|------|
| `ConfigViewModelTest` | mock `LegacyAppRepository` / `ManagedAppRepository` |
| `CrashLogCoordinatorTest` | 内存 backend 列表，无 Android |
| Repository 测试 | 临时目录 `events.jsonl` + `FileCrashLogRepository` |

`ServiceLocator.clear()` 保证测试间无单例泄漏。

## 演进（ADR-020 待决）

若引入 Hilt：

- `@Singleton` 绑定 `CrashLogRepository` / `LegacyAppRepository` / `ManagedAppRepository`
- `@HiltViewModel` 替代 `ViewModelFactory` 手写工厂
- hook 包 **仍不** 启用 Hilt（Xposed 加载路径复杂）

在 ADR-020 accepted 前，**禁止** 混用 Hilt 与 ServiceLocator 双轨。

## 相关文档

- [overview.md](overview.md) — 模块地图与 ServiceLocator 行
- [crash-data-layer.md](crash-data-layer.md) — Repository 读口
- [crash-capture-pipeline.md](crash-capture-pipeline.md) — hook 侧写口隔离
- [architecture-optimization.md](architecture-optimization.md) — §10 架构门禁
- [ADR-003](../decisions/003-xsharedpreferences-cross-process.md) — 跨进程 prefs
- [ADR-011](../decisions/011-feedback-failure-isolation.md) — 失败域隔离
- [dev/plans/architecture-decision-backlog.md](../../dev/plans/architecture-decision-backlog.md)
