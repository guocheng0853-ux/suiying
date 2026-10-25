# 本机 Emby 验收服务

`android_emby_fixture.py`只监听电脑loopback，提供合成登录、媒体库、播放信息和测试MP4。用于验证恢复预算与体检行为，不需要真实账号。

1. 准备一段普通MP4测试片（40秒左右便于观察进度）。启动：`python scripts/android_emby_fixture.py --video PATH_TO_TEST_MP4 --port 37813`。
2. 对目标Android设备执行`adb -s SERIAL reverse tcp:37813 tcp:37813`。
3. 应用中添加`http://127.0.0.1:37813`，用户名和密码均用`fixture`。服务器名称会显示`Local recovery fixture`。
4. 用以下本机控制接口设置场景，再在应用中执行体检或播放该测试片。所有凭据都是合成值；不要让服务监听公网。

| 接口 | 用途 |
| --- | --- |
| `POST /__fixture__/reset` | JSON包含`mode`、可选`delay`、`health_delay`、`auth_status`，同时清零计数 |
| `GET /__fixture__/state` | 查看播放信息、诊断信息、视频读取和播放开始计数 |
| `POST /__fixture__/shutdown` | 关闭本机服务，JSON空对象即可 |

`mode=health`始终返回可用数据；`recover`首次播放信息指向503，后续返回MP4；`fail`始终503；`delayed`在第二次播放信息请求等待`delay`秒再恢复。`health_delay`延迟公开信息接口，便于测试取消；`auth_status=401`模拟诊断登录失效。

关键断言：体检不发送播放开始；自动恢复最多有首次+一次刷新这两次播放信息请求；持续失败不继续刷新；取消后即使延迟响应到达，也不启动播放器；旋转完成的体检结果不重复请求。

结束后在应用中删除合成服务器，关闭本机服务，执行`adb -s SERIAL reverse --remove tcp:37813`并恢复测试前方向。保留用户其他服务器和设置。JSON结果、截图及本机测试视频放在忽略的build/deliverables目录。
