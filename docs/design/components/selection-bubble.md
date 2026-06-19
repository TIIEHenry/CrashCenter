---
title: "组件：选区悬浮气泡（SelectionBubble）"
type: concept
status: draft
phase: N/A
updated: 2026-06-19
summary: "文本选择 / 长按文本时出现的水平 pill 悬浮条——快捷动作、可选 inline 输入；实色 layer、内容自适应宽。"
---

# 选区悬浮气泡（SelectionBubble）

> 交互触发：[interaction-language.md §手势等价](../interaction-language.md#手势等价跨端) · 圆角容器 Item 栈：[visual-language.md §圆角容器内 Item 栈](../visual-language.md#圆角容器内-item-栈) · Popup 策略：[interaction-language.md §Popup](../interaction-language.md#popup触发方式与-pc-模式)
>
> **Web Demo**：[selection-bubble.html](../demos/selection-bubble.html)

**用途**：用户在 **选中文本** 或 **长按文本** 后，于选区附近浮现的 **水平胶囊工具条**。典型场景：AI 搜索 / 解释 / 翻译、复制、分享，以及 **inline 追问输入**（如「问问豆包」）。

与 [floating-chrome.md](floating-chrome.md)（页面级底栏 glass Toolbar）和 [popup-filter-menu.md](popup-filter-menu.md)（垂直 anchored 菜单）**不同层级**——本组件 **锚定选区矩形**，随选区移动/消失。

---

## 组件结构

```text
SelectionBubble（水平 pill 外轨）
├── BubbleDragHandle（可选）     ← 拖移整条；③ 形状按压
├── BubbleAvatar（可选）         ← 品牌 / 助手头像；正圆
├── BubbleActionStrip            ← 快捷动作区（inline 多项）
│   └── BubbleActionItem × N     ← icon + label；① iOS 按压
├── BubbleExpand（可选）         ← 展开更多；正圆 ② ripple
├── BubbleDivider（竖线）        ← 1dp；不交互
└── BubbleInputSlot（可选）      ← 圆角输入 + 提交；末段贴右圆角
```

参考布局（相册 / 助手选区条）：

```text
╭──╮ ┌──┐  🔍 AI搜索  📖 解释  🌐 翻译  ∨ │ ╭────────问问豆包──↑─╮
│‖‖│ │🧑│                                              ╰──────────────╯
╰──╯ └──┘
拖移  头像    ←── action strip（intrinsic 宽）──→  扩  │  inline 输入
```

---

## 行内宽度（所有 Item 共用规格）

Popup 的 **垂直列轨** 规则在此变为 **水平 intrinsic 宽**：**不设固定外轨宽**；外轨 = 各段固定 token + **动作 label 最大 intrinsic 宽** + 输入区 intrinsic 宽。

| 区段 | Token / 规则 | 说明 |
|------|----------------|------|
| **外轨高** | `selection_bubble_height` **44dp** | 统一高度 |
| **外轨圆角** | `radius_selection_bubble` **H/2** | 全圆 pill |
| **左右 inset** | `selection_bubble_inset_h` **4dp** | 外轨内缘；**无额外上下 padding** |
| **拖移把手** | `selection_bubble_drag_width` **20dp** | 双竖线 glyph；可选 |
| **头像** | `selection_bubble_avatar_size` **28dp** | 正圆；与 actions 间距 `space_sm` |
| **动作项** | `bubble_action_padding_h` **8dp** · `bubble_action_gap` **4dp** | icon `icon_glyph_size_compact` 18dp + label 13sp |
| **动作区间距** | `selection_bubble_action_gap` **2dp** | item 之间 |
| **展开钮** | `icon_button_size_compact` **32dp** | 正圆 ② ripple |
| **竖分割线** | `divider_height` 1dp × `selection_bubble_divider_height` **24dp** | 左右 `space_sm` margin |
| **输入槽** | `selection_bubble_input_min_width` **120dp** · 高 **32dp** | 内圆角 `radius_mobile_control` 8dp；placeholder + 提交 icon |

```text
bubble_width = inset×2 + drag? + avatar? + Σ(action_intrinsic) + expand? + divider + input_intrinsic
```

| 禁止 | 原因 |
|------|------|
| 固定外轨宽（如 360dp） | 短动作组被拉宽；输入框与动作区比例失调 |
| 动作项 `flex:1` 均分 | 破坏 intrinsic 宽；label 长短不一视觉跳动 |
| 未选时移除 check/输入占位 | 选区条宽度随状态跳变 |

---

## 容器

| 属性 | TouchPrimary |
|------|----------------|
| 材质 | **`surface` 实色** + `stroke` 1dp + `elevation_whisper` 4dp |
| 毛玻璃 | **不用**（非页面级 chrome；选区条需高可读） |
| 宽度 | **`max-content`**；上限 `selection_bubble_max_width`（默认 **min(100vw−32dp, 480dp)**）触顶时 action label **ellipsis** |
| 首/末段按压 | ①/③ overlay **clip 至** `radius_selection_bubble`；见 [visual-language §圆角容器内 Item 栈](../visual-language.md#圆角容器内-item-栈) |
| 上下 padding | **0**；内容垂直居中于 `selection_bubble_height` |

PC PointerPrimary：同形态；触发见下。

---

## 锚定与生命周期

| 事件 | 行为 |
|------|------|
| **文本选中**（拖选柄 / 双击词） | 显示；锚于选区 **上方** 居中，间距 `selection_bubble_anchor_gap` **8dp** |
| **长按文本**（~450ms） | 扩展选区至词/段 + 显示（与系统选区行为对齐） |
| **PointerPrimary 右键**（选区内） | 同位置显示（手势等价） |
| **选区清空 / 点外** | 关闭 + 可选提交 pending 输入 |
| **滚动宿主** | 跟选区重算位置；选区滚出视口 → 关闭 |
| **视口翻转** | 上方空间不足 → 锚于选区 **下方** |

```text
        ╭── SelectionBubble ──╮
        │  AI搜索 · 解释 · …  │
        ╰─────────────────────╯
              ↑ anchor_gap 8dp
    ════选中文本══════════════
```

锚点：`PressCentered` 于选区 bounding rect 顶边中点（或底边 flip 后）。

---

## 子件交互

| 子件 | 按压 | 行为 |
|------|------|------|
| **DragHandle** | ③ clip | 拖移整条（P1）；demo 可仅视觉 |
| **BubbleActionItem** | ① iOS | 即时动作（搜索/解释/翻译…） |
| **Expand** | ② ripple | 展开 overflow 动作或二级 sheet |
| **InputSlot** | 输入框 focus | Enter / ↑ 提交；不冒泡关闭选区 |

动作 **≤3** 时常驻；更多 → **Expand** 或 overflow，禁止压缩触控热区。

---

## 与系统选区关系

| 吸收 | 不吸收 |
|------|--------|
| 系统选区高亮 + 拖柄 | 替换系统 Copy/Paste 条（可并存或接管） |
| 选区变化实时 reposition | 无选区时常驻（→ 用 floating-chrome） |
| `contentDescription` / 动作 label | 仅长按无选区的纯菜单（→ popup-filter-menu） |

---

## Token 速查

| Token | 值 |
|-------|-----|
| `selection_bubble_height` | 44dp |
| `radius_selection_bubble` | 22dp（H/2） |
| `selection_bubble_inset_h` | 4dp |
| `selection_bubble_anchor_gap` | 8dp |
| `selection_bubble_max_width` | min(100vw−32dp, 480dp) |
| `selection_bubble_avatar_size` | 28dp |
| `selection_bubble_drag_width` | 20dp |
| `selection_bubble_divider_height` | 24dp |
| `selection_bubble_input_min_width` | 120dp |
| `bubble_action_padding_h` | 8dp |
| `selection_bubble_action_gap` | 2dp |

---

## 实现出口

| 栈 | 出口 |
|----|------|
| Compose KMP | `SelectionBubbleToolbar` + `TextSelectionObserver` |
| Web | `.selection-bubble` + `Selection` API 锚定 |
| Android View | `ActionMode` 定制 / 浮动 `PopupWindow` anchored to selection |

---

## Spec 模板（选区条一节）

- **Tier**：Floating contextual（非 Dropdown 垂直菜单）
- **触发**：选区变化 / 长按文本 / PC 右键选区
- **宽度**：intrinsic；动作 strip + 可选 input
- **材质**：实色 layer；非 glass

## 相关文档

- [floating-chrome.md](floating-chrome.md) — 页面级悬浮 Toolbar
- [popup-filter-menu.md](popup-filter-menu.md) — 垂直 Popup / overflow
- [form-controls.md](form-controls.md) — 输入框圆角 token
- [../interaction-language.md](../interaction-language.md)
