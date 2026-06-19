---
title: "Xposed 入口设计方案"
type: architecture
status: accepted
phase: N/A
updated: 2026-06-19
summary: "XposedEntry 的包过滤、hook 安装与通知展示"
---

# Xposed 入口设计方案

> 适用模块：`:app`
> 源码：`nota.android.crash.xp.XposedEntry`
> 相关 ADR：[ADR-003](../decisions/003-xsharedpreferences-cross-process.md)

## 概述

`XposedEntry` 实现 `IXposedHookLoadPackage`，是每个被 hook 进程的入口。负责决定是否处理目标包、安装崩溃拦截器、展示用户反馈。

## 入口注册

```
app/src/main/assets/xposed_init
  → nota.android.crash.xp.XposedEntry
```

AndroidManifest 声明 `xposedmodule`、`xposedminversion=54` 等元数据。

## 包过滤逻辑

`shouldHandlePackage()` 决策树：

```
selfCheck (模块自身) → 始终处理
  ↓
reload XSharedPreferences
  ↓
scope_mode == true?
  ├─ YES: hook 非系统包（或 handle_system 允许的系统包）
  │        排除: android、Xposed 管理器、禁用列表中的包
  │        showNotify = true
  └─ NO:  hook 全部包
           showNotify = !disabled
```

**永远排除的包**：
- `android`
- `de.robv.android.xposed.installer`
- `org.meowcat.edxposed.manager`
- `org.lsposed.manager`

## Hook 安装

在 `Application.onCreate` 的 `afterHookedMethod` 中调用 `CrashHandler.insert()`，传入异常回调（主线程 Toast + 通知）。**完整通知链路**见 [crash-notification.md](crash-notification.md)。

## Self Hook

模块加载自身包时，`selfCheck()` hook `ActivityMain.isModuleActived()` 恒返回 `true`，使 UI 侧能检测 Xposed 激活状态（不依赖静态 hook）。

## 通知与用户反馈

Toast、系统通知、`ActivityCrashInfo` 详情页的触发条件、线程/进程边界、PendingIntent 与 stack 差异见 **[crash-notification.md](crash-notification.md)**。

## 注意事项

- `showNotify` 是静态变量，多包并发加载时可能被后加载的包覆盖
- 通知展示失败时 `System.exit(0)` 作为最后手段（见 `hookToGrabCrash` catch 块）
- 大量注释掉的旧 stack trace 格式化代码保留在源码中，当前使用 `Log.getStackTraceString`

## 相关文档

- [crash-notification.md](crash-notification.md)
- [overview.md](overview.md)
- [crash-handler.md](crash-handler.md)
- [scope-and-prefs.md](scope-and-prefs.md)
- [reference/xposed-framework.md](../reference/xposed-framework.md)
