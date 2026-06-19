---
title: "术语表"
type: concept
status: accepted
phase: N/A
updated: 2026-06-19
summary: "CrashCenter 项目术语单一事实源（含受管应用与干预规则）"
---

# 术语表

> 本文档为术语 SSOT。其他文档引用术语时使用链接，不重复定义。

| 术语 | 定义 |
|------|------|
| **CrashCenter** | 本 Xposed 模块项目，应用 ID `nota.android.crash.xp.app`，显示名 CrashCenter / 崩溃中心 |
| **Scope Mode** | 作用域模式。开启时仅 hook 非禁用、非系统（除非允许系统应用）的目标包；关闭时 hook 全部包 |
| **Disabled Package** | `package_list` 中记录的包名（**Legacy**）。UI 上 Switch **关闭** = 加入禁用列表 = 不 hook；新模型见 [Managed App](#managed-app-受管应用) |
| **Managed App（受管应用）** | 用户通过「添加应用」主动加入配置列表的包；存于 `managed_packages`。在列表中 **不** 等于已 hook — 见 [app-management-ui.md](architecture/app-management-ui.md) |
| **Intervention Rule（干预规则）** | 单应用 hook 侧崩溃拦截配置（v1 类型 `CATCH_ALL`），存于 `intervention_rules` JSON。**无启用规则时不 hook**。与 Phase 4G **诊断规则（RuleEngine）** 不同 |
| **待配置** | 受管应用已入库但无 **enabled** 干预规则；Switch OFF；UI 角标；`shouldHook=false` |
| **CrashHandler** | 核心拦截器：通过无限 `Looper.loop()` 与替换 `UncaughtExceptionHandler` 吞掉崩溃 |
| **Observation Layer** | 观测层：记录每次被拦截的崩溃，不改变吞异常行为；与干预层（CrashHandler）分离 |
| **Analysis Layer** | 分析层：对 CrashEvent 做分类、聚类与诊断建议，**不修复**目标 app；见 [crash-intelligent-analysis.md](architecture/crash-intelligent-analysis.md) |
| **RuleEngine** | Phase 4G 组件：基于 exceptionClass / stack 规则输出 `exceptionType`、`rootCauseTags` 与模板建议 |
| **signatureHash** | 规范化 stack 指纹，用于重复崩溃聚类与 `clusterId` |
| **CrashLogger** | Phase 4 组件族：hook 侧 `CrashLogCoordinator` + 各 `CrashLogBackend`；模块侧 `CrashLogIngestCoordinator` |
| **CrashLogBackend** | 崩溃日志写入/合并后端抽象（root_su、provider_insert、target_relay 等）；见 [crash-log-backends.md](architecture/crash-log-backends.md) |
| **CrashCapturePipeline** | hook 侧单入口：构建 CrashEvent → 并行投递 CrashLogCoordinator 与 CrashFeedbackFacade；见 [architecture-optimization.md](architecture/architecture-optimization.md) |
| **CrashFeedbackFacade** | hook 侧反馈门面：Toast、Notification、PendingIntent；与日志路径失败域隔离 |
| **CrashLogCoordinator** | hook 侧写入协调器：Phase1 root 优先 → Phase2 多 IPC 并行 |
| **MainShellActivity** | Phase 4C+ UI 壳层：Toolbar + 状态条 + 2-tab BottomNav + NavHost；见 [ui-routing.md](architecture/ui-routing.md) |
| **ScopePolicy** | hook 侧纯函数/对象：根据 XSharedPreferences 与 LoadPackageParam 输出是否 hook 及 showNotify（替代 static showNotify） |
| **CrashLogIngestCoordinator** | 模块侧 ingest：root 读各 app 私有 relay → merge canonical JSONL |
| **RootFsBackend** | Phase 4 模块侧 libsu root 读 relay；模式见 [root-service-patterns.md](reference/root-service-patterns.md) |
| **crashcenter_relay** | 目标 app 私有目录 `files/crashcenter_relay/` 下单条 JSON；同 UID 写入兜底（原 `crashcenter_relay`） |
| **CrashEvent** | 单条崩溃记录（JSON）：时间戳、包名、异常类、stack trace、`source` 等，见 [crash-logging.md](architecture/crash-logging.md) |
| **CrashLogProvider** | Phase 4 Fallback：exported ContentProvider，`insert` 在模块 UID 下 append JSONL；**不得**使用 signature permission（hook 异签名调用） |
| **events.jsonl** | 模块私有目录 `files/crash_logs/` 下的 append-only 崩溃日志文件；Primary A 直写或 Provider 写入 |
| **QUERY_ALL_PACKAGES** | Android 11+ 正常权限，供**模块 UI 进程**枚举已安装包；侧载安装时通常自动授予；与 hook 侧包可见性无直接替代关系 |
| **XSharedPreferences** | Xposed 跨进程**读取**模块 SharedPreferences（UI 写 → hook `reload()`）；**不用于**崩溃事件体写入，见 [crash-log-ipc FAQ](architecture/crash-log-ipc.md#为何不用-xsharedpreferences-存崩溃日志) |
| **Self Hook** | 模块 hook 自身 `ActivityMain.isModuleActived()` 返回 true，用于检测 Xposed 激活状态 |
| **showNotify** | hook 侧静态标志，控制是否在崩溃时显示 Toast / Notification；见 [crash-notification.md](architecture/crash-notification.md) |
| **ADR** | Architecture Decision Record，记录在 `docs/decisions/` |
| **Phase** | 开发阶段任务清单，记录在 `dev/roadmap/active/` 或 `archive/` |
| **信息流 UI**（Content-first） | Clarence 生态 UI 模式：内容 full-bleed、按钮小、chrome 用悬浮层；见 [ui-modes.md](design/ui-modes.md) |
| **工具密度 UI**（Tool-dense） | Clarence 生态 UI 模式：按钮/窗口/面板多、固定壳层；Singular 为代表；见 [ui-modes.md](design/ui-modes.md) |
| **UI Hybrid** | 同屏分区组合两种 UI 模式（如列表=信息流 + Toolbar=工具密度）；见 [ui-modes.md §同页交叉组合](design/ui-modes.md#同页交叉组合hybrid) |
| **FloatingToolbar** | Clarence 悬浮底/顶 chrome 组件族：UnifiedCapsule、SplitCluster、IconNav、TopActions；见 [floating-chrome.md](design/components/floating-chrome.md) |
| **Icon Button Inset** | 仅 icon 按钮的对称内边距：glyph 在容器内视觉居中、不拉伸填满；token 族 `icon_glyph_size`、`icon_button_inset` 等；见 [visual-language §图标按钮居中](design/visual-language.md#图标按钮居中icon-button-inset) |
| **Enhanced Switch** | Clarence Switch 变体：OFF 时 thumb 为左对齐圆角短横条（胶囊），ON 时 morph 为右对齐正圆；prefs / attr `switch_style_enhanced`；见 [form-controls §Switch](design/components/form-controls.md#switch) |
| **Press-Drag-Release** | 按住锚点/菜单行滑动跟选、松手触发落点项、落点外关闭；见 [interaction-language §按住滑动选单](design/interaction-language.md#按住滑动选单-press-drag-release) |
| **Draggable Half Sheet** | Clarence TouchPrimary 半屏 BottomSheet：顶栏 chrome 内必填 **DragHandle**、默认 50% 高度、把手拖曳 expand/collapse/dismiss；见 [draggable-half-sheet.md](design/components/draggable-half-sheet.md) |
| **ScrollLinkedEdgeScrim** | 顶/底边缘 blur 或渐变；**仅**内容滚入对应区时显示，滚出消失；前景色随亮度自适应；见 [floating-chrome.md §ScrollLinkedEdgeScrim](design/components/floating-chrome.md#滚动联动边缘-scrimscrolllinkededgescrim) |
| **Framework injection** | 在 LSPosed 中勾选 **System Framework**（`android` 包），使模块 hook 代码加载进 `system_server` 进程；CrashCenter **不采用**为主架构，见 [framework-injection-feasibility.md](architecture/framework-injection-feasibility.md) |
| **System Framework scope** | LSPosed 作用域选项，对应 `handleLoadPackage` 中 `packageName == "android"`；启用后 hook 运行于 system 进程，稳定性风险高于 app 级 hook |
