---
title: "组件：悬浮 Toolbar（FloatingToolbar）"
type: concept
status: draft
phase: N/A
updated: 2026-06-20
summary: "FloatingToolbar 族 + 滚动联动边缘 Scrim（顶 blur 渐变 / 底渐隐）；自适应前景色。"
---

# 悬浮 Toolbar（FloatingToolbar）

> UI 模式：[ui-modes.md §信息流](../ui-modes.md) · 毛玻璃 token：[visual-language.md §毛玻璃](../visual-language.md#毛玻璃acrylic) · 导航分层：[interaction-language.md §导航](../interaction-language.md#导航与信息架构)
>
> **Web Demo**：[floating-chrome.html](../demos/floating-chrome.html)

**仅信息流 UI** 使用；工具密度 UI 用固定 Toolbar / MenuBar，见 [toolbar-list-chrome.md](toolbar-list-chrome.md)。

相册正向参考（本 spec 抽离来源）：

| 截图 | 形态 | 用途 |
|------|------|------|
| 照片网格主 feed | **统一胶囊** `[网格] [照片\|图集] [筛选]` | 任务域 + 次要操作同条 |
| 年/月/日时间线 | **双件簇** `[×] … [年\|月\|日]` | 退出临时模式 + 页内粒度 |
| 顶栏右上 | **浮动圆钮簇** `[搜索] [⋮]` | 全局操作；配合 **ScrollLinkedTopScrim** |
| 顶栏 Scrim | 内容滚入 header 区 → blur+渐变；滚出 → 消失 | 见 [§滚动联动边缘 Scrim](#滚动联动边缘-scrim) |
| 底栏 Scrim | 内容滚入底栏区 → 向上渐隐；无内容 → 消失 | 同上 |

---

## 组件族结构

```text
FloatingToolbar（族）
├── GlassCapsuleShell      ← 外轨：blur + glass_fill + glass_stroke
├── GlassIconButton        ← 圆/圆角方 icon 槽（Lead / Trail / Top）
├── SegmentedGlassTrack    ← 文字分段 + 唯一滑动 pill
└── 布局变体
    ├── UnifiedCapsule     ← 单条三槽（相册主 feed）
    ├── SplitCluster       ← × + gap + 分段轨（相册时间线）
    ├── IconNavCapsule     ← 纯图标 Tab（AppSnapShotor / CrashCenter）
    └── FloatingTopActions ← 顶角独立圆钮（可选）
```

**共用材质**：glass + **`glass_stroke` 1dp**；shadow **0dp**（极轻 whisper ≤4dp 可选，不抢内容）。

---

## 子件规范

### GlassCapsuleShell（外轨）

| Token | 值 | 说明 |
|-------|-----|------|
| `floating_bar_height` | **H = 40dp** | 外轨高度 |
| `radius_floating_capsule` | **H / 2** | 全圆胶囊 |
| `floating_bar_inset_bottom` | **≥ 16dp** | 距底边（含 gesture nav 再加 system inset） |
| `floating_bar_inset_horizontal` | **16dp** | 统一胶囊距左右；双件簇按子件分别 inset |
| `floating_toolbar_max_width` | **min(100% − 32dp, 360dp)** | 统一胶囊最大宽；居中 |

内边距：统一胶囊 **horizontal 4dp**（为 Lead/Trail 留边）；分段轨 **horizontal 2dp**（pill inset 用）。

### GlassIconButton（Icon 槽）

> Icon 居中 + 对称 inset：[visual-language.md §图标按钮居中](../visual-language.md#图标按钮居中icon-button-inset)

| 属性 | 规范 |
|------|------|
| 触控 | **40dp** 最小（`touch_target_min`）；视觉容器 **`icon_button_size_glass` 36dp** 嵌在 H=40 轨内（inset **6dp**） |
| 形状 | **正圆**（Trail/Top）或 **圆角方**（Lead，圆角 `radius_mobile_control` 8dp） |
| 默认 | 透明底；icon `text_on_glass` |
| **选中 / 按下** | 容器填 **`glass_icon_active`**（半透明白/灰，非 accent 大块） |
| 按压 | 正圆 **②** ripple；圆角方 **③** clip + 填色 |
| contentDescription | 必填 |

顶栏 **FloatingTopActions** 与底栏 Lead/Trail **同子件**，仅位置与间距不同（见下）。

### SegmentedGlassTrack（分段轨）

页内 **文字** 分段；**唯一**滑动 pill 指示器（禁止每段静态各一块背景）。

| Token | 值 |
|-------|-----|
| `segment_indicator_inset` | 2dp |
| `radius_segment_indicator` | **(H − 2×inset) / 2** |
| `segment_indicator_duration` | **200ms** FastOutSlowIn |
| `type_segment_label` | 14sp Medium |

| 状态 | 字色 | 背景 |
|------|------|------|
| 未选 | `text_on_glass` | 无 |
| 选中 | `text_primary` | pill `segment_selected_fill`（实色浅灰/off-white） |

**同心圆角**（外轨与内 pill 对齐）：

```text
  ╭────────────────────────────╮  r = H/2
  │ ╭────────────────────────╮ │  r = (H − 2×inset)/2
  │ │  A  │ 〈B〉│  C   │ │ │
  │ ╰────────────────────────╯ │
  ╰────────────────────────────╯
```

交互：点段 → pill **animate** 至段 bounds；连点从当前位置打断重播；段按压 **③** clip。

---

## 变体 A · UnifiedCapsuleToolbar（统一胶囊）

相册 **照片网格** 底栏：`[网格视图] [照片 | 图集] [筛选/菜单]` **一条** glass 胶囊。

```text
╭──────────────────────────────────────────────╮
│ [▦]    │  照片  │ 〈图集〉│    [≡]          │
╰──────────────────────────────────────────────╯
  Lead         SegmentedGlassTrack          Trail
```

| 槽位 | 职责 | 相册范例 |
|------|------|----------|
| **Lead** | 视图/layout 切换、次要模式 | 网格 ↔ 列表 |
| **Center** | **任务域**导航（2–3 段文字） | 照片 \| 图集 |
| **Trail** | overflow、筛选、排序入口 | 筛选菜单（≡） |

| 规则 | 说明 |
|------|------|
| 分段在 **Center 独占** | Lead/Trail 只能是 Icon，不得再塞第三套分段 |
| 任务域用 **文字分段** 或 **IconNav** 二选一 | 相册选文字；AppSnapShotor 选 Icon |
| Lead 可有 **toggle 选中态** | 网格钮 pressed 时用 `glass_icon_active` |
| Trail 常开 Popup | 锚定 Trail 或 FAB；见 [popup-filter-menu.md §按住滑动](popup-filter-menu.md#按住滑动选单-press-drag-release) |

**与 IconNav 差异**：统一胶囊 **更宽**、含次要操作，适合 **3 个入口以内** 且主切换需要 **文字标签** 的 feed。

---

## 变体 B · SplitToolbarCluster（双件簇）

相册 **年/月/日** 时间线：`×` 与分段轨 **分离**，中间 **`floating_chrome_gap` 8dp**。

```text
┌───┐   ┌─ SegmentedGlassTrack ─────┐
│ × │   │ 年 │ 〈月〉│ 日 │          │
└───┘   └───────────────────────────┘
  ↑                    ↑
GlassIconButton    仅分段，无 Lead/Trail
  └── 8dp gap ──┘
```

| 属性 | 规范 |
|------|------|
| 用途 | **临时子模式** + **页内粒度**（非任务域） |
| × / Lead | 直径 **= H**；退出缩放/多选/临时视图 |
| 分段 | **2–4 段**；年 \| 月 \| 日 |
| 对齐 | 两子件 **底边对齐**；整簇 **水平居中** |
| 内容避让 | 列表 `paddingBottom` ≥ H + inset + gap |

**何时用双件而非统一胶囊**：存在明确 **「退出当前模式」** 且分段语义是 **内容粒度** 而非 App 级 Tab 时。

---

## 变体 C · IconNavCapsule（纯图标主导航）

AppSnapShotor / CrashCenter Phase 4C：**无 Lead/Trail**，2–3 个 **等分 Icon** Tab。

```text
╭─────────────────────────╮
│  🛡   │   📊   │   ⚙   │   ← 仅 2–3 个；设置可不进 Tab
╰─────────────────────────╯
```

| 属性 | 规范 |
|------|------|
| 选中 | icon **`accent`** tint |
| 未选 | `text_on_glass` / `onSurfaceVariant` |
| 禁止 | 与变体 B 的 × 或页内分段 **拼进同一条** |

---

## 变体 D · FloatingTopActions（顶角浮动）

相册右上：**搜索**、**更多** 各一 **独立正圆** GlassIconButton；**不**并入底栏胶囊。

```text
                    ╭───╮ ╭───╮
                    │ 🔍 │ │ ⋮ │
                    ╰───╯ ╰───╯
                         ↑
              floating_top_action_gap 8dp
```

| Token | 值 |
|-------|-----|
| `floating_top_inset_top` | status bar inset + **8dp** |
| `floating_top_inset_end` | **16dp** |
| `floating_top_action_gap` | **8dp** |

与底栏 **材质一致**；点击搜索可展开为 inline 搜索或全屏（产品自定，交互见 [form-controls.md](form-controls.md)）。

---

## 滚动联动边缘 Scrim（ScrollLinkedEdgeScrim）

相册 **照片 / 图集** feed 顶栏与底栏共用的 **滚动驱动** 保护层：仅当 **有信息内容滚入** 对应边缘区域时，才出现 **模糊 + 渐变**；内容滚出该区域后 **Scrim 消失**；标题与 **FloatingTopActions** 前景色 **随底下采样亮度切换**。

> 与 **FloatingToolbar 本体的 glass** 独立：Scrim 是 content 与 chrome 之间的 **全宽过渡层**；底栏胶囊仍保留自有 stroke/glass。

### 顶栏 · ScrollLinkedTopScrim

```text
  内容未进入顶区（图集 resting）          照片网格：内容滚入顶区
  ┌────────────────────┐              ┌─ blur+gradient ─┐
  │ 图集    🔍 ⋮       │  实底/透明   │ 照片    🔍 ⋮     │  fg 自适应
  │                    │              │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│  ← 渐变向下淡出
  │  [置顶相册…]       │              │ [photo][photo]  │
  └────────────────────┘              └─────────────────┘
       scrimAlpha = 0                      scrimAlpha → 1
```

| 属性 | 规范 |
|------|------|
| **触发** | 列表/网格 **与顶 Scrim 矩形相交** → `scrimTopAlpha` ↑；**不相交** → α → **0** |
| **区域高** | **`scrim_top_height`** = status bar inset + **56dp**（紧凑标题）；大标题页可 **+64dp** 扩展区 |
| **材质** | 同 `glass_blur_radius` 采样根内容；叠 **`scrim_top_gradient`**（上浓下淡） |
| **渐变** | 顶缘 **`scrim_top_peak`**（略实）→ 底缘 **全透明**；禁止硬切线 |
| **大标题** | 可选：resting 时 **34sp Bold** 在内容流内；Scrim 出现后可 **collapse** 至 17sp 并 pin 顶栏 |
| **固定元素** | `FloatingTopActions`、紧凑标题 **z 在 Scrim 之上**；随 scrim 一起淡入/前景换色 |

#### scrimTopAlpha 计算

```text
overlap = height( contentBounds ∩ scrimTopRect )
scrimTopAlpha = clamp( overlap / scrim_fade_distance, 0, 1 )
// scrim_fade_distance 建议 24–48dp；曲线 easeOut
```

| 状态 | overlap | Scrim | 典型页 |
|------|---------|-------|--------|
| **Resting 顶** | 0 | 无 blur；顶区可 **实色 layer** 或全透明 | 图集首屏（白底） |
| **滚动中** | >0 | blur + 渐变随 overlap 增强 | 照片网格 |
| **滚回顶且无内容在顶区** | 0 | Scrim **消失**（非永久顶栏） | 回到列表顶部空白区 |

### 底栏 · ScrollLinkedBottomFade

相册 **FloatingToolbar 上方** 的 **向上渐隐**（截图 2 图集页底缘可见）：

```text
        [album cards…]
        ─ ─ ─ ─ ─ ─ ─
        ░░ gradient ░░   ← scrimBottomAlpha > 0 时出现
        ╭─ UnifiedCapsule ─╮
        ╰──────────────────╯
```

| 属性 | 规范 |
|------|------|
| **触发** | 内容 **与底 fade 区相交** → `scrimBottomAlpha` ↑；内容 **不进入** 底 fade 区 → α → **0** |
| **区域高** | **`scrim_bottom_fade_height` 80–120dp**（自屏幕底向上，含 toolbar 占位） |
| **材质** | **纯渐变**（`scrim_bottom_color` → transparent）；**不必**全宽 blur（性能） |
| **与 Toolbar** | 渐变在 **Toolbar 之下**；Toolbar 自有 glass 不变 |
| **列表末** | 滚至 **内容底部离开 fade 区** → 渐隐 **消失**（不永久留灰带） |

```text
scrimBottomAlpha = clamp( overlapBottom / scrim_fade_distance, 0, 1 )
```

### 自适应前景色（AdaptiveForeground）

顶栏标题、**FloatingTopActions** icon、系统 status bar 图标须 **同一 luminance 决策**：

| 输入 | 行为 |
|------|------|
| `scrimTopAlpha == 0` | 采样 **页面 resting 背景**（常 `layer` 实色）→ 浅底用 **`fg_on_scrim_dark`**，深底用 **`fg_on_scrim_light`** |
| `scrimTopAlpha > 0` | 采样 **Scrim 下方 blur 区域** 平均亮度 `L` |
| 切换 | `L < luminance_threshold` → **`fg_on_scrim_light`**（白）；否则 **`fg_on_scrim_dark`** |
| 迟滞 | 阈值 **±0.08** hysteresis，防照片边界抖动 |
| 动画 | 前景色 **150–200ms** crossfade；与 scrim alpha 同步 |

| Token | 浅色模式 | 深色模式 |
|-------|----------|----------|
| `fg_on_scrim_light` | `#FFFFFF` | `#F5F5F5` |
| `fg_on_scrim_dark` | `#1A1A1A` | `#E8E8E8` |
| `luminance_threshold` | **0.45** | 同左 |

**GlassIconButton** 在 Scrim 上：容器可用 **`glass_icon_active`** 半透底；icon 仍跟 AdaptiveForeground。

### 与 UI 模式

| 模式 | Scrim |
|------|-------|
| **信息流 feed** | ✅ 顶 + 底 scroll-linked |
| **工具密度** | ❌ 固定实色 Toolbar；不用 scroll blur |
| **Hybrid** | 仅 **信息流主区** scroll 驱动；顶 Chip 行若固定实色 Toolbar 则 **不** 叠 Scrim |

### 实现要点

```text
CoordinatorLayout / Compose:
  content()           // edge-to-edge，无 top/bottom 纯色 padding
  ScrollLinkedTopScrim(alpha, blur, gradient)
  ScrollLinkedBottomFade(alpha, gradient)
  FloatingTopActions(foreground = adaptive)
  FloatingToolbar(...)
```

| 禁止 | 原因 |
|------|------|
| Scrim **常开** 不随 overlap | 图集 resting 不应有 blur |
| 顶栏实色 48dp Toolbar + 全宽 Scrim blur **双份** | Hybrid 页须分区 |
| 底 fade 替代 Toolbar glass | 职责不同：fade=内容可读；glass=控件本体 |
| 前景色固定白/黑不采样 | 浅底图集页须深色字 |

### 相册页面对照

| 页面 | 顶 Scrim | 底 Fade | 前景 |
|------|----------|---------|------|
| **照片** 网格 | 滚入照片 → blur+渐变 | 有内容在底区 → 向上 fade | 多为 light（照片深） |
| **图集** 列表 | resting **无** Scrim（白底） | 卡片滚至底栏下 → fade | resting **dark** 标题 |

---

## 可选 · FloatingScrollHandle

相册右侧 **纵向小胶囊**（↑↓）：快速跳时间轴；**独立第三浮动件**。

| 属性 | 规范 |
|------|------|
| 尺寸 | 宽 **32dp** × 高 **56dp**；`r = min(w,h)/2` |
| 材质 | 同 GlassCapsuleShell |
| 位置 | `end` inset 8dp；垂直居中或随 scroll 贴边 |
| 优先级 | P2；无 timeline 长列表可不实现 |

---

## 选型决策

```text
底栏需要什么？
├── 仅 2–3 个任务域，图标即可
│     → 变体 C · IconNavCapsule
├── 任务域要文字 + 左右还要 1–2 个 icon 操作
│     → 变体 A · UnifiedCapsuleToolbar
└── 用户在临时视图，要退出 + 改内容粒度（年/月/日）
      → 变体 B · SplitToolbarCluster

顶栏全局操作（搜索、⋮）？
  → 变体 D · FloatingTopActions（勿塞进底栏）
```

### 禁止混用

| 禁止 | 原因 |
|------|------|
| 同一条 bar 上 **任务域分段 + 页内粒度分段** | 语义冲突 |
| 双件簇再挂 Lead/Trail | 变体 B 仅 × + 分段 |
| 底栏固定 `BottomNavigationView` + 悬浮底栏 | chrome 双份 |
| Sheet/Dialog 内容区用 glass | 仅悬浮层；见 visual-language |

---

## 内容区协作

| 项 | 规范 |
|----|------|
| `paddingBottom` | ≥ `floating_bar_inset_bottom` + H + **8dp** |
| 滚动 | 内容 **edge-to-edge**；**ScrollLinkedEdgeScrim** 随 overlap 显隐 |
| 列表标题 | 大标题可 collapse；与 TopScrim 联动 |
| Hybrid 页 | 顶区仍可用 [toolbar-list-chrome.md](toolbar-list-chrome.md)；底区仅 FloatingToolbar |

---

## Token 汇总

| Token | 值 |
|-------|-----|
| `floating_bar_height` | 40dp |
| `floating_bar_inset_bottom` | ≥16dp |
| `floating_bar_inset_horizontal` | 16dp |
| `floating_chrome_gap` | 8dp（双件簇 × 与分段轨） |
| `floating_top_action_gap` | 8dp |
| `glass_fill` / `glass_stroke` / `glass_blur_radius` | visual-language |
| `glass_icon_active` | `#33FFFFFF` / `#33000000`（Lead 选中、icon 容器） |
| `segment_selected_fill` | `#F5F5F5` / `#2A2A2A`（pill） |
| `text_on_glass` | `#FFFFFF` / `#E6FFFFFF`（深色内容上） |
| `scrim_top_height` | status inset + **56dp**（+ 可选 64dp 大标题区） |
| `scrim_fade_distance` | **32dp** | overlap → alpha 映射距离 |
| `scrim_top_peak` | `#99000000` / `#66FFFFFF` | 渐变顶缘 peak（叠 blur） |
| `scrim_bottom_fade_height` | **96dp** | 自底向上 fade 区 |
| `scrim_bottom_color` | `#1A000000` / `#14000000` | 底渐隐 peak |
| `fg_on_scrim_light` / `fg_on_scrim_dark` | 见 §ScrollLinkedEdgeScrim | AdaptiveForeground |
| `luminance_threshold` | **0.45** | 前景切换（±0.08 hysteresis） |

---

## 实现槽位（Compose / XML）

```kotlin
// 概念 API — 各产品自行命名
FloatingToolbar.Unified(
  lead = { GlassIconButton(...) },
  center = { SegmentedGlassTrack(segments, selected, onSelect) },
  trail = { GlassIconButton(...) },
)
FloatingToolbar.Split(
  aux = { GlassIconButton(icon = Close) },
  track = { SegmentedGlassTrack(...) },
)
FloatingToolbar.IconNav(items = listOf(...))
FloatingTopActions(actions = listOf(search, overflow))
```

| 产品 | 变体 | 参考 |
|------|------|------|
| 相册 | A + B + D | 本 spec 截图 |
| AppSnapShotor | C + D（搜索可折叠进 Toolbar） | `FloatingBottomNav.kt` |
| CrashCenter | C（2 Tab） | Phase 4C MainShell |
| UniverseAgent | — | 聊天流不用底栏 Tab；PC 用 Panel |

---

## 相关文档

- [ui-modes.md](../ui-modes.md)
- [popup-filter-menu.md](popup-filter-menu.md) — Trail 筛选 Popup
- [toolbar-list-chrome.md](toolbar-list-chrome.md) — 列表 bottom padding
- [form-controls.md](form-controls.md) — 搜索展开
- [../INDEX.md](../INDEX.md)
