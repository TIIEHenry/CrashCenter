---
title: "生态视觉设计语言"
type: concept
status: accepted
phase: N/A
updated: 2026-06-22
summary: "Clarence 生态视觉 SSOT——token、轻量通透、按压范式；组件 spec 见 components/。"
---

# 生态视觉设计语言

> **单一事实来源（SSOT）**：token 与按压全生态共用；**UI 模式**（信息流 / 工具密度）见 [ui-modes.md](ui-modes.md)。
> 交互见 [interaction-language.md](interaction-language.md)；组件见 [components/INDEX.md](components/INDEX.md)。

## 设计理念

| 理念 | 含义 | 落地要点 |
|------|------|----------|
| **UI 模式** | 信息流 vs 工具密度；同页可 Hybrid 分区 | [ui-modes.md](ui-modes.md) |
| **现代化** | 语义化 token、Edge-to-edge、深浅色自适应、轻量动效 | M3 语义色；透明系统栏；DayNight 全覆盖 |
| **扁平化** | 无厚重阴影；层级靠间距、留白、轻反馈 | 常驻 chrome elevation `0dp` |
| **轻量通透** | 外壳轻投影、内容结构化；拒绝 tonal + 深 elevation | 把 Sheet/Card 一律做成毛玻璃 |
| **留白分型** | 扁平 List 无全宽线；**详情 / 设置**用 Card + **与 item 对齐**的分割线 | 分割线拉满 Card 外缘 |
| **毛玻璃** | **仅悬浮层**（底栏、anchored Popup） | Sheet 内容区、列表行、per-row blur |
| **一套 token** | 所有 App 共用命名与数值 | `colors.xml` / `Color.kt` / TUI CSS |

---

## 轻量通透

在扁平与轻投影之上，明确**拒绝笨重装饰**：外壳不用 tonal 深 elevation；**内容区**可用 metadata 胶囊、pill 等信息形态。毛玻璃**只**用于悬浮层，Sheet / Dialog **内容**用实色 `layer` 即可。

### 原则

| 原则 | 规范 | 禁止 |
|------|------|------|
| **外壳圆角** | **手机 / PC 分档**（见 §圆角：平台分档）；PC 伪窗口 **0dp** | 全平台统一 8dp；PC 用大圆角 Sheet |
| **内容圆角** | 行内 metadata / 标签 / Chip 可用 **`radius_content_pill`**（全圆 capsule） | 把 **Sheet/Card 外壳**做成大 pill |
| **轻阴影** | 常驻 **0dp**；按 Tier 见 §轻投影分层 | Dropdown 12dp、Dialog tonal 6dp |
| **实色 Sheet** | 详情 / 设置 Sheet：**`layer` 实底** + 结构化分组 | 强制全屏毛玻璃 Sheet |
| **轻投影** | 低 elevation、**无 tonal**；PC 窗体可 8dp + outline | tonal 叠满、纯描边冒充窗体 |
| **动效克制** | 300ms 仅 TouchPrimary 必要 expand | 阴影 + 外壳大圆角 + tonal 同时上 |

### 圆角：平台分档 + 内容

圆角 **随 `InputModality` 分流**；禁止用手机大圆角套 PC 窗体，也禁止 PC 直角套手机 Dialog。

#### 共用

| Token | 值 | 用途 |
|-------|-----|------|
| `radius_control` | **4dp** | 按钮、徽章、小控件（UA `extraSmall`） |
| `radius_content_pill` | **全高/2** | 行内 metadata 胶囊；**内容**，非外壳 |

#### 手机 TouchPrimary（弹窗圆角 **更大**）

| Token | 值 | 用途 | UA 对照 |
|-------|-----|------|---------|
| `radius_mobile_control` | **8dp** | 输入框、小按钮 | `SingularityShapes.small` |
| `radius_mobile_card` | **16dp** | 设置 Card、分组容器 | `large` |
| `radius_mobile_dialog` | **16dp** | 居中 Dialog、AlertDialog | AdaptiveDialog Wide |
| `radius_mobile_popup` | **16dp** | Dropdown、anchored 菜单 | Card / 菜单 |
| `radius_mobile_sheet` | **28dp** | Modal BottomSheet、Picker 顶缘 | `extraLarge` |
| `radius_mobile_sheet_detail` | **16dp** | **详情 / 元数据** Sheet 顶缘（相册详情） | 较 Modal 略收敛 |

#### PC PointerPrimary（**微圆角 / 直角**，参考 UA 伪窗口）

| Token | 值 | 用途 | UA 对照 |
|-------|-----|------|---------|
| `radius_pc_panel` | **0dp** | FloatingPanel、DraggableFloatingPanel、L3 伪窗口 | `RectangleShape` |
| `radius_pc_dialog` | **0dp** | PointerPrimary 居中 Dialog / Wide 壳 | 同 FloatingPanel 窗体感 |
| `radius_pc_overlay` | **0dp** | Dropdown、上下文菜单 | outline 定界，无圆角 |
| `radius_pc_micro` | **2dp** | 拖曳把手等微元素 | PaneDialogDragHandle |

> **UA 参考**：`DraggableFloatingPanel` → **直角 + 8dp shadow + 1dp outline**；手机 `AdaptiveDialog` → **16dp**；Modal Sheet → **28dp**（`DESIGN-SPEC` `extraLarge`）。

#### 内容 vs 外壳（仍适用）

相册详情里 ISO/快门 **pill 是内容**（`radius_content_pill`）；Sheet 顶缘是 **外壳**（手机用 `radius_mobile_sheet_detail` 16dp 或 Modal 用 `radius_mobile_sheet` 28dp）。

### 圆角（废弃 token）

| Token | 原值 | 替代 |
|-------|------|------|
| ~~`radius_overlay`~~ | 8dp | 按平台：`radius_mobile_*` / `radius_pc_*` |
| ~~`radius_sheet`~~ | 28dp | `radius_mobile_sheet` |
| ~~`radius_sheet_top`~~ | 16dp | `radius_mobile_sheet_detail` |
| ~~`radius_card`~~ | 12dp | `radius_mobile_card`（16dp） |

### 阴影与 elevation 上限

| Token | 值 | 用途 |
|-------|-----|------|
| `elevation_flat` | **0dp** | Toolbar、列表、共面 chrome |
| `elevation_whisper` | **4dp** | 极轻投影（Toast、Desktop 窗口感）；offset 约 2dp |
| `elevation_panel` | **8dp** | PC 伪窗口 / FloatingPanel（**无 tonal**） |
| `elevation_max` | **4dp** | 手机浮层兜底上限（菜单、Card） |
| `elevation_popup` | **0–8dp** | 按 Tier；见 §轻投影分层 |
| `tonal_elevation` | **0dp** | Dialog / Sheet **禁止** M3 tonal 抬升 |

**浮层分层**：按容器类型选型，**非**「一律无 shadow」或「描边代替阴影」。

### 轻投影分层

> **术语澄清**：「轻量通透」≠ 不要阴影。拒绝的是 **tonal 抬升 + 深 elevation** 的笨重感；允许的是 UniverseAgent **`DraggableFloatingPanel`** 一类：**8dp 投影 + 1dp 描边、 tonal 0** 的 PC 伪窗口。

| 容器 | 投影 | 描边 | Tonal | 参考 |
|------|------|------|-------|------|
| **PC 伪窗口**（FloatingPanel） | **`elevation_panel` 8dp** | 1dp `outlineVariant` ~45% | 0 | UA `PopupSurfaceTier.FloatingPanel` |
| **PC 小菜单**（Dropdown） | **0dp** | 1dp outline | 0 | UA PointerPrimary 非 Panel |
| **手机 Popup 菜单** | ≤ **4dp**；悬浮层可选 glass | 可选 `glass_stroke` | 0 | anchored 小菜单 |
| **手机详情 Sheet** | **`layer` 实底**；顶缘 **`radius_mobile_sheet_detail`** | 可选细描边 | 0 | 相册详情范式 |
| **Toast / Snackbar** | **`elevation_whisper` 4dp** | — | 0 | whisper，非 tonal |
| **Desktop 窗口感** | whisper **4dp / offset 2dp** | 1px border | 0 | UA `DesktopWindowTitleBar` |

**禁止**：Overlay 12dp、Dialog tonal 6dp、shadow + 大圆角 + tonal **叠满**。

**PC 小菜单 vs 伪窗口**：小菜单靠 **outline 定界**（0 shadow）；需要「窗体」感的大面板靠 **轻投影 + outline**——二者并存，不是二选一。

---

## 留白与分割线

主阅读区 **无全宽线**；Card / Sheet / Popup 用 **item 对齐** divider。完整规则与场景表见 **[components/shared-dividers.md](components/shared-dividers.md)**。

| 要点 | 规范 |
|------|------|
| 扁平 List | 无 divider；靠 padding + ① 按压 |
| Card / Sheet | item 对齐 divider；见 [settings-card-detail-sheet.md](components/settings-card-detail-sheet.md) |
| Popup 筛选 | section_gap + Master 行；见 [popup-filter-menu.md](components/popup-filter-menu.md) |

### 视觉深度

```text
canvas → layer 共面区（Toolbar + 列表，无 divider）
       → 悬浮层（底栏 / Popup，可选 glass）
       → Sheet/Dialog 内容：实色 layer
```

---

## 浮层外观（平台分流）

语义 token 共用；**容器视觉**随 `InputModality` 变化（交互 SSOT 见 interaction-language §平台原生浮层外观）。

| Token / 属性 | 手机 TouchPrimary | PC PointerPrimary |
|--------------|-------------------|-------------------|
| 模态容器 | BottomSheet / Dialog | Dialog / FloatingPanel |
| **圆角** | Dialog **16dp**；Modal Sheet **28dp**；详情 Sheet **16dp** | Panel / Dialog **0dp**（`radius_pc_panel`） |
| 分层 | 轻投影 ≤4dp；悬浮层可选 glass | Panel **8dp + outline**；菜单 **0dp + outline** |
| 阴影 | ≤ **4dp**；**无 tonal** | Panel **`elevation_panel` 8dp** |
| 打开动效 | slide / expand 300ms | instant |
| Dialog 按钮区 | 底栏 | 顶栏（Wide 模式） |

---

## 统一色彩体系

### 语义角色（所有应用必用）

| Token | 浅色 | 深色 | 用途 |
|-------|------|------|------|
| `accent` | `#0078D4` | `#479EF5` | 主强调：链接、选中、active 图标、Switch on |
| `accent_container` | `#EFF6FC` | `#0A2E4A` | Chip 选中底、轻强调容器 |
| `canvas` | `#FAFAFA` | `#202020` | 页面背景（`fluent_canvas` / `background`） |
| `layer` | `#FFFFFF` | `#2D2D2D` | 卡片、输入框、浮层实底 |
| `text_primary` | `#242424` | `#FFFFFF` | 主文本 |
| `text_secondary` | `#616161` | `#A0A0A0` | 副文本、未选中图标 |
| `stroke` | `#E0E0E0` | `#3D3D3D` | Chip 描边；**item 对齐 divider** |
| `error` | `#C42B1C` | `#FF99A4` | 错误 |
| `success` | `#107C10` | `#6CCB5F` | 成功 |
| `warning` | `#8A6116` | `#FCE100` | 警告 |

### 品牌 accent 映射（允许 override）

产品可在主题层替换 `accent` 色值，**不得**新增平行色板：

| 产品 | `accent` override | 说明 |
|------|-------------------|------|
| AppSnapShotor / CrashCenter | 默认 `#0078D4` | Fluent Communication Blue |
| UniverseAgent / Singular Shell | `#607D8B` | BlueGrey；仍映射到 `accent` 角色 |
| 编辑器 Canvas（Singular） | `EditorColorScheme` | **不**消费 Shell token，边界独立 |

### M3 语义映射

```
primary          → accent
primary_container → accent_container
surface          → layer
background       → canvas
on_surface       → text_primary
outline          → stroke
error            → error
```

---

## 按压反馈（Press Feedback）

按压反馈按**控件几何形态**分为三种范式，全生态统一。**禁止**列表行使用 Material 全屏扩散 ripple（`selectableItemBackground` 默认行为）。

```text
大面积可点击面（行 / 卡片 / 菜单项）
  → ① iOS 按压（**背景 overlay**；**禁止 scale**）

小触区 · 圆形（IconButton、圆形 FAB）
  → ② 圆形水波纹（唯一允许 ripple 的形态）

小触区 · 非圆形（圆角矩形 Icon 盒、胶囊、不规则轮廓）
  → ③ 形状按压（填色；**Chip 优先 overlay+clip**）

小触区 · Chip / Filter chip
  → ① overlay + clip（与列表行同 token）或 ③ 填色；**禁止 ripple**
```

**Ripple 仅用于正圆触区。** 凡非圆形控件，包括圆角矩形与胶囊，**禁止** `<ripple>` / Material 水波纹（含带 mask 的形状 ripple）。

三种范式**不可叠用**。

---

### ① iOS 按压（Surface Press）

用于**整行可点击面**（列表行、设置行、Popup 菜单行、卡片整行），以及 **Chip 等小触区**（后者须 `clip` 至控件圆角）。反馈为**半透明背景 overlay**叠在控件表面之上，**禁止 scale / 整控件 alpha**；**禁止 ripple**。

| 属性 | Token | 值 |
|------|-------|-----|
| Overlay 浅色 | `press_overlay` | `#14000000`（黑 ~8%） |
| Overlay 深色 | `press_overlay` | `#1FFFFFFF`（白 ~12%） |
| 过渡 | `press_overlay_duration` | **100ms** |
| 松开淡出 | `press_release_ms` | **120ms**（`ShellMotion.DURATION_S`） |
| 按下 | `press_in` | **0ms** snap |
| Scale | — | **禁止**（列表/菜单/卡片行） |
| Ripple | — | **禁止** |

**适用范围**：

| ✅ 使用 | ❌ 不使用 |
|---------|-----------|
| 列表行、设置行、Popup 菜单行 | 圆形 IconButton（→②） |
| Tree 节点、Session 卡片整行 | 严格正圆 Icon（→②） |
| **Chip / Filter chip**（overlay+clip 或 ③ 填色） | ripple |
| Toolbar 文本区、Nav segment | scale 0.97（行级禁止） |
| Filled 主按钮（底色变化） | |

**Chip** 虽为小触区，**推荐与 ① 相同的 overlay**（`press_overlay`），须 `clip` 至 Chip 圆角（`radius_control`）；**禁止** ripple。可选以形状内填色（③）替代 overlay，但不叠用。

| 平台 | 实现 |
|------|------|
| Android View | `shell_press_surface.xml` / `ShellPressFeedback.applySurfacePress` |
| Compose | `Modifier.rowPress` / `shellSurfaceClickable` — overlay tint，`indication = null` |
| Web / Desktop | `::before` overlay 或 `background-color: var(--press-overlay)`；`:active` 同 token；**圆角容器内首/末行**见 §圆角容器内 Item 栈 |

---

### 圆角容器内 Item 栈

**Card、Popup、圆角 Sheet 内容区**等带外壳圆角的容器内，可点击 **Item 行**须同时满足：

| 规则 | 规范 | 禁止 |
|------|------|------|
| **首/末行按压 clip** | 容器 `overflow: hidden`；**首行**顶角、**末行**底角 `border-radius` = 容器外壳 token（如 `radius_mobile_card` / `radius_mobile_popup`）；① overlay 继承行 `border-radius` | 按压 tint 溢出容器圆角；首/末行仍用直角 overlay |
| **无额外上下 inset** | 容器 **上下 padding 0**；行与容器顶/底缘间距 **仅**来自行内 `paddingVertical`（如 `menu_row_padding_vertical`） | 容器再叠 `space_xs` 等上下 padding 造成「浮在中间」 |
| **中间行** | 行 `border-radius: 0`；divider / section gap 不参与圆角 | 每行独立小圆角 Card |
| **单行** | 首末同一行 → 四角均为容器半径 | — |

**实现要点**（Web demo：`.rounded-item-stack` / `.popup-menu`；Compose：`clip(RoundedCornerShape)` + 首末行 `Modifier` 分角）：

```text
┌─ radius_mobile_card ─────────────┐
│ ▓▓ 首行 press overlay 贴顶圆角 ▓▓ │
│ ─── item 对齐 divider ───        │
│     中间行（直角 overlay）         │
│ ▓▓ 末行 press overlay 贴底圆角 ▓▓ │
└──────────────────────────────────┘
  ↑ 容器 padding-top/bottom = 0
```

**`popup_section_gap`** 为组间 **内部** 竖向间隔，**不是**容器对首/末行的额外边距。

---

### ② 圆形水波纹（Circular Ripple — 唯一 Ripple）

**生态中唯一允许 Material 水波纹的形态**：触区为**正圆**（等宽等高，角半径 ≥ 宽/2）。

| 属性 | Token | 值 |
|------|-------|-----|
| 形态 | — | **正圆**，波心 = 触摸点 |
| 边界 | — | borderless circular（`shell_ripple_icon`） |
| 颜色 | `press_ripple_color` | `colorOnSurfaceVariant` |
| 按下 alpha | `press_ripple_alpha_pressed` | **0.16** |
| 聚焦 alpha | `press_ripple_alpha_focused` | **0.12** |
| 悬停 alpha | `press_ripple_alpha_hover` | **0.08**（PointerPrimary） |
| iOS scale | — | **禁止** |

**Android XML**：`@drawable/shell_ripple_icon` — bare `<ripple>`，无 mask。

**Compose**：`SingularityIndications.iconRipple(bounded = true)` / `shellIconClickable`。

**判定**：仅当视觉与触区均为正圆 → ②；圆角矩形、胶囊、方 Icon 盒 **不是圆** → ③。

---

### ③ 形状按压（Shaped Press — 无水波纹）

用于**非正圆**的小触区：圆角矩形 Icon 盒、Chip、胶囊底栏项、自定义 path。**按压反馈必须被限制在控件可见轮廓内**，通过**填色或缩放**表达，**不使用 ripple 扩散波**。

| 手段 | 适用 | 说明 |
|------|------|------|
| **iOS overlay + clip** | **Chip（推荐）**、小按钮、不规则 icon | 同 `press_overlay`；`clip` 至 `radius_control` |
| **形状内填色** | 方 Icon 盒、胶囊 | `state_pressed` selector；`press_shape_fill` |
| **形状内 overlay** | 毛玻璃底栏圆角项 | pressed tint，同 `corners` |

| Token | 浅色 | 深色 |
|-------|------|------|
| `press_shape_fill` | `#14000000` | `#12FFFFFF` |
| `press_shape_fill_selected` | `accent @ 20%` | 同左 |

| 控件示例 | 形状 | 按压实现 |
|----------|------|----------|
| Filter Chip | 圆角胶囊 | **overlay + clip**（推荐）或填色 tint |
| 方 Icon 盒 32×32dp | 圆角矩形 | 填色 selector 或 overlay+clip |
| 悬浮底栏圆角项 | 圆角非正圆 | selector / overlay + `clip` |
| 不规则 path 按钮 | 自定义 | `clip(path)` + 填色或 overlay |
| 毛玻璃胶囊内圆形图标 | 正圆 | **②** 圆形 ripple（非 ③） |

**Android XML**（填色，非 ripple）：

```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <corners android:radius="@dimen/radius_control" />
            <solid android:color="@color/press_shape_fill" />
        </shape>
    </item>
    <item android:drawable="@android:color/transparent" />
</selector>
```

**Compose**：

```kotlin
Modifier
    .clip(RoundedCornerShape(radius_control))  // 与可见形状一致
    .rowPress { }                              // 或 drawBehind 形状填色
    // indication = null — 禁止 ripple
```

**Chip / Toggle**：

- **推荐**：`Modifier.clip(RoundedCornerShape(radius_control)).rowPress { }` — 与 ① 同 `press_overlay`
- **可选**：`state_pressed` 背景 tint；选中态 `accent_container`
- **禁止**：`<ripple>`；iOS 与填色**不可叠用**

---

### 分流决策树

```text
可点击控件
├── 整行 / 大面积面（高 > 40dp 且宽 ≈ 父容器）？
│     └── 是 → ① iOS 按压
├── 触区是严格正圆（等宽等高 + 全圆角）？
│     └── 是 → ② 圆形水波纹（唯一 ripple）
├── Chip / Filter chip？
│     └── 是 → ① overlay+clip（推荐）或 ③ 填色
├── 触区非正圆（方 Icon 盒 / 胶囊 / 不规则）？
│     └── 是 → ③ 填色或 overlay+clip
└── Filled 主按钮 → ① 或底色加深
```

---

### 分流总表（速查）

| 控件 | 范式 | 禁止 |
|------|------|------|
| 列表 / 设置 / Popup 行 | ① overlay | 扩散 ripple、scale、整控件 alpha |
| Tree / 卡片整行 | ① overlay | 同上 |
| 圆形 IconButton | ② 圆形 ripple | overlay 铺满圆、形状填色 |
| **Toolbar 全部 icon** | **② 圆形 ripple**（32dp 正圆） | ③ 方盒、① overlay |
| **Chip / Filter chip** | **① overlay + clip**（推荐）或 ③ 填色 | ripple、overlay+填色叠用 |
| 方 Icon 盒 / 胶囊 | ③ 填色或 overlay+clip | ripple |
| 毛玻璃底栏·圆角项 | ③ 形状填色 | ripple |
| 毛玻璃底栏·正圆图标 | ② 圆形 ripple | ③ 填色 |
| Filled 主按钮 | ① overlay / 底色 | ripple |
| Switch / Checkbox | M3 原生 | — |
| Web 指针控件 | hover → pressed overlay | 触屏式 expand |

---

### Token — Web TUI v2（PointerPrimary）

| Token | 浅色 | 深色 | 状态 |
|-------|------|------|------|
| `--overlay-hover` | 4% | 6% | `:hover` |
| `--overlay-pressed` | 8% | 10% | `:active` |

Web 无 Material ripple；按钮用 overlay。`:focus-visible` 2px outline。

---

### 禁止清单

- ❌ 列表行 `?selectableItemBackground`（无 mask 的全宽扩散波）
- ❌ 非正圆控件使用 `<ripple>`（含 `@android:id/mask` 的形状 ripple）
- ❌ 圆形按钮走 overlay 铺满或形状填色（应走 ②）
- ❌ Chip / 胶囊用 borderless 圆 ripple
- ❌ ① 与 ②/③ 同控件叠用
- ❌ 行内 Switch 与行根双层 `clickable`
- ❌ 形状填色轮廓与可见 shape 不一致

---

### 多平台实现对照

| 范式 | Android XML | Compose KMP | Web TUI |
|------|-------------|---------------|---------|
| ① overlay | `shell_press_surface.xml` | `rowPress` / `shellSurfaceClickable` | `::before` + `--press-overlay` |
| ② 圆形 | `shell_ripple_icon.xml` | `iconRipple` / `shellIconClickable` | `.press-ripple` + `bindPressRipple` |
| ③ 形状 | shape selector 填色 | `clip` + overlay 或 drawBehind | 圆角 + overlay |

---

### 合规检查（按压）

- [ ] 列表行：① overlay，无 scale、无全宽 ripple
- [ ] 非正圆控件：**无 ripple**；③ 填色或 iOS+clip
- [ ] 严格正圆 Icon：② ripple
- [ ] 同一控件未叠用两种范式
- [ ] 深色模式 ripple alpha 仍可见（0.16）

---

## 毛玻璃（Acrylic）

**仅用于悬浮层**——与下方内容叠置、需透出背景的 anchored 控件；**不是**全局 Sheet / Dialog 默认材质。

| Token | 浅色 | 深色 | 用途 |
|-------|------|------|------|
| `glass_fill` | `#18FFFFFF` | `#06000000` | BlurView overlay |
| `glass_fill_fallback` | `#E6FAFAFA` | `#E6202020` | 不支持 blur 时回退 |
| `glass_stroke` | `#14000000` | `#12FFFFFF` | 胶囊描边 |
| `glass_blur_radius` | `50dp` | `50dp` | 模糊半径 |
| `glass_icon_active` | `#33FFFFFF` | `#33000000` | 悬浮 Icon 槽选中/按下容器 |
| `segment_selected_fill` | `#F5F5F5` | `#2A2A2A` | 分段轨滑动 pill 填色 |
| `text_on_glass` | — | — | 毛玻璃上未选文字/icon；深色内容上偏白 |
| `scrim_top_height` | — | — | 顶 Scrim 区高；见 floating-chrome |
| `scrim_fade_distance` | **32dp** | **32dp** | 内容 overlap → alpha |
| `scrim_top_peak` | `#99000000` | `#66FFFFFF` | 顶渐变 peak |
| `scrim_bottom_fade_height` | **96dp** | **96dp** | 底向上 fade |
| `scrim_bottom_color` | `#1A000000` | `#14000000` | 底 fade peak |
| `fg_on_scrim_light` | `#FFFFFF` | `#F5F5F5` | 深底内容上的 chrome 前景 |
| `fg_on_scrim_dark` | `#1A1A1A` | `#E8E8E8` | 浅底内容上的 chrome 前景 |
| `luminance_threshold` | **0.45** | **0.45** | AdaptiveForeground 切换 |

| 场景 | 毛玻璃 / Scrim |
|------|----------------|
| **悬浮底栏 / 分段 Tab** | ✅ glass + stroke — [floating-chrome.md](components/floating-chrome.md) |
| **滚动联动顶/底 Scrim** | ✅ blur + 渐变；**随内容 overlap 显隐** — 同上 §ScrollLinkedEdgeScrim |
| **Anchored Popup / Dropdown** | ⚪ 可选 glass |
| **详情 / 设置 BottomSheet 内容区** | ❌ 用 **`layer` 实色** |
| **Dialog 表单内容** | ❌ 实色 |
| **列表行 / 设置 Card 背景** | ❌ |

```
coordinator（采样根）
└── BlurView
        ├── setBlurRadius(glass_blur_radius)
        ├── setOverlayColor(glass_fill)
        └── API 31+ RenderEffectBlur；低版本 RenderScriptBlur
```

> 悬浮底栏 / 分段 Tab 组件 spec：[components/floating-chrome.md](components/floating-chrome.md)

---

## 字体

| 角色 | 字号 | 字重 | Token |
|------|------|------|-------|
| 工具栏标题 | 17sp | Medium | `type_toolbar_title` |
| 区块标题 | 16sp | Medium | `type_title` |
| 正文 | 14sp | Regular | `type_body` |
| Chip / 筛选 | 13sp | Regular | `type_chip` |
| 标签 / 辅助 | 11sp | Medium | `type_label` |

Compose 产品可扩展完整 M3 15 级字阶；工具 App 保留上表 5 档即可。

---

## 间距与布局

> 组件 spec：[components/shared-dividers.md](components/shared-dividers.md)

| Token | 值 | 用途 |
|-------|-----|------|
| `space_xs` | 4dp | Chip 间距 |
| `space_sm` | 8dp | 行内间隔、列表首尾边距 |
| `space_md` | 12dp | 筛选栏 padding、列表行 vertical padding |
| `space_lg` | 16dp | **`content_padding_horizontal`**、卡片内边距、Toolbar 标题区 |
| `space_xl` | 24dp | 区块 / 设置分组间距 |
| **`content_padding_horizontal`** | **16dp**（=`space_lg`） | **Item 文案 / 主内容列与布局左右缘**（SSOT） |
| `item_row_padding_vertical` | 12dp | Card / 设置 / Sheet / **扁平 List** 行上下 |
| `menu_row_padding_vertical` | **8dp** | **Popup 菜单行**上下（=`space_sm`） |
| `menu_row_min_height` | **20dp** | Popup 单行文案区（14sp × 1.25） |
| `list_row_padding_vertical` | 12dp | 扁平列表行上下（=`item_row_padding_vertical`） |
| `list_row_padding_horizontal` | 16dp | 扁平列表行左右（=`content_padding_horizontal`） |
| `section_gap` | 24dp | 区块之间（=`space_xl`） |
| `toolbar_height` | 48dp | 顶栏 |
| `filter_row_height` | 32dp | 筛选行 |
| `touch_target_min` | 40dp | 最小触控（视觉可 24–32dp + TouchDelegate） |
| `icon_glyph_size` | 24dp | 默认 icon 矢量（Toolbar、Glass、Popup leading） |
| `icon_glyph_size_compact` | 18dp | 紧凑槽 glyph（32dp 筛选容器） |
| `icon_button_size_compact` | 32dp | 方 Icon 盒视觉尺寸 |
| `icon_button_size_glass` | 36dp | GlassIconButton 视觉槽（嵌 H=40 轨） |
| `icon_button_inset` | (size − glyph) / 2 | 对称内边距；36/24→6dp，32/18→7dp，40/24→8dp |
| `icon_leading_column` | 24dp | Popup/设置行 check·icon 列宽；glyph 列内居中 |
| `icon_leading_slot` | 40dp | 带 leading 行首槽（+16dp → `divider_start_leading` 56dp） |
| `list_row_min_height` | 36dp | 列表行视觉下限（不含 padding） |

### Item 内容与布局边距

> **SSOT**：`content_padding_horizontal`（**16dp**，=`space_lg`）。Toolbar 标题、扁平 List 主文案、Card/Popup/Sheet 行文案、区块标题须与此列 **左对齐**；divider 右端与 trailing 区 **右对齐** 同 token。

| 原则 | 规范 | 禁止 |
|------|------|------|
| 水平 inset | 行 `padding-left/right = content_padding_horizontal` | 行内写死 `16px` / `14px` 混用 |
| 垂直 inset | List/Card **`item_row_padding_vertical` 12dp**；**Popup 菜单 `menu_row_padding_vertical` 8dp** | Popup 复用 List 60dp 行高 |
| 文案 | `title` / `sub` **margin: 0**；副标题仅 `margin-top: 2dp` | 标题额外 `margin-left` 破坏对齐 |
| 区块标题 | `section-label` 同 `content_padding_horizontal` | `margin-left: 4dp` 等偏移 |
| 带 leading | 文案左缘 = `content_padding_h + icon_leading_slot`（56dp divider） | leading 槽宽不一致 |
| 嵌套 Card | Card **贴 content 区全宽**；行内仍用 `content_padding_h` | Card 外再套一层 horizontal padding 导致 **双倍 indent** |

```text
布局左缘 │← content_padding_h 16dp →│ 主文案列
         │← 16 + 40 leading →│        │ 带图标行 divider 起点 56dp
```

Web Demo 共享类：`base.css` → `.list-row`、`.card-row`、`.menu-row`、`.settings-row`、`.kv-row`、`.content-inset-h`。

### Switch 控件

> 变体与交互细则：[form-controls.md §Switch](components/form-controls.md#switch)

| Token | 值 | 用途 |
|-------|-----|------|
| `switch_track_width` | **44dp** | Track 宽度 |
| `switch_track_height` | **24dp** | Track 高度 |
| `switch_track_radius` | **12dp** | Track 圆角（= 高 / 2） |
| `switch_thumb_margin` | **4dp** | thumb 与 track 内缘间距（内外边距） |
| `switch_thumb_circle_size` | **16dp** | `track 高 − 2 × margin`；Standard / Enhanced ON |
| `switch_thumb_bar_width` | **10dp** | Enhanced OFF 短横条宽度 |
| `switch_thumb_bar_height` | **6dp** | Enhanced OFF 短横条高度 |
| `switch_thumb_bar_radius` | **3dp** | Enhanced OFF 胶囊端圆角（= bar 高 / 2） |
| `switch_transition_ms` | **200ms** | thumb 位移 / morph 时长 |

**Thumb 定位（圆心对齐 track 端部圆心）**

| 变体 / 态 | 水平 `left` | 垂直 `top` |
|-----------|-------------|------------|
| Standard / Enhanced **ON** 圆 | `track 宽 − track 高/2 − thumb/2` | `(track 高 − thumb 高) / 2` |
| Enhanced **OFF** 横条 | `(track 高 − bar 高) / 2`（左端圆心 = track 左端圆心） | 同左 |

> 44×24 track、`margin=4dp` 时：圆 thumb **16dp**；圆心 x=12（左）/ 32（右）；OFF 横条 left=**9dp**。

---

## 形状与 Elevation

| Token | 值 | 用途 |
|-------|-----|------|
| `radius_control` | 4dp | 共用小控件 |
| `radius_mobile_card` | 16dp | 手机设置 Card |
| `radius_mobile_dialog` | 16dp | 手机 Dialog |
| `radius_mobile_sheet` | 28dp | 手机 Modal BottomSheet |
| `radius_mobile_sheet_detail` | 16dp | 手机详情 / 元数据 Sheet |
| `radius_pc_panel` | 0dp | PC 伪窗口（UA RectangleShape） |
| `radius_pc_overlay` | 0dp | PC Dropdown / 菜单 |
| `radius_pc_micro` | 2dp | PC 微元素（拖曳把手） |
| `radius_content_pill` | 全高/2 | 行内 metadata 胶囊 |
| `elevation_flat` | 0dp | 顶栏、列表、卡片 |
| `elevation_whisper` | 4dp | Toast、极轻提示 |
| `elevation_panel` | 8dp | PC FloatingPanel / 伪窗口 |
| `elevation_max` | 4dp | 手机浮层上限 |
| `elevation_popup` | 0–8dp | 按 Tier（见 §轻投影分层） |

---

## 组件标准（索引）

各组件视觉与交互细则已拆分至 **[components/INDEX.md](components/INDEX.md)**。实现 UI 时优先查阅组件 doc，再回查本文 token。

| 组件 | 文档 |
|------|------|
| Toolbar / List | [toolbar-list-chrome.md](components/toolbar-list-chrome.md) |
| 设置 Card / 详情 Sheet | [settings-card-detail-sheet.md](components/settings-card-detail-sheet.md) |
| 筛选 Popup | [popup-filter-menu.md](components/popup-filter-menu.md) |
| 悬浮底栏 / 分段 Tab | [floating-chrome.md](components/floating-chrome.md) |
| Chip / Switch / 搜索 | [form-controls.md](components/form-controls.md) |
| 分割线 | [shared-dividers.md](components/shared-dividers.md) |

---

## 图标

- Material 填充矢量；默认 **`icon_glyph_size` 24dp**；着色 `text_secondary` / `accent`（active）
- 正圆 Icon → **②** ripple；非圆 → **③** 填色或 overlay+clip，不用行级 scale

### 图标按钮居中（Icon Button Inset）

仅 icon 的按钮：**glyph 在容器内视觉居中**，靠**对称内边距**（`icon_button_inset`）；禁止拉伸 glyph 填满容器。触控区可大于视觉容器（`TouchDelegate` / 父级 `min_touch`）。

| 规则 | 说明 |
|------|------|
| **居中** | 四边 inset 相等；Compose `Box(Alignment.Center)` / XML `android:gravity="center"` |
| **对称 padding** | 禁止仅顶/底或左右不等的 icon padding（文字标签在槽外另计） |
| **固定 glyph** | 使用 `icon_glyph_size` 或 `icon_glyph_size_compact`；不用 `scaleType=fitXY` 撑满 |
| **触区分离** | 视觉 32–36dp 可嵌在 `touch_target_min` 40dp 内，或外包 TouchDelegate |

| 场景 | 视觉容器 | Glyph | Inset |
|------|----------|-------|-------|
| 筛选图标 | `icon_button_size_compact` 32dp | 18dp compact | 7dp |
| GlassIconButton | `icon_button_size_glass` 36dp | 24dp | 6dp |
| **Toolbar 全部 icon**（overflow / 返回 / trail） | **32dp 正圆** | 24dp | 4dp；按压 **② 圆形 ripple** |
| Popup check / leading 列 | `icon_leading_column` 24dp | 18–24dp | 列内居中 |
| 分段轨 pill | — | 文字非 icon | `segment_indicator_inset` 2dp 另计 |

组件落地：[floating-chrome.md §GlassIconButton](components/floating-chrome.md#glassiconbuttonicon-槽)、[form-controls.md §筛选图标](components/form-controls.md#筛选图标容器)、[toolbar-list-chrome.md](components/toolbar-list-chrome.md)、[popup-filter-menu.md](components/popup-filter-menu.md)。

---

## 主题

| 要求 | 说明 |
|------|------|
| **DayNight 必须** | `Theme.Material3.DayNight` 或 Compose `ThemeMode` |
| **系统跟随** | 默认 SYSTEM；允许 LIGHT / DARK 锁定 |
| **深色 press** | overlay 改用 `press_overlay` 深色列；ripple alpha 略提（0.16） |

---

## 产品合规检查

新 UI 或改版时逐项核对：

| # | 检查项 |
|---|--------|
| 1 | 色值仅引用语义 token，无硬编码 `#FFFFFF` 容器 |
| 2 | 列表行：① overlay，无 scale、无全宽 ripple |
| 3 | 严格正圆 Icon：② ripple |
| 4 | Chip：**① overlay+clip** 或 ③ 填色，无 ripple |
| 5 | 分层按 Tier：PC Panel **8dp+outline**；小菜单 **outline**；**无 tonal** |
| 6 | 圆角按 **InputModality**：手机 Dialog 16dp / Sheet 28dp；PC Panel **0dp** |
| 7 | 毛玻璃**仅悬浮层**；Sheet/Dialog 内容用实色 `layer` |
| 8 | 深色模式按压/ripple 可见 |
| 9 | 触控热区 ≥ `touch_target_min` |
| 10 | 扁平 List / Toolbar **无全宽** divider |
| 11 | Card / Sheet / Popup：**item 对齐** divider；筛选 Popup 含 **section_gap + Master 行** |
| 12 | 区块分隔用 `section_gap` 或 Card 间距 |
| 13 | Icon-only 按钮：glyph 对称居中；不拉伸填满容器；见 §图标按钮居中 |

---

## 存量产品映射

| 产品 | 合规状态 | 待迁移 |
|------|----------|--------|
| **UniverseAgent** | ⚪ ② iconRipple；行 overlay ✅ | 对齐 `press_overlay` token |
| **Singular** | ✅ ① overlay + ② 规范 | ③ 形状填色替换方 Icon ripple |
| **AppSnapShotor** | ❌ 行/Chip 误用 ripple | 行→① overlay；Chip/方 Icon→③ |
| **CrashCenter** | ❌ 行 Material ripple | 行→① overlay；筛选→③ 填色 |

---

## 完整 Token 清单（YAML）

```yaml
# 色彩
accent: "#0078D4"
accent-dark: "#479EF5"
accent-container-light: "#EFF6FC"
accent-container-dark: "#0A2E4A"
canvas-light: "#FAFAFA"
canvas-dark: "#202020"
layer-light: "#FFFFFF"
layer-dark: "#2D2D2D"
text-primary-light: "#242424"
text-primary-dark: "#FFFFFF"
text-secondary-light: "#616161"
text-secondary-dark: "#A0A0A0"
stroke-light: "#E0E0E0"
stroke-dark: "#3D3D3D"
error-light: "#C42B1C"
error-dark: "#FF99A4"
success-light: "#107C10"
success-dark: "#6CCB5F"
warning-light: "#8A6116"
warning-dark: "#FCE100"

# 按压 — ① iOS overlay（大面积面；禁止 scale）
press-overlay-light: "#14000000"
press-overlay-dark: "#1FFFFFFF"
press-overlay-duration: 100ms
press-in: 0ms
press-release-ms: 120

# 按压 — ② 圆形 ripple（唯一允许 ripple）
press-ripple-color: "colorOnSurfaceVariant"
press-ripple-alpha-pressed: 0.16
press-ripple-alpha-focused: 0.12
press-ripple-alpha-hover: 0.08

# 按压 — ③ 形状填色（非正圆，禁止 ripple）
press-shape-fill-light: "#14000000"
press-shape-fill-dark: "#12FFFFFF"

# 毛玻璃
glass-fill-light: "#18FFFFFF"
glass-fill-dark: "#06000000"
glass-stroke-light: "#14000000"
glass-stroke-dark: "#12FFFFFF"
glass-blur-radius: 50dp

# 悬浮分段 Tab（H=40dp, inset=2dp → 内 r=18dp）
floating-bar-height: 40dp
radius-floating-bar: "H/2"
segment-indicator-inset: 2dp
radius-segment-indicator: "(H - 2*inset)/2"
segment-indicator-duration: 200ms
floating-chrome-gap: 8dp
floating-bar-inset-bottom: 16dp
segment-selected-fill: layer
text-on-glass: "#FFFFFF"

# 间距（留白）
space-xs: 4dp
space-sm: 8dp
space-md: 12dp
space-lg: 16dp
space-xl: 24dp
list-row-padding-vertical: 12dp
list-row-padding-horizontal: 16dp
content-padding-horizontal: 16dp
item-row-padding-vertical: 12dp
menu-row-padding-vertical: 8dp
menu-row-min-height: 20dp
menu-row-height: 40dp
popup-max-width: 320dp
selection-bubble-height: 44dp
radius-selection-bubble: 22dp
selection-bubble-max-width: min(100vw−32dp, 480dp)
section-gap: 24dp
toolbar-height: 48dp
filter-row-height: 32dp
touch-target-min: 40dp
icon-glyph-size: 24dp
icon-glyph-size-compact: 18dp
icon-button-size-compact: 32dp
icon-button-size-glass: 36dp
# icon-button-inset: (size - glyph) / 2 — 36/24→6dp, 32/18→7dp, 40/24→8dp
icon-leading-column: 24dp
icon-leading-slot: 40dp

# Switch（详见 form-controls.md §Switch）
switch-track-width: 44dp
switch-track-height: 24dp
switch-track-radius: 12dp
switch-thumb-margin: 4dp
switch-thumb-circle-size: 16dp  # track_height - 2*margin
switch-thumb-bar-width: 10dp
switch-thumb-bar-height: 6dp
switch-thumb-bar-radius: 3dp
# thumb left/top: cap-center alignment — see form-controls.md §Switch
switch-transition-ms: 200ms

# 去线 — 禁止 token（存量仅作迁移标记，新代码不得新增）
# toolbar-divider: DEPRECATED — 全宽 Toolbar 底线
# list-divider: DEPRECATED — 扁平列表行间全宽线

# 圆角 — 共用
radius-control: 4dp
radius-content-pill: full

# 圆角 — 手机 TouchPrimary
radius-mobile-control: 8dp
radius-mobile-card: 16dp
radius-mobile-dialog: 16dp
radius-mobile-popup: 16dp
radius-mobile-sheet: 28dp
radius-mobile-sheet-detail: 16dp

# 圆角 — PC PointerPrimary（UA 伪窗口）
radius-pc-panel: 0dp
radius-pc-dialog: 0dp
radius-pc-overlay: 0dp
radius-pc-micro: 2dp

# elevation（轻量通透）
elevation-flat: 0dp
elevation-whisper: 4dp
elevation-panel: 8dp
elevation-max: 4dp
elevation-popup: tiered
tonal-elevation: 0dp

# DEPRECATED
# radius-overlay: 8dp
# radius-sheet-top: 16dp
# radius-card: 12dp

# item 对齐分割线
divider-height: 1dp
divider-color: stroke
divider-start-leading: 56dp
divider-align-end: list_row_padding_horizontal
divider-group-inset: 12dp
popup-section-gap: 12dp
popup-section-divider-height: 8dp
```

---

## 源码参考

| 生态 | 按压 / 视觉入口 |
|------|-----------------|
| UniverseAgent | `ui/theme/Shape.kt`（`SingularityShapes`）、`PopupPresentation.kt`、`DraggableFloatingPanel.kt` |
| Singular | `common/.../shell_press_surface.xml`、`shell_ripple_icon.xml`；`ShellPressFeedback.kt`；`shell-design-language.md` |
| AppSnapShotor | `colors.xml`、`FloatingBottomNav.kt` |
| CrashCenter | `app/.../res/values/colors.xml`、`dimens.xml` |

---

## 相关文档

- [INDEX.md](INDEX.md) — 设计文档总索引
- [components/INDEX.md](components/INDEX.md) — 公共组件标准
- [interaction-language.md](interaction-language.md) — 导航、手势、Popup 策略
- [configuration-ui.md](../architecture/configuration-ui.md) — CrashCenter UI 架构
- [ADR 006: Material3 工具链](../decisions/006-material3-toolchain.md)
- [glossary.md](../glossary.md) — 项目术语
