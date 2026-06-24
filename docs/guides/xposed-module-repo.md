---
title: "LSPosed 模块仓库发布指南"
type: guide
status: active
phase: N/A
updated: 2026-06-24
summary: "Xposed-Modules-Repo 上架条件、元数据文件格式与 listing 文案；操作流程见 release.md"
---

# LSPosed 模块仓库发布指南

本文说明 [LSPosed 模块仓库](https://modules.lsposed.org)（Xposed Module Repository）对 **listing 元数据**的要求。完整双渠道发布步骤见 **[release.md](release.md)**。

> **当前状态（v1.0.0）**：模块仓元数据已 push；主仓 [v1.0.0](https://github.com/TIIEHenry/CrashCenter/releases/tag/v1.0.0) 已发布。模块仓 Release `1-1.0.0`：配置 `XPOSED_REPO_TOKEN` 后运行 [xposed-module-release.yml](../../.github/workflows/xposed-module-release.yml) 工作流，或随下次 `v*` tag 自动发布。

## 与主仓库的差异

| 项 | 主仓库 | 模块仓库 |
|----|--------|----------|
| Release tag | `v1.0.0` | `1-1.0.0` |
| 触发 | 推送 tag → CI | 网页手动创建 Release |
| 仓库内容 | 完整源码 | `README` / `SUMMARY` / `SOURCE_URL` + APK |

## 上架条件

官方要求（[submission](https://github.com/Xposed-Modules-Repo/submission)）：

1. **Description 非空** — 列表显示名，建议 `CrashCenter`
2. **至少一个 Release** — tag `{versionCode}-{versionName}`，附 APK
3. 默认分支含 **`README.md`**

## Xposed 模块元数据（APK / manifest）

| 元数据 | 位置 | 值 |
|--------|------|-----|
| 包名 | `applicationId` | `nota.android.crash.xp.app` |
| `xposedmodule` | `AndroidManifest.xml` | `true` |
| `xposeddescription` | `strings.xml` | 英/中 |
| `xposedscope` | `@array/xposed_scope` | 已声明 |
| `xposedminversion` | manifest | `53` |

详见 [xposed-framework.md](../reference/xposed-framework.md)。

## 模块仓库文件

参考 [org.meowcat.example](https://github.com/Xposed-Modules-Repo/org.meowcat.example)。

| 文件 | 必填 | 说明 |
|------|------|------|
| `README.md` | 是 | 完整说明（Markdown） |
| `SUMMARY` | 推荐 | 列表页简介；**不支持** Markdown |
| `SOURCE_URL` | 推荐 | 源码 URL，单行无换行 |
| `SCOPE` | 可选 | 推荐作用域 JSON；manifest 已有 `xposedscope` 时可省略 |
| `ADDITIONAL_AUTHORS` | 可选 | 额外作者 JSON |
| `HIDE` | 可选 | 存在则临时隐藏 listing |

### 仓库 Settings

| 字段 | 建议值 |
|------|--------|
| **Description** | `CrashCenter` |
| **Website** | `https://github.com/TIIEHenry/CrashCenter/issues` |

## 附录：推荐文案

### `SUMMARY`

```
Intercept uncaught Java exceptions in hooked apps to prevent process exit. Observe crashes, analyze stack traces, optional logcat import. Swallows crashes — does not fix bugs. Use with caution.
```

### `SOURCE_URL`

```
https://github.com/TIIEHenry/CrashCenter
```

### `README.md`

```markdown
# CrashCenter / 稳定性中心

Xposed module that intercepts **uncaught Java exceptions** in hooked apps so the process does not exit.

**This module swallows crashes — it does not fix bugs.** Unexpected behavior may occur. Use only on apps you understand; avoid system apps unless necessary.

## Requirements

- Android 8.0+ (API 26+)
- LSPosed or compatible Xposed framework
- Enable this module in the manager and **reboot**
- Set **scope** to target apps in LSPosed; configure observe / intercept per app inside CrashCenter

## Features

- **Intercept** — replace uncaught exception handler in scoped apps
- **Observe** — record crash events without swallowing exceptions
- Per-app managed list, scope mode, crash notification and detail view
- Crash history, statistics, rule-based stack analysis
- Logcat import (root multi-buffer or SAF file), export crash records as zip
- CodeEditor-based stack trace viewer

## Source & support

- Source: https://github.com/TIIEHenry/CrashCenter
- Issues: https://github.com/TIIEHenry/CrashCenter/issues
- User guide: https://github.com/TIIEHenry/CrashCenter/blob/main/docs/guides/usage.md
```

## 相关文档

- [release.md](release.md) — **双渠道发布操作流程（SSOT）**
- [usage.md](usage.md) — 用户安装与作用域
- [xposed-framework.md](../reference/xposed-framework.md)
- [getting-started/INDEX.md](getting-started/INDEX.md)

## 外部链接

- [modules.lsposed.org](https://modules.lsposed.org)
- [提交入口](https://modules.lsposed.org/submission/)
- [Xposed-Modules-Repo/submission](https://github.com/Xposed-Modules-Repo/submission)
- [本模块仓库](https://github.com/Xposed-Modules-Repo/nota.android.crash.xp.app)
