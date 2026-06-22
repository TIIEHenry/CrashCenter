# CrashCenter — Xposed 异常拦截模块

在目标 Android 应用中拦截 Java 层未捕获异常，使 app 不因 crash 而退出。**吞掉异常，不修复错误。**

> **新会话必读**：`AGENTS.md` → [`docs/DOCUMENTATION.md`](docs/DOCUMENTATION.md) → [`dev/progress/status.md`](dev/progress/status.md)

---

## 1. 源码目录

| 目录 | 说明 |
|------|------|
| `app/` | 唯一模块：Xposed hook + 配置 UI |
| `app/src/main/java/nota/android/crash/` | 崩溃处理 |
| `app/src/main/java/nota/android/crash/xp/` | Xposed hook 与 prefs |
| `app/src/main/java/nota/android/crash/xp/app/` | 配置 UI |
| `docs/` | 项目文档（架构方案、ADR、指南） |
| `dev/` | 开发追踪（路线图、计划、进度、归档） |
| `scripts/` | 文档索引、健康检查、adb smoke 验收 |

---

## 2. 开发规范

> 完整规则见 [`docs/DOCUMENTATION.md`](docs/DOCUMENTATION.md)。

### 开发流程

```
1. 方案   → docs/architecture/<subsystem>.md
2. ADR    → docs/decisions/NNN-<topic>.md（架构取舍时）
3. 任务   → dev/roadmap/active/phaseN_*.md
   ── commit ── 方案阶段（仅文档，无实现代码）
4. 编码   → 按 checkbox 实施
5. 进度   → dev/progress/status.md + 勾选 roadmap
   ── commit ── 实施阶段（代码 + 行动层文档）
```

**提交前文档门禁**（[规则 3a](docs/DOCUMENTATION.md#规则-3a提交前文档门禁)）：含代码的 commit 前更新 `status.md`、roadmap checkbox、改动过的 `.md`；用户可见行为变更时同步 `docs/architecture/`。

**方案 / 实施 commit 必须分开**。

### 文档维护规则

| # | 规则 | 触发时机 |
|---|------|---------|
| 1 | **编码前查阅相关文档** | 每次编码任务 |
| 2 | **编码后更新进度** | 每次编码会话 |
| 3 | **新设计决策 → 创建 ADR** | 遇到技术选择 |
| 4 | **文档过时 → 归档** | 内容被取代 |
| 5 | **AGENTS.md 是最高权威** | 文档冲突 |
| 6 | **frontmatter**（AGENTS.md 豁免） | 新建或编辑文档 |
| 7 | **不重复创建，不删除文档** | 始终 |
| 8 | **结构变更后跑健康检查** | 每次实施 commit 前 |

### 文档类型与位置

| type | 位置 | 含义 |
|------|------|------|
| `architecture` | `docs/architecture/` | 子系统设计方案 |
| `decision` | `docs/decisions/` | 架构决策记录 (ADR) |
| `concept` | `docs/` | 跨系统约定、术语 |
| `reference` | `docs/reference/` | 外部参考资料 |
| `guide` | `docs/guides/` | 操作指南 |
| `roadmap` | `dev/roadmap/` | 阶段任务清单 |
| `plan` | `dev/plans/` | 实施计划 |
| `progress` | `dev/progress/` | 迭代状态 |

---

## 3. 模块地图

```
XposedEntry (入口)
  ├── shouldHandlePackage()    ← scope 过滤
  ├── selfCheck()              ← 模块自身 hook
  └── Application.onCreate hook
        └── CrashHandler.insert()
              ├── 无限 Looper.loop()
              └── UncaughtExceptionHandler 替换
                    ├── Toast + Notification → ActivityCrashInfo
                    └── CrashLogger (Phase 4 待建) ← 异步 JSONL 持久化

ActivityMain (配置 UI)
  ├── 应用列表 + per-app toggle
  ├── scope_mode / handle_system
  └── SharedPreferences → XSharedPreferences (跨进程)

CrashLogger / 历史统计 (Phase 4 backlog)
 ├── hook 侧 CrashLogCoordinator → 多 Backend（root / Provider / relay）
 ├── 模块侧 CrashLogIngestCoordinator → root harvest → events.jsonl
 └── UI：崩溃历史、统计、导出 — 见 crash-logging.md / crash-log-backends.md
```

详细设计见 [`docs/architecture/overview.md`](docs/architecture/overview.md)；架构演进见 [`docs/architecture/architecture-optimization.md`](docs/architecture/architecture-optimization.md)；崩溃观测见 [`docs/architecture/crash-logging.md`](docs/architecture/crash-logging.md)。

---

## 4. 技术栈

| 项 | 值 |
|---|---|
| compileSdk | 37 |
| targetSdk | 37 |
| minSdk | 26 |
| Kotlin | 2.3 |
| Java | 17 |
| Xposed API | 82 |
| Gradle | 9.2.1 / AGP 9.0.0 |
| applicationId | `nota.android.crash.xp.app` |
| 显示名 | CrashCenter（英文）/ 崩溃中心（中文） |

---

## 5. 常用命令

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./scripts/generate-docs-index.sh
python3 scripts/check-docs-health.py
./scripts/adb-smoke-verification.sh --help
```

GitHub Release：推送 `v*` tag 触发 [`.github/workflows/release.yml`](.github/workflows/release.yml)；见 [`docs/guides/release.md`](docs/guides/release.md)。

详见 [`dev/DEV_GUIDE.md`](dev/DEV_GUIDE.md)。

---

## 6. 文档入口

| 角色 | 路径 |
|------|------|
| LLM / Agent | 本文件 → DOCUMENTATION.md → status.md |
| 架构 | [docs/INDEX.md](docs/INDEX.md) |
| 人类开发者 | [docs/README.md](docs/README.md) → [guides/getting-started/INDEX.md](docs/guides/getting-started/INDEX.md) → [dev/DEV_GUIDE.md](dev/DEV_GUIDE.md) |
| 用户使用 | [docs/guides/usage.md](docs/guides/usage.md) |
| 构建 / 发布 | [build-and-install.md](docs/guides/build-and-install.md) · [release.md](docs/guides/release.md) |
| Root 参考（Phase 4） | [root-service-patterns.md](docs/reference/root-service-patterns.md) |
| 设备验收 | [dev/verification/README.md](dev/verification/README.md) |
| Roadmap | [dev/roadmap/INDEX.md](dev/roadmap/INDEX.md) |
