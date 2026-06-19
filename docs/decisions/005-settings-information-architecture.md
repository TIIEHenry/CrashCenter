---
title: "ADR-005: 设置信息架构"
type: decision
status: accepted
phase: 3
updated: 2026-06-19
summary: "配置 tab 单屏高密度：全局 Chip 与列表同屏；Phase 4C+ 观测域允许 2-tab 壳层（见 navigation-ia.md）"
---

# ADR-005: 设置信息架构

## 背景

Material v1 将三个全局 Switch 放在主屏设置卡片中，占用首屏垂直空间，应用列表可见区域偏小。Phase 3B 曾拟定将设置迁出主屏（独立 Settings Activity）。

**2026-06-19 用户要求修订**：「应该在一个界面配置所有应用，考虑优化 ui 的信息密度」——全局设置与 per-app 列表须 **同屏可达**，通过压缩控件而非拆分界面解决空间问题。

## 决策

**采用单屏高密度布局**（取代原「独立 Settings Activity」默认方案）：

- 主屏保留并紧凑化：状态条、全局设置、搜索、过滤 Chip、应用计数、RecyclerView
- 三个全局开关改为横向 **FilterChip** 行（可滚动）：`scope_mode`、`handle_system`、`show_system_ui`
- 作用域模式说明改 **长按 Chip** 弹窗，移除卡片内常驻说明段落
- 列表项去卡片化、缩小图标与 padding，最大化 RecyclerView 高度

## 备选方案（未采用）

| 方案 | 不选原因 |
|------|----------|
| 独立 Settings Activity | 与用户「单屏配置」要求冲突（原 3B 默认，已 defer） |
| Modal BottomSheet | 同上，增加导航步骤 |
| 可折叠设置卡片 | 仍占首屏空间，密度提升有限 |

## 不变项

- `PREF_SCOPE_MODE`、`PREF_HANDLE_SYSTEM`、`show_system_ui` key 与默认值不变
- 变更仍即时 `SharedPreferences.apply()`
- 与 [scope-and-prefs.md](../architecture/scope-and-prefs.md) 行为一致
- 搜索、过滤、排序、批量操作、空状态逻辑不变

## 主屏布局（当前）

```
Toolbar [排序⋮]
状态条（单行 · 激活/未激活）
[作用域] [处理系统] [显示系统]   ← FilterChip，可横滑
搜索框（Dense）
[全部] [已应用] [未应用]    共 N 个应用
─────────────────
应用列表（RecyclerView，扁平高密度行）
```

## 后果

| 正面 | 负面 |
|------|------|
| 零额外导航，全部配置一屏完成 | Chip 标签较短，需长按查看 scope 详细说明 |
| 列表首屏可见 app 显著增多 | FilterChip 与 Choice Chip 视觉需区分（样式已分） |
| prefs 与 hook 语义零变更 | ADR 原 Activity 方案作废，3B「迁出主屏」任务 defer |

## Phase 4 导航例外（2026-06-19）

Tab IA 分析结论：本 ADR 的 **单屏约束仅适用于配置 tab**，不禁止 Phase 4 观测 UI 引入顶级导航。

| 阶段 | 配置域 | 观测域 |
|------|--------|--------|
| Phase 3 / 4B | 单 `ActivityMain`（0 tab） | 无 UI |
| Phase 4C+ | **配置** bottom tab，保留本 ADR 布局 | **观测** bottom tab；内层 TabLayout（历史 \| 统计） |

详见 [navigation-ia.md](../architecture/navigation-ia.md)。观测 tab 与配置 tab 分离，**不**将历史/统计塞进配置首屏。

## 相关文档

- [navigation-ia.md](../architecture/navigation-ia.md)
- [configuration-ui.md](../architecture/configuration-ui.md)
- [scope-and-prefs.md](../architecture/scope-and-prefs.md)
- [density-optimization-2026-06-19.md](../../dev/iterations/configuration-ui/density-optimization-2026-06-19.md)
- [ADR-002](002-inverted-package-toggle.md)
