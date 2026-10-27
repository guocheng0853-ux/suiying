# 随影（MediaPocket）项目说明 —— 给接手的 AI / 开发者

> 0.1.150（code151）候选在 `codex/player-polish-0150`：播放页菜单去重、比例反馈、12sp状态行、白场对比度与TalkBack控件保留。首轮候选已做OPD2401合成验收，窄窗口发现的Media3截图键重叠已修正；平板随后断开，最终包覆盖安装、此项真机复测和清理核对待重连。见 `docs/handoff/acceptance-0150.md`，不要将首轮结果冒充最终包全项通过。
> 当前版本0.1.149（code150）：基于main的0.1.148 / PR #31，修正ColorOS分屏中播放器强制横屏造成的错误全屏兼容边界。原签名、94/94 JVM、APK校验和16KB对齐通过，已覆盖安装OPD2401；分屏失焦、失败卡片、连续缩放、Media3分屏与播放中回全屏通过，设置与测试环境已恢复。见`docs/handoff/acceptance-0149.md`及其脱敏汇总。
> 0.1.148已修复音频兼容/VLC入口、准备提示与O1侧栏返回。真实P5严格Media3声画已由用户在0.1.147独立样本确认，不能冒充0.1.149重测；真实转码受账号关闭权限限制，仍未通过成功转码验收。早期C1–C4及边界记录保留于0.1.147交接。
> 本仓库是**公开**的：本文不含任何令牌、密码以外的秘密和服务器地址；签名文件不在仓库里（见第 5 节）。

## 协作约定

- 用户要求及时同步 GitHub：每完成并验证一批改动，就创建本地提交并推送对应分支，不只保存在本地。合并 `main` 或发布 APK 按用户指示执行。推送失败时说明原因，保留本地工作；真实账号、令牌、签名密钥及本地配置不得提交。

## 1. 这是什么

随影是用户自用的 Android 影视播放器：Stremio 插件目录、影视仓（TVBox）配置、Emby 媒体库三类片源，VLC 播放，支持选集、字幕（在线搜索 / Whisper 语音生成）、弹幕、P2P、投屏、外部播放器。实测设备包括一加 PJZ110 手机（Android 17，1440×3168）和 OPD2401 平板（Android 16，2120×3000）。

## 2. 架构：基础 APK + Java 扩展层（不是 Gradle 工程）

```
cinema-ui/base.apk          原版 0.1.20：MainActivity / PlayerActivity / GesturePlayerActivity、VLC(libVLC 3.0.22)、
                            jlibtorrent 2.0.12、QuickJS、TorrentEngine 等。它的 dex、so、assets 原样保留（verify 会逐个比对）。
cinema-ui/src/…/mediapocket  扩展层 Java（约 130 个类），与原版同包名，编译成单独的 dex 加进 APK。
cinema-ui/stubs              原版类的编译期桩（只给扩展层编译用，不进 APK）。
cinema-ui/runtime-src|libs  影视仓爬虫运行时（JS / jar 爬虫），build_fusion_runtime.py 编译。
cinema-ui/tools/Patch*.java  对原版 classes.dex 的字节码补丁（dexlib2），每个补丁自带逐条指令自检。
cinema-ui/AndroidManifest.xml 扩展层的清单（入口是 ScrapedMainActivity，播放器仍是原版 GesturePlayerActivity）。
```

扩展层怎么"改"原版：
- **继承覆盖**：ScrapedMainActivity 继承原版 MainActivity，覆盖包内可见方法（同包同类加载器，invoke-virtual 会走到覆盖版本）。
- **反射读写原版字段 / 调原版方法**：`OnlineSubtitleSearch.field(obj,"name")`、`OnlineSubtitleSearch.call(obj,"method",types,args…)`、`SourceSubtitleLoader.set(obj,"field",value)`。原版字段名用 jadx 反编译 `build/original-main.dex` 查（`build-tools/jadx/bin/jadx -d out cinema-ui/build/original-main.dex`）。
- **字节码补丁**（tools/Patch*.java）：HeaderRoute（VLC 不支持自定义请求头 → 本地代理）、TorrentBoost / PieceReads（P2P）、SubtitleSearch、PlaybackOrientation、ClawNet。改补丁前先读对应 Verify 自检。
- **播放页 UI**：PlayerChrome 在原版播放页上挂扩展控件并每拍 `poll()`；PlayerOverlay 重排顶栏 / 底栏；PlayerSidePanel 右侧面板；CinemaMenu / CinemaDialog / CinemaWait 深色菜单、对话框、等待框。

## 3. 目录

| 路径 | 内容 |
| --- | --- |
| `cinema-ui/build_ui.py` | 构建脚本（Linux）。输出 `deliverables/MediaPocket-<版本>-<名>-arm64.apk` |
| `cinema-ui/verify_apk.py` | 构建后校验：类唯一、方法 ABI、版本号、原版代码 / so / assets 未被改动 |
| `cinema-ui/tests/*Test.java` | JVM 单测（`main` 方法）。`scripts/run_tests.sh` 全跑，数量等于该目录下的 `*Test.java`，当前 94 个。通过后写入 `cinema-ui/build/jvm-tests.passed`，`verify_apk.py` 读取这个数字，不再写死次数。 |
| `cinema-ui/README.md` | 每个版本一条更新记录（新的在最上面），写清验证范围 |
| `cinema-ui/VALIDATION.json` | 构建自动生成的校验摘要 |
| `cinema-ui/assets/fusion-default.example.json` | 默认影视仓配置的**脱敏样例**；真实的 `fusion-default.json` 含用户个人令牌，只在本地，已 gitignore |
| `cloud/whisper-srt/server.py` | 用户自己服务器上的 Whisper 字幕服务（默认只监听 127.0.0.1，经 Caddy HTTPS 反代） |
| `scripts/setup_build_tools.sh` | 在 Linux 上从 GitHub 拉齐构建工具到 `build-tools/`（jadx、apktool 的 aapt2、uber-apk-signer 等） |
| `HANDOFF-NEXT-AI.md` | 上一棒的交接（含 Windows 构建链、踩过的坑） |
| `HANDOFF-CODEX-DEVICE.md` | 真机调试任务清单（在 `wip/0.1.98-device-fixes` 分支上） |

## 4. 构建与测试

**Linux**：`sh scripts/setup_build_tools.sh`（一次）→ `python3 cinema-ui/build_ui.py`（无密钥时加 `--unsigned`，否则拒绝写出未签名的正式包）→ `python3 cinema-ui/verify_apk.py` → `sh scripts/run_tests.sh`。
流水线：ecj 编 stubs / src → D8 转 dex（min-api 26）→ dexlib2 补丁原版 dex 并自检 → aapt2 打清单 → zipalign（`-P 16`，16KB 页对齐）→ apksigner 签名 → verify。
**Windows（用户这台电脑）**：用本地的 `cinema-ui/build/build_win.py`（不在仓库，build_ui.py 的 Windows 适配，用 SDK 的 aapt2 / zipalign / d8 / apksigner）。Python 用 `uv.exe run --no-project python`。中文 Windows 上 JDK 默认 GBK：读写文件一律指定 UTF-8。详见 HANDOFF-NEXT-AI.md。

**每发一个版本，四处同步**：
1. `cinema-ui/AndroidManifest.xml` 的 `versionCode`（+1）和 `versionName`
2. `build_ui.py` 的 APK 文件名和 `'ui_version'`
3. `verify_apk.py` 的 APK 路径、`match.group(2)=='<code>' and match.group(3)=='<版本>'`、`'main_version_code'`
4. `cinema-ui/README.md` 顶部加一条（做了什么、为什么、测了什么、**没在真机测什么**）

## 5. 数字签名（重要）

覆盖安装要求新 APK 和手机上已装的 APK **用同一把签名**。换签名 = 只能卸载重装 = 用户的观看记录、接口、插件、密钥全部丢失。

| 项 | 值 |
| --- | --- |
| 文件 | `signing.jks`（2.6 KB） |
| 放在哪 | **不在仓库里**（`*.jks` 已 gitignore）。构建默认读 `cinema-ui/signing.jks`，也可用环境变量 `MEDIAPOCKET_KEYSTORE` 指到别处 |
| 用户电脑上的副本 | `%USERPROFILE%\Downloads\suiying-debug\signing.jks`（2026-10-19 放入）；用户 Windows 构建链原本也有一份 |
| 别名（alias） | `mediapocket` |
| 密码 | 密钥库和密钥都是 `android`（已在 build_ui.py 里：`--ks-pass pass:android --key-pass pass:android`） |
| 证书 | `CN=MediaPocket Local Preview`，RSA 2048，SHA256withRSA，有效期到 2054-02-26 |
| 证书 SHA-256 指纹 | `87a44d822580215fff01d604b1fedb31e52d69b42eb85ffbcd8b65aea21783d1` |
| 文件本身 SHA-256 | `5bc425608bd198d671f73924142d3e002830a2bf3362e4234b74350540191890` |
| 包名 | `com.guocheng.mediapocket.next` |

核对：
```
apksigner verify --print-certs 你的.apk        # Signer #1 certificate SHA-256 digest 必须等于上面的证书指纹
keytool -list -v -keystore signing.jks -storepass android   # 别名 mediapocket，SHA256 同上
```
规矩：
- **永远不要提交**签名文件，也不要上传到任何网盘、聊天、issue 或 PR。提交前 `git status` 看一眼有没有 `.jks`。
- 不要生成新签名、不要改别名或密码。找不到签名文件时停下来问用户，不要"先用一个临时签名打包"——那样装不上，用户只能卸载。
- 建议用户自己再备份一份到私人位置（不是公开仓库）。

## 6. 功能地图（类 → 做什么）

| 区域 | 主要类 |
| --- | --- |
| 首页 / 「我的」 | ScrapedMainActivity（入口，继承原版 MainActivity）、HomeChrome、HomeRules、HomeHeroRules、HomeAtmosphere、HeroSwipe、FadeFrame、FrostedBar、SettingsLayout / SettingsRules（「我的」分组）、HomeSources |
| 详情页 | DetailChrome（Stremio）、FusionLibraryActivity（影视仓片库与详情）、EmbyLibraryActivity（Emby 片库与详情），三者同一版式 |
| 片源 | StremioStreams / StremioSourceDialog、FusionCatalog / TvBoxPlay / PlaybackSitePicker / TvSpider*（影视仓）、Emby*（EmbyApi 纯逻辑、EmbyClient 网络、EmbyPlayback 开播与回报进度、EmbyCue 片头片尾 / 章节）、MagnetLinks / MagnetFiles（磁力合集选集）、TorrentProviders / PrivateTorrentAddons（他人 PR 加的，不要动） |
| 播放页 | PlayerChrome（挂件与 poll）、PlayerOverlay（顶 / 底栏、更多菜单、失败卡片）、PlayerSidePanel（字幕 / 音轨 / 选集或版本 / 线路 / 章节）、PlayerGestures（手势）、DecoderSwitch、AutoNext、PlaybackReviewRules（电影版本 vs 剧集）、PlaybackOrientation、FullscreenMode |
| 菜单与弹窗 | CinemaMenu（独立 Dialog 窗口的深色菜单）、CinemaDialog（AlertDialog.Builder 同款写法）、CinemaWait（替代 ProgressDialog） |
| 字幕 / 弹幕 | OnlineSubtitleSearch / OnlineSubtitleController / OnlineSubtitleClient（迅雷免密钥、射手、OpenSubtitles）、SourceSubtitleLoader、WhisperClient、DanmuController / DanDanPlay*（弹弹play，需用户自己的 AppId） |
| 网络 / 加速 | HeaderRoute（本地 127.0.0.1 代理，补 VLC 缺的请求头，m3u8 改写，Range 透传）、RangeAccelerator（多线程分段）、LinkProbe / PlaybackCheck（开播前检查地址）、TorrentBoost / VerifiedPieceReader（P2P） |
| 调试 | DebugLog / DebugText（崩溃 + 运行日志，网址只留主机名）：「我的 › 故障排查 › 调试信息」 |

## 7. 必须知道的坑（都真实踩过）

1. **播放页的触摸被原版吞掉**：`GesturePlayerActivity.dispatchTouchEvent` 只把落在播放器 root 里 Button / SeekBar 上的触摸交给视图，其余当画面手势。所以播放页上的自定义控件要么是 Button，要么放进独立窗口（CinemaMenu 就是 Dialog 窗口）。挂在 `android.R.id.content` 上的普通视图会"看得见点不动"。
2. **VLC 视频面初始化会把根视图重挂到最上层**，叠加层会被压住。
3. **AppFuse / ProxyFileDescriptorCallback.onRead 必须读满请求的字节数**，短读会被当成 EOF（P2P 起播卡住的原因）。
4. **libVLC 3.0.22 不支持 `:http-header`**，需要请求头的地址都走 HeaderRoute。
5. **Emby 令牌**只放在本服务器地址的 `api_key` 参数里，不放请求头（代理跟随 302 会把请求头带到网盘 / CDN）；外部主机地址不加 `api_key`。
6. **影视仓观看记录 id 是按集算的哈希**，按片子归并用 `HomeRules.showKey / dedupeHistory`。
7. 容器 `measure` 必须在子元素全部 add 之后；JDK 在中文 Windows 上 `\b` 正则对中文失效，数字边界用环视。
8. 纯逻辑放 `*Rules` 类（不 import Android），JVM 才能测；引用了 Android 类的类在 JVM 上加载会失败。

## 8. 外部服务与用户数据

- 小雅 AList：用户自托管，地址和令牌只在本地 `fusion-default.json` 和用户手机里。**不要写进仓库、文档、日志或提交信息**。
- Whisper 字幕服务：`cloud/whisper-srt/server.py`，部署在用户自己的服务器；令牌在服务器上，不进仓库。
- 弹弹play / 射手 / OpenSubtitles 的密钥：用户在 App「我的 › 字幕与弹幕」里自己填，代码里不内置任何人的密钥。

## 9. 用户的长期要求

- 用户已要求删除 TorrentClaw 专用入口和饭太硬等后来加的预置源，**不要重新添加**；保留用户自定义接口、插件和观看记录。
- 不能把"构建通过 / 配置可读 / 目录加载成功"当成"能播放"。每次交付写清楚：哪些在真机上验证过，哪些没有。
- 每次修改后给出可**覆盖安装**的 APK（同一签名，见第 5 节）。
- 用户希望直接把事做完，不要列一堆选项让他挑。
- 不要 force push 到 main；推送前检查没有签名文件、`fusion-default.json`、令牌、服务器 IP、本机用户目录路径。

## 10. 当前状态

- 本分支基于 main `18473e9`（0.1.130 / code 131），候选 0.1.131 / code 132，界面改版覆盖首页、详情、「我的」和播放页。
- 用户已明确只维护新版 `com.guocheng.mediapocket.next`，不再继续旧包候选。旧版本真机记录仅作历史证据，不能当作新版验收。
- 暖白陶土标题随包内置 Noto Serif SC Regular，许可与来源位于 `cinema-ui/assets/fonts/`。已有外观偏好保留；新装默认暖白陶土。
- 出包与真机验证结果以本版更新记录和 `cinema-ui/VALIDATION.json` 为准。大改使用独立分支与 PR，由用户合并。

## 多个 AI 协作规则（2026-10-22 起）

Claude、Codex、Cursor 会同时在这个仓库工作，此前出现过两次版本号相撞。约定：
1. 开工前先 `git fetch` 并基于最新 `origin/main`；动手前在 README 顶部看清当前版本号。
2. 版本号只在出包时才加，出包后**立刻推送**，不要在本地攒多个版本。下一个版本号 = main 上的版本 + 1。
3. 推送前再 `git fetch` 一次；main 有了别人的新提交就先合并、重新构建测试，再推。
4. 改动较大或会持续多轮时用分支 + PR，由用户合并；小修直接进 main。
5. 每次出包都要在 cinema-ui/README.md 顶部写更新记录，并跑 `scripts/run_tests.sh` 与 `verify_apk.py`。


## 界面改进待办（2026-10-22 参考 Findroid、Fladder、Streamyfin、Wholphin 整理）

已做（0.1.122–0.1.124）：Emby 详情页规格标签、操作按钮行、演职员、剧集缩略图卡片；拖动预览（两个播放页）；继续观看合并下一集与剧照卡片；全站加载骨架和图片淡入；更多类似；字幕样式；服务器管理列表。
0.1.124 补完：Jellyfin Trickplay 预览；HDR 片源字幕调暗；「我的」页概览卡片与分隔线；搜索 / 分类骨架。以下 7 项均已完成，剩余工作是真机验证。
待做，按价值排序：
1. 拖动进度条时显示画面预览（Emby BIF / Jellyfin Trickplay；仓库已有 BifIndex）。
2. 首页「继续观看」与「下一集」可合并为一行；卡片用横版剧照而不是竖版海报（Wholphin）。
3. 影视仓详情页同样改成剧集卡片 + 骨架（目前只改了 Emby）。
4. 首页、媒体库、搜索的加载都换成骨架；图片加载完成淡入。
5. 「更多类似」推荐行（Emby /Items/{id}/Similar）。
6. 字幕样式设置（字号、描边、底色、位置），HDR 片源单独一套（Wholphin）。
7. 「我的」页按 Material 3 列表样式重做；Emby 服务器管理页去掉整行大按钮，改成列表项 + 右侧菜单。
