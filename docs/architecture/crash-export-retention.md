---
title: "崩溃日志导出与 Retention 管理"
type: architecture
status: proposed
phase: 4
updated: 2026-06-19
summary: "Phase 4E SAF 导出 JSONL/zip、通知 crash_id Intent、retention 配置 UI 与轮转策略"
---

# 崩溃日志导出与 Retention 管理

> 适用模块：`:app`（Phase 4E）
> 数据层：[crash-data-layer.md](crash-data-layer.md) — `CrashLogRepository.applyRetention`
> 通知改造：[crash-notification.md](crash-notification.md)、[crash-capture-pipeline.md](crash-capture-pipeline.md)
> 历史 UI：[crash-history-ui.md](crash-history-ui.md)

## 概述

Phase 4E 为观测层增加 **运维与扩展** 能力：

1. **SAF 导出**：用户主动将崩溃历史导出为 JSONL 或 zip 文件
2. **通知 crash_id**：Notification PendingIntent 传 `crash_id`（UUID）替代整段 stack extra
3. **Retention 配置 UI**：用户可调整最大记录条数、清空历史

## SAF 导出

### 用户流程

1. 观测 tab Toolbar → "导出" 菜单项
2. 隐私提示对话框（"崩溃日志可能包含文件路径或敏感数据，是否继续？"）
3. 用户确认 → SAF `ACTION_CREATE_DOCUMENT` 选择保存路径
4. 导出完成 → Toast / Snackbar 提示

### 导出格式

| 格式 | MIME | 说明 |
|------|------|------|
| JSONL | `application/jsonl` | 原始 `events.jsonl` 内容 |
| ZIP | `application/zip` | `events.jsonl` + `meta.json` + 可选 `analysis.json` |

### 实现

```kotlin
fun exportToSaf(uri: Uri, format: ExportFormat) {
    contentResolver.openOutputStream(uri)?.use { out ->
        when (format) {
            JSONL -> repository.copyCanonicalTo(out)
            ZIP -> ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("events.jsonl"))
                repository.copyCanonicalTo(zip)
                zip.closeEntry()
            }
        }
    }
}
```

**约束**：导出操作在后台线程；UI 显示 indeterminate progress；导出过程不阻塞 ingest/写入。

## 通知 crash_id Intent

### 现状

当前 `XposedEntry.showNotification` 将整段 `Log.getStackTraceString(throwable)` 放入 Intent extra `"Exception"`。Binder 传输限制 ~512KB；大 stack 有截断风险。

### 目标（4E）

```java
intent.putExtra("crash_id", event.getId());  // UUID string
// 保留兼容：intent.putExtra("Exception", stackTrace);  // 4E 过渡期仍传
```

`CrashLogDetailActivity` 优先通过 `crash_id` 从 Repository 加载完整事件；若 `crash_id` 缺失则回退读 `Exception` extra（兼容旧通知点击）。

### 时序

```
CrashCapturePipeline.onException
  → buildEvent (UUID)
  → CrashLogCoordinator.logAsync(event)      // 先写入
  → CrashFeedbackFacade.show(event, decision) // 通知带 crash_id
       → PendingIntent extras: {crash_id: UUID, Exception: stack(兼容)}
```

详见 [ADR-013](../decisions/013-notification-crash-id-intent.md)。

## Retention 配置 UI

### 设置项

位于观测 tab Toolbar 菜单 → "记录设置" 对话框：

| 项 | pref key | 控件 | 默认 |
|----|----------|------|------|
| 记录总开关 | `crash_log_enabled` | Switch | true |
| 最大条数 | `crash_log_max_entries` | NumberPicker / Slider | 500 |
| 清空历史 | — | Button + 确认对话框 | — |

### 轮转触发

见 [crash-data-layer.md § Retention 策略](crash-data-layer.md#retention-策略)。

## 相关文档

- [crash-data-layer.md](crash-data-layer.md) — Repository retention 接口
- [crash-logging.md](crash-logging.md) — retention 规格
- [crash-capture-pipeline.md](crash-capture-pipeline.md) — crash_id 生成
- [crash-notification.md](crash-notification.md) — 通知流程
- [crash-history-ui.md](crash-history-ui.md) — 详情页 crash_id 加载
- [ui-routing.md](ui-routing.md) — Intent 参数兼容
- [ADR-013](../decisions/013-notification-crash-id-intent.md) — 通知 crash_id 决策
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md) — 4E 任务
