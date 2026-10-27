# 0.1.147 真实杜比、HDR 与音频排查

日期：2026-10-26。OPD2401 / Android 16，沿用0.1.147 / code148原签名包。只使用用户已有登录，不导出账号或令牌。公开记录使用匿名样本，不含真实服务地址。

## 已确认

| 场景 | 结果与证据边界 |
| --- | --- |
| 真实杜比兼容样本，默认播放 | 元数据4K/HEVC/TrueHD 7.1。实际默认内核是SystemPlayback，用户确认无声；切另一条AC3后仍无声。媒体未静音、音量非零，扬声器路由 |
| 同一杜比兼容样本，Media3 | 日志识别video/dolby-vision、dvhe.08.06、3840×1608，并记录首帧；20s准备提示弹出后，即使随后已有首帧，提示仍留在屏幕。未确认该路径声音 |
| 真实HDR10样本，VLC | 元数据HEVC 10-bit、PQ/smpte2084、BT.2020、3840×1608，默认EAC3音轨。确认实际实例为VlcPlayback、硬件视频模式，画面已渲染、进度18.034→67.221s；用户确认有声音 |
| 真实P5样本，Media3 | 元数据DoviProfile50，3840×1608，含DTS/EAC3/AAC/FLAC音轨；实际Media3VideoView、严格P5约束=true。日志识别dvhe.05.06并记录首帧，进度4.207→5.988→13.736s；频繁缓冲。2026-10-27重新单独核对后，实际解码器为c2.qti.dv.decoder，音轨audio/flac、c2.android.flac.decoder；用户明确确认“有声音，颜色也是正常”。此路径的主观声画验收通过，持续流畅性仍受缓冲影响 |
| 真实服务器转码 | 已实际进入transcode_fallback=true、direct=false路径，但未出帧并最终terminal=true。账号Policy明确EnableAudioPlaybackTranscoding=false、EnableVideoPlaybackTranscoding=false、EnablePlaybackRemuxing=false，播放本身EnableMediaPlayback=true；本账号没有授权音视频转码，不能宣称成功转码或色调映射通过 |

这些是实际文件和解码器证据，不是合成H.264，也不只依据窗口类。截图不能单独证明HDR亮度、色调映射或色准正确；本轮没有仪器对照。

## 音频与入口问题

- Android MediaCodecList实际声明AAC、FLAC、Opus等解码器，没有声明TrueHD、AC3、EAC3或DTS解码器。VLC具有独立音频解码路径，真实HDR/EAC3样本已经由用户确认出声。
- `SystemPlayerActivity.toVlc()`只返回原PlayerActivity Intent，原底座又按`useVlc`偏好选择内核；当前原偏好为false，实际仍可回到SystemPlayback。按钮名称不保证真的切到了VLC，需要修复并核对实际实例。
- Media3准备超过20s后弹出的“尚未显示画面”提示，后续收到首帧时未自动收起；需要避免已经播放仍显示过期的准备提示。

## 本轮接续

服务器转码受账号权限限制，本轮不修改服务器权限，也不重复尝试绕过限制。后续回退直连的单独GET探测返回403（text/html）；该探测不带播放器完整请求头，不能冒充生产转码请求的HTTP结果。优先按账号明确关闭转码这一事实记录阻碍。

真实播放排查已完成上述范围；接续处理O2分屏裁切、O1侧栏返回，以及本轮确认的VLC入口/无声兼容问题，再统一签名构建和复验。真实P5严格Media3路径的主观声画已由用户确认；持续流畅性、仪器级HDR色准以及服务器转码成功仍不在通过范围。

此前“偏色、无声”的晚到反馈未能唯一对应到严格Media3路径：期间转码失败后曾自动落回SystemPlayback直连P5，且样本发生切换。保留该失败记录，但不能用它否定本次明确隔离的P5验收，也不能断言它必然来自某个内核。客户端将阻止P5转码失败后静默回退原片。

本地证据在忽略目录`deliverables/tablet-0148/`。临时验收工具与正式应用同证书，只在设备内使用原有授权读取规格、核对实际对象与驱动原有播放方法，控制端只接收脱敏技术字段；测试结束恢复改过的播放偏好、卸载临时工具、移除专用ADB forward并删除临时文件。系统字号、density、输入法等不在本轮修改范围。
