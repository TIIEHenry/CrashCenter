---
title: "v1.0.0 双渠道发布清单"
type: plan
status: active
phase: N/A
updated: 2026-06-24
summary: "GitHub Release v1.0.0 与 LSPosed 模块仓 1-1.0.0 发布步骤（执行前 checklist）"
---

# v1.0.0 双渠道发布清单

| 项 | 值 |
|----|-----|
| `versionName` | `1.0.0` |
| `versionCode` | `1` |
| GitHub tag | `v1.0.0` |
| Xposed Release tag | `1-1.0.0` |
| APK 文件名 | `CrashCenter_v1.0.0_release.apk` |

## 发布前自检

- [ ] `scripts/extract-changelog.sh 1.0.0` 输出非空
- [ ] `./gradlew :app:assembleRelease` 成功
- [ ] 模块仓 `README.md` / `SUMMARY` / `SOURCE_URL` 已 push
- [ ] 模块仓 GitHub **Description** = `CrashCenter`（非空）
- [ ] 模块仓 **Website** = `https://github.com/TIIEHenry/CrashCenter/issues`

## 1. 主仓库 CrashCenter

```bash
cd /home/clarence/Project/Clarence/Xposed/CrashCenter

# 校验
scripts/extract-changelog.sh 1.0.0 | head
./gradlew :app:assembleRelease

# 提交版本 bump（若尚未提交）
git add CHANGELOG.md app/build.gradle dev/plans/release-v1.0.0.md
git commit -m "chore(release): prepare v1.0.0"
git push origin main

# 打 tag 触发 GitHub Actions Release
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

CI 完成后：https://github.com/TIIEHenry/CrashCenter/releases/tag/v1.0.0

## 2. LSPosed 模块仓

```bash
cd /home/clarence/Project/Clarence/Xposed/nota.android.crash.xp.app

git add README.md SUMMARY SOURCE_URL
git commit -m "docs: initial listing for v1.0.0"
git push -u origin main
```

在 GitHub 创建 Release：

| 字段 | 值 |
|------|------|
| Tag | `1-1.0.0` |
| Title | `1.0.0` |
| Body | 复制 `CrashCenter/release/xposed-release-notes-1.0.0.md` 或 CHANGELOG `## [1.0.0]` 段落 |
| APK | `CrashCenter/app/build/outputs/apk/release/CrashCenter_v1.0.0_release.apk` |

约 5 分钟后验证：https://modules.lsposed.org/module/nota.android.crash.xp.app.json

## 相关文档

- [guides/release.md](../../docs/guides/release.md)
- [guides/xposed-module-repo.md](../../docs/guides/xposed-module-repo.md)
