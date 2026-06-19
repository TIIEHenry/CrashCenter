---
title: "崩溃日志 IPC 方案问答归档"
type: iteration
status: accepted
phase: 4
updated: 2026-06-20
summary: "会话中 IPC 稳定性、XSP、公开 FS、framework 注入等问答同步至架构文档"
---

# 崩溃日志 IPC 方案问答归档

## 背景

2026-06-19–20 会话围绕「稳定性分析中心」定位，完成崩溃采集架构、IPC、导航 IA、framework 注入评估。用户追问 IPC 可行性与替代方案，本节归档问答结论（SSOT 已迁入 `docs/architecture/crash-log-ipc.md` § 方案取舍与常见疑问）。

## 问答摘要

| 问题 | 结论 |
|------|------|
| 什么方案满足 IPC 采集？ | ADR-007：**JSONL Primary A + exported CrashLogProvider Fallback B**；异步、与 `System.exit` 隔离 |
| 有没有稳定方案？ | **有分层主备**，无单一 API 全 ROM 零失败；B 为跨 ROM 保底 |
| 公开文件系统写入可以吗？ | **否**（主路径）；目标 UID 常无权写 `/sdcard/`；隐私差；仅 P3 SAF 导出 |
| XSharedPreferences 可以吗？ | **配置开关可读**；**事件体禁止** — 方向相反、无写 API、XML 不适合 append |
| framework 注入更稳吗？ | **否**为主路径；吞崩溃须 app 级；可选 `parseQueries` 仅补包可见性 |
| 独立启动通信权限？ | hook 目标 UID；signature Provider 悖论；IS-1–IS-6 验收矩阵 |
| Tab 几个？ | Phase 3/4B：0；4C+：**2 bottom tab**（配置 \| 观测） |

## 文档落点

- [crash-log-ipc.md](../../../docs/architecture/crash-log-ipc.md) — FAQ 主文档
- [crash-logging.md](../../../docs/architecture/crash-logging.md) — 观测层总览 + FAQ 表
- [ADR-007](../../../docs/decisions/007-crash-log-cross-process-storage.md) — 排除项扩充
- [ADR-003](../../../docs/decisions/003-xsharedpreferences-cross-process.md) — 不适用崩溃事件体
- [framework-injection-feasibility.md](../../../docs/architecture/framework-injection-feasibility.md)
- [navigation-ia.md](../../../docs/architecture/navigation-ia.md)
- [usage.md](../../../docs/guides/usage.md) — LSPosed 作用域 vs Switch

## 相关文档

- [phase4_crash_observability.md](../../roadmap/active/phase4_crash_observability.md)
- [status.md](../../progress/status.md)
