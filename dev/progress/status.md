---
title: "项目进度状态"
type: progress
status: active
phase: N/A
updated: 2026-06-20
summary: "Phase 3/4 UI + 4B-α/4C-β as-built 文档同步；ADR-017 proposed；IS 矩阵待验"
---

# 项目进度状态

## Snapshot

| 项 | 状态 |
|---|------|
| 活跃 Phase | [Phase 3](../roadmap/active/phase3_ui_redesign.md) 🔄（LSPosed 手动 smoke 收尾） |
| Phase 2 | [文档工具与验收](../roadmap/active/phase2_documentation_tooling.md) — 近完成（待 LSPosed 真机报告） |
| 下一 Phase | [Phase 4](../roadmap/active/phase4_crash_observability.md) 🔄 **4B-α + 4C-β as-built**；**ADR-017** proposed；IS 矩阵待验 |
| 阻塞 | LSPosed 手动项（Test 拦截、Switch→hook、激活状态条）；4B IS-1~IS-6 写路径 |
| 验证基线 | `461QYGDD2226C` **2026-06-19**：consolidated smoke **PASS**；dark mode Meizu **PASS** |
| 文档 | as-built 同步：ui-routing、crash-data-layer、overview、crash-history-ui、app-di-and-module-boundaries |

### 已完成

- Phase 1 归档；Phase 2 近完成（verification 工具链）
- Material v1 + 3D 结构清理 + Fluent 对齐 + 单屏密度 IA + 包可见性/迁移/更名
- **UI Shell 文档**：ADR-009；配置域 `ConfigFragment` / `ConfigUiState`；Phase 4C 2-tab Shell 边界
- **Phase 3G**：方案定稿 + 3G-α 数据层 + **3G-β 受管列表 UI**（Switch / Half Sheet / Edit）
- **Phase 4C-α Shell**：`MainShellActivity` / `ConfigFragment` / Design System 组件类化；adb smoke PASS
- **Phase 4C-β scaffolding**：`CrashHistoryFragment` + `FileCrashLogRepository` + `CrashEventRow`；Observe 空态
- **Phase 4B 写入 scaffold**：`CrashLogCoordinator` + `DirectFsCrashLogWriter`；hook 异步 append `events.jsonl`；UUID + `source`；`crash_log_enabled` pref
- **ADR-013**：通知 Intent 从 `Exception` 字符串改为 `crash_id` UUID；`ActivityCrashInfo` 通过 `CrashDetailLoader` 从 `CrashLogRepository` 加载详情；旧 `Exception` extra 保留 fallback（2026-06-20）
- **M2** permission banner compact；**M5** Add sheet 28dp 圆角 + DragHandle + peekHeight 50%
- **Legacy backlog**：删 `activity_main`、OnBackPressed、sheet EmptyState、i18n/a11y/tint、ScopePolicy 澄清
- **P0/P1 修复**：`ModuleActivation`、BNV StackOverflow、`PrefMigrator` fresh install、Observe 空态 copy、`ArrayUtil` 可见性
- **文档 as-built 同步（2026-06-20）**：ui-routing、crash-data-layer、crash-history-ui、overview、architecture-optimization §9.3；新建 app-di-and-module-boundaries、**ADR-017** proposed

### 待办

- **3A**：LSPosed 手动 smoke → 补全 `dev/verification/smoke_20260619.md`
- **4B-β**：按 [ADR-017](../../docs/decisions/017-root-ingest-and-dedupe.md) 实施 RootSu + ingest dedupe；**IS-1~IS-6** 真机矩阵
- **4D**：StatsAggregator、观测统计 tab、Repository clear/retention（ADR-019 待立项）
- **暗色**：AOSP 模拟器 API 30/34/36；CodeEditor `setDark(night)`
- Phase 2 归档（待 LSPosed smoke 报告）

---

## Recent Sessions

### 2026-06-20 — 架构文档 as-built 同步 + ADR-017 proposed

- **文档**：修订 ui-routing / crash-data-layer / crash-history-ui / architecture-optimization §9.3；新建 [app-di-and-module-boundaries.md](../../docs/architecture/app-di-and-module-boundaries.md)、[ADR-017](../../docs/decisions/017-root-ingest-and-dedupe.md)（4B-β root ingest + dedupe，待 IS 矩阵 accepted）
- **backlog**：[architecture-decision-backlog.md](../plans/architecture-decision-backlog.md) §3.1 多数已勾选
- **下一步**：LSPosed IS-1~IS-6 → verification 报告 → ADR-017 accepted → 4B-β 编码

### 2026-06-19 — M7 empty state icon polish（461QYGDD2226C）

- **实现**：`emptyIcon` 40dp + `textColorSecondary` tint α0.38；`EmptyState.bind(…, iconRes)` 可选重载；Config `ic_tab_config`、Observe `ic_tab_observe`、Add sheet `ic_add`；sheet empty `wrap_content` + `top|center_horizontal` 防 peek 裁切
- **构建**：`:app:assembleDebug` OK
- **真机**：Config/Observe 空态图标 110×110px；Add sheet 空筛 `ic_add` 居中可见

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
