---
title: "ADR-011: 反馈与日志失败域隔离"
type: decision
status: accepted
phase: 4
updated: 2026-06-19
summary: "CrashFeedbackFacade 与 CrashLogCoordinator 各自独立 try/catch；任一失败不影响另一方，禁止 System.exit"
---

# ADR-011: 反馈与日志失败域隔离

## 状态

**Accepted** — Phase 4B-α 实施。

## 背景

`XposedEntry.hookToGrabCrash`（第 91–106 行）当前将 Toast、Notification 和 XposedBridge.log 包裹在 **同一个 try 块** 中，catch 内调用 `System.exit(0)`：

```java
CrashHandler.insert(throwable -> new Handler(...).post(() -> {
    try {
        XposedBridge.log(throwable);
        if (showNotify) {
            Toast.makeText(...).show();
            showNotification(...);
        }
    } catch (Throwable e) {
        System.exit(0);   // ← 通知异常导致 exit
        XposedBridge.log(e.toString());
    }
}));
```

**问题**：

1. `showNotification` 内 `createPackageContext` 或 `NotificationManager` 异常（OEM 兼容、权限）导致 `System.exit`，**绕过了 CrashHandler 的续命语义**
2. Phase 4B 需在同一回调内新增 `CrashLogCoordinator.logAsync`；若沿用同一 try/catch 结构，日志 IPC 异常也会 exit
3. 反之，若日志写入（ContentResolver/su）抛异常，不应中断 Toast/Notification 的正常展示

## 决策

### 失败域分离

将崩溃回调内的操作拆为 **三个独立 try/catch 域**：

```
onException(ctx, lpparam, throwable, decision)
│
├── [Domain A] try { CrashLogCoordinator.logAsync(event) } catch { log only }
│
├── [Domain B] try { CrashFeedbackFacade.show(ctx, event, decision) } catch { log only }
│
└── [Domain C] 外层兜底 try/catch — 保证 CrashHandler Looper 继续 loop
```

### 核心规则

| 规则 | 说明 |
|------|------|
| **禁止 `System.exit` 因通知/日志失败** | exit 仅保留给不可恢复的 JVM 级问题（如 OOM 后状态损坏），且 4B refactor 默认移除 |
| **观测与反馈互不牵连** | Domain A 异常不传播到 Domain B，反之亦然 |
| **Pipeline 兜底** | 即使 A+B 均异常，CrashHandler `while(true) { Looper.loop() }` 不受影响 |
| **catch 内仅日志** | `XposedBridge.log(e)` + 可选 metrics counter；不 rethrow、不 exit |

### 执行顺序

观测路径（Coordinator）为异步提交到单线程 executor，不阻塞反馈路径：

1. `buildEvent` — 同步，微秒级
2. `Coordinator.logAsync(event)` — 提交异步任务后立即返回
3. `Facade.show(...)` — Handler.post 到主线程 Toast/Notification

反馈同步 post 与日志异步提交 **无执行依赖**；任一 catch 不影响另一已提交的操作。

## 后果

| 方面 | 影响 |
|------|------|
| **稳定性** | 通知兼容问题不再导致目标 app 意外退出 |
| **可观测性** | 日志路径失败时仍有 Toast/Notification 告知用户崩溃发生 |
| **代码结构** | 消除 140+ 行混合 lambda；各域可独立单测 |
| **向后兼容** | hook 语义不变（仍吞异常续命）；用户可见行为：通知失败不再 kill app |

## 风险

| 风险 | 缓解 |
|------|------|
| 反馈与日志均 silent 失败 → 用户无感知 | XposedBridge.log 保留；logcat 可审计 |
| 移除 exit 后 app 状态更不可预测 | 这是 CrashHandler 本身的设计哲学——"吞掉异常，不修复" |

## 相关文档

- [crash-capture-pipeline.md](../architecture/crash-capture-pipeline.md) — Pipeline 实现
- [crash-handler.md](../architecture/crash-handler.md) — 续命语义
- [crash-log-backends.md](../architecture/crash-log-backends.md) — 后端写入与 silent 约束
- [ADR-010](010-scope-policy-show-notify.md) — ScopePolicy 解耦
- [architecture-optimization.md](../architecture/architecture-optimization.md) — §5.1 / §6.3 IPC 失败域
