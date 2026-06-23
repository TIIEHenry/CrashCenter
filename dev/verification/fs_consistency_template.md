---
title: "文件系统一致性验收报告"
type: verification
status: draft
phase: 4B-gamma
created: 2026-06-23
summary: "FS-5 ~ FS-7 真机验收：mkdirs 失败、并发读写、删改与 hook 写交错"
---

# 文件系统一致性验收报告

## 设备信息

| 项 | 值 |
|----|-----|
| 设备型号 | |
| Android 版本 | |
| API Level | |
| Root 方案 | |
| LSPosed 版本 | |
| CrashCenter 版本 | |
| 验收日期 | |
| 验收人 | |

---

## FS-5: mkdirs 失败可观测

**目的**: 验证 `DirectFsBackend` 在目录创建失败时产生可观测日志和 `AppendResult.Failure`，而非静默吞掉错误。

**前置条件**

- [ ] 模块已激活，scope 含目标 app
- [ ] 准备模拟 mkdirs 失败的方法（以下任一）：
  - 磁盘空间不足（`adb shell dd if=/dev/zero of=/data/local/tmp/fill bs=1M count=...`）
  - 目录路径被 SELinux deny
  - 临时注释 mkdirs 调用（单元测试方式）

**操作**

1. 制造 mkdirs 失败条件
2. 触发测试崩溃
3. 检查 logcat 输出

**期望结果**

- [ ] logcat 包含 `XposedBridge.log` 一行错误日志（非 silent）
- [ ] `DirectFsBackend.append` 返回 `AppendResult.Failure`
- [ ] 其他后端（Provider / Relay）仍正常写入
- [ ] 干预层续命，进程不退出

**实际结果**

| 项 | 结果 |
|----|------|
| mkdirs 失败原因 | |
| logcat 错误日志 | 有 / 无 |
| AppendResult | Failure(reason:) |
| 其他后端兜底 | PASS / FAIL |
| 进程续命 | PASS / FAIL |

---

## FS-6: 连续崩溃后 deleteById 不损坏文件

**目的**: 验证 hook 连续写入 + UI 侧 `deleteById` 并发操作不会损坏 `events.jsonl`（文件锁串行化）。

**前置条件**

- [ ] 模块已激活，scope 含目标 app
- [ ] `events.jsonl` 已有少量历史数据

**操作**

1. 连续触发 **10 次**测试崩溃（每次间隔约 1 秒）
2. 在第 5~6 次崩溃之间，通过 UI 删除其中 **1 条**记录
3. 检查 `events.jsonl` 文件完整性

**期望结果**

- [ ] `events.jsonl` 为有效 JSONL（每行均为合法 JSON）
- [ ] 无损坏行（无截断、无乱码、无半行）
- [ ] 删除的记录确实不在文件中
- [ ] 条数 = _之前行数_ + 10 次新增 - 1 次删除 = _预期行数_
- [ ] FileLock 未发生死锁（操作全部完成）

**实际结果**

| 项 | 结果 |
|----|------|
| 之前行数 | |
| 触发崩溃次数 | |
| 删除次数 | |
| 之后行数 | |
| JSONL 校验 | 全部合法 / 有 N 行非法 |
| 非法行内容 | |
| 死锁 / 超时 | 无 / 有: |

---

## FS-7: clear 后 hook 再崩溃

**目的**: 验证模块 `clear()` 清空文件后，hook 侧新崩溃仍可正常写入，文件状态自洽。

**前置条件**

- [ ] 模块已激活
- [ ] `events.jsonl` 中有若干记录

**操作**

1. 通过 UI 执行「清空历史」
2. 确认 `events.jsonl` 已清空（行数为 0 或文件已删除）
3. 触发 **1 次**测试崩溃
4. 打开观测 tab 检查历史列表

**期望结果**

- [ ] `events.jsonl` 仅含 1 条新行
- [ ] JSON 格式合法
- [ ] 历史列表 Observe 显示 1 条记录
- [ ] 统计页显示 1 条
- [ ] 无残留旧数据

**实际结果**

| 项 | 结果 |
|----|------|
| clear 后行数 | |
| 崩溃后行数 | |
| JSON 校验 | PASS / FAIL |
| UI 显示条数 | |
| 统计页显示 | |
| 残留旧数据 | 无 / 有: |

---

## 附加: FileLock 并发压力（可选）

**前置条件**

- [ ] 模块已激活

**操作**

1. 快速连续触发 20 次崩溃（间隔 < 500ms）
2. 同时通过 UI 反复刷新历史列表
3. 同时执行 1~2 次 deleteById

**期望结果**

- [ ] 无 ANR
- [ ] 文件无损坏
- [ ] UI 列表最终与文件内容一致

**实际结果**

| 项 | 结果 |
|----|------|
| ANR | 无 / 有 |
| 文件损坏 | 无 / 有 |
| UI 一致性 | PASS / FAIL |

---

## 判定总结

| 用例 | 结果 | 备注 |
|------|------|------|
| FS-5 mkdirs 失败可观测 | PASS / FAIL / N/A | |
| FS-6 并发删改不损坏 | PASS / FAIL | |
| FS-7 clear 后再写入 | PASS / FAIL | |
| 附加: 压力并发 | PASS / FAIL / SKIP | |

**验收结论**: PASS / FAIL / 有条件通过

**遗留问题**:

-
