---
title: "设计文档索引"
type: concept
status: accepted
phase: N/A
updated: 2026-06-22
summary: "Clarence 生态设计 SSOT 入口——UI 模式、视觉 token、交互范式、组件标准。"
---

# 设计文档索引

```
docs/design/
├── ui-modes.md             ← 信息流 vs 工具密度；同页 Hybrid 分区
├── visual-language.md      ← Token、色彩、圆角、按压、轻量通透
├── interaction-language.md ← InputModality、导航、Popup 策略、响应式
└── components/             ← 公共组件 spec
    └── INDEX.md
└── demos/                  ← 各组件 Web demo（index.html）
```

## 读哪份

| 角色 | 路径 |
|------|------|
| **定 UI 模式 / 分区** | [ui-modes.md](ui-modes.md) |
| 定 token / 主题 | [visual-language.md](visual-language.md) |
| 定手势 / 浮层策略 / 导航 | [interaction-language.md](interaction-language.md) |
| 做具体 UI 组件 | [components/INDEX.md](components/INDEX.md) + [demos/](demos/INDEX.md) |

## 四层关系

| 层 | 回答 | 示例 |
|----|------|------|
| **UI 模式** | 内容为主还是工具为主 | 相册 grid vs Singular Editor |
| **视觉** | 长什么样、用什么 token | `accent`、`radius_mobile_dialog` |
| **交互** | 怎么触发、怎么分流 | 长按≡右键、`PopupPresentationPolicy` |
| **组件** | 这个控件怎么拼 | 筛选 Popup 的 Master 行 |

组件文档 **引用** 视觉/交互 SSOT，不重复定义 token 数值。

## 相关文档

- [../architecture/configuration-ui.md](../architecture/configuration-ui.md) — CrashCenter UI 架构
- [../glossary.md](../glossary.md) — 术语
