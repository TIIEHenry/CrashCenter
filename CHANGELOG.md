# Changelog

本文件记录 CrashCenter（崩溃中心）各版本的面向用户变更。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

## [0.1.0] - 2026-06-19

### Added

- Xposed 模块：拦截目标 app Java 层未捕获异常，防止进程退出
- 配置 UI：应用列表、作用域模式、包可见性授权、Material / Fluent 视觉
- 崩溃通知与详情页（`ActivityCrashInfo`）

### Changed

- 应用更名 CrashCenter / 崩溃中心；包名 `nota.android.crash.xp.app`
