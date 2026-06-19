---
title: "ADR-012: 包可见性手动授权"
type: decision
status: accepted
phase: 3
updated: 2026-06-19
summary: "Android 11+ QUERY_ALL_PACKAGES 通过手动 App 信息引导授权，不使用 requestPermissions；PackageVisibilityHelper 检测与降级"
---

# ADR-012: 包可见性手动授权

## 状态

**Accepted** — Phase 3 已实现。

## 背景

CrashCenter 配置 UI 需要枚举设备上所有已安装应用以显示 per-app hook 开关列表。Android 11（API 30）引入 **包可见性过滤**：即使 Manifest 声明 `QUERY_ALL_PACKAGES`（`normal` 保护级别），部分 OEM 或用户侧权限管理可能限制该权限，导致 `PackageManager.getInstalledApplications()` 仅返回部分包。

### 考虑的方案

| 方案 | 可行性 | 缺点 |
|------|--------|------|
| `requestPermissions(QUERY_ALL_PACKAGES)` | ❌ 不可用 | `normal` 权限无系统对话框；不走运行时权限流程 |
| `<queries>` 声明特定包 | ❌ 不可行 | 需 hook 全部包，无法穷举 |
| 引导用户到 App 信息手动开启 | ✅ | 需要用户操作 |
| 忽略 / 降级显示 | ✅ | 列表不完整；新用户困惑 |

## 决策

采用 **手动授权引导 + 降级显示** 组合策略：

### 1. 检测机制（`PackageVisibilityHelper`）

```kotlin
object PackageVisibilityHelper {
    fun isRestricted(context: Context): Boolean
    fun canSeeAllPackages(context: Context): Boolean
}
```

检测维度（多信号综合）：

| 信号 | 说明 |
|------|------|
| API level | < 30 → 视为无限制 |
| `checkSelfPermission(QUERY_ALL_PACKAGES)` | DENIED → 受限 |
| 探测包 `com.android.settings` | 不可见 → 列表被过滤 |
| 已加载列表启发式 | 可见包数 < 预期阈值 → 提示 |

### 2. UI 流程

1. `ActivityMain.onCreate` / `onResume` → `PackageVisibilityHelper.isRestricted()`
2. 受限 → 显示 **PermissionBanner**（条件 banner）
3. 用户点击 banner / "去授权" → `AlertDialog` 说明步骤
4. 确认 → `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))` 跳转 App 信息
5. 用户在系统 UI 中开启相关权限（OEM 文案各异）
6. 返回 → `onResume` 重检测 + 自动重载应用列表

### 3. 降级行为

若用户不授权：

- 仍加载可见的部分列表
- Banner 持续显示
- 不阻塞其他功能（hook 侧不受影响——LSPosed 作用域独立于包可见性）

## 后果

| 方面 | 影响 |
|------|------|
| 用户体验 | 首次需手动操作；返回后自动刷新 |
| 不依赖 root | 授权流程不需 root（模块 UI 进程） |
| hook 侧无影响 | hook 通过 LSPosed 作用域注入；不依赖 `QUERY_ALL_PACKAGES` |
| OEM 兼容 | 部分厂商权限文案不同；提供通用说明 |

## 与 hook 侧的区分

| 场景 | 机制 | 受包可见性影响？ |
|------|------|-----------------|
| **模块 UI 枚举包** | `PackageManager.getInstalledApplications()` | ✅ 是 |
| **hook 侧 createPackageContext(module)** | VFS 直访 + SELinux | 可能（ADR-007 Primary A） |
| **hook 侧 ContentResolver.insert** | Provider authority 路由 | ❌ 否 |
| **XSharedPreferences** | 直读 prefs 文件 | ❌ 否 |

## 相关文档

- [configuration-ui.md](../architecture/configuration-ui.md) — PackageVisibilityHelper 使用
- [scope-and-prefs.md](../architecture/scope-and-prefs.md) — prefs 与 scope
- [crash-log-ipc.md](../architecture/crash-log-ipc.md) — hook 侧包可见性风险
- [design-system.md](../architecture/design-system.md) — PermissionBanner 组件
- [ADR-010](010-scope-policy-show-notify.md) — ScopePolicy 不依赖 UI 包可见性
