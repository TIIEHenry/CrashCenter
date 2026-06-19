---
title: "CrashHandler 设计方案"
type: architecture
status: accepted
phase: N/A
updated: 2026-06-19
summary: "通过 Looper 续命与 UncaughtExceptionHandler 替换拦截崩溃"
---

# CrashHandler 设计方案

> 适用模块：`:app`
> 源码：`nota.android.crash.CrashHandler`
> 相关 ADR：[ADR-001](../decisions/001-looper-loop-resurrection.md)

## 概述

`CrashHandler` 是 CrashCenter 的核心机制。它在目标 app 主线程上重新驱动 `Looper.loop()`，并替换系统默认的 `UncaughtExceptionHandler`，使 Java 层异常不会导致进程退出。

## 架构

```
CrashHandler.insert(handler)
  │
  ├─ [1] Handler(mainLooper).post { while(true) { Looper.loop() } }
  │       └── catch Throwable → handler.handlerException(e)
  │
  └─ [2] setDefaultUncaughtExceptionHandler(custom)
            └── handler.handlerException(e)  // 不调用原 handler
```

## 关键实现

### Looper 续命

```java
while (true) {
    try {
        XposedHelpers.callStaticMethod(Looper.class, "loop");
    } catch (Throwable e) {
        sExceptionHandler.handlerException(e);
    }
}
```

主线程 `Looper.loop()` 正常退出意味着消息队列已空或发生致命错误。无限循环在 catch 中捕获异常后**继续 loop**，相当于给 app「续命」。

### UncaughtExceptionHandler 替换

保存原 handler 到 `sUncaughtExceptionHandler`，但自定义 handler **不转发**给原 handler——确保系统不会触发 crash 流程（杀进程、写 tombstone 等）。

### 单例安装

`sInstalled` 标志保证 `insert()` 只执行一次，避免重复 hook。

## 接口

```java
public interface ExceptionHandler {
    void handlerException(Throwable throwable);
}
```

`XposedEntry` 在 `insert()` 时传入 lambda，负责 UI 反馈（Toast / Notification）。详见 [crash-notification.md](crash-notification.md)。

## 与观测层的关系

`CrashHandler` 本身**不含持久化逻辑**。Phase 4B 起，`XposedEntry` 传入的 `ExceptionHandler` lambda 内部会调用 `CrashCapturePipeline`，将每次捕获的异常并行投递至：

- **CrashLogCoordinator**（异步写入 JSONL — [crash-capture-pipeline.md](crash-capture-pipeline.md)）
- **CrashFeedbackFacade**（Toast + Notification — [crash-notification.md](crash-notification.md)）

两条路径 **失败域隔离**：日志写入失败不影响 `CrashHandler` 续命语义，也不影响 UI 反馈（[ADR-011](../decisions/011-feedback-failure-isolation.md)）。

## 注意事项

- 仅处理 **Java 层**异常；Native crash（SIGSEGV 等）无法拦截
- ANR 不在拦截范围内
- 无限 loop 可能导致 app 处于**不一致状态**（半初始化 UI、损坏的数据结构）
- 使用 `XposedHelpers.callStaticMethod` 而非直接调用，避免 classloader 差异问题

## 相关文档

- [crash-capture-pipeline.md](crash-capture-pipeline.md) — hook 侧 Pipeline（观测 + 反馈）
- [crash-notification.md](crash-notification.md)
- [overview.md](overview.md)
- [xposed-entry.md](xposed-entry.md)
- [ADR-001](../decisions/001-looper-loop-resurrection.md)
- [ADR-011](../decisions/011-feedback-failure-isolation.md) — 失败域隔离
