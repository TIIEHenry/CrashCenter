---
title: "Framework 注入可行性评估"
type: architecture
status: accepted
phase: 4
updated: 2026-06-22
summary: "参照 celestailruler 评估 System Framework 注入对 CrashCenter 的价值；结论：不采用为主架构，保留 ADR-007 app 级 + Provider，可选 parseQueries 补丁"
---

# Framework 注入可行性评估

> 参照项目：[CelestailRuler](../reference/sibling-projects.md#celestailruler)（外部参考，非本仓库子模块）
> 触发背景：Phase 4 跨进程崩溃日志 IPC 分析后，评估是否应像 celestailruler 一样 hook **System Framework**（`system_server` / `android` 包）
> 关联：[crash-log-ipc.md](crash-log-ipc.md)、[ADR-007](../decisions/007-crash-log-cross-process-storage.md)、[xposed-entry.md](xposed-entry.md)

## 结论（Executive Summary）

**不采用 framework 注入作为 CrashCenter 主架构。** 干预层（`Looper.loop()` 续命）与观测层（CrashLogger）均须在**目标 app 进程**完成；framework 层无法替代。跨进程存储继续 [ADR-007](../decisions/007-crash-log-cross-process-storage.md) 的 app 级 JSONL Primary A + exported Provider Fallback B。

**唯一可选补丁**（非 MVP）：若 Phase 4B 真机矩阵显示 Primary A 因 **API 30+ 包可见性**（`createPackageContext` → `NameNotFoundException`）大面积失败——**而非 SELinux EACCES**——可局部借鉴 celestailruler 的 `AndroidHook.parseQueries`，向被解析 app 注入 `<queries>` 指向模块包。须额外勾选 LSPosed **System Framework** scope。

**不创建 ADR-008**：决策为「不转向」，现有 ADR-007 已覆盖存储路径。

---

## celestailruler 做法摘要

### 架构分层

| 模块 | 包名 / 路径 | 职责 |
|------|-------------|------|
| `:app` | `tiiehenry.celestialruler` | Xposed 入口、目标 app hook、管理 UI |
| `:server` | `tiiehenry.ruler.server` | **独立 APK**：规则存储 + IPC（Provider / Service） |
| `:api` | 共享 bridge | `RulerProviderClient`、`RulerServiceClient` |
| `:nota_xposed` | `XServiceManager`、`BaseXposedEntry` | framework 注入基础设施（**当前未接线**） |

入口：`celestailruler/app/src/main/assets/xposed_init` → `tiiehenry.celestialruler.inject.t4.a`

### Hook 目标（实际运行代码）

`handleLoadPackage` 按 `packageName` 分发：

| 进程 / 包 | Hook 内容 | 目的 |
|-----------|-----------|------|
| **`android`（System Framework）** | `ParsingPackageUtils.parseQueries` | 给**所有被解析的 app** 自动注入 `<queries>` 指向模块 + server APK |
| **模块进程** | `XposedType.isActivated` 恒 true | UI 检测 Xposed 激活 |
| **目标 app UI 进程** | Activity 生命周期、View/Touch、可选 `CrashHandlerHook` | 规则应用、崩溃拦截 |
| **system_server 自定义 Service** | `XServiceManager.initForSystemServer()` | **源码中无调用**；`RuleService` 注释写「注入 system_server」，实际跑在 `:server` APK |

**未 hook**：`ActivityManagerService.handleApplicationCrash`、`PackageManager.getInstalledPackages`（framework 层）、DropBox 等。

### LSPosed / xposed scope

- Manifest 推荐 scope 仅为 **`android`（System Framework）**，供 `AndroidHook` 使用。
- 目标 app hook 仍依赖用户在 LSPosed 中**逐 app 勾选**；与 CrashCenter 空 `xposed_scope` + 动态 prefs 思路不同。
- CrashCenter `XposedEntry` **显式排除** `android`；celestailruler **专门** hook `android`。

### IPC 模式（主路径，非 framework）

```
目标 app 进程 (hook)
    │ ContentResolver.call → tiiehenry.ruler.server.RuleProvider
    │ bindService → RulerServer (可选)
    ▼
tiiehenry.ruler.server APK（persistent，QUERY_ALL_PACKAGES）
    └── RuleService（进程内 Stub，非 system_server）
```

- **不是** framework 注入 IPC；是**第三方 persistent app + exported Provider**。
- 权限：`checkCallingUid()` ↔ 包名白名单，非 signature permission。
- `CRLayoutInflater` 在 app 进程 hook `PackageManager.getApplicationInfoAsUserUncached` 作 fallback，向 Provider 要模块 `ApplicationInfo`。

### 崩溃处理

`CrashHandlerHook` 与 CrashCenter `CrashHandler` **同构**：目标进程内 `Looper.loop()` 续命 + 替换 `UncaughtExceptionHandler`，**非** framework 层。

### SELinux / 版本兼容

- **XServiceManager**：Android 5+ SELinux 限制真·ServiceManager 注册；通过劫持 **clipboard Binder** 托管自定义服务；运行在 `system_server` → **崩溃风险极高**；**CrashCenter 不采用**。
- **AndroidHook**：API 34+ 与旧版 `ParsingPackageUtils` 类路径分支（`com.android.internal.pm...` vs `com.android.server.pm...`）。
- celestailruler：minSdk 24，compileSdk 36；CrashCenter：minSdk 21，compileSdk 36。

---

## CrashCenter 现状（对照基线）

| 维度 | 现状 |
|------|------|
| Hook | 仅 `IXposedHookLoadPackage`；`Application.onCreate` → `CrashHandler.insert` |
| Scope | 空 `xposed_scope`；`scope_mode` / 禁用列表 via [XSharedPreferences](../decisions/003-xsharedpreferences-cross-process.md) |
| 排除 | 显式跳过 `android`、Xposed 管理器（见 [xposed-entry.md](xposed-entry.md)） |
| 日志 IPC | ADR-007：Primary A（直写 module filesDir）+ Fallback B（exported Provider，无 signature permission） |
| 包可见性 UI | 模块侧 `QUERY_ALL_PACKAGES` + `PackageVisibilityHelper`（**仅模块进程**） |

核心约束（[crash-log-ipc.md](crash-log-ipc.md)）：hook 代码在**目标 UID** 运行，`Binder.getCallingUid()` 是目标 app → signature Provider **永远失败**；SELinux 常拒跨 UID 直写 `module_data_file`。

---

## Framework 注入可行性矩阵（5 用例）

| # | 用例 | LSPosed / 经典 Xposed | Android 10–16 / minSdk 21 | Scope 要求 | 对 CrashCenter 价值 | 风险 | 维护负担 | 结论 |
|---|------|----------------------|---------------------------|------------|-------------------|------|----------|------|
| **1** | **集中式崩溃采集**（hook AMS / `handleApplicationCrash`） | 技术上可 hook `system_server` | AMS 内部每 major 版本大变 | **必须勾选 System Framework** | **低**：被吞掉的 Java 异常**不会**走到 AMS；观测点仍在 app 内 `CrashHandler` 回调 | system 进程 hook 稳定性；与干预层重复 | **高** | ❌ 不推荐 |
| **2** | **包可见性 bypass**（framework `parseQueries` / PM） | celestailruler 已验证 `parseQueries` | 需按 API 分支类名/签名 | **System Framework** | **中**：可缓解 Primary A 的 `createPackageContext(module)` 失败（API 30+） | 篡改所有 app manifest 解析；Play / OEM 策略 | **中**（已有先例） | ⚠️ **可选补丁**，非 MVP |
| **3** | **跨进程写文件**（system_server 代写 module 存储） | XServiceManager 模式可行 | SELinux 对 system UID 宽松 | **System Framework** + 复杂初始化 | **中**：绕过目标 UID SELinux | **system_server 崩溃 → 软砖**；API 漂移 | **很高** | ❌ 不推荐（Provider 更简单） |
| **4** | **Broadcast 崩溃事件**经 framework | system 发广播可行 | 后台限制、force-stop | System Framework | **低**：不比 Provider 可靠；安全面更大 | 伪造 / DoS | 中 | ❌ 不推荐（[crash-log-ipc.md](crash-log-ipc.md) 已排除 C） |
| **5** | **Framework 级 UEH 替代 app 级** | 无法在 system 进程替目标 app 主线程 `Looper.loop()` | — | — | **零 / 负**：无法续命；进程级 handler 与 AMS 路径太晚 | 破坏核心产品定义 | — | ❌ **不可行** |

### 机制说明

**用例 1**：CrashCenter 在 UEH / `Looper.loop()` catch **之前**就吞掉异常，进程不杀 → `ActivityManagerService` 的 Java crash 路径通常**不触发**。Framework 采集只能补 Native crash、未 hook 进程、或干预失败的场景，与 Phase 4「记录每次**被拦截**崩溃」目标错位。

**用例 2**：celestailruler 的 `AndroidHook` 解决「目标 app 看不见模块包」——与 CrashCenter 日志 Primary A 的 API 30+ 风险对齐；**不能**替模块 UI 的 `QUERY_ALL_PACKAGES`（那是模块进程自己的列表）。

**用例 3**：celestailruler **已放弃**主路径：`initForSystemServer()` 无调用，`RuleService` 跑在独立 server APK。说明作者也认为 system_server 注入成本 > Provider。

**用例 5**：`Looper` 续命必须在**目标 app 主线程**执行；framework 无法在 `system_server` 代劳。

---

## App 级 hook vs Framework 注入

| 维度 | App 级（当前） | Framework 注入（celestailruler 风格） |
|------|----------------|--------------------------------------|
| **核心目标：吞 Java 崩溃** | ✅ 唯一正确位置 | ❌ 无法替代 |
| **崩溃日志** | ✅ 在 `CrashHandler` 回调异步写 | ⚠️ 仅能做补充通道；主数据仍在 app 内 |
| **跨进程存储** | Provider fallback（ADR-007） | system 代写可行但过重 |
| **包可见性** | 模块 UI：`QUERY_ALL_PACKAGES`；hook 侧 A 路径有风险 | `parseQueries` 可帮目标 app 看见模块 |
| **LSPosed 配置** | 只勾目标 app（+ 模块 self） | 额外勾 **System Framework** |
| **复杂度** | 单 APK | 多模块 / 多 APK / system hook |
| **维护** | AMS 无关 | 跟 AOSP 内部 API 强绑定 |
| **稳定性** | 目标进程 crash 仅影响该 app | system_server hook 失败影响整机 |

---

## 对 CrashCenter 各子系统的价值

| 子系统 | Framework 注入价值 |
|--------|-------------------|
| **干预层（续命）** | **无**；必须保持 app 级 |
| **观测层（CrashLogger）** | **低**；事件产生于 app 内 handler，app 级异步写 + Provider 即可 |
| **Primary A 兼容性** | **中等**；若真机矩阵大量 EACCES / `NameNotFoundException`，可考虑 **仅** `parseQueries` 补丁 |
| **UI 包列表** | **无**；继续模块侧 `QUERY_ALL_PACKAGES` |
| **安全** | system_server 方案扩大 blast radius；Provider 内 UID 校验已够用 |

celestailruler 的 IPC 启示：**独立 persistent 组件 + exported Provider** 是跨进程写入的务实解，而非 system_server。CrashCenter ADR-007 已对齐该思路（且更轻：无需 `:server` 第二 APK）。

---

## 推荐路径

### 总体：不采用 framework 注入作为主架构

| 阶段 | 建议 |
|------|------|
| **Phase 4 MVP** | 坚持 app 级 `CrashLogger` + JSONL Primary A + `CrashLogProvider` Fallback B（[crash-log-ipc.md](crash-log-ipc.md) / ADR-007） |
| **Phase 4B 真机矩阵后** | 若 Primary A 因**包可见性**（非 SELinux）大面积失败 → **可选**增加 celestailruler 式 `AndroidHook.parseQueries`，注入 `nota.android.crash.xp.app`；文档化 LSPosed 须额外启用 System Framework |
| **若 Primary A 因 SELinux EACCES** | 启用 Provider B，**不要**上 system_server 写文件 |
| **永不** | XServiceManager / AMS crash hook / framework UEH / Broadcast 写日志 |

### 为何 app 级 + Provider 足够

1. **崩溃事件源**在目标进程 `CrashHandler`——最近、最全、与「是否 showNotify」解耦。
2. **签名悖论**已由 ADR-007 修正：Provider 无 signature permission + 内部 UID 校验。
3. **AM 按需拉起** Provider 进程，不依赖模块 UI 曾运行（[crash-log-ipc.md § 独立启动](crash-log-ipc.md#目标进程独立启动时的权限与通信)）。
4. celestailruler 证明：复杂跨进程需求用 **Provider**，不是 system_server。
5. Framework hook 增加 **System Framework scope** 门槛，与 CrashCenter「装模块 → 勾 app → 防崩」的简单 UX 冲突。

### 若未来做「可选 framework 补丁」（非 MVP）

```
XposedEntry.handleLoadPackage
  case "android":
    if (prefs.crash_log_framework_queries_enabled)
      AndroidQueriesHook.install()  // 仅 parseQueries，~80 行 + 版本分支
  default:
    现有 Application.onCreate → CrashHandler（不变）
```

---

## LSPosed Scope 要求与风险

| 方案 | 用户须在 LSPosed 勾选 | 主要风险 |
|------|----------------------|----------|
| **当前 CrashCenter** | 模块 + 目标 app（或全局） | Provider 伪造写入（已有缓解）；SELinux 拒 A 路径 |
| **+ System Framework（parseQueries）** | 上述 + **System Framework / android** | framework 升级 break hook；全 app manifest 语义被改 |
| **+ XServiceManager** | System Framework | system_server 崩溃、SELinux、clipboard 劫持脆弱 |
| **+ AMS crash hook** | System Framework | 收不到已吞异常；AMS 内部 API 漂移 |

**LSPosed 注意**：勾选 **System Framework** 后模块代码加载进 `system_server`（高权限）；任何未捕获异常可能导致 **系统不稳定**。CrashCenter 哲学是「目标 app 坏了自己承担」，不应把同等风险搬进 system 进程。

术语见 [glossary.md](../glossary.md)（**Framework injection**、**System Framework scope**）。

---

## 外部参考：celestailruler 关键路径

| 主题 | 路径（celestailruler 仓库，外部） |
|------|----------------------------------|
| Xposed 入口分发 | `app/src/main/java/tiiehenry/celestialruler/inject/t4/a.java` |
| framework 包可见性 hook | `app/src/main/java/tiiehenry/celestialruler/inject/android/AndroidHook.java` |
| XServiceManager（未接线） | `nota_xposed/src/main/java/nota/xp/inject/XServiceManager.java` |
| server IPC | `server/src/main/java/tiiehenry/ruler/server/provider/RulerProvider.kt` |

CrashCenter 对应入口：[xposed-entry.md](xposed-entry.md)（`nota.android.crash.xp.app.XposedEntry`）。

---

## 相关文档

- [crash-log-ipc.md](crash-log-ipc.md) — 跨进程 IPC 主备链路与独立启动矩阵
- [ADR-007](../decisions/007-crash-log-cross-process-storage.md) — JSONL + Provider 存储决策
- [xposed-entry.md](xposed-entry.md) — app 级 hook 入口
- [crash-handler.md](crash-handler.md) — 干预层机制
- [crash-logging.md](crash-logging.md) — 观测层总方案
- [glossary.md](../glossary.md) — Framework injection、System Framework scope
