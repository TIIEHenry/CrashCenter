---
title: "公共组件 Web Demo"
type: guide
status: accepted
phase: N/A
updated: 2026-06-22
summary: "Clarence 生态公共 UI 组件的静态网页 demo——与 components/ spec 一一对应。"
---

# 公共组件 Web Demo

每个 [components/](../components/INDEX.md) 公共组件均有 **可交互 HTML demo**，用于设计评审与实现对照。

## 本地预览

```bash
cd docs/design/demos
python3 -m http.server 8765
```

浏览器打开 [http://localhost:8765](http://localhost:8765)。

## Demo 清单

| 组件 spec | Demo |
|-----------|------|
| [shared-dividers.md](../components/shared-dividers.md) | [shared-dividers.html](shared-dividers.html) |
| [toolbar-list-chrome.md](../components/toolbar-list-chrome.md) | [toolbar-list-chrome.html](toolbar-list-chrome.html) |
| [settings-card-detail-sheet.md](../components/settings-card-detail-sheet.md) | [settings-card-detail-sheet.html](settings-card-detail-sheet.html) |
| [draggable-half-sheet.md](../components/draggable-half-sheet.md) | [draggable-half-sheet.html](draggable-half-sheet.html) |
| [popup-filter-menu.md](../components/popup-filter-menu.md) | [popup-filter-menu.html](popup-filter-menu.html) |
| [floating-chrome.md](../components/floating-chrome.md) | [floating-chrome.html](floating-chrome.html) |
| [selection-bubble.md](../components/selection-bubble.md) | [selection-bubble.html](selection-bubble.html) |
| [form-controls.md](../components/form-controls.md) | [form-controls.html](form-controls.html) |

## 结构

```
docs/design/demos/
├── index.html              ← 目录
├── INDEX.md                ← 本说明
├── assets/
│   ├── tokens.css          ← 与 visual-language 对齐的 CSS 变量
│   ├── base.css
│   ├── press.js            ← ① iOS 按压
│   ├── segmented.js        ← 滑动 pill
│   ├── press-drag-menu.js  ← Press-Drag-Release
│   ├── selection-bubble.js ← 选区锚定
│   └── scrim.js            ← ScrollLinked Scrim
└── *.html                  ← 各组件 demo 页
```

## 约定

- Demo **不替代** spec 文档；数值以 `visual-language.md` / 组件 md 为准
- 新增公共组件时 **必须** 同步添加 demo 页并在本 INDEX 登记
- 支持浅色/深色切换（`localStorage` `clarence-demo-theme`）

## 相关文档

- [../components/INDEX.md](../components/INDEX.md)
- [../INDEX.md](../INDEX.md)
- [../visual-language.md](../visual-language.md)
