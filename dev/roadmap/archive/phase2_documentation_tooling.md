---
title: "Phase 2: 文档工具与验收体系"
type: roadmap
status: archived
phase: 2
updated: 2026-06-22
summary: "验收模板、adb smoke 脚本、Cursor 规则；已归档（首份 smoke 报告 smoke_20260619）"
---

# Phase 2: 文档工具与验收体系

## 背景

Phase 1 建立了 docs/dev 骨架与初始架构文档。本 Phase 补齐验收流程、Agent 规则与工具链，使文档系统可闭环运行。

**状态（2026-06-22）**：✅ 已归档 — 工具链、Cursor 规则、索引/健康检查脚本就绪；首份验收报告 [smoke_20260619.md](../../verification/smoke_20260619.md) 已写入（LSPosed 手动项在 Phase 3 验收中继续跟踪）。

## 目标

- [x] 创建 `dev/verification/README.md` 验收指南
- [x] 创建 `docs/templates/verification-template.md`
- [x] 创建 `scripts/adb-smoke-verification.sh`
- [x] 创建 `.cursor/rules/`（documentation-maintenance、architecture-review、loop-iteration）
- [x] 归档 Phase 1 → `dev/roadmap/archive/`
- [x] 更新索引脚本扫描 verification / design / iterations
- [x] 真机 smoke 验收并写入首份报告 → [smoke_20260619.md](../../verification/smoke_20260619.md)

## 验收标准

```bash
./scripts/generate-docs-index.sh
python3 scripts/check-docs-health.py --strict-links --strict-frontmatter
./scripts/adb-smoke-verification.sh --help
```

## 相关文档

- [dev/verification/README.md](../../verification/README.md)
- [docs/DOCUMENTATION.md](../../../docs/DOCUMENTATION.md)
