---
title: "Phase D 暗色模式设备 QA"
type: verification
status: draft
phase: 3
updated: 2026-06-19
summary: "461QYGDD2226C 上 light/dark × Config/Observe/Add sheet 矩阵通过；受管行与对话框已覆盖；崩溃行与 AOSP 模拟器未测"
---

# Phase D 暗色模式设备 QA

- **设备**：461QYGDD2226C（MEIZU 21 / Flyme）
- **构建**：`:app:assembleDebug` → `CrashCenter_v0.1.0_debug.apk`（`adb install -r`）
- **切换**：`adb shell cmd uimode night no|yes` + 冷启动 `MainShellActivity` + ~4s settle
- **产物**：`dev/verification/dark_mode_qa_20260619_artifacts/`（PNG + uiautomator XML + `matrix_v3.json`）
- **数据准备**：`run-as` 写入受管 `com.android.settings` + 启用 CATCH_ALL，用于 **ManagedAppRow** 暗色验收

## 矩阵（Shell 主路径）

| 检查项 | Light | Dark | 备注 |
|--------|:-----:|:----:|------|
| Config 页 / canvas | PASS | PASS | 内容区 luma ≈ 250 / 32 |
| Status banner（未激活） | PASS | PASS | 琥珀底 + 警告字色成对 |
| Permission banner | N/A | N/A | 本机当前可见应用足够，未出现条；中间轮次曾见「仅 1 个可见」条（见 artifacts 早期 dump） |
| 设置 Chip（作用域/系统/筛选） | PASS | PASS | 选中/未选中对比可读 |
| Search 字段 | PASS | PASS | hint + 图标 tint 正常 |
| Bottom nav | PASS | PASS | 选中态 accent 可见 |
| 受管列表行（Settings） | PASS | PASS | 角标「已启用」+ Switch 暗色底 |
| Observe 空态 | PASS | PASS | 「暂无崩溃记录…」 |
| Add sheet | PASS | PASS | 深/浅 surface + 搜索 + 「完成」 |
| Material 对话框 | PASS | PASS | 作用域长按说明 + 状态条 Xposed 引导 |
| 崩溃历史行 | — | — | 无 4B 写入数据 |
| logcat FATAL | PASS | PASS | 矩阵期间无 FATAL |

## 对话框

- **作用域说明**（`chipScopeMode` 长按）：`MaterialAlertDialogBuilder` 标题/正文在 dark 下可读（`dark_scope_dialog.xml`）；无显式按钮，靠返回键关闭。
- **Xposed 未激活**（点 status banner）：light/dark 均有按钮区（`light_xposed_dialog.xml` / `dark_xposed_dialog.xml`）。

## 问题 / 缺口

1. **未覆盖 AOSP 模拟器 API 30/34/36**（Phase D 项仍 open）。
2. **Permission banner compact 路径**：依赖包可见性受限；本机未稳定复现。
3. **CrashEventRow / CrashInfo / Edit 页**：无崩溃样本，未纳入本轮矩阵。
4. **自动化注意**：`uimode` 切换后必须 force-stop + 冷启动；否则易误判 canvas。
5. **Gradle**：增量 `assembleDebug` OK；`--rerun-tasks` 曾触发 daemon 异常（与 UI 无关）。

## 相关文档

- [dark-mode-theming.md](../../docs/architecture/dark-mode-theming.md)
- [configuration-ui.md](../../docs/architecture/configuration-ui.md)
- [status.md](../progress/status.md)
- [phase_d_dark_qa_461QYGDD2226C.md](phase_d_dark_qa_461QYGDD2226C.md)（前序部分矩阵）
