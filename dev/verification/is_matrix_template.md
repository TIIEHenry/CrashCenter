---
title: "独立启动矩阵验收报告"
type: verification
status: draft
phase: 4B
created: 2026-06-23
summary: "IS-1~IS-6 + IS-R1~IS-R5 真机验收模板"
---

# 独立启动矩阵验收报告

## 设备信息

| 项 | 值 |
|----|-----|
| 设备型号 | |
| Android 版本 | |
| API Level | |
| ROM / 定制 | |
| Root 方案 | Magisk / KernelSU / 其他: |
| LSPosed 版本 | |
| CrashCenter 版本 | |
| 验收日期 | |
| 验收人 | |

---

## IS-1: force-stop 模块后目标 app 冷启动

**前置条件**

- [ ] 模块已在 LSPosed 中激活，scope 含目标 app
- [ ] 目标 app 已安装且可正常启动
- [ ] 执行 `adb shell am force-stop nota.android.crash.xp.app` 确认模块进程已终止

**操作**

1. Force-stop 模块进程
2. 冷启动目标 app
3. 触发测试崩溃（菜单 Test 或已知崩溃路径）

**期望结果**

- [ ] `events.jsonl` 新增一行，或 Provider 路径写入成功
- [ ] 干预层续命（进程不被 kill）

**实际结果**

| 项 | 结果 |
|----|------|
| 写入路径 | Provider / DirectFs / Relay: |
| events.jsonl 变化 | 行数: _之前_ → _之后_ |
| 进程续命 | PASS / FAIL |
| logcat 关键日志 | |

---

## IS-2: 安装后从未打开 CrashCenter UI

**前置条件**

- [ ] 卸载并重新安装 CrashCenter
- [ ] LSPosed 中重新激活模块
- [ ] **不启动** CrashCenter 管理器 UI

**操作**

1. 仅启动目标 app
2. 触发测试崩溃

**期望结果**

- [ ] `XSharedPreferences` 默认 scope 仍触发 hook
- [ ] `events.jsonl` 新增一行或 Provider 路径成功

**实际结果**

| 项 | 结果 |
|----|------|
| hook 是否生效 | PASS / FAIL |
| 写入路径 | Provider / DirectFs / Relay: |
| events.jsonl 变化 | 行数: _之前_ → _之后_ |

---

## IS-3: API 30+ 目标 app 无 QUERY_ALL_PACKAGES

**前置条件**

- [ ] 目标 app targetSdkVersion >= 30
- [ ] CrashCenter 未声明 `QUERY_ALL_PACKAGES`

**操作**

1. 观察 Primary A（DirectFs `createPackageContext`）行为
2. 检查 logcat 是否有 `NameNotFoundException` 或 `EACCES`

**期望结果**

- [ ] 记录实际行为：成功 / `NameNotFoundException` / EACCES / 其他

**实际结果**

| 项 | 结果 |
|----|------|
| Primary A 结果 | 成功 / 失败: |
| 错误信息 | |
| Fallback 路径是否生效 | PASS / FAIL |

---

## IS-4: 模块 force-stop 后 ContentResolver.insert 拉起模块

**前置条件**

- [ ] 模块已 force-stop
- [ ] ProviderBackend 启用（`crash_log_backend_provider = true`）

**操作**

1. Force-stop 模块
2. 目标 app 触发崩溃，hook 侧调用 `ContentResolver.insert`

**期望结果**

- [ ] 模块进程被 ActivityManager 拉起
- [ ] JSONL 写入成功

**实际结果**

| 项 | 结果 |
|----|------|
| 模块进程是否被拉起 | PASS / FAIL |
| JSONL 写入 | PASS / FAIL |
| logcat 拉起日志 | |

---

## IS-5: Provider 误配 signature permission（负例）

**前置条件**

- [ ] 临时为 Provider 添加 `android:permission="...signature"`
- [ ] 重新编译安装

**操作**

1. 触发测试崩溃
2. hook 侧 `ContentResolver.insert`

**期望结果**

- [ ] 抛出 `SecurityException`（验证设计禁止此配置）

**实际结果**

| 项 | 结果 |
|----|------|
| 是否抛 SecurityException | PASS / FAIL |
| 异常信息 | |
| 其他后端是否兜底 | PASS / FAIL |

---

## IS-6: 模块运行中 vs force-stop 延迟对比

**前置条件**

- [ ] 准备计时工具（logcat timestamp 或 `System.currentTimeMillis`）

**操作**

1. **A 组**：模块运行中，触发崩溃 3 次，记录每次写入延迟
2. **B 组**：force-stop 模块，触发崩溃 3 次，记录每次写入延迟

**期望结果**

- [ ] 两组均成功写入
- [ ] 记录 ROM 差异（模块被拉起的额外开销）

**实际结果**

| 次数 | A 组延迟 (ms) | B 组延迟 (ms) |
|------|--------------|--------------|
| 1 | | |
| 2 | | |
| 3 | | |
| 平均 | | |

---

## IS-R1: root + 非 DenyList 目标

**前置条件**

- [ ] 设备已 root
- [ ] 目标 app 未在 Magisk DenyList 中
- [ ] `crash_log_backend_root_su = true`

**操作**

1. 触发测试崩溃

**期望结果**

- [ ] Phase 1 `RootSuBackend` 写入 canonical JSONL

**实际结果**

| 项 | 结果 |
|----|------|
| RootSu 写入 | PASS / FAIL |
| backendWritten 字段 | |

---

## IS-R2: DenyList 目标 app

**前置条件**

- [ ] 将目标 app 加入 Magisk DenyList
- [ ] 重启目标 app

**操作**

1. 触发测试崩溃

**期望结果**

- [ ] Phase 1 root_su 失败
- [ ] Phase 2 Provider / Relay 兜底成功

**实际结果**

| 项 | 结果 |
|----|------|
| root_su 失败原因 | |
| 兜底路径 | Provider / DirectFs / Relay |
| 最终写入 | PASS / FAIL |

---

## IS-R3: 仅 relay 成功后 ingest 可见

**前置条件**

- [ ] 禁用 ProviderBackend 和 DirectFsBackend（仅保留 relay）
- [ ] 模块未运行

**操作**

1. 触发测试崩溃（仅 relay 写入）
2. 打开 CrashCenter 管理器（触发 ingest）

**期望结果**

- [ ] RelayMergeBackend harvest relay → canonical
- [ ] UI 历史列表可见该事件

**实际结果**

| 项 | 结果 |
|----|------|
| relay 文件是否生成 | PASS / FAIL |
| ingest 后 canonical | PASS / FAIL |
| UI 可见 | PASS / FAIL |

---

## IS-R4: 全 hook 后端失败

**前置条件**

- [ ] 禁用所有 hook 侧后端（Provider / DirectFs / Relay / root_su 全关）

**操作**

1. 触发测试崩溃

**期望结果**

- [ ] 写入全部失败，但 silent（不 crash、不 ANR）
- [ ] 干预层续命

**实际结果**

| 项 | 结果 |
|----|------|
| 写入结果 | 全失败（符合预期） |
| 进程续命 | PASS / FAIL |
| logcat 异常 | 无 / 有: |

---

## IS-R5: 模块无 root

**前置条件**

- [ ] 设备无 root 或 root 授权已撤销

**操作**

1. 触发测试崩溃
2. 打开管理器查看历史

**期望结果**

- [ ] canonical / Provider 路径仍可用
- [ ] ingest 跳过（无 root 不扫描 relay）

**实际结果**

| 项 | 结果 |
|----|------|
| 写入路径 | Provider / DirectFs: |
| UI 可见 | PASS / FAIL |
| ingest 行为 | 跳过（符合预期） |

---

## 判定总结

| 用例 | 结果 | 备注 |
|------|------|------|
| IS-1 | PASS / FAIL / N/A | |
| IS-2 | PASS / FAIL / N/A | |
| IS-3 | PASS / FAIL / N/A | |
| IS-4 | PASS / FAIL / N/A | |
| IS-5 | PASS / FAIL / N/A | |
| IS-6 | PASS / FAIL / N/A | |
| IS-R1 | PASS / FAIL / N/A | |
| IS-R2 | PASS / FAIL / N/A | |
| IS-R3 | PASS / FAIL / N/A | |
| IS-R4 | PASS / FAIL / N/A | |
| IS-R5 | PASS / FAIL / N/A | |

**验收结论**: PASS / FAIL / 有条件通过

**遗留问题**:

-
