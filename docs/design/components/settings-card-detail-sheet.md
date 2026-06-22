---
title: "组件：设置 Card 与详情 Sheet"
type: concept
status: accepted
phase: N/A
updated: 2026-06-22
summary: "卡片式设置、相册式详情 BottomSheet——实色 layer、item 对齐 divider、内容 pill。"
---

# 设置 Card 与详情 Sheet

> 分割线：[shared-dividers.md](shared-dividers.md) · 圆角 token：[visual-language.md §圆角](../visual-language.md#圆角平台分档--内容)
>
> **Web Demo**：[settings-card-detail-sheet.html](../demos/settings-card-detail-sheet.html)

**材质**：**实色 `layer`** + scrim；**非**毛玻璃 Sheet 内容。

## 详情 Sheet（相册信息面板 · 参考）

TouchPrimary **元数据 / 详情 / 关于** 类 BottomSheet：

```text
[ scrim ]
┌─ radius_mobile_sheet_detail (16dp) ────┐
│  标题区（时间、文件名）                    │
│  ┌ pill: ISO · 快门 · 光圈 ┐  ← radius_content_pill
│  位置 / 设备信息（label + value 行）       │
│  ─── item 对齐 divider ───              │
│  存储路径 / 相册来源                       │
│  ─── divider ───                         │
└──────────────────────────────────────────┘
  [ 底栏操作 icon row ]
```

| 吸收 | 不吸收 |
|------|--------|
| 实色 layer + scrim | Sheet 毛玻璃 |
| item 对齐 divider | 全宽分割线 |
| 行内 metadata **pill**（内容圆角） | 整页 28dp 药丸 Modal |
| 顶缘 **`radius_mobile_sheet_detail` 16dp** | Picker 用 **`radius_mobile_sheet` 28dp** |

## 卡片式设置

```text
canvas
├── section 标题（type_label，可选）
├── Card（layer，radius_mobile_card 16dp）
│     ├── Row（开关 / 导航）
│     ├── ─── item 对齐 divider ───
│     ├── Row
│     └── Row（末行无线）
├── section_gap
└── Card（下一组）
```

- Card 与 canvas 色差或外距 `space_lg`；Card 可无 stroke
- 行按压 **① iOS**；divider 规则同 [shared-dividers.md](shared-dividers.md)
- Card **上下 padding 0**；首/末行贴容器顶/底缘，按压 overlay **适配** `radius_mobile_card` — 见 [visual-language §圆角容器内 Item 栈](../visual-language.md#圆角容器内-item-栈)

## Token 速查

| Token | 值 |
|-------|-----|
| `radius_mobile_card` | 16dp |
| `radius_mobile_sheet_detail` | 16dp |
| `radius_content_pill` | 全高/2（行内 metadata） |
| `section_gap` | 24dp |

## 相关文档

- [popup-filter-menu.md](popup-filter-menu.md) — 筛选 Popup（非 Sheet）
- [../interaction-language.md §平台原生浮层](../interaction-language.md#平台原生浮层外观)
