---
title: "项目进度状态"
type: progress
status: active
phase: N/A
updated: 2026-06-24
summary: "ADR-024 分布式 cache 存储 as-built；Phase 5 监测/拦截"
---

# 项目进度状态

## Snapshot

| 项 | 状态 |
|---|------|
| 活跃 Phase | [Phase 3](../roadmap/active/phase3_ui_redesign.md) 🔄（LSPosed 手动 smoke + 3E M3） |
| 并行 Phase | [Phase 4](../roadmap/active/phase4_crash_observability.md) 🔄 **IS 矩阵待验**；[Phase 5](../roadmap/active/phase5_observe_intercept_split.md) 🔄 **5.1–5.3 as-built** |
| 阻塞 | LSPosed 手动 smoke（observe + intercept）；4B IS-D1~IS-D5 真机矩阵 |
| 验证基线 | `461QYGDD2226C` **2026-06-19**：consolidated smoke **PASS**；dark mode Meizu **PASS** |
| 文档 | 2026-06-24 ADR-024 as-built + 4F-ANR + logcat Paging3 |

### 已完成

- Phase 1–2 归档；UI Shell（ADR-009）；Phase 3G 受管应用 + 干预规则（**2026-06-24 收敛为全量列表 + managed_packages**）
- **4B-α**：`CrashLogCoordinator` 多后端并行；`CrashLogProvider`；retention 默认 500/8MB
- **4B-δ（as-built）**：[ADR-024](../../docs/decisions/024-distributed-cache-crash-storage.md) 分布式 `cache` 存储；`LocalCacheBackend` hook 写；`DistributedCrashLogRepository` root 聚合读；移除 Provider/canonical/ingest
- **4C**：历史 Paging + `CrashDetailBottomSheet` + CodeEditor 双载体
- **4D（部分）**：`CrashStatsFragment` + `StatsAggregator`；Toolbar 清空/retention；**`PerAppCrashActivity`** 统计 TOP 下钻
- **4E（部分）**：SAF zip 导出 + 隐私提示；通知 Intent `crash_id`
- **4F（部分）**：`LogcatFragment` SAF 导入；`scripts/adb-logcat-capture.sh`；root 多 buffer + **`LogcatParser.isAnrHint`（4F-ANR）**
- **4G-MVP（部分）**：`RuleEngine` + `rules_v1.json` + 详情 lazy 分析卡片 + 单测
- **4G-V2（部分）**：`CrashSignature` + `topCategories()` / `topClusters()` + 统计页双 TOP 区块；`CrashEvent.analysis` JSONL 持久化 defer
- **5（部分）**：[ADR-023](../../docs/decisions/023-injection-observe-intercept-split.md)：`ScopeDecision` install/intercept 分离；`CrashHandler` 双模式；观测 `logSync`

### 待办

- **5**：UI 文案 + 文档 as-built ✅；LSPosed observe+intercept smoke 待验
- **4B**：IS-D1~IS-D5 真机矩阵（取代 IS-1~IS-6 canonical 路径）
- **4B-γ**：`CanonicalJsonlStore` 统一读写锁；ADR-021 accepted
- **4D+**：配置 tab「崩溃记录」入口；统计页时间范围 Chip
- **4G**：`CrashEvent.analysis` schema、`AnalysisWorker` ingest 路径、规则 i18n
- **暗色**：AOSP 模拟器 API 30/34/36；CodeEditor `setDark(night)`

---

## Recent Sessions

### 2026-06-24 — 双渠道发布文档 SSOT

- **guides/release.md**：GitHub + LSPosed 完整流程、版本约定、故障排除
- **guides/xposed-module-repo.md**：精简为模块仓元数据与文案
- **dev/archive/release-v1.0.0.md**：v1.0.0 一次性清单归档

### 2026-06-24 — v1.0.0 发布执行

- **版本**：`versionCode 1` / `versionName 1.0.0`；`CHANGELOG.md` 首个公开发布段落
- **产物**：`CrashCenter_v1.0.0_release.apk`；`release/xposed-release-notes-1.0.0.md`
- **模块仓**：`nota.android.crash.xp.app` 元数据 push；Release tag `1-1.0.0`
- **主仓**：push `v1.0.0` tag → GitHub Actions Release

### 2026-06-24 — 文档 as-built 同步 + 提交

- **架构**：`crash-log-backends.md` 4B-δ 全文；`crash-log-distributed-storage.md` 实施勾选；`code-editor-porting` 移除 CodeEditorAntlr
- **验收**：`verification/README` IS-D1~IS-D5 取代 IS-1~IS-6
- **roadmap**：phase4 4B-δ 文档项勾选
- **验证**：`generate-docs-index.sh` + `check-docs-health.py` + `:app:assembleDebug` + `:app:testDebugUnitTest`

### 2026-06-24 — ADR-024 分布式 cache 崩溃存储（编码）

- **写**：`LocalCacheBackend` → 各 app `cache/crash_logs/events.jsonl`；`CrashLogCoordinator` 单后端
- **读**：`DistributedCrashLogRepository` root 扫描聚合；无 root 返回空
- **移除**：`CrashLogProvider`、`DirectFs`/`Relay`/`RootSu`/`RelayMerge`/`IngestCoordinator`
- **迁移**：`CrashLogMigrationCoordinator` + `distributed_cache_migrated`
- **验证**：`:app:assembleDebug` + `:app:testDebugUnitTest` OK；审查修复 retention/迁移/指纹/空态

### 2026-06-24 — 4F-ANR UI 过滤 + ANR 规则补全

- **编码**：`chipCrashOnly` + `PREF_LOGCAT_CRASH_FILTER_DEFAULT`；`loadFromRoot`/`loadFromText` 应用 `filterCrashRelated`；`Input dispatching timed out`；列表 `ANR ·` 前缀
- **测试**：`LogcatViewModelTest` ANR + `crashOnly`；`LogcatParserTest` 更新
- **验证**：`:app:testDebugUnitTest` + `:app:assembleDebug`

### 2026-06-24 — 4F-ANR 实施（LogcatParser ANR_HINT）

- **编码**：`LogcatParser.isAnrHint` / `filterCrashRelated`；`LogcatParserTest` ANR 用例
- **文档**：`anr-observation` as-built；phase4 4F-ANR 勾选；`anr_logcat_l7_template.md`；`unified-root-service` minSdk 26
- **验证**：`:app:testDebugUnitTest`（LogcatParserTest）；`:app:assembleDebug`

### 2026-06-24 — ADR-025 评审后文档补齐

- **新建**：[anr-observation.md](../../docs/architecture/anr-observation.md)（路径 A/B、INTERCEPT 表、旁路 schema）
- **修订**：ADR-025 路径命名；`adb-logcat-analysis` `ANR_HINT`；`logcat-multi-source` → accepted；phase4 **4F-ANR** 任务；`crash-intelligent-analysis` RuleEngine 澄清
- **验证**：`generate-docs-index.sh` + `check-docs-health.py`

### 2026-06-24 — ADR-025：ANR 观测不走 Framework hook

- **决策**：[ADR-025](../../docs/decisions/025-anr-observation-no-framework-hook.md) accepted；否决 `AnrHelper` / system_server ANR hook 与 Framework 抑制 ANR
- **采纳路径**：logcat P2/P3（`system` / `events`）、可选 `ApplicationExitInfo`；`events.jsonl` 仍为 Java crash SSOT
- **文档**：`framework-injection-feasibility` 用例 6、`crash-logging`、`logcat-multi-source`、`glossary`

### 2026-06-24 — 监测/拦截术语与 CrashEvent.intercepted

- **数据**：`CrashEvent.intercepted` 必填；缺字段 JSONL 行丢弃；列表始终显示监测/拦截角标
- **UI**：历史/单应用列表角标「已拦截」「仅监测」；文案「监测」取代误用的「拦截」（统计/空态/tab）
- **验证**：`:app:assembleDebug` + 相关单测

### 2026-06-24 — 移除 legacy 兼容 + 配置单轨化

- **代码**：删除 `PrefMigrator`、`LegacyPrefSnapshotReader`、`ManagedModelMigrator`、`package_list` / `intervention_rules` 模型；`ConfigViewModel` + `ManagedAppRepository` 单轨；`ScopePolicy` 仅读 `managed_packages`
- **UI**：全量已安装应用列表；Switch 写拦截集；观测子页 ViewPager2 / 单应用崩溃页布局调整
- **文档**：`scope-and-prefs`、`app-management-ui`、`glossary`、`usage`、ADR-014 superseded
- **验证**：`:app:assembleDebug` + `:app:testDebugUnitTest` OK

### 2026-06-23 — 观测菜单去重 + 空态 CTA

- **菜单**：移除 Toolbar「统计」（与子 tab 重复）；`CrashHistoryMenuActions` 提取；`ObserveHostFragment` 按历史/统计/logcat 显隐菜单项
- **空态**：历史「去配置」、统计「查看历史」；配置 tab 空列表时隐藏 Toolbar「添加应用」
- **验证**：`:app:compileDebugKotlin` OK（Gradle daemon OOM 时 Studio build 通过）

### 2026-06-23 — Phase 5 文档/UI 同步（ADR-023 实施后检查）

- **UI**：`managed_status_*` / 筛选 Chip →「仅观测」「已拦截」；空态与移除确认文案
- **文档**：`usage`、`scope-and-prefs`、`app-management-ui`、`crash-handler`、`crash-capture-pipeline`、`glossary`

### 2026-06-23 — Phase 5 ADR-023 观测/拦截分离（编码）

- **方案**：`injection-observe-intercept-split.md` + ADR-023 accepted；ADR-015 hook 门控 superseded
- **编码**：`ScopePolicy` 全量 `shouldInstall`；Switch → `shouldIntercept`；`CrashHandler.install(INTERCEPT|OBSERVE)`；`CrashLogCoordinator.logSync`
- **验证**：`ScopePolicyTest` / `CrashCapturePipelineTest`；`:app:assembleDebug` OK

### 2026-06-23 — 4B-β / 4D+ / 4G-V2 编码 + 文档 as-built

- **编码**：`RelayMergeBackend`（relay harvest + id dedupe）；`PerAppCrashActivity`；`CrashSignature` + 统计页类别/重复崩溃 TOP
- **roadmap**：勾选 4B-β relay merge、4D+ 单应用页、4G-V2 聚类统计
- **架构**：`crash-log-backends`、`crash-logging`、`crash-stats-ui`、`crash-intelligent-analysis`、`ui-routing`、`usage.md`

### 2026-06-23 — Phase 4 文档 as-built 同步（早）

- **roadmap**：`phase4_crash_observability.md` 勾选 4D/4E/4F/4G-MVP/4B-β 已实现项
- **架构**：`crash-history-ui`、`navigation-ia`、`architecture-optimization` as-built 段落

### 2026-06-22 — 文档系统修复与过时内容清理

- **索引**：`generate-docs-index.sh` 纳入 `docs/design/`、`dev/iterations/`；`docs/README` 增加设计入口
- **链接**：新建 [sibling-projects.md](../../docs/reference/sibling-projects.md)；移除本机绝对路径与跨仓库相对链接
- **规范**：DOC-SPEC 增加 `type: iteration`、`status: proposed`；`check-docs-health.py` 校验枚举
- **CI**：`build.yml` 增加 `--strict-links --strict-frontmatter`
- **归档**：Phase 2 → `roadmap/archive/`；`ui-redesign-execution-plan` → `dev/archive/`
- **design/**：14 篇 `draft` → `accepted`（与 design-system.md 对齐）

### 2026-06-20 — Repository clear/delete + CrashDetail 详情重构

- **实现**：`CrashLogRepository.deleteById/clear`；`CrashDetailArgs`；删 `CrashHistoryAdapter`
- **测试**：`FileCrashLogRepositoryTest` +7；`:app:assembleDebug` OK
- **文档**：crash-data-layer as-built 同步

### 2026-06-20 — 架构文档 as-built 同步 + ADR-017 proposed

- **文档**：ui-routing / crash-data-layer / crash-history-ui；新建 app-di-and-module-boundaries、ADR-017
- **下一步**：LSPosed IS-1~IS-6 → verification 报告 → ADR-017 accepted → 4B-β 编码

---

更早会话见 [archive/status-sessions-2026-06.md](archive/status-sessions-2026-06.md)。

## 相关

- [roadmap/INDEX.md](../roadmap/INDEX.md)
- [docs/INDEX.md](../../docs/INDEX.md)
- [verification/README.md](../verification/README.md)
