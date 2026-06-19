---
title: "AppSnapShotor 视觉风格对齐"
type: plan
status: implemented
phase: 3
updated: 2026-06-19
summary: "配置 UI 对齐 AppSnapShotor Fluent 设计语言：色板、扁平 Toolbar、Chip/搜索/列表样式"
---

# AppSnapShotor 视觉风格对齐

> 参考项目：`/home/clarence/Projects/Android/AppSnapShotor`
> 架构依据：[configuration-ui.md](../../../docs/architecture/configuration-ui.md)

## 背景

CrashCenter 配置 UI 已完成 Material 化与单屏密度优化，但视觉语言与 sibling 项目 AppSnapShotor 不一致。用户要求 UI 样式参考 AppSnapShotor，仅借用视觉设计，不引入截图等业务功能。

## 自 AppSnapShotor 采纳的元素

| 类别 | 采纳内容 |
|------|----------|
| 色板 | Fluent Communication Blue `#0078D4`、canvas `#FAFAFA`、layer `#FFFFFF`、文本 `#242424` / `#616161` |
| 圆角 | Control 4dp、Overlay 8dp（`shapes.xml`） |
| Toolbar | 白底 surface + 1dp 分隔线，无彩色 AppBar；17sp medium 标题 |
| 状态栏 | 透明 + edge-to-edge inset（对齐 AppSnapShotor `setupSystemBars`） |
| Chip | 1dp 描边、选中 primary 边框 + container 底、13sp、32dp 高 |
| 搜索 | Outlined Dense、4dp 圆角、`AppTheme.SearchField` |
| Switch | primary 轨道/描边色（`switch_*_color` selector） |
| 列表项 | `selectableItemBackground` 扁平行、15sp bold 名称、12sp 包名 |
| 状态条 | Fluent 语义色（success `#107C10`、warning `#8A6116`）+ 4dp 圆角 |
| 间距 | filter 区 12dp 水平内边距、8dp 区块间距 |

## 刻意保留的 CrashCenter 差异

| 项 | 原因 |
|----|------|
| 单屏 FilterChip 全局设置 | ADR-005 用户要求 |
| 36dp 紧凑列表图标 | 单屏信息密度策略 |
| Material Components 1.6 主题 | ADR-006 M3 升级 defer；Fluent token 映射到 M2 API |
| Xposed 状态条 | CrashCenter 特有，AppSnapShotor 无对应组件 |
| per-app Switch（非 Checkbox） | 现有交互与 prefs 语义 |

## 改动文件

- `values/colors.xml`、`dimens.xml`、`shapes.xml`、`styles.xml`
- `color/chip_*.xml`、`color/switch_*.xml`
- `layout/activity_main.xml`、`activity_main_appitem.xml`、`activity_crashinfo.xml`
- `drawable/bg_status_*.xml`、`bg_system_badge.xml`、`ic_shield_*.xml`

## 验证

- `./gradlew :app:assembleDebug`
- `./scripts/adb-smoke-verification.sh --skip-build`（adb 在线时）

## 相关文档

- [configuration-ui.md](../../../docs/architecture/configuration-ui.md)
- [density-optimization-2026-06-19.md](density-optimization-2026-06-19.md)
- [material-ui-redesign-2026-06-19.md](material-ui-redesign-2026-06-19.md)
