---
title: "ADR-023: 全量注入与观测/拦截分离"
type: decision
status: accepted
phase: 5
updated: 2026-06-23
summary: "默认对 LSPosed 作用域内 app 安装观测捕获；Switch/CATCH_ALL 仅控 shouldIntercept；修订 ADR-015 hook 门控"
---

# ADR-023: 全量注入与观测/拦截分离

## 状态

**Accepted** — 方案见 [injection-observe-intercept-split.md](../architecture/injection-observe-intercept-split.md)。

## 背景

[ADR-015](015-managed-apps-intervention-rules.md) 将「受管列表成员」与「至少一条 enabled 干预规则」作为 `shouldHook` 前提，导致：

- 未策展的 app **完全不注入**，无法记录崩溃
- Switch OFF = **不 hook**，与用户对「开关 = 是否拦截」的心智不符
- [crash-logging.md](../architecture/crash-logging.md) 观测层理论上可独立于干预层，但实现上绑定 `CrashHandler.insert`

用户明确要求：**默认注入、不区分模块内作用域；Switch 只区分是否拦截。**

## 决策

### 1. 拆分 `ScopeDecision` 门控

**采纳** `shouldInstall` + `shouldIntercept`，废弃 `shouldHook` 一词的多义用法：

| 字段 | 含义 |
|------|------|
| `shouldInstall` | 是否 hook `Application.onCreate` 并安装捕获器 |
| `shouldIntercept` | 是否启用 ADR-001 拦截续命（否则为观测模式） |
| `showNotify` | 反馈；观测模式默认 false |
| `crashLogEnabled` | 是否写 JSONL；观测/拦截均可用 |

### 2. 默认注入策略

- **除** `IGNORED_PACKAGES`、无 `appInfo`、`scope_mode` 系统过滤外，凡 LSPosed 调用 `handleLoadPackage` 的包 → `shouldInstall = true`
- **不**以 `managed_packages` 成员资格阻止注入
- 外层门控仍为 **LSPosed 作用域**；模块无法观测未 scoped 的 app

### 3. Switch / 干预规则语义

- `CATCH_ALL.enabled`（及行内 Switch ON）→ `shouldIntercept = true`
- Switch OFF 或无 enabled 规则 → `shouldIntercept = false`，**`shouldInstall` 仍为 true**
- `managed_packages` 降级为 **策展配置集**（快捷访问 per-app 拦截/通知偏好），非 hook 白名单

### 4. `CrashHandler` 双模式

- **INTERCEPT**：现网行为，不转发 UEH，Looper 续命
- **OBSERVE**：捕获后走 `CrashCapturePipeline`，再转发 saved default UEH；主线程 Looper 单次 catch 后 **不续命**

### 5. 观测模式日志

- **必须**在进程退出前完成至少 relay 或 canonical 同步写入（短超时）
- 拦截模式可继续使用 `logAsync`

### 6. 修订 ADR-015

以下条款由本 ADR **取代**（ADR-015 正文保留历史，顶部加 superseded 注记）：

- §2「无 enabled 规则 → shouldHook=false」
- §3 Picker 添加「hook 侧不安装 CrashHandler」
- §4 `managed_packages` 作为 hook 成员 SSOT
- 「待配置 = 不 hook」

### 7. 迁移

- 新键 `observe_intercept_split_migrated`
- 原 `shouldHook=true` 的 app → `shouldIntercept=true`（行为保持）
- 原 `shouldHook=false` 的 app → `shouldInstall=true`, `shouldIntercept=false`（**行为变化**：从无日志到可观测）

## 备选方案

| 方案 | 描述 | 未采纳原因 |
|------|------|------------|
| A. 维持 ADR-015，仅加全局「观测全部」开关 | 两套门控并存 | 语义重复，用户仍困惑 |
| B. 仅改 Legacy，受管模型不变 | 双轨策略 | 与「不区分作用域」目标不符 |
| C. 观测靠 logcat ingest，不注入 | 无 Xposed 观测 | 无法结构化 stack + 与模块 prefs 联动 |
| D. observe 仍用无限 Looper 但不通知 | 进程不退出 | 违背「不拦截则允许退出」 |

## 后果

### 正面

- 崩溃历史覆盖 **全部 scoped app**，无需先「添加受管」
- 观测 / 拦截分层与 crash-logging 架构一致
- Switch 语义与用户心智对齐

### 负面

- 全量 `Application.onCreate` hook（scoped 包）；observe 仍有 UEH + Looper 包装开销
- JSONL 体积与隐私面扩大
- 受管模型用户经历 **行为迁移**（原无 hook → 现观测）
- 实现量：CrashHandler 重构 + ScopePolicy 矩阵重写 + 同步日志路径

## 合规

- [ADR-011](011-feedback-failure-isolation.md)：拦截路径不变；观测路径允许进程退出
- [ADR-010](010-scope-policy-show-notify.md)：`ScopeDecision` 扩展，仍实例级闭包
- [规则 3a](../../docs/DOCUMENTATION.md#规则-3a提交前文档门禁)：本 ADR 为方案 commit；代码实施另 commit

## 相关文档

- [injection-observe-intercept-split.md](../architecture/injection-observe-intercept-split.md)
- [ADR-015](015-managed-apps-intervention-rules.md)
- [ADR-001](001-looper-loop-resurrection.md)
- [crash-logging.md](../architecture/crash-logging.md)
- [scope-and-prefs.md](../architecture/scope-and-prefs.md)
