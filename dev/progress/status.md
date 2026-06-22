---
title: "项目进度状态"
type: progress
status: active
phase: N/A
updated: 2026-06-22
summary: "文档系统修复；Phase 2 归档；design/ accepted；sibling 参考 SSOT"
---

# 项目进度状态

## Snapshot

| 项 | 状态 |
|---|------|
| 活跃 Phase | [Phase 3](../roadmap/active/phase3_ui_redesign.md) 🔄（LSPosed 手动 smoke 收尾） |
| 下一 Phase | [Phase 4](../roadmap/active/phase4_crash_observability.md) 🔄 **4B-α + 4C-β as-built**；**ADR-017/021** proposed；IS 矩阵待验 |
| 阻塞 | LSPosed 手动项（Test 拦截、Switch→hook、激活状态条）；4B IS-1~IS-6 写路径 |
| 验证基线 | `461QYGDD2226C` **2026-06-19**：consolidated smoke **PASS**；dark mode Meizu **PASS** |
| 文档 | Phase 2 归档；`docs/design/` accepted；[sibling-projects.md](../../docs/reference/sibling-projects.md)；CI `--strict-links` |

### 已完成

- Phase 1–2 归档；文档索引扩展 design/ + iterations/；健康检查校验 type/status 枚举
- Material v1 + 3D 结构清理 + Fluent 对齐 + 单屏密度 IA + 包可见性/迁移/更名
- **UI Shell**：ADR-009；`MainShellActivity` 2-tab；Phase 4C Design System 组件类化
- **Phase 4C-β**：`CrashHistoryFragment` + `FileCrashLogRepository` + Paging
- **Phase 4B-α**：`CrashLogCoordinator` + `DirectFsCrashLogWriter`；ADR-013 已实施
- **Repository**：`deleteById` / `clear`；`CrashDetailArgs`；`CrashDetailLoader.titleFromStackTrace`

### 待办

- **3A**：LSPosed 手动 smoke → 补全 `dev/verification/smoke_20260619.md`
- **4B-β**：ADR-017 accepted + RootSu ingest；**IS-1~IS-6** 真机矩阵
- **4D**：StatsAggregator、观测统计 tab、Toolbar 清空 UI（API 已有，retention 待 ADR-019）
- **暗色**：AOSP 模拟器 API 30/34/36；CodeEditor `setDark(night)`

---

## Recent Sessions

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
