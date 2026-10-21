# 内置杜比播放路径

SystemPlayerActivity 改用 Media3VideoView：Media3 1.8.0 解复用 MP4/MKV/HLS，Android MediaCodec 负责解码，SurfaceView 输出。Media3 不是独立杜比软件解码器；设备必须提供对应 profile 的解码能力。

Emby 的 dolby.kind 随 stream 传入。确认 Profile 5 时拒绝普通 HEVC fallback，避免把非兼容底层当普通 HEVC 输出紫绿色。默认“每次询问”时，如果设备声明 P5 解码能力则先尝试内置；显式选择外部播放器/转码的偏好继续尊重。能力声明并不能保证该文件的分辨率、级别、音轨实际可播，错误与首帧仍须真机检查。

请求头、续播位置、Emby 进度回报保留。进入后台暂停，回来只恢复此前在播放的状态。这个独立页面仍是精简播放界面，尚未复用主播放页弹幕、外挂字幕和手势。

## 依赖与构建

build_fusion_runtime.py 先运行 fetch_media3.py，按 media3-dependencies.json 的固定上游 URL 和 SHA256 下载并提取依赖。下载文件不提交 Git。9 个依赖包含 Media3 的 common/container/datasource/decoder/extractor/database/exoplayer/hls 和 ExifInterface。它们采用 Apache-2.0；上游项目：https://github.com/androidx/media ，https://android.googlesource.com/platform/frameworks/support/ 。

构建时排除未使用的 DownloadNotificationHelper（唯一引用 AAR 字符串资源的类），不修改稳定 APK 的资源表；随影离线下载不使用这个类。应用现有依赖和底座提供 core、collection、Guava 等类。

## 本次验证边界

0.1.115 / versionCode 116：完整 APK 构建、签名校验、APK 类与组件检查通过；70 项 JVM 回归测试通过；Media3 依赖完整性检查通过。没有 Android 设备，因此未验证 P5 首帧、色彩、HDR 模式、硬件解码性能、TrueHD/DTS 音轨或实际网络文件。不能据此宣布手机杜比问题已解决。

真机验证应使用同一条失败片源：检查是否出现画面、颜色是否自然、声音是否正常、拖动与切后台是否恢复；日志记录视频 MIME、codecs、设备 P5 声明与错误码。设备不支持时需要服务器正确转码或选择兼容片源。
