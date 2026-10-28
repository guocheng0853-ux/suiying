# 随影 0.1.131–0.1.134 审查 —— 对比 18473e9（0.1.130）→ 58f67e3（0.1.134）
只审后打包。工作树 /workspace/wt-review-0134。
测试：JVM 85/85 通过；test_media3_offline.py 通过；build_ui.py --unsigned 通过（0.1.134 / code 135）；verify_apk.py 通过（apk signature: absent，未签名包符合预期）。
0.1.130 修复未回退：AggregateHome / CacheFiles / BifCache / ScopedHttpDataSource / HttpRange / EmbyStore / PreviewFetch / FadeFrame / UiImages / JellyfinTrickplay 等均不在本区间 diff 内。
结论：**可打包**。无中等及以上阻塞项；以下均为非阻塞说明。

## 范围摘要
5 个提交、42 个文件（+475 / −162）。主线是外观（纸张陶土 + Noto Serif SC、日/夜/跟随系统）、首页一键播放、横屏详情分栏、首屏紧凑、播放「更多」去重、AKG/动漫音效。无播放引擎、离线、备份、跳读上限、EmbyStore 线程安全相关改动。

## 重点核对
### InstallDefaults / 升级是否强改
- 新装默认 `system`（跟随系统），不再默认 `gold` + 写聚合首页以外的强推；聚合首页仍只在 `writeProvider`（新装）时写入。✓
- 升级：已有 `clay` / `gold` / `system` 不改；旧 `dark`→折成 `gold`，`light`→`clay`，未设置→写 `system`。测试覆盖。✓
- ScrapedMainActivity 现算 `effective(normalize, night)`，并同步 `themeMode` / `ui.gold` / `ui.clay`。两套外观互斥，`editorial()` 恒真——深青色独立外观已移除（有意产品决策，见 L1）。
- **不是** 0.1.125–0.1.128 那种「已有用户强制暗金+聚合」；已选 clay/gold 的用户保留。✓

### CinemaTheme / PlayerTheme / 字体
- `paperTitle` 双检锁 + volatile，失败回退系统衬线；播放页菜单不用衬线。✓
- 浅色页 `barIcons` 深色状态栏图标；播放器表面仍深色（`PlayerTheme.paper` 排除 Player/SystemPlayer）。✓
- 内置 NotoSerifSC-Regular.otf ≈11.1MB（OFL），APK 约 44.6MB（0.1.130 约 34.8MB）。见 L2。

### ScrapedMainActivity / 一键播放
- 大图点击 → `openDetail`；主按钮 → `heroAction` → Emby 走 `playSmart`，其它进详情。✓
- `playSmart`：电影/单集直播；Series/Season 在 `emby-play` 线程查 NextUp，失败再扫一页 episodes 找未看，再失败开详情；可取消。不占主线程。✓
- `setHeroes` 缓存仍在 `job()`、前 5 条、gen 去重——0.1.130 行为保留。✓

### EmbyPlayback.playSmart
- IO 线程 + CinemaWait；`runOnUiThread` 前检查 cancel/finishing。✓
- 回退只用 `EmbyClient.episodes` 单页（非 episodesAll）——见 L3。
- Season 的 `emby.series` 在 meta 里通常未写（EmbyApi 只给 Episode 写）——见 L4。

### DetailLandscape / PlayerOverlay
- 横屏左右分栏；`screen()` 调 `unsplit` 还原。✓
- 「更多」合并两个外部播放入口为 `external()`（磁力走原版 external，直链 `externalReplay`）；副标题用 `CinemaMenu.detail`。✓
- 进度条颜色改走 PlayerTheme token，不影响逻辑。✓

### 音效（0.1.134）
- `AKG` / `ANIME` 经 `normalize` / `curve` / `label`；VLC `AudioEffects.apply` 与 Media3 `Media3Sound.tick` 都走 `curve()`，旧 dialog/night/preset/off 行为不变。✓
- 测试校验削波保护（段增益+前级 ≤1.5dB）。✓
- 不改默认音效键；用户未选手动仍为 off。✓

### 安全
- 无新 token/密钥；备份/Emby 路径未改；字体附 OFL。✓

## 问题（按优先级）
无 H / M。

L1 低｜产品｜外观选项收成「跟随系统 / 暖白陶土 / 影院暗金」。曾选「深青色」或从未写过 `cinemaAppearance`、仅靠默认 `dark` 跑的用户，升级后会变成 gold 或跟随系统（clay/gold），视觉会变；设置里无法再选深青色。属本版有意折叠，不是静默强推聚合首页。

L2 低｜体积 / 首启｜`Typeface.createFromAsset` 在主线程首次加载约 11MB OTF，冷启首页标题时可能有一次短顿；之后缓存。APK 约大 10MB。可后续预热或换子集字体。

L3 低｜EmbyPlayback.playSmart｜NextUp 空时只扫 `episodes` 第一页找未看；该页若全已看则退回该页第一集（可能已看完）。大剧集更依赖 NextUp。可改 `episodesAll` 或分页，但在后台且有取消，不 ANR。

L4 低｜Season 一键播｜`emby.series` 仅 Episode meta 有；首页若出现 Season 卡，可能找不到剧 ID 而退回详情（安全降级）。

L5 低｜DetailLandscape.unsplit｜还原 padding 时左右都用 `getPaddingRight()`；分栏时左曾被设为 8dp，一般对称无感，极端 insets 下可能略偏。

L6 低｜跟随系统｜换系统深浅色后需 recreate / 重进才刷新（打开时算 `night()`），符合常见做法。

## 其余已核对没问题
- 音效菜单与 normalize 对称（VLC + Media3）。
- PlayerTheme 播放页仍深色面板；纸面仅非播放 Activity 的菜单。
- EmbyLibrary / FusionLibrary `screen`→`unsplit`；详情 `overlay` 防重复分栏（`mp-split` tag）。
- verify 期望 versionCode 135 / versionName 0.1.134；build 产物文件名 `MediaPocket-0.1.134-sound-presets-arm64.apk`。
