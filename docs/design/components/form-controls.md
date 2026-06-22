---
title: "组件：表单与筛选控件"
type: concept
status: accepted
phase: N/A
updated: 2026-06-22
summary: "FilterChip、Switch、搜索框、筛选图标的视觉与交互规范。"
---

# 表单与筛选控件

> 按压 SSOT：[visual-language.md §按压反馈](../visual-language.md#按压反馈press-feedback)
>
> **Web Demo**：[form-controls.html](../demos/form-controls.html)

## Chip / FilterChip

| 属性 | 规范 |
|------|------|
| 圆角 | `radius_control` 4dp 或 clip 胶囊 |
| 描边 | 1dp `stroke` |
| 选中 | `accent_container` 底 |
| 按压 | **① overlay + clip**（推荐）或 **③** 填色；**无 ripple** |
| 行为 | 变更 **即时** 生效过滤 |

### 模式（跨项目）

| 类型 | 行为 | 项目 |
|------|------|------|
| 单选过滤 | `singleSelection=true` | NeverCrash 状态 Chip |
| 多选设置 | 即时写 prefs | NeverCrash settingsChips |
| 多选过滤 | 至少保留一项 | AppSnapShotor |
| 标签 Chip | 单行滚动 + 多选 | AppSnapShotor Tags |

## Switch

- 行内 Switch **`clickable=false`**；整行承载点击（①）
- thumb/track 自定义 selector；on 态 track 填 `accent`
- 两种 thumb 变体：**Standard**（默认）与 **Enhanced**（`switch_style_enhanced` / `SwitchEnhanced`）

### Standard Switch（默认）

| 属性 | Token / 值 |
|------|------------|
| Track 宽 × 高 | `switch_track_width` × `switch_track_height` — **44 × 24dp** |
| Track 圆角 | `switch_track_radius` — **track 高 / 2**（12dp） |
| Thumb 边距 | `switch_thumb_margin` — **4dp**（thumb 与 track 内缘；圆 thumb 尺寸由此推导） |
| Thumb（OFF / ON） | 正圆 **16dp**（`= track 高 − 2 × margin`） |
| Thumb 定位 | **圆心**对齐 track 端部圆心；见 [visual-language §Switch](../visual-language.md#switch-控件) |
| 过渡 | `switch_transition_ms` — **200ms**；属性 `left` |

### Enhanced Switch（增强）

> OFF 态 thumb 为**左对齐圆角短横条**；滑到 ON 后 morph 为**右对齐正圆**。

| 属性 | Token / 值 |
|------|------------|
| Track | 与 Standard 相同（44 × 24dp） |
| Thumb OFF | **胶囊短横条**：宽 `switch_thumb_bar_width` **10dp**，高 `switch_thumb_bar_height` **6dp** |
| Thumb OFF 圆角 | `switch_thumb_bar_radius` — **bar 高 / 2**（3dp） |
| Thumb OFF 位置 | **左端圆心**对齐 track **左端圆心**；`left = (track 高 − bar 高) / 2` → **9dp** |
| Thumb ON | 正圆 **16dp**（同 Standard）；**圆心**对齐 track **右端圆心** |
| 过渡 | `switch_transition_ms` **200ms**；`left` + `width` + `height` + `border-radius`（bar → circle morph） |

```text
OFF (Enhanced)          ON (Enhanced)
┌──────────────────┐    ┌──────────────────┐
│ ▬▬                │    │              ● │
└──────────────────┘    └──────────────────┘
  bar 10×6dp 左           circle 16dp 右
```

**选用**：CrashCenter 设置行、工具密度列表默认可开 Enhanced；需与 M3 原生 Switch 像素级一致时用 Standard。

**Android**：主题 attr `switchStyleEnhanced` 或 prefs `switch_style_enhanced`；Compose 封装 `SwitchEnhanced`。

**Web Demo**：`.switch`（Standard）与 `.switch-enhanced` 并列 — [form-controls.html](../demos/form-controls.html)。

Token 数值 SSOT：[visual-language.md §Switch](../visual-language.md#switch-控件)。

## 搜索框

- Outlined Dense；`radius_control`；`type_body`
- 与 Chip 行之间靠 **间距**，无 divider
- IME Action：Go / 搜索

## 筛选图标容器

> Icon 居中 + 对称 inset：[visual-language.md §图标按钮居中](../visual-language.md#图标按钮居中icon-button-inset)

| 属性 | 值 |
|------|-----|
| 容器 | **`icon_button_size_compact` 32dp** |
| 图标 | **`icon_glyph_size_compact` 18dp**（inset **7dp**） |
| 按压 | **③** 形状填色 selector |

## 主按钮

- 确认语义 → **①** 或底色加深
- 非正圆 → **禁止** 全宽 ripple

## 相关文档

- [toolbar-list-chrome.md](toolbar-list-chrome.md)
- [../interaction-language.md §表单与输入](../interaction-language.md#表单与输入)
