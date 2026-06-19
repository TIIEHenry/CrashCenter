---
title: "Phase 3: 配置 UI 重设计"
type: roadmap
status: in_progress
phase: 3
updated: 2026-06-19
summary: "Material v1 验收 + 单屏密度 IA；LSPosed 手动项收尾"
---

# Phase 3: 配置 UI 重设计

## 背景

Material v1 已实现（见 [material-ui-redesign-2026-06-19.md](../../iterations/configuration-ui/material-ui-redesign-2026-06-19.md)），但未完成构建验证与真机 smoke。本 Phase 先建立验收基线，再分层优化信息架构与体验。

详细拆解见 [ui-redesign-execution-plan.md](../../plans/ui-redesign-execution-plan.md)。

## 前置

- Phase 2 verification 工具链就绪（adb-smoke 脚本、模板）
- 不改变 hook/scope 语义（ADR-002、ADR-003）

---

## 3A — 验收与基线修复 🚧

- [x] ADR-004 构建工具链策略定稿（Gradle 9.2.1 / AGP 9.0.0 / compileSdk 36 / JDK 17）
- [x] `./gradlew :app:assembleDebug` 稳定通过
- [x] 非 Windows 环境可构建 debug（release 使用 debug 签名）
- [x] material-ui-redesign 验证清单：构建项已勾选
- [ ] adb smoke + 手动 UI 项 → 首份 `dev/verification/smoke_YYYYMMDD.md`（自动化部分已完成）
- [x] 修正 verification README scope_mode 描述

## 3D — 代码结构（轻量）

- [x] 启用 ViewBinding，移除 kotlin-android-extensions
- [x] 去 Anko `doAsync`，消除 Activity 泄漏风险
- [x] 替换 ProgressDialog
- [x] 删除 `pref_general.xml`
- [x] 3A smoke 回归通过
- [x] 迭代记录 `dev/iterations/configuration-ui/structure-cleanup-*.md`

## 3B — 信息架构（修订：单屏密度）

- [x] ADR-005 定稿 → **单屏 FilterChip 全局设置**（否决迁出主屏）
- [~] ~~设置迁出主屏（Settings Activity 或 BottomSheet）~~ **defer**（用户要求单屏配置）
- [x] 主屏紧凑化：单行状态条、Chip 设置、Dense 搜索、扁平列表
- [x] 状态条点击 → Xposed 管理器（多框架回退）
- [x] 更新 `configuration-ui.md`、`usage.md`
- [ ] （P2）搜索/Chip 粘性头部

## 3C — 体验 Polish

- [x] 过滤空状态 UI
- [ ] ActivityCrashInfo 复制 + 分享
- [ ] （可选）暗色主题 `values-night/`
- [ ] 新文案 `values` + `values-zh`

## 3E — Material 3（可选，默认不做）

- [ ] ADR-006 M3 主题定稿（工具链已在 ADR-004 升级）
- [x] compileSdk 36 / AGP 9.0.0 / Gradle 9.2.1 升级（ADR-004）
- [ ] Material 3 主题迁移
- [ ] 全量 hook smoke 回归

---

## 验收标准

```bash
./gradlew :app:assembleDebug
./scripts/generate-docs-index.sh
python3 scripts/check-docs-health.py
# 真机:
./scripts/adb-smoke-verification.sh -s <serial>
# + 手动 UI 项见 execution plan 3A 节
```

## 相关文档

- [ui-redesign-execution-plan.md](../../plans/ui-redesign-execution-plan.md)
- [configuration-ui.md](../../../docs/architecture/configuration-ui.md)
- [material-ui-redesign-2026-06-19.md](../../iterations/configuration-ui/material-ui-redesign-2026-06-19.md)
