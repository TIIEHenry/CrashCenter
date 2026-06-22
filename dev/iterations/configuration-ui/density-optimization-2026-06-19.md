---
title: "配置界面信息密度优化"
type: iteration
status: implemented
phase: 3
updated: 2026-06-19
summary: "单屏保留全局设置，压缩状态条/Chip/列表项，释放 RecyclerView 空间"
---

# 配置界面信息密度优化

> 子系统：配置 UI（`:app`）
> 架构依据：[configuration-ui.md](../../../docs/architecture/configuration-ui.md)、[ADR-005](../../../docs/decisions/005-settings-information-architecture.md)

## 背景

用户要求「在一个界面配置所有应用」，否决 Phase 3B 将设置迁出主屏的方向。需在 **不改变 prefs 语义** 前提下提高首屏信息密度。

## 改动摘要

| 区域 | 变更 |
|------|------|
| 状态条 | 双行 → 单行合并文案（`xposed_*_inline`），高度约 32dp |
| 全局设置 | 设置卡片 + Switch → 横向 FilterChip ×3 |
| scope 说明 | 卡片内 TextView → 长按「作用域」Chip 弹窗 |
| 搜索 | OutlinedBox → Dense 变体 |
| 过滤 | Chip 28dp；应用计数与过滤 Chip 同行右对齐 |
| 列表项 | 去 MaterialCardView；图标 48→36dp；扁平 ripple 行 |

## 涉及文件

- `activity_main.xml`、`activity_main_appitem.xml`
- `ActivityMain.kt`（`setupSettingsChips`、`updateXposedStatusBanner`）
- `dimens.xml`、`styles.xml`、`strings.xml` / `values-zh`

## 验证

- `./gradlew :app:assembleDebug`
- `./scripts/adb-smoke-verification.sh --skip-build`（设备在线时）

## 相关文档

- [phase3_ui_redesign.md](../../roadmap/active/phase3_ui_redesign.md)
- [usage.md](../../../docs/guides/usage.md)
