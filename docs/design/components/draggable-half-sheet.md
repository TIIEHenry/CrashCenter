---
title: "组件：可拖曳半屏 Sheet"
type: concept
status: accepted
phase: N/A
updated: 2026-06-22
summary: "TouchPrimary 半屏 BottomSheet——顶栏内嵌拖曳把手、Half/Full chrome 形态切换、28dp 顶缘、把手驱动展开/收起/关闭。"
---

# 可拖曳半屏 Sheet（Draggable Half Sheet）

> 分割线：[shared-dividers.md](shared-dividers.md) · Toolbar 行规范：[toolbar-list-chrome.md](toolbar-list-chrome.md) · 圆角 token：[visual-language.md §圆角](../visual-language.md#圆角平台分档--内容)
>
> **Web Demo**：[draggable-half-sheet.html](../demos/draggable-half-sheet.html)

TouchPrimary **任务型 / 列表型 / 管理型** 半模态浮层：默认约占视口 **50%**，通过顶栏内 **DragHandle** 拖曳在 **半屏 ↔ 全屏 ↔ 关闭** 间切换；点 scrim 关闭。

**材质**：实色 `layer` + scrim；顶缘 **`radius_mobile_sheet` 28dp**（Modal Picker 同级，**非**详情 Sheet 的 16dp）。

## 解剖结构（Anatomy）

```text
[ scrim — tap dismiss ]
┌─ radius_mobile_sheet (28dp) ─────────────────────┐
│  Toolbar chrome（顶栏区；内容行可选，把手不可省）      │
│  ┌──────────────────────────────────────────────┐  │
│  │     ━━━ sheet_drag_handle（必填）━━━         │  │  ← 内嵌于 chrome，非悬浮 pill
│  │  [lead icon?]    title?    [trail icon?]     │  │  ← 可选一行；可无 title
│  └──────────────────────────────────────────────┘  │
│  ─── 可滚动内容区 ───                               │
│  …                                                 │
└────────────────────────────────────────────────────┘
```

| 子元素 | 必填 | 说明 |
|--------|------|------|
| **DragHandle** | ✅ | 水平条，**属于 Toolbar chrome 顶栏区**；禁止脱离 chrome 的独立浮动 pill（对比 [settings-card-detail-sheet](settings-card-detail-sheet.md) 装饰把手） |
| **Toolbar 行** | ❌ | Half / Full icon：**32dp 正圆 + ② 圆形 ripple**（见 [toolbar-list-chrome](toolbar-list-chrome.md)） |
| **内容区** | ✅ | 列表 / 表单 / 任意；item 对齐 divider 规则同 shared-dividers |

**最小配置**：仅 DragHandle + 内容（无 title、无左右 icon）合法。

## Chrome 形态切换（Half ↔ Full）

拖曳至 **Full（100%）** 时，顶栏 **morph** 为与主界面一致的 **CenterTitleToolbar**（工具密度 UI）；**非** 继续显示半屏 Sheet 把手行。

### Half chrome（默认 · 半屏停靠）

```text
┌─ radius 28dp ─────────────────────────┐
│        ━━━ DragHandle（必填）━━━        │
│  [lead?]      title（居中）    [trail?] │  ← 可选；lead 常为 ✕ 关闭半屏
└────────────────────────────────────────┘
```

| 元素 | 规范 |
|------|------|
| **DragHandle** | ✅ 可见；唯一拖曳命中区 |
| **Toolbar 行** | 可选；`lead \| title \| trail`；icon **32dp 正圆 + ② ripple** |
| **Toolbar icon 按压** | **② 圆形 ripple**；禁止 ③ 方盒填色 |
| **lead 语义** | 半屏：**关闭 Sheet**（✕）或省略；**非** 系统返回 |
| **title** | 居中；`type_toolbar_title` 17sp Medium |

### Full chrome（全屏停靠 · CenterTitleToolbar）

```text
┌─ radius 0 · edge-to-edge ──────────────┐
│  status bar inset（系统安全区）          │
│  [← 返回]     title（居中）    [trail?] │  ← 同 toolbar-list-chrome 共面 Toolbar
└────────────────────────────────────────┘
```

| 元素 | 规范 |
|------|------|
| **DragHandle** | **隐藏**（高度收为 0；拖曳区让位给 Toolbar） |
| **Navigation（lead）** | **← 返回**（32dp **正圆**）；**② ripple**；**必填**（有 title 时） |
| **title** | **视口居中**；与 [toolbar-list-chrome](toolbar-list-chrome.md) 同 token |
| **trail** | 可选主操作；无 trail 时右侧 **32dp spacer** 保持标题几何居中 |
| **返回行为** | **Full → Half**（一级）；Half 再返回 / ✕ / scrim → **Dismissed** |
| **拖曳** | Full 态 **不**显示把手；下拉收起靠 **返回** 或自 Half 态拖曳 |

### 形态切换阈值

| 过渡 | 建议阈值 | 动效 |
|------|----------|------|
| Half → Full chrome | 高度 **≥ 75%** 视口 | `transition_sheet` 300ms；handle 淡出 + Full 行淡入 |
| Full chrome + 顶缘圆角 0 | 高度 **≥ 97%** 视口 | 圆角与 chrome 同步 |
| Full → Half chrome | 高度 **< 75%** 或吸附 Half | 反向 |

```text
        Half chrome              morph (≥75%)           Full chrome (100%)
  ━━━ handle + 半屏行    →    交叉过渡    →    status bar + ← title trail
  radius 28dp                                    radius 0
```

**禁止**：全屏仍显示 DragHandle 与半屏 ✕ 行；全屏 lead 用 ✕ 代替 ←（破坏「子页」语义）。

## 与相近组件对照

| 维度 | **Draggable Half Sheet**（本组件） | [settings-card-detail-sheet](settings-card-detail-sheet.md) | Modal BottomSheet（Picker） |
|------|-----------------------------------|------------------------------------------------------------|-----------------------------|
| 用途 | 任务列表、设置子页、可调整高度的半模态工作区 | 元数据 / 关于 / 相册详情 | 长列表 Picker、单选目录 |
| 顶缘圆角 | **`radius_mobile_sheet` 28dp** | **`radius_mobile_sheet_detail` 16dp** | **`radius_mobile_sheet` 28dp** |
| 把手 | **可拖曳**，内嵌 Toolbar chrome | 装饰性，chrome 外居中 pill | 系统/M3 默认（本生态统一用本组件把手 token） |
| 默认高度 | **~50% 视口** | 内容驱动（常 < 70vh） | 内容或 `skipHalfExpanded` 策略 |
| 主操作 | Toolbar trail / 内容内 | 底栏 icon row | ActionBar 图标（P0） |
| 拖曳语义 | expand / collapse / dismiss 阈值 | 无（仅 scrim 关） | 系统行为；生态内对齐本组件阈值 |

PointerPrimary：**禁止**自底滑入半屏 Sheet；改 **Dialog / DraggableFloatingPanel**（instant）。见 [interaction-language §平台原生浮层](../interaction-language.md#平台原生浮层外观)。

## 高度与拖曳交互

### 停靠点（Snap）

| 状态 | 高度 | 进入方式 |
|------|------|----------|
| **Half** | **50%** 视口（`sheet_height_half`） | 打开（默认档）、从 Full 下拉 |
| **Full** | **100%** 视口（`sheet_height_expanded`） | 上拖过阈值；顶缘圆角 **0dp** |
| **Dismissed** | 0（移除） | 下拖过关闭阈值 / scrim tap / 返回键 |

生态内 **两档内容高度**：Half 与 Full。产品通过 **`sheet_snap_policy`** 声明启用哪一档（或两档皆启）。

| `sheet_snap_policy` | 启用档 | 打开默认 | 上拖 | 全屏 ← 返回 |
|---------------------|--------|----------|------|-------------|
| **`both`**（默认） | Half + Full | Half 50% | 可拖至 Full | Full → Half |
| **`half_only`** | 仅 Half | Half 50% | **不可**展开 Full；高度钳在 50% | —（无 Full 态） |
| **`full_only`** | 仅 Full | **Full 100%** | 不可降至 Half | **关闭 Sheet**（无 Half 可回） |

```kotlin
// Compose / 配置示例
SheetSnapPolicy.Both       // 两档
SheetSnapPolicy.HalfOnly   // -picker 短列表、固定半屏任务
SheetSnapPolicy.FullOnly   // 直接全屏子页（仍走 Sheet 容器 + morph chrome）
```

**`half_only`**：DragHandle 仍必填；拖曳仅 **Half ↔ Dismiss**；禁止 morph CenterTitleToolbar（除非产品误配，验收应 fail）。

**`full_only`**：打开即 Full chrome + 100%；拖曳仅 **Full ↔ Dismiss**（或禁拖仅 ← 关）；无 Half chrome 中间态。

阈值建议（`both` 时；单档时去掉跨档吸附）：

- 向上拖过 **+80dp** 或速度 **> 500dp/s** → Full（仅 `both` / `full_only` 打开态）
- 向下拖过 **-120dp** 或落点 **< 25%** 视口 → Dismissed
- 其余松手 → 吸附**已启用**的最近停靠点

全屏时顶缘 **`radius_mobile_sheet` 过渡为 0dp**（edge-to-edge）；Android 须处理 `WindowInsets` 状态栏/刘海。

### 拖曳命中

- **仅 DragHandle 条带**（含扩展触区 `sheet_drag_handle_hit_height`）响应拖曳；内容区滚动与把手拖曳分离（`nestedScroll` / `pointerEvents` 分流）
- Toolbar 左右 icon：**点击**走 ③ 形状填色，**不**触发 Sheet 拖曳
- 长按把手后开始跟手；未移动前短按不拖曳

### 打开 / 关闭动效

| InputModality | 打开 | 关闭 / 吸附 |
|---------------|------|-------------|
| **TouchPrimary** | 自底 **slide + expand 300ms**（`transition_sheet`） | 同曲线吸附停靠点 |
| **PointerPrimary** | **instant**（若未来平板 DeX 误触须切 Dialog） | instant |

禁止 tonal 抬升；阴影 ≤ **4dp**（`elevation_max`）。

## Token 速查

| Token | 值 | 用途 |
|-------|-----|------|
| `radius_mobile_sheet` | **28dp** | 顶缘左右圆角 |
| `toolbar_height` | **48dp** | Toolbar 行（含 title/icon 时） |
| `sheet_drag_handle_width` | **36dp** | 把手视觉条宽度 |
| `sheet_drag_handle_height` | **4dp** | 把手视觉条高度 |
| `sheet_drag_handle_hit_height` | **24dp** | 把手垂直触区（居中于 chrome 顶） |
| `sheet_drag_handle_radius` | **2dp** | 把手圆角（或 `radius_pc_micro` 同级） |
| `sheet_height_half` | **50%** | 默认停靠高度 |
| `sheet_height_expanded` | **100%** | 全屏展开停靠 |
| `sheet_snap_policy` | **`both` \| `half_only` \| `full_only`** | 启用停靠档；默认 `both` |
| `sheet_chrome_full_threshold` | **75%** | Half → Full CenterTitleToolbar morph（`half_only` 时不触发） |
| `transition_sheet` | **300ms** `cubic-bezier(0.4, 0, 0.2, 1)` | TouchPrimary 打开/吸附 / chrome morph |

Chrome 顶区内边距：把手条上下各 **8dp**（`space_sm`）；有 Toolbar 行时 handle 区 + 行合计 ≥ handle hit + `toolbar_height`。

## 实现要点（Compose / XML）

```text
ModalBottomSheet / BottomSheetDialogFragment
├── sheetContainer（radius_mobile_sheet 顶缘）
├── SheetToolbarChrome
│     ├── DragHandle（Half 可见；Full 折叠）
│     ├── HalfToolbarRow?（✕ / title / trail）
│     └── FullToolbarRow（← NavigationIcon / Title / Actions — Full 态显示）
└── content（LazyColumn / 表单）
```

- `skipHalfExpanded = false` 时系统 half 步进应对齐 **`sheet_height_half` 50%**
- 勿在 handle 上方再叠一层装饰 pill
- IME 弹出：优先 **Full** 或全屏 Sheet，避免键盘遮挡唯一确认按钮

## 禁止

| 禁止 | 原因 |
|------|------|
| 把手浮在 chrome 外独立 pill | 破坏 Toolbar 一体性；与详情 Sheet 混淆 |
| PC PointerPrimary 仍用半屏滑入 | 违反 [interaction-language](../interaction-language.md) 平台分流 |
| 全宽拖曳（任意内容区下拉关闭） | 与列表滚动冲突；仅 handle |
| 全屏仍显示 DragHandle / 半屏 ✕ 行 | 须 morph 为 CenterTitleToolbar |
| 全屏 lead 用 ✕ 代替 ← | 破坏子页导航语义 |
| 详情 metadata 用 28dp + 可拖 half | 应走 settings-card-detail-sheet |

## 相关文档

- [settings-card-detail-sheet.md](settings-card-detail-sheet.md) — 详情 / 元数据（16dp、装饰把手）
- [toolbar-list-chrome.md](toolbar-list-chrome.md) — Toolbar 标题与 icon 触区
- [../interaction-language.md §平台原生浮层](../interaction-language.md#平台原生浮层外观)
- [../visual-language.md](../visual-language.md)
