# 0.1.146 继续测试：体检 HTTP 取消与连续超时

日期：2026-10-26。应用基线 `d8f7680`，版本0.1.146 / code147。测试使用该版已编译的 `EmbyHealthHttp`，没有改应用运行时代码、版本号或公共构建目录。运行环境为桌面 OpenJDK 17.0.19，本页没有 Android 真机结论。

## 已确认问题：正文停顿时取消仍占用 worker（L2）

**状态：桌面 JVM 3/3复现，修复未实施；Android证据见本轮主记录。**

可重复条件：

1. 本机 HTTP 服务立即发送200响应头、声明100字节正文，并发送/flush首字节，然后保持连接、不发送剩余正文。
2. 建立与 `EmbyDiagnostics.IO` 相同大小的2线程池，用两个独立 `EmbyHealthHttp` 发起默认10000ms预算请求。
3. 等两者都进入正文读取，追加等待200ms，然后分别执行 `http.cancel()` 与对应 `future.cancel(true)`。
4. 立刻向同一线程池提交一个能马上返回200的快速请求。分别记录取消调用返回、原worker实际退出，以及快速请求开始执行的时间。不能以 `Future.isCancelled()` 代替worker退出证据。

| 轮次 | 取消调用耗时 | 取消后worker1退出 | 取消后worker2退出 | 快速请求排队 | 快速请求结果 |
| --- | ---: | ---: | ---: | ---: | --- |
| 1 | 0ms | 9782ms | 9782ms | 9783ms | 200 |
| 2 | 0ms | 9794ms | 9794ms | 9794ms | 200 |
| 3 | 1ms | 9795ms | 9795ms | 9795ms | 200 |

三个场景的两个Future都立即进入cancelled状态，实际worker都到原读取超时才退出，异常为`SocketTimeoutException`。因此“界面已取消”可以与“旧请求仍占用两个网络线程”同时发生；在这段时间内重新体检会等待线程空闲。本机验证中的排队有约10秒上限，没有观察到永久挂死。

源码对应 [EmbyHealthHttp](../../cinema-ui/src/com/guocheng/mediapocket/EmbyHealthHttp.java)：进入正文后`cancel()`有意不跨线程断开，正文read使用剩余整段预算；取消/中断标记要等read返回后才检查。[EmbyDiagnostics](../../cinema-ui/src/com/guocheng/mediapocket/EmbyDiagnostics.java)的执行器只有2个worker，`stop()`同时调用传输取消与`future.cancel(true)`。本次测量保留了这两个取消动作，没有仅凭源码推断耗时。

对照场景：服务器尚未发送响应头时，使用3000ms预算，在请求发出250ms后取消，三轮实际worker分别在14ms、11ms、13ms内退出。**响应头前等待与正文停顿必须分开验收**；只使用旧的`health_delay`不能证明正文取消及时。

## 连续超时与同线程后续请求

每轮做20次，三轮共60次：服务发送首字节后每40ms再发一个字节，请求总预算300ms；捕获超时后立即检查调用线程中断标记，等待150ms再检查，随后在同一调用线程、同一传输对象发起快速请求。

- 60/60次都触发正文总超时，实测301–357ms。
- 60/60次立即及延后检查都没有遗留中断标记。
- 60/60次后续快速请求返回200。

这组结果支持0.1.145移除计时器`interrupt`后的回归行为。它与L2分别记录：中断残留没有复现，正文取消占用线程已复现。

## 合成服务扩展与烟测

`scripts/android_emby_fixture.py`新增可选`health_body_delay`秒数，默认0，仅作用于`/System/Info/Public`。例如控制请求：

```json
{"mode":"health","health_body_delay":20}
```

响应头和首字节会先发送并flush，再等待指定秒数发送剩余JSON。`state`新增`health_public_requests`、`health_body_started`；`events`新增对应事件，其中`at_ms`是相对最近一次reset的毫秒数。它们能用于比较“用户重新检查”的时刻和第三次请求真正到达服务的时刻。重置不带该字段会恢复0。

同一脚本也捕获`BrokenPipeError`、`ConnectionResetError`和`ConnectionAbortedError`，避免客户端正常取消在合成服务刷异常栈；未吞掉其他类型异常。

独立临时端口烟测通过：默认公开接口109ms；`health_body_delay=1`时首字节16ms收到、完整JSON1016ms收到；正文开始计数及相对时间事件存在；reset后恢复默认；客户端读取首字节后关闭连接，服务没有异常栈且正常退出。`py_compile`与`git diff --check`通过。

## 证据与限制

本机测量程序、三轮JSON、日志及烟测JSON保留在忽略目录`cinema-ui/build/continued-0146-http/`；没有添加JVM回归测试文件，也没有更改既有92项的计数或签名APK。所有HTTP端点仅监听loopback，只有合成数据，未使用真实服务器或账号。

本页只确认桌面JDK表现；Android内置HttpURLConnection实现、连接后调整read timeout的效果、实际UI连续取消/重启体验，以主验收文档的真机记录为准。尚未对生产代码应用“250ms分片读取”等方案，不能据本页声称问题已修复。

平板后续已单独完成2轮复验，第三次健康请求排队8641ms与8687ms；实际步骤、UI证据与清理状态见 [本轮主记录](continued-test-0146.md)。这些Android数值不计入上面的桌面测量表。
