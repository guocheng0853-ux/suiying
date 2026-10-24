# 随影 0.1.137–0.1.139 审查 —— 对比 60ef75e（0.1.136）→ bd6dc0f（main tip）
只审，不打包、不合并、不上传。工作树 /workspace/wt-review-0139。
测试：JVM **87/87** 通过（相对 0.1.136 的 86 新增 PlayerGlassTest）；`tests/test_media3_offline.py` 通过。未跑 `build_ui` / 签名包（用户要求先只审）。
0.1.130 硬约束与 0.1.136 `embyKeep`：本区间 diff **不含** AggregateHome / AggregateRules / CacheFiles / BifCache / EmbyStore / FadeFrame / Offline*；main tip 上 `MERGE=agg-merge`、300ms 首画、content-signature、`embyKeep` / `weakerEmbyLatest` 仍在。✓
结论：**通过**。无 High / Medium 阻塞项。文档版本有轻微漂移（见 L1）；0.1.139 播放器改版未真机验证（见 L6）。

## 版本号
| 位置 | 值 |
|------|-----|
| tip SHA | `bd6dc0f2c769f391c0215e179d01cb06f299e4e0` |
| AndroidManifest | versionName **0.1.139** / versionCode **140** ✓ |
| build_ui.py 产出名 / 写入 VALIDATION | 0.1.139 ✓ |
| verify_apk.py 断言 | code 140 / name 0.1.139 ✓ |
| cinema-ui/VALIDATION.json（仓库内） | ui_version **0.1.138**（构建时会被覆盖） |
| 根 README.md | 仍写「0.1.138 / versionCode 139」 |

打包侧三处（Manifest / build_ui / verify）已对齐 0.1.139/140；根 README 与已提交 VALIDATION 落后一版 → L1。

## 范围摘要
7 个提交（含 4 条 0.1.136 真机/README 记录）、18 文件（+269 / −77）。代码主线三版：

1. **0.1.137** `852f7e8`：横屏详情左栏封面出血到屏幕左缘（含挖孔），右侧圆角；`DetailLandscape` 左 padding 改为 0。真机验证见 DEVICE-AGG-VALIDATION.md。
2. **0.1.138** `1b0ab07`：Episode 写入 `emby.seriesPoster`（系列 Primary、无 tag）；继续观看卡 / 分类格 / 首页大图走多级 fallback；`TvPosterRules.candidates` 对 `/Images/` URL 去 `tag=` 再试。真机：4/5 报错 → 0。
3. **0.1.139** `bd6dc0f`：播放器「三层悬浮面板」——`PlayerGlass` / `PlayerGlassRules`；Media3 与 VLC（PlayerOverlay）顶栏玻璃胶囊、正中 −N/播放/+N、底部圆角进度面板 +「剩 X 分钟，HH:MM 看完」。未真机验证。

## 硬约束核对
- agg-merge 单线程、300ms 首画合并、painted 内容签名：未改 AggregateHome。✓
- CacheFiles / mp-json-v1、BifCache、EmbyStore 深拷贝与 mergeAuth、FadeFrame：不在 diff。✓
- 0.1.136 `embyKeep`（空/弱 latest 不写盘、keepRows）：仍在 AggregateRules / AggregateHome。✓

## 分项核对

### 0.1.137 横屏封面出血
- `split` / `balance`：左栏 `setPadding(0, …)`，封面 `cornerRadii` 仅右上/右下圆角。✓
- 左栏宽度仍按 `contentL` 参与 `leftColumn` 计算，视觉上封面伸入 digout 区，属有意设计；提交注明真机验证。✓
- 未改安全区计算本身；顶栏/正文仍用 content inset。✓

### 0.1.138 海报兜底链
- `EmbyApi` Episode：`emby.seriesPoster=imageUrl(..., seriesId, "Primary", …, tag="")`，无 tag，避免抽帧 500。✓
- `TvPosterImages.chain`：FALLBACK 改为 `String[][]`，失败取下一备用；同 URL 去重；链耗尽停止——**无死循环**。✓
- `candidates` 去 tag：只动 query 中 `tag=`，保留其它参数；非法 header 仍在 `parse` 拒绝。✓
- HomeChrome：继续观看优先单集图 → background → seriesPoster；大图 background → poster → seriesPoster。✓
- 失败回调一律 `loadPoster`（忽略已存的 poster 标志位）→ 大图兜底会走 RGB_565，见 L2。

### 0.1.139 三层悬浮面板
- Media3：顶栏 dress circle/pill；正中 `PlayerGlass.center`；底栏玻璃圆角 + `remain`；拖进度时收起正中键；`paintPlay` 用 WeakHashMap 去重。✓
- VLC PlayerOverlay：`glassCenter` + `OnGlobalLayoutListener` 与顶栏同显隐；锁屏 / PiP / 侧栏 / 拖进度时隐藏；底栏 `mp-play` 强制 GONE（播放改在正中）；`PlayerGlass.panel` 包底栏。✓
- `hideCenter` / `PlayerRedesign.hideSeekButtons` 仍只动 Activity 反射字段 `center`（旧底座中心），**不动** `mp-center` tag。✓
- `PlayerGlassRules.remaining`：时长未知/越界空串；&lt;1 分钟「马上看完」；按倍速折算；向上取整分钟；PlayerGlassTest 覆盖。✓
- 无新明文密钥；触摸控件仍为 Button（底座只交出 Button/SeekBar 点击）。✓
- GlobalLayoutListener 挂上后未显式 remove → L3；`layout()` 不建正中键、靠 refresh → L4。

## 安全 / 崩溃 / ANR
- 无新增 token、密码、跨站 Cookie 行为；图片 header 白名单未放宽。✓
- 主线程：海报失败链式切换仍在 MAIN；剩余时间字符串相等才 `setText`；dress/paintPlay 有缓存。未见新的主线程网络/重 JSON。✓
- Android 15 `onTimeout`、OfflineHttp 等不在本 diff。✓
- NPE：`optString` / 空链短路；`center==null` 守卫。✓

## 问题（按优先级）

无 High / Medium。

### L1 低｜文档版本未跟到 0.1.139
根 README 仍写 0.1.138/code 139；仓库内 `VALIDATION.json` 的 `ui_version` 为 0.1.138（且字段被裁成构建前最小集）。Manifest / build_ui / verify 已是 0.1.139/140。打包前建议改 README；VALIDATION 会在正式构建时重写。

### L2 低｜海报链失败回调忽略 poster 标志
`complete` 失败时固定 `loadPoster(...)`，不读 `fb[0][2]`。首页大图（`load` / ARGB）失败后兜到海报时会按 RGB_565 解码，可能轻微色带，不致崩溃。旧单备用路径会保留标志。

### L3 低｜PlayerOverlay GlobalLayoutListener 未移除
按 Activity 只挂一次（`CENTER_HOOKED` WeakHashMap）；随根 View 销毁。配置变更/异常路径下略粗糙，未见强泄漏必现路径。

### L4 低｜VLC 正中键依赖 refresh
`layout()` 不调 `glassCenter`，首帧到第一次 `refresh` 前可能没有 −N/播放/+N。Media3 构造时已创建，不受影响。

### L5 低｜跳秒偏好变更后正中键文案可能旧
`PlayerGlass.center` 复用已有 `mp-center` 行，不重建 `−{seconds}` 标签；改「双击跳秒」后需重建播放页才更新。

### L6 低｜0.1.139 未真机验证
0.1.137 / 0.1.138 有 DEVICE-AGG-VALIDATION 记录；0.1.139 播放器改版（Media3+VLC 双路径、触摸/锁屏/PiP/字幕上移）建议装包后重点点验。

### L7 低｜其它琐碎
- Media3Controls 字段 `paintedPlay` 已无读写。
- `chain` 内未使用的 `pastFirst` 变量。
- cinema-ui/README 0.1.139 节仍写「单独分支 design/player-glass」，实际已在 main。

## 测试
- `sh scripts/run_tests.sh`：**87/87**（工作树先 ecj 编译 `build/classes`，未打 APK）。
- `python3 tests/test_media3_offline.py`：PASS。

## 相对 0.1.136 的主要风险摘要（给打包前）
1. 0.1.139 双播放器 UI 大改，真机未测：正中键显隐、锁屏、PiP、拖进度、字幕避让。
2. 海报多级兜底在共享 Emby 上已验证；其它图源/大图 RGB_565 兜底观感未扫全。
3. 横屏出血依赖 cutout inset；异形屏/折叠外屏建议再瞄一眼详情页。
4. 文档版本号落后，不影响 APK badging，但发版说明易混。
5. 硬约束与 embyKeep 未被动到，聚合首页回归风险低。
