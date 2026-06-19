---
title: "Phase D dark mode 设备 QA（部分）"
type: verification
status: draft
phase: 3
updated: 2026-06-19
summary: "461QYGDD2226C 上 Config/Observe/Add sheet 明暗对比通过；行级与 permission banner 未覆盖"
---

# Phase D dark mode 设备 QA（部分）

- **设备**：461QYGDD2226C（MEIZU 21）
- **APK**：`CrashCenter_v0.1.0_debug.apk`（`adb install -r`）
- **系统**：`cmd uimode night no/yes`，结束后恢复 `night no`
- **产物**：`dev/verification/phase_d_dark_qa_461QYGDD2226C/`（截图 + uiautomator xml，未提交）

## 矩阵（v2，冷启动 + 4s settle）

| 项 | Light | Dark |
|---|:---:|:---:|
| Config tab | PASS | PASS |
| Observe tab | PASS | PASS |
| Add sheet 打开 | PASS | PASS |
| Canvas / surface（内容区 mean luma） | ~248 | ~33–61 |
| Status banner（未激活） | PASS | PASS |
| Chips（作用域/系统/筛选） | PASS | PASS |
| Search 字段 | PASS | PASS |
| Bottom nav | PASS | PASS |
| Empty states | PASS | PASS |
| 对比度可读性（行/列表） | — | **未测** |

## uiautomator 要点

- Config/Observe：状态条「未激活 · 请在 Xposed 中启用并重启」；筛选 chip「全部/已启用/待配置」；搜索 hint「搜索应用名或包名」；空态文案可见。
- Add sheet：标题「添加应用」、搜索、应用列表、「完成」。
- Dark 状态条区域采样 avg RGB ≈ (63,55,0) / 标题区 ≈ (73,64,0)（琥珀警告样式，未做 WCAG 计量）。

## 问题 / 风险

1. **uimode 后需冷启动并等待**：不足等待时截图仍为浅底（非产品回归，自动化易误判）。
2. **覆盖缺口**：无受管应用行、无崩溃列表行、未触发 package visibility permission banner。
3. **构建**：增量 `:app:assembleDebug` OK；`--rerun-tasks` 曾报 `ConfigFragment` `AlertDialog` 未解析（清缓存全量编译风险）。

## 相关文档

- [configuration-ui.md](../../docs/architecture/configuration-ui.md)
- [status.md](../progress/status.md)
