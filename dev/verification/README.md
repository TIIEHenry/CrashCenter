---
title: "设备验收指南"
type: guide
status: accepted
phase: N/A
updated: 2026-06-24
summary: "CrashCenter 真机 adb 验收入口；含 ANR logcat L7（ADR-025）"
---

# 设备验收

CrashCenter 的验收以 **Xposed 真机环境** 为前提：模块激活、hook 生效、崩溃被拦截。自动化脚本覆盖安装与日志检查；崩溃拦截需配合 UI 手动测试。

## 统一入口

```bash
# 查看帮助
./scripts/adb-smoke-verification.sh --help

# 完整 smoke：编译 → 安装 → 启动 → 检查 logcat
./scripts/adb-smoke-verification.sh

# 指定设备
./scripts/adb-smoke-verification.sh -s 192.168.2.154:5555

# 跳过编译（已有 APK）
./scripts/adb-smoke-verification.sh --skip-build
```

## 报告命名

| 格式 | 示例 |
|------|------|
| `phaseNN_<topic>_YYYYMMDD.md` | `phase2_smoke_20260619.md` |
| `smoke_YYYYMMDD.md` | 通用 smoke 回归 |

模板：[docs/templates/verification-template.md](../../docs/templates/verification-template.md)

## 手动验收 checklist

| # | 步骤 | 期望 |
|---|------|------|
| 1 | LSPosed 启用模块，作用域含 CrashCenter | 模块界面显示已激活 |
| 2 | 打开 CrashCenter | 无 crash，应用列表加载 |
| 3 | 菜单 → Test | 2 秒后 Toast，**进程不退出** |
| 4 | 通知栏 | 出现异常通知，点击可打开 stack trace |
| 5 | **开启 Scope Mode**，关闭某 app 的 Switch，重启该 app | logcat 无 `catch package:` |
| 6 | Scope Mode **开启** | 仅 Switch **开启**的非系统 app 被 hook（「处理系统应用」开启时含系统 app） |
| 7 | Scope Mode **关闭**（默认） | 全部 app 被 hook；Switch 关闭仅影响 Toast/通知，**不**阻止 hook |

## Phase 4B-δ：分布式 cache 验收（ADR-024）

写路径**不依赖**模块进程；读路径**须 root**。矩阵见 [crash-log-distributed-storage.md](../../docs/architecture/crash-log-distributed-storage.md#独立启动与-is-矩阵分布式) 与 [phase4 §4B-δ](../roadmap/active/phase4_crash_observability.md#4b-δ--分布式-cache-存储-编码-as-built)。

| # | 要点 | 期望 |
|---|------|------|
| IS-D1 | root；目标 app 崩溃 → 打开历史 | `{pkg}/cache/crash_logs/events.jsonl` 有新行；UI 可见 |
| IS-D2 | **无** root | 历史/统计空态；不展示部分数据 |
| IS-D3 | 模块 force-stop → 目标 app 崩溃 | adb root `cat` cache JSONL 仍含 stack |
| IS-D4 | legacy canonical 升级 | `distributed_cache_migrated`；legacy 已清理 |
| IS-D5 | Toolbar 清空 → 再崩溃 | 他包 cache 已删；仅新事件 |

> 历史 IS-1~IS-6（Provider / canonical）已由 ADR-024 取代，见 [crash-log-ipc.md](../../docs/architecture/crash-log-ipc.md) 归档说明。

## logcat 关键字

完整 adb / logcat 崩溃分析需求见 [adb-logcat-analysis.md](../../docs/architecture/adb-logcat-analysis.md)。

```bash
adb logcat -s XposedBridge | grep -E "catch package|onCreate|XposedEntry"
```

| 日志 | 含义 |
|------|------|
| `catch package: <pkg>` | 该包已被 hook |
| `onCreate` | Application hook 已触发 |
| `selfCheck:pkg: nota.android.crash.xp.app` | 模块自身 hook 成功 |

## ANR 验收（L7）

模板：[anr_logcat_l7_template.md](anr_logcat_l7_template.md)（[ADR-025](../../docs/decisions/025-anr-observation-no-framework-hook.md)）。

```bash
adb shell su -c 'logcat -b system -d -v threadtime -t 500' | grep -iE 'ANR in|am_anr|not responding'
```

| 日志 | 含义 |
|------|------|
| `ANR in <pkg>` | 系统 ANR 文本头 |
| `am_anr` | events buffer 结构化事件（文本模式） |

## 单会话策略

同一设备 **避免并行** 多个 adb 安装/启动脚本。锁文件：`dev/verification/.adb-test.lock`（脚本内 `flock`）。

## 与文档系统的关系

| 写什么 | 放哪里 |
|--------|--------|
| 验收结论（PASS/FAIL、环境、截图） | `dev/verification/*.md` |
| 架构/行为变更 | `docs/architecture/` |
| phase 任务勾选 | `dev/roadmap/active/` |
| 会话摘要 | `dev/progress/status.md` |

## 相关文档

- [guides/build-and-install.md](../../docs/guides/build-and-install.md)
- [architecture/xposed-entry.md](../../docs/architecture/xposed-entry.md)
- [dev/DEV_GUIDE.md](../DEV_GUIDE.md)
