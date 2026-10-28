# 随影 0.1.135 审查 —— 对比 58f67e3（0.1.134）→ f047257（PR #26）
只审后打包。工作树 /workspace/wt-review-0135。
测试：JVM **86/86** 通过（含 PaperPolishTest）；test_media3_offline.py 通过；build_ui.py --unsigned 通过（0.1.135 / code 136）；verify_apk.py 通过（apk signature: absent，未签名包符合预期）。
0.1.130 修复未回退：AggregateHome / CacheFiles / BifCache / ScopedHttpDataSource / HttpRange / EmbyStore / PreviewFetch / FadeFrame / UiImages / JellyfinTrickplay / InstallDefaults 等均不在本区间 diff 内。
结论：**可合并**。无中等及以上阻塞项；以下均为非阻塞说明。

## 范围摘要
1 个提交、26 个文件（+220 / −73）。主线是纸面对比度、Noto Serif SC 子集+后台预热、首页大图收紧、横屏详情分栏左右边距独立还原、播放侧栏陶土选中色、平板（sw600）横屏图下排版与短渐变。无播放引擎、离线、备份、跳读上限、EmbyStore 线程安全相关改动。

## 重点核对
### 字体子集与预热
- `NotoSerifSC-Regular.otf` 11,625,800 → **2,635,076** 字节；SHA-256 `eea4ea91…cae9`，与 fonts/README 一致。OFL 未改。✓
- `CinemaApp` 注册为 Application，`onCreate` → `CinemaTheme.warm` 在 daemon 线程 `paper-font` 调 `paperTitle`；双检锁 + `volatile paperFace` + `warming` 防重复。失败回退系统衬线。✓
- API 29+ 用 `CustomFallbackBuilder` + system serif fallback，子集缺字不崩。✓
- 冷启若预热未完成，首绘仍可能在锁上短等，但对象已是 ~2.6MB 子集（相对 0.1.134 的 ~11MB）。见 L1。

### 浅色对比度 / 暗金是否误伤
- `STONE` `#57554f`→`#45423c`，新增 `PROSE` `#322f2a`、`LINE` `#a58b74`、`CLAY_TRACK`；主按钮填充走 `CLAY` `#d97757`，12sp 操作/底栏选中仍 `CLAY_DEEP`。✓
- `muted()` / 导航未选中 / 开关 thumb 等 clay 路径才用 STONE；**gold 仍用 MUTED/SOFT/GOLD**，暗金色板未改。✓
- PaperPolishTest 校验纸面对比度（stone≥7、prose 更深、line≥3、deep≥4.5）。✓

### 首页大图与平板
- `PaperLayoutRules`：`tablet(sw≥600)`；手机竖屏 fade 90、手机横屏叠字 fade 180、平板 fade 64；`stackCaptionBelow` 仅「竖屏或平板横屏」图下排版，**手机横屏仍叠在画面上**。✓
- 平板横屏 hero 高度约屏高 40%；栏目标题/海报/简介字号略加大，间距收紧；按钮高度仍 44dp。✓
- clay 下年份/简介强制 STONE/PROSE + medium，解决平板白纸浅灰看不清。✓
- `goldBelow()` 现按 `stackCaptionBelow(wide,tablet)`，平板暗金横屏也会图下排版（可读性优先，仍用暗金色 token）。✓

### DetailLandscape unsplit
- `remember`/`PAD_TAG` 分别存左右 content inset；`unsplit` 用 `DetailSplitRules.restore`，**不再把右边距抄到左边**（修 0.1.134 L5）。✓
- 左栏宽按安全区内宽的 ~36%，大 cutout 不吞海报；测试覆盖。✓

### 播放侧栏
- `PlayerTheme.rowText/rowFill/chipText/chipFill/note`；选中暖陶 `ACCENT`/`ACCENT_DIM`；去掉 `sans-serif-light`。✓
- 播放面板仍深色；`paper()` 排除 Player/SystemPlayer。✓

### 安全
- 无新 token/密钥；备份/Emby 路径未改；字体仍 OFL。✓
- Application 类仅预热字体，无额外权限。✓

## 问题（按优先级）
无 H / M。

L1 低｜首启｜`warm` 与首绘仍可能竞态：预热未完成时 `paperTitle` 在主线程锁内解码子集（~2.6MB）。比 0.1.134 的 11MB 轻很多，一般可接受。

L2 低｜死代码｜`PaperLayoutRules.resumeSp` 已定义未引用；继续观看卡片字号仍走 HomeChrome 原值。

L3 低｜unsplit 回退｜若 body 尚无 `PAD_TAG`（极端早退路径），左右仍都用 `getPaddingRight()`；正常 `apply`/`split` 都会 `remember`，真机进出分栏应无感。

L4 低｜真机｜平板横屏可读性、浅色对比度、一键播放与侧栏选中色尚未在真机验证（PR 自述同样）。

## 其余已核对没问题
- versionName 0.1.135 / versionCode 136；verify 期望同步；产物名 `MediaPocket-0.1.135-paper-polish-arm64.apk`。
- InstallDefaults / 外观选项未再强改用户已选外观。
- Emby/Fusion 主按钮 fill 统一为 clay 时的 `CLAY`（非 CLAY_DEEP），与设计一致。
