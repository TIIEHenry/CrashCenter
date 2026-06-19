---
title: "组件：Toolbar 与扁平列表"
type: concept
status: draft
phase: N/A
updated: 2026-06-19
summary: "共面 Toolbar、扁平 List 行、Banner、筛选行的视觉与按压规范。"
---

# Toolbar 与扁平列表

> **Web Demo**：[toolbar-list-chrome.html](../demos/toolbar-list-chrome.html)

## Toolbar

| 属性 | 规范 |
|------|------|
| 高度 | **`toolbar_height` 48dp** |
| 背景 | 与内容 **共面** 同色 `layer` |
| 分割线 | **无**底部分割 View |
| 标题 | **`type_toolbar_title`** — 17sp **Medium（500）**；禁止 SemiBold/600 |
| 水平 padding | 左右均为 **`content_padding_horizontal` 16dp**（标题与 overflow 同列对齐） |
| Overflow 按钮 | 视觉 **32dp 正圆** + 24dp glyph；**② 圆形 ripple** |
| 排序/About 等 | Compact 时 P2+ 进 ⋮ overflow（32dp 正圆；Android `shell_ripple_icon`） |
| **全部 Toolbar icon** | 触区须 **正圆**（等宽等高 + `border-radius: 50%`）；按压 **② ripple**；**禁止** ③ 方盒填色 |

### CenterTitleToolbar（居中标题 · 带 Navigation）

用于 **全屏子页**（含 [Draggable Half Sheet](draggable-half-sheet.md) Full 态 morph）：

| 属性 | 规范 |
|------|------|
| 布局 | **`←` 返回**（左）· **title 居中** · trail 或 **32dp spacer**（右，保持标题几何居中） |
| 返回 | `icon_button_size_compact` 32dp **正圆**；**② 圆形 ripple**；语义 **Up / 上一级** |
| 与 Half Sheet | Full 态显示；Half 态用 DragHandle + 半屏行（✕ 关闭） |

## 扁平 List / RecyclerView

| 属性 | 规范 |
|------|------|
| 行底 | **透明** |
| 行间 divider | **无**（应用列表、会话列表） |
| Padding | `item_row_padding_vertical` **12dp**；horizontal **`content_padding_horizontal` 16dp** |
| 文案 | 主/副标题 **margin: 0**；靠行 padding 定界，禁止文字级 horizontal margin |
| 最小视觉高度 | `list_row_min_height` 36dp + padding → ≥40dp 触区 |
| 选中 | overlay / 字重；**非**全宽 ripple |
| 按压 | **① iOS overlay**（`press_overlay` ~8–12%；**禁止 scale**） |

## 筛选 Chip 行

- 独立一行 `filter_row_height` 32dp
- 与 Toolbar / Banner 靠 `space_sm` / `space_md` 间距；与列表之间 **无** hairline，靠 `space_md` 间距
- Chip 行为见 [form-controls.md](form-controls.md)

## Banner / 状态条

- 语义色块即分区（`warning_container` / `warning_on_container`）；上下 `space_sm`；左右 `space_lg`
- 无 stroke 外框

## 视觉深度（主屏）

```text
canvas
├── layer 共面区：Toolbar + 筛选 + 列表（无 divider 切分）
└── 悬浮层：底栏 / Popup（见 floating-chrome、popup-filter-menu）
```

## 列表行交互（跨项目）

| 项目 | 行点击 | 次要控件 |
|------|--------|----------|
| **CrashCenter** | 整行 toggle + 写 prefs | Switch `clickable=false` |
| **AppSnapShotor** | 跳转详情 | Fragment 自有 |
| **UniverseAgent** | Session primary + 上下文菜单 | hover / 内联关闭 |

## 相关文档

- [shared-dividers.md](shared-dividers.md)
- [form-controls.md](form-controls.md)
- [../interaction-language.md §响应式布局](../interaction-language.md#响应式布局与功能密度)
