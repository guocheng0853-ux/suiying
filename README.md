# 随影 MediaPocket

当前候选版本：0.1.146 / versionCode 147。修复分屏失焦时提示消失、自动恢复丢失服务器转码选择，以及短暂失焦耗尽重试机会的问题。构建与验收范围见 [本版记录](DEVICE-RECOVERY-0146.md)。继承 0.1.145 的体检与地址检查超时修复。

0.1.144 新增Emby失败后自动刷新一次播放信息，以及服务器「⋮ → 片源体检」五阶段检查、耗时和脱敏报告。自动恢复可取消，旋转保留体检结果。完整构建、回归与平板证据见 [0.1.144 验收](DEVICE-RECOVERY-0144.md)。

此前0.1.143的失败卡片与真实Emby 4K实播证据见 [片源恢复记录](EMBY-0143-RECOVERY.md)，0.1.142的手机精修证据见 [手机验收](DEVICE-POLISH-VALIDATION.md)；历史记录不代替本版新包的验证范围。

安卓播放器（Stremio 插件 + 影视仓配置，VLC 播放）。采用"基础 APK + Java 界面/功能扩展"的叠加构建方式，不是 Gradle 工程。

- 源码与说明：[`cinema-ui/`](cinema-ui/)，版本记录见 [`cinema-ui/README.md`](cinema-ui/README.md)
- 审查记录见 [`docs/handoff/`](docs/handoff/)
- 构建：`python3 cinema-ui/build_ui.py`（有 `signing.jks` 才签名）。没有密钥时必须写明 `python3 cinema-ui/build_ui.py --unsigned`，否则不会把未签名包装成正式文件名。需要同级 `build-tools/` 目录（见 `scripts/setup_build_tools.sh`，如有）
- 签名密钥 `cinema-ui/signing.jks` **不在仓库中**，需自行放回本地才能签出可覆盖安装的 APK。
