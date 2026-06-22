---
title: "Phase 3: 配置 UI 重设计"
type: roadmap
status: in_progress
phase: 3
updated: 2026-06-22
summary: "Shell/3Gβ/暗色 A–D/M2+M5 已编码；3E M3 方案已定（ADR-022）；LSPosed 手动 smoke 收尾"
---

# Phase 3: 配置 UI 重设计

## 背景

Material v1 已实现（见 [material-ui-redesign-2026-06-19.md](../../iterations/configuration-ui/material-ui-redesign-2026-06-19.md)），但未完成构建验证与真机 smoke。本 Phase 先建立验收基线，再分层优化信息架构与体验。

详细拆解见 [ui-redesign-execution-plan.md](../../archive/ui-redesign-execution-plan.md)。

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
- [x] （可选）暗色主题 `values-night/` — Phase A–D 已编码（[dark-mode-theming.md](../../../docs/architecture/dark-mode-theming.md)；Phase D **Meizu 实机 PASS**，AOSP 模拟器 defer）
- [ ] 新文案 `values` + `values-zh`

## 3E — Material 3 + minSdk 26 🔄

方案：[material3-migration.md](../../../docs/architecture/material3-migration.md) · [ADR-022](../../../docs/decisions/022-material3-static-theme-minsdk26.md)

- [x] ADR-022：minSdk **26**、M3 静态 Fluent、**拒绝 dynamic color**（取代 ADR-006）
- [x] compileSdk 36 / AGP 9.0.0 / Gradle 9.2.1 升级（ADR-004）
- [ ] **M0** minSdk 26 全模块 + Material 1.14+ catalog 统一
- [ ] **M1** `Theme.Material3.DayNight` + Fluent → M3 语义色 + Widget 父样式
- [ ] **M2** Design System 组件回归（Chip/Switch/Sheet/Banner）
- [ ] **M3** 页面扫尾 + `ui_common` tag chip
- [ ] **M4** 浅/深 smoke + hook 回归 + verification 报告

## 3G — 受管应用与干预规则（方案）

- [x] [app-management-ui.md](../../../docs/architecture/app-management-ui.md) accepted + [ADR-015](../../../docs/decisions/015-managed-apps-intervention-rules.md)
- [x] 迭代记录 [managed-apps-intervention-2026-06-19.md](../../iterations/configuration-ui/managed-apps-intervention-2026-06-19.md)
- [x] 3G-α：`managed_packages` / `intervention_rules` + `PrefMigrator` 迁移 + `ScopePolicy` 扩展
- [x] 3G-β：受管列表 UI + 行内 Switch + `AddManagedAppBottomSheet` + `AppInterventionEditActivity`
- [x] M5：`AddManagedAppBottomSheet` 装饰 polish — 28dp 顶圆角 + DragHandle + `peekHeight` 50%（全功能拖曳 / Full chrome morph defer）
- [x] M2：permission banner compact — Xposed 未激活时单行文案、隐藏 Grant（整行仍可点 rationale）

## 3F — UI 架构重设（文档 + 壳层 α）

- [x] ADR-009：Shell + Design System + Domain Page + Feature State 架构定稿
- [x] 更新 `configuration-ui.md`：配置域从 `ActivityMain` 单体演进为 `ConfigFragment` + `ConfigUiState`
- [x] 更新 `navigation-ia.md` / `ui-routing.md`：Phase 4C 2-tab Shell、`MainShellActivity`、`ObserveHostFragment`、详情参数兼容
- [x] 代码实施（4C-α）：`MainShellActivity` / `ShellViewModel` / `ConfigFragment` / `ConfigViewModel` / `AppListRepository`；Launcher 迁 Shell；`ActivityMain` 兼容 wrapper
- [x] Design System 组件类化：`StatusBanner`、`PermissionBanner`、`FilterChipRow`、`DenseSearchField`；`ConfigFragment` / `MainShellActivity` 接线

## 3H — UI 审计遗留 backlog

- [x] 删 `activity_main.xml`；`MainShellActivity` `OnBackPressedCallback`；Observe `menu_observe_stub` disabled
- [x] `AddManagedAppBottomSheet` → `EmptyState`；`crash_source_*` EN+zh；`FilterChipRow` dead code 清理
- [x] `ActivityCrashInfo` → `ToolbarHeaderInsets`；图标 tint / legacy row a11y；`ScopePolicy` `globalDefaultShowNotify` 澄清

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

- [app-management-ui.md](../../../docs/architecture/app-management-ui.md)
- [ADR-015: 受管应用与干预规则](../../../docs/decisions/015-managed-apps-intervention-rules.md)
- [ui-redesign-execution-plan.md](../../archive/ui-redesign-execution-plan.md)
- [configuration-ui.md](../../../docs/architecture/configuration-ui.md)
- [ADR-009: UI Shell 与 Design System 架构](../../../docs/decisions/009-ui-shell-design-system.md)
- [material3-migration.md](../../../docs/architecture/material3-migration.md)
- [ADR-022: M3 静态 Fluent + minSdk 26](../../../docs/decisions/022-material3-static-theme-minsdk26.md)
- [material-ui-redesign-2026-06-19.md](../../iterations/configuration-ui/material-ui-redesign-2026-06-19.md)
