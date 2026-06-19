---
title: "CrashCenter 系统总览"
type: architecture
status: accepted
phase: N/A
updated: 2026-06-19
summary: "Xposed 异常拦截模块的整体架构与数据流；Phase 4 多后端观测层；演进见 architecture-optimization.md"
---

# CrashCenter 系统总览

> 适用模块：`:app`
> 源码包：`nota.android.crash` / `nota.android.crash.xp` / `nota.android.crash.xp.app`
> 显示名：CrashCenter（英文）/ 崩溃中心（中文）

## 概述

CrashCenter（崩溃中心）是一个 **Xposed 模块**，在目标 Android 应用的 Java 层拦截未捕获异常，使进程不因崩溃而退出。模块**不修复错误**，而是**吞掉异常**并可选展示 Toast / 系统通知。

## 架构

```
Xposed Framework
  └── XposedEntry (IXposedHookLoadPackage)
        ├── shouldHandlePackage()     ← XSharedPreferences 读取 scope
        ├── selfCheck()               ← hook 自身 isModuleActived()
        └── hook Application.onCreate
              └── CrashHandler.insert()
                    ├── 无限 Looper.loop() 续命
                    └── 替换 UncaughtExceptionHandler
                          ├── Toast + Notification → ActivityCrashInfo
                          └── CrashLogCoordinator (Phase 4) → 多 Backend → JSONL / relay / Provider
```

## 产品演进：稳定性分析中心

| 层级 | 职责 | 状态 |
|------|------|------|
| **干预层** | 吞异常、进程续命 | 已实现 |
| **观测层** | 记录每次被拦截崩溃 | Phase 4 backlog — [crash-logging.md](crash-logging.md) |
| **分析层** | 分类、聚类、诊断建议（不修复 bug） | Phase 4G backlog — [crash-intelligent-analysis.md](crash-intelligent-analysis.md) |
| **观测 UI** | 历史列表、统计、导出 | Phase 4C+ — [navigation-ia.md](navigation-ia.md) |

跨进程写入方案见 [crash-log-backends.md](crash-log-backends.md)、[crash-log-ipc.md](crash-log-ipc.md)。

**架构演进 / 优化**（prescriptive）：[architecture-optimization.md](architecture-optimization.md) — 现状债务、目标分层、包结构、Phase 4 落地映射。

## 模块地图

| 组件 | 源码 | 职责 |
|------|------|------|
| Xposed 入口 | `XposedEntry.java` | 包过滤、hook 安装、通知展示 |
| 崩溃拦截 | `CrashHandler.java` | Looper 续命 + 异常处理器替换 |
| 配置 UI | `ActivityMain.kt` | 应用列表、scope 开关、搜索排序 |
| 崩溃详情 | `ActivityCrashInfo.java` | 通知点击后展示 stack trace |
| 偏好常量 | `PrefManager.java` | SharedPreferences key 定义 |
| RecyclerView 辅助 | `recyclerhelper/` | 通用 Adapter / ViewHolder |

## 跨进程配置同步

```
ActivityMain (UI 进程)
  └── SharedPreferences "crash"
        ├── scope_mode
        ├── handle_system
        └── package_list (Set<String>, 禁用列表)

XposedEntry (目标 app 进程)
  └── XSharedPreferences → reload() → 同上 keys
```

## 关键设计决策

| 决策 | 原因 | 详见 |
|------|------|------|
| Looper.loop 无限循环 | 主线程 crash 后恢复事件循环 | [ADR-001](../decisions/001-looper-loop-resurrection.md) |
| 禁用列表（反向 toggle） | 默认全选 hook，关闭 = 排除 | [ADR-002](../decisions/002-inverted-package-toggle.md) |
| XSharedPreferences | hook 侧读取 UI 配置 | [ADR-003](../decisions/003-xsharedpreferences-cross-process.md) |
| JSONL + 多后端 | 崩溃事件 hook→模块；模块 root ingest | [ADR-007](../decisions/007-crash-log-cross-process-storage.md)、[ADR-008](../decisions/008-multi-backend-crash-log-storage.md) |

## 相关文档

- [architecture-optimization.md](architecture-optimization.md) — 架构优化与演进路线图
- [code-editor-porting.md](code-editor-porting.md) — celestailruler CodeEditor 移植（日志浏览）
- [crash-intelligent-analysis.md](crash-intelligent-analysis.md) — 规则分类、聚类与诊断建议（4G）
- [adb-logcat-analysis.md](adb-logcat-analysis.md) — PC adb / 本机 logcat 崩溃分析（补充 JSONL）
- [crash-stats-ui.md](crash-stats-ui.md) — 全局统计与单应用观测页需求
- [crash-notification.md](crash-notification.md) — Toast / 通知 / 详情页流程
- [crash-logging.md](crash-logging.md) — 观测层方案
- [crash-log-backends.md](crash-log-backends.md) — 多后端编排与 ingest
- [crash-log-ipc.md](crash-log-ipc.md) — 跨进程 IPC 与 FAQ
- [navigation-ia.md](navigation-ia.md) — Phase 4+ 双 Tab 导航
- [ui-routing.md](ui-routing.md) — 界面路由与外部 Intent
- [xposed-entry.md](xposed-entry.md) — Xposed 入口与 hook 逻辑
- [crash-handler.md](crash-handler.md) — 崩溃拦截机制
- [scope-and-prefs.md](scope-and-prefs.md) — 作用域与偏好模型
- [configuration-ui.md](configuration-ui.md) — 配置界面
- [guides/usage.md](../guides/usage.md) — 用户使用指南
