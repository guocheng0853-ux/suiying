# 内置杜比播放路径

SystemPlayerActivity 改用 Media3VideoView：Media3 1.8.0 解复用 MP4/MKV/HLS，Android MediaCodec 负责解码，SurfaceView 输出。Media3 不是独立杜比软件解码器；设备必须提供对应 profile 的解码能力。

Emby 的 dolby.kind 随 stream 传入。确认 Profile 5 时拒绝普通 HEVC fallback，避免把非兼容底层当普通 HEVC 输出紫绿色。默认“每次询问”时，如果设备声明 P5 解码能力则先尝试内置；显式选择外部播放器/转码的偏好继续尊重。能力声明并不能保证该文件的分辨率、级别、音轨实际可播，错误与首帧仍须真机检查。

请求头、续播位置、Emby 进度回报保留。进入后台暂停，回来只恢复此前在播放的状态。这个独立页面仍是精简播放界面，尚未复用主播放页弹幕、外挂字幕和手势。

## 依赖与构建

build_fusion_runtime.py 先运行 fetch_media3.py，按 media3-dependencies.json 的固定上游 URL 和 SHA256 下载并提取依赖。下载文件不提交 Git。9 个依赖包含 Media3 的 common/container/datasource/decoder/extractor/database/exoplayer/hls 和 ExifInterface。它们采用 Apache-2.0；上游项目：https://github.com/androidx/media ，https://android.googlesource.com/platform/frameworks/support/ 。

构建时排除未使用的 DownloadNotificationHelper（唯一引用 AAR 字符串资源的类），不修改稳定 APK 的资源表；随影离线下载不使用这个类。应用现有依赖和底座提供 core、collection、Guava 等类。

## 本次验证边界

0.1.116 / versionCode 117：完整 APK 构建、签名校验、APK 类与组件检查通过；71 项 JVM 回归测试通过；Media3 依赖完整性检查通过。没有 Android 设备，因此未验证 P5 首帧、色彩、HDR 模式、硬件解码性能、TrueHD/DTS 音轨或实际网络文件。不能据此宣布手机杜比问题已解决。

真机验证应使用同一条失败片源：检查是否出现画面、颜色是否自然、声音是否正常、拖动与切后台是否恢复；日志记录视频 MIME、codecs、设备 P5 声明与错误码。设备不支持时需要服务器正确转码或选择兼容片源。

## 0.1.116 HTTP 反馈

用户真机返回 Media3 2004（HTTP 状态异常），不能归因于杜比解码失败。现在从异常因果链提取真实 HTTP 状态、区分 IO/容器/解码错误；启用与现有请求探测相同的默认 UA，并支持 HTTP/HTTPS 重定向。IO 失败提供重新获取 Emby 地址入口。截图没有给出实际 HTTP 状态，因此尚不能确定是鉴权、地址失效、重定向还是其他服务器响应。

## 0.1.117 — Media3 播放控件

杜比入口已补上独立控件，并读取原来的播放偏好：倍速、比例、快进步长、长按加速、亮度、高刷和状态栏。音轨/文字字幕切换直接操作 Media3；在线字幕搜索下载后加入当前 MediaItem。支持 Emby 选集/版本、弹幕入口、屏幕锁定及旋转。原来的 VLC 解码方式不控制 Media3。音效使用 Android 会话均衡器，设备不支持时明确提示。

验证：Java 全量构建、72 个 JVM 测试程序及 APK 依赖/签名检查；新增比例与跳转边界测试。未连接安卓实机，音效、手势和字幕需安装后确认。当前字幕控件支持文字字幕；不声明支持 PGS/VobSub 位图渲染或字幕时间偏移。
