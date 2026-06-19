---
title: "受管应用与干预规则 UI 方案"
type: plan
status: active
phase: 3
updated: 2026-06-19
summary: "Phase 3G 配置 IA 从全量列表改为受管策展 + Half Sheet 添加 + 行内 Switch + 编辑页规则"
---

# 受管应用与干预规则 UI 方案

## 背景

用户要求：支持 **添加/移除应用**；**无规则也可添加**；进 **编辑页手动添加规则**；Picker 用 **Half Sheet**；列表保留 **行内 Switch** 快捷开关。

## 决策摘要

| 项 | 结论 |
|----|------|
| 数据 | `managed_packages` + `intervention_rules` JSON；Legacy 哨兵 `null` |
| 无规则 | 不 hook；Switch OFF；角标「待配置」 |
| Switch ON | 隐式/启用 `CATCH_ALL` |
| Switch OFF | 全部规则 `enabled=false`，保留数据 |
| Picker | `AddManagedAppBottomSheet`（Draggable Half Sheet） |
| 编辑 | `AppInterventionEditActivity`（L3） |
| ADR | [ADR-015](../../../docs/decisions/015-managed-apps-intervention-rules.md) |

## 文档产出

- [app-management-ui.md](../../../docs/architecture/app-management-ui.md) — accepted
- ADR-015 — accepted
- ADR-005 修订、usage/configuration-ui/navigation-ia/ui-routing/scope-and-prefs/glossary 交叉链接

## 实施顺序

1. **3G-α**：`ManagedAppRepository`、`PrefMigrator.migrateManagedModel()`、`ScopePolicy` 扩展、单测
2. **3G-β**：UI — 受管列表、Half Sheet、编辑页、行内 Switch

## 相关文档

- [phase3_ui_redesign.md](../../roadmap/active/phase3_ui_redesign.md) §3G
- [app-management-ui.md](../../../docs/architecture/app-management-ui.md)
- [ADR-015](../../../docs/decisions/015-managed-apps-intervention-rules.md)
