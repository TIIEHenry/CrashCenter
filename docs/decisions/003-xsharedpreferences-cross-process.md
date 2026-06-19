---
title: "ADR-003: XSharedPreferences 跨进程配置"
type: decision
status: accepted
phase: N/A
updated: 2026-06-20
summary: "hook 侧只读 UI 写入的 scope 配置；不适用崩溃事件体持久化"
---

# ADR-003: XSharedPreferences 跨进程配置

## 背景

Xposed hook 运行在**目标 app 进程**中，配置 UI（ActivityMain）运行在**模块自身进程**中。hook 侧需要读取用户在 UI 中设置的 scope 偏好。

## 决策

使用 Xposed 提供的 `XSharedPreferences`：

```java
new XSharedPreferences("nota.android.crash.xp.app", "crash");
```

每次 `handleLoadPackage` 时调用 `reload()` 获取最新值。

UI 侧使用标准 `SharedPreferences` 写入相同文件名和 key。

## 关键要点

1. `PACKAGE_NAME` 必须与 applicationId 一致
2. `PREF_NAME = "crash"` 两侧共用
3. 每次包加载 reload，非实时推送
4. 需要 Xposed 框架正确配置 prefs 可读权限（LSPosed 默认支持）

## 后果

- **正面**：简单可靠，无需 IPC 服务或 ContentProvider
- **负面**：配置变更有延迟；reload 有 IO 开销（每个包加载时）
- **跟进**：文档说明修改 scope 后需重启目标 app

## 不适用场景（崩溃日志）

本 ADR **仅**覆盖小体积配置（boolean、StringSet 等）。**禁止**用 XSharedPreferences 或 hook 直改 `shared_prefs` 存储：

- 崩溃 stack trace、历史列表、统计聚合
- Phase 4 `CrashEvent` 事件体

原因：数据方向相反（hook 写 → UI 读）、无公开写 API、XML 不适合 append、与 UI `commit()` 并发易损坏。崩溃观测走 [ADR-007](007-crash-log-cross-process-storage.md)（JSONL + Provider）。`crash_log_enabled` 等开关仍可读 prefs。

详见 [crash-log-ipc.md § 为何不用 XSharedPreferences](../architecture/crash-log-ipc.md#为何不用-xsharedpreferences-存崩溃日志)。

## 备选方案

- **ContentProvider** → 过度设计，需额外组件 → 不选
- **文件直读** → 绕过 Xposed API，兼容性差 → 不选
- **静态 manifest scope** → 无法 per-app 动态配置 → 不选

## 相关

- 方案文档：[scope-and-prefs.md](../architecture/scope-and-prefs.md)
- [xposed-entry.md](../architecture/xposed-entry.md)
