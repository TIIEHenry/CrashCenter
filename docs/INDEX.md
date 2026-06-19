---
title: "文档索引"
type: concept
status: accepted
phase: N/A
updated: 2026-06-19
summary: "docs/ + dev/ 完整导航索引（自动生成）"
---

# CrashCenter 文档索引

> **本文件由脚本自动生成，请勿手动编辑。**
> Xposed 异常拦截模块文档集

---

## 文档系统

| 文件 | 说明 |
|------|------|
| [DOCUMENTATION.md](DOCUMENTATION.md) | **LLM 维护规则** |
| [DOC-SPEC.md](DOC-SPEC.md) | **文档系统规范** |
| [glossary.md](glossary.md) | **术语表** |
| [AGENTS.md](../AGENTS.md) | **项目权威入口** |
| [dev/DEV_GUIDE.md](../dev/DEV_GUIDE.md) | **开发速查手册** |

---

## 架构方案（`docs/architecture/`）

| 文档 | 内容 |
|------|------|
| [adb-logcat-analysis.md](architecture/adb-logcat-analysis.md) | 通过 PC adb 或本机 root 读取 logcat 辅助验收与崩溃分析；与 JSONL 观测层互补，非 SSOT 替代 |
| [architecture-optimization.md](architecture/architecture-optimization.md) | 现状债务清单、目标分层与包结构、Phase 4 落地映射；prescriptive 演进路线图 |
| [code-editor-porting.md](architecture/code-editor-porting.md) | 参照 celestailruler CodeEditor 三模块，在 CrashCenter 中替换 TextView 详情页并支撑 Phase 4 崩溃历史浏览 |
| [configuration-ui.md](architecture/configuration-ui.md) | ActivityMain 单屏高密度 Fluent/Material UI：AppSnapShotor 视觉对齐、透明状态栏 + inset、Chip 全局设置、搜索与列表、包可见性手动授权 |
| [crash-handler.md](architecture/crash-handler.md) | 通过 Looper 续命与 UncaughtExceptionHandler 替换拦截崩溃 |
| [crash-intelligent-analysis.md](architecture/crash-intelligent-analysis.md) | 在 JSONL 观测层之上做规则分类、签名聚类与诊断建议；默认端侧离线，不自动修复目标 app |
| [crash-log-backends.md](architecture/crash-log-backends.md) | CrashLogBackend 抽象、hook 侧 root 优先并行写入、模块侧 root ingest 与管理；canonical JSONL 为 SSOT |
| [crash-logging.md](architecture/crash-logging.md) | hook 侧异步持久化全量拦截崩溃；多后端编排见 crash-log-backends.md |
| [crash-log-ipc.md](architecture/crash-log-ipc.md) | hook 目标进程向模块进程写入 CrashEvent 的 IPC 机制对比；编排见 crash-log-backends.md（多后端并行、root 优先） |
| [crash-notification.md](architecture/crash-notification.md) | 目标 app 崩溃后 Toast / 系统通知的触发条件、线程模型、PendingIntent 与 ActivityCrashInfo 详情页 |
| [crash-stats-ui.md](architecture/crash-stats-ui.md) | 观测 tab 全局统计页与单应用崩溃列表/统计页的信息架构、指标、导航与数据聚合需求（Phase 4D） |
| [framework-injection-feasibility.md](architecture/framework-injection-feasibility.md) | 参照 celestailruler 评估 System Framework 注入对 CrashCenter 的价值；结论：不采用为主架构，保留 ADR-007 app 级 + Provider，可选 parseQueries 补丁 |
| [navigation-ia.md](architecture/navigation-ia.md) | 分阶段导航：Phase 3/4B 无 tab；Phase 4C+ 双底栏（配置 | 观测），观测内 TabLayout（历史 | 统计）；路由表见 ui-routing.md |
| [overview.md](architecture/overview.md) | Xposed 异常拦截模块的整体架构与数据流；Phase 4 多后端观测层；演进见 architecture-optimization.md |
| [scope-and-prefs.md](architecture/scope-and-prefs.md) | SharedPreferences 键、scope 模式与跨进程同步；legacy tiiehenry.xp.grapcrash 迁移 |
| [ui-routing.md](architecture/ui-routing.md) | Activity/Fragment 路由表、外部 Intent 入口、返回栈与 Phase 4C+ Navigation 图；信息架构见 navigation-ia.md |
| [xposed-entry.md](architecture/xposed-entry.md) | XposedEntry 的包过滤、hook 安装与通知展示 |

---

## 架构决策（`docs/decisions/`）

| ADR | 决策 | 状态 |
|-----|------|------|
| [001-looper-loop-resurrection.md](decisions/001-looper-loop-resurrection.md) | 主线程 crash 后通过无限 Looper.loop 恢复事件循环，使 app 继续运行 |  |
| [002-inverted-package-toggle.md](decisions/002-inverted-package-toggle.md) | package_list 存储禁用包名，Switch 开启表示 hook，默认全选 |  |
| [003-xsharedpreferences-cross-process.md](decisions/003-xsharedpreferences-cross-process.md) | hook 侧只读 UI 写入的 scope 配置；不适用崩溃事件体持久化 |  |
| [004-build-toolchain-jdk17.md](decisions/004-build-toolchain-jdk17.md) | JDK 17 + Gradle 9.2.1 / AGP 9.0.0 / compileSdk 36；与 AppSnapShotor 对齐除 Java 版本 |  |
| [005-settings-information-architecture.md](decisions/005-settings-information-architecture.md) | 配置 tab 单屏高密度：全局 Chip 与列表同屏；Phase 4C+ 观测域允许 2-tab 壳层（见 navigation-ia.md） |  |
| [006-material3-toolchain.md](decisions/006-material3-toolchain.md) | 工具链已升级（ADR-004）；Material 3 完整主题仍 defer，当前 Material Components 2.x Fluent 对齐 |  |
| [007-crash-log-cross-process-storage.md](decisions/007-crash-log-cross-process-storage.md) | JSONL canonical + Provider；编排扩展见 ADR-008 |  |
| [008-multi-backend-crash-log-storage.md](decisions/008-multi-backend-crash-log-storage.md) | CrashLogBackend 抽象；hook root 优先并行写入；模块 root ingest 管理 relay；ADR-007 canonical 仍有效 |  |

---

## 参考资料（`docs/reference/`）

| 文档 | 内容 |
|------|------|
| [root-service-patterns.md](reference/root-service-patterns.md) | 从 AppSnapShotor 提炼的 libsu RootService 模式；CrashCenter Phase 4 ingest 侧参考，非产品依赖 |
| [xposed-framework.md](reference/xposed-framework.md) | CrashCenter 使用的 Xposed API 与框架兼容性 |

---

## 指南（`docs/guides/`）

| 文档 | 内容 |
|------|------|
| [build-and-install.md](guides/build-and-install.md) | Gradle 9.2.1 构建、version catalog、签名与 APK 安装 |
| [release.md](guides/release.md) | GitHub Release 发布流程、CHANGELOG 维护与 AI 辅助发布（移植自 AppSnapShotor） |
| [usage.md](guides/usage.md) | 模块安装、界面说明、scope 与 LSPosed 作用域、包可见性、崩溃观测 FAQ |

---

## 开发追踪（`dev/`）

| 目录 | 说明 |
|------|------|
| [dev/README.md](../dev/README.md) | dev/ 使用说明 |
| [dev/DEV_GUIDE.md](../dev/DEV_GUIDE.md) | 开发速查手册 |

### 路线图（`dev/roadmap/`）

#### Active

| Phase | 文档 | 说明 | 状态 |
|-------|------|------|------|
| 2 | [phase2_documentation_tooling.md](../dev/roadmap/active/phase2_documentation_tooling.md) | 文档工具与验收体系 | 🔄 |
| 3 | [phase3_ui_redesign.md](../dev/roadmap/active/phase3_ui_redesign.md) | 配置 UI 重设计 | 🔄 |
| 4 | [phase4_crash_observability.md](../dev/roadmap/active/phase4_crash_observability.md) | 崩溃可观测性 |  |

#### Archive

| Phase | 文档 | 说明 | 状态 |
|-------|------|------|------|
| 1 | [phase1_documentation_system.md](../dev/roadmap/archive/phase1_documentation_system.md) | 文档系统建设 | ✅ |

### 实施计划（`dev/plans/`）

| 文档 | 内容 |
|------|------|
| [ui-redesign-execution-plan.md](../dev/plans/ui-redesign-execution-plan.md) | Material v1 验收门禁 + 信息架构/体验/结构分层实施路径 |

### 进度追踪（`dev/progress/`）

| 文档 | 内容 |
|------|------|
| [status.md](../dev/progress/status.md) | 文档系统审计修复；Phase 3 收尾、Phase 2 近完成 |

### 设备验收（`dev/verification/`）

| 文档 | 内容 |
|------|------|
| [README.md](../dev/verification/README.md) | CrashCenter 真机 adb 验收入口、脚本与报告规范 |
| [smoke_20260619.md](../dev/verification/smoke_20260619.md) | assembleDebug 与 adb 自动化 smoke 通过；LSPosed 手动项待补 |

---

## 按阅读路径

### 新人入门
1. [AGENTS.md](../AGENTS.md) — 项目全貌
2. [dev/DEV_GUIDE.md](../dev/DEV_GUIDE.md) — 开发速查
3. [architecture/overview.md](architecture/overview.md) — 系统总览
4. [guides/usage.md](guides/usage.md) — 用户使用

### 若关注 Xposed hook 机制
1. [architecture/xposed-entry.md](architecture/xposed-entry.md)
2. [architecture/crash-handler.md](architecture/crash-handler.md)
3. [decisions/001-looper-loop-resurrection.md](decisions/001-looper-loop-resurrection.md)

### 若关注配置与 scope
1. [architecture/scope-and-prefs.md](architecture/scope-and-prefs.md)
2. [architecture/configuration-ui.md](architecture/configuration-ui.md)
3. [decisions/002-inverted-package-toggle.md](decisions/002-inverted-package-toggle.md)

---

*索引生成日期：2026-06-19*
