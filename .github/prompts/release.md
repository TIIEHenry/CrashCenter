# CrashCenter 发布 Prompt

人类可读的完整说明见 [docs/guides/release.md](../../docs/guides/release.md)（双渠道：GitHub + LSPosed 模块仓）。模块仓元数据见 [xposed-module-repo.md](../../docs/guides/xposed-module-repo.md)。

将下方「任务 Prompt」整段复制给 AI，或在 Cursor 中 `@.github/prompts/release.md` 引用后说明目标版本（例如「发布 v1.1.0，含 LSPosed」）。

---

## 任务 Prompt

你是 CrashCenter 仓库的发布助手。请根据自上一个 Git tag 以来的变更，撰写更新日志并完成**双渠道**发布准备。

### 仓库约定

- 更新日志：`CHANGELOG.md`（[Keep a Changelog](https://keepachangelog.com/zh-CN/)，中文、面向用户）
- 版本号：`app/build.gradle` 的 `versionCode`（递增）与 `versionName`（不含 `v`）
- **主仓库 GitHub Release**：推送 `v*` tag → `.github/workflows/release.yml` 构建并发布
- **LSPosed 模块仓**：`Xposed-Modules-Repo/nota.android.crash.xp.app`，Release tag `{versionCode}-{versionName}`（如 `2-1.1.0`），手动附 APK
- Release 正文：`scripts/extract-changelog.sh <versionName>`；可写入 `release/xposed-release-notes-<versionName>.md`
- APK：`CrashCenter_v{versionName}_release.apk`

### 你必须执行的步骤

1. **确定版本号** — `git tag -l 'v*' --sort=-v:refname`；未指定时建议 semver 并说明理由
2. **收集变更** — `git log <最新tag>..HEAD`；区分用户可见 vs 内部变更
3. **更新 CHANGELOG.md** — 新版本 `## [X.Y.Z] - 日期`；保留空 `## [Unreleased]`
4. **bump 版本** — `versionName` + `versionCode + 1`
5. **本地校验** — `extract-changelog.sh` 非空；可选 `assembleRelease`
6. **生成模块仓正文** — `release/xposed-release-notes-X.Y.Z.md`（可选提交）
7. **提交** — `chore(release): prepare vX.Y.Z`
8. **主仓发布（需用户同意才 push）** — `git push origin main`；`git tag -a vX.Y.Z`；`git push origin vX.Y.Z`
9. **模块仓（若用户要求 LSPosed 渠道）** — 提醒更新 `../nota.android.crash.xp.app` 的 README/SUMMARY（若需要）；网页创建 Release tag `{code}-{name}` 并附 APK；Description = `CrashCenter`

### 禁止事项

- 不要 force push；不要未经确认 push/tag
- `versionName`、CHANGELOG、`v*` tag 必须一致
- 模块仓 tag 必须是 `{versionCode}-{versionName}`，不是 `v*`

### 输出给用户

- 版本号与 `versionCode`、CHANGELOG 摘要
- 主仓 / 模块仓各自待执行命令
- GitHub Release 与 modules.lsposed.org 验证链接

---

## 快速触发示例

```
@.github/prompts/release.md 请发布 v1.1.0（GitHub + LSPosed 双渠道）
```

```
@.github/prompts/release.md 写好 CHANGELOG，准备好发布但不要 push
```
