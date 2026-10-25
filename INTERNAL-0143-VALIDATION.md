# 0.1.143 本机内测补验

日期：2026-10-25。基线提交 `10c2da0`，同一0.1.143 APK，未修改应用代码、重新打包或再次安装。用户需要使用平板，因此停止设备操作，以下均为Windows本机测试，不能代替平板按钮实测。

## 自动化结果

- 完整编译stubs、runtime、src和tests，执行89个JVM测试程序：首轮88通过，`RangeAcceleratorTest` 在180秒时超时，退出码1。没有放宽时间或通过条件。
- 仅隔离重跑该失败项，退出码0：单连接8.2秒，分段代理2.1秒，约4.0倍；SHA-256一致，中间Range、小Range及不支持Range的回退均通过，经代理完整解码400帧、11.6MB MP4。首次超时原因未确认，保留为稳定性待观察项，不能写成首轮89/89无失败。
- `HeaderRouteTest`：不完整请求头触发403，完整代理路径成功解码150帧AES-HLS；主/子列表、密钥与分片请求、MP4 Range/206、媒体与403/404/网页/JSON/网络失败区分均通过。
- `PlaybackRegressionTest`：拒绝错误Content-Range，保留正确分块206、字幕鉴权及元数据；慢滴流探测在1000ms预算内结束。
- `PlaybackFailureRulesTest` / `PlaybackReviewRulesTest` / `EmbyResumeRulesTest`：卡片安全区、窄屏、短横屏、48dp，正常结束/失败区分，同片重播保持进度均通过。
- Media3离线依赖测试通过：完整依赖无需联网；损坏或缺失文件被拒绝。
- 同一APK重新完成唯一类、组件方法ABI、Manifest/版本、运行时资产、原生库和原版代码保留检查，通过。校验脚本临时生成的元数据已恢复为真实设备验收记录。

## 重连入口核对

直接读取原版DEX，确认 `PlayerActivity.retryCurrentDecoder()V` 和实际 `StartupBudget.manualRestart(long)V` 存在；前者确有重置prepared/rendered/terminal、停止旧播放器、重建VLC实例及以当前URL调用setUrl的指令。此为静态字节码证据，不证明Android按钮点击或Emby服务端重连已成功。

## 边界与本地记录

平板“重新连接”点击验证仍待完成，真实Emby来源实播仍未通过；按用户要求不继续占用平板。日志保留在忽略目录 `cinema-ui/build/internal-0143-tests.log`、`internal-0143-range-retry.log`、`internal-0143-apk.log` 和各测试类日志，不提交本地设备日志或私有配置。
