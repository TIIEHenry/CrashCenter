---
title: "跨项目交互设计语言"
type: concept
status: draft
phase: N/A
updated: 2026-06-19
summary: "Clarence 生态交互 SSOT——InputModality、导航、Popup 策略；组件 spec 见 components/。"
---

# 跨项目交互设计语言

> 与 [visual-language.md](visual-language.md) 配对；**UI 模式**见 [ui-modes.md](ui-modes.md)；组件见 [components/INDEX.md](components/INDEX.md)。
> 全部应用共用 token 与交互原则；差异在 **UI 模式分区** 与导航深度，不 fork 按压体系。

## 设计理念

| 理念 | 含义 | 落地要点 |
|------|------|----------|
| **UI 模式** | 内容为主用悬浮 chrome；工具为主用固定壳层 | [ui-modes.md](ui-modes.md) |
| **现代化** | 语义化导航、Edge-to-edge、输入形态自适应 | 系统栏透明 + Insets；Compact 贴边、Expanded 留呼吸边距 |
| **扁平交互** | 扁平 List 无全宽线；详情/设置用 **item 对齐** divider | 应用列表 divider；分割线拉满 Card 外缘 |
| **轻量反馈** | 按压可见但不抢注意力 | 100–180ms 状态过渡；TouchPrimary 才用 expand 动效 |
| **轻量通透** | 无 tonal；PC Panel 8dp；Sheet **实色 layer** | 全局毛玻璃；禁止内容 pill |
| **语义导航** | 按任务域划分路由；**主导航 Tab** 与 **页内分段 Tab** 分层 | CrashCenter：干预 vs 观测 |
| **主导航 Tab** | 任务域（配置 \| 观测）；2–3 个；FloatingBottomNav | 照片 \| 图集 |
| **分段 Tab** | 同页粒度/模式；glass 轨道 + **滑动 pill 指示器** | 年 \| 月 \| 日 |
| **紧凑留白** | 竖屏横向 chrome 只保留核心；次要功能进 overflow / 上下文菜单 | Compact 见 §响应式布局 |
| **跨端等价** | 同一操作在不同输入设备上语义一致 | 右键 ≡ 长按；reselect ≡ 菜单入口之一 |
| **输入形态分流** | 触控 vs 指针决定 popup/hover，非 OS 标签 | `InputModality` |
| **平台原生外观** | PC 上 PC 样式弹窗，手机上手机样式弹窗 | 同语义不同壳；见 §平台原生浮层外观 |

### 设计原则

| 原则 | 说明 |
|------|------|
| **三维度正交** | **布局**（窗口多宽）、**输入形态**（触控/指针）、**手势语义**（用户意图）独立判断，禁止混用 |
| **控件形态分流按压** | ① 行 iOS · ② **仅正圆** ripple · ③ 形填色无 ripple | visual-language §按压反馈 |
| **多触发等价** | 上下文菜单须同时支持：PointerPrimary 右键 + TouchPrimary 长按；Tab 类加 reselect |
| **最小可点高度不降** | `touch_target_min` 40dp；Compact 布局不缩小 |
| **筛选即时生效** | Chip / toggle 变更即过滤；多选至少保留一项 |
| **Overlay 统一出口** | Popup / Dialog / Sheet 经策略层；**容器随平台变、语义不变**；**禁止**内联大 shadow / 大圆角 |
| **Toolbar 稳定** | 顶栏固定；与内容共面、**无全宽**底部分割线 |
| **Compact 减入口** | 竖屏窄宽横向区只放 P0 核心；P2+ 进 ⋮ overflow 或上下文菜单 |

---

## 三正交维度

设计任何可交互组件时，须分别回答三个问题：

```text
LayoutProfile（布局能力）
  → 几栏？侧栏 drawer 还是 pane？isCompact？
        ×
InputModality（输入形态）
  → popup instant 还是 expand？hover 是否启用？
        ×
手势语义（用户意图）
  → 主单击 / 长按 / 右键 / reselect / 悬停 / 快捷键
```

| 维度 | 典型问题 | ❌ 错误做法 |
|------|----------|------------|
| **LayoutProfile** | 侧栏固定还是 drawer？消息列表多宽？ | 用 `sw >= 600` 决定 dropdown 是否 instant |
| **InputModality** | 菜单 instant 还是 expand？ | 用 `Platform == Desktop` 隐藏 Android 长按 |
| **手势语义** | 如何打开同一套菜单？ | Desktop 有右键、触屏无长按 |

---

## 多种触发方式

### 触发方式总表

| 触发 | 输入设备 | 典型场景 | TouchPrimary | PointerPrimary |
|------|----------|----------|--------------|----------------|
| **单击 / 左键** | 手指、鼠标 | 主操作：打开、切换、发送、toggle | ✅ | ✅ |
| **长按** (~450ms) | 手指、触控笔 | 上下文菜单、Tab 更多 | ✅ 主路径 | ✅ 亦支持 |
| **右键** (secondary) | 鼠标、触控板 | 上下文菜单、Tab 更多 | — | ✅ 主路径 |
| **Reselect** | 任意 | 点击**已选中** Tab/项 → 与长按/右键等价的菜单 | ✅ | ✅ |
| **Hover** | 鼠标、触控板 | 露出行内操作栏、预览卡片 | 长按 toggle 替代 | ✅ 主路径 |
| **双击** | 鼠标 | 极少使用；须组件文档显式声明 | 一般不启用 | 可选 |
| **滚轮 / 触控板滑动** | 鼠标、触控板 | 列表/面板滚动 | 手指滑动 | ✅ |
| **键盘快捷键** | 键盘 | Command Palette、全局操作 | 外接键盘时可用 | ✅ 主路径 |
| **IME Action** | 软键盘 | 搜索 Go、表单提交 | ✅ | — |

**生态规则**：凡 PointerPrimary 可用右键触发的 secondary 操作，TouchPrimary **必须**有长按（或 reselect）等价入口，除非组件 spec 写明例外。

### Tab / 列表「三态菜单」

承载「更多操作」（关闭、设置、Pin 等）的 Tab **必须**三态等价，弹出**同一**菜单：

| # | 触发 | 平台 |
|---|------|------|
| 1 | **Reselect** — 点击已选中 Tab | 全平台 |
| 2 | **长按** | 全平台（触屏主路径） |
| 3 | **右键** | PointerPrimary 主路径 |

✕ 关闭等**直接动作**不走菜单，不占用 reselect 槽位。

**Reselect 承担其他语义时**（如「点击已选 Tab = 取消过滤」）：文档须显式声明，且仍须通过**长按 + 右键**提供等价的「更多操作」菜单。

**实现要点**：`pointerInput(selectedId)` 须传入会随选中变化的 key，禁止 `pointerInput(Unit)`（防 stale closure）。

### 菜单锚定（概要）

| 触发 | 锚定 | 详见 |
|------|------|------|
| 长按 / Reselect | `PressCentered` | [§Popup 锚定与翻转](#popup-锚定与视口翻转) |
| 右键 | `AtPointer` | 同上 |

上下文菜单实现：`contextMenuGestures` / `ContextMenuArea` — 完整 Popup 触发矩阵见 [§Popup：触发方式与 PC 模式](#popup触发方式与-pc-模式)。

---

## 输入设备与 InputModality

### 输入形态定义

| 枚举 | 含义 | 典型环境 |
|------|------|----------|
| **TouchPrimary** | 手指为主 | Android 手机、平板触屏 |
| **PointerPrimary** | 鼠标/触控板/键盘效率为主 | Desktop、Web、Android PC (DeX/Chromebook) |

### 解析规则

```text
Desktop / Web          → PointerPrimary
Android + PC 形态      → PointerPrimary   （DeX、FEATURE_PC、桌面模式）
Android + 非 PC 形态   → TouchPrimary     （手机、触屏平板）
```

**Android PC 判定信号**（满足其一即为 PC 形态）：

| 信号 | 说明 |
|------|------|
| `FEATURE_PC` | Chromebook、Android on PC |
| 桌面 / 自由窗口模式 | DeX 等 |
| 物理键盘已连接 | 辅助信号，不可单独作为唯一条件 |

**禁止**：仅用 `sw >= 600dp` 判定 PointerPrimary（会误伤触屏平板横屏）。

### 触控 vs 鼠标 — 呈现差异

同一「打开上下文菜单」意图，**菜单内容相同**，呈现可不同：

| 属性 | TouchPrimary | PointerPrimary |
|------|--------------|----------------|
| Popup 进场 | fade + expandVertical **300ms** | **instant** |
| 阴影 | ≤ **4dp** 或 glass | PC 小菜单 **0dp+outline**；Panel **8dp+outline** |
| Hover 操作栏 | 不依赖；**长按 toggle** | **`hoverable`** 露出 |
| 行级按压 | overlay（与 Modality **无关**） | 同左 + hover 叠加 |
| 滚轮 | 手指滑动 | 滚轮 / 触控板 |
| 按钮高度 | 触控区 ≥ 40dp | Web 24–32dp 指针优先 |

**禁止**：`if (Platform == Android)` 作为 popup 动效或 hover 的唯一分支。

### 单次指针事件 API（调试 / 增强）

| 层级 | API | 用途 |
|------|-----|------|
| Compose | `PointerType.Touch` / `.Mouse` / `.Stylus` | 区分手指与鼠标 |
| Compose | `isSecondaryPressed` | 右键 |
| Android View | `MotionEvent.getToolType()` | FINGER / MOUSE |
| Android View | `InputDevice.SOURCE_*` | 触摸屏 vs 鼠标 |

事件级 API 用于 hover、滚动、埋点；**popup 默认值**由 Theme 注入的 `InputModality` 决定。

---

## 平台与运行环境分类

### 平台栈

| 平台 | UI 栈 | InputModality 默认 | 手势 SSOT |
|------|-------|-------------------|-----------|
| **Android View/XML** | Material 3 DayNight | 视 PC 形态 | 本文 + Singular mobile-interaction-guidelines |
| **Compose KMP** | Material 3 + 自定义 Indication | `resolveInputModality()` | UniverseAgent pointer-interaction |
| **Web TUI v2** | SolidJS + CSS token | PointerPrimary | TUI keybind-v2 / menu-v2 |
| **Desktop (JVM)** | Compose / 宿主 | PointerPrimary | 同 KMP |
| **IDE Plugin** | 嵌入 Singular Shell | PointerPrimary | Singular Action 系统 |

### 窗口尺寸分级（LayoutProfile）

与输入形态**独立**；决定布局，不决定 popup 动效：

| 分级 | 约略条件 | 布局倾向 | 主操作位置 |
|------|----------|----------|------------|
| **Compact width** | 宽 < 600dp | 单列、overflow | ActionBar 图标 (P0) |
| **Medium width** | 600dp ≤ 宽 < 840dp | 单列或局部双栏 | ActionBar |
| **Expanded width** | 宽 ≥ 840dp | 双栏/三栏 | ActionBar 图标+文本 |
| **Compact height** | 屏高 < 420dp 或 IME | 禁止底部唯一确认 | 顶部固定入口 |

Singular Shell 三档 UI 模式映射：手机竖屏 / 手机横屏 / PC·平板·折叠内屏 — 详见 Singular `mainmenu-responsive-presentation`。

### 设备 × 形态手测矩阵

| 场景 | Layout | InputModality | 重点验证 |
|------|--------|---------------|----------|
| Android 手机竖屏 | Compact | TouchPrimary | 长按菜单、M3 展开 |
| Android 平板竖屏 | Medium | TouchPrimary | 同手机；侧栏可为 pane |
| Android 平板横屏（纯触屏） | Medium/Expanded | TouchPrimary | **仍** Material 动效，非 instant |
| Chromebook / FEATURE_PC | Expanded | PointerPrimary | instant + 描边 |
| Samsung DeX | Expanded | PointerPrimary | 右键、hover、快捷键 |
| JVM Desktop | Expanded | PointerPrimary | 右键、Command Palette |
| Web 浏览器 | 视窗口 | PointerPrimary | focus-visible、右键 |
| Android 模拟器 + 鼠标 | 视窗口 | 可能误判 | **以真机为准** |

---

## 响应式布局与功能密度

竖屏与 **Compact width**（宽 < 600dp）下，横向空间是最稀缺资源。**横向 chrome（Toolbar、筛选行、TabBar、Action 条）只保留当前任务的核心入口**，其余功能降级到 **overflow 菜单（⋮）** 或 **上下文菜单（长按 / 右键）**，而非继续堆图标或缩小触控热区。

> 分割线组件：[components/shared-dividers.md](components/shared-dividers.md)。本节定义**功能入口**如何随布局收缩。

### 功能优先级（Action Tier）

| 层级 | 含义 | Compact 竖屏 | Expanded / PC |
|------|------|--------------|---------------|
| **P0 核心** | 当前上下文唯一主操作 | **始终可见**（Toolbar 右侧 1 个图标或 Chip） | 图标或图标+文本 |
| **P1 上下文** | 与当前 Tab/选中项强相关 | 可见 1–2 项，或合并为单入口 | 内联 Toolbar / 筛选行 |
| **P2 辅助** | 筛选扩展、排序、刷新、设置 | **overflow ⋮** 或行内上下文菜单 | 可内联；仍可无文字图标 |
| **P3 低频** | About、导出、高级选项 | **仅** overflow / 长按菜单 / 设置页 | Toolbar 次级或菜单 |
| **P4 危险/撤销** | 删除、重置 | Dialog 二次确认；不进 Toolbar 主区 | 同左 |

**原则**：P0 不得只存在于 overflow；P3 不得占用 P0 的横向位。

### 竖屏 Compact 横向 chrome 规则

| 区域 | 保留（P0–P1） | 移入 overflow / 上下文菜单（P2+） |
|------|---------------|-----------------------------------|
| **Toolbar** | 标题、导航返回、**1 个主操作** | 排序、About、批量、第二主操作 → `menu_main` ⋮ |
| **筛选行** | 最高频 2–3 个 Chip 或 1 组 toggle | 次要筛选、标签多选 → 行尾 ⋮ 或「更多筛选」Sheet |
| **TabBar** | Tab 标签本身 | Tab 关闭/Pin/设置 → **三态上下文菜单**（reselect / 长按 / 右键） |
| **列表行** | 行主操作（toggle / 进入详情） | 次要操作 → 行 **长按/右键** 菜单，非常驻图标列 |
| **ToolWindow Header** | 标题 + 关闭 | 其余 Action → overflow（Singular LeftMenu/RightMenu 模式） |

**禁止**：

- 竖屏 Toolbar 横排 **4 个以上** 图标按钮（应合并为 ⋮）
- 为塞功能而把图标缩到 < 32dp 且无 TouchDelegate
- 把 P3 功能放在 bottom tab（应用级 Tab 只承载任务域，不承载设置项堆砌）
- 仅依赖 hover 发现功能（触屏无 hover）

### 与 Popup 的联动

Compact 从横向区移除的功能，**必须**仍可通过 Popup 触达，且遵守 [§Popup：触发方式与 PC 模式](#popup触发方式与-pc-模式)：

| 原位置 | Compact 降级目标 | 触发 |
|--------|------------------|------|
| Toolbar 多余图标 | **Overflow Dropdown** | 单击 ⋮ |
| Tab 关闭 / 设置 | **Tab 上下文菜单** | reselect / 长按 / 右键 |
| 列表次要操作 | **行上下文菜单** | 长按 / 右键 |
| 批量 / 排序 | **Overflow 或 Toolbar 子菜单** | ⋮ → 子项 |
| 高级筛选 | **BottomSheet 或 Dropdown** | 「更多筛选」入口 |

**发现性**：Shell 级入口（搜索、主菜单、消息）不可**仅**靠长按；须有可见 affordance（图标、Chip、⋮）。

### 按 LayoutProfile 的布局预期

| Profile | 宽度/高度 | 横向 chrome | 次要功能 | 导航 |
|---------|-----------|-------------|----------|------|
| **手机竖屏** | Compact width | Toolbar：标题 + 1 主操作 + ⋮ | overflow + 行长按 | 单栏；底栏 2–3 tab |
| **手机横屏** | Compact height | 同竖屏；**禁止**底部唯一确认 | overflow 优先 | 顶栏主操作 |
| **平板竖屏** | Medium width | 可 2–3 Toolbar 图标或 Chip 全展 | 部分可内联 | 可选侧栏 pane |
| **平板横屏 / PC** | Expanded | 图标+文本；筛选行可全展 | 内联 + 快捷键 | 双栏/三栏 |

宽度与高度信号冲突时，以**更受限**方向为准（如矮横窗按 Compact height 处理）。

### 响应式 Toolbar 模式（KMP 参考）

UniverseAgent `ToolbarLayout`（600dp 断点）：

```text
isCompact = width < 600dp

Standard:  [Nav] Title … [action₁][action₂][action₃]
Compact:   [Nav] Title … [action₀ 主操作][⋮ overflowActions]
```

overflow 内项与 Standard 内联项**同一 Action 模型**，仅 presentation 不同。

### 工具 App 示例

**CrashCenter（NeverCrash）Compact 建议**：

| 功能 | 当前 | Compact 目标 |
|------|------|--------------|
| 搜索 | 常显 | 可折叠为图标（AppSnapShotor 模式） |
| 过滤 Chip | 全展 | 保留 3 个单选 Chip；`show_system` 等进 ⋮ 或二级 |
| 排序 | Toolbar 子菜单 | 保留在 ⋮（P2） |
| About | Toolbar 菜单 | 保留在 ⋮（P3） |
| 应用行 toggle | 行点击 | 保持；scope 说明 → **长按** Dialog |

**AppSnapShotor**：竖屏 portrait；搜索可折叠；底栏 3 tab 为核心任务域；系统/用户 filter 用图标 toggle 而非横排多 Chip。

**Singular IDE**：Compact width 下 LeftMenu/RightMenu **横向滚动或折叠进 MainMenu/overflow**；ToolWindow 改 overlay drawer，不长期挤压编辑器。

### 布局 × 交互决策表

新增或改版横向 chrome 时：

| # | 问题 | Compact 答案 |
|---|------|--------------|
| 1 | 该功能是 P0 还是 P2+？ | P0 才进 Toolbar 主区 |
| 2 | 竖屏能否 ≤3 个横向控件？ | 否 → 移 overflow / 菜单 |
| 3 | 移走后如何触达？ | 指定 ⋮ / 长按 / reselect |
| 4 | 触屏能否无 hover 完成？ | 必须 |
| 5 | Expanded 是否还原内联？ | 是，同一 Action 源 |

### 合规检查（布局向）

- [ ] Compact 竖屏 Toolbar 可见操作 ≤ **2**（含 ⋮ 自身不算操作项）
- [ ] overflow 与内联项共用同一状态/启用逻辑
- [ ] 从 Toolbar 移除的项在 overflow 或上下文菜单可找到
- [ ] 筛选行横向不滚动超过 **1 屏**；超出项进「更多」
- [ ] 列表行非常驻 **>2** 个图标按钮

---

## 平台原生浮层外观

**同一业务语义，不同平台用不同「壳」**：用户在 PC 上应看到 **桌面式弹窗**，在手机上应看到 **移动端式浮层**。由 `InputModality` + `LayoutProfile` 决定外观，**禁止**全平台共用一种 Material 手机弹窗。

```text
业务语义（确认 / 选择 / 设置 / 列表 Picker）
        │
        ├── TouchPrimary（手机、触屏平板）
        │     → 手机样式：BottomSheet、底栏按钮、**轻量 glass / outline**、expand 动效
        │
        └── PointerPrimary（PC、Web、DeX、Chromebook）
              → PC 样式：居中 Dialog、顶栏操作、instant、描边、可拖拽面板
```

**手势语义仍跨端等价**（右键≡长按）；变的只是**容器形态与动效**，不是菜单项内容。

### 手机样式（TouchPrimary）

用户预期接近 **Material 3 移动端 / iOS Sheet**：

| 浮层类型 | 容器 | 外观与动效 | 主操作位置 |
|----------|------|------------|------------|
| 轻量菜单 | Dropdown / Popup | **glass + 描边** 或 0dp outline；expand **300ms**；`PressCentered` | — |
| 短确认 | `AlertDialog` / 居中 Dialog | 宽 `85%`；**`radius_mobile_dialog` 16dp**；无 tonal | 底部按钮行 |
| 长列表 Picker | **ModalBottomSheet** | 顶缘 **`radius_mobile_sheet` 28dp** | **ActionBar 图标**（P0） |
| 半屏任务 / 可拖子页 | **Draggable Half Sheet** | 顶缘 **`radius_mobile_sheet` 28dp** + chrome 内 **DragHandle** | Toolbar trail / 内容内 |
| 详情 / 元数据 | **BottomSheet** | 顶缘 **`radius_mobile_sheet_detail` 16dp** + 实色 layer | 底栏操作 |
| 复杂表单 | BottomSheet 或全屏 Sheet | IME 时尽量全屏；避免底部唯一确认 | ActionBar |
| 命令/搜索 | 全宽 Overlay | 自顶或居中；**半透明 layer + blur** | — |
| Toast | 底/顶 snackbar 风格 | ≤ **4dp** shadow（无 glass 时）；**无 tonal** | — |

**禁止（手机）**：无滑入的 PC 小窗、仅顶栏按钮无底部可达的确认（半屏 Sheet 时）。

### PC 样式（PointerPrimary）

用户预期接近 **VS Code / Cursor / Fluent 桌面**：

| 浮层类型 | 容器 | 外观与动效 | 主操作位置 |
|----------|------|------------|------------|
| 上下文菜单 | Dropdown | **instant**；**`radius_pc_overlay` 0dp** + 1dp outline | — |
| 短确认 | **居中 Dialog**（Wide 模式） | **`radius_pc_dialog` 0dp**；顶栏操作 | 顶栏 Cancel / OK |
| 长列表 Picker | **Dialog**（非 BottomSheet） | 可滚动内容区；instant 打开 | 顶栏或 Dialog 正向按钮 |
| 管理/工具面板 | **DraggableFloatingPanel** / L3 伪窗口 | **`radius_pc_panel` 0dp** + 8dp shadow + 1dp outline；可拖曳 | 面板顶栏 |
| Command Palette | Overlay | instant + outline；居中宽面板 | 键盘 |
| Toast | 角部通知 | outline；无 expand | — |

**禁止（PC）**：BottomSheet 自底滑入、Material 手机 expand 菜单、仅底部确认且不在顶栏的半屏流。

### 对照总表

| 维度 | 手机 TouchPrimary | PC PointerPrimary |
|------|-------------------|-------------------|
| **代表容器** | BottomSheet、M3 Dialog | 居中 Dialog、FloatingPanel |
| **打开动效** | slide / expand 300ms | **instant** |
| **阴影** | ≤ **4dp**；无 tonal | 小菜单 **0dp+outline**；Panel **8dp+outline** |
| **圆角** | Dialog **16dp**；Modal Sheet **28dp**；详情 **16dp** | **`radius_pc_panel` 0dp**（直角窗体） |
| **按钮布局** | 内容在上，**按钮在底** | **标题栏 + 操作在顶** |
| **上下文菜单** | 长按 | 右键 |
| **Dismiss** | 下滑 / 返回键 | Escape / 点外部 |
| **Hover** | 无 | 有 |

### Adaptive 双模式（Compose KMP 参考）

`AdaptiveDialog` 按**对话框可用宽度**切换壳，与 InputModality 正交但常重合：

| 模式 | 条件 | 风格 |
|------|------|------|
| **Compact** | 宽 < 600dp | **手机**：内容 → 分割线 → 底栏右对齐按钮 |
| **Wide** | 宽 ≥ 600dp | **PC**：标题 + 顶栏按钮 → 分割线 → 可滚动内容 |

PointerPrimary 下的 L3 管理类面板（设置、Skills、Changelog 等）可走 **DraggableFloatingPanel**，TouchPrimary 仍用 AdaptiveDialog / BottomSheet。

**FilePicker 双形态**：Compact → `ModalBottomSheet`；Wide → `Dialog`（UniverseAgent `filepicker-dual-host`）。

### 选型决策树

```text
需要模态浮层？
├── 仅选项列表 / 上下文操作 → Dropdown（Policy 分流动效）
├── 长列表 / 目录 Picker
│     ├── TouchPrimary → BottomSheet
│     └── PointerPrimary → Dialog（可滚动）
├── 短确认 / _alert_
│     ├── TouchPrimary → AlertDialog（底栏按钮）
│     └── PointerPrimary → Wide Dialog（顶栏按钮）
├── 复杂管理 UI
│     ├── TouchPrimary → BottomSheet 或全屏 Sheet
│     └── PointerPrimary → DraggableFloatingPanel / Wide Dialog
└── 全局命令 → Overlay（Palette）
```

### 工具 App（XML / 单端 Android）

CrashCenter、AppSnapShotor 当前仅 TouchPrimary，**默认手机样式**即可：

| 场景 | 容器 |
|------|------|
| 说明 / 确认 | `AlertDialog` |
| 长列表 / About / 忽略应用 | `BottomSheetDialogFragment` |
| 上下文 | overflow ⋮ 或长按 |

若未来支持 DeX / `FEATURE_PC`，须切 **PointerPrimary 分支**：同一 `About` 改为 `Dialog` + 顶栏操作，**不得**仍用 BottomSheet。

### 禁止模式（平台外观）

| 禁止 | 原因 |
|------|------|
| PC 端用 BottomSheet 滑入 | 违反桌面预期 |
| 手机端用 instant 无动效小窗作唯一 Picker | 缺少移动端 affordance |
| 全平台同一 Dialog 布局（仅底栏按钮） | 宽屏/PC 操作应在顶栏 |
| 用 `Platform.OS` 而非 `InputModality` 分支 | Android PC 误判 |
| 宽屏仍 `fillMaxWidth(0.85f)` 无 max 宽 | 桌面 Dialog 过宽 |

### 组件 spec 补充（平台外观）

```markdown
### 平台浮层外观

| | TouchPrimary（手机） | PointerPrimary（PC） |
|--|---------------------|---------------------|
| 容器 | BottomSheet / M3 Dialog | Dialog Wide / FloatingPanel |
| 动效 | expand / slide | instant |
| 主操作 | 底栏 或 ActionBar | 顶栏右侧 |
| Dismiss | 返回 / 下滑 | Escape / 点外部 |
```

---

## Popup：触发方式与 PC 模式

Popup 是交互文档的核心：**同一浮层**须定义「谁触发、在哪锚定、Touch/PC 如何呈现」。业务禁止直写 Material 默认 shadow/expand，须经策略层出口。

### Popup 类型

| 类型 | Tier | 典型用途 | 模态 |
|------|------|----------|------|
| **Dropdown / 上下文菜单** | `Dropdown` | Tab 更多、Session 行、树节点 | 非模态 |
| **Anchored Card** | `Card` | Slash 命令、补全、小面板 | 非模态 |
| **Overlay** | `Overlay` | Command Palette、快捷键帮助 | 半模态 |
| **Hover 预览** | `Hover` | Session 卡片、消息操作栏 | 非模态 |
| **Toast** | `Toast` | 轻通知 | 非模态 |
| **FloatingPanel** | `FloatingPanel` | PC 可拖拽伪窗口 | 非模态 |
| **Dialog** | —（独立容器） | 确认、表单 | 模态 |
| **BottomSheet** | — | Picker / **详情元数据**（实色 + item 对齐 divider） | 半模态 |
| **Draggable Half Sheet** | — | 半屏任务区；**chrome 内 DragHandle** 拖曳 expand/collapse/dismiss | 半模态 |

**半屏拖曳**：TouchPrimary 下 [draggable-half-sheet.md](components/draggable-half-sheet.md) 规定把手命中区、50% 默认高度、**Full 态 morph 为 CenterTitleToolbar（← 返回）** 与 scrim tap 关闭；详情 metadata 仍走 [settings-card-detail-sheet.md](components/settings-card-detail-sheet.md)（装饰把手、16dp 顶缘）。

### 触发方式 × Popup 类型

| 触发 | Dropdown 菜单 | Card 弹层 | Overlay | Hover | Dialog/Sheet |
|------|---------------|-----------|---------|-------|--------------|
| **单击** | ✅ overflow / 筛选按钮 | 输入 `/` 触发 | — | — | 按钮打开 |
| **长按** (~450ms) | ✅ 触屏主路径 | — | — | — | — |
| **按住滑动选单** | ✅ 锚点按钮（见 [§按住滑动选单](#按住滑动选单-press-drag-release)） | — | — | — | — |
| **右键** | ✅ PC 主路径 | 可选 | — | — | — |
| **Reselect** | ✅ Tab 已选项 | — | — | — | — |
| **Hover 进入** | — | — | — | ✅ PC 露出 | — |
| **Hover 离开** | — | — | — | 关闭/延迟关闭 | — |
| **快捷键** | — | — | ✅ Ctrl+K / `/` | — | — |
| **Escape** | 关闭 | 关闭 | 关闭 | 关闭 | 取消/关闭 |
| **程序调用** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **返回键 / 下滑** | 关闭 | 关闭 | 关闭 | — | Sheet 关闭 |

**等价规则**：Dropdown 类 Popup 在 PointerPrimary 以**右键**为主路径，TouchPrimary 以**长按**为主路径；Tab 类额外加 **Reselect**。两种 Modality 弹出**相同 menuItems**，仅锚定与动效不同。

### ContextMenuTrigger 三态（Dropdown 专用）

Compose KMP 枚举（`ContextMenuGestures.kt`）：

| 枚举 | 手势来源 | 指针坐标 | 锚定 |
|------|----------|----------|------|
| `LongPress` | `detectTapGestures(onLongPress)` | **必须** | `PressCentered` |
| `RightClick` | `tabContextMenu` / `isSecondaryPressed` | **必须** | `AtPointer` |
| `Reselect` | 点击已选中 Tab 的 `onTap` | 可选；无则取 anchor 几何中心 | `PressCentered` |

```text
openMenu(trigger, pointer)
    → resolveContextMenuPlacement(trigger, pointer, anchor, …)
    → AppDropdownMenu(placement = …)
```

**Tab 组合 Modifier**（须带 `selectedId` key）：

```text
.appTabItemGestures(tabId, isSelected, …)
    onPrimaryTap  → 未选中：切换；已选中：Reselect 开菜单
    onContextMenu → LongPress | RightClick
```

**列表/卡片行**（无 Reselect）：

```text
.sessionItemGestures(onClick, onContextMenu)
    onTap         → 主操作
    onLongPress   → LongPress 开菜单
    tabContextMenu→ RightClick 开菜单
```

**禁止**：行外包普通 `clickable` 吞掉 `onLongPress`；`pointerInput(Unit)` 导致 reselect  stale。

### PC 模式（PointerPrimary）专章

**何时进入 PC 模式**（`InputModality.PointerPrimary`）：

| 环境 | 条件 |
|------|------|
| JVM Desktop / IDE Plugin | 始终 |
| Web TUI | 始终 |
| Android + `FEATURE_PC` | Chromebook 等 |
| Android + 桌面/DeX 模式 | 厂商桌面 flag |
| Android 手机/触屏平板 | **否**（即使横屏 ≥600dp） |

PC 模式影响 **Popup 呈现与 Hover**，不影响行级 overlay 按压。

#### PC 模式下的 Dropdown 行为

| 属性 | PC（PointerPrimary） | 触屏（TouchPrimary） |
|------|----------------------|----------------------|
| 主触发 | **右键** top-start 对齐指针 | **长按** 按压点居中 |
| 辅触发 | 长按、Reselect（Tab） | Reselect（Tab）、外接鼠标右键 |
| 进场动效 | **instant**（无 expand） | fade + expandVertical **300ms** |
| 阴影 | **0dp+outline**（小菜单） | Tier 决定（Dropdown ≤ **4dp**） |
| 描边 | **1dp** `outlineVariant` ~45% | 无 |
| 菜单锚定 | `AtPointer(offset)` | `PressCentered(offset)` |
| Hover 辅助 | 行内操作栏 `hoverable` | 长按 toggle 替代 |
| 菜单行高 | Compact token | 同左；触屏仍 ≥36dp |

#### PC 模式下的其他 Popup

| Tier | PC 呈现 | 典型触发 |
|------|---------|----------|
| **Card** | instant + outline | 输入框 `/`、补全 |
| **Overlay** | instant + outline | `Ctrl+K` / Command Palette |
| **Hover** | instant + outline | 鼠标移入 Session 行 |
| **FloatingPanel** | **`radius_pc_panel` 0dp** + 8dp shadow + outline + 可拖拽 | PC 专用伪窗口（UA） |
| **Toast** | outline（无 expand） | 程序 |
| **Dialog** | 标准 Dialog；Instant 菜单与之独立 | 按钮 / Action |

#### PC 键盘与 Popup

| 按键 | 行为 |
|------|------|
| **Escape** | 关闭最顶层非模态 Popup / 取消 Dialog |
| **Ctrl+K / /** | 打开 Command Palette（Overlay） |
| **Enter** | Dialog 默认确认（若焦点在确认按钮） |
| **↑↓** | 菜单项 / Palette 列表导航 |
| **Tab** | Dialog 内焦点循环 |

Dropdown 类菜单打开时，须 `PopupProperties(focusable = …)` 按场景配置（聊天输入栏打开时常用 `focusable=false` 防抢 IME 焦点）。

#### Android PC（DeX / Chromebook）与 Desktop 对齐

DeX 接显示器、Chromebook `FEATURE_PC` 与 JVM Desktop **同一套 PointerPrimary Policy**：

- 右键上下文菜单 instant + 描边
- Hover 可用
- **仍保留** TouchPrimary 长按入口（接触控屏时）
- 不得因 `PlatformClass.Android` 回退 Material expand 动画

手测重点：DeX 窗口缩放、Chromebook 触屏+触控板混合、外接鼠标。

### TouchPrimary 模式下的 Popup

| 属性 | 值 |
|------|-----|
| Dropdown 分层 | **glass + 描边** 或 0dp outline |
| Card 分层 | 0dp outline 或 ≤ **2dp** shadow |
| Overlay 分层 | **半透明 + blur**；shadow **0dp** |
| Hover 分层 | outline；≤ **4dp** shadow |
| Toast | ≤ **4dp** shadow；**无 tonal** |
| 圆角 | 手机 Dialog **16dp** / Sheet **28dp**（见 visual-language §平台分档） |
| 动效 | `fadeIn(300) + expandVertically(300)` 进；对称退出 |
| 锚定 | 长按 / Reselect → `PressCentered`；overflow **单击** → `AnchorBelow`；无右键主路径 |

### 按住滑动选单（Press-Drag-Release）

相册 **Trail 筛选钮（≡）** 等 **锚点 Dropdown** 的触屏增强：手指 **不抬起** 即可完成「开菜单 → 选行 → 确认」，减少拇指往返。

#### 手势状态机

```text
                    ┌─────────────────────────────────────┐
                    │  OPEN_IDLE（菜单已开，常规点击态）    │
                    └───────────────┬─────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
   单击锚点                    长按锚点                    点菜单外 / 返回
   开菜单                      开菜单 + 进入 DRAG              关闭
        │                           │
        └───────────────┬───────────┘
                        ▼
              ┌─────────────────┐
              │  DRAG_SELECTING  │  指针在菜单内移动 → 跟选行
              └────────┬────────┘
                       │
         ┌─────────────┴─────────────┐
         ▼                           ▼
   松手在菜单行上                 松手在菜单外
   → 触发该行 action              → 关闭，无 action
   → 关闭菜单
```

#### 路径 A · 从锚点按住（主路径 · 相册）

| 步骤 | 行为 |
|------|------|
| 1 **按下** Trail / overflow 钮 | 开始追踪；**~450ms** 或产品配置 **即时**（按下即开，相册型） |
| 2 **菜单展开** | 手指 **仍按住**；Popup `PressCentered` / 锚点下方；expand **300ms** 可与按住并行 |
| 3 **滑动** | 指针移入 Popup → **hitTest 最近行** → 更新 `highlightedIndex` |
| 4 **行按压反馈** | 当前落点行 **① iOS overlay**（100ms）；**仅一行**高亮 |
| 5 **松手在行内** | 执行该行 **primary action**（跳转 / toggle / 打开子菜单）；关闭 Popup |
| 6 **松手在 Popup 外** | **仅关闭**；不触发任何项 |

**关键**：从锚点按下到松手视为 **同一 pointer 会话**；菜单须 `pointerInput` 接棒，勿因展开动画丢失 track。

#### 路径 B · 单击打开 + 按住滑动（增强 · P1）

| 步骤 | 行为 |
|------|------|
| 1 **单击**锚点 | 开菜单 → `OPEN_IDLE` |
| 2 **常规** | 再 **点** 某行 → 触发并关闭 |
| 3 **增强** | 在某行 **按下不抬** → 进入 `DRAG_SELECTING` → **滑动** 改 highlighted 行 → **松手** 触发落点行 |

路径 B **可选实现**；路径 A 为相册 overflow **推荐默认**。

#### 与单击、长按的优先级

| 手势 | 结果 |
|------|------|
| 短按锚点（< 长按阈值）并抬起 | **单击** → 开菜单，`OPEN_IDLE` |
| 长按锚点不滑动 | 开菜单；松手在锚点上 **不** 误触首行（首行高亮需指针进入 Popup bounds） |
| 长按 + 滑入菜单 | `DRAG_SELECTING` |
| 已开菜单 + 点外部 | 关闭 |

#### 菜单行视觉（跟选态）

| 属性 | 规范 |
|------|------|
| 跟选高亮 | **① iOS overlay**；非 ripple 全宽 |
| 已选 checkmark（筛选 Popup） | **松手前** 不变；松手 toggle 后再刷新 ✓ |
| 分隔线 | 高亮行仍遵守 [shared-dividers.md](components/shared-dividers.md) item 对齐 |
| Master 行 | 可跟选；松手 = 应用 Master 语义 |

#### PointerPrimary 对齐

| 输入 | 行为 |
|------|------|
| **单击**锚点 | 开菜单；**单击**行确认 |
| **Hover** | 行 `hoverable` 预高亮（等同跟选视觉，无 overlay 亦可） |
| **MouseDown + drag** | ⚪ 可选与路径 A 等价（IDE 产品推荐） |
| **右键** | 仍走 `AtPointer` **上下文**菜单；与 overflow **单击 Dropdown** 不同入口 |
| **↑↓ + Enter** | 菜单 focusable 时键盘导航 |

TouchPrimary **不要求** Hover；跟选仅 pointer 落点驱动。

#### 实现要点

```text
anchorButtonGestures(
  onClick       → openMenu(OPEN_IDLE)
  onLongPress   → openMenu(DRAG_SELECTING)   // 或 onPress 即时
  onDrag        → if (menuOpen) updateHighlight(hitTest)
  onRelease     → if (highlight != null) perform(highlight) else dismiss()
)
```

| 禁止 | 原因 |
|------|------|
| 松手在行外仍触发最后一行 | 须 cancel |
| 跟选无按压反馈 | 用户不知落点 |
| 展开动画结束才接 pointer | 按住滑动断链 |
| 跟选时用 ripple 铺满 Popup | 违反 ① 行按压 |

组件级（筛选多选、Master 行）：[popup-filter-menu.md §按住滑动](components/popup-filter-menu.md#按住滑动选单-press-drag-release)

### Popup 锚定与视口翻转

`resolveContextMenuPlacement` 逻辑（KMP SSOT）：

```text
RightClick(pointer)
  └─ AtPointer: 菜单 top-start = 指针 local 坐标

LongPress(pointer) / Reselect(pointer?)
  └─ PressCentered:
        1. 菜单水平中心对齐 press.x（clamp 在 anchor 宽内）
        2. 估算 menuHeight（itemCount × minTapHeight + padding）
        3. 优先在按压点下方 4px 展开
        4. 下方空间不足 →  flip 到上方
        5. 上下都不足 → 选空间较大侧
        Reselect 无 pointer → defaultReselectPressPoint(anchor 中心)
```

**必备上下文**：`onGloballyPositioned` → `ContextMenuAnchorMetrics`；`LocalWindowInfo` 视口高度。

### Popup 菜单分组与 Master 行

完整 spec（相册筛选参考、Master 语义、`popup_section_gap`）见 **[components/popup-filter-menu.md](components/popup-filter-menu.md)**。

要点：item 对齐 divider；Master 行与 filter 区 **`popup_section_gap`** 分隔；简单菜单 ≤4 项可组内无线。

### PopupPresentationPolicy 数值

由 `InputModality` 注入 `LocalPopupPresentationPolicy`（非 `Platform` 标签）：

| Tier | TouchPrimary 分层 | TouchPrimary 动效 | PointerPrimary |
|------|-------------------|-------------------|----------------|
| Dropdown | glass / outline；≤ **4dp** | expand 300ms | 0dp + outline, instant |
| Card | outline；≤ **2dp** | expand 300ms | 0dp + outline, instant |
| Overlay | blur + layer；**0dp** | expand 300ms | 0dp + outline, instant |
| FloatingPanel | **`elevation_panel` 8dp** + outline | expand 300ms | **8dp** + outline, instant |
| Hover | outline；≤ **4dp** | expand 300ms | 0dp + outline, instant |
| Toast | ≤ **4dp**；无 tonal | expand 300ms | 0dp + outline, instant |

**实现出口**：

| 栈 | 出口 |
|----|------|
| Compose KMP | `SingularityPopupSurface(tier)`、`SingularityAnchoredDropdownMenu`、`ContextMenuArea` |
| Singular View | `DialogManagerImpl`、`*DialogBuilder` |
| 工具 App XML | `AlertDialog` / `BottomSheetDialogFragment`（简化；Dropdown 待 Policy 对齐） |

### Popup 决策流

```text
需要浮层？
├── 模态确认/表单 → Dialog（Compact 竖屏可 BottomSheet）
├── 任务列表 Picker → BottomSheet + ActionBar 主操作
├── 上下文操作列表 → Dropdown
│     ├── Tab？ → 三态：Reselect + 长按 + 右键
│     └── 行/卡片？ → 长按 + 右键（无 Reselect）
├── 全局命令 → Overlay（Command Palette）
├── 指针悬停预览 → Hover（仅 PointerPrimary；触屏用长按 toggle）
└── 只读提示 → Toast

Dropdown 打开后：
  resolveInputModality()
    ├── PointerPrimary → AtPointer / instant / outline
    └── TouchPrimary   → PressCentered / expand / glass·outline
```

### 组件 spec 模板（Popup 一节）

每个含 Popup 的组件 spec 须包含：

```markdown
### Popup 交互

| 触发 | PointerPrimary (PC) | TouchPrimary |
|------|---------------------|--------------|
| 打开上下文菜单 | 右键 → AtPointer | 长按 ~450ms → PressCentered |
| … | … | … |

- **Tier**：Dropdown / Card / …
- **Policy**：经 SingularityPopupSurface / DialogManager
- **分组**：`MenuSection` / 逻辑段；组内 divider；Master 行前 **`popup_section_gap`**
- **Dismiss**：点击外部 / Escape / 返回键
- **Reselect 例外**：（若有）须声明且保留长按+右键菜单
```

### 已落地参考（UniverseAgent）

| 组件 | Reselect | 长按 | 右键 | 文件 |
|------|----------|------|------|------|
| EngineTabRow | ✅ | ✅ | ✅ | `SessionListScreen.kt` |
| AppTabItemView | ✅ | ✅ | ✅ | `AppTabBar.kt` |
| SessionCard | — | ✅ | ✅ | `sessionItemGestures` |
| ContextMenuArea | — | ✅ | ✅ | `ContextMenu.kt` |

---

## Popup 呈现分流（速查）

> 详细触发与 PC 模式见上一节。本节为 Policy 速查。

业务须经策略层出口，禁止内联大 shadow / 大圆角 / expand。

### 浮层 Tier

| Tier | 用途 | TouchPrimary | PointerPrimary |
|------|------|--------------|----------------|
| **Dropdown** | 上下文菜单 | glass / outline + expand | outline + instant |
| **Card** | 命令补全、Slash | outline；≤ **2dp** | outline + instant |
| **Overlay** | Command Palette | blur + layer；**0dp** | outline + instant |
| **Hover** | 预览卡片 | outline；≤ **4dp** | outline + instant |
| **Toast** | 通知 | ≤ **4dp**；无 tonal | outline + instant |
| **FloatingPanel** | PC 拖拽面板 | outline；≤ **4dp**（触屏） | **`elevation_panel` 8dp** + outline |

### 容器选型（Dialog / Sheet）

| 窗口状态 | 推荐容器 | 主操作位置 |
|----------|----------|------------|
| 手机竖屏 Compact | BottomSheet（长列表） | ActionBar 图标 |
| 平板/PC 宽屏 | Dialog | ActionBar 或 Dialog 正向按钮 |
| 轻量菜单 | Popup / Dropdown | — |

---

## 组件作者决策表

新增可交互 / 浮层组件时按序回答：

| # | 问题 | 查什么 | 落地 |
|---|------|--------|------|
| 1 | 布局随窗口如何变？ | `LayoutProfile` / `WindowSizeClass` | 断点、pane/drawer |
| 2 | 主输入是触控还是指针？ | `InputModality` | Policy，非 OS 二分 |
| 3 | 各端手势是否等价？ | §多种触发方式 / §Popup | spec 写手势对照表 |
| 4 | Compact 是否减入口？ | §响应式布局 | P2+ 进 overflow/菜单 |
| 5 | Popup 走哪条动效与**平台壳**？ | §平台原生浮层外观 / §Popup | Policy + 容器选型 |
| 6 | 按压范式？ | visual-language §按压 | ① / ② / ③ |
| 7 | PointerPrimary 要 hover 吗？ | §输入设备 | `hoverable` + 触屏长按替代 |

### 禁止模式

| 禁止 | 原因 |
|------|------|
| `Platform == Desktop` 决定 dropdown 动画 | Android PC 同为 PointerPrimary |
| `sw >= 600` 单独决定 instant 菜单 | 误伤触屏平板 |
| TouchPrimary 去掉长按 | 违反手势等价 |
| PointerPrimary 手机 expand 菜单 | 与桌面预期不符 |
| TouchPrimary 强制 PC 顶栏 Dialog | 手机缺底栏/Sheet affordance |
| 列表行外包 `clickable` 吞掉 `onLongPress` | 长按菜单失效 |
| 业务内联 `expandVertically` + `shadow(>4dp)` 或大圆角 | 绕过 Policy；违反轻量通透 |

### 组件 spec 检查清单

- [ ] 「交互行为」含 PointerPrimary / TouchPrimary 手势对照表
- [ ] 列表行不双层 `clickable`；Switch 由行承载点击
- [ ] Tab 菜单：reselect + 长按 + 右键（或文档声明 reselect 例外）
- [ ] `pointerInput` 带 `selectedId` key
- [ ] Icon 有 `contentDescription`
- [ ] Popup spec 含 PointerPrimary / TouchPrimary 触发对照表（见 §Popup spec 模板）
- [ ] Dropdown：PC 右键 AtPointer + 触屏长按 PressCentered
- [ ] TouchPrimary 长列表用 BottomSheet；PointerPrimary 用 Dialog
- [ ] PC Dialog 顶栏操作；手机 Dialog 底栏或 ActionBar

---

## 导航与信息架构

### UniverseAgent（A 族 · Agent 壳层）

**形态**：Compose Multiplatform — 会话列表侧栏 + 多 Tab 聊天 + Settings 路由栈。

| 层级 | 模式 | 说明 |
|------|------|------|
| 顶级 | `AppRoute` 路由 | SessionList / Chat / Settings 等 |
| 聊天区 | `AppTabBar` + `ToolbarLayout` | 多 Engine Tab；Compact 隐藏连接图标 |
| 侧栏 | `SidebarViewStore` | 会话分组、Engine Tab 过滤 |
| 自适应 | `WindowSizeClass` | `<600dp` Compact：单栏全屏；`≥600dp` 侧栏 + 内容 |
| 外边距 | `LayoutProfile.chatRouteMargins()` | 横屏非 Compact：底部 chrome 12dp 呼吸；竖屏/Compact 贴边 |

**Web TUI**：SolidJS 路由 `Home | Session | Plugin`；Command Palette + 40+ 快捷键；Provider 深度嵌套（Route → Dialog → CommandPalette）。

### Singular（A 族 · IDE Shell）

**形态**：XML View Shell，语义对齐 IntelliJ IDEA。

| 层级 | 模式 | 说明 |
|------|------|------|
| 主窗 | `MainActivity` Toolbar + Editor + TabBar | Action 系统驱动，非静态按钮集 |
| 面板 | Bottom ToolWindow | Terminal / Git 等，可折叠 |
| 菜单 | CategoryMenu / MainMenu | 窄屏 `CATEGORY_POPUP`；宽屏 `MENU_BAR` |
| 导航条 | Breadcrumb（TabBar 上方 Layer 2） | IDEA 式路径，非 Android 标准 Nav |

窗口分级：`Compact width` / `Medium` / `Expanded` + `Compact height`（IME/横屏矮窗）。

### AppSnapShotor（B 族 · Fluent 工具）

**形态**：单 Activity + Navigation Component + 3-tab 悬浮底栏。

```
MainActivity
├── MaterialToolbar (48dp，随 Nav 变标题)
├── FragmentContainerView (NavHost)
└── FloatingBottomNav (BlurView 胶囊，3×ImageButton)
SettingsActivity — 独立 Activity，Toolbar 返回
```

| Tab | 内容 |
|-----|------|
| 存档 | `LauncherFragment` |
| 时间线 | 筛选 Chip + 可折叠搜索 + 热力图 |
| 应用 | 用户 Tab + 图标筛选 + 标签 Chip + 搜索 |

**约束**：`screenOrientation=portrait`；跨 Tab 用 `selectBottomNavTab()` + `launchSingleTop` / `restoreState`。

**相册分段 Tab**：见 [components/floating-chrome.md](components/floating-chrome.md)（滑块 200ms、同心 pill、× 辅钮）。

### CrashCenter / NeverCrash（B 族 · 当前与规划）

| 阶段 | 导航 |
|------|------|
| **Phase 3/4B（当前）** | 单 `ActivityMain`，0 bottom tab |
| **Phase 4C+** | 2-tab：`配置 \| 观测`；观测内 `TabLayout`（历史 \| 统计） |

参考 AppSnapShotor 壳层（固定 Toolbar + `FloatingBottomNav`），但 tab 数为 **2** 非 3；设置不进 tab（ADR-005 配置同屏高密度）。

---

## 触控与按压

> **三种按压范式 SSOT**：[visual-language.md §按压反馈](visual-language.md#按压反馈press-feedback)

| 范式 | 交互语义 | 典型控件 |
|------|----------|----------|
| **① iOS 按压** | 整面 overlay（`press_overlay` ~8–12%）；**禁止 scale** | 列表行、设置行、卡片、菜单行 |
| **② 圆形水波纹** | **唯一**允许 ripple；严格正圆触区 | Toolbar 圆 Icon、Tab ✕ |
| **③ 形状按压** | 填色 / overlay+clip；**Chip 推荐 overlay** | 方 Icon 盒、胶囊 |

| 控件形态 | 交互行为 |
|----------|----------|
| 列表/行 | 整行单点；Switch `clickable=false` → **①** |
| 圆形 IconButton | 独立触区 → **②**（须正圆） |
| **Chip / Filter** | **① overlay + clip**（推荐）或 ③ 填色 |
| 胶囊 / 方 Icon | ③ 填色或 overlay+clip |
| 主按钮 | 确认语义 → **①** 或底色加深 |

### 触摸目标

| Token | 值 | 说明 |
|-------|-----|------|
| `touch_target_min` | 40dp | 生态默认 |
| `icon_button_inset` | (size − glyph) / 2 | 对称内边距；见 [visual-language §图标按钮居中](visual-language.md#图标按钮居中icon-button-inset) |
| `list_row_min_height` | 36dp | 视觉下限，靠 padding 扩展热区 |
| 筛选图标容器 | 32dp | 18dp glyph + 7dp inset |
| Web 指针按钮 | 24–32dp 高 | 非触屏 48dp |

---

## 反馈与状态

### 加载与空态

| 项目 | 模式 |
|------|------|
| NeverCrash | `loadingPanel`（CircularProgressIndicator）+ `emptyState` 文案 |
| AppSnapShotor | Provider 检查 Dialog；列表 bottom padding 避让底栏 |
| UniverseAgent | `StartupLoading` overlay；Session 连接状态圆点 |
| Singular | StatusBar / Notification badge；Action enabled 绑定 ViewModel |

### Banner / 状态条

| 场景 | NeverCrash | 兄弟参考 |
|------|------------|----------|
| 模块未激活 | `statusBanner` → Xposed 管理器 / AlertDialog | — |
| 权限缺失 | `permissionBanner` + TextButton | AppSnapShotor Provider Dialog |
| 有过滤词 | 搜索图标 tint 主题色 | `CollapsibleSearchController.updateToggleState()` |

### 动效时长

| 类型 | 时长 | 来源 |
|------|------|------|
| 行按压 overlay | 100ms | UniverseAgent `Indications.kt` |
| 搜索展开 | 180ms AutoTransition | AppSnapShotor |
| Popup 进入（Touch） | 300ms | UniverseAgent DESIGN-SPEC §9 |
| Shell 回弹 | 120ms `ShellMotion.DURATION_S` | Singular |
| 列表项插入 | 100ms | NeverCrash `DefaultItemAnimator` |

---

## 表单与输入

### 搜索

| 项目 | 模式 |
|------|------|
| **NeverCrash** | 常显 Dense Outlined `TextInputLayout`；`clear_text`；即时过滤 name/package |
| **AppSnapShotor** | **可折叠**：默认仅图标 → 展开 `layout_search_field` + 自动 IME；收起保留 query |
| **UniverseAgent** | Chat 输入栏 + Slash 命令弹窗；`PopupProperties(focusable=false)` 防抢焦点 |

### 设置 / Toggle

| 项目 | 模式 |
|------|------|
| **NeverCrash** | 水平滚动 `ChipGroup`：`scope_mode` / `handle_system` / `show_system`；长按 scope 说明 Dialog |
| **AppSnapShotor** | 系统/用户 **图标 toggle**（至少一项）；用户 **TabLayout** |
| **UniverseAgent** | Settings 卡片分组；`ThemeMode` SYSTEM/LIGHT/DARK + `ThemePreset` |
| **Singular** | Settings row；主确认在 ActionBar 图标（P0），非底部唯一按钮 |

### 主题切换

| 项目 | 机制 |
|------|------|
| UniverseAgent | `ThemeMode` + `ThemePreset`（5 预设） |
| Singular / AppSnapShotor | `Theme.Material3.DayNight` |
| NeverCrash | 仅浅色 — **待补齐 DayNight** |

---

## 列表与筛选

Chip 模式与列表行交互见 **[components/form-controls.md](components/form-controls.md)**、**[toolbar-list-chrome.md](components/toolbar-list-chrome.md)**。

### 排序

| 项目 | 入口 |
|------|------|
| NeverCrash | Toolbar 子菜单（名称/安装/更新时间 ±） |
| AppSnapShotor | 各 Tab 自有（时间线日期范围等） |
| UniverseAgent | Command Palette 快捷键 |

---

## 对话框与 Overlay

> Popup Tier 与 InputModality 分流见 [§Popup 呈现分流](#popup-呈现分流)。本节为各产品实现入口。

### UniverseAgent Popup 出口

业务须经 `SingularityPopupSurface` / `SingularityAnchoredDropdownMenu`；菜单行 **`SingularityMenuItem`**（禁止 M3 `DropdownMenuItem` 48dp）。

### AppSnapShotor / NeverCrash

| 类型 | 实现 |
|------|------|
| 确认/说明 | `AlertDialog.Builder` |
| 设置子页 | AppSnapShotor：`IgnoreAppsFragment` / `AboutFragment` **BottomSheet** |
| Provider 检查 | 全屏 blocking Dialog + 行内重试 |

### Singular Dialog 入口

统一 `DialogManagerImpl` / `*DialogBuilder`（`:ui:shared`）；禁止业务直调 `MaterialAlertDialogBuilder`。

---

## 无障碍

| 项目 | 策略 |
|------|------|
| **Singular** | ADR-054：**不提供**完整 TalkBack；保留 `contentDescription` 供测试与长按提示 |
| **UniverseAgent** | Icon 必填 `contentDescription`；`SingularityIconButton` 统一入口 |
| **AppSnapShotor** | 搜索 toggle / 展开按钮有 `contentDescription` |
| **NeverCrash** | 部分 `contentDescription=@null` — **待补齐** |

触控目标：Singular 40dp 为误触治理下限，非 WCAG 合规替代。

---

## 多平台实现映射

| concern | Android View | Compose KMP | Web TUI | Desktop |
|---------|--------------|-------------|---------|---------|
| 行按压 | `shell_press_surface` | `rowPress` / `shellSurfaceClickable` | CSS overlay token | hover → pressed |
| 图标按钮 | `shell_ripple_icon` | `iconRipple` / `shellIconClickable` | `icon-button-v2` | 同 Web |
| 上下文菜单 | 长按 | `contextMenuGestures` | 右键 | 右键 |
| InputModality | `isAndroidPcFormFactor()` | `resolveInputModality()` | PointerPrimary | PointerPrimary |
| Popup 动效 | Policy / Material 默认 | `PopupPresentationPolicy` | instant | instant |
| Hover | — | `hoverable` | CSS `:hover` | 同 Web |
| 快捷键 | — | Command Palette | `KeybindV2` | 全局 keymap |
| 底栏 | `FloatingBottomNav` | 侧栏/Tab | — | Window chrome |
| 搜索 | `CollapsibleSearchController` | Inline / Command | `/` 面板 | `/` + Ctrl+K |
| Edge-to-edge | `WindowCompat…` | 同 | N/A | 自定义 title bar |

### Web TUI v2 要点

- 组件：`button-v2` / `icon-button-v2` / `menu-v2` / `dialog-v2` / `keybind-v2`
- 指针：`focus-visible` 2px outline；`:hover` / `:active` overlay token
- 默认 PointerPrimary；无触屏 expand 动效

### 架构注入点（Compose KMP 参考）

```text
Theme / Scaffold
  ├─ LocalLayoutProfile          ← 窗口断点
  ├─ LocalPopupPresentationPolicy ← InputModality
  └─ LocalIndication             ← rowPress / iconRipple（与 Modality 正交）

业务 UI
  ├─ contextMenuGestures / ContextMenuArea
  └─ SingularityPopupSurface(tier)
```

## 跨项目对照表

| 维度 | UniverseAgent | Singular | AppSnapShotor | NeverCrash |
|------|---------------|----------|---------------|------------|
| **InputModality** | ✅ `resolveInputModality` | ⚪ View 默认 Touch | ⚪ 仅 Touch | ⚪ 仅 Touch |
| **手势等价** | ✅ pointer-interaction SSOT | ⚪ IDE 右键待完善 | ❌ 仅触屏 | ❌ 仅触屏 |
| **Tab 三态** | ✅ reselect+长按+右键 | 部分 Tab | — | — |
| **Hover** | ✅ Compose hoverable | DeX 增强 | — | — |
| **快捷键** | Command Palette | SearchEverywhere | — | — |
| **Popup 分流** | ✅ Policy | DialogManager | AlertDialog | AlertDialog |

---

## CrashCenter 落地指引

### 已对齐生态规范

- 固定 48dp Toolbar；Fluent 语义色映射到 `accent` / `canvas`
- Filter Chip + 搜索 + 状态 banner
- Edge-to-edge（`SystemBars.kt`）

### 优先补齐

1. **Compact 布局**：Toolbar 排序/About 收进 ⋮；评估可折叠搜索；筛选 Chip 超 3 个时移 overflow
2. **去全宽线**：见 [components/toolbar-list-chrome.md](components/toolbar-list-chrome.md)
3. 列表行 **① overlay**；Chip **① overlay+clip**；正圆 Icon **②**；方 Icon/胶囊 **③**
4. `values-night/` + DayNight
5. `touch_target_min` 40dp；`contentDescription`
6. 毛玻璃 2-tab 底栏（Phase 4C）— [floating-chrome.md](components/floating-chrome.md)

---

## 源码参考

| 项目 | 关键路径 |
|------|----------|
| **UniverseAgent** | `singularity/docs/ui/DESIGN-SPEC.md`；`pointer-interaction.md`；`input-modality-interaction.md`；`ui/theme/Indications.kt`、`Dimens.kt`；`components/ContextMenuGestures.kt`；`tui/packages/ui/src/v2/components/*` |
| **Singular** | `docs/architecture/concepts/shell-design-language.md`；`mobile-interaction-guidelines.md`；`docs/decisions/058-shell-design-tokens-touch-target.md`；`ui/shared/compose/ShellPressFeedback.kt`；`common/res/values/design_tokens.xml` |
| **AppSnapShotor** | `docs/guides/getting-started/ui-shell.md`；`ui/widget/FloatingBottomNav.kt`；`CollapsibleSearchController.kt`；`TagsFilterLayout.kt`；`app/AppFilterHelper.kt`；`main/MainActivity.kt` |
| **NeverCrash** | `app/.../ActivityMain.kt`；`SystemBars.kt`；`res/layout/activity_main.xml`；`docs/architecture/navigation-ia.md` |

---

## 术语

| 术语 | 含义 |
|------|------|
| **InputModality** | TouchPrimary vs PointerPrimary；决定 popup 外观/动效/hover |
| **LayoutProfile** | 窗口尺寸分级；决定布局，非 popup 动效 |
| **Reselect** | 点击已选中项；可与长按/右键等价触发菜单 |
| **ContextMenuTrigger** | `LongPress` / `RightClick` / `Reselect` — Dropdown 三态枚举 |
| **PressCentered** | 触屏/reselect 菜单锚定：水平居中按压点，上下 flip |
| **AtPointer** | PC 右键菜单锚定：top-start 对齐指针 |
| **Press Paradigm** | ① overlay（含 Chip+clip）/ ② 正圆 ripple / ③ 形填色 |
| **PopupPresentationPolicy** | 按 InputModality + Tier 注入 shadow/outline/expand |
| **Action Tier** | P0–P4 功能优先级；Compact 只外露 P0–P1 |
| **Overflow** | Toolbar ⋮ 菜单；承接 P2+ 横向放不下的 Action |
| **平台浮层** | 同语义不同壳：手机 Sheet/底栏按钮；PC Dialog/顶栏按钮 |
| **Inset divider** | 与 item 行内容列对齐；不拉满 Card/Sheet/Popup 外缘 |
| **Floating chrome** | 悬浮毛玻璃底栏 / 分段 Tab；glass + stroke，0 shadow |
| **Segment indicator** | 可滑动 pill；与轨道 **同心圆角**；`r=(H−2×inset)/2`；200ms |
| **Press-Drag-Release** | 按住锚点开菜单 → 滑动跟选行（① overlay）→ 松手触发 / 落点外关闭 |
| **Master 行** | Popup 底部汇总项；选中 ≡ 上方全部选项；与上区间 **`popup_section_gap`** |

---

## 相关文档

- [INDEX.md](INDEX.md) — 设计文档总索引
- [components/INDEX.md](components/INDEX.md) — 公共组件标准
- [visual-language.md](visual-language.md) — 视觉 token、按压、浮层外观
- [configuration-ui.md](../architecture/configuration-ui.md) — CrashCenter 配置 UI 架构
- [navigation-ia.md](../architecture/navigation-ia.md) — CrashCenter 分阶段导航
- [ADR-005: 设置信息架构](../decisions/005-settings-information-architecture.md)
- [ADR-006: Material3 工具链](../decisions/006-material3-toolchain.md)
- [glossary.md](../glossary.md) — 项目术语
