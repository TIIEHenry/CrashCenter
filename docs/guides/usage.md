---
title: "用户使用指南"
type: guide
status: accepted
phase: N/A
updated: 2026-06-23
summary: "模块安装、界面说明、scope 与 LSPosed 作用域、观测 tab、崩溃分析 FAQ"
---

# 用户使用指南

## 模块用途

CrashCenter（崩溃中心）是一个 Xposed 模块，**拦截目标 app 的 Java 异常使其不崩溃**。它**不修复错误**，只是让 app 继续运行。

> ⚠️ 忽略异常可能导致不可预期的行为。建议仅对频繁崩溃的 app 使用，系统 app 请谨慎。

## 安装步骤

1. 安装 APK（见 [build-and-install.md](build-and-install.md) 或 [getting-started/INDEX.md](getting-started/INDEX.md)）
2. 在 Xposed 管理器（LSPosed 推荐）中启用模块
3. 打开 CrashCenter，确认顶部**状态条**显示「模块已激活」（绿色）
4. 在应用列表中配置要 hook 的应用（**当前版本**：全量列表 + Switch；**Phase 3G 起**：受管列表 + 添加应用，见 [受管应用配置](#受管应用配置phase-3g-起)）

若状态条为橙色「模块未激活」，请点击状态条或到 Xposed 管理器启用本模块并重启。

## LSPosed 作用域 vs 应用内 Switch

两套配置**必须同时理解**：

| 机制 | 在哪里设置 | 控制什么 |
|------|------------|----------|
| **LSPosed 作用域** | Xposed / LSPosed 管理器 | 模块代码是否加载进目标 app 进程（**外层门控**） |
| **应用内 Switch** | CrashCenter 受管列表 | 是否**拦截**崩溃（续命）；关闭时**仅观测**，进程可正常退出 |
| **Scope Mode Chip** | CrashCenter 全局 Chip | 是否对**系统应用**安装捕获（见 [scope-and-prefs.md](../architecture/scope-and-prefs.md)） |

管理器未勾选目标 app → 模块**不会**进入该进程，无观测也无拦截。管理器已勾选、Switch 关闭 → **仍记录崩溃**到 `events.jsonl`，但不阻止 app 退出。

崩溃日志记录 LSPosed 作用域内、已被模块捕获的 Java 未捕获异常（含仅观测与已拦截），存储于模块私有 `events.jsonl`。

## 界面说明

主界面为 **双底栏**：**配置** | **观测**（见 [navigation-ia.md](../architecture/navigation-ia.md)）。

### 配置 tab

> **当前版本**使用受管应用模型（Phase 3G）。详见 [app-management-ui.md](../architecture/app-management-ui.md)。

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

## 受管应用配置（Phase 3G 起）

Phase 3G 起，配置方式改为 **显式添加应用** + **干预规则**（[ADR-015](../decisions/015-managed-apps-intervention-rules.md)）：

```
┌─────────────────────────────┐
│  Toolbar（排序 / 帮助）       │
├─────────────────────────────┤
│  状态条 · 包可见性条 · Chip   │
├─────────────────────────────┤
│  搜索 + [全部][已拦截][仅观测] │
├─────────────────────────────┤
│  已安装应用列表（Switch）     │
└─────────────────────────────┘
        │ 点击行
        ▼
   单应用观测页（崩溃历史）
```

| 操作 | 说明 |
|------|------|
| **Switch 打开** | 包名写入 `managed_packages` → 拦截续命 |
| **Switch 关闭** | 从 `managed_packages` 移除 → **仅观测**（仍记录崩溃） |
| **点击行** | 打开该应用的崩溃历史（`PerAppCrashActivity`） |
| **处理系统应用** | Chip 控制 hook 侧是否对系统 app 安装捕获 |
| **显示系统应用** | Chip 控制列表是否显示系统 app |

已纳入 LSPosed 作用域的应用默认 **观测**；Switch 仅控制是否 **拦截**。

## 系统应用与列表过滤

- **处理系统应用**（`handle_system`）：hook 侧是否对系统 app 安装 `CrashHandler`
- **显示系统应用**（`show_system_ui`）：配置列表是否列出系统 app（纯 UI）
- Switch **关闭** → 包不在 `managed_packages` → **仅观测**（ADR-023）

## 观测 tab

底栏选择 **观测** 后，内层有三个子页：

| 子页 | 说明 |
|------|------|
| **历史** | 按时间倒序显示已记录崩溃（含仅观测与已拦截） |
| **统计** | 总次数、独立应用数、应用/异常 TOP、**异常类别 TOP**、**重复崩溃 TOP**、按日计数；点击应用 TOP 可进入**单应用观测页** |
| **logcat** | 通过 SAF 导入 logcat 文本文件，浏览解析后的片段 |

### 观测 Toolbar 菜单（历史子页）

| 选项 | 说明 |
|------|------|
| 筛选 / 按包名筛选 | 缩小历史列表范围 |
| 排序 | 按时间、包名或异常类排序 |
| 导出日志 | 将崩溃记录打包为 zip（含隐私提示） |
| 保留上限 | 设置最大条数与文件大小（默认 500 条 / 8 MB） |
| 清空历史 | 永久删除所有本地崩溃记录（需确认） |

可在 **设置 → 崩溃日志** 相关 pref 中关闭写入（`crash_log_enabled`）；关闭后不再记录新崩溃，但不影响是否拦截。

### 单应用观测

在 **统计** 子页点击 **应用 TOP** 某一行，进入该应用的观测页：显示崩溃次数、最近时间与事件列表。

## 崩溃反馈

**已拦截**（Switch ON）的 app 发生未捕获异常时：

1. 弹出 Toast 显示异常信息（若规则允许通知）
2. 发送系统通知（可点击查看详情；Intent 携带 `crash_id`）
3. App **不会退出**
4. 若崩溃日志已启用，事件写入本地 `events.jsonl`

**仅观测**（Switch OFF）时：通常无 Toast/通知；进程按系统路径退出；崩溃仍尝试写入 `events.jsonl`。

通知或历史列表点开后进入**崩溃详情**：stack trace 为等宽字体（CodeEditor），可复制；若规则匹配成功，会显示**分析卡片**（异常类别、排查建议）。分析仅为参考，模块**不会**自动修复目标 app。

技术说明：崩溃日志**不能**用 XSharedPreferences 传递，须模块私有存储 + IPC — 见 [crash-log-ipc FAQ](../architecture/crash-log-ipc.md#方案取舍与常见疑问)。

## 兼容性

支持 Xposed 框架：

- LSPosed（推荐）
- EdXposed
- 经典 Xposed Installer

## 相关文档

- [getting-started/INDEX.md](getting-started/INDEX.md) — 指南导航
- [app-management-ui.md](../architecture/app-management-ui.md)
- [ADR-015](../decisions/015-managed-apps-intervention-rules.md)
- [configuration-ui.md](../architecture/configuration-ui.md)
- [crash-logging.md](../architecture/crash-logging.md)
- [crash-intelligent-analysis.md](../architecture/crash-intelligent-analysis.md)
- [crash-stats-ui.md](../architecture/crash-stats-ui.md)
- [crash-log-ipc.md](../architecture/crash-log-ipc.md)
- [navigation-ia.md](../architecture/navigation-ia.md)
- [injection-observe-intercept-split.md](../architecture/injection-observe-intercept-split.md)
- [ADR-023](../decisions/023-injection-observe-intercept-split.md)
- [glossary.md](../glossary.md)
- [build-and-install.md](build-and-install.md)
