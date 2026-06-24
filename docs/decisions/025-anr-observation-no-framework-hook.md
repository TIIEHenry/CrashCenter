---
title: "ADR-025: ANR 观测不走 Framework hook"
type: decision
status: accepted
phase: 4
updated: 2026-06-24
summary: "否决 system_server / AnrHelper AMS hook；ANR 线索走 logcat 多源（P2/P3）与可选 ApplicationExitInfo，不进入 Framework 注入主路径"
---

# ADR-025: ANR 观测不走 Framework hook

## 状态

**Accepted** — 2026-06-24 架构评审结论。

## 背景

CrashCenter 核心 scope 为 **Java 层未捕获异常**的拦截与观测（[crash-handler.md](../architecture/crash-handler.md)、[crash-logging.md](../architecture/crash-logging.md)）。**ANR**（Application Not Responding）由 `system_server` 判定主线程/组件超时，与 Java crash 故障模型不同。

曾评估在 `system_server` hook `AnrHelper.appNotResponding` / `ProcessErrorStateRecord.appNotResponding` 等路径，实现实时 ANR 观测或抑制弹窗。详见会话分析与 [framework-injection-feasibility.md](../architecture/framework-injection-feasibility.md) 补充说明。

[framework-injection-feasibility.md](../architecture/framework-injection-feasibility.md) 已否决 **AMS Java crash hook**（用例 1）；本 ADR 将 **AMS ANR hook** 单独定论，避免与「Framework 永不 hook」表述混淆。

## 决策

### 1. 不实施 Framework / AMS ANR hook

**否决**以下长期方案：

| 方案 | 说明 |
|------|------|
| Hook `AnrHelper` / `ProcessErrorStateRecord` | 在 `packageName == "android"`（system_server）观测 ANR |
| Hook `addErrorToDropBox("anr", ...)` | 从 DropBox 路径旁路采集 |
| Framework 级 **抑制** ANR（不弹窗、不 `killLocked("anr")`） | 产品与安全风险过高；与「吞 Java 异常」不同，易造成僵尸 UI |

**不**要求用户为 CrashCenter 勾选 LSPosed **System Framework** 以获取 ANR 能力。

### 2. ANR 观测的采纳路径

> 下列 **路径 A/B** 与 [logcat-multi-source.md](../architecture/logcat-multi-source.md) 内 buffer 实施优先级 **P0–P4** 不同，避免与「P2 = system buffer」混读。

| 路径 | 内容 | 说明 |
|------|------|------|
| **路径 A** | [logcat-multi-source.md](../architecture/logcat-multi-source.md) buffer **P2/P3** | Root `logcat -b system`（`ANR in` / `ActivityManager`）与 `-b events`（`am_anr`）；文件导入降级 |
| **路径 B** | 目标 app 内 `ApplicationExitInfo`（API 30+） | 下次启动读上次 `REASON_ANR` 与 trace；与 app 级 hook 同进程，**无需** system_server |
| **已有** | `RuleEngine` + `rules_v1.json` | stack 含 `ANR in` 时的**被动**分类，**非** ANR 监测（见 [crash-intelligent-analysis.md](../architecture/crash-intelligent-analysis.md)） |

实施规格见 [anr-observation.md](../architecture/anr-observation.md)。

### 3. 数据模型与 SSOT

- `events.jsonl` / `CrashEvent` 仍为 **Java crash** SSOT；ANR **默认不入**主统计与时间线（[crash-event-timeline-ui.md](../architecture/crash-event-timeline-ui.md)）。
- 若未来持久化 ANR，须独立事件类型或旁路文件（概念模型见 [anr-observation.md](../architecture/anr-observation.md) §未来旁路 schema），**不得**在未扩展 schema 前混入 `CrashEvent` 默认语义。
- logcat ANR 线索定位为 **诊断补充**，与 [adb-logcat-analysis.md](../architecture/adb-logcat-analysis.md) 一致。

### 4. 与干预层的关系

- **INTERCEPT** 续命后，主线程仍可能因卡死/死锁触发系统 ANR；Framework hook 对此有观测价值，但维护成本与 blast radius 不可接受。
- 干预层 **不**扩展为「吞 ANR」；ANR 仅作可选观测，不改变 AMS 杀进程与弹窗语义。

| 场景 | `CrashEvent` / JSONL | 系统 ANR 对话框 | 进程结局 |
|------|----------------------|-----------------|----------|
| INTERCEPT + Java 异常 | ✅ 写入 | 通常不出现（未走 Java FATAL） | 续命存活 |
| INTERCEPT + 主线程卡死（无异常） | ❌ | ✅ 可出现 | AMS 可 `killLocked("anr")` |
| OBSERVE + ANR | ❌（非 Java crash） | ✅ | 通常被杀 |

详见 [anr-observation.md](../architecture/anr-observation.md) §INTERCEPT 模式与 ANR。

## 备选方案

| 方案 | 描述 | 未采纳原因 |
|------|------|------------|
| A. `AnrHelper` 观测 + Provider 回写 | system_server hook 后 `insert` `CrashLogProvider` | System Framework scope；API/OEM 漂移；system 进程稳定性 |
| B. Framework 抑制 ANR | hook `killLocked` / 不发 ANR 对话框 | 僵尸 UI；整机风险；偏离产品定义 |
| C. 仅 logcat，不做任何 ANR | 维持现状 | 已采纳路径 A；路径 B 作为无 root 增强 backlog |
| D. app 内 Watchdog 先于系统 5s | 主线程 ping | 误报与耗电；非 MVP |

## 后果

### 正面

- 维持「只勾目标 app」的 LSPosed UX；不扩大 system_server blast radius
- ANR 能力与 [framework-injection-feasibility.md](../architecture/framework-injection-feasibility.md) 用例 2（`parseQueries`）解耦：后者仍为**可选**包可见性补丁，非 ANR
- 实施路径清晰：logcat 多源 → 可选 `ApplicationExitInfo`

### 负面

- 无实时 Framework 回调；依赖 root logcat 拉取或事后导入
- logcat 环形缓冲可能冲掉旧 ANR 行
- INTERCEPT 场景下「假活 + 真 ANR」只能靠系统侧日志间接发现

## 合规

- [ADR-007](007-crash-log-cross-process-storage.md)：跨进程存储仍走 app 级 + Provider；ANR 不引入 system 代写
- [ADR-023](023-injection-observe-intercept-split.md)：观测/拦截分离不扩展为吞 ANR；INTERCEPT 下 ANR 行为见 §4
- [framework-injection-feasibility.md](../architecture/framework-injection-feasibility.md)：用例 1（AMS crash）结论不变；本 ADR 覆盖 ANR 专用 hook
- [规则 3a](../../docs/DOCUMENTATION.md#规则-3a提交前文档门禁)：本 ADR 为方案 commit；logcat buffer P2/P3 / ApplicationExitInfo 编码另 commit

## 相关文档

- [anr-observation.md](../architecture/anr-observation.md)
- [framework-injection-feasibility.md](../architecture/framework-injection-feasibility.md)
- [logcat-multi-source.md](../architecture/logcat-multi-source.md)
- [adb-logcat-analysis.md](../architecture/adb-logcat-analysis.md)
- [crash-logging.md](../architecture/crash-logging.md)
- [crash-intelligent-analysis.md](../architecture/crash-intelligent-analysis.md)
- [glossary.md](../glossary.md)
