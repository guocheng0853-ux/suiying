# 0.1.165 下载缺陷修正

日期：2026-10-30。基线 main `5e9623f`，0.1.164 / code 165。本版 0.1.165 / code 166。对照 `docs/handoff/review-0164.md`（分支 `codex/review-0164`）里的下载问题；根因按代码核对，不沿用报告里的猜测原文。

## 改了什么

- **线路记错（P1）**。`episodeChoices` 仍按集去重、只留第一条线路，选集列表不改。开播（`FusionLibraryActivity.play`）和播放器内换源（`PlaybackSitePicker.stream` / `apply`）把当前 `config/site/flag/value` 写进正在播的 stream，并同步到 meta。`PlaybackDownload.source` 转发到 `OfflineQueueRules.downloadSource`：这份身份优先于 `meta.videos`。flag 和 value 必须来自同一份，避免用第一条线路的 value 配上当前线路的 flag。
- **续传不拼接另一条线路**。`OfflineRules.resumeOffset`：来源路径不同（查询参数变化仍算同一文件）时，即使服务器回了 206 且总长度相同，也不把新内容接到已有文件后面。过期重取之后，还要新旧总长度都已知且相等才续传；对不上或还不知道总长度，就删掉已有片段从头下。`OfflineService` 用这个决定，不再凡是 206 就追加。
- **取消后不再入队（P2）**。解析返回后调用 `OfflineQueueRules.accept`。已经取消则丢掉这一集并停止本批；取消前已经放进 ready 的仍会 `commitAll`。
- **汇总计入事先跳过的集（P2）**。`scope` 把「下载本季全部」和「从当前集往后全部」里被置灰的原因留给汇总。一集都不能下时直接弹出「已加入 0 集，跳过 m 集」和原因，不再只说「还没有选择剧集」。手动「下载所选」不把没勾的集算进跳过。
- **错误地址脱敏（P2）**。`redact` 不再用 `\b`。汉字紧贴、括号、换行后的 http(s)/rtsp/rtmp 只留主机名；磁力换成「磁力」。字母紧贴在协议前面的（如 `nothttps://`）仍不当成地址。
- **0.1.164 文档**。README、`PROJECT-GUIDE.md`、`docs/handoff/playback-download-0164.md` 与 `VALIDATION.json` 对齐：该版 Windows 签名包已记录。没有把 JVM 结果写成真机菜单验收，也没有在本版重新核对证书。

磁力 / 种子、m3u8、DASH、只允许转码的 Emby 仍拒绝。播放页下载仍不调用 `requestPermissions`，通知权限推迟逻辑未改。

## 怎么验证

在 `cinema-ui/` 已有 `build/classes`、`build/stubs`、`build/runtime-classes` 时：

```sh
sh scripts/run_tests.sh
```

关注：

- `OfflineQueueRulesTest`：从 routeB 开播，`downloadSource` 得到 `flag=routeB,value=valueB`，而不是去重列表里的 routeA；取消返回时第二集不入队；整季范围的汇总含「跳过 2 集」和原因；`连接失败https://…?token=` 与括号里的地址只留主机名。
- `OfflineDownloadRulesTest`：`resumeOffset(false, …)` 在总长度相同的 206 上返回 0。

本版没有打签名 APK，没有跑 `verify_apk.py`，没有在 OPD2401 或手机上点下载、取消或续传。签名、包名、keystore 未改。
