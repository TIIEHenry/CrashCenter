---
title: "ADR-015: 受管应用与干预规则"
type: decision
status: accepted
phase: 3
updated: 2026-06-19
summary: "配置域改为 managed_packages 策展列表 + intervention_rules JSON；hook 门控已由 ADR-023 修订"
---

# ADR-015: 受管应用与干预规则

> **部分 superseded**：[ADR-023](023-injection-observe-intercept-split.md) 修订 §2–§4 hook 门控：`managed_packages` 改为策展配置集；无 enabled 规则时 **仍注入观测**（`shouldIntercept=false`）。

## 状态

**Accepted**（hook 门控条款由 ADR-023 取代）— 方案见 [app-management-ui.md](../architecture/app-management-ui.md)。

## 背景

当前配置 UI（ADR-002）展示 **全部已安装应用**，用行内 Switch 维护 `package_list` **禁用列表**；默认 hook 全部 app。用户反馈与产品演进需要：

1. **显式添加/移除** 受关注应用，而非在冗长全量列表中查找  
2. **添加时不要求配置规则** — 先入库，后在编辑页手动添加  
3. **规则驱动 hook** — 「在列表中」不足以 hook，须至少一条启用的干预规则  

该模型与 ADR-002 默认哲学、ADR-005「一屏全量列表」假设均存在冲突，需架构级决策。

## 决策

### 1. 存储：新增键，不翻转 `package_list`

| Key | 职责 |
|-----|------|
| `managed_packages: Set<String>?` | **`null` = Legacy**；非 null = 受管成员 SSOT |
| `intervention_rules: String` | JSON map，`pkg → AppInterventionProfile` |
| `managed_model_migrated: boolean` | 一次性迁移标记 |
| `package_list` | **Legacy only**；新 UI 不写入；下一大版本重命名为 `disabled_packages` 或删除 |

### 2. 干预规则 v1

- 类型枚举：**仅 `CATCH_ALL`**（拦截未捕获 Java 异常）  
- Profile 结构：`{ rules: Rule[], updatedAt }`  
- **`rules` 为空或全无 `enabled`** → `shouldHook=false`  
- 字段 `showNotify` / `crashLogEnabled`：**`null` = 继承全局**

### 3. 「无规则可添加」与行内 Switch

| 操作 | 存储 | Hook |
|------|------|------|
| 添加受管应用（Picker） | `managed_packages += pkg`；**不写** rules | `shouldHook=false`；Switch **OFF** |
| **行内 Switch OFF → ON** | 无规则 → append `CATCH_ALL`；有规则 → 全部或首条 `enabled=true` | 经 ScopePolicy |
| **行内 Switch ON → OFF** | 全部规则 `enabled=false`（**保留** rules 数据） | `shouldHook=false` |
| 编辑页添加首条规则 | `intervention_rules[pkg].rules += CATCH_ALL` | 经 ScopePolicy |
| 编辑页删除全部规则 | rules 空 | `shouldHook=false`；Switch OFF |
| 移除受管应用 | 删 pkg + profile | `shouldHook=false` |

**Picker 添加时禁止**自动创建规则；**Switch ON** 与 **编辑页添加规则** 均可作为首次启用路径。**迁移除外**（§5）。

### 4. ScopePolicy 双模式

```
managed_packages == null  → 完全沿用 ADR-010 现有逻辑（读 package_list disabled）
managed_packages != null  → 新模型：
  - pkg ∉ managed → shouldHook=false
  - 无 enabled 干预规则 → shouldHook=false
  - 否则：enabled 规则 + scope_mode × handle_system × isSystemApp
```

`scope_mode` 在新模型下语义：**系统 app 过滤 + 全局 notify 默认分支**，不再表示「仅 hook 列表内 app」（该职责由 `managed_packages` 承担）。

### 5. 迁移（PrefMigrator 第二轮）

- 触发：`!managed_model_migrated`  
- 算法：见 [app-management-ui.md §迁移](../architecture/app-management-ui.md#迁移与兼容)  
- 对原 Switch-ON（不在 `package_list`）的包 **写入默认 `CATCH_ALL`**，保证 hook 行为等价  
- 验收：迁移前后 `ScopePolicy` 单测一致  

### 6. 配置 IA

| 项 | 决策 |
|----|------|
| 列表范围 | **仅** `managed_packages` |
| 行内 Switch | **v1 保留**；OFF→ON 可隐式创建/启用 `CATCH_ALL`；ON→OFF 仅 disable 规则 |
| 添加入口 | Toolbar P0 + 空态按钮 → **`AddManagedAppBottomSheet`** |
| 编辑页 | `AppInterventionEditActivity`；手动「添加规则」 |
| Picker 载体 | **Draggable Half Sheet**（v1）；见 [draggable-half-sheet.md](../design/components/draggable-half-sheet.md) |
| 新用户空列表 | **接受**：默认零 hook；空态强引导添加应用 |
| ADR-005 | **修订**：单屏高密度保留，列表语义改为受管子集 |

### 7. 与观测域命名

| 页面 | 域 | 职责 |
|------|-----|------|
| `AppInterventionEditActivity` | 配置 / 干预 | 规则 CRUD |
| `PerAppCrashActivity` | 观测 | 崩溃历史/统计；**无** hook 开关 |

### 8. 术语

| 术语 | 定义 |
|------|------|
| **干预规则** | Hook 侧拦截配置；本文 ADR |
| **诊断规则** | Phase 4G RuleEngine 分析模板；**不同概念** |

写入 [glossary.md](../glossary.md)（实施 commit 时）。

## 备选方案

| 方案 | 不选原因 |
|------|----------|
| 翻转 `package_list` 为白名单 | 破坏 ADR-002/014；升级行为不可预测 |
| 添加时自动默认规则 | Picker 添加时仍禁止；**Switch ON** 时允许隐式创建，属用户显式操作 |
| 无规则默认 hook | 与编辑页「添加规则才生效」矛盾 |
| 保留全量列表 + 星标受管 | 双列表 IA 混乱；不符合「策展」目标 |

## 后果

| 方面 | 影响 |
|------|------|
| **正面** | 意图清晰；配置渐进；列表规模可控 |
| **负面** | 新用户空列表默认零 hook；需引导 |
| **ADR-002** | Legacy 层保留；新模型下实质 supersede |
| **ADR-005** | 需修订列表范围与过滤 Chip |
| **ADR-010** | `ScopePolicy` 输入扩展；需单测 |
| **Phase** | 3G-α 数据层 + 3G-β UI；不阻塞 4C Shell |

## 相关文档

- [app-management-ui.md](../architecture/app-management-ui.md)
- [scope-and-prefs.md](../architecture/scope-and-prefs.md)
- [configuration-ui.md](../architecture/configuration-ui.md)
- [ui-routing.md](../architecture/ui-routing.md)
- [ADR-002](002-inverted-package-toggle.md)
- [ADR-005](005-settings-information-architecture.md)
- [ADR-010](010-scope-policy-show-notify.md)
- [ADR-014](014-legacy-prefs-migration.md)
