---
title: "导航与信息架构"
type: architecture
status: accepted
phase: 4
updated: 2026-06-19
summary: "分阶段导航：Phase 3/4B 无 tab；Phase 4C+ 双底栏（配置 | 观测），观测内 TabLayout（历史 | 统计）；路由表见 ui-routing.md"
---

# 导航与信息架构

> 适用模块：`:app` 配置与观测 UI  
> 分析来源：Tab IA 分析（2026-06-19，会话 a0a7a9db）  
> 相关决策：[ADR-005](../decisions/005-settings-information-architecture.md)（配置 tab 单屏高密度）

## 概述

CrashCenter 产品叙事分为 **干预层**（hook 配置 + 吞异常）与 **观测层**（崩溃记录 + 统计），见 [crash-logging.md](crash-logging.md)。顶级导航按 **任务域** 划分，而非按设置项数量机械拆 tab。

**Activity / Fragment 路由表、Intent 参数、返回栈**见 **[ui-routing.md](ui-routing.md)**。

| 阶段 | 顶级 tab 数 | 形态 |
|------|-------------|------|
| Phase 3 / 4B | **0** | 单 `ActivityMain`，全部配置同屏 |
| Phase 4C+ | **2** | 底栏：**配置** \| **观测** |

---

## Phase 3 / 4B：0 tab（当前）

- **载体**：单一 `ActivityMain`（见 [configuration-ui.md](configuration-ui.md)）
- **内容**：Xposed 状态条、包可见性 banner、全局 FilterChip、搜索与过滤、per-app 列表、Toolbar 排序/批量/About/Test
- **4B**：`CrashLogger` MVP 仅后台写 JSONL，**无 UI**，导航不变

此阶段完全符合 [ADR-005](../decisions/005-settings-information-architecture.md) 修订后的单屏高密度配置 IA。

---

## Phase 4C+：2 个 Bottom Tab

观测 UI 落地后引入 **2-tab 壳层**（非 3 tab）。推荐在 **4C（历史 P1）** 引入壳层，观测 tab 先仅「历史」；**4D** 在观测 tab 内增加「统计」子 tab，避免后续再加顶级 tab 的重构。

```
┌─────────────────────────────────┐
│ Toolbar + 溢出菜单               │
│ [壳层] Xposed 状态条（两 tab 共享）│
├─────────────────────────────────┤
│                                 │
│   Fragment 内容区                │
│                                 │
├─────────────────────────────────┤
│  [配置]  │  [观测]               │  ← Bottom Nav（2 项）
└─────────────────────────────────┘
```

### Tab 1 — 配置

完整保留当前 `ActivityMain` 紧凑布局（ADR-005 不变）：

- 包可见性 banner（条件显示）
- 全局 FilterChip：`scope_mode` / `handle_system` / `show_system_ui`
- Dense 搜索 + 全部/已应用/未应用 Chip + 应用计数
- RecyclerView per-app Switch
- Toolbar：排序、全选/取消、About、Test

### Tab 2 — 观测

内层 **`TabLayout`（页级，非 bottom nav）**：

| 子 tab | Phase | 内容 |
|--------|-------|------|
| **历史** | 4C | 时间倒序列表 → 按 `crash_id` 打开详情 |
| **统计** | 4D | 按包名/异常类 TOP N、日/周摘要；清空、retention — 详见 [crash-stats-ui.md](crash-stats-ui.md) |
| （Toolbar 菜单） | 4E | SAF 导出、`crash_log_enabled` 等运维项 |

观测域设置挂在观测 tab 的 Toolbar 或统计页脚，**不**增设第三个 bottom tab。

### 壳层实现（可选）

视觉与 sibling 项目 **AppSnapShotor** 对齐时，可参考其主壳层模式（非 1:1 照搬 tab 数量）：

- 固定 `MaterialToolbar` + `FragmentContainerView` + 底栏
- 自定义 **`FloatingBottomNav`**（`BlurView` 悬浮胶囊、等宽 `ImageButton`），见 AppSnapShotor `docs/guides/getting-started/ui-shell.md`
- CrashCenter 底栏为 **2 项**（约 120dp 宽），列表区 `paddingBottom` 避让底栏
- 使用 `NavController` + `launchSingleTop` / `restoreState` 绑定 tab

完整 M3 / Navigation 组件选型见 [ADR-006](../decisions/006-material3-toolchain.md) defer；Phase 4 可用 Material Components + 现有 Fluent token 先行。

---

## 不应放进 Bottom Tab 的内容

| 内容 | 推荐载体 | 理由 |
|------|----------|------|
| Stack trace 详情 | `ActivityCrashInfo`（`crash_id` / Intent extra） | 深度阅读，单次任务 |
| About / 使用警告 | Toolbar 对话框 | 低频；AppSnapShotor 同理不进 tab |
| 测试崩溃 | Toolbar 菜单 | 开发/验收，非用户主路径 |
| Xposed 管理器跳转 | 状态条点击 | 系统外链 |
| 包可见性授权 | 条件 banner + 对话框 | 阻塞型提示 |
| SAF 导出 | 系统 picker + 确认对话框 | 瞬时流程 |
| 排序 / 批量操作 | Toolbar 子菜单 | 配置域工具 |

---

## 与 AppSnapShotor 对比

| 维度 | AppSnapShotor | CrashCenter（推荐） |
|------|---------------|-------------------|
| Bottom tab 数 | **3**（存档 \| 时间线 \| 应用） | **2**（配置 \| 观测） |
| 设置 | 独立 `SettingsActivity`，Toolbar 进入 | 不进 tab；全局 hook 设置在 **配置 tab** 同屏 Chip（ADR-005） |
| Tab 内再分 tab | 应用 tab 内 `TabLayout`（用户/系统筛选） | **观测 tab** 内 `TabLayout`（历史 \| 统计） |
| 底栏组件 | `FloatingBottomNav` + 3× `ImageButton` | 可选同款壳层，**2 项** |
| 产品主轴 | 三条独立工作流（存档、时间线、应用配置） | 干预 vs 观测两层；统计为历史的聚合视图，不必独立顶级 tab |

**可借鉴**：壳层布局、悬浮底栏、内联 TabLayout、Toolbar 固定不随列表收起。  
**不照搬**：3 tab 数量；CrashCenter 观测层早期较轻，独立「统计」顶级 tab 价值不足（见 Phase 4D「Chip + 简单列表，无重型图表」）。

---

## ADR-005 澄清

[ADR-005](../decisions/005-settings-information-architecture.md) 的 **单屏高密度** 约束 **仅适用于配置 tab**：

- 全局设置与 per-app 列表须 **同屏可达**（FilterChip + RecyclerView）
- **不** 表示整个应用只能有一个 Activity 或禁止 Phase 4 观测域顶级导航
- Phase 4C+ 增加「观测」bottom tab 与 ADR-005 **不冲突**；历史/统计 **不应** 塞进配置 tab 下方（会占用列表空间并混淆干预/观测语义）

---

## 分阶段 rollout

| 阶段 | Tab 数 | 导航形态 | 备注 |
|------|--------|----------|------|
| **Phase 3**（当前） | 0 | 单 `ActivityMain` | ADR-005 单屏配置 |
| **Phase 4B** | 0 | 无 UI 变更 | `CrashLogger` 后台写盘 |
| **Phase 4C** | 0 → **2** | 引入 2-tab 壳层；观测 tab 仅 **历史** | 推荐此时上壳层，避免 4D 再重构 |
| **Phase 4D** | 2 | 观测 tab 内增加 **统计** 子 tab | 不增 top-level tab |
| **Phase 4E** | 2 | 导出 / retention 挂观测 Toolbar | 仍 2 tab |

**备选（最小 4C 改动）**：4C 暂用 Toolbar 菜单 → 独立历史 Activity（0 tab），4D 再一次性上 2-tab 壳层——可行但多一次导航重构；**首选 4C 直接 2-tab**。

---

## 相关文档

- [ui-routing.md](ui-routing.md) — Activity/Fragment 路由与 NavGraph
- [configuration-ui.md](configuration-ui.md) — 配置 tab 布局与密度策略
- [crash-logging.md](crash-logging.md) — 观测层数据与 UI 需求
- [ADR-005: 设置信息架构](../decisions/005-settings-information-architecture.md)
- [ADR-006: Material 3 工具链](../decisions/006-material3-toolchain.md)
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md)
- [phase3_ui_redesign.md](../../dev/roadmap/active/phase3_ui_redesign.md)
- AppSnapShotor 参考：`/home/clarence/Projects/Android/AppSnapShotor/docs/guides/getting-started/ui-shell.md`
