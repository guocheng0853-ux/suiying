# 随影 main `19f135b`（0.1.148）→ `dd66906`（0.1.155 + release gate）审查
只审，不打包、不合并（已在 main）、不推送。对照范围用 cursor-github `compare_commits` / `list_commits` / `get_file_contents`；本机用已有仓库 worktree 检出 tip 编译并跑全量 JVM（未 clone 新仓）。
测试：tip 上 **96/96 JVM 单测 PASS**；未跑完整 `build_ui` / `verify_apk` / 签名包（用户要求只审不打包）。
0.1.130 硬约束与 0.1.136 `embyKeep`：tip AggregateHome 仍有 `MERGE=agg-merge`、首画 300ms / 再画 500ms、`painted` 内容签名；AggregateRules 仍有 `embyKeep` / `weakerEmbyLatest`；CacheFiles / BifCache / EmbyStore.mergeAuth / FadeFrame 仍在且本范围 diff **未改**这些文件。✓
结论：**有条件通过（已在 main）**。无 High；**1 条 Medium**（提前断流修复依赖状态文案关键字）；若干 Low。版本号一致；release gate 已补上曾漏打的 PatchPlaybackBackend 断言。

## 版本号
| 位置 | 值 |
|------|-----|
| tip SHA | `dd669061d0fb8ec6ba91723aae483b1fe336ae9e` |
| base（已审合入 0.1.148） | `19f135bc…`（PR #31 squash） |
| ahead / behind | **17 / 0**（GitHub compare；含 merge 拓扑上的 0148 原提交，见下） |
| AndroidManifest | versionName **0.1.155** / versionCode **156** ✓ |
| build_ui.py 产出名 | `MediaPocket-0.1.155-playback-fixes-arm64.apk` ✓ |
| verify_apk.py 断言 | code 156 / name 0.1.155；并断言 **7 个 PATCH_MARKS** ✓ |
| cinema-ui/VALIDATION.json | ui_version **0.1.155** / main_version_code **156** / jvm_test_programs_passed **96** ✓ |
| cinema-ui/README.md | 0.1.149–0.1.155 节已写 ✓ |

说明：`compare_commits(19f135b…main)` 的 commits 列表会带上 squash 前的 `20da562/e4710f8/71c3555` 以及 `6afca3b` merge（分支并入拓扑）。**净文件 diff 不含** 0.1.148 的 PlaybackBackend / EmbyPlayback / PlayerPanelBack 再改；本审查按净变更覆盖 **0.1.149–0.1.155 + VALIDATION/release-gate**。

## 提交（用户关心的版本序列，北京时间）
1. `c3266ca` 0.1.149 — Fix split window orientation and resize（约 11:19）
2. `542b8e1` 0.1.150 — progressive source pick and keep detail page after playback
3. `8799458` 0.1.150 — buffered segment on the VLC seek bar for P2P
4. `994c105` 0.1.151 — player P5（正中只留播/停，控件收敛）
5. `6fb58a4` 0.1.152 — smart fill ~2.2:1，三种比例模式
6. `549e5b0` 0.1.153 — aspect button always changes the picture
7. `097e417` 0.1.154 — learn VLC video size from track
8. `768a403` Record user device confirmation for 0.1.154 aspect fix
9. `faeb394` 0.1.155 — five playback fixes from OnePlus 13 debug log（约 10/27 00:50）
10. `8377948` / `bf92824` / `7360f8a` — VALIDATION：Windows 重建、补回 PatchPlaybackBackend、PJZ110/OPD2401 真机记录
11. `dd66906` Harden release gate：verify_apk 断言全部 7 个补丁标记；提交便携 `build_win.py`

## 范围摘要
48 文件（约 +774 / −93）。**以播放器 UX / 分屏方向 / 选源 / Emby 恢复与收尾为主**，另有验收文档与构建门禁；不动聚合首页核心文件。

主改：
- **0.1.149 PlaybackOrientation**：分屏 `windowRequest→UNSPECIFIED`，避免传感器横屏触发 ColorOS 全屏兼容面；Media3 同步 enter/windowChanged。
- **0.1.150 DetailChrome + SourceCardRules**：插件并行（上限 8）渐进推荐；DV-only（`hdr=="DV"`，不含 `DV+HDR`）降权；播完回详情 `HomeRules.keepOnResume(home,detail,…)`。
- **0.1.150 BufferRules + PlayerOverlay.buffer**：P2P 连续分块 → SeekBar secondary；1s 节流、MAX_SCAN=4096。
- **0.1.151 P5**：`PlayerGlassRules.SKIP_KEYS=false`；顶栏/底栏/侧栏收敛；Media3 对齐；倍速 1.0× 用 QUIET_TEXT。
- **0.1.152–0.1.154 FullscreenRules/Mode**：三模式 smart/fit/crop；智能满屏裁 12% + 拉 10%；`visibleNext` 跳过同观感；`learnSize` 从视频轨补 vw/vh（SAR）。
- **0.1.155 PlaybackEndRules + PlaybackRepair**：提前断流、无 Vout 出画面、PGS 交 VLC、Emby 路径脱敏、解码档按次还原 + 升级清 sticky 软解；EmbyHome 5xx 冷却 6h；EmbyAutoRecovery 打断只退一次额度。
- **dd66906**：`PATCH_MARKS` 七项（含 PatchPlaybackBackend）；便携 `build_win.py`。

## 播放路径核对

| # | 要求 / 版本项 | 结论 | 证据 |
|---|---------------|------|------|
| 1 | 0.1.149 分屏方向不锁传感器横屏 | **落地** | `windowRequest(multi,desired)`；Media3 `onMultiWindowModeChanged` |
| 2 | 0.1.150 渐进选源 + 播完留详情 | **落地** | CompletionService；`replaceAuto`；`keepOnResume(...,detailPage,...)` |
| 3 | 0.1.150 P2P 进度条缓存段 | **落地** | BufferRules + `seek.setSecondaryProgress`；直链无 infoHash 时 secondary=progress |
| 4 | 0.1.151 P5 正中只留播/停 | **落地** | SKIP_KEYS；±15 键 GONE；截图/解码进「更多」 |
| 5 | 0.1.152–154 智能满屏 / 三模式 / 每点有变化 / 轨尺寸 | **落地** | smart/visibleNext/learnSize；用户 PJZ110 确认记入 README/VALIDATION |
| 6 | 0.1.155 提前断流 ≠ 播完 | **落地** | premature + AutoNext `!PlaybackRepair.premature`；文案含「失败」驱动 failure 卡 |
| 7 | 0.1.155 无 Vout 仍可出画面 | **落地** | `playingWithoutVout` → 写 `outputReady` |
| 8 | 0.1.155 PGS 交 VLC | **落地** | `imageSubtitle` + `imageToPlayer`（最多等 10s） |
| 9 | 0.1.155 Emby 5xx 冷却 / 打断退一次额度 / 解码按次 | **落地** | BROKEN map；RESUMED；DECODE_BEFORE + CinemaApp migrate |

## 硬约束核对
- agg-merge 单线程、300ms 首画合并、`painted` 内容签名：tip AggregateHome 仍在；diff 未改。✓
- CacheFiles / BifCache / EmbyStore（含 mergeAuth）/ FadeFrame：tip 仍在；不在本 diff。✓
- 0.1.136 `embyKeep` / `weakerEmbyLatest`：tip AggregateRules 仍在。✓

## 安全 / 崩溃 / ANR
- 无新增明文凭证或跨站 Cookie；`allowBackup=false` 仍在。✓
- Emby 错误日志用 `pathPattern` 去 query、打码长 id/数字段，不带服务器地址/令牌。✓
- usesCleartextTraffic=true 为既有（本范围未新开）。
- PlayerChrome 250ms 主线程轮询新增 learnSize / smartTick / PlaybackRepair / buffer（见 L2）；未见新的主线程网络。
- EmbyHome.BROKEN / DetailChrome 线程池 / WeakHashMap 用法合理；并发未改 MERGE 写路径。✓
- VALIDATION 自述：Windows 链曾漏 PatchPlaybackBackend，tip 已重建并加门禁。✓

## 问题（按优先级）

无 High。

### M1 中｜提前断流修复依赖状态文案关键字
`PlaybackRepair.prematureEnd` 仅在 `state` TextView 文案以「本集播放完毕」开头时介入，再把文案改成含「失败」的句子；`PlaybackReviewRules.failure` 在已出画面（`rendered`）时必须靠「失败/超时/…」才把 terminal 当成失败，从而弹出失败卡并走 Emby 自动恢复。`AutoNext` 则显式读 `PlaybackRepair.premature`。  
**影响**：底座若改掉「本集播放完毕」或 failure 关键字规则，提前断流会重新变成「播完 + 清进度 + 弹下一集」。  
**建议**：可接受为当前 overlay 策略（有单测与真机记录）；后续若动底座状态机，应改成显式标志位而非字符串。不阻塞「已在 main」结论。

### L1 低｜VALIDATION 机器可读标志与叙述不完全一致
tip `android_device_tested: true`、`phone_tested_this_release: true`，但 `real_source_playback_tested: false`、`dolby_android_device_tested: false`；`device_acceptance_record` 写明 0.1.155 尚无正式验收表（最近正式为 0.1.149）。`verify_apk.py` 跑过后仍会把 `android_device_tested` 写回 `False`。文档可信，矩阵仍偏弱。

### L2 低｜主线程 250ms 叠加反射 / JNI
`PlayerChrome.poll` 每拍调用 `learnSize`、`smartTick`、`PlaybackRepair.tick`，控件刷新里还有 P2P `havePiece` 扫描（已 1s 节流 + MAX_SCAN）。与既有 chrome 轮询同风格；低端机偶发卡顿风险，未见必然 ANR 路径。

### L3 低｜`vlcRisky` 仅降权纯 `DV`
与 StreamInfo 的 `DV` vs `DV+HDR` 一致，符合 README「无 HDR10 兜底」；若标题误标导致 hdr 解析偏差，排序可能过抑或漏抑。有意近似。

### L4 低｜Emby 5xx 冷却返回空结果
冷却期内该接口直接 `new JSONObject()`，该库「最新」暂时为空；依赖聚合侧 `embyKeep` 减轻刷空。进程内 Map，重启后重试。可接受。

### L5 低｜解码按次还原的崩溃边界
`applyDecode` 立即写入 prefs；`restoreDecode` 在 `PlayerChrome.dispose`。若进程在播放中被杀，可能留下本次软解直到用户再改；升级迁移只清 `decodeSession.v1` 之前的 sticky 软解。

### L6 低｜构建链漂移已被门禁接住（过程债）
0.1.148–0.1.155 间 Windows `build_win.py` 曾漏 PatchPlaybackBackend；`dd66906` 用 7 个 PATCH_MARKS 断言防再发。审查范围内属已关闭问题，记一笔提醒以后改 build 脚本必跑 verify。

### L7 低｜其它琐碎
- `FullscreenMode.SMART_LOGGED` / `PlaybackRepair.HANDLED` 等 WeakHashMap 正常。
- Media3 顶栏本就无 PiP 入口（P5 只动了 DV 字样与锁位置），非回归。
- 未知时长短片：`premature` 仅在 &lt;2 分钟结束时判提前结束，超 2 分钟无 runtime 的断流仍可能当播完（与注释一致）。

## 测试
- 本机 worktree `dd66906`：ecj 编译 stubs+src，**96/96** `tests/*Test.java` **PASS**（含新增 BufferRulesTest / PlaybackEndRulesTest，以及更新后的 FullscreenRulesTest、SourceCardRulesTest、PlayerGlassTest、PlaybackOrientationRulesTest、HomeRulesTest 等）。与 VALIDATION `jvm_test_programs_passed: 96` 一致。
- 未执行完整 `build_ui.py`（d8/补丁打 APK）、未跑 `verify_apk.py`、未打签名包。
- 真机：以 tip VALIDATION / README 为准——0.1.154 比例 PJZ110 确认；0.1.155 Windows 重建后 PJZ110 Stremio+Emby、OPD2401 首页+实播有记录；正式 acceptance 文档仍停在 0.1.149。

## 给父代理 / 用户的摘要
1. **结论：有条件通过（已在 main）。无 High；1 Medium（提前断流靠状态文案关键字）；7 Low。**
2. 硬约束与 embyKeep 完好；播放路径 0.1.149–0.1.155 项在代码侧对齐 README。
3. 本机 **96/96 JVM PASS**；按用户要求 **不打包、不推送**。
4. 审查全文：`/workspace/handoff/review-0155.md`。
