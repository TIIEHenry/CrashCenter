---
title: "组件：Item 对齐分割线"
type: concept
status: draft
phase: N/A
updated: 2026-06-19
summary: "Card / Sheet / Popup 共用的 item 对齐 divider 与 popup_section_gap 规范。"
---

# Item 对齐分割线

> 全局留白原则：[visual-language.md §留白与分割线](../visual-language.md#留白与分割线)
>
> **Web Demo**：[shared-dividers.html](../demos/shared-dividers.html)

**无边界扁平**：主阅读区（应用列表）靠留白 + 按压，**不用**全宽行间线。

**Card / Sheet / Popup** 用 **与 item 行内容列对齐** 的分割线——**不得**拉满外容器宽度。

## 原则

| 原则 | 规范 | 禁止 |
|------|------|------|
| **去全宽线** | 扁平 List、Toolbar↔内容：无 divider | `listDivider`、Toolbar 底线 |
| **item 对齐线** | divider 与行主文案 ↔ trailing 同宽 | 线与 Card/Sheet 外缘等宽 |
| **Popup 分组** | 组内可对齐 divider；功能区加 **`popup_section_gap`** | Master 行贴在上面 |
| **圆角容器 Item** | 首/末可点击行按压 clip 至容器圆角；容器 **无**额外上下 padding | 容器 `space_xs` 上下 inset；首/末行直角 overlay 溢出 |

## Token

| Token | 值 | 含义 |
|-------|-----|------|
| `divider_height` | **1dp** | 线粗 |
| `divider_color` | `stroke` | `#E0E0E0` / `#3D3D3D` |
| `divider_align_start` | 与 item 主文案左缘 | 无图标：`content_padding_horizontal` 16dp；有图标：**56dp** |
| `divider_start_leading` | **56dp** | 40dp 图标槽 + `content_padding_horizontal` |
| `divider_start_menu` | **40dp** | Popup 简单行：padding + icon 列 |
| `divider_start_menu_label` | **48dp** | Popup 筛选行：padding + icon + gap → **label 左缘** |
| `divider_end_menu_check` | **`content_padding_horizontal` 16dp** | Popup 筛选行：divider **右端** inset；线 **覆盖 check 列**至列右缘 |
| `divider_align_end` | 与 trailing 右缘 | **`content_padding_horizontal` 16dp** 右 inset |
| `popup_section_gap` | **12dp** | Master 行上缘 section 分割带 **下方** 间隔（`popup_section_divider { margin-bottom }`） |
| `popup_section_divider_height` | **8dp** | Master 行上缘 **全宽 section 分割带**（`layer` 底 + 顶 inset 线）；**层级高于**组内 1dp item-divider |

## 几何

```text
Card 外容器 |←────────── 全宽 ──────────→|
            | pad |← item 对齐 divider →| pad |
```

- divider **短于**外缘（左右 padding）
- **禁止** Material `Divider()` 拉满父容器

## 场景

| 场景 | 规则 |
|------|------|
| 设置 Card 行 | item 对齐；末行无线 |
| 详情 Sheet | 同左；见 [settings-card-detail-sheet.md](settings-card-detail-sheet.md) |
| Popup 组内 | 筛选：**label 列**对齐 divider（不贯穿 icon/check）；简单菜单 icon 列后 divider |
| Popup 功能区之间 | divider + **`popup_section_gap`** |
| Popup Master 行上缘 | **`popup_section_divider`（全宽）+ `popup_section_gap`**；见 [popup-filter-menu.md](popup-filter-menu.md) |
| 圆角 Card / Popup 首末行 | ① 按压 + 容器 `overflow: hidden`；见 [visual-language §圆角容器内 Item 栈](../visual-language.md#圆角容器内-item-栈) |

## `stroke` 用途

- Chip 描边、毛玻璃 `glass_stroke`、焦点环、**item 对齐 divider**
- **不得**：扁平 List 行间、Toolbar 底、拉满外容器

## 相关文档

- [settings-card-detail-sheet.md](settings-card-detail-sheet.md)
- [popup-filter-menu.md](popup-filter-menu.md)
- [toolbar-list-chrome.md](toolbar-list-chrome.md)
- [../visual-language.md §圆角容器内 Item 栈](../visual-language.md#圆角容器内-item-栈)
