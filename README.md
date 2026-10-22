# 随影 MediaPocket

当前版本：0.1.130 / versionCode 131。详见[更新记录](cinema-ui/README.md)。0.1.108 的既有验证结果保留于[真机记录](DEVICE-VALIDATION.md)，完整清单仍未完成。

安卓播放器（Stremio 插件 + 影视仓配置，VLC 播放）。采用"基础 APK + Java 界面/功能扩展"的叠加构建方式，不是 Gradle 工程。

- 源码与说明：[`cinema-ui/`](cinema-ui/)，版本记录见 [`cinema-ui/README.md`](cinema-ui/README.md)
- 构建：`python3 cinema-ui/build_ui.py`（有 `signing.jks` 才签名）。没有密钥时必须写明 `python3 cinema-ui/build_ui.py --unsigned`，否则不会把未签名包装成正式文件名。需要同级 `build-tools/` 目录（见 `scripts/setup_build_tools.sh`，如有）
- 签名密钥 `cinema-ui/signing.jks` **不在仓库中**，需自行放回本地才能签出可覆盖安装的 APK。
