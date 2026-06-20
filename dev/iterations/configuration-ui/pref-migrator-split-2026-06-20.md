---
title: "ADR-014 PrefMigrator 拆分与受管模型迁移实现"
type: progress
status: completed
phase: 3
updated: 2026-06-20
summary: "将单体 PrefMigrator.kt 拆分为 LegacyPrefSnapshotReader + LegacyPrefImporter + ManagedModelMigrator，实现 legacy prefs 读取、导入、受管模型迁移三阶段分离"
---

# ADR-014 PrefMigrator 拆分与受管模型迁移实现

## 变更概述

将 `PrefMigrator.kt` 拆分为三个职责单一的对象，实现 legacy prefs 读取、导入、受管模型迁移三阶段分离。

## 文件变更

### 新增

| 文件 | 职责 |
|------|------|
| `app/src/main/java/nota/android/crash/xp/migration/LegacyPrefSnapshotReader.kt` | 读取 legacy `grapcrash.xml` snapshot（package context 优先，root 后备） |
| `app/src/main/java/nota/android/crash/xp/migration/LegacyPrefImporter.kt` | 将 snapshot 导入当前 `crash.xml`（boolean / StringSet 白名单写入） |
| `app/src/main/java/nota/android/crash/xp/migration/ManagedModelMigrator.kt` | ADR-002 `package_list` → ADR-015 受管模型迁移（`managed_packages` + `intervention_rules` JSON） |

### 相关变更

- `PrefMigrator.kt` — 保留入口协调，委托三个新对象
- `LegacyAppRepository` — 提供已安装包枚举、系统包判定、系统过滤逻辑

## 测试

| 文件 | 覆盖 |
|------|------|
| `app/src/test/java/nota/android/crash/xp/migration/LegacyPrefSnapshotReaderTest.kt` | package context 读取、root XML 解析、空 snapshot、异常降级 |
| `app/src/test/java/nota/android/crash/xp/migration/LegacyPrefImporterTest.kt` | 导入成功/空 snapshot/幂等写入 |
| `app/src/test/java/nota/android/crash/xp/migration/ManagedModelMigratorTest.kt` | 已迁移跳过、空激活、legacy 升级迁移、scope_mode 过滤、系统包过滤 |
| `app/src/test/java/nota/android/crash/xp/app/config/LegacyAppRepositoryTest.kt` | 包枚举、系统包判定、过滤规则 |
| `app/src/test/java/nota/android/crash/xp/PrefMigratorTest.kt` | 端到端迁移流程 |

## As-Built 行为

1. **LegacyPrefSnapshotReader.read(context)**
   - 尝试 `createPackageContext("tiiehenry.xp.grapcrash")` 读 `grapcrash` prefs
   - 失败则 `su -c cat /data/data/.../grapcrash.xml` 并 XML pull-parse
   - 返回 `Snapshot(booleans, stringSets)` 或 `null`

2. **LegacyPrefImporter.import(context, snapshot)**
   - snapshot 无数据 → 返回 `false`
   - 有数据 → 写入 `crash.xml` boolean / StringSet 白名单 → 返回 `true`

3. **ManagedModelMigrator.migrateIfNeeded(context, legacyPrefState)**
   - 已标记 `PREF_MANAGED_MODEL_MIGRATED` → 跳过
   - 已存在 `PREF_MANAGED_PACKAGES` → 标记 migrated 并跳过
   - 无 legacy 数据且无 prior session → `activateEmpty()`（空受管列表 + 空规则）
   - 有 legacy 数据或 app upgrade → `migrateFromLegacy()`：
     - 取 `scope_mode` + `handle_system` + `package_list`
     - 枚举已安装包，排除 disabled，按 scope/system 过滤
     - 每个受管包生成默认 `CATCH_ALL` 规则
     - 写入 `managed_packages` + `intervention_rules` JSON

## 设计原则

| 原则 | 说明 |
|------|------|
| 幂等 | `PREF_MANAGED_MODEL_MIGRATED` 哨兵防止重复执行 |
| 不依赖 root | package context 优先；root 仅可选后备 |
| 只读旧路径 | 不修改/删除旧包 prefs |
| 不阻塞启动 | 失败 silent（首次用户按默认值使用） |
| 迁移键白名单 | 仅导入已知 boolean 和 StringSet；不 blind copy |

## 相关文档

- [ADR-014](../../../docs/decisions/014-legacy-prefs-migration.md) — 原始决策
- [ADR-015](../../../docs/decisions/015-managed-apps-intervention-rules.md) — 受管模型决策
- [scope-and-prefs.md](../../../docs/architecture/scope-and-prefs.md) — prefs key 定义
