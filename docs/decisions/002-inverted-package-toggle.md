---
title: "ADR-002: 反向 Toggle（禁用列表）"
type: decision
status: accepted
phase: N/A
updated: 2026-06-19
summary: "package_list 存储禁用包名，Switch 开启表示 hook，默认全选"
---

# ADR-002: 反向 Toggle（禁用列表）

## 背景

配置 UI 需要让用户选择哪些 app 被 hook。有两种存储模型：
- **白名单**：`package_list` 存储要 hook 的包
- **黑名单**：`package_list` 存储要**排除**的包

## 决策

采用**黑名单（禁用列表）**模型：

- `package_list` 存储**不 hook** 的包名
- UI Switch **开启** = 包名 **不在** `package_list` 中
- 首次使用（`package_list == null`）→ 所有 app 默认开启（全部 hook）

## 关键要点

1. 与 scope_mode 配合：scope 模式下排除系统 app + 禁用列表
2. `updatePref()` 将 unchecked 的 app 加入 Set
3. 变量名 `prefWhiteList` 是历史命名，实际语义为 disabled set

## 后果

- **正面**：默认行为 hook 全部 app，符合「全局防 crash」直觉；用户只需关闭少数 app
- **负面**：命名 `package_list` / `prefWhiteList` 与实际语义不一致，易混淆
- **跟进**：glossary 中明确定义；未来重构可考虑重命名为 `disabled_packages`

## 备选方案

- **白名单模型** → 新用户需逐个开启，默认不 hook 任何 app → 不选
- **manifest xposed_scope 静态声明** → 无法动态 per-app 配置 → 不选（当前 `arrays.xml` 中 xposed_scope 为空）

## 相关

- 方案文档：[scope-and-prefs.md](../architecture/scope-and-prefs.md)
- 术语：[glossary.md](../glossary.md)
