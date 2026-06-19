---
title: "项目进度状态"
type: progress
status: active
phase: N/A
updated: 2026-06-19
summary: "4B-α 多后端写入 + Provider + retention 已编码；IS-1~6 待 LSPosed 真机"
---

# 项目进度状态

## Snapshot

| 项 | 状态 |
|---|------|
| 活跃 Phase | [Phase 3 — 配置 UI 重设计](../roadmap/active/phase3_ui_redesign.md) 🔄（Shell/受管/暗色 **已提交**） |
| Phase 2 | [文档工具与验收](../roadmap/active/phase2_documentation_tooling.md) — 近完成（待 LSPosed 真机报告） |
| 下一 Phase | [Phase 4 — 崩溃可观测性](../roadmap/active/phase4_crash_observability.md) 🔄 **4B-α** 多后端写入已编码；IS-1~6 真机验待 LSPosed |
| 阻塞 | LSPosed 手动项（Test 拦截、Switch→hook、激活状态条绿/无弹窗） |
| 验证基线 | `461QYGDD2226C` **2026-06-19**：consolidated smoke **PASS**；dark mode QA **PASS**（Meizu；AOSP defer） |

### 已完成

- Phase 1 归档；Phase 2 近完成（verification 工具链）
- Material v1 + 3D 结构清理 + Fluent 对齐 + 单屏密度 IA + 包可见性/迁移/更名
- **UI Shell 文档**：ADR-009；配置域 `ConfigFragment` / `ConfigUiState`；Phase 4C 2-tab Shell 边界
- **Phase 3G**：方案定稿 + 3G-α 数据层 + **3G-β 受管列表 UI**（Switch / Half Sheet / Edit）
- **Phase 4C-α Shell**：`MainShellActivity` / `ConfigFragment` / Design System 组件类化；adb smoke PASS
- **Phase 4C-β 半屏详情**：`CrashDetailBottomSheet` + `CrashLogViewerClient` + Gradle CodeEditor（CelestailRuler sibling）；历史列表 → Sheet；通知仍走 `ActivityCrashInfo`
- **Phase 4B-α 多后端写入**：`CrashLogBackend` + 注册表；Coordinator Phase 2 并行 Provider / DirectFs / TargetRelay；`CrashLogProvider`（exported、无 signature permission）；`CanonicalJsonlWriter` retention 500 条 / 8MB；`CrashEvent.backendWritten`
- **Phase 4B 写入 scaffold**：`CrashLogCoordinator`；hook 异步 append `events.jsonl`；UUID + `source`；`crash_log_enabled` pref
- **暗色 DayNight A–D**：`values-night/` + `ThemeColors` + Material 对话框；Meizu Phase D QA PASS
- **M2** permission banner compact；**M5** Add sheet 28dp 圆角 + DragHandle + peekHeight 50%
- **Legacy backlog**：删 `activity_main`、OnBackPressed、sheet EmptyState、i18n/a11y/tint、ScopePolicy 澄清
- **P0/P1 修复**：`ModuleActivation`、BNV StackOverflow、`PrefMigrator` fresh install、Observe 空态 copy、`ArrayUtil` 可见性
- **架构 P0（ADR-010/011）**：`ScopePolicy`/`ScopeDecision`；`CrashCapturePipeline` + `CrashFeedbackFacade`；移除 feedback 路径 `System.exit`

### 待办

- **3A**：LSPosed 手动 smoke → 补全 `dev/verification/smoke_20260619.md`
- **4B-α 真机验**：IS-1~IS-6 独立启动矩阵；LSPosed 手动项
- **暗色**：AOSP 模拟器 API 30/34/36；Permission banner compact 稳定复现；CrashEventRow（待 4B 真机数据）
- Phase 2 归档；Phase 4B-β root ingest 待建

---

## Recent Sessions

### 2026-06-19 — Phase 4B-α 多后端崩溃写入

- **`CrashLogBackend`** 接口 + `CrashLogBackendRegistry`（hook Phase 2：Provider / DirectFs / TargetRelay）
- **`CrashLogCoordinator`**：并行 Phase 2（2s 超时）；各后端独立 catch；`backendWritten` 追踪
- **`CrashLogProvider`**：`exported="true"`、无 signature permission；UID↔package 校验 + rate limit
- **`CanonicalJsonlWriter`**：FileLock append + retention（500 条 / 8MB 删最旧）
- **`CrashEvent.backendWritten`**：JSON 数组字段；各后端写入时 stamp 自身 id
- 验证：`:app:assembleDebug` PASS；IS-1~IS-6 待 LSPosed 真机

### 2026-06-19 — 半屏 + Editor 崩溃详情（文档 + 编码）

- **架构**：Hybrid dual-carrier — 壳内 `CrashDetailBottomSheet`；外部 `ActivityCrashInfo`；共用 `CrashLogViewerClient`
- **编码**：`CrashDetailBottomSheet`、`CrashLogViewerClient`、`bottom_sheet_crash_detail.xml`；`CrashHistoryFragment` → Sheet；`ActivityCrashInfo` 接入 CodeEditor
- **构建**：Gradle include `../CelestailRuler` CodeEditor 模块；`assembleDebug` PASS；CI 需 sibling checkout + `android.nonTransitiveRClass=false`
- **文档**：`navigation-ia`、`ui-routing`、`crash-history-ui`、`code-editor-porting`、`design-system`、roadmap

### 2026-06-19 — 半屏 + Editor 崩溃详情方案（文档）

- **架构决策**：Hybrid dual-carrier — 壳内 `CrashDetailBottomSheet`（Draggable Half Sheet + `CrashLogViewerClient`）；外部通知 / 深链仍走全屏 `ActivityCrashInfo` / `CrashLogDetailActivity`
- **Sheet 规范**：复用 `AddManagedAppBottomSheet` 模式（28dp 顶圆角、50% peek、DragHandle-only）；**禁止** settings-card-detail-sheet 承载 stack trace
- **文档更新**：`navigation-ia.md`、`ui-routing.md`（`crash_detail_sheet` 路由）、`crash-history-ui.md`、`code-editor-porting.md`、`design-system.md`、`phase4_crash_observability.md`

### 2026-06-19 — 提交：Phase 3/4 UI marathon + 文档

- **方案 commit**：ADR-009~015、架构文档（Shell/受管/暗色/崩溃观测）、roadmap 更新
- **实施 commit**：`MainShellActivity` 2-tab Shell、`ConfigFragment`/3G 受管模型、`CrashHistoryFragment`、DayNight A–D、4B `CrashLogCoordinator` scaffold、Design System 组件、M2/M5/M7 polish
- **验收**：[dark_mode_qa_20260619.md](../verification/dark_mode_qa_20260619.md)；consolidated smoke PASS；4B 读路径 PASS / 写路径待 LSPosed
- **仍 open**：LSPosed 手动项、AOSP dark QA、CodeEditor 详情、4B-α 多后端

---

更早会话见 [archive/status-sessions-2026-06.md](archive/status-sessions-2026-06.md)。

### 2026-06-19 — 架构 P0：CrashCapturePipeline + ADR-011

- **`CrashCapturePipeline`**：hook 回调单入口 → `CrashLogCoordinator` + `CrashFeedbackFacade`
- **`CrashFeedbackFacade`**：Toast/Notification 从 `XposedEntry` 迁出；失败仅 log，**删除** `System.exit(0)`
- **`XposedEntry`**：薄入口，仅 ScopePolicy + `CrashHandler.insert` 委托
- 文档：`xposed-entry.md`、`crash-notification.md`、`architecture-optimization.md` P0 标记已解决
- 验证：`:app:assembleDebug` PASS

### 2026-06-19 — UI 视觉合规修复（adb 截图验证）

- **按压反馈**：列表行 / 状态条从 Material ripple 迁移到 iOS-style overlay；新增 `shell_press_surface.xml` / `shell_press_surface_rounded.xml` + `press_overlay` token（light/dark）
- **搜索框**：圆角 4dp → 8dp（`radius_mobile_control`）
- **Chip**：高度 32dp → 28dp
- **水平内边距**：统一为 16dp `content_padding_horizontal`；配置页 / BottomSheet 列表行 / 状态条对齐
- **BottomSheet**：轻量标题栏（× / 居中标题 / 完成，对称 16dp inset）；去掉全宽 divider；drag handle 加深 + 抬高；增加 4dp elevation 与 subtle outline 以增强 28dp 圆角感知
- **验证**：`./gradlew :app:assembleDebug` PASS；adb install + 截图确认主屏与 Add app sheet 视觉改善
- **CrashEventRow**：同步 `shell_press_surface` + 16dp 内边距；来源角标右对齐垂直居中；副标题优先展示 message；角标文案缩短（UEH/Looper）

---

## 相关

- [roadmap/INDEX.md](../roadmap/INDEX.md)
- [verification/README.md](../verification/README.md)
- [DEV_GUIDE.md](../DEV_GUIDE.md)
