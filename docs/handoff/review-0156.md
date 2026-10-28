# 随影 PR #32 `cursor/premature-end-flag-8f8a` → main（0.1.156 / code 157）审查
只审，不合入、不打包。对照用 cursor-github `get_pull_request` / `list_pull_request_files` / `get_pull_request_diff` / `get_file_contents`；本机用已有仓库浅取 PR tip 到 `/workspace/suiying-pr32`，单独编译并跑 `PlaybackEndRulesTest` + `PlaybackReviewRulesTest`（未 clone 新仓、未跑完整 `run_tests.sh` / `build_ui` / 签名包）。
测试：相关 JVM **2/2 PASS**；作者自报全量 **96/96**；仓库仍有 **96** 个 `*Test.java`。
0.1.130 硬约束与 0.1.136 `embyKeep`：本 PR **13 文件**均不涉及 AggregateHome / AggregateRules / CacheFiles / BifCache / EmbyStore / FadeFrame。✓
结论：**有条件通过（建议合 main）**。无 High；无 Medium（0155 的 M1 已落地）；若干 Low。版本号一致；`PATCH_MARKS`（含 PatchPlaybackBackend）未被动坏。

## 版本号
| 位置 | 值 |
|------|-----|
| PR | [#32](https://github.com/guocheng0853-ux/suiying/pull/32) open，mergeable clean |
| head SHA | `4951551dedfece00f5aa697a1a33c8c07e4666b8` |
| base（已审 tip） | `dd669061…`（0.1.155 + release gate） |
| commits | **1**（`4951551` 0.1.156: mark premature EndReached with an explicit flag） |
| 净变更 | **13 文件，+82 / −31** |
| AndroidManifest | versionName **0.1.156** / versionCode **157** ✓ |
| build_ui.py / build_win.py 产出名 | `MediaPocket-0.1.156-playback-fixes-arm64.apk` ✓ |
| verify_apk.py 断言 | code **157** / name **0.1.156**；仍断言 **7 个 PATCH_MARKS**（含 PatchPlaybackBackend）✓ |
| cinema-ui/VALIDATION.json | ui_version **0.1.156** / main_version_code **157** / jvm_test_programs_passed **96** ✓ |
| cinema-ui/README.md | 0.1.156 节已写 ✓ |
| signed_apk_sha256 | 仍为 **0.1.155** 包（本版未打包，与 PR 说明一致） |

## 范围摘要
针对 review-0155 **M1**：提前断流控制信号从状态文案改为 **libvlc Ended + 显式 `PREMATURE` 标志**。顺手收 **L1**（VALIDATION 真机标志与 verify 不再强行抹掉已记录项）与 **L7**（全长未知 &lt;2 分钟注释/断言）。**L2–L6 按作者说明未改**。

主改：
- **PlaybackEndRules**：`VLC_ENDED=6`、`endReached`、`prematureFlag(terminal,state,pos,runtime,duration)`、`naturalFinish(terminal,premature)`；`prematureText` 仍给人看，但注释明确不再驱动失败卡。
- **PlaybackRepair.prematureEnd**：去掉 `state` TextView「本集播放完毕」前缀判断；改为 `getPlayerState()==Ended` 后写 `PREMATURE`、恢复续播、可选更新文案。
- **PlaybackReviewRules.failure(..., premature)**：`terminal&&premature` 直接失败；三参重载保持旧语义（`premature=false`）。
- **PlayerOverlay.status**：读 `PlaybackRepair.premature(a)` 传入 failure → `EmbyAutoRecovery.nativeTick`。
- **AutoNext**：`PlaybackEndRules.naturalFinish(terminal, PlaybackRepair.premature(a))`。
- **verify_apk.py**：打包后回写 VALIDATION 时保留已记录的真机相关键；杜比仍强制 false。
- **测试**：PlaybackEndRulesTest / PlaybackReviewRulesTest 覆盖标志、文案无关失败卡、naturalFinish，以及源码接线扫描（repair 无「本集播放完毕」、含 getPlayerState/prematureFlag 等）。

## M1 核对（核心）

| # | 要求 | 结论 | 证据 |
|---|------|------|------|
| 1 | 不再靠「本集播放完毕」触发 | **落地** | `PlaybackRepair` 无该字符串；改为 `endReached(playerState)`；单测源码扫描断言 |
| 2 | 不再靠文案含「失败」驱动失败卡 / Emby 恢复 | **落地** | `failure(..., premature)` 在 `terminal&&premature` 时直接 true；单测「本集播放完毕」+flag=true 为失败、flag=false 非失败；Overlay 把 flag 传入；`nativeTick(a,failure,...)` |
| 3 | AutoNext 不弹下一集 | **落地** | `naturalFinish(terminal,premature)` = `terminal&&!premature`；单测覆盖 |
| 4 | 时间阈值与 0.1.155 相同 | **落地** | `premature(pos,runtime,duration)` 未改阈值；仅包装 `prematureFlag` |
| 5 | 标志设置 | **正确** | `terminal` + `Ended` + 位置规则 → `PREMATURE.put(a,true)`；同 attempt 用 `HANDLED`（attemptId\|id）一次性 |
| 6 | 标志清除 | **基本正确** | 同一次 Ended 但判定非提前 → `PREMATURE.remove`；Activity 弱引用可 GC；**换源未 terminal 时不主动清**（见 L2） |

说明：用户可见文案仍是「读取失败：…提前结束」（含「失败」字样），属展示/日志；控制路径已不依赖匹配该字。一般失败路径仍可用文案关键字（`!rendered` 或「失败/超时/…」），合理。

## 硬约束核对
- 本 diff 无 AggregateHome / AggregateRules / CacheFiles / BifCache / EmbyStore / FadeFrame。✓
- `verify_apk.py` 的 `PATCH_MARKS` 七项（含 **PatchPlaybackBackend**）保留；版本号断言升到 157/0.1.156。✓

## 安全 / 崩溃 / ANR
- 无新增凭证、网络、权限；无主线程新网络。✓
- `playerState` 反射失败返回 -1，不当成 Ended；`tick`/`prematureEnd` 仍吞 Throwable。✓
- 仍走既有 250ms chrome 轮询（L2 未改，可接受）。
- 无新的并发写路径。✓

## 问题（按优先级）

无 High。无 Medium（相对 0155：M1 已关闭）。

### L1 低｜`android_device_tested` / `real_source_playback_tested` 继承 0.1.155，本版未真机
VALIDATION 写明 0.1.156 未上机、`phone_tested_this_release=false`，并用 `validation_flags_note` 解释；比 0155「矩阵与叙述打架」更诚实。仍建议合入后真机点一次提前断流路径再打签名包。

### L2 低｜`PREMATURE` 在换 attempt 且尚未再次 Ended 时可能残留
`prematureEnd` 在 `!terminal` 时直接 return，不清 `PREMATURE`。消费者均要求 `terminal&&premature`（failure / naturalFinish），故活跃播放中不会误弹失败卡或误抑下一集；下一轮 Ended 会按新 key 重算。可在 attemptId 变化时 `PREMATURE.remove` 更干净，非必须。

### L3 低｜检测依赖 `getPlayerState` 停留在 Ended(6)
若底座在 EndReached 后很快 `stop` 成 Stopped(5)，250ms 轮询可能永远看不到 Ended，从而既不设标志也不改文案——退回 0.1.155 前的「当播完」行为。反射失败同理。建议真机确认；必要时把 Stopped+terminal+位置规则也纳入候选（需防误伤手动停止）。

### L4 低｜`HANDLED` 在确认 premature 之前写入
`endReached` 为真后即 `HANDLED.put`，随后若位置规则判非提前则只清 `PREMATURE`。同 attempt 不会重入——符合一次性；若未来要「先 Ended 再补 runtime」二次判定，需调整顺序。

### L5 低｜完整 `build_ui`/`build_win` 仍会先把 VALIDATION 写成最小集
verify 的「保留已记录真机键」只对**运行时磁盘上已有的键**生效；build 脚本仍覆写为仅含 `android_device_tested:false` 的短 JSON。下次正式打包需记得事后补回说明，或再改 build 脚本合并而非覆盖。不阻塞本 PR（本版未打包）。

### L6 低｜L2–L6（0155）有意未改
主线程 250ms、`vlcRisky` 纯 DV、Emby 5xx 空结果、解码按次边界、门禁已接住的过程债——与 PR 范围一致，不重复升级。

## 测试
- 本机 `/workspace/suiying-pr32` @ `4951551`：javac 编译 `PlaybackEndRules` + `PlaybackReviewRules` + 两测试，**PlaybackEndRulesTest PASS**、**PlaybackReviewRulesTest PASS**（含标志/文案解耦/naturalFinish/源码接线扫描）。
- 作者：`scripts/run_tests.sh` **96/96**；本环境未跑全量（缺 `build/classes`/`stubs`/`android.jar` 完整 classpath，未执行 `build_ui`）。
- 未跑 `verify_apk.py`（无 0.1.156 APK）、未打签名包。
- 真机：本版无；继承 0.1.155 叙述。

## 给父代理 / 用户的摘要
1. **结论：有条件通过，建议合 main。无 High/Medium；6 Low（残留标志生命周期、Ended 窗口、VALIDATION/打包流程等）。0155 M1 已关闭。**
2. 硬约束与 7 个 PATCH_MARKS 完好；版本号 0.1.156/157 一致。
3. 相关 JVM **2/2 PASS**；作者 96/96。按要求 **不合、不打包**。
4. 审查全文：`/workspace/handoff/review-0156.md`。
