---
title: "配置界面 Material UI 重构"
type: plan
status: implemented
phase: N/A
updated: 2026-06-19
summary: "ActivityMain / ActivityCrashInfo Material 化：布局、主题、交互迁移与代码清理"
---

# 配置界面 Material UI 重构

> 子系统：配置 UI（`:app`）
> 架构依据：[configuration-ui.md](../../../docs/architecture/configuration-ui.md)

## 背景

原 UI 为纯 `LinearLayout` + 溢出菜单堆叠选项，列表项信息密度过高（安装/更新时间），缺少 Xposed 激活状态的常驻反馈，且 `menu_main.xml` 引用的矢量图标缺失。

## 目标

1. 采用 Material Components，主操作留在主界面可见区域
2. 简化列表项，保留 per-app Switch 核心交互
3. 崩溃详情页可读性提升（Toolbar + 等宽 trace）
4. 清理遗留测试代码与列表点击索引 bug

## 改动摘要

### 依赖

| 包 | 用途 |
|----|------|
| `androidx.appcompat` | `AppCompatActivity`、Toolbar |
| `com.google.android.material` | 卡片、Switch、Chip、TextInputLayout |
| `androidx.constraintlayout` | 列表项布局 |

### 布局与资源

| 文件 | 变更 |
|------|------|
| `activity_main.xml` | Toolbar、状态条、设置卡片、搜索、Chip、RecyclerView |
| `activity_main_appitem.xml` | `MaterialCardView` + 系统应用标签 |
| `activity_crashinfo.xml` | Toolbar + `NestedScrollView` |
| `colors.xml` / `styles.xml` / `dimens.xml` | 主题色与间距 |
| `drawable/ic_*.xml` | 搜索、排序、盾牌等矢量图 |
| `menu_main.xml` | 仅保留排序、批量、关于、测试 |

### 逻辑 (`ActivityMain.kt`)

- 基类改为 `AppCompatActivity`
- [Scope Mode](../../../docs/glossary.md#scope-mode) / 系统应用开关绑定设置卡片
- 搜索 `TextWatcher` + Chip 过滤替代菜单 SearchView / Filter
- 列表点击使用 `data` 对象，避免过滤后索引错位
- 移除 `CountDownTimer` 测试代码
- 新增 `PREF_SHOW_SYSTEM_UI` 持久化「显示系统应用」

### 崩溃详情 (`ActivityCrashInfo.java`)

- `AppCompatActivity` + 返回导航
- stack trace 使用 `AppTheme.CrashTrace` 等宽样式

## 行为变更（用户可见）

| 之前 | 之后 |
|------|------|
| 作用域/系统开关在溢出菜单 | 主界面设置卡片 |
| SearchView 在 Toolbar | 主界面搜索框 |
| 过滤在子菜单 | Chip：全部 / 已应用 / 未应用 |
| 列表显示安装/更新时间 | 仅名称、包名、系统标签 |
| 未激活仅弹窗 | 顶部状态条 + 首次弹窗 |
| 崩溃详情纯 TextView | Toolbar + 可选中复制 |

业务语义不变：Switch 开启 = hook；[Disabled Package](../../../docs/glossary.md#disabled-package) 仍写入 `package_list`。

## 验证

- [x] `./gradlew :app:assembleDebug`（AGP 9.0.0 / Gradle 9.2.1，Java 17）
- [ ] 真机：状态条激活/未激活、搜索、Chip、Switch 写 prefs（自动化 smoke 已安装启动；UI 项待 LSPosed 手动）
- [ ] LSPosed scope 与 UI 配置一致

## 相关文档

- [configuration-ui.md](../../../docs/architecture/configuration-ui.md)
- [usage.md](../../../docs/guides/usage.md)
- [scope-and-prefs.md](../../../docs/architecture/scope-and-prefs.md)
- [glossary.md](../../../docs/glossary.md)
