# 随影 PR #28 / 0.1.140 审查 —— 对比 bd6dc0f（0.1.139）→ f3c199a
只审，不打包、不合并、不上传。工作树 /workspace/wt-review-0140（detached `origin/cursor/player-glass-refine-0b2d`）。
测试：JVM **87/87** 通过（含扩充后的 PlayerGlassTest）；`tests/test_media3_offline.py` 通过。未跑 `build_ui` / 签名包（用户要求先只审）。
0.1.130 硬约束与 0.1.136 `embyKeep`：本 PR diff **不含** AggregateHome / AggregateRules / CacheFiles / BifCache / EmbyStore / FadeFrame；tip 上 `MERGE=agg-merge`、300ms 首画、`painted` 内容签名、`embyKeep` / `weakerEmbyLatest`、`mp-json-v1`、`mergeAuth` 仍在。✓
结论：**通过**。无 High / Medium 阻塞项。设计 5 条均已落地（亮场对比以「恒按最坏亮场加厚」保守实现，见 L1）；未真机验证（L5）。

## 版本号
| 位置 | 值 |
|------|-----|
| PR head SHA | `f3c199ae6dba9dfe0813f7ba58b9a87b86a0875f` |
| base (main) | `bd6dc0f2c769f391c0215e179d01cb06f299e4e0` |
| AndroidManifest | versionName **0.1.140** / versionCode **141** ✓ |
| build_ui.py 产出名 / VALIDATION 写入 | 0.1.140 ✓ |
| verify_apk.py 断言 | code 141 / name 0.1.140 ✓ |
| cinema-ui/VALIDATION.json | ui_version **0.1.140** ✓ |
| 根 README.md | 0.1.140 / code 141 ✓ |

## 范围摘要
1 个提交、16 文件（+211 / −87）。纯播放器 UI 精修，不动聚合。

主改：`PlayerGlassRules`（对比/48dp/剩余句/底栏取舍/暗金）、`PlayerGlass`（flash ±N、加厚色）、`Media3Controls` / `PlayerOverlay`（底栏蒸馏 + quieter 中岛）、`PlayerTheme` / `PlayerSidePanel`（gold 强调色）、`PlayerLayoutMetrics` / `PlayerMotionRules`（TOUCH/TOP/TIME/MENU → 48）、测试与版本号。

## 设计 5 条落地核对

| # | 要求 | 结论 | 证据 |
|---|------|------|------|
| 1 | 亮场玻璃动态加厚，白字 ≥4.5:1 | **落地（保守）** | `glassAlpha` / `panelAlpha` / `contrast` 按 WCAG 相对亮度算最小 α；亮场 ≥`STRONG_ALPHA`（0xB3）。运行时 `chipColor`/`panelColor` **固定 sceneLuma=255**（最坏白雪），故胶囊/底栏恒加厚，暗场透气路径在公式里存在但未接帧亮度采样。白字在 α≥152、剩余句/暗金在 panelα 下对比均 ≥4.5（测试覆盖）。`topFade` → `0x99`。 |
| 2 | 触控热区 ≥48dp | **落地** | `PlayerGlassRules.TOUCH_DP=48`；`PlayerLayoutMetrics.TOUCH/TOP_H/TIME_ROW`、`PlayerMotionRules.MENU_ROW_DP`、侧栏 chip/row、Media3 `glassLp`/seek 行、VLC seek `minimumHeight` 均 ≥48。正中跳秒圆视觉 56、播放盘 76（≥48）。 |
| 3 | 正中 quieter：默认只播放盘，±N 短暂显示 | **落地** | `SEEK_FLASH_MS=1600`；`flashSeek` / `applySeek`；Media3 `reveal` 开控时 flash，`update` 按窗口收起；拖进度整岛 GONE；VLC `syncCenter` 由隐→显时 flash，`seekBy`/双击跳秒再 flash。 |
| 4 | 底栏蒸馏 + 剩余句缩短 | **落地** | Media3 底栏仅上一集/下一集/倍速；选集·音轨·字幕进 `more()`。VLC 新建 `mp-transport` 不再加 `mp-slot-*`；`more()` 顶部补选集/音轨/字幕。剩余句：`剩 29 分，22:41 看完`，整点小时不写「0 分」，「马上看完」保留。`showsOnBar`/`tuckedInMore` 有单测；生产路径为硬编码同语义。 |
| 5 | gold `#e0b25c` + Media3/VLC 底栏语言统一 | **落地** | `playbackAccent(true)=0xffe0b25c`；Media3/VLC 进度与倍速、侧栏选中色走 `PlayerTheme.playbackAccent` / `rowFill(...,gold)`。两边底栏均为图标（上/下集）+ 倍速短标签（`tnum`），不再一边长中文一边图标。 |

## 硬约束核对
- agg-merge 单线程、300ms 首画合并、`painted` 内容签名：未改 AggregateHome。✓
- CacheFiles / mp-json-v1、BifCache、EmbyStore 深拷贝与 mergeAuth、FadeFrame：不在 diff。✓
- 0.1.136 `embyKeep` / `weakerEmbyLatest`：仍在 AggregateRules / AggregateHome。✓

## 安全 / 崩溃 / ANR
- 无新增 token、密码、跨站 Cookie；无明文密钥。✓
- 主线程：`glassAlpha` 最多约百次循环且有 `STYLED`/`panel` key 缓存，未见新的主线程网络/重 JSON。✓
- NPE：`flashSeek`/`applySeek`/`seekVisible` 空守卫；WeakHashMap 持 View。✓
- 并发：未改 MERGE / Emby 缓存写路径。✓
- 触摸控件仍为 Button（底座只交出 Button/SeekBar）。✓

## 问题（按优先级）

无 High / Medium。

### L1 低｜亮场「动态」未接帧亮度，恒按白雪加厚
`PlayerGlassRules` 支持按 `sceneLuma` 在暗场保留 `AIRY`、亮场加厚；`PlayerGlass.chipColor`/`panelColor` 写死 `255`。对比目标满足，但暗场也会更「贴纸」、透气感弱于评审叙述。若以后要真动态，需从播放器取亮度再刷玻璃（注意主线程与重绘频率）。

### L2 低｜VLC 已存在的 `mp-transport` 不会拆掉旧 `mp-slot-*`
`bottom()` 仅在 `findViewWithTag("mp-transport")==null` 时新建无槽行。正常装包/进页会新建；同进程热替换旧布局的路径不存在。残留风险可忽略；若要防御可在 refresh 时 `GONE` 掉 `mp-slot-*`。

### L3 低｜`PlayerLayoutMetrics.measure` 仍为音轨/字幕/选集占位
度量/截图辅助仍算 `audio`/`cc`/`episodes` Box；真机底栏已不画这三颗。PlayerOverlayRulesTest 文案仍写「bottom icons map…」，测的是 `bottomAction` 顺序（现用于「更多」），易误导。

### L4 低｜cinema-ui/README 旧「0.1.139」叙述未改
文件顶部 0.1.140 节正确；下文仍写正中常驻三键、「剩 28 分钟」等 0.1.139 描述。根 README / Manifest / verify 已对齐 0.1.140。

### L5 低｜未真机 / 模拟器验证
无截屏。建议装包后重点：亮雪场景白字、开控后 ±N 1.6s 收起、底栏仅三键、更多里音轨字幕选集、gold 主题进度/倍速色、Media3 与 VLC 各走一遍锁屏/PiP/拖进度。

### L6 低｜其它琐碎
- `PlayerTheme.TRACK_PLAYED` / 无参 `rowText`/`chipFill` 仍钉暖陶；播放路径已改走 `playbackAccent`。
- 侧栏季节 chip 从 32dp 提到 48dp，横屏多季时行高变大（符合触控，略挤）。
- GlobalLayoutListener 仍未显式 remove（承 0.1.139 L3）。

## 测试
- `sh scripts/run_tests.sh`：**87/87**（工作树 ecj -17 重编本 PR 改动类 → `build/classes`，未打 APK）。
- `python3 tests/test_media3_offline.py`：PASS。

## 给合包前的摘要
1. 设计 5 条代码侧已兑现；亮场对比用恒加厚保守实现，可接受。
2. 硬约束与 embyKeep 未动，聚合回归风险低。
3. 播放器双引擎 UX 变更建议真机点验后再发用户包。
4. 无 H/M，**可以合并**；本次按用户要求停在审查。
