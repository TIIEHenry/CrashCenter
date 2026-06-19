---
title: "ADR-013: 通知 PendingIntent 传 crash_id"
type: decision
status: accepted
phase: 4
updated: 2026-06-19
summary: "Phase 4E 起 Notification PendingIntent 传 crash_id UUID 替代整段 stack extra，详情页从 Repository 加载；保留 Exception extra 兼容过渡"
---

# ADR-013: 通知 PendingIntent 传 crash_id

## 状态

**Accepted** — Phase 4E 实施。

## 背景

当前 `XposedEntry.showNotification` 将完整 `Log.getStackTraceString(throwable)` 放入 Intent extra `"Exception"`（可达 64KB+）。问题：

1. **Binder 限制**：`PendingIntent` 序列化后若超 ~512KB（实测 ~1MB hard limit）会 crash
2. **不可引用**：通知消失后该 stack 无其他留存
3. **与 Repository 重复**：Phase 4B 后 `events.jsonl` 已有完整事件

## 决策

### 优先 `crash_id`

```java
intent.putExtra("crash_id", event.getId());  // 36 char UUID
```

`CrashLogDetailActivity` 启动时：

1. 读 `crash_id` → `Repository.getById(id)` → 展示
2. 若 `crash_id` 缺失（旧通知）→ 回退读 `Exception` extra

### 过渡期双 extra

Phase 4E 初期仍同时传 `Exception`（截断 32KB），确保：

- 旧版 `ActivityCrashInfo` 兼容
- Repository 中事件因 retention 轮转被删除时仍可展示

### 最终态（4E 稳定后）

- 仅传 `crash_id`
- 事件已删除 → 详情页显示 "记录已清除" 空态

## 后果

| 方面 | 影响 |
|------|------|
| Intent 大小 | 36 bytes UUID vs 4–64KB stack → 彻底消除 Binder 上限风险 |
| 通知→详情一致性 | 详情页展示完整事件（含 metadata），不再仅 stack |
| 向后兼容 | `Exception` extra 保留过渡；`ActivityCrashInfo` 不需立即删除 |
| 离线查看 | Repository 必须先于通知点击存在——Pipeline 先 logAsync 再 show |

## 相关文档

- [crash-capture-pipeline.md](../architecture/crash-capture-pipeline.md) — 先写入后通知顺序
- [crash-export-retention.md](../architecture/crash-export-retention.md) — retention 删除后兼容
- [crash-notification.md](../architecture/crash-notification.md) — 通知流程
- [ui-routing.md](../architecture/ui-routing.md) — Intent 参数定义
- [crash-history-ui.md](../architecture/crash-history-ui.md) — 详情页加载
