# 随影 MediaPocket

底栏候选：0.1.101 / code 102（`codex/bottom-nav-0101`），已构建及测试，尚未安装真机；变化见 [更新记录](cinema-ui/README.md)。

当前 main：0.1.100 / code 101。Emby 遇到只允许官方客户端的服务器时，会改用 `Client="Emby"` 再登录一次；真机尚未安装这个版本。验证范围见 [更新记录](cinema-ui/README.md)，此前的真机清单见 [真机验证记录](DEVICE-VALIDATION.md)。

安卓播放器（Stremio 插件 + 影视仓配置，VLC 播放）。采用"基础 APK + Java 界面/功能扩展"的叠加构建方式，不是 Gradle 工程。

- 源码与说明：[`cinema-ui/`](cinema-ui/)，版本记录见 [`cinema-ui/README.md`](cinema-ui/README.md)
- 构建：`python3 cinema-ui/build_ui.py`，需要同级 `build-tools/` 目录（见 `scripts/setup_build_tools.sh`，如有）
- 签名密钥 `cinema-ui/signing.jks` **不在仓库中**，需自行放回本地才能签出可覆盖安装的 APK。
