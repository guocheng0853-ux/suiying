# 随影 MediaPocket

当前候选版本：0.1.140 / versionCode 141。播放器仍是方向 2 的三层悬浮岛，这一轮只精修亮场对比、48dp 热区、正中少抢戏、底栏蒸馏和影院暗金强调色。0.1.139 是三层悬浮面板，0.1.138 继续观看海报兜底已真机验证。详见[更新记录](cinema-ui/README.md)。0.1.131 暖纸与陶土的验证结果保留于[暖纸记录](DEVICE-PAPER-VALIDATION.md)，0.1.108 的既有验证结果保留于[真机记录](DEVICE-VALIDATION.md)，完整清单仍未完成。

安卓播放器（Stremio 插件 + 影视仓配置，VLC 播放）。采用"基础 APK + Java 界面/功能扩展"的叠加构建方式，不是 Gradle 工程。

- 源码与说明：[`cinema-ui/`](cinema-ui/)，版本记录见 [`cinema-ui/README.md`](cinema-ui/README.md)
- 审查记录见 [`docs/handoff/`](docs/handoff/)
- 构建：`python3 cinema-ui/build_ui.py`（有 `signing.jks` 才签名）。没有密钥时必须写明 `python3 cinema-ui/build_ui.py --unsigned`，否则不会把未签名包装成正式文件名。需要同级 `build-tools/` 目录（见 `scripts/setup_build_tools.sh`，如有）
- 签名密钥 `cinema-ui/signing.jks` **不在仓库中**，需自行放回本地才能签出可覆盖安装的 APK。
