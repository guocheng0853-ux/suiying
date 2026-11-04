# 0.1.169 测试交接（2026-11-04，中途收尾）

用户因用量不足要求写 handoff，已停止扩展测试。产品代码未修改；本版不能标记为完整验收通过。接手时先读取本文，再确认远端是否新增提交。

## 基线与位置

- 被测提交：`bbd28f6`，来自 `origin/cursor/subtitle-auto-style-b1d5`，0.1.169 / code 170。
- 本轮 fetch 时 main 为 `1a73daf`（0.1.168）；本次测试的是较新的候选分支，不是 main。
- 交接分支：`codex/acceptance-0169`。本机 worktree 位于主仓库下 `cinema-ui/build/polish-0141`。
- 主工作目录在 `fix/detail-timeout-0103`，原有未提交改动未动。不要覆盖或将其混入本次提交。

## 构建及已完成验证

- Windows 原签名构建成功，105/105 JVM 测试程序通过；正式 verify_apk 校验通过，9 个补丁标记及 16KB 对齐通过。
- 证书 SHA-256：`87a44d822580215fff01d604b1fedb31e52d69b42eb85ffbcd8b65aea21783d1`。
- APK：`deliverables/MediaPocket-0.1.169-subtitle-auto-arm64.apk`，37,124,661 字节。
- APK SHA-256：`cfd7ae9e690e79c1a72da8fc6ac0326596e7e1b94354ea358046344ca07e9612`。覆盖安装后，设备 base.apk 哈希与之相同。
- OPD2401 / Android 16 平板已安装；手机未测试。用 localhost 合成服务、合成媒体及字幕，未导出真实账号配置。
- VLC 播放双内嵌字幕 MKV，画面与进度正常；默认英文轨存在时自动选择中文轨（SPU 3）。
- Media3 合成媒体播放正常，中文字幕显示；截图中字幕为修改后的黄色。
- VLC 更多菜单精简分组可打开；Media3 锁与播放键截图中位于同一水平线。
- VLC 键盘路径：基础步长 15 秒，repeat=0 约跳 15 秒，repeat=10 约跳 30 秒（位置 20737→36203→66193 ms）。是通过实际 Window callback 注入的 KeyEvent，不是实体键盘验收。

## 问题与建议（尚未修复）

### T1 / P2：Media3 长按方向键没有加速，已复现

基础步长 15 秒，单次右键及 repeat=10 都只跳约 15 秒：10057→25302→40532 ms。VLC 同条件能升至约 30 秒。

`KeyboardKeys.media3()` 调用 `c.jump(direction)`，未使用 repeatCount；VLC 使用 `KeyboardRules.seekStepMs(base, repeat)`。建议两端复用步长规则，保留普通点击行为并补回归。证据：`media3-keyboard.json`、`vlc-keyboard-window.json`。

### T2 / P2：VLC 修改字幕颜色后原位续播仍使用旧色，已观察到；根因待确认

字幕设置选择黄色，关闭设置触发原位重开；等待约 7 秒后已回到中文轨 3，字幕仍为青色。播放位置约 75 秒，画面及进度正常。Media3 对照截图为黄色。

证据：`vlc-color-after-reopen.json/.png`；早期截图 `subtitle-style-yellow.png` 也为青色，但当时临时回到英文默认轨，不能据此认定选轨持久化失败。

优先检查 `SubtitleSettings.restartVlc`、`DecoderSwitch.reopen`、`SubtitleVlc` 的选项应用及 LibVLC 实例生命周期。**尚未完成关闭整个 VLC 页面后重新打开影片的对照，因此不能断言重建播放器即可解决。** 不要只以偏好已保存或 JVM 规则通过判定原生渲染已生效。

### T3 / P2：字幕设置打开时原生字幕预览被面板遮住

VLC 打开字幕设置后底部大面积不透明浅色面板遮住原生字幕；当时处于合成字幕有效时间段，没有看到字幕被抬到面板上方。`previewLiftPx` / 自绘字幕抬升与 VLC 原生字幕不是同一路径，需验证原生 margin 应用时机。

证据：`subtitle-settings.json/.png`。README 宣称半透明面板和预览抬升，实际体验尚未达成。

### 附带打磨建议（不是全部已证实的功能缺陷）

- 字幕设置打开后系统状态栏、导航栏显现，浅色不透明面板与深色播放菜单不一致；建议统一播放页主题和沉浸状态，并保证关闭后恢复。
- 平板字幕设置面板占高较大，首屏可见设置较少；优化信息密度，同时保留触控尺寸。
- VLC 与 Media3 同一字号档位的截图字高差异较明显；建议用相同画幅、字幕文本与设置做尺寸标定，不直接用不同引擎参数名认定视觉一致。
- 注释、README 与真机证据同步：105 个 JVM 程序通过不代表字幕渲染、硬件键盘和整版功能通过。

## 未完成的测试，按此续测

1. 完成 T2 整页关闭再打开对照，以及颜色、字号、描边、位置的两引擎实际渲染验证。
2. 字幕关闭记忆、手动选择不被自动接管、外挂与内嵌竞争、自动搜索无字幕内容、第二字幕与延迟。
3. 详情页 Emby / 影视仓跳集（实际序号 1、3、5、65；不存在的 2）、主视觉兜底、选中集对应下载目标、播放返回后详情完整性。旧交接提到影视仓播放返回可能丢失详情，**本轮未复测，不能报告为新增确认**。
4. 键盘音量、左右长按、锁定与侧栏优先级；实体键盘测试。
5. 新音效实际听感、手机窄屏、大字号、TalkBack、真实 HDR / 杜比、真实服务器转码、Stremio 网络速度均未在本轮验收。

## 工具与证据（本机保留，未公开上传私密材料）

以下均相对于本次 worktree；build / deliverables 为忽略目录，远端不会自动带上：

- `cinema-ui/build/validate_build_0169.py`、`test_win_0169.py`、`verify_win_0169.py`：Windows 构建与完整测试适配。
- `cinema-ui/build/accept-0169-{build,tests,apk}.log`、`accept-0169-package.json`。
- `cinema-ui/build/make_fixture_0169.py`、`fixture_0169.py`、`fixture-0169/`：180 秒合成 H.264/AAC、双字幕 MKV、稀疏集数 Emby/影视仓服务。仅 localhost:38170。
- `cinema-ui/build/accept_device_0169.py`、`prepare_accept_0169.py`、`build_accept_probe_0169.py`、`accept-probe-0169/Probe.java`：临时 instrumentation。RPC 38169，服务 reverse 38170。
- `deliverables/tablet-0169/`：状态 JSON、截图、清理记录。`*.private.json` 包含原设置快照，仅本地保留。
- 初始 `vlc-keyboard.json` 测试工具绕过 Window callback，结果无效；应使用修正后的 `vlc-keyboard-window.json`，不要把前者算作产品故障。
- 辅助工具首次 nodes 调用可能空列表，第二次正常，是工具初始化现象。
- 继续影视仓播放测试前，先补齐其哈希 watchId 对应 pos/dur 偏好的备份；当前工具只自动清理含 fixture ID 的键，不能假定哈希键会被清理。

## 收尾状态（已完成）

- 已关闭测试页面，停止 instrumentation，恢复所备份的应用偏好；`restored=true`、`preferences_match=true`，原服务器数量 3。
- 字体、density、rotation、默认及启用输入法、night mode 和所记录无障碍设置，前后逐项相同。
- 临时包 `com.guocheng.polishacceptance` 已卸载；正式应用保留原签名 0.1.169。
- 仅移除本轮 forward 38169、reverse 38170；既有其他转发未清理。合成服务器已请求关闭。
- 清理证据：`cleanup-preferences.json`、`cleanup-summary.json`、`settings-after.private.json`。
- 下一轮必须重新建立快照及启动测试环境，不要沿用上一轮偏好快照覆盖用户后续改动。
