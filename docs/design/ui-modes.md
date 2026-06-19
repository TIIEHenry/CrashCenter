---
title: "UI 模式：信息流与工具密度"
type: concept
status: draft
phase: N/A
updated: 2026-06-19
summary: "生态两种 UI 范式——信息流大 UI（内容为主、悬浮 chrome）与工具密度 UI（按钮/窗口多）；同页可分区交叉组合。"
---

# UI 模式：信息流与工具密度

> Token 共用：[visual-language.md](visual-language.md) · 交互分流：[interaction-language.md](interaction-language.md) · 组件：[components/INDEX.md](components/INDEX.md)

生态 **共用一套 token 与按压范式**，但 **视角与布局** 分两种 UI 模式。产品、页面、甚至**同一屏内的不同区域**须声明采用哪种模式。

## 两种模式

| 维度 | **信息流 UI**（Content-first） | **工具密度 UI**（Tool-dense） |
|------|-------------------------------|------------------------------|
| **中文** | 信息流大 UI | 工具小 UI |
| **重心** | **展示信息**；内容占满视口 | **操作与面板**；按钮、窗口、命令多 |
| **内容区** | full-bleed、edge-to-edge、大图/长列表 | 分割布局：Editor + Dock + ToolWindow |
| **Chrome** | **悬浮层**（glass 底栏、FAB、浮动 Tab） | **固定壳层**（Toolbar、MenuBar、StatusBar、Pane） |
| **控件尺寸** | 按钮 **小**、稀疏；筛选进 Popup/overflow | 按钮 **多**、Action 条、图标密集 |
| **导航** | 2–3 Tab 浮动底栏；页内 **分段 Tab** | 任务域 Tab + ToolWindow + Command Palette |
| **浮层** | anchored Popup、BottomSheet 详情 | Dialog、FloatingPanel、Draggable 伪窗口 |
| **典型产品** | 相册、AppSnapShotor 时间线、UA Chat 流 | **Singular** IDE Shell、UA 管理面板（PC） |
| **CrashCenter** | 应用列表主区（只读浏览） | 配置 Toolbar、Chip 行、设置（高密度） |

```text
信息流                         工具密度
┌────────────────────┐        ┌──── Toolbar / MenuBar ────┐
│                    │        ├──────┬──────────────────┤
│    内容 full-bleed  │        │ Dock │   Editor / 面板   │
│                    │        ├──────┴──────────────────┤
│  ╭─ glass 底栏 ─╮   │        │      StatusBar          │
└──╰──────────────╯──┘        └─────────────────────────┘
     悬浮、不抢画面                 固定、多入口
```

## 设计原则（模式级）

| 原则 | 信息流 | 工具密度 |
|------|--------|----------|
| **chrome 不抢内容** | 悬浮 + 透明/毛玻璃；`paddingBottom` 避让 | 壳层稳定；内容区让位给面板比例 |
| **操作入口** | 少而精 → FAB、⋮、长按、浮动 Tab | 多而全 → ActionBar、Menu、快捷键 |
| **分割线** | 详情 Sheet / Card 内 item 对齐线 | 面板边界、ToolWindow 标题栏 |
| **圆角** | 手机 Sheet/Popup **较大** | PC Panel **直角**；控件 **小圆角** |
| **阴影** | 轻投影 + stroke；悬浮层 0–4dp | PC Panel 8dp + outline；菜单 outline |

两种模式 **不 fork token**；差异体现在 **布局分区 + chrome 形态 + 组件选型**。

## 同页交叉组合（Hybrid）

**常见**：一屏内同时存在「浏览区」与「操作区」，须 **按区域** 标注模式，禁止整页混用同一套 chrome 规则。

### 分区模型

```text
┌─────────────────────────────────────────┐
│  [可选] 工具密度 · 顶栏 Toolbar + Chip   │  ← 固定壳层
├─────────────────────────────────────────┤
│                                         │
│     信息流 · 主内容（列表/网格/预览）      │  ← full-bleed
│                                         │
│  ╭ 悬浮 · 分段 Tab / FAB Popup ╮         │  ← 悬浮层（仅覆盖内容区）
╰─────────────────────────────────────────╯
```

| 区域 | 模式 | Chrome 规则 |
|------|------|-------------|
| **主浏览区** | 信息流 | 无全宽 divider；① 行按压；避让悬浮层 |
| **顶栏/筛选** | 工具密度 | 48dp 共面 Toolbar；Chip/overflow；Compact 收 P2+ |
| **浮动控件** | 信息流 | glass + stroke；[floating-chrome.md](components/floating-chrome.md) |
| **侧栏/底栏面板** | 工具密度 | 固定宽/高；Singular ToolWindow 规则 |
| **详情/设置 Sheet** | 信息流结构 | 实色 layer + Card；[settings-card-detail-sheet.md](components/settings-card-detail-sheet.md) |

### 组合示例

| 产品 / 场景 | 信息流区 | 工具密度区 |
|-------------|----------|------------|
| **相册** | 照片网格、详情 Sheet | 顶栏搜索/⋮（轻）；筛选 **Popup** |
| **AppSnapShotor** | 时间线/存档列表 | Toolbar + 可折叠搜索 + 底栏 Tab |
| **UniverseAgent** | MessageList、Session 流 | Chat 输入栏、Command Palette、PC 管理 Panel |
| **Singular** | Editor Canvas（内容） | MenuBar、ToolWindow、StatusBar、Action 系统 |
| **CrashCenter** | 应用列表（浏览+toggle） | 全局 Chip 行、排序 ⋮、Xposed banner |

### Hybrid 禁止

| 禁止 | 原因 |
|------|------|
| 信息流主区铺 **固定厚 Toolbar + 底栏 + 侧栏** 三重壳 | 内容被挤成「工具 app 小窗」 |
| 工具密度区用 **全屏 glass 悬浮 Tab** 替代 MenuBar | 操作入口不可达、不符合 PC 预期 |
| 同一区域 **既** fixed 底栏 **又** floating 底栏 | chrome 双份 |
| 不标注模式，按产品名硬套组件 | CrashCenter 列表≠Singular 文件树 |

## 选型决策

```text
这一屏（或这一区域）用户主要是在「看」还是「操作工具」？
├── 看内容、扫列表、沉浸浏览
│     → 信息流：悬浮 chrome、小按钮、Popup 收操作
└── 编辑、配置、多命令、多面板
      → 工具密度：固定壳层、Action/Menu、ToolWindow

若两者都有 → Hybrid：分区标注，各 zone 用对应组件 doc
```

## 组件映射

| 组件 | 首选模式 | 文档 |
|------|----------|------|
| FloatingToolbar（Unified / Split / IconNav） | 信息流 | [floating-chrome.md](components/floating-chrome.md) |
| 筛选 Popup / Master 行 | 信息流 | [popup-filter-menu.md](components/popup-filter-menu.md) |
| 详情 Sheet / metadata | 信息流 | [settings-card-detail-sheet.md](components/settings-card-detail-sheet.md) |
| Toolbar + Chip 行 + overflow | 工具密度 | [toolbar-list-chrome.md](components/toolbar-list-chrome.md) |
| FilterChip / Switch 密集表单 | 工具密度 | [form-controls.md](components/form-controls.md) |
| PC FloatingPanel / Dialog Wide | 工具密度 | [interaction-language.md §平台原生浮层](interaction-language.md#平台原生浮层外观) |
| Item 对齐 divider | **两者** | [shared-dividers.md](components/shared-dividers.md) |

## 与 InputModality 的关系

| 维度 | 说明 |
|------|------|
| **UI 模式** | 内容 vs 工具 — **布局与 chrome 密度** |
| **InputModality** | 触控 vs 指针 — **Popup 动效与圆角分档** |

正交：相册 **信息流 + TouchPrimary**；Singular **工具密度 + PointerPrimary**；DeX 上可 **信息流内容 + PointerPrimary 菜单**。

## 相关文档

- [INDEX.md](INDEX.md)
- [visual-language.md](visual-language.md)
- [interaction-language.md](interaction-language.md)
- [components/INDEX.md](components/INDEX.md)
- [../glossary.md](../glossary.md)
