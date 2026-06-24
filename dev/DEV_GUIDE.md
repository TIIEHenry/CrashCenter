---
title: "开发速查手册"
type: concept
status: accepted
phase: N/A
updated: 2026-06-19
summary: "日常开发速查：模块地图、常用命令、工作流；Phase 3 活跃、Gradle 9.2.1"
---

# CrashCenter 开发速查手册

> 快速定位模块、命令、工作流。深度文档见 [docs/INDEX.md](../docs/INDEX.md)；用户指南见 [guides/getting-started/INDEX.md](../docs/guides/getting-started/INDEX.md)。

---

## 1. 项目现状速览

| 项 | 状态 |
|---|------|
| 模块 | 单模块 `:app` |
| 语言 | Java (Xposed core) + Kotlin (UI) |
| SDK | compileSdk 36, minSdk 21, targetSdk 36 |
| 构建 | Gradle 9.2.1, AGP 9.0.0, Kotlin 2.3.0, Java 17 |
| 依赖版本 | `gradle/libs.versions.toml` |
| Xposed API | 82 (compileOnly) |
| 活跃 Phase | Phase 3 — 配置 UI 重设计（Phase 4 崩溃观测 backlog） |
| CI | `build.yml`（main/PR debug）、`release.yml`（`v*` tag + 可选 LSPosed 模块仓）、`xposed-module-release.yml`（手动补发） |

---

## 2. 源码地图

```
app/src/main/
├── assets/xposed_init              → XposedEntry
├── java/nota/android/crash/
│   ├── CrashHandler.java           # 崩溃拦截核心
│   ├── ActivityCrashInfo.java      # 崩溃详情
│   └── xp/
│       ├── XposedEntry.java        # Xposed 入口
│       ├── PrefManager.java        # 偏好 key
│       ├── PrefMigrator.kt         # 旧包 prefs 一次性迁移（含 root）
│       └── app/
│           ├── ActivityMain.kt     # 配置 UI
│           ├── ArrayUtil.java      # 列表工具
│           └── recyclerhelper/     # RecyclerView 辅助
└── res/                            # 布局、字符串、菜单
```

---

## 3. 常用命令

```bash
# 构建
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease

# 文档
./scripts/generate-docs-index.sh
python3 scripts/check-docs-health.py

# 真机 smoke（需 adb + LSPosed）
./scripts/adb-smoke-verification.sh
./scripts/adb-smoke-verification.sh -s 192.168.2.154:5555

# 安装
adb install -r app/build/outputs/apk/debug/CrashCenter_v*.apk

# 发布（详见 docs/guides/release.md）
scripts/extract-changelog.sh 0.1.0   # 校验 CHANGELOG 段落
git tag -a v0.1.0 -m "Release v0.1.0" && git push origin v0.1.0

# 查看 Xposed 日志
adb logcat -s Xposed
```

---

## 4. 开发工作流

```
1. 方案   → docs/architecture/<subsystem>.md
2. ADR    → docs/decisions/NNN-<topic>.md（架构取舍时）
3. 任务   → dev/roadmap/active/phaseN_*.md
   ── commit ── 方案阶段（仅文档）
4. 编码   → 按 checkbox 实施
5. 进度   → dev/progress/status.md + 勾选 roadmap
   ── commit ── 实施阶段（代码 + 行动层文档）
```

完整规则见 [docs/DOCUMENTATION.md](../docs/DOCUMENTATION.md)。

---

## 5. 已知缺口

| 缺口 | 说明 |
|------|------|
| 无 unit/instrumentation test | Phase 2+ 可补 |
| Release 生产签名 | 当前 debug 签名；见 [release.md](../docs/guides/release.md) |
| Phase 4 CrashLogger | 设计就绪，代码待建；root 参考 [root-service-patterns.md](../docs/reference/root-service-patterns.md) |

---

## 6. 关键文档速查

| 主题 | 文档 |
|------|------|
| 系统总览 | [architecture/overview.md](../docs/architecture/overview.md) |
| 崩溃拦截 | [architecture/crash-handler.md](../docs/architecture/crash-handler.md) |
| 崩溃日志 / root 后端 | [crash-log-backends.md](../docs/architecture/crash-log-backends.md) |
| Scope 模型 | [architecture/scope-and-prefs.md](../docs/architecture/scope-and-prefs.md) |
| 构建安装 | [guides/build-and-install.md](../docs/guides/build-and-install.md) |
| 发布 | [guides/release.md](../docs/guides/release.md) · [guides/xposed-module-repo.md](../docs/guides/xposed-module-repo.md) |
| 设备验收 | [verification/README.md](verification/README.md) |
| 术语 | [glossary.md](../docs/glossary.md) |
