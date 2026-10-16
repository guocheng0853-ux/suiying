# 随影 MediaPocket

安卓播放器（Stremio 插件 + 影视仓配置，VLC 播放）。采用"基础 APK + Java 界面/功能扩展"的叠加构建方式，不是 Gradle 工程。

- 源码与说明：[`cinema-ui/`](cinema-ui/)，版本记录见 [`cinema-ui/README.md`](cinema-ui/README.md)
- 构建：`python3 cinema-ui/build_ui.py`，需要同级 `build-tools/` 目录（见 `scripts/setup_build_tools.sh`，如有）
- 签名密钥 `cinema-ui/signing.jks` **不在仓库中**，需自行放回本地才能签出可覆盖安装的 APK。
