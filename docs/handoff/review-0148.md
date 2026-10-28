# 随影 `codex/playback-0148` / 0.1.148 审查 —— 对比 `0e44213`（0.1.147）→ `71c3555`
只审，不打包、不合并、不上传。对照范围用 cursor-github `compare_commits` / `get_commit` / `get_file_contents`（未 clone）。
测试：本机单独编译并跑 **PlaybackBackendRulesTest → PASS**；未跑完整 `run_tests.sh` / `build_ui` / 签名包（用户要求先只审；仓库未检出完整工作树）。
0.1.130 硬约束与 0.1.136 `embyKeep`：本分支 diff **不含** AggregateHome / AggregateRules / CacheFiles / BifCache / EmbyStore / FadeFrame；tip 上 `MERGE=agg-merge`、300ms 首画、`painted` 内容签名、`embyKeep` / `weakerEmbyLatest` 仍在 AggregateHome / AggregateRules。✓
结论：**有条件通过**。无 High 阻塞项；**1 条 Medium**（无转码权限账号选「服务器转码」后无原片回退）；若干 Low。播放兼容主路径设计合理，建议合 main 前真机再点验 README 已列项；本次按用户要求停在审查，不建议现在就打签名包。

## 版本号
| 位置 | 值 |
|------|-----|
| tip SHA | `71c35556a3d4a98cb4673c1cde7727592e17a537` |
| base (main) | `0e442131051014f06ef85feb50528697a989250f` |
| ahead / behind | **3 / 0** ✓ |
| AndroidManifest | versionName **0.1.148** / versionCode **149** ✓ |
| build_ui.py 产出名 | `MediaPocket-0.1.148-playback-compat-arm64.apk` ✓ |
| verify_apk.py 断言 | code 149 / name 0.1.148 ✓ |
| cinema-ui/VALIDATION.json | ui_version **0.1.148** / main_version_code **149** / jvm_test_programs_passed **94** ✓ |
| cinema-ui/README.md | 0.1.148 节已写 ✓ |

## 提交
1. `20da562` Record real Dolby HDR audio diagnosis and server transcode restrictions  
   → 仅 `docs/handoff/real-playback-0147.md`（真实杜比/HDR/音频与账号转码权限记录，脱敏）。
2. `e4710f8` Fix playback audio compatibility and panel Back in 0.1.148  
   → PlaybackBackend / Rules / PanelBack、EmbyPlayback 音频回退与 P5 转码列表、toVlc 显式 extra、侧栏 Back、viewport 同步、版本号。
3. `71c3555` Select VLC before playback creation and close the live prepare notice  
   → 去掉 poll 内 `PlaybackBackend.apply` 热切换；改为 `PatchPlaybackBackend` 在创建前替换两处 `useVlc`；`prepareNotice=` 赋值修复；规则测试同步。

## 范围摘要
15 文件（约 +189 / −69，compare 汇总）。**纯播放路径与文档**，不动聚合首页。

主改：
- **PlaybackBackend + Rules**：直连且已知音轨无平台解码器 → 本场 Intent 带 `mp.playback.vlc`；P5/未知 DV 转码优先时 sources 列表去掉原片；`preferVlc(requested\|\|default)`。
- **PatchPlaybackBackend**：base dex 中 PlayerActivity 恰好两处 `SharedPreferences.getBoolean("useVlc")` 换成 `PlaybackBackend.preferVlc(this)`，自检寄存器/指令宽度/其余指令不变。
- **PlayerPanelBack**：侧栏打开时代理 Window.Callback + API33+ OnBackInvokedCallback；关闭/销毁时还原。
- **SystemPlayerActivity**：首帧/错误/销毁收起准备对话框；`toVlc` 写入 VLC extra。
- **PlayerChrome**：布局变化与 resume 时 `syncViewport`（不再要求 hasWindowFocus）；dispose 顺带 `PlayerSidePanel.dispose`。

## 播放路径核对

| # | 要求 / 诊断项 | 结论 | 证据 |
|---|---------------|------|------|
| 1 | 「改用 VLC」不被默认 `useVlc=false` 覆盖 | **落地** | `toVlc` → `putExtra(VLC,true)`；创建前 `preferVlc` 读 Intent extra \|\| 偏好 |
| 2 | Emby 直连 AC3/TrueHD 等无平台解码器 → VLC | **落地** | `needsAudioFallback` + `softwareAudio`；P5/DV `risky` 时**不**因音频改走 VLC（保 Media3） |
| 3 | P5/DV 转码失败勿静默回原片 | **落地（刻意）** | `transcodeOnly` 时 list 只放转码源 |
| 4 | Media3 首帧后收起过期准备提示 | **落地** | tip 上 `prepareNotice=...show()` + `what==3` / error / destroy → `dismissPrepareNotice`（中间提交曾漏赋值，已由 71c3555 修） |
| 5 | 内核在创建前选择，避免开播后重开解码器 | **落地** | 去掉 `poll`→`apply`→`retryCurrentDecoder`；改 dex 钩子 |
| 6 | 系统返回先关侧栏再退出 | **落地（原生侧栏）** | `PlayerPanelBack`；Media3 自有控件未接同一套（见 L3） |

## 硬约束核对
- agg-merge 单线程、300ms 首画合并、`painted` 内容签名：tip AggregateHome 仍在；diff 未改。✓
- CacheFiles / BifCache / EmbyStore / FadeFrame：不在 diff。✓
- 0.1.136 `embyKeep` / `weakerEmbyLatest`：tip AggregateRules 仍在。✓

## 安全 / 崩溃 / ANR
- 无新增 token、密码、跨站 Cookie；`real-playback-0147.md` 声明脱敏、无真实服务地址。✓
- `MediaCodecList` 扫描在 `needsAudioFallback` 内、于 `EmbyPlayback.playItem` 工作线程、结果 volatile 缓存；未见新的主线程网络。✓
- 中间态 `apply` 每 250ms poll 热切 VLC 已从 tip 移除，降低主线程重开解码器风险。✓
- `PlayerPanelBack` Proxy + OnBackInvoked 在 dismiss/dispose 成对注销，降低 Callback 泄漏。✓
- 并发：未改 MERGE / Emby 缓存写路径。✓

## 问题（按优先级）

无 High。

### M1 中｜无转码权限账号选「服务器转码」后，P5/DV 可能硬失败且无原片回退
`PlaybackBackendRules.transcodeOnly` + EmbyPlayback 在 `transcodeFirst` 时对 `p5`/`dv` **只保留转码 URL**。排查文档已确认本账号 `Enable*Transcoding=false`。此时选转码会进入必败列表，不能再静默落到原片（这正是本版要修的「转码失败→原片紫屏/错核」）。  
**影响**：无转码权限用户若点「服务器转码」，体验变差于旧版「失败后还能播原片」。  
**建议**：合入可接受（与诊断一致）；UI/文案或 DolbyChoice 在探测到策略关闭转码时禁用/降级该选项（本分支未做，不阻塞代码合入，但应进后续）。

### L1 低｜VALIDATION 设备字段变瘦且部分标志不一致
相对 0.1.147，删掉了详细 `android_device_scope` / acceptance 列表；tip 上 `android_device_tested: true`，但 `real_source_playback_tested: false`、`dolby_android_device_tested: false`；`verify_apk.py` 仍会把 `android_device_tested` 写回 `False`。文档可信，但机器可读验收矩阵变弱。

### L2 低｜`PlayerPanelBack` 代理 Window.Callback 的边界
- `dismiss()` 先 `back.dispose()` 再滑出动画：动画期间再按返回会直接退出播放器（真机笔记称连续边缘手势先收起通过，可接受）。
- Proxy `throw e.getCause()` 在 cause 为 null 时不优雅（极罕见）。
- 若其它模块再包一层 Window.Callback，恢复 original 需仍是当前 wrapper（已有 `==wrapper` 守卫）。

### L3 低｜Media3（SystemPlayerActivity）侧栏未接 `PlayerPanelBack`
本版 Back 修复挂在 `PlayerSidePanel`（原生/VLC 链）。Media3Controls 自有「更多/音轨」若同样抢返回，不在本 diff。README 写的是「原生播放侧栏」。

### L4 低｜音频回退按「当前选中音轨」整场切 VLC，不尝试换 AAC 轨
`needsAudioFallback` 选默认/语言偏好轨；该轨无解码器即整场 decorate VLC。合理保守；多音轨片可能本可留在系统核。

### L5 低｜`PatchPlaybackBackend` 强依赖 base.apk 恰有 2 处 `useVlc` 选型
断言 `changes==2` 失败即打崩构建（失败安全）。base 升级或混淆布局变化时需同步改补丁；与既有 Orientation/HeaderRoute 补丁同风险级别。

### L6 低｜本审查未跑全量 JVM / 未装包复验
作者 README 称 94/94 与 OPD2401 合成项；本环境只复跑了新增 Rules 单测。合包前建议再跑仓库惯用 `run_tests.sh` + 签名校验。

### L7 低｜其它琐碎
- `PlaybackBackend.mime` 未覆盖少见别名时 `knownCodec=false` → 不回退（测试已覆盖「缺元数据不冒充不支持」）。
- `nativeTranscodeRefresh` 用 `keepMedia3(true,…)` 仅借用「同集」语义，命名略绕，行为正确。

## 测试
- **PlaybackBackendRulesTest**（本机 javac + main）：**PASS**（11 条断言：softwareAudio / transcodeOnly / preferVlc）。
- 完整 JVM 94/94、APK class 解析、16KB zipalign、真机项：以分支 VALIDATION / README 自述为准，**本审查未复跑**。
- 未执行 `build_ui` / 签名包。

## 给合包前的摘要
1. 诊断三项（VLC 入口、音频兼容、准备提示残留）在 tip 代码侧已兑现；创建前选型优于中间提交的 poll 热切。
2. 硬约束与 embyKeep 未动，聚合回归风险低。
3. **M1** 为有意取舍：无转码权限 + 用户强选转码 → 硬失败；合入前知悉即可，建议后续补策略探测。
4. 无 High；**可以合并**（用户本次只要先审 → 停在审查）。  
5. **暂不建议本轮打签名包**：等用户明确要求，并建议合并后按 README 真机清单再验一版再签。
