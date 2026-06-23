---
title: "崩溃日志冒烟测试报告"
type: verification
status: draft
phase: 4B
created: 2026-06-23
summary: "崩溃日志全链路冒烟：安装 → 触发 → 检查 → 统计 → 导出"
---

# 崩溃日志冒烟测试报告

## 设备信息

| 项 | 值 |
|----|-----|
| 设备型号 | |
| Android 版本 | |
| API Level | |
| Root 方案 | |
| LSPosed 版本 | |
| CrashCenter 版本 | |
| 测试目标 app | |
| 验收日期 | |
| 验收人 | |

---

## 1. 安装与启动

- [ ] `./gradlew :app:assembleDebug` 编译成功
- [ ] `adb install -r` 安装成功
- [ ] LSPosed 中启用模块，scope 含目标 app
- [ ] 打开 CrashCenter，界面正常加载
- [ ] 底栏「配置」「观测」两个 tab 可切换
- [ ] logcat 无异常堆栈

**结果**: PASS / FAIL

备注:

---

## 2. 触发测试崩溃

- [ ] 目标 app 中触发崩溃（菜单 Test 或已知路径）
- [ ] 干预层续命：目标 app 进程未被 kill
- [ ] 通知栏出现崩溃通知
- [ ] 点击通知可打开 stack trace 详情

**结果**: PASS / FAIL

备注:

---

## 3. 检查 events.jsonl

- [ ] `adb shell run-as nota.android.crash.xp.app cat files/crash_logs/events.jsonl` 可读
- [ ] 新增行包含完整字段：`id`, `packageName`, `timestampMs`, `exceptionClass`, `stackTrace`
- [ ] `backendWritten` 字段非空，记录实际写入的后端
- [ ] JSON 格式合法（`python3 -c "import json; ..."` 校验）
- [ ] 无重复 `id` 行

**结果**: PASS / FAIL

events.jsonl 行数: _之前_ → _之后_

备注:

---

## 4. 历史列表验证

- [ ] 观测 tab → 历史子 tab 显示崩溃记录
- [ ] 记录按时间倒序排列
- [ ] 点击记录可打开详情 BottomSheet
- [ ] 详情包含完整 stack trace
- [ ] stack trace 文本可选中复制

**结果**: PASS / FAIL

备注:

---

## 5. 统计页验证

- [ ] 观测 tab → 统计子 tab 可进入
- [ ] 摘要卡片显示总崩溃数
- [ ] 应用 TOP 5 列表正确
- [ ] 异常 TOP 5 列表正确
- [ ] 点击应用 TOP 行可下钻进入 PerAppCrashActivity
- [ ] 按日计数列表与实际崩溃次数一致

**结果**: PASS / FAIL

备注:

---

## 6. 清空历史

- [ ] 观测 Toolbar「清空历史」按钮可用
- [ ] 点击后弹出确认对话框
- [ ] 确认后 `events.jsonl` 清空或删除
- [ ] 历史列表显示空状态
- [ ] 统计页归零

**结果**: PASS / FAIL

备注:

---

## 7. 导出验证

- [ ] 导出按钮可用
- [ ] 弹出隐私提示对话框
- [ ] 确认后 SAF 文件选择器打开
- [ ] 导出 zip 包含 `events.jsonl` + `metadata.json`
- [ ] zip 内文件内容正确（解压校验）
- [ ] metadata.json 包含导出时间、条数等元信息

**结果**: PASS / FAIL

导出文件大小: __ KB

备注:

---

## 8. logcat 分析（4F）

- [ ] 观测 tab → logcat 子 tab 可进入
- [ ] SAF 导入 logcat 文件功能正常
- [ ] 导入后片段列表可浏览
- [ ] 点击片段可查看详情

**结果**: PASS / FAIL / N/A（功能未就绪）

备注:

---

## 判定总结

| 步骤 | 结果 | 备注 |
|------|------|------|
| 1. 安装与启动 | PASS / FAIL | |
| 2. 触发崩溃 | PASS / FAIL | |
| 3. events.jsonl | PASS / FAIL | |
| 4. 历史列表 | PASS / FAIL | |
| 5. 统计页 | PASS / FAIL | |
| 6. 清空历史 | PASS / FAIL | |
| 7. 导出 | PASS / FAIL | |
| 8. logcat 分析 | PASS / FAIL / N/A | |

**验收结论**: PASS / FAIL / 有条件通过

**遗留问题**:

-
