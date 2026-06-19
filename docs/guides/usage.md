---
title: "用户使用指南"
type: guide
status: accepted
phase: N/A
updated: 2026-06-19
summary: "模块安装、界面说明、scope 与 LSPosed 作用域、包可见性、崩溃观测 FAQ"
---

# 用户使用指南

## 模块用途

CrashCenter（崩溃中心）是一个 Xposed 模块，**拦截目标 app 的 Java 异常使其不崩溃**。它**不修复错误**，只是让 app 继续运行。

> ⚠️ 忽略异常可能导致不可预期的行为。建议仅对频繁崩溃的 app 使用，系统 app 请谨慎。

## 安装步骤

1. 安装 APK（见 [build-and-install.md](build-and-install.md) 或 [getting-started/INDEX.md](getting-started/INDEX.md)）
2. 在 Xposed 管理器（LSPosed 推荐）中启用模块
3. 打开 CrashCenter，确认顶部**状态条**显示「模块已激活」（绿色）
4. 在应用列表中配置要 hook 的应用

若状态条为橙色「模块未激活」，请点击状态条或到 Xposed 管理器启用本模块并重启。

## LSPosed 作用域 vs 应用内 Switch

两套配置**必须同时理解**：

| 机制 | 在哪里设置 | 控制什么 |
|------|------------|----------|
| **LSPosed 作用域** | Xposed / LSPosed 管理器 | 模块是否**注入**目标 app 进程 |
| **应用内 Switch** | CrashCenter 列表 | 注入后是否**拦截**该 app 崩溃 |
| **Scope Mode Chip** | CrashCenter 全局 Chip | 作用域模式下 hook 哪些包 |

仅在本应用里全开 Switch，但管理器未勾选目标 app → **不会 hook**。仅管理器勾选但 Switch 关闭 → 该包在禁用列表中，**也不拦截**。

崩溃日志（Phase 4 计划）只记录**已被 hook 且被拦截**的异常；与「应用列表是否完整」无直接关系。列表完整性依赖 [包可见性权限](#包可见性android-11)（模块进程 `QUERY_ALL_PACKAGES`）。

## 界面说明

主界面自上而下分为以下区域（详见 [configuration-ui.md](../architecture/configuration-ui.md)）：

```
┌─────────────────────────────┐
│  Toolbar（排序 / 更多菜单）   │
├─────────────────────────────┤
│  状态条（单行 · 激活/未激活）  │
├─────────────────────────────┤
│  包可见性条（需授权时显示）    │  ← 点击去系统设置
├─────────────────────────────┤
│  [作用域][处理系统][显示系统]  │  ← 可横滑 Chip
├─────────────────────────────┤
│  搜索框（紧凑）               │
│  [全部][已应用][未应用] 共N个 │
├─────────────────────────────┤
│  应用列表（扁平行 + Switch）   │
└─────────────────────────────┘
```

### 状态条

| 样式 | 含义 |
|------|------|
| 绿色 + 盾牌勾 | [Self Hook](../glossary.md#self-hook) 已生效，模块正常工作 |
| 橙色 + 盾牌 | 模块未在 Xposed 中激活，配置不会生效 |

单行文案合并了激活/未激活提示；未激活时仍会弹出完整说明对话框。

### 包可见性（Android 11+）

应用列表依赖 **查询所有软件包**（`QUERY_ALL_PACKAGES`）权限。该权限不能在应用内一键弹窗授予，需要您手动在系统设置中开启：

1. 若主界面出现蓝色 **包可见性** 提示条，点击 **去授权** 或提示条本身
2. 阅读说明后 tap **打开设置**，进入 CrashCenter 的 App 信息
3. 打开 **权限**，允许 **查询所有软件包**（各厂商文案可能略有不同）
4. 返回 CrashCenter，列表会自动刷新为完整应用列表

未授权时列表可能不完整，且不会有额外 toast；请根据提示条操作。

### 全局设置（Chip 行）

| Chip | 说明 |
|------|------|
| **作用域** | 见下文 [Scope Mode 行为](#scope-mode-行为)；**长按**可查看详细说明 |
| **处理系统** | 作用域模式下，是否对系统 app 生效 |
| **显示系统** | 列表中是否显示系统 app（仅影响 UI 展示） |

选中 Chip 表示对应开关 **开启**。

### 应用列表

- **Switch 开启**：该 app 被 hook，异常不会导致崩溃
- **Switch 关闭**：该 app 不被 hook（加入 [Disabled Package](../glossary.md#disabled-package) 列表）
- 点击整行即可切换 Switch
- 系统 app 会显示「系统」标签

### 搜索与过滤

- **搜索框**：按应用名称或包名实时过滤
- **Chip**：`全部` / `已应用` / `未应用`，筛选当前列表

### Toolbar 菜单

| 选项 | 说明 |
|------|------|
| 排序 | 按名称、安装时间、更新时间（正/逆序） |
| 全部应用 | 列表中所有 app 开启 hook |
| 全部取消 | 列表中所有 app 关闭 hook |
| 关于 | 显示详细使用说明 |
| 测试 | 触发测试崩溃，验证模块是否拦截 |

## Scope Mode 行为

[Scope Mode](../glossary.md#scope-mode) 决定 hook 范围：

- **关闭**（默认）：hook 所有 app（除 `android` 和 Xposed 管理器）；Switch 关闭的包写入禁用列表，仍不 hook
- **开启**：仅 hook 列表中 Switch **开启**的非系统 app；若同时开启「处理系统应用」，系统 app 也可被 hook

## 崩溃反馈

被 hook 的 app 发生异常时：

1. 弹出 Toast 显示异常信息
2. 发送系统通知（可点击查看完整 stack trace）
3. App **不会退出**

通知点开后进入**崩溃详情**页，stack trace 为等宽字体，长按可选中复制。

当前版本**不持久化**历史崩溃；Phase 4 将支持本地崩溃历史与统计（见 [crash-logging.md](../architecture/crash-logging.md)）。技术说明：崩溃日志**不能**用 XSharedPreferences 传递，须模块私有存储 + IPC — 见 [crash-log-ipc FAQ](../architecture/crash-log-ipc.md#方案取舍与常见疑问)。

## 兼容性

支持 Xposed 框架：

- LSPosed（推荐）
- EdXposed
- 经典 Xposed Installer

## 相关文档

- [getting-started/INDEX.md](getting-started/INDEX.md) — 指南导航
- [configuration-ui.md](../architecture/configuration-ui.md)
- [crash-logging.md](../architecture/crash-logging.md)
- [crash-log-ipc.md](../architecture/crash-log-ipc.md)
- [navigation-ia.md](../architecture/navigation-ia.md)
- [architecture/overview.md](../architecture/overview.md)
- [glossary.md](../glossary.md)
- [build-and-install.md](build-and-install.md)
