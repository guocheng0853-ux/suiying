# 随影 PR #33 `cursor/review-0155-l2-l6-2183` → main（0.1.157 / code 158）审查
只审，不合入、不打包。对照用 cursor-github `get_pull_request` / `list_pull_request_files` / `get_pull_request_diff` / `get_file_contents`；本机浅取 tip 到 `/workspace/suiying-pr33`，用 `/workspace/wt-build-0156`（base `e1d263b`）的 stubs/classpath 重编译 tip 改动类，跑相关 JVM 单测（未 clone 新仓、未跑完整 `run_tests.sh` / `build_ui` / 签名包）。
测试：相关 JVM **7/7 PASS**；作者自报全量 **96/96**；仓库 tip 仍有 **96** 个 `*Test.java`。
0.1.130 硬约束与 0.1.136 `embyKeep`：本 PR **24 文件**均不涉及 AggregateHome / AggregateRules / CacheFiles / BifCache / EmbyStore / FadeFrame；tip 与 base `e1d263b` 这 7 个文件 **sha256 相同**。✓
结论：**有条件通过（建议合 main）**。无 High；无 Medium；若干 Low。版本号一致；`PATCH_MARKS`（含 PatchPlaybackBackend）未被动坏。0155 的 L2–L6 与 0156 的 PREMATURE 清理按设计落地。

## 版本号
| 位置 | 值 |
|------|-----|
| PR | [#33](https://github.com/guocheng0853-ux/suiying/pull/33) open，mergeable clean |
| head SHA | `957afa72682e9fb0a8b4a7f911362e64cae17162` |
| base | `e1d263b0…`（0.1.156 / PR #32 squash） |
| commits | **1**（`957afa7` 0.1.157: ease the player poll and keep per-play decode from sticking） |
| 净变更 | **24 文件，+135 / −41** |
| AndroidManifest | versionName **0.1.157** / versionCode **158** ✓ |
| build_ui.py / build_win.py 产出名 | `MediaPocket-0.1.157-playback-fixes-arm64.apk` ✓ |
| verify_apk.py 断言 | code **158** / name **0.1.157**；仍断言 **7 个 PATCH_MARKS**（含 PatchPlaybackBackend）✓ |
| cinema-ui/VALIDATION.json | ui_version **0.1.157** / main_version_code **158** / jvm_test_programs_passed 仍跟 stamp；新增 `source_rank_rule` / `build_verify_note` ✓ |
| cinema-ui/README.md | 0.1.157 节已写 ✓ |
| signed_apk_sha256 | 仍为 **0.1.155** 包（本版未打包，与 PR 说明一致） |

## 范围摘要
针对 review-0155 **L2–L6**，并顺手收 **0156 L2**（`PREMATURE` 在 attempt 变化时清掉）。**0156 L3（Ended→Stopped）有意未改**。

| 0155 项 | 本版做法 | 结论 |
|---------|----------|------|
| L2 主线程 250ms | 脉冲仍 `postDelayed(this,250)`；`surfaceDirty` 跳过无变化写 View；`havePiece` 改 IO 线程（1s）；`markRendered` 优先复用 `vw`；libVLC 读仍在主线程 | **部分落地**（有意不迁 VLC） |
| L3 纯 DV 降权 | `releaseText`=文件名+标题首行；Profile 5 强制降权；Profile 7+→DV+HDR；DVD/DVB 不误判；自动选中风险源打日志 | **落地** |
| L4 Emby 5xx 空结果 | `latestOrKept` + `EmbyCue.cachedLatest`；有缓存交 `Items`，无缓存仍空；`embyKeep` 未改 | **落地** |
| L5 解码按次边界 | `decodeSession.open` + prev；冷启动 `unwindDecodeSession`；转屏 `rehydrateDecode` 不还原 | **落地** |
| L6 构建链漂移 | 已关闭；README/VALIDATION/`build_*` 注释提醒改脚本必跑 verify；补丁链未改 | **文档确认** |
| 0156 L2 PREMATURE | `dropPremature` + `ATTEMPT` map，attempt/id 变则 `PREMATURE.remove` | **落地** |

## 硬约束核对
- tip AggregateHome 仍有 `MERGE=agg-merge`、首画 300ms / 再画 500ms、`painted` 内容签名；与 base 同哈希。✓
- AggregateRules `embyKeep` / `weakerEmbyLatest` 未改；EmbyFeaturesTest 覆盖「last-good 可写盘」与「真·空 latest 仍 keepRows」。✓
- CacheFiles / BifCache / EmbyStore（含 `mergeAuth`）/ FadeFrame：tip 与 base 同哈希，不在 diff。✓
- `verify_apk.py` 七项 `PATCH_MARKS` 保留；版本断言升到 158/0.1.157。✓

## 安全 / 崩溃 / ANR
- 无新增凭证、权限、明文密钥；无新的主线程网络。✓
- Emby 冷却/5xx 走后台 `FETCH`；缓存读失败回空对象。✓
- P2P `havePiece` 移出主线程，降低 250ms 拍上的卡顿风险；句柄失效时吞异常、进度条不动（作者已声明）。✓
- `PlaybackRepair` / `buffer` 仍吞 Throwable；`BUFFER_BUSY` 在外层 catch / UI 回调里清掉，避免永久卡住。✓
- 解码会话标记写 SharedPreferences；冷启动还原失败路径吞 RuntimeException（与既有 CinemaApp 风格一致）。✓

## 问题（按优先级）

无 High。无 Medium。

### L1 低｜本版未真机；VALIDATION 继承 0.1.155 机器标志
`android_device_tested` / `real_source_playback_tested` 仍为 true，但 `phone_tested_this_release=false`，`validation_flags_note` / `device_test_scope` 写明 0.1.156/0.1.157 未再测。比「矩阵撒谎」诚实。合入后建议真机点：普通播完、提前断流、换源、手动软解后强杀再开、Emby 库冷却场景。

### L2 低｜主线程仍读 libVLC（有意保留）
`learnSize` / `getPlayerState` / `isPlaying` / 未知 `vw` 时的 `getCurrentVideoTrack` 仍在 250ms 拍上。作者说明迁线程更易弄坏 VLC；缓解项（surfaceDirty、havePiece 下沉、vw 复用）合理。低端机偶发卡顿风险未彻底消除。

### L3 低｜Ended→Stopped 窗口仍可能漏判提前断流
0156 L3 有意未改：底座若在 EndReached 后很快变成 Stopped(5)，250ms 轮询可能看不到 Ended。把 Stopped+terminal 纳入会误伤手动停止。残留行为与 0.1.156 相同；真机仍应验一次提前断流。

### L4 低｜标题只写 `DV HDR`、未写 Profile 5 的真·P5 不降权
作者 residual：Profile 5 文件若只标 `DV HDR`（无 “Profile 5”）会当成 DV+HDR。解析只能靠文字；可接受近似。显式 Profile 5 / 纯 DV / 插件名干扰 有单测。

### L5 低｜`build_ui`/`build_win` 仍先把 VALIDATION 写成最小集
与 0156 L5 相同：verify 保留已记录真机键，但打包脚本仍覆写短 JSON。本版未打包、未改覆盖策略；注释已强调改脚本必跑 verify。不阻塞。

### L6 低｜后台分块扫描期间跳过刷新；句柄并发
`BUFFER_BUSY` 为真时直接 return，扫描超过约 1s 时进度条 secondary 会晚一拍。`TorrentHandle.havePiece` 跨线程属常规状态查询；撕毁中扫描失败则保持原值。可接受。

### L7 低｜琐碎
- 渐进选源每次刷新到「仍风险」的 best 都会打一行调试日志，可能偏吵，非功能问题。
- `releaseText` 为空时回退整段 `text()`（含插件名）——无文件名/标题时行为与旧版接近。
- `surfaceDirty` 用 float `!=`（与旧 `getScaleX()!=x` 同风格）。

## 测试
- 本机 `/workspace/suiying-pr33` @ `957afa7`：重编译 tip 改动类后跑 **BufferRulesTest / EmbyFeaturesTest / FullscreenRulesTest / PlaybackEndRulesTest / SourceCardRulesTest / StreamInfoTest / PlaybackReviewRulesTest → 7/7 PASS**（含 surfaceDirty、havePiece 下沉源码扫描、cachedLatest、embyKeep 形状、unwindDecodeSession、dropPremature、Profile 5/8、插件名干扰等）。
- 作者：`scripts/run_tests.sh` **96/96**；tip 仍 **96** 个 `*Test.java`。本环境未跑全量（未执行 `build_ui` 生成完整 stamp 路径）。
- 未跑 `verify_apk.py`（无 0.1.157 APK）、未打签名包。
- 真机：本版无；继承 0.1.155 叙述。

## 给父代理 / 用户的摘要
1. **结论：有条件通过，建议合 main。无 High/Medium；7 Low（未真机、VLC 仍主线程、Ended 窗口、P5 文案近似、VALIDATION 打包覆写、buffer 忙锁、日志噪音）。0155 L2–L6 与 0156 PREMATURE 清理已落地；Stopped-as-Ended 有意未动。**
2. 硬约束与 7 个 PATCH_MARKS 完好；版本号 0.1.157/158 一致。
3. 相关 JVM **7/7 PASS**；作者 96/96。按要求 **不合、不打包**。
4. 审查全文：`/workspace/handoff/review-0157.md`。
