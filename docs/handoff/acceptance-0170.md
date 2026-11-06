# 0.1.170 测试交接（2026-11-04，修复轮收尾）

接手 0169 交底的修复轮。三个已知问题的修复已全部出包并在 OPD2401 / Android 16 真机上验证；0169 交底的剩余测试清单（字幕矩阵、详情页跳集、键盘扩展、音效/HDR/真实服务器）本轮未测，按 0169 清单续测即可。接手时先读本文，再确认远端是否新增提交。

## 基线与位置

- 被测提交：基于 `codex/acceptance-0169`（交接提交 `e08eeea`，被测功能提交 `bbd28f6`）的修复提交，0.1.170 / code 171。
- worktree 仍在主仓库下 `cinema-ui/build/polish-0141`，分支 `codex/acceptance-0169`（修复提交在此分支上）。主工作目录 `fix/detail-timeout-0103` 的未提交改动仍未动。
- 远端核对（本轮开始时）：`codex/acceptance-0169` 头 `e08eeea`、`cursor/subtitle-auto-style-b1d5` 头 `bbd28f6`、main `1a73daf`，与 0169 交底一致，无新提交。

## 构建与验证

- Windows 原签名构建成功；106/106 JVM 测试通过（新增 `Accept0170Test`）；verify_apk 全部补丁标记与 16KB 对齐通过；证书 SHA-256 与 0169 一致（`87a44d82…783d1`）。
- 最终 APK：`deliverables/MediaPocket-0.1.170-subtitle-fix-arm64.apk`，38,517,301 字节。
- APK SHA-256：`b9f4349e4aa88b518345142c73d9217c84ca84c050cbf91c71f9cdfec1add8bf`（中途多轮重建，此为最终装包版本；构建历史见 `cinema-ui/build/accept-0170-*.log`、`accept-0170-package.json`）。

## 三项修复与真机证据（deliverables/tablet-0170/）

### T1 / P2：Media3 长按方向键加速 — 已修复，真机通过

- `KeyboardKeys.media3` 现在把 `repeatCount` 传进共享规则 `KeyboardRules.seekStepMs`（与 VLC 同一条），经新增的 `Media3Controls.jump(direction,stepMs)` 下发。
- 真机（Window callback 注入 KeyEvent）：`media3-keyboard-0170.json`，位置 10794 → 26276（repeat=0，+15.5 秒）→ 56839（repeat=10，+30.6 秒）。修复前两种都只有约 15 秒。

### T2 / P2：VLC 改样式不生效 — 已修复，真机通过；根因链完整

- 根因（三层，逐层证伪后收敛）：
  1. freetype 样式（字号/颜色/描边/位置）只在**建 LibVLC 实例时的全局选项**里生效；media 级 `:freetype-*` 进不了 vout。0169 的原位重开（换 Media）换不掉样式，与此吻合。
  2. 第一版修复走 `retryCurrentDecoder`（底座重建播放器）——真机日志确认重建发生、中文轨恢复，但**颜色仍不变**（`t2-final-yellow-check.png`）；而「关闭播放页整页重开」（onCreate 路径）同设置下字幕是黄色（`t2-full-reopen-color.png`）。结论：retryCurrentDecoder 并不会真正换掉 LibVLC 实例。
  3. 第二版修复改为 `DecoderSwitch.rebuild` = `persist(false)` + 原样 `startActivity(本页 intent)` + `finish()`，让 onCreate 重建实例。
- 最终真机验证（最终包）：白色基线 → 设置改黄色 → 关窗 → 整页重开 → 字幕黄色（`t2b-after-rebuild-yellow.png`）、spu=3（中文轨自动选回）、位置从断点续播（54344 → 59609）。诊断日志 `字幕 · 样式改了，重建播放器让 VLC 内嵌字幕套用`（probe 新增 `debuglog` 动作可读 events.log）。
- 附带修复：重建后轨道曾被钉在片源默认轨（英文）——底座重建不会重跑自动选轨（autoDone 已置位）。整页重开后由底座自身选轨逻辑恢复，实测中文轨正确。**手动选轨后的保真未测**（0169 清单里本就有「手动选择不被自动接管」待测项）。

### T3 / P2：设置面板遮字幕、与播放菜单主题不一致 — 深色玻璃 + 实时预览已落地，状态栏一项环境受限

- 播放页弹层改深色半透明玻璃（`PlayerTheme.PANEL_SOFT`，与播放菜单同套 `PlayerTheme` 配色），行卡 `PlayerTheme.CARD`；「我的」整页保持各主题纸面不变（`shell(...,false)` 未动）。
- 弹层上方新增按当前设置实时渲染的预览条（`SubtitleStyle.style` 复用，`previewLiftPx` 一起抬升），改字号/颜色/样式/位置即时可见：真机截图白→黄实时变化（`settings-sheet-0170.png` → `settings-preview-yellow-0170.png`、`step3-picked-yellow.png`）。
- 沉浸状态：`show()` 在 `d.show()` 之后调 `VideoFullscreen.hideBars(d.getWindow())`（show 前 window 未 attach，insetsController 不生效，已修）。**但本 instrumentation 环境下播放页本身就恒显状态栏**——0169 的旧截图同环境一致（`compare-0169-top.png` 可见 15:42），故「打开设置才显现、关闭不恢复」的现象在自动化会话里无法对照复现；hideBars 的真机日常效果待用户复核，不要据此判过或判不过。

## 测试工具（本机 build/ 下，均 gitignore，不入远端）

- `validate_build_0170.py`（编排 build+tests+verify）、`test_win_0170.py`、`verify_win_0169.py`（沿用）；`accept-0170-{build,tests}.log`、`accept-0170-package.json`。
- 夹具：`make_fixture_0170.py`、`fixture_0170.py`、`fixture-0170/`（180 秒合成 H.264/AAC、双内嵌字幕 MKV；仅 localhost:38170）。
- Probe：`prepare_accept_0170.py`（由 stremio-probe-0163 生成）、`accept-probe-0170/`、`build_accept_probe_0170.py`；RPC 38169 / reverse 38170。probe ID 用 `fixture-accept-0170r2`——**换 ID 是为了绕开哈希 watchId 的旧进度/选轨残留**（0169 交底预警过的清理盲区，本轮实际踩到：旧 ID 会话从 113 秒残留进度恢复、基线轨是英文）。后续再遇"基线不对"先换 ID。
- 驱动：`accept_0170_device.py`（setup / start-probe / t1 / t2 / t3 / state / stop）。0169 轮的 `direct` 动作对 SystemPlayerActivity 过不了 safe() guard（intent 无 id extra、source 无 emby.server）——**测 Media3 走 Emby 夹具路径**（`fixture` → 点「播放」→ `media3`），t1 里已实现。
- probe 新增动作：`subtitle`（直设字幕偏好，带 remember 恢复）、`debuglog`（读 DebugLog events.log）。probe 的 shot/nodes 首次调用可能异常，重试即可（0169 已知现象）。

## 环境与收尾状态

- 正式应用已覆盖安装 0.1.170（原签名）；临时 probe `com.guocheng.polishacceptance` 仍安装在本机（下一轮续测直接用，`prepare_accept_0170.py` 可随时重新生成）；本轮 forward 38169 / reverse 38170 已移除。
- 每次 `rpc('stop')` 均返回 `restored=true`、服务器数 3；偏好经 probe 日志恢复，无遗留改动。字体/density/rotation/输入法未动过。
- 本轮多次 force-stop / 重装应用（排障），应用登录态等本地数据不受影响（fixture 服务器 ID 均为合成账号）。
- 下一轮必须重新建立快照（0169 交底要求仍然有效），不要沿用旧偏好快照。

## 未完成测试（沿用 0169 清单，按此续测）

1. 字幕关闭记忆、手动选轨不被自动接管（T2 重建路径会回退到自动选轨，此项现在更重要）、外挂与内嵌竞争、自动搜索无字幕内容、第二字幕与延迟；颜色/字号/描边/位置两引擎渲染对照（VLC 侧本轮只证了颜色，字号等走同一路径但未逐项截图）。
2. 详情页 Emby / 影视仓跳集（序号 1、3、5、65；不存在的 2）、主视觉兜底、选中集对应下载目标、播放返回后详情完整性（0169 未复测项仍未复测）。
3. 键盘音量、左右长按（VLC 侧已有 0169 证据）、锁定与侧栏优先级；实体键盘测试。
4. 新音效实际听感、手机窄屏、大字号、TalkBack、真实 HDR / 杜比、真实服务器转码、Stremio 网络速度。
5. T3 状态栏沉浸的真机日常复核（非 instrumentation 环境）。
