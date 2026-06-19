---
title: "[Phase/功能] 设备验收报告"
type: verification
status: draft
phase: N/A
updated: YYYY-MM-DD
summary: "一句话描述本次验收范围与结论"
---

# [Phase/功能] 设备验收报告

> 日期：YYYY-MM-DD
> 设备：
> 框架：LSPosed / EdXposed / 经典 Xposed
> 脚本：`scripts/adb-smoke-verification.sh`（如有）

## 验收范围

- [ ] 模块安装与启动
- [ ] Xposed 模块已激活（`isModuleActived`）
- [ ] 目标 app hook 生效（logcat 见 `catch package:`）
- [ ] 测试崩溃被拦截（app 不退出）
- [ ] Toast / 通知正常
- [ ] Scope 配置生效

## 环境

| 项 | 值 |
|---|---|
| APK 版本 | |
| Android 版本 | |
| 设备型号 | |
| Xposed 框架 | |
| adb serial | |

## 执行步骤

### 1. 构建与安装

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/CrashCenter_v*_debug.apk
```

### 2. 激活模块

（LSPosed 管理器中启用 CrashCenter，勾选作用域）

### 3. 自动化 smoke（可选）

```bash
./scripts/adb-smoke-verification.sh [device_serial]
```

### 4. 手动崩溃测试

1. 打开 CrashCenter → 菜单 → Test
2. 等待 2 秒，确认 Toast 出现且 app **未退出**
3. 检查通知栏是否有异常通知

## 结果

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 模块激活 | PASS / FAIL | |
| hook 日志 | PASS / FAIL | |
| 崩溃拦截 | PASS / FAIL | |
| 通知 | PASS / FAIL | |

**总结**：PASS / FAIL

## 日志摘录

```
# adb logcat -s Xposed:XposedBridge:CrashCenter 相关行
```

## 相关文档

- Phase：[链接]
- 架构：[链接]
