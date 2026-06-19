---
title: "包可见性手动授权流程"
type: iteration
status: accepted
phase: 3
updated: 2026-06-19
summary: "Android 11+ QUERY_ALL_PACKAGES 运行时检测、授权条 UI、设置跳转与 onResume 重载"
---

# 包可见性手动授权流程

## 背景

`getInstalledPackages()` 在 Android 11+ 受 package visibility 限制。Manifest 已声明 `QUERY_ALL_PACKAGES`，但该权限为 **normal / install-time**，无标准运行时申请对话框；若未授予，列表静默不完整。

## 实现

| 组件 | 职责 |
|------|------|
| `PackageVisibilityHelper` | `checkSelfPermission` + 探测 `com.android.settings` + Launcher 数量启发式 |
| `permissionBanner` | 状态条下方；文案 + 「去授权」按钮 |
| `ActivityMain` | 启动/`onResume` 检测；对话框说明 → `APPLICATION_DETAILS_SETTINGS`；重载 `initApps()` |

## 验证

- `:app:assembleDebug` 通过
- adb smoke（若设备在线）

## 相关文档

- [configuration-ui.md](../../../docs/architecture/configuration-ui.md)
- [usage.md](../../../docs/guides/usage.md)
