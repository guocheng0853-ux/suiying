# 0.1.136 聚合首页验证

日期：2026-10-23。包名 `com.guocheng.mediapocket.next`，versionCode 137。仅维护新版；旧包记录不作本版通过依据。

## 构建与测试

- 基于 main `60ef75e`（PR #27 合并，0.1.136 聚合首页）。manifest、README、VALIDATION.json、产物名四处版本号一致（0.1.136 / 137）。
- Windows 全量重建（独立工作树，`cinema-ui/build/build_win.py` 为本机移植脚本，不提交）：fetch_media3 本地校验、stubs / runtime / src 三层编译、host-dex 与 UI dex、六个 base dex 补丁（ClawNet / SubtitleSearch / HeaderRoute / TorrentBoost / PieceReads / PlaybackOrientation）全部由校验器 PASS，原方法体逐指令保留。
- 86/86 JVM 测试通过（首跑 3 项失败为测试进程工作目录错误：OfflineHttpTest、PaperPolishTest、PlayerMotionRulesTest 以相对路径读源码，须从 `cinema-ui/` 目录运行；从正确目录复跑全部通过，未改任何测试或时限）。
- 稳定资产逐字节保留（lib/、assets/、resources.arsc 与未补丁 dex），ZIP 自检、16KB 页对齐（zipalign -P 16）、apksigner verify 通过。
- 按项目指定证书签名，指纹 SHA-256 `87a44d822580215fff01d604b1fedb31e52d69b42eb85ffbcd8b65aea21783d1`。
- APK：`MediaPocket-0.1.136-agg-home-arm64.apk`（38,414,901 字节），SHA-256 `c295618f1437581ee39916f644354b0ededeee08534ab306870ed542e1d3b6d5`。本地交付，不提交公开仓库。

## 真机

设备：一加 PJZ110，Android 17。从 0.1.134（code 135）同签名覆盖升级至 0.1.136（adb install -r，Streamed Install 成功）。没有卸载、清空数据或改动服务器与插件。安装时应用在前台，升级后自动重启并停留在聚合首页。

| 项目 | 结果 | 证据与边界 |
| --- | --- | --- |
| 覆盖安装与启动 | 通过 | install -r Success；dumpsys 显示 versionName 0.1.136 / versionCode 137 |
| 聚合首页分区渲染 | 通过 | 006/007 XML + 001–005 截图：继续观看、最近更新、电影、动画剧集、动画电影、纪录片各行内容独立取片（分行不再从"最近更新"切取）；海报标题/年份/评分齐全，12 秒时各行已加载完成，无灰块空卡 |
| 大图轮播与来源 | 通过 | 大图自动换页（VisionQuest → Carrie），"今日推荐 · 剧集"标签、★/年份/类型/简介/查看详情/收藏完整；12 秒截图大图左上有 EMBY 角标。来源优先级取片规则由 JVM 测试覆盖，真机只确认了角标与换页 |
| 继续观看 | 通过 | East of Eden（S1E2 · 剩 32 分钟）与 东大高武学院（S1E14 · 剩 13 分钟）两张独立卡片；标题去重、续集不并入正片、看完项沉底由 HistoryDedupeTest / PosterSampleTest 等 JVM 测试覆盖，真机未见重复项 |
| 海报与缓存刷新 | 通过（渲染层面） | 各行海报正常显示；"按签名刷新"的缓存级行为未在真机单独插桩验证 |
| 详情页跳转 | 通过 | 点继续观看卡片进入 East of Eden 详情：★8.1 / 59 min / 类型 / 选集 / 音轨 / 字幕 / 返回首页均正常 |
| 稳定性 | 通过 | 全程同一 PID 存活；logcat 无本应用 FATAL / AndroidRuntime 崩溃（日志中一处 SIGABRT 属于验证工具 app_process，非应用） |
| 下拉刷新 | 未单独确认 | 已存刷新瞬间截图（136-008），提示文字未能在 dump 中捕获；核心逻辑未在本版改动 |
| 播放管线与失败处理 | 部分 | PlayerActivity 正常打开（Emby 与 Stremio 条目均试），媒体会话与系统通知/锁屏控制集成正常（SystemUI 日志可见"东大高武学院 · S1E14"条目），源失败卡片"没连上这个片源，可以换一个试试"+换下一个源/选择片源、线路面板（自动换源/每源最长等待）、集数与来源记忆、详情页"继续播放"直达按钮全部工作；退出播放干净、会话正常释放 |
| 真实片源画面输出 | 未完成 | 外部源当晚不可用：用户 Emby 服务器持续 5xx/超时（流连不上，多轮重试约 1 分钟未出画面）；Stremio 种子源 90–120 秒窗口内未连上；手机网络仅约 200–300 KB/s，低于种子源约 11 Mbps 码率。不能据此认定播放通过，也不构成应用缺陷证据 |

## 过程中遇到的问题与错误记录

应用侧（当晚真机实测观察，多为外部源问题，应用容错行为一并记录）：

1. **Emby 服务器 5xx/超时**：聚合首页三张继续观看卡片中两张的海报 content-desc 为 `HTTP 500` / `连接或读取超时`（单集图片接口），卡片降级显示、可点击、元数据行正常；Emby 详情页海报同样 `HTTP 500`。属服务器侧故障，应用未崩溃。
2. **Emby 流打不开**：「▶ 继续播放」（4K HEVC 集）进入播放页后约 1 分钟未出画面，报"没连上这个片源，可以换一个试试"；线路面板显示服务器仅一条线路，无插件额外线路，换源与自动换源均无法救回。与 1 同源（服务器当晚不可用）。
3. **Stremio 种子源未能在窗口期内起播**：选集后弹出选源面板（8/8 插件、约 30 条目，P2P 源含做种数与码率信息）；记录的来源与自动轮换的多个 P2P 源在 90–120 秒内未进入播放（媒体会话保持 PAUSED/position 0）。与当晚网络带宽（约 200–300 KB/s）明显低于源码率（约 11 Mbps）相符。
4. **媒体会话状态粒度**：播放准备/失败期间媒体会话 active=true 但始终 PAUSED(2)/position 0，系统通知面板随即可见条目；对用户无误导，但无法用会话位置区分"缓冲中"与"失败"，本次以失败卡片 UI 为准。

工具与方法侧（影响取证效率，不影响应用结论）：

5. **uiautomator dump 在非空闲页不可靠**：首页轮播、播放页"正在准备"动画进行时 dump 失败或返回旧缓存文件（本次曾因此误判页面未跳转、误触下一集）。改进：dump 前先删设备上旧文件，连续两次结果一致才采信。
6. **SafeUiDump（app_process + UiAutomation）在本机 Android 17 被 ABRT**：shell 侧自绘 dump 通道不可用，回退原生 uiautomator。
7. **Git Bash 路径转换**：adb shell 参数中的 `/sdcard/...` 会被 MSYS 转换成 Windows 路径导致命令落空，需 `MSYS_NO_PATHCONV=1`。
8. **JVM 测试工作目录**：OfflineHttpTest、PaperPolishTest、PlayerMotionRulesTest 以相对路径读源码，必须从 `cinema-ui/` 目录发起测试，否则 3 项误报失败（已复跑全绿，测试本身无缺陷）。
9. **构建警告（无害）**：aapt2 对含非 ASCII 字符（中文用户名）的本机路径报 `Failed to stat` 警告后仍成功完成链接与资源断言。

原始截图（136-001 至 136-015）、UI XML、logcat 仅保存在忽略的本地交付目录 `deliverables/device-validation/`，可能含私人内容（含服务器地址与片源信息），未提交仓库。手机保持竖屏锁定，高刷设置未改动；测试期间应用进程自始至终同一 PID，无重启。
