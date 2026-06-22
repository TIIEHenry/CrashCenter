---
title: "Sibling 项目参考"
type: reference
status: accepted
phase: N/A
updated: 2026-06-22
summary: "CrashCenter 参考的外部 Clarence 生态仓库（GitHub URL；文档引用 SSOT，非 Gradle 构建依赖）"
---

# Sibling 项目参考

CrashCenter 文档中引用的 **外部参考仓库**。本仓库 CI 仅 checkout [CelestailRuler](https://github.com/TIIEHenry/CelestailRuler) 用于 CodeEditor 构建；其余为设计/模式参考，**禁止**在文档中使用本机绝对路径链接。

## CelestailRuler

| 项 | 值 |
|----|-----|
| GitHub | https://github.com/TIIEHenry/CelestailRuler |
| 用途 | `CodeEditor` / `CodeEditorClient` 模块；`CrashInfoActivity` 详情页参考 |
| CI checkout | `.github/workflows/build.yml` → `../CelestailRuler` |
| 本地开发 | `settings.gradle` 可 `projectDir = file('../CelestailRuler/CodeEditor')` |

关键路径（仓库内相对路径）：

| 主题 | 路径 |
|------|------|
| Xposed 入口 | `app/src/main/java/tiiehenry/celestialruler/inject/t4/a.java` |
| Framework hook | `app/src/main/java/tiiehenry/celestialruler/inject/android/AndroidHook.java` |
| 崩溃详情 | `CrashInfoActivity.kt` + `BaseCodeEditorClient.kt` |

详见 [code-editor-porting.md](../architecture/code-editor-porting.md)、[framework-injection-feasibility.md](../architecture/framework-injection-feasibility.md)。

## AppSnapShotor

| 项 | 值 |
|----|-----|
| GitHub | https://github.com/TIIEHenry/AppSnapshoter |
| 文档内名称 | **AppSnapShotor**（本地/历史命名；上游仓库名为 AppSnapshoter） |
| 用途 | Fluent Design token、RootService / libsu 模式、UI Shell 参考 |
| CrashCenter 依赖 | **无** — 仅提炼模式，见 [root-service-patterns.md](root-service-patterns.md) |

关键路径（上游仓库内，若存在）：

| 主题 | 路径 |
|------|------|
| Root 服务 | `docs/architecture/root-service.md` |
| 安全模型 | `docs/architecture/cross-cutting/security.md` |
| UI Shell | `docs/guides/getting-started/ui-shell.md` |
| 悬浮底栏 | `ui/widget/FloatingBottomNav.kt` |

## 文档维护

- 新增 sibling 引用：**先**在本文件登记 GitHub URL，再在架构/决策文档中链接至本页或 `https://` URL。
- **禁止** `/home/...` 绝对路径与 `../../../../SiblingRepo/...` 跨仓库相对路径（`check-docs-health.py --strict-links` 会失败）。

## 相关文档

- [root-service-patterns.md](root-service-patterns.md)
- [code-editor-porting.md](../architecture/code-editor-porting.md)
- [design-system.md](../architecture/design-system.md)
- [glossary.md](../glossary.md)
