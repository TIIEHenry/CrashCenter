---
title: "项目进度状态"
type: progress
status: active
phase: N/A
updated: 2026-06-19
summary: "文档系统审计修复；Phase 3 收尾、Phase 2 近完成"
---

# 项目进度状态

## Snapshot

| 项 | 状态 |
|---|------|
| 活跃 Phase | [Phase 3 — 配置 UI 重设计](../roadmap/active/phase3_ui_redesign.md) 🔄 |
| Phase 2 | [文档工具与验收](../roadmap/active/phase2_documentation_tooling.md) — 近完成（待 LSPosed 真机报告） |
| 下一 Phase | [Phase 4 — 崩溃可观测性](../roadmap/active/phase4_crash_observability.md) 📋 backlog |
| 阻塞 | LSPosed 手动 UI 项（Test 崩溃拦截、状态条激活色）待验 |
| 验证基线 | `assembleDebug` 通过；adb smoke 自动化（安装+启动）通过 |

### 已完成

- Phase 1 归档：docs/dev 骨架、架构文档、ADR、索引脚本
- Phase 2 近完成：verification 指南、验收模板、adb-smoke 脚本、Cursor 规则（剩真机报告一项）
- 配置 UI Material 重构（ActivityMain / ActivityCrashInfo + 主题资源）
- Phase 3D 结构清理：ViewBinding、内联加载指示器、删 pref_general、过滤空状态
- **Phase 3 Fluent 视觉对齐**：色板/Toolbar/Chip/搜索/列表对齐 AppSnapShotor；ADR-006 M3 defer
- **Phase 3 单屏密度优化**：紧凑状态条、FilterChip 全局设置、扁平列表项；ADR-005 修订为单屏 IA
- **包可见性手动授权**：`PackageVisibilityHelper` + 授权条 + 设置跳转 + `onResume` 重载列表
- **包名迁移**：`nota.android.crash.*` 分包；prefs `crash`；`PrefMigrator` 一次性从 `tiiehenry.xp.grapcrash` 导入
- **应用更名**：显示名 CrashCenter / 崩溃中心；APK `CrashCenter_v*`；文档与字符串中英双语

### 待办

- LSPosed 环境完成手动 smoke（Test 拦截、Switch/prefs、状态条）
- 真机报告补全 `dev/verification/smoke_20260619.md` 手动项
- Phase 2 完成后归档
- Phase 4：按 [phase4_crash_observability.md](../roadmap/active/phase4_crash_observability.md) 实施（[crash-log-backends.md](../../docs/architecture/crash-log-backends.md) + ADR-008 方案已就绪）
- Phase 4 导航 IA：Phase 3/4B **0 tab**；4C+ **2 bottom tab**（配置 \| 观测），观测内 TabLayout（历史 \| 统计）— [navigation-ia.md](../../docs/architecture/navigation-ia.md)

---

## Recent Sessions

### 2026-06-19 — 架构优化设计文档

- 新建 [architecture-optimization.md](../../docs/architecture/architecture-optimization.md)：现状 as-is / 目标 to-be 分层、包结构、8 项关键优化、Phase 4 落地映射
- 更新 overview、phase4 roadmap 交叉链接

### 2026-06-19 — 文档系统审计修复

- CRLF→LF（docs/dev）；`generate-docs-index.sh` 剥离 `\r`；INDEX summary 不再为 `---`
- `scope-and-prefs` 修正 legacy 包 `tiiehenry.xp.grapcrash`；`check-docs-health.py` CRLF / status 行数告警
- 旧会话迁入 [archive/status-sessions-2026-06.md](archive/status-sessions-2026-06.md)；phase3 `in_progress`；phase2 标注近完成

### 2026-06-19 — Gradle 配置同步 AppSnapShotor

- Gradle wrapper 9.2.1；`gradle/libs.versions.toml` version catalog；`gradle.properties` 优化项对齐
- `settings.gradle` 仓库配置简化（google content filter + mavenCentral + jitpack）
- Material 1.13.0（AppSnapShotor 1.14.0 需 minSdk 23，CrashCenter 保留 21 故用 1.13.0）；显式 `kotlin-android` 插件；保留 Java 17 / minSdk 21 / CrashCenter APK 命名
- 新增 Xposed Maven 仓库 `https://api.xposed.info/`（替代原 aliyun/jcenter 镜像解析）

### 2026-06-19 — 自动化构建发布（移植 AppSnapShotor）

- 新增 `.github/workflows/build.yml`（main/PR `assembleDebug`）、`release.yml`（`v*` tag → Release APK）
- 新增 `CHANGELOG.md`、`scripts/extract-changelog.sh`、`.github/prompts/release.md`、`docs/guides/release.md`
- `app/build.gradle`：`versionCode`/`versionName` 改为显式 semver（`0.1.0`），替代日期自动版本
- 更新 build-and-install、AGENTS、DEV_GUIDE、DOCUMENTATION

### 2026-06-19 — 界面路由设计

- 新建 [ui-routing.md](../../docs/architecture/ui-routing.md)：L0–L3 分层、路由表、NavGraph、launchMode、分阶段演进
- 更新 navigation-ia、overview、crash-stats-ui、code-editor-porting 交叉链接

### 2026-06-19 — ADB / logcat 崩溃分析需求

- 新建 [adb-logcat-analysis.md](../../docs/architecture/adb-logcat-analysis.md)：PC 脚本、导入/root 扫描、解析规范、与 JSONL 分工
- 更新 crash-logging、verification README、phase4 4F checklist

### 2026-06-19 — 崩溃统计 UI 需求

- 新建 [crash-stats-ui.md](../../docs/architecture/crash-stats-ui.md)：全局统计页、单应用观测页、指标、导航与验收
- 更新 navigation-ia、crash-logging、phase4 4D checklist

### 2026-06-19 — CodeEditor 移植方案（celestailruler）

- 新建 [code-editor-porting.md](../../docs/architecture/code-editor-porting.md)：模块分层、CrashInfoActivity 对照、Gradle include、Phase 4C 复用
- 更新 configuration-ui / overview / phase4 roadmap

### 2026-06-19 — 崩溃通知流程文档

- 新建 [crash-notification.md](../../docs/architecture/crash-notification.md)：showNotify、线程/进程边界、PendingIntent、stack 差异
- 更新 xposed-entry / crash-handler / overview / glossary 交叉链接

### 2026-06-19 — 应用更名 CrashCenter

- 显示名：英文 CrashCenter、中文崩溃中心（`values` / `values-zh`）
- APK 输出 `CrashCenter_v*`；`settings.gradle` → CrashCenter；README 双语简介
- 文档 bulk 更新 NeverCrash → CrashCenter；relay 目录 `crashcenter_relay`

### 2026-06-19 — 包名迁移与 prefs 导入

- applicationId → `nota.android.crash.xp.app`；源码分包 `crash` / `crash.xp` / `crash.xp.app`
- prefs 文件名 `grapcrash` → `crash`；`PrefMigrator` 首次启动从 `tiiehenry.xp.grapcrash` / `grapcrash.xml` 导入后标记完成
- 更新 AGENTS.md、scope-and-prefs、ADR-003、glossary

### 2026-06-19 — 多后端崩溃日志方案

- 新增 [crash-log-backends.md](../../docs/architecture/crash-log-backends.md)：CrashLogBackend 抽象、hook root 优先并行、模块 root ingest
- 新增 [ADR-008](../../docs/decisions/008-multi-backend-crash-log-storage.md)；ADR-007 标注演进关系
- 用户问答归档：IPC 稳定性、XSharedPreferences、公开 FS、framework 注入、Tab IA、LSPosed 作用域
- [crash-log-ipc.md](../../docs/architecture/crash-log-ipc.md) § 方案取舍与常见疑问；迭代 [ipc-design-qa-2026-06-20.md](../iterations/crash-observability/ipc-design-qa-2026-06-20.md)

### 2026-06-19 — Framework 注入可行性评估

- 参照 celestailruler：framework 侧主要为 `parseQueries`；`XServiceManager` 未接线；IPC 走独立 server APK + Provider
- **结论**：不采用 framework 注入为主架构；保留 ADR-007 app 级 + Provider；可选 `parseQueries` 补丁仅当 Primary A 因包可见性失败
- 新建 [framework-injection-feasibility.md](../../docs/architecture/framework-injection-feasibility.md)；更新 [crash-log-ipc.md](../../docs/architecture/crash-log-ipc.md)

---

更早会话见 [archive/status-sessions-2026-06.md](archive/status-sessions-2026-06.md)。

## 相关

- [roadmap/INDEX.md](../roadmap/INDEX.md)
- [verification/README.md](../verification/README.md)
- [DEV_GUIDE.md](../DEV_GUIDE.md)
