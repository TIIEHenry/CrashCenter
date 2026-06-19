---
title: "ADR-014: Legacy Prefs 一次性迁移策略"
type: decision
status: accepted
phase: 3
updated: 2026-06-19
summary: "PrefMigrator 首次启动从 tiiehenry.xp.grapcrash / grapcrash.xml 一次性导入配置到 crash.xml，标记 KEY_MIGRATED 后不再读旧路径"
---

# ADR-014: Legacy Prefs 一次性迁移策略

## 状态

**Accepted** — Phase 3 已实现（`PrefMigrator.kt`）。

## 背景

CrashCenter 源自 `tiiehenry.xp.grapcrash` 项目。包名迁移至 `nota.android.crash.xp.app` 后，prefs 文件名从 `grapcrash.xml` 改为 `crash.xml`。已有用户可能持有旧 prefs 文件，需保证一次性迁移、不重复读取。

## 决策

### 迁移流程（`PrefMigrator.migrateIfNeeded`）

```
1. 读 dest SharedPreferences("crash")
2. if dest.getBoolean("legacy_prefs_migrated", false) → return
3. 尝试读取 legacy snapshot：
   a. createPackageContext("tiiehenry.xp.grapcrash") → getSharedPreferences("grapcrash")
   b. 若失败：尝试 root 读 /data/data/tiiehenry.xp.grapcrash/shared_prefs/grapcrash.xml
   c. 若仍失败：标记 migrated 并 return
4. 解析 legacy keys：scope_mode, handle_system, show_system_ui, package_list
5. 写入 dest SharedPreferences
6. dest.edit().putBoolean("legacy_prefs_migrated", true).apply()
```

### 设计原则

| 原则 | 说明 |
|------|------|
| 幂等 | `KEY_MIGRATED` 标记防止重复执行 |
| 不依赖 root | `createPackageContext` 优先；root 仅可选后备 |
| 只读旧路径 | 不修改/删除旧包 prefs |
| 不阻塞启动 | 失败 silent（首次用户按默认值使用） |
| 迁移键白名单 | 仅导入已知 boolean 和 StringSet；不 blindcopy |

### 迁移键

| Key | 类型 | 说明 |
|-----|------|------|
| `scope_mode` | boolean | 作用域模式 |
| `handle_system` | boolean | 系统应用 |
| `show_system_ui` | boolean | UI 显示系统应用 |
| `package_list` | StringSet | 禁用列表 |

### 调用时机

`ActivityMain.onCreate` 中（首屏加载前）调用 `PrefMigrator.migrateIfNeeded(this)`。

## 后果

| 方面 | 影响 |
|------|------|
| 老用户升级 | 配置自动继承；无需重新勾选 |
| 新安装 | 无 legacy → 标记 migrated → 空操作 |
| 旧包卸载 | 迁移后旧包可安全卸载 |
| XSharedPreferences | hook 侧只读 `crash.xml`；旧路径不再使用 |

## 相关文档

- [scope-and-prefs.md](../architecture/scope-and-prefs.md) — prefs key 定义
- [configuration-ui.md](../architecture/configuration-ui.md) — 调用 PrefMigrator
- [ADR-003](003-xsharedpreferences-cross-process.md) — XSharedPreferences 路径
