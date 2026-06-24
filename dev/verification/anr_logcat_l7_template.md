---
title: "ANR logcat 验收 L7（ADR-025 / 4F-ANR）"
type: verification
status: draft
phase: 4
updated: 2026-06-24
summary: "INTERCEPT 目标 app 主线程卡死后，logcat system/events 或导入文件可被 ANR_HINT 过滤命中"
---

# ANR logcat 验收 L7

对应 [adb-logcat-analysis.md](../../docs/architecture/adb-logcat-analysis.md) 验收 **L7** 与 [phase4 § 4F-ANR](../roadmap/active/phase4_crash_observability.md#4f-anranr-观测adr-025)。

## 设备信息

| 项 | 值 |
|----|-----|
| 设备型号 | |
| Android 版本 / API | |
| Root | 是 / 否 |
| LSPosed + CrashCenter 版本 | |
| 目标 app（已拦截） | |
| 验收日期 | |

---

## 前置

- [ ] 目标 app 在 CrashCenter **已拦截**（Switch ON）
- [ ] 模块观测 → **logcat** 子页可用
- [ ] Root 机：Toolbar「从 root 读取」可用；无 root 时准备含 ANR 的 `.txt` 导入

---

## 步骤

### A. 触发 ANR（INTERCEPT 卡死）

在目标 app 内制造主线程阻塞 **>5s** 且**不抛异常**（例如调试菜单 `Thread.sleep` on main），或复现已知 ANR。

- [ ] 系统出现 ANR 对话框或进程被系统回收（视 ROM 而定）

### B. 采集 logcat

**Root 路径**（优先）：

```bash
adb shell su -c 'logcat -b system -d -v threadtime -t 500' | grep -iE 'ANR|not responding|am_anr'
```

应用内：观测 → logcat → 缓冲区 **system**（或 **events**）→「从 root 读取」。

**无 root**：PC 执行上式 adb 保存为 `anr_system.txt`，应用内 SAF 导入。

- [ ] 原始 log 中含 `ANR in` / `am_anr` / `ActivityManager` + `not responding` 之一

### C. 应用内过滤

- [ ] 开启「仅崩溃相关」过滤（`filterCrashRelated`）
- [ ] 列表中可见至少 **1** 条 ANR 相关行（与 B 中关键字一致）
- [ ] `Process ... has died` **单独**出现时不应仅凭此行判定为 ANR（可有，但须有 ANR 关键字行）

---

## 结果

| 项 | PASS / FAIL | 备注 |
|----|-------------|------|
| L7 整体 | | |
| Root system buffer | | |
| events / am_anr（可选） | | |
| SAF 导入（无 root 时） | | |

---

## 相关文档

- [ADR-025](../../docs/decisions/025-anr-observation-no-framework-hook.md)
- [anr-observation.md](../../docs/architecture/anr-observation.md)
- [dev/verification/README.md](README.md)
