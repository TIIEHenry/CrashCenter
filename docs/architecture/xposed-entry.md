---
title: "Xposed 入口设计方案"
type: architecture
status: accepted
phase: N/A
updated: 2026-06-19
summary: "XposedEntry 薄入口：ScopePolicy 过滤、CrashHandler 安装、委托 CrashCapturePipeline"
---

# Xposed 入口设计方案

> 适用模块：`:app`
> 源码：`nota.android.crash.xp.XposedEntry`
> 相关 ADR：[ADR-003](../decisions/003-xsharedpreferences-cross-process.md)

## 概述

`XposedEntry` 实现 `IXposedHookLoadPackage`，是每个被 hook 进程的入口。负责决定是否处理目标包、安装崩溃拦截器，并将异常回调委托给 `CrashCapturePipeline`（观测 + 反馈，见 [crash-capture-pipeline.md](crash-capture-pipeline.md)）。

## 入口注册

```
app/src/main/assets/xposed_init
  → nota.android.crash.xp.XposedEntry
```

AndroidManifest 声明 `xposedmodule`、`xposedminversion=54` 等元数据。

## 包过滤逻辑

`evaluatePackage()` → `ScopePolicy.evaluate()` 决策树（返回实例级 `ScopeDecision`，非 static 字段）：

```
selfCheck (模块自身) → ScopeDecision(shouldHook=true, showNotify=true)
  ↓
reload XSharedPreferences
  ↓
ScopePolicy.evaluate(xsp, lpparam) → { shouldHook, showNotify, crashLogEnabled }
```

**永远排除的包**：
- `android`
- `de.robv.android.xposed.installer`
- `org.meowcat.edxposed.manager`
- `org.lsposed.manager`

## Hook 安装

在 `Application.onCreate` 的 `afterHookedMethod` 中调用 `CrashHandler.insert()`，异常回调委托 `CrashCapturePipeline.onException()`。**完整链路**见 [crash-capture-pipeline.md](crash-capture-pipeline.md)、[crash-notification.md](crash-notification.md)。

## Self Hook

模块加载自身包时，`selfCheck()` hook `ModuleActivation.isModuleActive()` 恒返回 `true`，使 UI 侧能检测 Xposed 激活状态（不依赖静态 hook）。

## 通知与用户反馈

Toast、系统通知、`ActivityCrashInfo` 详情页的触发条件、线程/进程边界、PendingIntent 与 stack 差异见 **[crash-notification.md](crash-notification.md)**。

## 注意事项

- `showNotify` 由 `ScopeDecision` 闭包捕获，不再使用类级 static 字段（[ADR-010](../decisions/010-scope-policy-show-notify.md)）
- 反馈路径失败仅 `XposedBridge.log`，**禁止** `System.exit`（[ADR-011](../decisions/011-feedback-failure-isolation.md)）

## 相关文档

- [crash-capture-pipeline.md](crash-capture-pipeline.md)
- [crash-notification.md](crash-notification.md)
- [ADR-010](../decisions/010-scope-policy-show-notify.md)
- [ADR-011](../decisions/011-feedback-failure-isolation.md)
- [overview.md](overview.md)
- [crash-handler.md](crash-handler.md)
- [scope-and-prefs.md](scope-and-prefs.md)
- [reference/xposed-framework.md](../reference/xposed-framework.md)
