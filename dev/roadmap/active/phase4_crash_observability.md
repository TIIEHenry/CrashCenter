---
title: "Phase 4: 崩溃可观测性"
type: roadmap
status: draft
phase: 4
updated: 2026-06-22
summary: "4B-α 多后端写入已编码；4B-γ canonical FS 方案；4C-β 半屏详情 + CodeEditor 已编码；4D 统计待实施"
---

# Phase 4: 崩溃可观测性

## 背景

当前拦截崩溃仅 Toast / 通知 / logcat，无历史与统计。方案：**多后端并行 + root 优先 + 模块 ingest**（[crash-log-backends.md](../../../docs/architecture/crash-log-backends.md)、[ADR-008](../../../docs/decisions/008-multi-backend-crash-log-storage.md)）；单后端 IPC 见 [crash-log-ipc.md](../../../docs/architecture/crash-log-ipc.md)、[ADR-007](../../../docs/decisions/007-crash-log-cross-process-storage.md)。

**产品叙事**：Xposed 稳定性分析中心 — **观测层**（记录 + 统计）与 **干预层**（吞异常）分离。

**实施顺序说明**（[architecture-optimization.md](../../../docs/architecture/architecture-optimization.md) §8）：4B 前宜完成 hook 侧 `showNotify` 消除与反馈/日志 try 隔离；4B-α 不依赖 UI 壳层；**4C 拆为 Shell/Design System 迁移（α）与历史 UI（β）**，避免 4D 二次导航重构；4G 不阻塞 4B–4E。

## 前置

- Phase 3 配置 UI 基线稳定（或并行，但不改 hook 语义）
- 方案文档 + ADR-007 / ADR-008 已 commit（本 Phase 编码前）

---

## 4A — 文档与决策 ✅

- [x] `docs/architecture/crash-logging.md`（status: proposed）
- [x] ADR-007 跨进程存储决策
- [x] `docs/architecture/crash-log-backends.md` + ADR-008 多后端并行
- [x] 本 roadmap 立项

## 4B — MVP：CrashLogCoordinator（无 UI）

> **2026-06-19 4B-α**：`CrashLogBackend` + Phase 2 并行（Provider + DirectFs + TargetRelay）；`CrashLogProvider`；retention 500/8MB。RootSu + ingest 仍待 4B-β。

### 4B-α — 基础后端 + 并行

- [x] `CrashLogBackend` 接口 + 注册表
- [x] `CrashLogCoordinator`：Phase 2 并行（Provider + DirectFs + TargetRelay）
- [x] `CrashEvent` 结构化 + UUID + `backendWritten` 多后端追踪
- [x] hook 回调内、`showNotify` **之外**调用 Coordinator
- [x] 采集：timestamp、package、进程/线程、异常类、完整 stack、`source`（`CrashHandler` 区分 looper/uncaught）
- [x] 默认 retention：500 条或 8MB 轮转
- [x] 写入失败 silent；不与 Toast/通知同 try
- [x] `CrashLogProvider`：`exported="true"`，无 signature permission
- [ ] **独立启动矩阵** IS-1~IS-6

### 4B-β — root 优先 + ingest

> 统一 Root 方案：[unified-root-service.md](../../../docs/architecture/unified-root-service.md)（单 `CrashCenterRootService` + `RootBroker`）；实施前 ADR-023。

- [ ] `RootSuBackend`（hook，`ShellOnlyAdapter` append canonical）
- [ ] Coordinator Phase 1 root 短窗（≤1500ms）
- [ ] 模块 `RootFsBackend` + `RelayMergeBackend`（libsu，参考 AppSnapShotor）
- [ ] `CrashLogIngestCoordinator`：模块启动 / Provider 回调触发
- [ ] 验收 IS-R1~IS-R5（见 [crash-log-backends.md](../../../docs/architecture/crash-log-backends.md)）
- [ ] 真机矩阵记录于 `dev/verification/`

### 4B-γ — Canonical 文件系统一致性

> 方案：[crash-log-filesystem.md](../../../docs/architecture/crash-log-filesystem.md)；决策 [ADR-021](../../../docs/decisions/021-canonical-jsonl-io-consistency.md)（proposed）。**不依赖** root；可与 IS 矩阵并行。

- [ ] `CanonicalJsonlStore`：`append` / `applyRetention` / `deleteById` / `clear` 统一 `FileLock`（FS-1、FS-2）
- [ ] `FileCrashLogRepository`：`timestampMs` 降序 + 读路径 `distinctBy { id }`（FS-3）
- [ ] 单测：append ∥ delete 同文件；排序 / dedupe（FS-4）
- [ ] DirectFs `mkdirs` 失败可观测（FS-5）
- [ ] 真机 FS-6 / FS-7（删改与 hook 写交错）

### 4B 验收：独立启动矩阵

> 详见 [crash-log-ipc.md § 目标进程独立启动](../../../docs/architecture/crash-log-ipc.md#目标进程独立启动时的权限与通信)

| # | 前置条件 | 操作 | 期望 |
|---|----------|------|------|
| IS-1 | 模块已激活 scope，**force-stop 模块**，目标 app 冷启动 | 触发崩溃 | `events.jsonl` 新增一行 **或** Provider 路径成功；干预层续命 |
| IS-2 | 安装后**从未打开** CrashCenter UI，仅启动目标 app | 触发崩溃 | 同上；`XSharedPreferences` 默认 scope 仍 hook |
| IS-3 | API 30+ 目标 app（无 `QUERY_ALL_PACKAGES`） | 观察 Primary A | 记录 `NameNotFoundException` / EACCES / 成功 |
| IS-4 | 模块 force-stop | `ContentResolver.insert` Fallback B | 模块进程被 AM 拉起；JSONL 写入 |
| IS-5 | （负例）Provider 若误配 signature permission | hook 侧 insert | **SecurityException** — 验证设计禁止此配置 |
| IS-6 | 模块运行中 vs force-stop | 对比 A/B 延迟与成功率 | 文档记录 ROM 差异 |

**判定**：IS-1 + IS-2 必须通过其一写入路径；IS-5 负例确认 signature 悖论。IS-R1~IS-R5 见 crash-log-backends.md。

### 4B-fallback — 已并入多后端

Provider / DirectFs / Relay 作为 `CrashLogBackend` 实现，不再单独 Phase。见 4B-α checklist。

## 4C — P1：Shell + 崩溃历史 UI

### 4C-α — UI Shell / Design System

- [x] 引入 `MainShellActivity` + `ShellViewModel`：Toolbar、Xposed 状态条、2-tab BottomNavigation、WindowInsets
- [x] `ActivityMain` 页面内容迁为 `ConfigFragment`；Launcher 入口迁 `MainShellActivity`
- [x] 抽出 common UI：`EmptyState`、`LoadingState`、`ToolbarHeaderInsets`、`StatusBanner`、`PermissionBanner`、`FilterChipRow`、`DenseSearchField`
- [x] 配置域拆出 `ConfigUiState`、`ConfigViewModel`、`AppListRepository`、`AppToggleAdapter`
- [x] `ObserveHostFragment` 占位（空态文案）；历史列表待 4C-β
- [x] 验收：adb smoke（461QYGDD2226C 2026-06-19）安装/启动/logcat PASS；底栏 **配置 | 观测** + Observe 空态/回路已 tap 验；配置 tab Phase 3 parity 与 Test 拦截仍 LSPosed 手动

### 4C-β — 崩溃历史

- [x] `ObserveHostFragment`：观测 tab 宿主；4C 先仅「历史」子页（4D 再加「统计」内层 TabLayout）
- [x] `CrashHistoryFragment`：时间倒序历史列表（scaffolding + `FileCrashLogRepository` 读 `events.jsonl`）
- [x] 列表：时间、包名、异常类、应用名（`CrashEventRow`）
- [x] 详情页：`CrashDetailBottomSheet`（壳内半屏）+ `ActivityCrashInfo`（通知全屏）；共用 `CrashLogViewerClient` / CodeEditor
- [x] **`CrashDetailBottomSheet`**：Draggable Half Sheet（28dp、50% peek、DragHandle）；历史列表 → Sheet
- [x] `CrashLogViewerClient`：只读 CodeEditor；Sheet 隐藏 float `upView`/`downView`
- [x] Gradle 引入 `CodeEditor` + `CodeEditorClient` + `CodeEditorAntlr` + `ui_common`（sibling CelestailRuler）
- [x] 按 `crash_id` 打开详情，兼容旧 `Exception` extra（壳内 Sheet + 外部 Activity 双载体；通知仍走 Activity）
- [x] 空状态与加载态

## 4D — P2：统计

需求详见 [crash-stats-ui.md](../../../docs/architecture/crash-stats-ui.md)。

- [ ] **全局统计页**（观测 → 统计子 tab）：摘要卡片、时间 Chip、应用/异常 TOP N
- [ ] **单应用观测页**：包级摘要 + 过滤列表；自全局统计下钻
- [ ] `StatsAggregator` + `CrashLogRepository` 扫描 `events.jsonl`
- [ ] 按日计数简单列表（无重型图表）
- [ ] 「清空历史」与确认对话框（全局 Toolbar）
- [ ] retention pref：`crash_log_enabled`、`crash_log_max_entries`
- [ ] （4D+）配置 tab 应用行菜单「崩溃记录」入口

## 4E — P3：导出与扩展

- [ ] SAF 导出 JSONL / zip（导出前隐私提示）
- [ ] 通知 Intent 传 `crash_id` 替代整段 stack
- [ ] （可选）JSONL → Room 迁移或 sidecar `stats.json` 索引
- [ ] 更新 [usage.md](../../../docs/guides/usage.md) 用户可见说明

## 4F — logcat 分析（可选，与 4E 并行）

需求详见 [adb-logcat-analysis.md](../../../docs/architecture/adb-logcat-analysis.md)。

- [ ] P0：`scripts/adb-logcat-capture` + 解析摘要；验收模板扩展
- [ ] P1：观测 tab「导入 logcat」SAF + 片段列表 + CodeEditor 详情
- [ ] P1b：（可选）root `logcat -d` 扫描

## 4G — 智能分析（backlog，依赖 4C 详情页）

需求详见 [crash-intelligent-analysis.md](../../../docs/architecture/crash-intelligent-analysis.md)。**不阻塞** 4B–4E；hook 路径禁止分析逻辑。

### 4G-MVP — 规则分类 + 模板建议

- [ ] `CrashEvent.analysis` schema + `RuleEngine`（`exceptionType` + `rootCauseTags`）
- [ ] `assets/crash_analysis/rules_v1.json` + i18n 模板
- [ ] 模块进程 `AnalysisWorker`：ingest 或首次打开详情时 lazy 分析
- [ ] 详情页 `AnalysisCard`：分类 Chip + user/developer 建议 + 免责声明
- [ ] 单元测试：典型 stack → 预期 category/tags

### 4G-V2 — 签名聚类 + 统计扩展

- [ ] `signatureHash` / `clusterId` 规范化算法
- [ ] `StatsAggregator.topClusters()` / `topCategories()`
- [ ] 全局统计页「异常类别 TOP」「重复崩溃 TOP」
- [ ] （可选）`signatures.jsonl` 内置签名库

### 4G-V3 — 可选 LLM / PC 分析

- [ ] `scripts/analyze-crashes.py` 读导出 JSONL + Markdown 报告
- [ ] （可选）设置项：端侧 LLM 实验 — **须另立 ADR 后再做**
- [ ] logcat 导入事件走同一分析管道（对齐 adb-logcat-analysis P2）

---

## 验收标准

```bash
./gradlew :app:assembleDebug
./scripts/generate-docs-index.sh
python3 scripts/check-docs-health.py
```

**MVP（4B）**：Test 菜单触发崩溃后 `events.jsonl` 新增一行；禁用 `crash_log_enabled` 后不写入。

**P1+**：adb smoke + 手动：历史列表条数与拦截次数一致；统计页聚合正确；清空后文件/meta 一致。

---

## 相关文档

- [architecture-optimization.md](../../../docs/architecture/architecture-optimization.md) — 包结构、分层与 Phase 映射
- [navigation-ia.md](../../../docs/architecture/navigation-ia.md) — 壳内 Sheet vs 外部 Activity
- [ui-routing.md](../../../docs/architecture/ui-routing.md) — `crash_detail_sheet` / `crash_detail`
- [code-editor-porting.md](../../../docs/architecture/code-editor-porting.md) — Sheet vs Activity 双载体
- [crash-history-ui.md](../../../docs/architecture/crash-history-ui.md) — 历史 → `CrashDetailBottomSheet`
- [ADR-009](../../../docs/decisions/009-ui-shell-design-system.md)
- [crash-logging.md](../../../docs/architecture/crash-logging.md)
- [ADR-007](../../../docs/decisions/007-crash-log-cross-process-storage.md)
- [crash-log-backends.md](../../../docs/architecture/crash-log-backends.md) — 多后端编排
- [ADR-008](../../../docs/decisions/008-multi-backend-crash-log-storage.md)
- [framework-injection-feasibility.md](../../../docs/architecture/framework-injection-feasibility.md)
- [crash-intelligent-analysis.md](../../../docs/architecture/crash-intelligent-analysis.md) — 4G 智能分析 backlog
- [ipc-design-qa-2026-06-20.md](../../iterations/crash-observability/ipc-design-qa-2026-06-20.md)
- [crash-handler.md](../../../docs/architecture/crash-handler.md)
- [xposed-entry.md](../../../docs/architecture/xposed-entry.md)
