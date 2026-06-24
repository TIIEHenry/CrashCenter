---
title: "CrashCenter 系统总览"
type: architecture
status: accepted
phase: N/A
updated: 2026-06-21
summary: "Xposed 异常拦截模块的整体架构与数据流；4B-α 观测 + 4C-α UI Shell as-built；演进见 architecture-optimization.md"
---

# CrashCenter 系统总览

> 适用模块：`:app`
> 源码包：`nota.android.crash` / `nota.android.crash.xp` / `nota.android.crash.xp.app`
> 显示名：CrashCenter（英文）/ 稳定性中心（中文）

## 概述

CrashCenter（稳定性中心）是一个 **Xposed 模块**，在目标 Android 应用的 Java 层拦截未捕获异常，使进程不因崩溃而退出。模块**不修复错误**，而是**吞掉异常**并可选展示 Toast / 系统通知。

## 架构

```
Xposed Framework
  └── XposedEntry (IXposedHookLoadPackage)
        ├── ScopePolicy.evaluate()        ← XSharedPreferences 读取 scope
        ├── selfCheck()                   ← hook 自身 isModuleActived()
        └── hook Application.onCreate
              └── CrashHandler.insert()
                    ├── 无限 Looper.loop() 续命
                    └── UncaughtExceptionHandler
                          └── CrashCapturePipeline.onException
                                ├── CrashLogCoordinator (4B-α Phase 2) → JSONL / relay / Provider
                                └── CrashFeedbackFacade → Toast / Notification → ActivityCrashInfo
```

## 产品演进：稳定性分析中心

| 层级 | 职责 | 状态 |
|------|------|------|
| **干预层** | 吞异常、进程续命 | 已实现 |
| **观测层** | 记录每次被拦截崩溃 | **4B-α 部分 MVP** — hook 侧 Phase 2 持久化；ingest / 统计 UI defer — [crash-logging.md](crash-logging.md) |
| **分析层** | 分类、聚类、诊断建议（不修复 bug） | Phase 4G backlog — [crash-intelligent-analysis.md](crash-intelligent-analysis.md) |
| **观测 UI** | 历史列表、统计、导出 | **4C-α as-built** — MainShell 2-tab + Paging 历史；统计 defer 4D — [navigation-ia.md](navigation-ia.md) |

跨进程写入方案见 [crash-log-backends.md](crash-log-backends.md)、[crash-log-ipc.md](crash-log-ipc.md)。

**架构演进 / 优化**（prescriptive）：[architecture-optimization.md](architecture-optimization.md) — 现状债务、目标分层、包结构、Phase 4 落地映射。

## 技术栈

| 项 | 值 |
|---|---|
| compileSdk / targetSdk | **37** |
| minSdk | 21 |
| Kotlin | 2.3 |
| Java | 17 |
| Xposed API | 82 |
| Gradle / AGP | 9.2.1 / 9.0.0 |
| 模块 DI | `ServiceLocator`（手动单例；Hilt defer — 见 D-05） |
| 历史列表 | **Paging3** + `CrashEventPagingSource` + `FileCrashLogRepository` |

## As-built（2026-06-20）

hook 侧观测层 MVP 与模块 UI Shell 已落地：

| 组件 | 包路径 | 职责 |
|------|--------|------|
| `CrashCapturePipeline` | `nota.android.crash.capture` | 单入口：`logAsync` + `CrashFeedbackFacade`；失败域隔离 |
| `CrashLogCoordinator` | `nota.android.crash.log` | 单线程 executor → Phase 2 并行三后端（≤2s） |
| `CrashLogBackendRegistry` | `nota.android.crash.log` | `ProviderBackend` / `DirectFsBackend` / `TargetRelayBackend` |
| `CanonicalJsonlWriter` | `nota.android.crash.log` | canonical `events.jsonl` append + 500 条 / 8 MB retention |
| `CrashLogProvider` | `nota.android.crash.xp.app` | exported Provider IPC（无 signature permission） |
| `FileCrashLogRepository` | `nota.android.crash.xp.app.data` | 读 canonical：getAll/getById/getCount、LRU 200 |
| `ServiceLocator` | `nota.android.crash.xp.app.di` | `LegacyAppRepository` + `ManagedAppRepository` + `CrashLogRepository` 单例 |
| `MainShellActivity` | `nota.android.crash.xp.app.shell` | Launcher；底栏 配置 \| 观测 |
| `ConfigFragment` | `nota.android.crash.xp.app.config` | 受管应用列表、scope、干预入口 |
| `ObserveHostFragment` | `nota.android.crash.xp.app.observe` | 观测 tab；嵌入 `CrashHistoryFragment`（Paging3） |
| `CrashEventPagingSource` | `nota.android.crash.xp.app.observe` | Paging3 分页读 Repository |

**defer 4B-β**：`RootSuBackend` Phase 1、`CrashLogIngestCoordinator` relay harvest、dedupe / `ingestedFrom`。

**defer 4D**：`StatsAggregator`、观测统计 tab、Repository `clear` / `observeChanges`。

详见 [crash-logging.md § As-built](crash-logging.md#as-built4b-α2026-06-19)、[ui-routing.md § As-built](ui-routing.md#as-built2026-06)、[crash-data-layer.md](crash-data-layer.md)。

## 模块地图

| 组件 | 源码 | 职责 |
|------|------|------|
| Xposed 入口 | `XposedEntry.kt` | 包过滤、hook 安装、委托 `CrashCapturePipeline` |
| 崩溃拦截 | `CrashHandler.kt` | Looper 续命 + 异常处理器替换 |
| 崩溃采集管道 | `capture/CrashCapturePipeline.kt` | hook 单入口：观测 + 反馈并行、失败域隔离 |
| 日志协调 | `log/CrashLogCoordinator.kt` | hook 侧 Phase 2 多后端并行写入 |
| 日志 Provider | `xp/app/CrashLogProvider.kt` | exported ContentProvider IPC append |
| 日志读路径 | `xp/app/data/FileCrashLogRepository.kt` | canonical JSONL 读；LRU 200；Paging3 消费方 |
| UI 壳层 | `xp/app/shell/MainShellActivity.kt` | Launcher；BottomNav 配置 \| 观测 |
| 配置 UI | `xp/app/config/ConfigFragment.kt` | 应用列表、scope 开关、搜索排序、FilterChip |
| 观测 UI | `xp/app/observe/` | `ObserveHostFragment`、`CrashHistoryFragment`、Paging |
| 模块 DI | `xp/app/di/ServiceLocator.kt` | Repository 单例（until Hilt） |
| 崩溃详情 | `ActivityCrashInfo.java` | 通知点击后展示 stack trace |
| 偏好常量 | `PrefManager.kt` | `managed_packages`、`handle_system`、`show_system_ui`、crash log 键 |
| 包可见性 | `PackageVisibilityHelper.kt` | Android 11+ `QUERY_ALL_PACKAGES` 检测与手动授权引导 |
| Xposed 管理器跳转 | `XposedManagerLauncher.kt` | 状态条点击 → 多框架回退打开 LSPosed / EdXposed |
| 通用列表 | `common/ui/adapter/BaseListAdapter.kt` | 通用 Adapter / ViewHolder |

## 跨进程配置同步

```
MainShellActivity / ConfigFragment (UI 进程)
  └── SharedPreferences "crash"
        ├── managed_packages (Set<String>, 已拦截包)
        ├── handle_system / show_system_ui
        └── crash_log_enabled / crash_log_backend_* (观测开关，4B-α)

XposedEntry (目标 app 进程)
  └── XSharedPreferences → reload() → 同上 keys（hook 只读）

CrashLogCoordinator (目标 app 进程)
  └── Phase 2 backends → events.jsonl / relay / Provider IPC
        └── 与 scope 无关；crash_log_enabled == false 时短路
```

## 关键设计决策

| 决策 | 原因 | 详见 |
|------|------|------|
| Looper.loop 无限循环 | 主线程 crash 后恢复事件循环 | [ADR-001](../decisions/001-looper-loop-resurrection.md) |
| 禁用列表（反向 toggle） | 默认全选 hook，关闭 = 排除 | [ADR-002](../decisions/002-inverted-package-toggle.md) |
| XSharedPreferences | hook 侧读取 UI 配置 | [ADR-003](../decisions/003-xsharedpreferences-cross-process.md) |
| JSONL + 多后端 | 崩溃事件 hook→模块；模块 root ingest | [ADR-007](../decisions/007-crash-log-cross-process-storage.md)、[ADR-008](../decisions/008-multi-backend-crash-log-storage.md) |
| 4B-β dedupe / ingest | canonical 按 id 合并；ingest SSOT | [ADR-017](../decisions/017-root-ingest-and-dedupe.md)（proposed） |

## 相关文档

- [architecture-optimization.md](architecture-optimization.md) — 架构优化与演进路线图
- [code-editor-porting.md](code-editor-porting.md) — celestailruler CodeEditor 移植（日志浏览）
- [crash-intelligent-analysis.md](crash-intelligent-analysis.md) — 规则分类、聚类与诊断建议（4G）
- [adb-logcat-analysis.md](adb-logcat-analysis.md) — PC adb / 本机 logcat 崩溃分析（补充 JSONL）
- [crash-stats-ui.md](crash-stats-ui.md) — 全局统计与单应用观测页需求
- [crash-event-timeline-ui.md](crash-event-timeline-ui.md) — CrashHistoryFragment 时间线呈现
- [crash-notification.md](crash-notification.md) — Toast / 通知 / 详情页流程
- [crash-logging.md](crash-logging.md) — 观测层方案
- [crash-log-backends.md](crash-log-backends.md) — 多后端编排与 ingest
- [crash-log-ipc.md](crash-log-ipc.md) — 跨进程 IPC 与 FAQ
- [navigation-ia.md](navigation-ia.md) — Phase 4+ 双 Tab 导航
- [crash-data-layer.md](crash-data-layer.md) — Repository as-built 与 4D 读口目标
- [app-di-and-module-boundaries.md](app-di-and-module-boundaries.md) — ServiceLocator 与 hook/UI 包门禁
- [ui-routing.md](ui-routing.md) — 界面路由与外部 Intent
- [xposed-entry.md](xposed-entry.md) — Xposed 入口与 hook 逻辑
- [crash-handler.md](crash-handler.md) — 崩溃拦截机制
- [scope-and-prefs.md](scope-and-prefs.md) — 作用域与偏好模型
- [configuration-ui.md](configuration-ui.md) — 配置界面
- [guides/usage.md](../guides/usage.md) — 用户使用指南
