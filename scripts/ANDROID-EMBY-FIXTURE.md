# 本机 Emby 验收服务

`android_emby_fixture.py`只监听电脑loopback，提供合成登录、媒体库、播放信息和测试MP4。用于验证恢复预算与体检行为，不需要真实账号。

1. 准备一段普通MP4测试片（40秒左右便于观察进度）。启动：`python scripts/android_emby_fixture.py --video PATH_TO_TEST_MP4 --port 37813`。
2. 对目标Android设备执行`adb -s SERIAL reverse tcp:37813 tcp:37813`。
3. 应用中添加`http://127.0.0.1:37813`，用户名和密码均用`fixture`。服务器名称会显示`Local recovery fixture`。
4. 用以下本机控制接口设置场景，再在应用中执行体检或播放该测试片。所有凭据都是合成值；不要让服务监听公网。

| 接口 | 用途 |
| --- | --- |
| `POST /__fixture__/reset` | JSON包含`mode`、可选`delay`、`health_delay`、`health_body_delay`、`auth_status`，同时清零计数 |
| `GET /__fixture__/state` | 查看播放信息、诊断信息、视频读取和播放开始计数 |
| `POST /__fixture__/shutdown` | 关闭本机服务，JSON空对象即可 |
| `POST /__fixture__/configure` | 与reset相同的模式/参数，但保留计数、事件和时钟；未传参数回默认值 |

`mode=health`始终返回可用数据；`recover`首次播放信息指向503，后续返回MP4；`fail`始终503；`delayed`在第二次播放信息请求等待`delay`秒再恢复。`health_delay`延迟公开信息响应头；`health_body_delay`先发送响应头和首字节，再延迟剩余正文，二者需分别验收。`auth_status=401`模拟诊断登录失效。

`health_public_requests`、`health_body_started`与对应`events.at_ms`用于区分请求到达和正文开始，时间相对最近一次reset，不能与客户端单调时钟直接相减。连续取消/重启的正文停顿复现与平台范围见`docs/handoff/continued-0146-http.md`和`docs/handoff/continued-test-0146.md`。

0.1.147复测辅助：`health_body_client_closed`和`playback_client_closed`通过socket peek记录EOF/reset，不消费请求字节；configure用于保留这些事件。`catalog=series`提供Shows/Recovery series/Season 1和两集合成剧集；`playback_conflicts=1`让下一次实际PlaybackInfo返回409，之后恢复。事件只含合成item/session、响应码和相对时间。新增能力可用`python3 scripts/fixture_smoke_0147.py PATH_TO_TEST_MP4`独立烟测（证明服务能力，不证明Android修复）。平板通过标准见[acceptance-0147-pass-criteria.md](../docs/handoff/acceptance-0147-pass-criteria.md)；现场接续见[本机交接](../docs/handoff/handoff-0147-local-acceptance.md)。

关键断言：体检不发送播放开始；自动恢复最多有首次+一次刷新这两次播放信息请求；持续失败不继续刷新；取消后即使延迟响应到达，也不启动播放器；旋转完成的体检结果不重复请求。

结束后在应用中删除合成服务器，关闭本机服务，执行`adb -s SERIAL reverse --remove tcp:37813`并恢复测试前方向。保留用户其他服务器和设置。JSON结果、截图及本机测试视频放在忽略的build/deliverables目录。

## 0.1.146 失焦与分屏回归

- 设置 `mode=delayed, delay=5`，打开测试片。首次播放信息返回失效地址，第二次请求是自动刷新；在第二次请求尚未完成时展开通知栏，确认播放器失焦，再收起。应允许第三次播放信息请求，最终只启动一次播放。VLC 和「系统解码重播」路径分别验证，Media3 恢复后仍应留在 Media3。通过画面和播放进度验证解码，不能只看视频请求计数。
- 设置 `mode=delayed, delay=25`，在自动刷新时点「取消重连」，等超过25秒。应维持2次播放信息请求、0次播放开始，迟到响应不打开新播放器，轮询不自动重启。
- 设置 `mode=fail`，等待首次+自动刷新均失败，再展开/收起通知栏，追加观察12秒。应维持2次播放信息请求，已完成的失败不能退还次数。
- 分屏打开测试片后把焦点移到另一半应用：连接提示与终止失败卡片仍应可见；没有焦点时不自动刷新。点回失败卡片后「选择片源」应能打开线路面板。该场景需保存实际分屏画面与窗口焦点证据。
- 合成视频为普通H.264，不验证杜比P5或真实服务器转码兼容性。转码选择保留需要另用合适片源验收；没有条件时明确记录只有源码与JVM覆盖。

## 继续验收的边界流程

- 手动取地址路径：`fail`下进入「系统解码重播」，等Media3失败对话框；服务切`health`后点「重新获取地址」，记录恢复前后Activity。`PlayerActivity`按偏好可用不同原生引擎，窗口类不能直接证明VLC。
- 取消后的队列：`delayed, delay=25`下等第二次播放信息挂起，点取消；服务切`health`，立即手动重连。先定位按钮再开始计时，与健康开播使用相同计时方式比较。25秒长于客户端15秒读取超时，这项测试的是旧请求超时前是否阻塞新任务。
- 退出后的响应：另用`delayed, delay=8`，在第二次请求挂起时退出，确认回到EmbyLibraryActivity而非仅被弹窗覆盖，追加观察10秒；记录是否重开及播放开始计数。
- 自动换源：记录原值，关闭后在`fail`下观察终止失败后至少12秒，应只有首次播放信息请求；恢复原值，并从新建播放器再次核对。侧栏可点外部空白关闭，系统返回键的实际导航行为单独记录。
- 体检旋转/返回：使用`health_delay=6`，进行中旋转应取消旧检查后重启；完成后旋转应保留逐项结果/耗时且不重复发请求。另在进行中返回服务器列表，确认迟到响应不回写旧页。
