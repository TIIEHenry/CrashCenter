---
title: "ANR 观测（诊断补充）"
type: architecture
status: accepted
phase: 4
updated: 2026-06-24
summary: "ANR 不入 events.jsonl SSOT；线索来自 logcat system/events 与可选 ApplicationExitInfo；否决 Framework hook（ADR-025）"
---

# ANR 观测（诊断补充）

> 决策：[ADR-025](../decisions/025-anr-observation-no-framework-hook.md)
> 实施任务：[phase4 § 4F-ANR](../../dev/roadmap/active/phase4_crash_observability.md#4f-anranr-观测adr-025)
> 解析规范：[adb-logcat-analysis.md](adb-logcat-analysis.md)（`ANR_HINT`）
> logcat 多源：[logcat-multi-source.md](logcat-multi-source.md)

## 概述

**ANR**（Application Not Responding）由 `system_server` 判定组件/主线程超时，与 Java 未捕获异常（`CrashHandler`）故障模型不同。CrashCenter **不** hook `AnrHelper` / `system_server`；ANR 能力定位为 **诊断补充**，**不入** [crash-logging.md](crash-logging.md) `events.jsonl` 主统计与时间线。

## 采纳路径（与 ADR-025 对齐）

| 路径 | 说明 | 依赖 |
|------|------|------|
| **A — logcat** | Root `logcat -b system` / `-b events` 或 SAF 导入；解析 `ANR in`、`am_anr` | [logcat-multi-source.md](logcat-multi-source.md) buffer P2/P3 |
| **B — ApplicationExitInfo** | API 30+；目标 app 下次 `Application.onCreate` 读 `REASON_ANR` + trace stream | app 级 hook（与 `CrashHandler` 同入口）；**事后** |
| **被动分类** | `RuleEngine` 对 stack 含 `ANR in` 文本做分类 | 非监测；见 [crash-intelligent-analysis.md](crash-intelligent-analysis.md) |

路径命名 **A/B** 与 [logcat-multi-source.md](logcat-multi-source.md) 内 buffer 优先级 **P0–P4** 不同，避免混读。

## INTERCEPT 模式与 ANR

| 场景 | Java crash（`CrashHandler`） | 系统 ANR |
|------|------------------------------|----------|
| INTERCEPT 吞异常后续命 | ✅ 记录 `CrashEvent`；进程通常不杀 | ⚠️ 主线程仍可能卡死 → AMS 仍可能 ANR |
| ANR 后 AMS 行为 | — | 系统 ANR 对话框；用户等待或超时后 **`killLocked("anr")`**（CrashCenter **不**抑制） |
| 写入 `events.jsonl` | ✅（`intercepted=true`） | ❌ 默认不写 |
| 用户可见线索 | Toast/通知（若开启） | logcat 路径 A；或路径 B 下次启动旁路展示（backlog） |
| 与 `CrashEvent` 关联 | — | **不**自动关联；对账靠包名 + 时间窗口（手动） |

干预层 **不**扩展为「吞 ANR」（[ADR-023](../decisions/023-injection-observe-intercept-split.md) 观测/拦截仅针对 Java 异常）。

## ApplicationExitInfo（路径 B，backlog）

| 项 | 约定 |
|----|------|
| 触发 | 目标 app `Application.onCreate`（与 `CrashHandler.install` 同 hook，**之后**读取） |
| API | `ActivityManager.getHistoricalProcessExitReasons(packageName, 0, 5)`，筛选 `REASON_ANR` |
| 限制 | 系统保留条数有限（通常约 5）；进程未杀时无记录 → **INTERCEPT 假活**覆盖弱 |
| 存储 | defer：旁路 `anr_hints.jsonl` 或仅 Logcat/设置页展示；**不入** `events.jsonl` |
| trace | `ApplicationExitInfo.traceInputStream`；null 时仅记 reason/description |

## 未来旁路 schema（defer，编码前须 ADR/架构修订）

```kotlin
// 概念模型；非当前 CrashEvent
data class AnrHintEvent(
    val id: String,
    val timestampMs: Long,
    val packageName: String,
    val processName: String?,
    val pid: Int?,
    val reason: String,           // e.g. "Input dispatching timed out"
    val source: String,           // "logcat_system" | "logcat_events" | "exit_info"
    val traceSnippet: String?,    // 截断
)
```

## As-built（2026-06-24，4F-ANR 部分）

| 项 | 实现 |
|----|------|
| 路径 A — logcat | `RootLogcatReader` + `LogcatBuffer`（MAIN/SYSTEM/CRASH/EVENTS/RADIO）；`LogcatFragment` buffer Chip + root 读取 |
| ANR 过滤 | `LogcatParser.isAnrHint` + `filterCrashRelated`；`LogcatFragment`「仅崩溃」Chip + `PrefManager.PREF_LOGCAT_CRASH_FILTER_DEFAULT`（默认 true） |
| UI 标记 | `LogcatAdapter` 对 ANR 行前缀 `ANR ·` |
| 单测 | `LogcatParserTest` ANR / 进程死亡区分 |
| 验收 L7 | [anr_logcat_l7_template.md](../../dev/verification/anr_logcat_l7_template.md)（真机待填） |
| **defer** | 路径 B `ApplicationExitInfo`；`anr_hints.jsonl`；`LogcatCrashSnippet` 聚合层 |

---

## 相关文档

- [ADR-025](../decisions/025-anr-observation-no-framework-hook.md)
- [framework-injection-feasibility.md](framework-injection-feasibility.md) — 用例 6
- [crash-handler.md](crash-handler.md)
- [glossary.md](../glossary.md) — ANR 观测
