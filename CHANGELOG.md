# Changelog

本文件记录 CrashCenter（稳定性中心）各版本的面向用户变更。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

## [1.0.0] - 2026-06-24

首个公开发布。

### Added

- Xposed 模块：拦截目标 app Java 层未捕获异常，防止进程退出
- **观测 / 拦截分离**：安装（观测）与崩溃拦截可独立配置；观测模式同步崩溃日志但不吞异常
- 配置 UI：受管应用列表、作用域模式、包可见性授权、Material / Fluent 视觉
- **崩溃历史**：分页列表、统计页（异常类型 / 分类 TOP）、按应用下钻、详情 BottomSheet + CodeEditor 浏览堆栈
- **崩溃分析**：规则引擎与内置签名库，详情页展示分析卡片
- **Logcat**：多 buffer（root）与 SAF 文件导入；崩溃相关过滤与 ANR 提示
- **导出**：崩溃记录 SAF zip 导出（含隐私提示）
- 崩溃通知可跳转详情；中英文界面
- GitHub Actions：`main`/PR 构建 debug；`v*` tag 自动 Release

### Changed

- 应用名 CrashCenter / 稳定性中心；包名 `nota.android.crash.xp.app`
- 崩溃详情由 TextView 升级为 CodeEditor（可滚动、可选中文本）
- 崩溃日志存储为各应用 `cache` 分布式 JSONL（root 聚合读取）

### Fixed

- 崩溃详情 BottomSheet / Dialog 触摸与主题问题
- CodeEditor 在详情页不可交互
- 测试崩溃改为记录事件而非令模块进程退出

## [0.1.0] - 2026-06-19

内部开发里程碑（未公开发布）。

### Added

- Xposed 模块骨架、基础配置 UI、崩溃通知与 `ActivityCrashInfo`
