---
title: "Xposed 框架参考"
type: reference
status: accepted
phase: N/A
updated: 2026-06-19
summary: "CrashCenter 使用的 Xposed API 与框架兼容性"
---

# Xposed 框架参考

## API 依赖

```gradle
compileOnly 'de.robv.android.xposed:api:82'
```

## 使用的 Xposed API

| API | 用途 |
|-----|------|
| `IXposedHookLoadPackage` | 模块入口接口 |
| `XC_LoadPackage.LoadPackageParam` | 包加载参数 |
| `XposedHelpers.findAndHookMethod` | hook Application.onCreate |
| `XposedHelpers.callStaticMethod` | CrashHandler 中调用 Looper/Thread |
| `XC_MethodHook` | onCreate after 回调 |
| `XC_MethodReplacement` | self hook isModuleActived |
| `XSharedPreferences` | 跨进程读取配置 |
| `XposedBridge.log` | 调试日志 |

## 框架兼容性

| 框架 | 包名 | 处理 |
|------|------|------|
| 经典 Xposed | `de.robv.android.xposed.installer` | 排除 hook |
| EdXposed | `org.meowcat.edxposed.manager` | 排除 hook |
| LSPosed | `org.lsposed.manager` | 排除 hook |

## Manifest 元数据

```xml
<meta-data android:name="xposedmodule" android:value="true" />
<meta-data android:name="xposeddescription" android:value="..." />
<meta-data android:name="xposedminversion" android:value="54" />
<meta-data android:name="xposedscope" android:resource="@array/xposed_scope" />
```

当前 `xposed_scope` 为空数组——scope 由 app 内动态管理。

## 外部链接

- [Xposed Framework API](https://api.xposed.info/)
- [LSPosed GitHub](https://github.com/LSPosed/LSPosed)

## 相关文档

- [architecture/xposed-entry.md](../architecture/xposed-entry.md)
- [decisions/003-xsharedpreferences-cross-process.md](../decisions/003-xsharedpreferences-cross-process.md)
