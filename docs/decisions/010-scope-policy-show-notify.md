---
title: "ADR-010: ScopePolicy 消除 static showNotify"
type: decision
status: accepted
phase: 4
updated: 2026-06-19
summary: "将 XposedEntry.showNotify 静态字段替换为 ScopePolicy 实例级 ScopeDecision，消除多包并发竞态"
---

# ADR-010: ScopePolicy 消除 static showNotify

## 状态

**Accepted** — Phase 4B 前实施（小 refactor，不改 hook 语义）。

## 背景

`XposedEntry` 当前使用一个 **类级 static** 字段 `showNotify`（第 39 行）来决定崩溃发生时是否显示 Toast / Notification。该字段在 `shouldHandlePackage()` 中根据 scope 规则赋值，但因 Xposed 对每个目标 app 进程分别调用 `handleLoadPackage`，**不同进程的 hook 代码拥有各自独立的 static 拷贝**——因此实际并发竞态不会跨进程。

然而在同一进程内，`handleLoadPackage` 可能被多次调用（如 SharedUserId 场景或 hook 自身进程后继续 hook 其他组件），此时后续调用**覆盖**先前赋值。更重要的是，`showNotify` 与 `shouldHandlePackage` 的过滤逻辑混合在同一方法中，使得通知决策与 hook 决策**语义耦合**：

```java
if (scopeMode) {
    showNotify = true;  // 副作用
    return (!isSystemApp || handleSystemApp) && !(ignore || isXposedInstaller || disabled);
} else {
    showNotify = !disabled;  // 副作用
    return true;
}
```

Phase 4B 引入 `CrashCapturePipeline` 后，`showNotify` 的值需传递到独立的 `CrashFeedbackFacade`。static 字段作为隐式通道不利于测试和推理。

## 决策

引入 `ScopePolicy` 纯函数/对象，返回 **实例级** `ScopeDecision`：

```kotlin
data class ScopeDecision(
    val shouldHook: Boolean,
    val showNotify: Boolean
)

object ScopePolicy {
    fun evaluate(xsp: XSharedPreferences, lpparam: LoadPackageParam): ScopeDecision
}
```

### 规则映射（与现有行为等价）

| scope_mode | 条件 | shouldHook | showNotify |
|------------|------|------------|------------|
| `true` | 非系统（或允许系统）且非禁用、非忽略 | `true` | `true` |
| `true` | 系统且不允许、或禁用/忽略包 | `false` | — |
| `false` | 任意包 | `true` | `!disabled` |
| — | self hook（模块自身） | `true` | `true` |

### 调用方式

```kotlin
// handleLoadPackage 内
val decision = ScopePolicy.evaluate(xsp, lpparam)
if (!decision.shouldHook) return

// hookToGrabCrash 内 — 闭包捕获 decision
CrashCapturePipeline.onException(ctx, lpparam, throwable, decision)
```

### 删除

- 删除 `XposedEntry.showNotify` static 字段
- `shouldHandlePackage` 方法替换为 `ScopePolicy.evaluate` 调用

## 后果

| 方面 | 影响 |
|------|------|
| 消除竞态 | `ScopeDecision` 为 val，闭包捕获后不可变 |
| 可测试 | `ScopePolicy.evaluate` 为纯函数，输入 XSP mock 即可单测 |
| Pipeline 解耦 | `CrashFeedbackFacade` 不再读 static 字段，由 Pipeline 传参 |
| 源码改动范围 | 仅 `XposedEntry`；hook 语义不变 |

## 相关文档

- [crash-capture-pipeline.md](../architecture/crash-capture-pipeline.md) — Pipeline 如何使用 ScopeDecision
- [scope-and-prefs.md](../architecture/scope-and-prefs.md) — prefs 模型
- [architecture-optimization.md](../architecture/architecture-optimization.md) — §5.1 拆分 XposedEntry
- [ADR-011](011-feedback-failure-isolation.md) — 反馈失败域隔离
- [glossary.md](../glossary.md) — ScopePolicy 术语
