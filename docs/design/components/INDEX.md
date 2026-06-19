---
title: "公共组件标准"
type: concept
status: draft
phase: N/A
updated: 2026-06-19
summary: "Clarence 生态可复用 UI 组件 catalog——分割线、列表、Sheet、Popup、悬浮 chrome、表单控件；含 Web demo。"
---

# 公共组件标准

> Token 与全局原则：[visual-language.md](../visual-language.md) · UI 模式：[ui-modes.md](../ui-modes.md) · 交互：[interaction-language.md](../interaction-language.md)
>
> **Web Demo**：[demos/INDEX.md](../demos/INDEX.md) · 本地 `cd docs/design/demos && python3 -m http.server 8765`

## UI 模式 → 组件

| 模式 | 优先组件 |
|------|----------|
| **信息流** | floating-chrome、popup-filter-menu、settings-card-detail-sheet、draggable-half-sheet、selection-bubble |
| **工具密度** | toolbar-list-chrome、form-controls（密集 Chip/Action） |
| **Hybrid** | 分区：主区用左列，顶栏/Chip 用右列；见 [ui-modes.md §同页交叉组合](../ui-modes.md#同页交叉组合hybrid) |

## 组件清单

| 组件 | 文档 | Demo | 视觉要点 | 交互要点 |
|------|------|------|----------|----------|
| **Item 对齐分割线** | [shared-dividers.md](shared-dividers.md) | [▶](../demos/shared-dividers.html) | `divider_*`、`popup_section_gap` | 禁止全宽 divider |
| **Toolbar / 扁平列表** | [toolbar-list-chrome.md](toolbar-list-chrome.md) | [▶](../demos/toolbar-list-chrome.html) | 共面 48dp、无底线 | 行 ① iOS；Switch 行内 |
| **设置 Card / 详情 Sheet** | [settings-card-detail-sheet.md](settings-card-detail-sheet.md) | [▶](../demos/settings-card-detail-sheet.html) | 实色 layer、16dp 顶缘 | metadata pill |
| **可拖曳半屏 Sheet** | [draggable-half-sheet.md](draggable-half-sheet.md) | [▶](../demos/draggable-half-sheet.html) | 28dp 顶缘、chrome 内把手 | 把手拖曳 expand/dismiss |
| **筛选 Popup / 菜单** | [popup-filter-menu.md](popup-filter-menu.md) | [▶](../demos/popup-filter-menu.html) | 16dp、`layer` 实底 | 按住滑动选单 |
| **悬浮 Toolbar** | [floating-chrome.md](floating-chrome.md) | [▶](../demos/floating-chrome.html) | glass、Scrim、pill | Unified / Split / Scrim |
| **选区悬浮气泡** | [selection-bubble.md](selection-bubble.md) | [▶](../demos/selection-bubble.html) | pill 实色、intrinsic 宽 | 选区/长按锚定 |
| **Chip / 搜索 / Switch** | [form-controls.md](form-controls.md) | [▶](../demos/form-controls.html) | `radius_control` | 即时过滤 |

## 选型速查

```text
需要结构分组线？
  → shared-dividers（Card/Sheet/Popup 共用 item 对齐规则）

主阅读区？
  → toolbar-list-chrome（扁平 List 无线）

设置 / 关于 / 元数据？
  → settings-card-detail-sheet

半屏任务 / 可拖曳列表子页？
  → draggable-half-sheet

筛选 / overflow / 上下文多选？
  → popup-filter-menu（含 Master 行）

底栏 / 顶角悬浮 / 滚动 Scrim？
  → floating-chrome（Unified / Split / IconNav / TopActions / ScrollLinkedEdgeScrim）

文本选区 / 长按文字 / AI 快捷条？
  → selection-bubble

页内年月季 / 退出临时模式？
  → floating-chrome · SplitToolbarCluster

筛选 Chip / 搜索框？
  → form-controls
```

## 相册 App 正向参考映射

| 界面 | 组件 doc |
|------|----------|
| 详情 metadata Sheet | settings-card-detail-sheet |
| 半屏可拖任务 Sheet | draggable-half-sheet |
| FAB / Trail 筛选 Popup | popup-filter-menu |
| 年/月/日 + × 双件底栏 | floating-chrome · **SplitCluster** |
| 照片 \| 图集 + 网格/筛选 统一底栏 | floating-chrome · **UnifiedCapsule** |
| 顶 blur 渐变 / 底 fade（随滚动） | floating-chrome · **ScrollLinkedEdgeScrim** |
| 右上搜索 + ⋮ | floating-chrome · **FloatingTopActions** |

## 相关文档

- [../INDEX.md](../INDEX.md) — 设计文档总索引
- [../visual-language.md](../visual-language.md)
- [../demos/INDEX.md](../demos/INDEX.md) — Web Demo 索引
