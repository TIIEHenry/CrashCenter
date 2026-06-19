---
title: "组件：筛选 Popup 与上下文菜单"
type: concept
status: draft
phase: N/A
updated: 2026-06-20
summary: "anchored Popup、筛选菜单、Master 行、popup_section_gap；按住滑动选单（Press-Drag-Release）。"
---

# 筛选 Popup 与上下文菜单

> 分割线：[shared-dividers.md](shared-dividers.md) · Popup 策略：[interaction-language.md §Popup](../interaction-language.md#popup触发方式与-pc-模式)
>
> **Web Demo**：[popup-filter-menu.html](../demos/popup-filter-menu.html)

## 行内层级（三列 · 筛选 Popup）

相册筛选 Popup **禁止**「左 ✓ + 文案」两列混排；须拆 **语义 icon / 文案 / 选中态** 三列，divider **覆盖 label + check 列**（不贯穿 leading icon）。

**宽度**：Popup **不设固定宽**；容器宽 = 各行共享列轨之和，由 **内部控件 token + 最长 label intrinsic 宽** 决定；**所有 item 共用同一套列轨**（Web demo：`subgrid`；Compose：`LazyColumn` + 统一 `Column` 宽约束）。

```text
│←16→│ icon │←8→│  label（max intrinsic） │ check │←16→│
│     │ 24dp │     │  ← 所有行共享此列宽 →   │ 24dp  │     │
│     └──────┴─────┴── divider 起止 ────────┴───────┘     │
  popup_width = 2×content_padding_h + icon + gap + label_max + check
```

| 列 | Token | 说明 |
|----|-------|------|
| **Leading icon** | `icon_leading_column` **24dp** | 类型 glyph（图片/视频…）；**未选** `text_secondary`，**已选** `accent` |
| **Label** | **max-content**（共享列轨） | 主文案；宽度取 **所有行 label 的最大 intrinsic 宽**；已选同色 `accent` |
| **Trailing check** | `icon_leading_column` **24dp** | **多选筛选专用**；已选显示 **✓**（`accent`），未选占位保留列宽 |
| **Divider** | `divider_start_menu_label` → `divider_end_menu_check` | 起 label 左缘、止 **check 列右缘**（`margin-right = content_padding_h`）；**不**贯穿 leading icon 列 |
| **左右 inset** | `content_padding_horizontal` **16dp** | 容器 **无**额外水平 padding；由列轨首末列承担 |

**简单菜单**（搜索/设置/关于 · 无多选）：`menu-row--simple` + 容器 `popup_menu_simple` — **icon + label** 两列；无 check 列：

```text
popup_width = 2×content_padding_h + icon + gap + label_max
```

| 禁止 | 原因 |
|------|------|
| check 与 icon 同列左置 | 破坏选中态与语义分离 |
| divider 拉满 Popup 外缘 | 切断 icon 列视觉锚点 |
| divider 止于 label 右缘、不覆盖 check 列 | check 列与文案区视觉脱节 |
| 未选行隐藏 check 列宽 | 文案列跳动 |
| **固定 popup 宽**（如 220dp） | 短文案行被无谓拉宽；长文案与列轨不对齐 |
| 各行独立 `1fr` label 列 | 列宽不一致，check 列无法对齐 |

## 容器

| 属性 | TouchPrimary 筛选 Popup |
|------|-------------------------|
| 形状 | **`radius_mobile_popup` 16dp** |
| 材质 | **`layer` 实色** + 轻 shadow ≤4dp |
| **宽度** | **`width: max-content`**；列轨见 §行内层级；上限 **`popup_max_width`**（默认 320dp）触顶时 label **ellipsis** |
| 容器 padding | **上下 0**；左右由行 `content_padding_horizontal` 承担；首/末行竖向间距 **仅** `menu_row_padding_vertical` |
| 首/末行按压 | ① overlay **clip 至** `radius_mobile_popup`；见 [visual-language §圆角容器内 Item 栈](../visual-language.md#圆角容器内-item-栈) |
| 行高 | **`menu_row_height`** = `menu_row_padding_vertical`×2 + **`icon_leading_column`** → **40dp**（容纳 24dp icon；非 List 60dp） |
| 行间距 | icon–文案 gap **`space_sm` 8dp**；**禁止**复用 `list_row_min_height` |
| 毛玻璃 | **不用**（非悬浮 anchored 小菜单可选 glass） |
| 触发 | FAB / overflow / **Trail 筛选钮**；Touch：**单击** 或 **按住滑动** |
| 动效 | expand 300ms |

PC PointerPrimary：0dp shadow + outline + instant（见 Policy 表）。

## 按住滑动选单（Press-Drag-Release）

相册 **筛选 Popup** 与 FloatingToolbar **Trail（≡）** 绑定的 Dropdown 须支持 [interaction-language §按住滑动选单](../interaction-language.md#按住滑动选单-press-drag-release)。

### 本组件行为

| 阶段 | 筛选 / 多选 Popup |
|------|-------------------|
| **按住 Trail 滑动** | 跟选行 + ① overlay；松手 **toggle 该行**（多选）或 **应用 Master** |
| **单击 Trail 后点行** | 常规 toggle；**松手/click 后清除按压 overlay** |
| **单击 Trail 后按住滑动** | P1 增强：同 DRAG_SELECTING |
| **松手在 Popup 外** | 关闭；**不**改变已生效的 toggle（本次未 confirm 的行不变） |

```text
  [≡] ──按住──→ ╭─ Popup ─╮
                 │ 🖼 图片        ✓ │ ← 跟选高亮
                 │ 🎬 视频        ✓ │
                 │ ▦ 所有项目       │
                 ╰──────────────╯
                      ↑ 松手 → toggle / 触发
```

| 行类型 | 松手 action |
|--------|-------------|
| 普通过滤项 | toggle checkmark |
| Master 行 | 全选 / 全不选（并集语义） |
| 跳转项（如「管理忽略应用」） | `navigate` + 关闭 |

### 锚点

| 来源 | 锚定 |
|------|------|
| FloatingToolbar Trail | 钮 `PressCentered`；视口 flip 见 interaction-language |
| FAB | FAB 中心上方优先 |
| Toolbar ⋮ | overflow 钮下方 4dp |

## 分组规则

| 层级 | 规则 |
|------|------|
| **组内行** | item 对齐 **divider**（筛选/多选） |
| **同级功能区** | 组末 divider；可选 `popup_section_gap` |
| **Master 行** | **`popup_section_divider`（全宽，含 `margin-bottom = popup_section_gap`）** | 独立 `popup_section_gap` 节点叠在 Master 上缘 |
| **简单菜单**（≤4 项） | 组内可无线 |

## Master 行（汇总项）

| 属性 | 规范 |
|------|------|
| **语义** | 选中 ≡ **上方所有可选项的并集** |
| **视觉** | 与 filter 区有明显 **竖向间隔**（section divider + gap） |
| **行高** | 与组内 filter 行 **相同**（`menu_row_height`）；**禁止**额外 padding / 更大 min-height |
| **字重** | 可用 **Medium/600** 区分语义；**不得**撑高行盒 |
| **状态** | 全选 ✓；部分选中未选或 indeterminate |
| **禁止** | Master 紧贴最后一项 filter |

## 相册 · 照片筛选（参考）

```text
┌─ radius_mobile_popup (16dp) ──────────┐
│  [🖼]  图片                      [✓]  │
│        ─── divider（label + check）───  │
│  [🎬]  视频                      [✓]  │
│        ─── divider ───                │
│  [✎]  已编辑                      [ ]  │
│  ═══ popup_section_divider（全宽 + gap 下距）═══  │
│  [▦]  所有项目                    [ ]  │
└────────────────────────────────────────┘
  icon    label                    check
```

| 吸收 | 不吸收 |
|------|--------|
| 白底 + 轻 shadow + 16dp | 毛玻璃 Popup |
| divider 对齐 label 列 + 右 check 列 | 全宽线；check 左置 |
| 多选 + checkmark | 无间隔 Master |

## 实现出口

| 栈 | 出口 |
|----|------|
| Compose KMP | `SingularityPopupSurface`、`SingularityAnchoredDropdownMenu` |
| 工具 App XML | 待 Policy 对齐；暂 `PopupMenu` / 自定义 |

## Spec 模板（Popup 一节）

- **Tier**：Dropdown / Card
- **分组**：`MenuSection`；Master 行前 `popup_section_gap`
- **触发**：TouchPrimary 长按 / **按住滑动** / 单击；PC 单击 + 键盘

## 相关文档

- [settings-card-detail-sheet.md](settings-card-detail-sheet.md)
- [floating-chrome.md](floating-chrome.md)
