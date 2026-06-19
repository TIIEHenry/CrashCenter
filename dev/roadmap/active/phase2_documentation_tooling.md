---
title: "Phase 2: 文档工具与验收体系"
type: roadmap
status: in_progress
phase: 2
updated: 2026-06-19
summary: "验收模板、adb smoke 脚本、Cursor 规则；近完成，待 LSPosed 真机报告"
---

# Phase 2: 文档工具与验收体系

## 背景

Phase 1 建立了 docs/dev 骨架与初始架构文档。本 Phase 补齐验收流程、Agent 规则与工具链，使文档系统可闭环运行。

**状态（2026-06-19）**：近完成 — 工具链与脚本已就绪，仅剩 LSPosed 环境真机 smoke 报告一项。

## 目标

- [x] 创建 `dev/verification/README.md` 验收指南
- [x] 创建 `docs/templates/verification-template.md`
- [x] 创建 `scripts/adb-smoke-verification.sh`
- [x] 创建 `.cursor/rules/`（documentation-maintenance、architecture-review、loop-iteration）
- [x] 归档 Phase 1 → `dev/roadmap/archive/`
- [x] 更新索引脚本扫描 verification
- [ ] 真机 smoke 验收并写入首份报告（需 adb + LSPosed 环境）

## 验收标准

```bash
./scripts/generate-docs-index.sh
python3 scripts/check-docs-health.py
./scripts/adb-smoke-verification.sh --help
# 真机（可选）:
# ./scripts/adb-smoke-verification.sh -s <serial>
# 报告 → dev/verification/smoke_YYYYMMDD.md
```

## 相关文档

- [dev/verification/README.md](../../verification/README.md)
- [docs/DOCUMENTATION.md](../../../docs/DOCUMENTATION.md)
