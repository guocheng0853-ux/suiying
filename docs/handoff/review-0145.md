# 随影 0.1.142 → 0.1.145 审查与交接 —— 对比 903da65（0.1.141）→ 01a098b

日期：2026-10-26。基线 `main` = `01a098b3a69170e52ceeba75f15a12f9329c9c29`（0.1.145 / code 146）。范围 8 个提交、52 文件（+978 / −99）。

后续处理：0.1.146 / code 147 已修复 M1、M2、L1，补充92项回归及平板分屏、原生/Media3失焦恢复、主动取消与次数上限验收。M2真实杜比P5转码中断恢复仅有源码与JVM覆盖，未作真机通过结论；具体证据与范围见 [0.1.146验收](../../DEVICE-RECOVERY-0146.md)。L2–L5保留为后续小项。下文是原始审查记录。

**结论：可以继续发布，没有阻断项。** 有 2 个中等问题建议下一版修（M1、M2），另有 5 个小问题（L1–L5），其中 L1 建议与 M1/M2 一起修。

**本次验证范围（只到源码和构建，未上真机）：**

- `python3 cinema-ui/build_ui.py` 出包 `MediaPocket-0.1.145-probe-timeout-arm64.apk`。
- `bash scripts/run_tests.sh` 92/92 通过。
- `verify_apk.py` PASS；签名 SHA-256 `87a44d82…`，与正式版一致，可覆盖安装。
- 构建改写了 `cinema-ui/VALIDATION.json`，已 `git checkout` 还原。
- 公开仓库检查：
  - 密钥 grep 为空。
  - `scripts/android_emby_fixture.py` 只监听 `127.0.0.1`，令牌是合成值 `fixture-token-only`。
  - TorrentClaw 专用入口已在搜索页移除（`ScrapedMainActivity.polishBrowse`），饭太硬等预置源未加回。
- **审查人没有在任何设备上运行这些代码**，以下真机现象都是读代码推出来的，修复后必须实测。

## 版本号

| 位置 | 值 |
|------|-----|
| AndroidManifest | versionName **0.1.145** / versionCode **146** ✓ |
| build_ui.py 产出名 / `ui_version` | `MediaPocket-0.1.145-probe-timeout-arm64.apk` / 0.1.145 ✓ |
| verify_apk.py 断言 | code 146 / name 0.1.145 ✓ |
| cinema-ui/VALIDATION.json | ui_version 0.1.145 ✓ |
| 测试文件数 / jvm 戳 | 92 ✓ |

## 中等问题

### M1　平板分屏或窗口失焦时，加载提示和失败卡片都被隐藏

- **位置**：`PlayerOverlay.java:200`（`status()`）。
- **现状**：0.1.143 起，`blocked` 的条件加上了 `!a.hasWindowFocus()`。`blocked` 既决定是否自动恢复，也通过 `connectHud(a, blocked ? "" : …)` 决定提示是否显示。
- **现象**：
  - 平板左右分屏（或自由窗口）时，用户在另一半操作，播放器就没有焦点。
  - 这时片源终止失败：画面停住，不出失败卡片、也没有「重新连接 / 选择片源」。
  - 正在连接时，转圈也会消失。用户点回播放器才会出现。
  - 这是相对 0.1.141 的回退：0.1.141 的 `blocked` 只含侧栏、菜单、锁定和画中画。
- **改法**：拆成两个条件。
  ```java
  boolean covered=PlayerSidePanel.open(a)||CinemaMenu.isOpen(a)||PlayerGestures.locked(a)||(Build.VERSION.SDK_INT>=24&&a.isInPictureInPictureMode());
  EmbyAutoRecovery.nativeTick(a,failure,covered||!a.hasWindowFocus(),message); // 自动恢复仍要求有焦点，和 EmbyPlayback 完成时的 hasWindowFocus 检查一致
  connectHud(a,covered||(!failure&&(rendered||terminal||message.trim().isEmpty()))?"":…,failure); // 提示不看焦点
  ```
  如果当初加焦点条件是为了不让卡片压在对话框下面：对话框在自己的窗口里，本来就盖在卡片上面，不需要隐藏。实在要隐藏，至少排除多窗口：`!a.hasWindowFocus()&&!(Build.VERSION.SDK_INT>=24&&a.isInMultiWindowMode())`。
- **验收**：
  - 本机验收服务设 `mode=fail`，平板分屏打开测试片，把焦点放到另一个应用。失败卡片应可见，按钮点回后可用。
  - 同样在分屏下设 `mode=delayed`，连接中应一直有转圈。

### M2　自动恢复不沿用杜比视界 / 服务器转码的选择

- **位置**：`EmbyPlayback.java:65`。
- **现状**：`if(recovery!=null)vlc.accept(false);` 让自动恢复一律直连优先，跳过了 `DolbyChoice`。
- **现象**：
  - 杜比视界 P5 片源，用户选了「服务器转码」，VLC 按 [转码, 直连] 播放。
  - 网络短断，两路都失败，进入 terminal。
  - 自动恢复重新取到地址后用 [直连, 转码] 打开，VLC 直连 P5 又会发紫发绿，这正是 0.1.111 `DolbyChoice` 要避免的。
  - 手动「重新连接」走的是不带 recovery 的 `playItem`，会重新经过 `DolbyChoice`，不受影响。
  - 从内置杜比（`SystemPlayerActivity`）发起的恢复走 `instanceof SystemPlayerActivity` 分支，也不受影响。
- **改法**：在进 IO 线程之前，记下当前在播的是不是转码那一路。
  ```java
  final boolean wasTranscode=recovery!=null&&TrackSelection.stream(a).optBoolean("emby.transcodeFallback",false);
  …
  if(recovery!=null)vlc.accept(wasTranscode);else if(DolbyRules.risky(dolby))…
  ```
  `emby.transcodeFallback` 由 `EmbyApi.withTranscode` 写入。新选择不是直连时 `all.length()==1`，`vlc` 里的 `transcodeFirst&&all.length()>1` 会自然退回单一路，不用额外处理。
- **验收**：
  - 在 `EmbyRecoveryRules` 加纯逻辑方法和 JVM 测试，或在源码 grep 测试里锁住「recovery 分支不再是固定的 `accept(false)`」。
  - 真机：有杜比视界 P5 片源时，选「服务器转码」，播放中断网再恢复，确认恢复后仍是「Emby 服务器转码（兼容模式）」。没有这样的片源时，在验证记录里写明只做了源码 + 单测。

## 小问题

### L1　短暂失焦会用掉唯一一次自动恢复（建议与 M1/M2 一起修）

- **位置**：`EmbyAutoRecovery.java:18`（`start` 先写 `COUNT=1`）、`:24`（`nativeTick` 失焦即 `cancel`）。`EmbyPlayback` 完成时 `!a.hasWindowFocus()` 也会 `cancel`。
- **现象**：
  - 恢复请求要 1–3 秒。这期间拉一下通知栏或弹出系统对话框，尝试就被取消。
  - 但 intent 里的 `COUNT` 已经是 1，回到播放器后不会再自动尝试。
  - 卡片显示「已尝试自动重新连接」，其实这次尝试没有跑完。
- **改法**：
  - 尝试还没有结果（`pending` 仍为 true）就被取消时，回滚 `COUNT`（移除 `EmbyRecoveryRules.COUNT` / `ITEM`），`complete()` 和 `failed()` 保持 1。
  - 把规则做成 `EmbyRecoveryRules.afterCancel(boolean pending,int count)` 并加测试。
  - 上限仍然有保证：每次重新开始都需要一次用户造成的失焦，不会自转。
- **验收**：
  - 验收服务 `mode=delayed`、`delay=5`。第二次播放信息请求挂起期间拉下通知栏再收起，应自动重新发起并开播。
  - `/__fixture__/state` 里 `playback_info` 应为 3。

### L2　取消体检时，后台线程最长还要约 10 秒才退出

- **位置**：`EmbyHealthHttp.java:13`（`reading` 后 `cancel()` 有意不断开）、`:21`（读超时 = 整段剩余预算，最长 10 s）。
- **现象**：服务器回包卡住时点「取消体检」，界面立即显示已取消，但 worker 要等这次 read 超时。连续点几次「重新体检」可以占满 `EmbyDiagnostics.IO` 的 2 个线程，新的体检一直停在「等待」。
- **改法**：照 `LinkProbe` 的做法，拿到响应码后把读超时切成 250 ms 一段，捕获 `SocketTimeoutException` 后检查 `cancelled` 和截止时间再继续。
- **注意**：Android 内置 HttpURLConnection（OkHttp 实现）连接后再 `setReadTimeout` 可能不生效。要用验收服务 `health_delay` 在真机上确认，不生效就保持现状并在注释里写明。

### L3　「服务器连接」失败后仍继续查「登录状态」

- **位置**：`EmbyHealthCheck.java:25`。第 2 段失败只记 FAIL、不 return，第 3 段对同一个不可达地址再等最长 10 s。
- **判断**：可能是有意为之（反代可能拦了 `/System/Info/Public`）。若保留，第 3 段文案可提示「服务器连接未通过，登录检查结果仅供参考」。若不需要，按超时类错误跳过后续阶段。

### L4　失败卡片显示期间每 250 ms 重新量尺寸、提到最前

- **位置**：`PlaybackFailureCard.java:20`。`show()` 每次都 `position()` → `measure()`，再 `bringToFront()`（触发重新布局），`EmbyAutoRecovery.count()` 还会解析一次 stream JSON。
- **改法**：只在描述、宽度或底边距变化时才 `measure` / `bringToFront`。功能不受影响，只是失败画面停留时多耗电。

### L5　VLC 解码类失败也会触发一次播放信息刷新

- **位置**：`EmbyRecoveryRules.java:9`。永久错误词表只有「格式不支持 / 解码器不支持 / unsupported」等。
- **现象**：VLC 状态文字是「解码失败」之类时，仍会向 Emby 重新要一次地址，结果多半相同。无害，只是多一个请求。可补「解码失败」「无法解码」并加测试。

## 做得好的地方（不要改回去）

- **自动恢复有上限**：每条播放链最多一次，计数随 intent 传给新的播放器（`carry`）；手动重试才开新的一条链；换集计数归零。
- **取消后不会再拉起播放器**：IO 线程开头、拉起播放器前，都检查 `EmbyAutoRecovery.valid` 和窗口焦点。
- **体检全程只读**：
  - `IsPlayback=false`、`AutoOpenLiveStream=false`，不发送播放开始，也不改观看进度。
  - 跨站跳转拒绝，不带凭据；`https` → `http` 降级同样拒绝。
  - 响应上限 1 MB。
- **脱敏报告**：只含阶段名、状态、耗时和固定文案，不含地址、账号、令牌、片名、编号。
- **0.1.145 的 `LinkProbe`**：计时器不再 `interrupt` 调用线程，响应头用完整预算，响应体才用 250 ms 分段读。这样修掉了「中断信号漏到线程池、下一次检查立刻失败」的问题。
- **非 Emby 片源的「重新连接」**：从保留的 stream 重新打开，绕过 terminal 后被清空的地址。已由 132c3a4 / 7868151 在平板上实测。
- **Activity 清理到位**：`EmbyDiagnostics.RUNS`、`EmbyAutoRecovery.ALL` 在 `onDestroy` / `PlayerChrome.dispose` 里移除，没有因 value 反引用 key 导致的泄漏。

## 下一版（0.1.146 / code 147）执行清单

1. 修 M1、M2、L1，各自补 `*Rules` 纯逻辑测试或源码 grep 测试。JVM 测试不能加载带 Android import 的类。
2. 版本号四处同步：
   - AndroidManifest。
   - `build_ui.py` 产出名与 `ui_version`。
   - `verify_apk.py`：路径、`group(2)` / `group(3)`、`main_version_code`。
   - `cinema-ui/README.md` 条目。
3. `python3 cinema-ui/build_ui.py` → `bash scripts/run_tests.sh`（jvm 戳 = `*Test.java` 文件数）→ `python3 cinema-ui/verify_apk.py` → `apksigner verify --print-certs`，SHA-256 以 `87a44d822580215f` 开头。最后 `git checkout -- cinema-ui/VALIDATION.json`，除非这次就是要更新它。
4. 真机：
   - 按 `scripts/ANDROID-EMBY-FIXTURE.md` 起验收服务，`adb -s SERIAL reverse tcp:37813 tcp:37813`。
   - 跑 M1 / L1 的验收，M2 视片源条件。
   - 把实际做了什么写进验证记录，不要把「目录加载成功」当成播放成功。
5. 推送前：
   - `git grep -nE "guocheng126|490cd2aca5|lijunfei|李俊飞|69yun69"` 必须为空。
   - 不提交 `signing.jks`、真实 `fusion-default.json`、服务器 IP、Whisper 令牌、本机用户路径。
   - 直接提交到 `main`，提交身份为 Ethan。

## 环境备注

- 电脑上的 adb 不在 PATH 里，位于 `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`。
- 手机和平板常同时连着，所有 adb 命令都要带 `-s SERIAL`（`adb devices -l` 看 `model:` 区分）。
- 覆盖安装用 `install -r`，保留片源、插件和观看记录。
