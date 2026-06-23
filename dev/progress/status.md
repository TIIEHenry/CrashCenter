---
title: "项目进度状态"
type: progress
status: active
phase: N/A
updated: 2026-06-23
summary: "4B-β RelayMerge、4D+ PerAppCrash、4G-V2 统计聚类 as-built；IS 矩阵待验"
---

# 项目进度状态

## Snapshot

| 项 | 状态 |
|---|------|
| 活跃 Phase | [Phase 3](../roadmap/active/phase3_ui_redesign.md) 🔄（LSPosed 手动 smoke + 3E M3 迁移） |
| 并行 Phase | [Phase 4](../roadmap/active/phase4_crash_observability.md) 🔄 **4B-β / 4D+ / 4G-V2 部分 as-built**；**IS 矩阵待验** |
| 阻塞 | LSPosed 手动项（Test 拦截、Switch→hook、激活状态条）；4B IS-1~IS-6 / IS-R1~IS-R5 真机矩阵 |
| 验证基线 | `461QYGDD2226C` **2026-06-19**：consolidated smoke **PASS**；dark mode Meizu **PASS** |
| 文档 | 2026-06-23 roadmap + 架构 as-built 同步；`docs/design/` accepted |

### 已完成

- Phase 1–2 归档；UI Shell（ADR-009）；Phase 3G 受管应用 + 干预规则
- **4B-α**：`CrashLogCoordinator` 多后端并行；`CrashLogProvider`；retention 默认 500/8MB
- **4B-β（部分）**：`RootSuBackend`、`RootFsBackend`、`RelayMergeBackend`（relay harvest + id dedupe）、`CrashLogIngestCoordinator`；`ingestedFrom` 字段
- **4C**：历史 Paging + `CrashDetailBottomSheet` + CodeEditor 双载体
- **4D（部分）**：`CrashStatsFragment` + `StatsAggregator`；Toolbar 清空/retention；**`PerAppCrashActivity`** 统计 TOP 下钻
- **4E（部分）**：SAF zip 导出 + 隐私提示；通知 Intent `crash_id`
- **4F（部分）**：`LogcatFragment` SAF 导入；`scripts/adb-logcat-capture.sh`
- **4G-MVP（部分）**：`RuleEngine` + `rules_v1.json` + 详情 lazy 分析卡片 + 单测
- **4G-V2（部分）**：`CrashSignature` + `topCategories()` / `topClusters()` + 统计页双 TOP 区块；`CrashEvent.analysis` JSONL 持久化 defer

### 待办

- **3A**：LSPosed 手动 smoke → 补全 `dev/verification/smoke_20260619.md`
- **4B**：IS 矩阵 → verification 报告 → ADR-017 accepted；Coordinator Phase 1 短窗（当前 RootSu 与 Phase 2 并行）
- **4B-γ**：`CanonicalJsonlStore` 统一读写锁；ADR-021 accepted
- **4D+**：配置 tab「崩溃记录」入口；统计页时间范围 Chip
- **4G**：`CrashEvent.analysis` schema、`AnalysisWorker` ingest 路径、规则 i18n
- **暗色**：AOSP 模拟器 API 30/34/36；CodeEditor `setDark(night)`

---

## Recent Sessions

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
