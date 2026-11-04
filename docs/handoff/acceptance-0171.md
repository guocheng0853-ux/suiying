# 0.1.171 测试交接（2026-11-04，字幕关闭记忆修复 + 0169 清单续测）

接手 0170 交底的续测轮。清单第 1 项（手动选轨不被自动接管）实测**通过**，但过程中发现并修复了一个真 bug：**字幕关闭记忆在冷启动重开时失效**（0170 交底预警的正是这一项的重建路径）。修复已出包装机全绿。0169/0170 清单的字幕矩阵大部分完成，详情页跳集复测通过。

## 基线与位置

- 被测提交：基于 `codex/acceptance-0169`（0170 修复提交 `e6c70a7`）的 0.1.171 / code 172。
- worktree 仍在 `cinema-ui/build/polish-0141`，分支 `codex/acceptance-0169`。开始时远端核对：`origin/codex/acceptance-0169` 头 `e6c70a7`、`origin/cursor/subtitle-auto-style-b1d5` 头 `bbd28f6`、main `1a73daf`，与 0170 交底一致，无新提交。

## 发现的真 bug 与修复（T4，本轮核心）

### 现象（0.1.170 对照组）
- 字幕面板「关闭」→ prefs 写入正确（`subtitleDisabled:<id>=true`，spu/name 键移除）→ **普通重开后字幕自动回开**（spu=2，片源默认英文轨）。改样式整页重建路径在暖启动下偶尔正确（时序竞态），冷启动必现。

### 根因（两处，都在 `TrackSelection.attach` 轮询）
1. **alreadySpu 误判未选态**：轮询首拍时 player 已就绪但 VLC 还没解析出轨道列表，`currentSpu()=-1` 被 `alreadySpu(-1,-1)` 当成「已是关闭态」→ 跳过下发，且 `userOff` 立即 `subtitleDone=true` 收拍 → VLC 随后自己挂上默认轨，无人纠正。重建路径因暖启动抢先下发而侥幸正确——这正是 0170 轮没暴露它的原因。
2. **视图换新静默断拍**：底座整页重开/换视图后 view 实例变化，原 `if(initial!=null&&video!=initial)return;` 直接 return **不重排下一次**——轮询永久死亡，重建页的关闭/选轨记忆无人接管（装机实测重建后 90 秒 spu 无一次被纠正）。

### 修复（`TrackSelection.attach`，两处 + 注释）
- 下发跳过只适用于具体轨道目标（`step.spu>=0`）且列表已出（`raw!=null`）；关闭态（spu=-1）在 `playPolls>8`（与现有 waitedOut 语义一致）内每拍重申，穿过 VLC 默认轨的整个初始化窗口才收拍。
- `initial` 改单元素数组，视图换新就重接（`initial[0]=video`）继续管；来源变化由 `sameSource` 兜底。

### 真机验证（最终包，逐拍 prefs 取证）
| 场景 | 0.1.170 | 0.1.171 |
|---|---|---|
| 基线（zho 自动选轨） | spu=3 ✅ | spu=3 ✅ |
| 关闭（面板「关闭」行） | spu=-1、disabled=true ✅ | 同左 ✅ |
| **普通重开** | **spu=2 回开 ❌** | **spu=-1 保持 ✅** |
| **改样式整页重建** | （暖启动时对时不对） | **spu=-1 保持 ✅**（重建事件 DebugLog 铁证） |
| 手动选轨（面板→字幕来源→内嵌字幕轨→English） | — | spu=2 + prefs `subtitleSpu=2`/`subtitleName` ✅ |
| 手动选轨→普通重开 / →重建 | — | spu=2 保持（各两次复证）✅ |

- JVM：新增 `Accept0171Test`（下发跳过门槛、收拍窗口、视图跟随接线、vlcPoll 记忆分支语义、off/embedded 互补持久化）；同步更新 `SubtitleAutoTest` 对旧接线的源码断言。107/107。
- 最终包：`deliverables/MediaPocket-0.1.171-subtitle-off-race-arm64.apk`，38,517,301 字节，sha256 `8d157ebefcb54e5ee6d635df6e6346097920669bc42d2b62277b00d17535f470`，证书与 0170 一致（`87a44d82…783d1`），16KB 对齐通过。

## 0169 清单其余项的本轮结论（deliverables/tablet-0171/）

1. **手动选轨不被自动接管：通过**（上表）。消费链=`TrackSelection.attach` 轮询读 `subtitleSpu/subtitleName:<id>` → `SubtitleAuto.vlcPoll` 记忆分支优先于偏好语言自动选。
2. **外挂与内嵌竞争**：S1 偏好语言内嵌胜非偏好外挂（zho 下中文内嵌 spu=3，英文 sidecar 不加载）✅；S3 无内嵌对白轨时外挂加载，经 cue 层渲染（`t6-s3-crop.png`，VLC 轨关闭 spu=-1）✅。**工具限制**：probe `direct` 每次强制写 `subtitleLanguage=zho`，「偏好语言外挂胜内嵌」分支无法在 attach 前置 eng 偏好，未覆盖（代码路径 `sidecarPreferred` 分支有 JVM 语义测试）。
3. **第二字幕**：面板 第二字幕→`○ 外挂 · eng` → `subtitle2Url:<id>` 即时持久化 ✅，第一字幕（VLC 特大青色中文）+ 第二字幕（cue 层英文）同屏叠加（`t7-second-overlay-crop.png`）✅。**字幕同步**：+100ms → `sub.offsetMs=100` ✅、归零 → 0 ✅。
4. **VLC 渲染对照**：颜色（0170 黄）+ 本轮字号「特大」（`t5-vlc-huge-clean-crop.png`，同文本明显放大，freetype 全局选项经重建生效）。描边 / 位置未逐项截图（同一通道，弹层预览即时性 0170 已证）。
5. **详情页 Emby 跳集复测：通过**。夹具 series 4 集（1/3/5/65）：「更多来自…」分区头 → 跳至对话框 → 65 跳转成功（E65 选中）、不存在的 2 → toast + 保持原选集（`t8-jump65-real/2-real-0171.png`）。主视觉兜底、下载目标对应、播放返回详情完整性未复测（时间预算，沿用 0169 清单）。

## 测试工具（本机 build/ 下，gitignore 不入远端）

- probe：`prepare_accept_0171.py`（生成 Probe.java，**probe ID=`fixture-accept-0171`**）、`build_accept_probe_0171.py`、`accept-probe-0171/`。新增动作：`prefs`（转储含 ID/sub.*/subtitle* 的偏好——本轮排障关键）、沿用 0170 的 `subtitle`/`debuglog`/`direct`(支持 `sidecar`/`file`/`reset`) 等。
- 驱动：`rpc_0171.py`（助手模块）、`validate_build_0171.py`（build+tests+verify 编排，日志 `accept-0171-*.log`、`accept-0171-package.json`）。
- fixture：沿用 0170（`fixture_0170.py` 端口 38170；dual.mkv 双内嵌轨、english.srt 外挂、plain.mp4 无字幕轨）。

## 环境坑与纪律（下轮必读）

1. **probe 重启会洗测试状态**：probe `recover()` 启动时还原 journal 并**删除所有含 probe ID 的键**（含 `subtitleSpu:<id>` 等原始 ID 记忆键），journal 里的 `subtitleLanguage` 旧值（曾为 'eng'）也会覆盖回来。任何「记忆没生效」的结论前先 `prefs` 动作取证；跨 probe 重启的对比无效。
2. **`direct` 的 `reset:true`（默认）删除所有含字面 ID 的键**——测记忆必须 `reset=False` 重开；本夹具流的观看进度键也是原始 ID（`pos:<id>`），会被洗掉（0170 说的哈希 watchId 是 Emby 路径的）。
3. **双 instrumentation 会话互踩**：重启 probe 前必须 force-stop 两个包再起单个会话；probe 线程被单条命令打死时同样处理（0170 已知现象的重现与配方一致）。
4. **808dp 是设备自然值**：density 2.625 下 2120px≈808dp，横屏全屏即报 808——0170 交底把它归因缩窗参数不准确，本轮无缩窗参数同样 808。缩窗参数 `-e window_dp` 本轮全程未用（全屏）。
5. **chrome 时序飘**：控制条 ~4s 自动隐藏且 DPAD 键唤不出（`handled=False`，与 Media3 路径不同）。可靠模式=新开 Activity 后 0 延迟重试点击；面板（scrim）一旦打开不依赖 chrome。点击后立刻 prefs 取证可甄别「点到过期节点」（performed=true 但无效果）。
6. 会话中途改 `subtitleLanguage` 不影响已收拍的轮询（设计如此；真实用户走设置页的即时处理器）。

## 环境与收尾状态

- 设备：0.1.171（code 172）已覆盖安装（原签名）；probe `com.guocheng.polishacceptance`（0171 版，带 prefs 动作）仍在机；forward/reverse 已移除。
- `rpc('stop')` 返回 `restored=true`、服务器数 3；无遗留改动。下一轮重新建快照（0169 纪律仍有效）。
- 证据：`deliverables/tablet-0171/`（60 个文件：JSON 取证 + 截图）。

## 未完成测试（按优先级续）

1. 主视觉兜底、选中集对应下载目标、播放返回后详情完整性（0169 未复测项延续）。
2. 偏好语言外挂胜内嵌的分支（需改 probe：`direct` 支持不写 subtitleLanguage 或可指定语言）。
3. 描边 / 位置 VLC 渲染逐项截图（颜色 + 字号已两点实证同通道）。
4. 0169 清单第 2-4 条：键盘音量实测（方向键上下）、手机窄屏、大字号、TalkBack、真实 HDR / 杜比、真实服务器转码、Stremio 网速。
5. T3 状态栏沉浸的真机日常复核（0170 遗留，非 instrumentation 环境）。
