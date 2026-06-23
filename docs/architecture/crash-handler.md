---
title: "CrashHandler 设计方案"
type: architecture
status: accepted
phase: N/A
updated: 2026-06-23
summary: "INTERCEPT 续命 + OBSERVE 转发；由 ScopeDecision.shouldIntercept 选择模式"
---

# CrashHandler 设计方案

> 适用模块：`:app`
> 源码：`nota.android.crash.CrashHandler`
> 相关 ADR：[ADR-001](../decisions/001-looper-loop-resurrection.md)、[ADR-023](../decisions/023-injection-observe-intercept-split.md)

## 概述

`CrashHandler` 在目标 app 进程安装崩溃捕获。`XposedEntry` 按 `ScopeDecision.shouldIntercept` 选择模式：

| 模式 | 行为 |
|------|------|
| **INTERCEPT** | 无限 `Looper.loop()` 续命；UEH **不转发**；进程存活 |
| **OBSERVE** | Looper 单次 catch 后 **rethrow**；UEH 记录后 **转发** saved default；进程可退出 |

## 架构（INTERCEPT）

```
CrashHandler.install(INTERCEPT, handler)
  ├─ Handler(mainLooper).post { loop → catch → handler → loop }
  └─ setDefaultUncaughtExceptionHandler → handler（不转发）
```

## 架构（OBSERVE）

```
CrashHandler.install(OBSERVE, handler)
  ├─ Handler(mainLooper).post { loop → catch → handler → rethrow }
  └─ setDefaultUncaughtExceptionHandler → handler → previous.uncaughtException
```

## 接口

```kotlin
enum class Mode { INTERCEPT, OBSERVE }

fun interface ExceptionHandler {
    fun handleException(throwable: Throwable, source: String)
}

fun install(mode: Mode, handler: ExceptionHandler)
```

`XposedEntry` 传入 lambda → `CrashCapturePipeline.onException()`。详见 [crash-capture-pipeline.md](crash-capture-pipeline.md)。

## 与观测层的关系

- **INTERCEPT**：`CrashLogCoordinator.logAsync`
- **OBSERVE**：`CrashLogCoordinator.logSync`（relay 优先，短超时，进程退出前完成）

反馈默认仅在拦截模式且 `showNotify=true` 时触发（[crash-notification.md](crash-notification.md)）。

## 注意事项

- 仅 **Java 层**异常；Native crash、ANR 不在 scope
- INTERCEPT 无限 loop 可能导致 app 处于不一致状态
- 使用 `XposedHelpers.callStaticMethod` 调用 `Looper.loop`

## 相关文档

- [injection-observe-intercept-split.md](injection-observe-intercept-split.md)
- [crash-capture-pipeline.md](crash-capture-pipeline.md)
- [xposed-entry.md](xposed-entry.md)
- [ADR-001](../decisions/001-looper-loop-resurrection.md)
- [ADR-011](../decisions/011-feedback-failure-isolation.md)
