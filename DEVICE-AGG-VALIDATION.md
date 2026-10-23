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
| 横屏、播放、VLC / Media3、杜比 | 未验证 | 不在本轮范围，不能从安装与首页通过推断播放通过 |

其他边界：一张继续观看海报 content-desc 为 HTTP 500（Emby 服务端单集图片错误），应用降级显示、卡片仍可点，属预期容错。原始截图、UI XML、logcat 仅保存在忽略的本地交付目录 `deliverables/device-validation/`，可能含私人内容，未提交仓库。手机保持竖屏锁定，高刷设置未改动。
