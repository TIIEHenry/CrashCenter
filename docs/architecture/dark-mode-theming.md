---
title: "夜间模式与主题适配"
type: architecture
status: accepted
phase: 3
updated: 2026-06-22
summary: "Phase A–D 已编码；Meizu 实机 QA PASS；M3 迁移见 ADR-022（静态 Fluent，无 dynamic color）"
---

# 夜间模式与主题适配

> 适用模块：`:app` 配置 UI、Shell、详情页
> 设计系统：[ADR-009](../decisions/009-ui-shell-design-system.md)、[design-system.md](design-system.md)
> 主题决策：[ADR-022](../decisions/022-material3-static-theme-minsdk26.md)（M3 静态 Fluent；**拒绝** dynamic color）、[material3-migration.md](material3-migration.md)
> 全局色板 SSOT：[visual-language.md](../design/visual-language.md)

## 概述

CrashCenter 当前为 **仅浅色** 的 Material Components 2.x + Fluent token 实现。`SystemBars` 已根据 `uiMode` 切换 status bar 图标明暗，但 **无** `values-night/` 资源，窗口与控件仍强制浅色（`#FAFAFA` canvas、`#FFFFFF` surface）。系统开启深色模式时，状态栏图标变亮、内容区仍白底，产生 **割裂体验**。

本方案引入 **语义 token + DayNight 资源覆盖**（Phase A–D 已实施）。后续 M3 迁移（[material3-migration.md](material3-migration.md)）沿用同一 `values-night/` SSOT，**仍不** 引入 dynamic color / 壁纸取色。

**范围**：模块 UI 进程（配置、观测、详情）；hook 侧无 UI，无跨进程主题同步需求。

**非目标**：Material You 取色、Compose 主题、**应用内**浅色/深色/跟随系统三态 toggle（**v1 不做**；若日后增加须先写 **ADR-016**，预留编号尚未创建）。

**v1 范围（DayNight）**：仅 **系统级** `UI_MODE_NIGHT` → `values-night/` 资源覆盖 + `Theme.MaterialComponents.DayNight`。**无** Settings 项、**无** `SharedPreferences` 主题 override、**无** `AppCompatDelegate.setDefaultNightMode` 应用级强制。

---

## 1. 现状分析

### 1.1 主题与样式链

| 项 | 当前实现 | 暗色缺口 |
|----|----------|----------|
| 根主题 | `Theme.MaterialComponents.Light.NoActionBar` → `AppTheme` | `Light` 父类锁定浅色 widget 默认色 |
| 语义色 | `values/colors.xml`：`fluent_*` 原始值 + `background`/`surface`/`textPrimary` 等别名 | **无** `values-night/colors.xml` |
| 控件样式 | `AppTheme.Toolbar` / `Chip` / `SearchField` / `Switch` / `CrashTrace` | 均引用 `@color/*`，夜间需别名覆盖 |
| 状态栏 | `android:statusBarColor=transparent`；`SystemBars.setup()` 读 `uiMode` 设 `isAppearanceLightStatusBars` | **已部分就绪**；nav bar 未处理 |
| Night 资源 | `values-night/` **不存在** | 全量缺失 |

```xml
<!-- 当前 styles.xml 根主题（节选） -->
<style name="AppTheme" parent="Theme.MaterialComponents.Light.NoActionBar">
    <item name="android:colorBackground">@color/background</item>
    <item name="android:textColorPrimary">@color/textPrimary</item>
    ...
</style>
```

### 1.2 硬编码与固定填充

**colors.xml 原始 hex（浅色单份）**

| Token | 浅色值 | 用途 |
|-------|--------|------|
| `fluent_canvas` | `#FAFAFA` | 窗口底 |
| `fluent_layer` | `#FFFFFF` | Toolbar、BottomNav、Sheet |
| `fluent_accent` | `#0078D4` | Primary、PermissionBanner 文案/按钮 |
| `status_success` / `status_warning` | `#107C10` / `#8A6116` | 盾牌图标、角标文案 |
| `statusActiveBg` / `statusInactiveBg` | `#E8F4FD` / `#FFF4CE` | StatusBanner、ManagedApp 角标底 |

**Drawable shape（引用 `@color`，无内嵌 hex — 夜间友好）**

- `bg_status_active.xml` → `@color/statusActiveBg`
- `bg_status_inactive.xml` → `@color/statusInactiveBg`
- `bg_status_permission.xml` → `@color/fluent_accent_container_subtle`
- `bg_status_enabled_badge.xml` / `bg_status_pending_badge.xml` → 同上语义底
- `bg_system_badge.xml` → `@color/surfaceVariant`
- `bg_panel_stroke.xml` → `@color/surface` + `@color/outline`

**矢量图标**

- `ic_shield_check` / `ic_shield_off` → `@color/status_success` / `@color/status_warning`（随 token 切换）
- `ic_search` / `ic_sort` / `ic_filter` → `@color/textPrimary`（随 token 切换）

**布局中直接 `@color/` 引用（非 `?attr/`）**

- Shell：`activity_main_shell.xml` — `background`、`surface`、`toolbarDivider`
- 配置：`fragment_config.xml`、`view_permission_banner.xml`、`view_status_banner.xml`
- 详情：`activity_crashinfo.xml`、`activity_app_intervention_edit.xml`
- Bottom sheet：`bottom_sheet_add_managed_app.xml`
- 空态/加载：`view_empty_state*.xml`、`view_loading_state.xml`

部分列表行已用 `?android:attr/textColorPrimary|Secondary`（`view_managed_app_row.xml`、`view_crash_event_row.xml`），主题切换后可自动适配 **若** `AppTheme` 正确设置 `textColorPrimary`。

### 1.3 Kotlin 程序化设色

| 类 | 行为 | 风险 |
|----|------|------|
| `StatusBanner.bind()` | `setBackgroundResource` + `ContextCompat.getColor(R.color.statusActiveText\|statusInactiveText)` | 资源 ID 正确则 OK；更宜改 `?attr/colorOnStatusBanner*` |
| `ManagedAppRow.bind()` | 角标 `setBackgroundResource` + `statusActiveText` / `statusInactiveText` | 同上 |
| `PermissionBanner.bind()` | 仅 visibility / 文案，颜色在 XML | 低 |
| `CrashEventRow` / `EmptyState` / `LoadingState` | 无硬编码色 | 低 |

### 1.4 与 Material DayNight / 系统暗色的差距

| 能力 | 现状 | 目标 |
|------|------|------|
| `uiMode` 资源限定符 | 无 | `values-night/colors.xml` + 可选 `themes.xml` |
| DayNight 主题父类 | `Light` | `Theme.MaterialComponents.DayNight.NoActionBar` |
| BottomNavigation 默认 tint | 未显式设置 `itemIconTint` / `itemTextColor` | 定义 `@color/bottom_nav_*` selector |
| Navigation bar 图标 | 未设置 `isAppearanceLightNavigationBars` | `SystemBars` 与 status bar 对称 |
| `configChanges` 热切换 | 默认 Activity 重建 | 可接受；确保无静态色缓存 |
| M3 dynamic color | ADR-006 defer | 本方案 **不引入** |

### 1.5 边缘组件

| 组件 | 现状 | 适配策略 |
|------|------|----------|
| **ActivityCrashInfo** | `TextView` + `AppTheme.CrashTrace`（monospace `@color/textPrimary`） | token 覆盖；CodeEditor **`setDark(night)` 归属 4C-β**（见 [code-editor-porting.md](code-editor-porting.md)） |
| **WebView** | 未使用 | N/A；若后续引入须 `WebSettings` + CSS 或 `forceDark` |
| **Notification** | hook 侧用 **目标 app** `applicationInfo.icon` | 与模块主题无关；模块自有通知（若有）用 `ic_launcher` + 系统模板 |
| **Dialog / BottomSheet** | Material 默认；sheet 布局 `@color/surface` | token 覆盖 |
| **第三方 Material** | M2 1.13.0，`BottomNavigationView`、`TextInputLayout` | DayNight 父主题 + 显式 tint 属性 |

---

## 2. 适配策略

### 2.1 决策摘要

| 决策 | 选择 | 理由 |
|------|------|------|
| 主题父类 | `Theme.MaterialComponents.DayNight.NoActionBar` | ADR-006 允许 M2；DayNight ≠ M3 |
| Token 模型 | 语义别名 + `fluent_*` 原始值分 light/night | 与 [visual-language.md](../design/visual-language.md) YAML 一致 |
| Drawable | **优先 tint / `@color` 引用**；避免 `-night` drawable 副本 | 现有 shape 已引用 color |
| 程序化色 | 改 `theme.resolveAttribute` 或 `@color` 语义名（非 `ContextCompat` 硬编码新 hex） | 减少 bind 分支 |
| 用户 override | **v1 仅跟随系统** DayNight | 单模块 app；三态 toggle **defer** → 须 ADR-016 |
| M3 / dynamic color | **不做** | ADR-006、ADR-009 |

### 2.2 Night token SSOT

**权威来源**：[visual-language.md](../design/visual-language.md) §统一色彩体系 + §完整 Token 清单（YAML）。CrashCenter `values-night/colors.xml` **不得**自行发明平行色板；深色 hex 须与 YAML `*-dark` 键一致（或文档化的一阶派生，见 status 容器对）。

**实现规则**

1. `values/colors.xml` / `values-night/colors.xml` 仅维护 **语义别名**（对外 API）；`fluent_*` 为 mode 原始值。
2. 深色 mode **禁止**把浅色容器 tint 直接 alias 到 night（例如 `statusActiveBg` 不得在 night 仍指向 `fluent_accent_container_subtle` 浅色值）。
3. Status / banner 须 **bg + fg 成对** 在 `values-night/` 定义；WCAG 按对校验，不单换字色。

#### visual-language → CrashCenter 映射（基础面）

| visual-language (YAML) | CrashCenter 语义别名 | 浅色 | 深色 (SSOT) |
|------------------------|----------------------|------|-------------|
| `canvas-light` / `canvas-dark` | `background` ← `fluent_canvas` | `#FAFAFA` | `#202020` |
| `layer-light` / `layer-dark` | `surface` ← `fluent_layer` | `#FFFFFF` | `#2D2D2D` |
| `text-primary-*` | `textPrimary` ← `fluent_text_primary` | `#242424` | `#FFFFFF` |
| `text-secondary-*` | `textSecondary` ← `fluent_text_secondary` | `#616161` | `#A0A0A0` |
| `stroke-*` | `outline`, `divider` ← `fluent_stroke` | `#E0E0E0` | `#3D3D3D` |
| `accent` / `accent-dark` | `colorPrimary` ← `fluent_accent` | `#0078D4` | `#479EF5` |
| `accent-container-*` | `primaryContainer` ← `fluent_accent_container` | `#EFF6FC` / `#DEECF9` | `#0A2E4A` |
| `success-*` | `status_success` | `#107C10` | `#6CCB5F` |
| `warning-*` | `status_warning` | `#8A6116` | `#FCE100` |

> `fluent_accent_container` 浅色当前为 `#DEECF9`（实现值）；与 visual-language `accent-container-light` `#EFF6FC` 差 1 档灰 — Phase A 可选对齐 SSOT，非阻塞 night。

#### Status banner 深色 **成对** token（禁止复用浅色 tint）

| 语义对 | 浅色 bg → fg | 深色 bg → fg | 说明 |
|--------|--------------|--------------|------|
| Xposed **已激活** | `#E8F4FD` → `#107C10` | `#0D3A5C` → `#6CCB5F` | 深底用 `success-dark` 字，**非**浅蓝字 |
| Xposed **未激活** | `#FFF4CE` → `#8A6116` | `#3D3500` → `#FCE100` | 深底用 `warning-dark` 亮黄字；**禁止** `#FFF4CE` 作 night 字色 |
| Permission 提示 | `#E8F4FD` → `#0078D4` | `#0D3A5C` → `#479EF5` | 容器同 accent subtle 系；字色随 `accent-dark` |

`statusActiveBg` / `statusInactiveBg` / `statusActiveText` / `statusInactiveText` 在 `values-night/colors.xml` **各自覆盖**，不依赖 `@color/fluent_accent_container_subtle` 等浅色-only 引用链。

### 2.3 colors.xml 语义别名命名规范

现有 `app/src/main/res/values/colors.xml` 在 Phase A 实施时 **规范化**（仅重命名/alias 链，不改布局 `@color/` 引用面）：

| 现状名 | 规范方向 | visual-language 角色 |
|--------|----------|----------------------|
| `fluent_canvas` | 保留 raw；`background` → alias | `canvas` |
| `fluent_layer` | 保留 raw；`surface`, `surfaceCard` → alias | `layer` |
| `fluent_fill_subtle` | 保留 raw；`surfaceVariant` → alias | layer 变体 |
| `fluent_stroke` / `fluent_stroke_subtle` | 保留 raw；`outline`, `divider`, `toolbarDivider`, `outlineVariant` → alias | `stroke` |
| `fluent_text_primary` … | 保留 raw；`textPrimary` … → alias | `text_primary` … |
| `fluent_accent` … | 保留 raw；`colorPrimary`, `colorAccent`, `chipSelectedText` → alias | `accent` |
| `fluent_accent_container` | 保留 raw；`primaryContainer`, `chipSelectedBg` → alias | `accent_container` |
| `fluent_accent_container_subtle` | 保留 raw；PermissionBanner / **浅色** statusActiveBg 引用 | accent subtle 容器 |
| `status_success` / `status_warning` | 保留；与 `success` / `warning` 角色 1:1 | 状态前景 |
| `statusActiveBg` / `statusInactiveBg` | **night 独立 hex**；见 §2.2 成对表 | 状态容器 |
| `statusActiveText` / `statusInactiveText` | alias → `status_success` / `status_warning`（night 覆盖链） | 状态前景 |
| `colorPrimaryDark` | night 覆盖为 `#2899F5`（派生，非 SSOT 主键） | — |
| Legacy `chipSelectedStroke`, `systemAppBadge` | 保持 alias，随 semantic 覆盖 | — |

**命名约定**：新增 token 优先 **camelCase 语义名**（`textPrimary`）映射 visual-language **snake**（`text_primary`）；`fluent_*` 前缀仅用于 mode 原始 palette，不直接在 Kotlin 硬编码。

### 2.4 语义 Token 模型（light / night 值表）

在 `values/colors.xml` 保留 **语义别名** 作为唯一对外 API；`fluent_*` 作为浅色原始值。`values-night/colors.xml` 覆盖同名语义别名（及必要 `fluent_*` night 值）。

#### 基础面与文本

| 语义 Token | 浅色 | 深色 | WCAG 备注 |
|------------|------|------|-----------|
| `background` (canvas) | `#FAFAFA` | `#202020` | 深底主文本对比 > 15:1 |
| `surface` (layer) | `#FFFFFF` | `#2D2D2D` | Toolbar / Nav / Sheet |
| `surfaceVariant` | `#F5F5F5` | `#383838` | Badge 底、Chip 未选中 |
| `textPrimary` | `#242424` | `#FFFFFF` | 正文 ≥ 4.5:1 on canvas |
| `textSecondary` | `#616161` | `#A0A0A0` | 副文案 ≥ 4.5:1 on canvas |
| `textTertiary` | `#707070` | `#8A8A8A` | 辅助 ≥ 3:1 |
| `outline` / `divider` | `#E0E0E0` | `#3D3D3D` | 描边 ≥ 3:1 on surface |

#### 强调色（Communication Blue）

| 语义 Token | 浅色 | 深色 | WCAG 备注 |
|------------|------|------|-----------|
| `colorPrimary` / `fluent_accent` | `#0078D4` | `#479EF5` | 链接/按钮 on dark canvas ~ 4.6:1 |
| `colorPrimaryDark` | `#005A9E` | `#2899F5` | status bar 无关（透明栏） |
| `primaryContainer` | `#DEECF9` | `#0A2E4A` | Chip 选中底 |
| `fluent_accent_container_subtle` | `#E8F4FD` | `#0D3A5C` | PermissionBanner 底 |

#### 状态语义

| 语义 Token | 浅色 | 深色 | 用途 |
|------------|------|------|------|
| `status_success` | `#107C10` | `#6CCB5F` | 盾牌 check、enabled 角标字 |
| `status_warning` | `#8A6116` | `#FCE100` | 盾牌 off、pending 角标字；**深底用亮黄字** |
| `statusActiveBg` | `#E8F4FD` | `#0D3A5C` | Xposed 已激活 banner |
| `statusInactiveBg` | `#FFF4CE` | `#3D3500` | 未激活 banner（深底降饱和黄底） |
| `statusActiveText` | → `status_success` | → `status_success` | 别名链 |
| `statusInactiveText` | → `status_warning` | → `status_warning` | 深底 `#FCE100` on `#3D3500` > 4.5:1 |

> **WCAG**：状态 banner 以 **容器底 + 前景字** 成对校验；禁止仅换字色不换底。深色系 warning 字色用 `#FCE100`（visual-language SSOT），不用浅黄 `#FFF4CE` 作字色。

#### 新增建议 Token（可选，减少 magic name）

| Token | 说明 |
|-------|------|
| `colorOnSurface` | 等同 `textPrimary`；供 `MaterialColors.getColor(context, R.attr.colorOnSurface)` |
| `bottomNavItemColor` | selector：选中 primary / 未选中 textSecondary |
| `statusBannerActiveBackground` | 别名 `statusActiveBg`；bind 可读主题 attr |

### 2.5 资源结构

```
app/src/main/res/
├── values/
│   ├── colors.xml          # 语义别名 → fluent_* 浅色
│   ├── styles.xml          # AppTheme parent → DayNight
│   ├── themes.xml          # （可选）拆分 themes vs styles
│   └── attrs.xml           # （新增）cc_* 自定义 attr → @color 引用
├── values-night/
│   ├── colors.xml          # 同名语义别名 → fluent_* 深色
│   └── themes.xml          # （可选）仅 night 差异项
├── color/
│   ├── chip_*.xml          # 已引用 @color；无需 night 副本
│   └── bottom_nav_item_color.xml  # （新增）
└── drawable/
    └── bg_*.xml            # 保持 @color 引用，不复制 -night
```

#### 主题变更（Phase A 核心）

```xml
<!-- values/styles.xml -->
<style name="AppTheme" parent="Theme.MaterialComponents.DayNight.NoActionBar">
    <item name="colorPrimary">@color/colorPrimary</item>
    <item name="colorSurface">@color/surface</item>
    <item name="android:colorBackground">@color/background</item>
    <item name="android:windowBackground">@color/background</item>
    <item name="android:textColorPrimary">@color/textPrimary</item>
    <item name="android:textColorSecondary">@color/textSecondary</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
    <!-- BottomNavigation 默认色 -->
    <item name="bottomNavigationStyle">@style/AppTheme.BottomNavigation</item>
</style>
```

`values-night/` **不必**复制整份 `styles.xml`，除非 night 需不同 `windowLightNavigationBar`；Chip/Switch/SearchField 样式通过 `@color` 自动跟随。

#### attrs.xml（程序化读取）

```xml
<declare-styleable name="CrashCenterTheme">
    <attr name="statusBannerActiveBackground" format="color" />
    <attr name="statusBannerInactiveBackground" format="color" />
    <attr name="statusBannerActiveTextColor" format="color" />
    <attr name="statusBannerInactiveTextColor" format="color" />
</declare-styleable>
```

在 `AppTheme` 中映射到 `@color/statusActiveBg` 等；`StatusBanner.bind` 用 `MaterialColors` 或 `TypedValue` 解析。

### 2.6 SystemBars 扩展（Shell + 详情，API 26+）

`SystemBars.setup()` 由 **Shell 与详情 Activity 共用**（`MainShellActivity`、`ActivityCrashInfo`、`AppInterventionEditActivity` 等）。当前仅处理 status bar；Phase A 须扩展 navigation bar，使 edge-to-edge 底栏与 `@color/background` canvas 对比一致。

**调用方**

| Activity | 入口 | Phase A 动作 |
|----------|------|--------------|
| `MainShellActivity` | Shell 唯一 Launcher | `SystemBars.setup()` + BottomNav 贴底 inset |
| `ActivityCrashInfo` | 详情 / 通知 | 已有 `setup()`；随 nav bar 扩展自动受益 |
| `AppInterventionEditActivity` | 干预规则编辑 | 同上 |

**实现要点**

```kotlin
val isLightTheme = (uiMode and UI_MODE_NIGHT_MASK) != UI_MODE_NIGHT_YES
controller.isAppearanceLightStatusBars = isLightTheme
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    controller.isAppearanceLightNavigationBars = isLightTheme
}
```

- **API 26+（O）**：`WindowInsetsControllerCompat.isAppearanceLightNavigationBars` 可用；与 status bar 对称设置。
- **API 21–25**：无 navigation bar 图标明暗 API；保持透明 `navigationBarColor`，依赖系统默认对比（Phase D 抽检）。
- **浅色主题**：dark icons（现有 status bar 行为）
- **深色主题**：light icons on `#202020` canvas
- edge-to-edge 保持；`ToolbarHeaderInsets` 仍仅处理 status bar inset
- **Meizu / MIUI / ColorOS**：部分机型 nav bar 半透明；透明栏 + icon 对比在 Phase D 实机验证

> Shell 引入前 status bar 已在 `ActivityMain` 使用；4C-α 已迁至 `MainShellActivity`。nav bar 扩展 **一次改 `SystemBars.kt`**，全 Activity 受益。

### 2.7 组件矩阵（Design System）

| 组件 | 布局/类 | 浅色依赖 | 夜间动作 |
|------|---------|----------|----------|
| **StatusBanner** | `view_status_banner.xml`, `StatusBanner.kt` | `bg_status_*`, `status*Text` 程序化 | token 覆盖 + bind 改 attr；默认 XML 未激活态随 night |
| **PermissionBanner** | `view_permission_banner.xml` | `bg_status_permission`, `fluent_accent` 字/按钮 | token 覆盖；`TextButton` 继承 `colorPrimary` |
| **FilterChipRow** | `view_*_chip_row.xml`, `AppTheme.Chip*` | `chip_*_color.xml` selectors | selectors 已绑 semantic color，**零改** |
| **DenseSearchField** | `view_dense_search_field.xml` | `AppTheme.SearchField` | token 覆盖 outline/hint |
| **EmptyState** | `view_empty_state*.xml` | `textSecondary`, MaterialButton 主题色 | token + 按钮 `?attr/colorPrimary` |
| **LoadingState** | `view_loading_state.xml` | `colorPrimary` indicator | token 覆盖 |
| **ManagedAppRow** | `view_managed_app_row.xml`, `ManagedAppRow.kt` | 行文本 `?attr/` OK；角标 programmatic | 同 StatusBanner 角标 token |
| **CrashEventRow** | `view_crash_event_row.xml` | `?attr/textColor*`, `bg_system_badge` | token 覆盖 badge |
| **BottomNavigation** | `activity_main_shell.xml` | 无 tint；`@color/surface` 底 | 新增 `@color/bottom_nav_item_color` + `AppTheme.BottomNavigation` |
| **Bottom sheet** | `bottom_sheet_add_managed_app.xml` | `@color/surface`, `textPrimary`, `colorPrimary` | token 覆盖 |
| **AppInterventionEdit** | `activity_app_intervention_edit.xml` | 大量 `@color/text*`, `status_warning` 描边 | token 覆盖；warning 按钮描边随 night |
| **ActivityCrashInfo** | `activity_crashinfo.xml` | `background`, `AppTheme.CrashTrace` | Phase A–D：token；**4C-β**：`CrashLogViewerClient` + CodeEditor `setDark(isNight)` |
| **Shell / Config fragment** | `activity_main_shell`, `fragment_config` | `@color/background`, `surface` | token 覆盖 |

### 2.8 程序化颜色重构

**原则**：bind 方法 **不** 分支 `if (isNight)`；统一从 theme 解析。

```kotlin
// 目标形态（示意）
private fun Context.themeColor(@AttrRes attr: Int): Int =
    MaterialColors.getColor(this, attr, "CrashCenter")

// StatusBanner.bind
root.setBackgroundColor(context.themeColor(R.attr.statusBannerActiveBackground))
title.setTextColor(context.themeColor(R.attr.statusBannerActiveTextColor))
```

短期可保留 `ContextCompat.getColor(context, R.color.statusActiveText)` **若** `values-night/colors.xml` 已覆盖同名资源——改动最小。中长期迁移到 attr；**若** 增加应用内主题 toggle，须 ADR-016 + `ContextThemeWrapper`。

### 2.9 Hook / UI 进程

- 模块仅 **UI 进程** 呈现 Activity；hook 在目标 app 进程，**无** 主题上下文
- XSharedPreferences、通知、`ActivityCrashInfo` Intent 与深浅色无关
- 无需跨进程同步 theme prefs

### 2.10 备选方案

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| **强制浅色** `Light` + `android:forceDarkAllowed=false` | 零实现 | 与系统设置冲突；`SystemBars` 已读 night | **拒绝** |
| **DayNight + 语义 token**（本方案） | 跟随系统；与 ADR-006/009 兼容；改动可分期 | 需审计 layout/programmatic | **采用** |
| **Material 3 + dynamic color** | 自动 harmonize | 与 Fluent SSOT 冲突；[ADR-022](../decisions/022-material3-static-theme-minsdk26.md) **拒绝** | **不做** |
| **应用内三态 override** | 用户可控 | Settings、持久化、ADR-016 | **defer**（v1 不做） |
| **drawable-night 副本** | 精确控制 | 维护双倍 shape | **仅** 无法用 tint 的 bitmap 时使用 |

---

## 3. 迁移阶段

### Phase A — Token 与主题基座（估计 0.5–1 天）

- [x] 新增 `values-night/colors.xml`（§2.2 SSOT 深色值 + status **成对** bg/fg）
- [x] 规范化 `values/colors.xml` 语义 alias 链（§2.3；布局 `@color/` 名不变）
- [x] `AppTheme` parent 改为 `Theme.MaterialComponents.DayNight.NoActionBar`
- [x] 新增 `bottom_nav_item_color.xml`、`AppTheme.BottomNavigation`
- [x] 扩展 `SystemBars.setup()`：`isAppearanceLightNavigationBars`（**API 26+**）；Shell `MainShellActivity` 与详情 Activity 共用
- [x] `attrs.xml` + theme item 映射（`statusBanner*` → `@color/status*Bg|Text`）
- [x] `./gradlew :app:assembleDebug` + 模拟器 night toggle smoke（**461QYGDD2226C 实机 PASS**；AOSP 模拟器仍 defer）

### Phase B — 布局与 Drawable 审计（估计 1–1.5 天）

- [x] 将稳定文本色改为 `?android:attr/textColorPrimary|Secondary` 或 `?attr/colorOnSurface`（Shell、Sheet、EmptyState）
- [x] BottomNavigation 显式 `app:itemIconTint` / `itemTextColor`
- [x] 确认所有 `drawable/bg_*`、`ic_*` 无硬编码 hex（已满足则勾选）
- [x] `activity_app_intervention_edit` warning 色改 `@color/status_warning`
- [x] 更新 [configuration-ui.md](configuration-ui.md) 视觉表（light/dark 双列）

### Phase C — 程序化 bind（估计 0.5 天）

- [x] `StatusBanner.bind` / `ManagedAppRow.bind` → `themeColor(R.attr.statusBanner*TextColor)`（`ThemeColors.kt` + `attrs.xml`）
- [x] 搜索 `ContextCompat.getColor` / `setTextColor` / `Color.parse` 全模块清零（仅剩 `CrashEventRow` 的 `getDrawable`，无 programmatic 字色）
- [x] Dialog / AlertDialog：`ConfigFragment` 四处 → `MaterialAlertDialogBuilder` + activity DayNight theme

### Phase D — 设备 QA（估计 0.5–1 天）

- [ ] AOSP 模拟器 API 30/34/36 系统暗色切换
- [x] 实机：Meizu / MIUI / ColorOS 状态栏与 nav bar（**461QYGDD2226C** MEIZU 21 PASS）
- [x] 截图对比：Shell、Config、Observe 空态、BottomSheet、Managed 行（History/CrashInfo/Edit 待 4B 数据）
- [x] 对比度抽检（status banner、Chip 选中、Managed 角标；未做仪器 WCAG）
- [x] 记录于 `dev/verification/` → [dark_mode_qa_20260619.md](../../dev/verification/dark_mode_qa_20260619.md)

**总估计**：2.5–4 人天（Shell/配置域 DayNight）。**不含** CodeEditor — `setDark(night)` 随 **4C-β** `CrashLogViewerClient` 接入，**不**纳入 Phase 3C 可选 polish。

### Phase E — CodeEditor 暗色（4C-β，非 3C）

与 [code-editor-porting.md](code-editor-porting.md) / [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md) **4C-β** 对齐：

- [ ] Gradle 引入 `CodeEditor` + `CodeEditorClient`
- [ ] `CrashLogViewerClient` 读 `uiMode`，调用 `codeEditor.setDark(isNight)`
- [ ] 详情页（`ActivityCrashInfo`）验收：系统 night 下编辑器背景/语法色可读

> Phase 3C roadmap「可选暗色 `values-night/`」指 **应用壳 DayNight**（Phase A–D）；**不**包含 CodeEditor。3C 暗色任务实施时应引用本方案 Phase A–D，Editor 暗色留在 4C-β。

---

## 4. 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| Material M2 BottomNav 默认 tint 与 night 不符 | 选中项不可见 | 显式 `@color/bottom_nav_item_color` |
| `statusInactiveBg` 深底对比不足 | 未激活条难读 | 采用 `#3D3500` 底 + `#FCE100` 字 |
| Meizu Flyme status/nav bar 行为差异 | icon 与背景混淆 | Phase D 实机；必要时 OEM 分支 **最后手段** |
| 硬编码 hex 遗漏 | 夜间白块 | Phase B grep + lint `HardcodedColor`（若启用） |
| CodeEditor 接入 | 编辑器自带亮色系 | **4C-β** [code-editor-porting.md](code-editor-porting.md) `setDark(night)`；**非** 3C |
| 用户截图/文档仍展示浅色 | 认知偏差 | 验收截图含 dark variant |

---

## 5. 与 roadmap 关系

| Roadmap | 与本方案关系 |
|---------|--------------|
| [phase3_ui_redesign.md](../../dev/roadmap/active/phase3_ui_redesign.md) **3C** 可选「暗色 `values-night/`」 | 实施 **Phase A–D**（应用壳 DayNight）；架构 SSOT 为本文 |
| [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md) **4C-β** | CodeEditor + `setDark(night)` → 本文 **Phase E** |
| [ADR-009](../decisions/009-ui-shell-design-system.md) | Shell / Design System 须随 DayNight；后果见 ADR-009 §跟进（主题） |

- 实施 commit 独立于本文档 commit（方案 / 实施分离）
- 不触发 ADR-006 修订（DayNight ≠ M3）
- 应用内主题 toggle **不** 在 v1；若做 → **ADR-016**（尚未创建）

---

## 6. 验收标准

1. 系统设置切换深色模式后，**无需杀进程**，重启 Activity 即全屏 dark canvas/surface
2. StatusBanner / PermissionBanner / Chip / Search / 列表 / BottomNav / CrashInfo **无** 白底残留
3. Status bar + navigation bar 图标与背景对比正确
4. WCAG AA：正文 `textPrimary` on `background` ≥ 4.5:1；banner 语义色成对达标
5. `./gradlew :app:assembleDebug` 通过；`check-docs-health.py` 通过

---

## 相关文档

- [ADR-009: UI Shell 与 Design System](../decisions/009-ui-shell-design-system.md)
- [ADR-022: M3 静态 Fluent + minSdk 26](../decisions/022-material3-static-theme-minsdk26.md)
- [material3-migration.md](material3-migration.md)
- [configuration-ui.md](configuration-ui.md)
- [design-system.md](design-system.md)
- [code-editor-porting.md](code-editor-porting.md)
- [visual-language.md](../design/visual-language.md)
- [glossary.md](../glossary.md)
- [phase3_ui_redesign.md](../../dev/roadmap/active/phase3_ui_redesign.md)
- [phase4_crash_observability.md](../../dev/roadmap/active/phase4_crash_observability.md)
