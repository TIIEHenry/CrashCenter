---
title: "文档索引"
type: concept
status: accepted
phase: N/A
updated: 2026-06-24
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
| [adb-logcat-analysis.md](architecture/adb-logcat-analysis.md) | 通过 PC adb 或本机 root 读取 logcat；含 ANR_HINT（ADR-025）；与 JSONL 互补，非 SSOT |
| [anr-observation.md](architecture/anr-observation.md) | ANR 不入 events.jsonl SSOT；线索来自 logcat system/events 与可选 ApplicationExitInfo；否决 Framework hook（ADR-025） |
| [app-di-and-module-boundaries.md](architecture/app-di-and-module-boundaries.md) | ServiceLocator 手动 DI、ViewModelFactory、hook 包禁止依赖 xp.app 的门禁与测试替身 |
| [app-management-ui.md](architecture/app-management-ui.md) | 全量已安装应用列表 + Switch 控 managed_packages 拦截；已移除策展列表与 intervention_rules |
| [architecture-optimization.md](architecture/architecture-optimization.md) | 现状债务清单、目标分层与包结构、Phase 4 落地映射；4D/4E/4F/4G-MVP 部分 as-built |
| [code-editor-porting.md](architecture/code-editor-porting.md) | CodeEditor + CodeEditorClient vendored；CodeEditorAntlr 未纳入构建；日志浏览用 TextLanguage |
| [configuration-ui.md](architecture/configuration-ui.md) | ConfigFragment 全量应用列表 + managed_packages Switch；已移除 Legacy/策展/intervention_rules 双轨 |
| [crash-capture-pipeline.md](architecture/crash-capture-pipeline.md) | CrashCapturePipeline：观测 logSync / 拦截 logAsync；ScopeDecision install+intercept |
| [crash-data-layer.md](architecture/crash-data-layer.md) | CrashLogRepository 读口 as-built（Paging3 + LRU + clear/deleteById/applyRetention）；4B-γ FileLock 统一见 crash-log-filesystem.md |
| [crash-event-timeline-ui.md](architecture/crash-event-timeline-ui.md) | CrashHistoryFragment 的时间线呈现规范：按时间组织 CrashEvent、复用 Repository/详情路由，不新增独立页面或第三个 tab |
| [crash-export-retention.md](architecture/crash-export-retention.md) | Phase 4E SAF 导出 JSONL/zip、通知 crash_id Intent、retention 配置 UI 与轮转策略 |
| [crash-handler.md](architecture/crash-handler.md) | INTERCEPT 续命 + OBSERVE 转发；由 ScopeDecision.shouldIntercept 选择模式 |
| [crash-history-ui.md](architecture/crash-history-ui.md) | Phase 4C-β CrashHistoryFragment：Paging、筛选/排序、三子 tab 宿主；详情含 4G 分析卡片 |
| [crash-intelligent-analysis.md](architecture/crash-intelligent-analysis.md) | 4G-MVP + 4G-V2 部分 as-built：RuleEngine、详情 lazy 分析、CrashSignature 统计聚类；JSONL analysis 持久化 defer |
| [crash-log-backends.md](architecture/crash-log-backends.md) | ADR-024：hook LocalCacheBackend 写 + 模块 RootFs 聚合读；历史多后端见归档章节 |
| [crash-log-distributed-storage.md](architecture/crash-log-distributed-storage.md) | 取消模块 canonical SSOT；各 app cache 写（无 root）；UI 须 root 聚合读，无降级 |
| [crash-log-filesystem.md](architecture/crash-log-filesystem.md) | events.jsonl 跨进程读写、FileLock 统一、时间倒序读口、dedupe 与 IS 验收；衔接 ADR-017 / 4B-β |
| [crash-logging.md](architecture/crash-logging.md) | CrashEvent JSONL；4B-δ 分布式 cache 写 + root 聚合读（ADR-024） |
| [crash-log-ipc.md](architecture/crash-log-ipc.md) | hook 目标进程向模块进程写入 CrashEvent 的 IPC 机制对比；编排见 crash-log-backends.md（多后端并行、root 优先） |
| [crash-notification.md](architecture/crash-notification.md) | 目标 app 崩溃后 Toast / 系统通知的触发条件、线程模型、PendingIntent 与 ActivityCrashInfo 详情页 |
| [crash-stats-ui.md](architecture/crash-stats-ui.md) | observe/detail 域全局统计页与单应用观测页 as-built（4D+ 部分） |
| [dark-mode-theming.md](architecture/dark-mode-theming.md) | Phase A–D 已编码；Meizu 实机 QA PASS；M3 迁移见 ADR-022（静态 Fluent，无 dynamic color） |
| [design-system.md](architecture/design-system.md) | 桥接 docs/design/ 视觉语言与 CrashCenter res/ 实现：Fluent token、共享 UI 组件、域复用规则；M3 静态主题见 material3-migration |
| [framework-injection-feasibility.md](architecture/framework-injection-feasibility.md) | 不采用 System Framework 为主架构；AMS crash/ANR hook 均否决；ANR 走 logcat（ADR-025）；可选 parseQueries 补丁 |
| [injection-observe-intercept-split.md](architecture/injection-observe-intercept-split.md) | 默认对所有 LSPosed 作用域内 app 注入观测；行内 Switch 仅控制是否拦截续命；观测与拦截共用 CrashCapturePipeline |
| [logcat-multi-source.md](architecture/logcat-multi-source.md) | Root 实时读取 + 文件导入；ANR 路径 A 见 buffer P2/P3（ADR-025）；status accepted 供编码 |
| [material3-migration.md](architecture/material3-migration.md) | minSdk 26 + Theme.Material3.DayNight 静态 Fluent 映射；拒绝 dynamic color；分 M0–M4 实施与验收 |
| [navigation-ia.md](architecture/navigation-ia.md) | 分阶段导航：Phase 4C+ 双底栏（配置 | 观测），观测内 TabLayout（历史 | 统计 | logcat）；路由表见 ui-routing.md |
| [overview.md](architecture/overview.md) | Xposed 异常拦截模块的整体架构与数据流；4B-α 观测 + 4C-α UI Shell as-built；演进见 architecture-optimization.md |
| [scope-and-prefs.md](architecture/scope-and-prefs.md) | SharedPreferences 键、ScopePolicy install/intercept；managed_packages 单模型 |
| [ui-routing.md](architecture/ui-routing.md) | MainShellActivity、ConfigFragment、ObserveHost 与详情 Activity 的路由表、Intent 兼容参数、返回栈与 Phase 4C+ Navigation 图 |
| [unified-root-service.md](architecture/unified-root-service.md) | 单 CrashCenterRootService + RootBroker 多子 Binder；模块进程统一特权 I/O；hook 侧同契约 Shell 适配器 |
| [xposed-entry.md](architecture/xposed-entry.md) | XposedEntry 薄入口：ScopePolicy install/intercept、CrashHandler 双模式、委托 CrashCapturePipeline |

---

## 架构决策（`docs/decisions/`）

| ADR | 决策 | 状态 |
|-----|------|------|
| [001-looper-loop-resurrection.md](decisions/001-looper-loop-resurrection.md) | 主线程 crash 后通过无限 Looper.loop 恢复事件循环，使 app 继续运行 |  |
| [002-inverted-package-toggle.md](decisions/002-inverted-package-toggle.md) | package_list 存储禁用包名，Switch 开启表示 hook，默认全选 |  |
| [003-xsharedpreferences-cross-process.md](decisions/003-xsharedpreferences-cross-process.md) | hook 侧只读 UI 写入的 scope 配置；不适用崩溃事件体持久化 |  |
| [004-build-toolchain-jdk17.md](decisions/004-build-toolchain-jdk17.md) | JDK 17 + Gradle 9.2.1 / AGP 9.0.0 / compileSdk 36；与 AppSnapShotor 对齐除 Java 版本 |  |
| [005-settings-information-architecture.md](decisions/005-settings-information-architecture.md) | 配置 tab 单屏高密度：全局 Chip 与列表同屏；Phase 3G 受管列表（ADR-015）；Phase 4C+ 观测域 2-tab |  |
| [006-material3-toolchain.md](decisions/006-material3-toolchain.md) | 已取代：Phase 3/4 曾 defer M3；2026-06-22 由 ADR-022 启动 minSdk 26 + 静态 M3 Fluent 迁移 | _(已归档)_ |
| [007-crash-log-cross-process-storage.md](decisions/007-crash-log-cross-process-storage.md) | JSONL canonical + Provider；编排扩展见 ADR-008 |  |
| [008-multi-backend-crash-log-storage.md](decisions/008-multi-backend-crash-log-storage.md) | CrashLogBackend 抽象；hook root 优先并行写入；模块 root ingest 管理 relay；ADR-007 canonical 仍有效 |  |
| [009-ui-shell-design-system.md](decisions/009-ui-shell-design-system.md) | 确立 MainShellActivity + Fluent Design System + domain pages + feature state 四层 UI 架构；ActivityMain 降级为 ConfigFragment，详情路由兼容旧 Exception extra |  |
| [010-scope-policy-show-notify.md](decisions/010-scope-policy-show-notify.md) | 将 XposedEntry.showNotify 静态字段替换为 ScopePolicy 实例级 ScopeDecision，消除多包并发竞态 |  |
| [011-feedback-failure-isolation.md](decisions/011-feedback-failure-isolation.md) | CrashFeedbackFacade 与 CrashLogCoordinator 各自独立 try/catch；任一失败不影响另一方，禁止 System.exit |  |
| [012-package-visibility-manual-grant.md](decisions/012-package-visibility-manual-grant.md) | Android 11+ QUERY_ALL_PACKAGES 通过手动 App 信息引导授权，不使用 requestPermissions；PackageVisibilityHelper 检测与降级 |  |
| [013-notification-crash-id-intent.md](decisions/013-notification-crash-id-intent.md) | Phase 4E 起 Notification PendingIntent 传 crash_id UUID 替代整段 stack extra，详情页从 Repository 加载；保留 Exception extra 兼容过渡 |  |
| [014-legacy-prefs-migration.md](decisions/014-legacy-prefs-migration.md) | 已移除：2026-06-24 删除 PrefMigrator 与 tiiehenry.xp.grapcrash 导入；新安装仅使用 crash.xml 当前键 | _(已归档)_ |
| [015-managed-apps-intervention-rules.md](decisions/015-managed-apps-intervention-rules.md) | 配置域改为 managed_packages 策展列表 + intervention_rules JSON；hook 门控已由 ADR-023 修订 |  |
| [017-root-ingest-and-dedupe.md](decisions/017-root-ingest-and-dedupe.md) | 落实 ADR-008 Phase 1 RootSu + 模块 ingest merge；canonical 按 crash_id 去重；读路径可选防御性 dedupe |  |
| [021-canonical-jsonl-io-consistency.md](decisions/021-canonical-jsonl-io-consistency.md) | events.jsonl 变异统一 FileLock；Repository 读口 timestampMs 降序；删改经 CanonicalJsonlStore |  |
| [022-material3-static-theme-minsdk26.md](decisions/022-material3-static-theme-minsdk26.md) | minSdk 升至 26；UI 迁移 Theme.Material3.DayNight + Fluent 静态语义色；明确拒绝 dynamic color / 壁纸取色；取代 ADR-006 defer |  |
| [023-injection-observe-intercept-split.md](decisions/023-injection-observe-intercept-split.md) | 默认对 LSPosed 作用域内 app 安装观测捕获；Switch/CATCH_ALL 仅控 shouldIntercept；修订 ADR-015 hook 门控 |  |
| [024-distributed-cache-crash-storage.md](decisions/024-distributed-cache-crash-storage.md) | 各 app cache 写（无 root）；UI 须 root 聚合读；无无 root 降级 |  |
| [025-anr-observation-no-framework-hook.md](decisions/025-anr-observation-no-framework-hook.md) | 否决 system_server / AnrHelper AMS hook；ANR 线索走 logcat 多源（P2/P3）与可选 ApplicationExitInfo，不进入 Framework 注入主路径 |  |

---

## 参考资料（`docs/reference/`）

| 文档 | 内容 |
|------|------|
| [root-service-patterns.md](reference/root-service-patterns.md) | 从 AppSnapShotor 提炼的 libsu RootService 模式；CrashCenter 统一方案见 unified-root-service.md |
| [sibling-projects.md](reference/sibling-projects.md) | CrashCenter 参考的外部 Clarence 生态仓库（GitHub URL；文档引用 SSOT，非 Gradle 构建依赖） |
| [xposed-framework.md](reference/xposed-framework.md) | CrashCenter 使用的 Xposed API 与框架兼容性 |

---

## 指南（`docs/guides/`）

| 文档 | 内容 |
|------|------|
| [build-and-install.md](guides/build-and-install.md) | Gradle 9.2.1 构建、version catalog、签名与 APK 安装 |
| [release.md](guides/release.md) | 双渠道发布：主仓库 GitHub Release（v* tag + CI）与 LSPosed 模块仓（{versionCode}-{versionName} + XPOSED_REPO_TOKEN） |
| [usage.md](guides/usage.md) | 模块安装、界面说明、scope 与 LSPosed 作用域、观测 tab、崩溃分析 FAQ |
| [xposed-module-repo.md](guides/xposed-module-repo.md) | Xposed-Modules-Repo 上架条件、元数据文件格式与 listing 文案；操作流程见 release.md |

---

## 设计规范（`docs/design/`）

| 文档 | 内容 |
|------|------|
| [INDEX.md](design/INDEX.md) | Clarence 生态设计 SSOT 入口——UI 模式、视觉 token、交互范式、组件标准。 |
| [interaction-language.md](design/interaction-language.md) | Clarence 生态交互 SSOT——InputModality、导航、Popup 策略；组件 spec 见 components/。 |
| [ui-modes.md](design/ui-modes.md) | 生态两种 UI 范式——信息流大 UI（内容为主、悬浮 chrome）与工具密度 UI（按钮/窗口多）；同页可分区交叉组合。 |
| [visual-language.md](design/visual-language.md) | Clarence 生态视觉 SSOT——token、轻量通透、按压范式；组件 spec 见 components/。 |

### 组件（`docs/design/components/`）

| 文档 | 内容 |
|------|------|
| [INDEX.md](design/components/INDEX.md) | Clarence 生态可复用 UI 组件 catalog——分割线、列表、Sheet、Popup、悬浮 chrome、表单控件；含 Web demo。 |
| [draggable-half-sheet.md](design/components/draggable-half-sheet.md) | TouchPrimary 半屏 BottomSheet——顶栏内嵌拖曳把手、Half/Full chrome 形态切换、28dp 顶缘、把手驱动展开/收起/关闭。 |
| [floating-chrome.md](design/components/floating-chrome.md) | FloatingToolbar 族 + 滚动联动边缘 Scrim（顶 blur 渐变 / 底渐隐）；自适应前景色。 |
| [form-controls.md](design/components/form-controls.md) | FilterChip、Switch、搜索框、筛选图标的视觉与交互规范。 |
| [popup-filter-menu.md](design/components/popup-filter-menu.md) | anchored Popup、筛选菜单、Master 行、popup_section_gap；按住滑动选单（Press-Drag-Release）。 |
| [selection-bubble.md](design/components/selection-bubble.md) | 文本选择 / 长按文本时出现的水平 pill 悬浮条——快捷动作、可选 inline 输入；实色 layer、内容自适应宽。 |
| [settings-card-detail-sheet.md](design/components/settings-card-detail-sheet.md) | 卡片式设置、相册式详情 BottomSheet——实色 layer、item 对齐 divider、内容 pill。 |
| [shared-dividers.md](design/components/shared-dividers.md) | Card / Sheet / Popup 共用的 item 对齐 divider 与 popup_section_gap 规范。 |
| [toolbar-list-chrome.md](design/components/toolbar-list-chrome.md) | 共面 Toolbar、扁平 List 行、Banner、筛选行的视觉与按压规范。 |

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
| 3 | [phase3_ui_redesign.md](../dev/roadmap/active/phase3_ui_redesign.md) | 配置 UI 重设计 | 🔄 |
| 4 | [phase4_crash_observability.md](../dev/roadmap/active/phase4_crash_observability.md) | 崩溃可观测性 | 🔄 |
| 5 | [phase5_observe_intercept_split.md](../dev/roadmap/active/phase5_observe_intercept_split.md) | 全量观测与拦截分离 | 🔄 |

#### Archive

| Phase | 文档 | 说明 | 状态 |
|-------|------|------|------|
| 1 | [phase1_documentation_system.md](../dev/roadmap/archive/phase1_documentation_system.md) | 文档系统建设 | ✅ |
| 2 | [phase2_documentation_tooling.md](../dev/roadmap/archive/phase2_documentation_tooling.md) | 文档工具与验收体系 | ✅ |

### 实现日志（`dev/iterations/`）

| 文档 | 内容 |
|------|------|
| [configuration-ui/appsnapshot-style-alignment-2026-06-19.md](../dev/iterations/configuration-ui/appsnapshot-style-alignment-2026-06-19.md) | 配置 UI 对齐 AppSnapShotor Fluent 设计语言：色板、扁平 Toolbar、Chip/搜索/列表样式 |
| [configuration-ui/density-optimization-2026-06-19.md](../dev/iterations/configuration-ui/density-optimization-2026-06-19.md) | 单屏保留全局设置，压缩状态条/Chip/列表项，释放 RecyclerView 空间 |
| [configuration-ui/managed-apps-intervention-2026-06-19.md](../dev/iterations/configuration-ui/managed-apps-intervention-2026-06-19.md) | Phase 3G 配置 IA 从全量列表改为受管策展 + Half Sheet 添加 + 行内 Switch + 编辑页规则 |
| [configuration-ui/material-ui-redesign-2026-06-19.md](../dev/iterations/configuration-ui/material-ui-redesign-2026-06-19.md) | ActivityMain / ActivityCrashInfo Material 化：布局、主题、交互迁移与代码清理 |
| [configuration-ui/permission-flow-2026-06-19.md](../dev/iterations/configuration-ui/permission-flow-2026-06-19.md) | Android 11+ QUERY_ALL_PACKAGES 运行时检测、授权条 UI、设置跳转与 onResume 重载 |
| [configuration-ui/pref-migrator-split-2026-06-20.md](../dev/iterations/configuration-ui/pref-migrator-split-2026-06-20.md) | 将单体 PrefMigrator.kt 拆分为 LegacyPrefSnapshotReader + LegacyPrefImporter + ManagedModelMigrator，实现 legacy prefs 读取、导入、受管模型迁移三阶段分离 |
| [configuration-ui/structure-cleanup-2026-06-19.md](../dev/iterations/configuration-ui/structure-cleanup-2026-06-19.md) | ViewBinding 迁移、去 ProgressDialog、删 pref_general、过滤空状态 |
| [crash-observability/ipc-design-qa-2026-06-20.md](../dev/iterations/crash-observability/ipc-design-qa-2026-06-20.md) | 会话中 IPC 稳定性、XSP、公开 FS、framework 注入等问答同步至架构文档 |

### 实施计划（`dev/plans/`）

| 文档 | 内容 |
|------|------|
| [architecture-decision-backlog.md](../dev/plans/architecture-decision-backlog.md) | 待决 ADR、实现与架构文档漂移；4B-β RelayMerge dedupe 已编码；IS 矩阵待验 |

### 进度追踪（`dev/progress/`）

| 文档 | 内容 |
|------|------|
| [status.md](../dev/progress/status.md) | ADR-024 分布式 cache 存储 as-built；Phase 5 监测/拦截 |

### 设备验收（`dev/verification/`）

| 文档 | 内容 |
|------|------|
| [README.md](../dev/verification/README.md) | CrashCenter 真机 adb 验收入口；含 ANR logcat L7（ADR-025） |
| [anr_logcat_l7_template.md](../dev/verification/anr_logcat_l7_template.md) | INTERCEPT 目标 app 主线程卡死后，logcat system/events 或导入文件可被 ANR_HINT 过滤命中 |
| [crash_log_smoke.md](../dev/verification/crash_log_smoke.md) | 崩溃日志全链路冒烟：安装 → 触发 → 检查 → 统计 → 导出 |
| [dark_mode_qa_20260619.md](../dev/verification/dark_mode_qa_20260619.md) | 461QYGDD2226C 上 light/dark × Config/Observe/Add sheet 矩阵通过；受管行与对话框已覆盖；崩溃行与 AOSP 模拟器未测 |
| [fs_consistency_template.md](../dev/verification/fs_consistency_template.md) | FS-5 ~ FS-7 真机验收：mkdirs 失败、并发读写、删改与 hook 写交错 |
| [is_matrix_template.md](../dev/verification/is_matrix_template.md) | IS-1~IS-6 + IS-R1~IS-R5 真机验收模板 |
| [phase_d_dark_qa_461QYGDD2226C.md](../dev/verification/phase_d_dark_qa_461QYGDD2226C.md) | 461QYGDD2226C 上 Config/Observe/Add sheet 明暗对比通过；行级与 permission banner 未覆盖 |
| [smoke_20260619.md](../dev/verification/smoke_20260619.md) | assembleDebug 与 adb 自动化 smoke 通过；LSPosed 手动项待补 |

---

## 按阅读路径

### 新人入门
1. [AGENTS.md](../AGENTS.md) — 项目全貌
2. [dev/DEV_GUIDE.md](../dev/DEV_GUIDE.md) — 开发速查
3. [architecture/overview.md](architecture/overview.md) — 系统总览
4. [design/INDEX.md](design/INDEX.md) — 视觉/交互设计 SSOT
5. [guides/usage.md](guides/usage.md) — 用户使用

### 若关注 Xposed hook 机制
1. [architecture/xposed-entry.md](architecture/xposed-entry.md)
2. [architecture/crash-handler.md](architecture/crash-handler.md)
3. [decisions/001-looper-loop-resurrection.md](decisions/001-looper-loop-resurrection.md)

### 若关注配置与 scope
1. [architecture/scope-and-prefs.md](architecture/scope-and-prefs.md)
2. [architecture/configuration-ui.md](architecture/configuration-ui.md)
3. [decisions/002-inverted-package-toggle.md](decisions/002-inverted-package-toggle.md)

---

*索引生成日期：2026-06-24*
