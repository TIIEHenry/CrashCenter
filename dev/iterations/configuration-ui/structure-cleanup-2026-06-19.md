---
title: "配置 UI 结构清理"
type: iteration
status: completed
phase: 3
updated: 2026-06-19
summary: "ViewBinding 迁移、去 ProgressDialog、删 pref_general、过滤空状态"
---

# 配置 UI 结构清理（Phase 3D）

## 变更

| 项 | 前 | 后 |
|---|---|---|
| 视图绑定 | `findViewById` | ViewBinding（`ActivityMainBinding` / `ActivityCrashinfoBinding`） |
| 加载 UX | `ProgressDialog` | 列表区 `CircularProgressIndicator` + 文案 |
| 异步加载 | `Thread` + `runOnUiThread`（隐式 Activity 引用） | `applicationContext` + `Handler` + `isFinishing` 检查 |
| 遗留资源 | `pref_general.xml` | 已删除（未接入 UI） |
| 过滤空状态 | 空白列表 | `emptyState` 文案提示 |

## 行为保持

- per-app toggle、`scope_mode`、`handle_system`、`show_system_ui`
- 搜索 / Chip 过滤 / Toolbar 排序与批量操作
- Xposed 状态条与未激活对话框

## 验证

- `./gradlew :app:assembleDebug`
- `./scripts/adb-smoke-verification.sh --skip-build`（adb 在线时）

## 相关文档

- [phase3_ui_redesign.md](../../roadmap/active/phase3_ui_redesign.md)
- [configuration-ui.md](../../../docs/architecture/configuration-ui.md)
