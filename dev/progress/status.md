---
title: "项目进度状态"
type: progress
status: active
phase: N/A
updated: 2026-06-19
summary: "Phase 3/4 UI marathon 已提交：Shell、3G、暗色、4B scaffold、验收报告"
---

# 项目进度状态

## Snapshot

| 项 | 状态 |
|---|------|
| 活跃 Phase | [Phase 3 — 配置 UI 重设计](../roadmap/active/phase3_ui_redesign.md) 🔄（Shell/受管/暗色 **已提交**） |
| Phase 2 | [文档工具与验收](../roadmap/active/phase2_documentation_tooling.md) — 近完成（待 LSPosed 真机报告） |
| 下一 Phase | [Phase 4 — 崩溃可观测性](../roadmap/active/phase4_crash_observability.md) 🔄 **4B scaffold** 已编码；CodeEditor 详情待建 |
| 阻塞 | LSPosed 手动项（Test 拦截、Switch→hook、激活状态条绿/无弹窗） |
| 验证基线 | `461QYGDD2226C` **2026-06-19**：consolidated smoke **PASS**；dark mode QA **PASS**（Meizu；AOSP defer） |

### 已完成

- Phase 1 归档；Phase 2 近完成（verification 工具链）
- Material v1 + 3D 结构清理 + Fluent 对齐 + 单屏密度 IA + 包可见性/迁移/更名
- **UI Shell 文档**：ADR-009；配置域 `ConfigFragment` / `ConfigUiState`；Phase 4C 2-tab Shell 边界
- **Phase 3G**：方案定稿 + 3G-α 数据层 + **3G-β 受管列表 UI**（Switch / Half Sheet / Edit）
- **Phase 4C-α Shell**：`MainShellActivity` / `ConfigFragment` / Design System 组件类化；adb smoke PASS
- **Phase 4C-β scaffolding**：`CrashHistoryFragment` + `FileCrashLogRepository` + `CrashEventRow`；Observe 空态
- **Phase 4B 写入 scaffold**：`CrashLogCoordinator` + `DirectFsCrashLogWriter`；hook 异步 append `events.jsonl`；UUID + `source`；`crash_log_enabled` pref
- **暗色 DayNight A–D**：`values-night/` + `ThemeColors` + Material 对话框；Meizu Phase D QA PASS
- **M2** permission banner compact；**M5** Add sheet 28dp 圆角 + DragHandle + peekHeight 50%
- **Legacy backlog**：删 `activity_main`、OnBackPressed、sheet EmptyState、i18n/a11y/tint、ScopePolicy 澄清
- **P0/P1 修复**：`ModuleActivation`、BNV StackOverflow、`PrefMigrator` fresh install、Observe 空态 copy、`ArrayUtil` 可见性

### 待办

- **3A**：LSPosed 手动 smoke → 补全 `dev/verification/smoke_20260619.md`
- **4C-β 收尾**：Gradle CodeEditor + 详情页；**4B scaffold** 已编码 — 真机验历史行（IS-1/2、跨包直写）
- **暗色**：AOSP 模拟器 API 30/34/36；Permission banner compact 稳定复现；CrashEventRow（待 4B 真机数据）
- Phase 2 归档；Phase 4B-α 多后端 / Provider / retention 待建

---

## Recent Sessions

### 2026-06-19 — 提交：Phase 3/4 UI marathon + 文档

- **方案 commit**：ADR-009~015、架构文档（Shell/受管/暗色/崩溃观测）、roadmap 更新
- **实施 commit**：`MainShellActivity` 2-tab Shell、`ConfigFragment`/3G 受管模型、`CrashHistoryFragment`、DayNight A–D、4B `CrashLogCoordinator` scaffold、Design System 组件、M2/M5/M7 polish
- **验收**：[dark_mode_qa_20260619.md](../verification/dark_mode_qa_20260619.md)；consolidated smoke PASS；4B 读路径 PASS / 写路径待 LSPosed
- **仍 open**：LSPosed 手动项、AOSP dark QA、CodeEditor 详情、4B-α 多后端

---

更早会话见 [archive/status-sessions-2026-06.md](archive/status-sessions-2026-06.md)。

## 相关

- [roadmap/INDEX.md](../roadmap/INDEX.md)
- [verification/README.md](../verification/README.md)
- [DEV_GUIDE.md](../DEV_GUIDE.md)
