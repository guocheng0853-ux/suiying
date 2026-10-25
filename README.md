# 随影 MediaPocket

当前候选版本：0.1.142 / versionCode 143。基于 0.1.141 全页 Impeccable 精修：按钮对比、48dp 触控、设置长值与无障碍、登录标签、空态及跨页主题。已同签名装机，VLC 与 Media3 真实视频及进度验证通过，完整边界见 [本版验收](DEVICE-POLISH-VALIDATION.md)，设计评审见 [全页 critique](docs/handoff/critique-all-0141.md)。

安卓播放器（Stremio 插件 + 影视仓配置，VLC 播放）。采用"基础 APK + Java 界面/功能扩展"的叠加构建方式，不是 Gradle 工程。

- 源码与说明：[`cinema-ui/`](cinema-ui/)，版本记录见 [`cinema-ui/README.md`](cinema-ui/README.md)
- 审查记录见 [`docs/handoff/`](docs/handoff/)
- 构建：`python3 cinema-ui/build_ui.py`（有 `signing.jks` 才签名）。没有密钥时必须写明 `python3 cinema-ui/build_ui.py --unsigned`，否则不会把未签名包装成正式文件名。需要同级 `build-tools/` 目录（见 `scripts/setup_build_tools.sh`，如有）
- 签名密钥 `cinema-ui/signing.jks` **不在仓库中**，需自行放回本地才能签出可覆盖安装的 APK。
