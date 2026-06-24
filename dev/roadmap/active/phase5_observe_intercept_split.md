---
title: "Phase 5: 全量观测与拦截分离"
type: roadmap
status: in_progress
phase: 5
updated: 2026-06-24
summary: "ADR-023：默认注入观测；Switch 仅控 shouldIntercept；CrashHandler 双模式"
---

# Phase 5: 全量观测与拦截分离

## 背景

[ADR-023](../../../docs/decisions/023-injection-observe-intercept-split.md) / [injection-observe-intercept-split.md](../../../docs/architecture/injection-observe-intercept-split.md)：模块内不再用受管列表门控注入；行内 Switch 映射 `shouldIntercept`。

## 5.0 — 方案 ✅

- [x] `docs/architecture/injection-observe-intercept-split.md`
- [x] ADR-023 accepted
- [x] 本 roadmap

## 5.1 — ScopePolicy + XposedEntry

- [x] `ScopeDecision`: `shouldInstall` + `shouldIntercept`
- [x] `ScopePolicy` 新语义（全量 install；intercept 由 Switch / CATCH_ALL）
- [x] `XposedEntry` 按模式安装 `CrashHandler`
- [x] `ScopePolicyTest` / `ScopeDecisionTest` 更新

## 5.2 — CrashHandler 双模式

- [x] `Mode.INTERCEPT` — 现网 Looper 续命 + 不转发 UEH
- [x] `Mode.OBSERVE` — 捕获后转发 UEH / Looper rethrow

## 5.3 — 观测路径同步日志

- [x] `CrashLogCoordinator.logSync`（relay 优先 + 短超时）
- [x] `CrashCapturePipeline` 观测模式调用 `logSync`

## 5.4 — 迁移与文档

- [x] `observe_intercept_split_migrated` 一次性标记
- [x] UI 文案：角标/筛选「仅监测」「已拦截」；空态与移除确认
- [x] `CrashEvent.intercepted` + 历史/单应用 item 角标
- [x] 观测 Toolbar 去重（移除「统计」菜单；子 tab 显隐）；空态 CTA（历史→配置、统计→历史）
- [x] 同步 `scope-and-prefs.md`、`app-management-ui.md`、`usage.md`、`crash-handler.md`、`crash-capture-pipeline.md`、`glossary.md`

## 5.5 — 验收

- [ ] scoped app 未策展：崩溃后 JSONL 新增且进程退出（observe）
- [ ] Switch ON：进程不退出（intercept）
- [x] `assembleDebug` + 单元测试
- [ ] LSPosed 手动 smoke（observe + intercept）

## 相关文档

- [injection-observe-intercept-split.md](../../../docs/architecture/injection-observe-intercept-split.md)
- [ADR-023](../../../docs/decisions/023-injection-observe-intercept-split.md)
- [status.md](../../progress/status.md)
