---
title: "ADR-001: Looper.loop 无限循环续命"
type: decision
status: accepted
phase: N/A
updated: 2026-06-19
summary: "主线程 crash 后通过无限 Looper.loop 恢复事件循环，使 app 继续运行"
---

# ADR-001: Looper.loop 无限循环续命

## 背景

Android 主线程通过 `Looper.loop()` 驱动消息队列。当未捕获异常发生时，`Looper.loop()` 抛出异常并退出，导致主线程终止、app 崩溃。

CrashCenter 的目标是让 app **不因 Java 异常而退出**，需要一种机制在主线程 crash 后恢复事件循环。

## 决策

在 `CrashHandler.insert()` 中，于主线程 post 一个无限循环：

```java
while (true) {
    try { Looper.loop(); }
    catch (Throwable e) { handler.handlerException(e); }
}
```

同时替换 `UncaughtExceptionHandler`，不转发给系统默认 handler。

## 关键要点

1. catch 中调用用户 handler 展示反馈，然后继续 loop
2. 不调用原 `UncaughtExceptionHandler`，避免系统杀进程
3. `insert()` 单例安装，防止重复 hook

## 后果

- **正面**：有效阻止 Java 层 crash 导致的进程退出
- **负面**：app 可能处于不一致状态（半初始化 UI、损坏数据结构）；用户无感知地继续使用可能有风险的 app
- **跟进**：文档和 UI 中明确说明「吞异常非修复」

## 备选方案

- **仅替换 UncaughtExceptionHandler** → 主线程 loop 已退出，无法恢复 UI 事件 → 不选
- **重启 Activity** → 需要额外逻辑判断重启时机，复杂且可能丢状态 → 不选
- **Process.killProcess 后保活** → 等同于 crash → 不选

## 相关

- 方案文档：[crash-handler.md](../architecture/crash-handler.md)
- 总览：[overview.md](../architecture/overview.md)
