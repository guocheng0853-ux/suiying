# 随影 MediaPocket

当前候选版本：0.1.143 / versionCode 144。播放失败提示移至底部安全区，重做标题、说明与「重新连接 / 选择片源」双操作卡片。已同签名安装到 OPD2401 平板，详见 [平板验收](DEVICE-TABLET-VALIDATION.md)。继承 0.1.142 的全页精修；该版手机 VLC / Media3 实播证据见 [手机验收](DEVICE-POLISH-VALIDATION.md)，不代替本版平板实播结果。

安卓播放器（Stremio 插件 + 影视仓配置，VLC 播放）。采用"基础 APK + Java 界面/功能扩展"的叠加构建方式，不是 Gradle 工程。

- 源码与说明：[`cinema-ui/`](cinema-ui/)，版本记录见 [`cinema-ui/README.md`](cinema-ui/README.md)
- 审查记录见 [`docs/handoff/`](docs/handoff/)
- 构建：`python3 cinema-ui/build_ui.py`（有 `signing.jks` 才签名）。没有密钥时必须写明 `python3 cinema-ui/build_ui.py --unsigned`，否则不会把未签名包装成正式文件名。需要同级 `build-tools/` 目录（见 `scripts/setup_build_tools.sh`，如有）
- 签名密钥 `cinema-ui/signing.jks` **不在仓库中**，需自行放回本地才能签出可覆盖安装的 APK。
