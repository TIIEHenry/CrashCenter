# CrashCenter

基于 Xposed 的 Android 应用稳定性工具：在目标进程中拦截 Java 层未捕获异常，防止闪退，并（规划中）采集崩溃日志供分析。

| | English | 中文 |
|---|---------|------|
| 应用名 | CrashCenter | 稳定性中心 |
| 包名 | `nota.android.crash.xp.app` | 同左 |

## 文档

- 开发入口：[AGENTS.md](AGENTS.md)
- 用户使用：[docs/guides/usage.md](docs/guides/usage.md)
- 文档索引：[docs/INDEX.md](docs/INDEX.md)

## 构建

```bash
./gradlew :app:assembleDebug
```

输出 APK：`app/build/outputs/apk/debug/CrashCenter_v*_debug.apk`
