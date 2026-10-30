# 0.1.164 最新代码评估

日期：2026-10-30。评估固定在远端 main `5e9623f9b865b9bb60901ef4db545fbf5b64dc87`，版本 0.1.164 / code 165。比较范围 `git diff 5cf660e...5e9623f`，覆盖 #40 的 Stremio 开播接力和 #41 的播放页/批量下载。只评估并记录问题，本报告不代表缺陷已经修复。

建议先修正下面的下载来源身份问题，再对 0.1.164 的新菜单进行真机验收。0.1.163 本轮没有新增确认缺陷；此前用户“已经出画面”的反馈仍仅证明该次实际试播，不能推导固定提速比例。

## Standards：规范和交接证据

### S1 · P2：中文紧贴 URL 时，错误汇总脱敏失效

- 位置：`cinema-ui/src/com/guocheng/mediapocket/OfflineQueueRules.java:149`，同类边界还在第 153 行。
- `redact()` 用 `\b` 限定协议开头。JDK 17 的独立探针调用正式方法：`连接失败https://fixture.invalid/video.mp4?token=synthetic` 仍含完整路径和查询参数；在汉字与 URL 之间加空格的对照样本会被脱敏。全程使用合成地址和合成参数。
- 标准依据：PROJECT-GUIDE 第 7.7 节已提醒中文环境的单词边界问题；本版交接要求错误汇总中的 HTTP(S) 地址只保留主机名。
- 影响：中文错误紧贴地址时可能向错误弹窗暴露地址参数；本轮未在 Android 上复测此正则，不宣称已发生真实凭证泄漏。
- 建议：使用不依赖单词边界的协议识别，补充中文相邻、括号、换行和 magnet 用例。现有测试只有带空格样本。

### S2 · P2：签名构建状态在文档中互相矛盾

- 位置：`cinema-ui/README.md:6`、`PROJECT-GUIDE.md:3`、`docs/handoff/playback-download-0164.md:26`。
- 三处仍称“没有签名 APK”或“没有跑 verify”，但最新提交是 Windows signed build，`cinema-ui/VALIDATION.json:21–29` 记录 JVM 100、APK 类校验 passed、签名存在。
- 标准依据：PROJECT-GUIDE 第 4 节要求每版更新记录准确写明验证范围。
- 建议：由对应包和构建日志核对后同步文字，继续保留“未进行本版真机菜单验收”的边界。本轮只核对仓库内证据，未重新签名出包，不据摘要声称独立验过证书。

Standards 共 2 项；风险较高的是错误信息脱敏缺口。未另列没有明确收益的代码风格建议。

## Spec：需求行为

### D1 · P1：播放页下载保存了错误的影视仓线路

- 位置：`cinema-ui/src/com/guocheng/mediapocket/PlaybackDownload.java:45–46`。
- 需求原文：“下这个地址并记下线路，方便过期后续传”。
- `source()` 从 `meta.videos` 按观看 ID 反推 `fusion.flag/value`；该数组来自 `FusionPlaybackRules.episodeChoices()`，明确按集去重、仅保留第一条线路。
- 复现：两条线路都有 E01，从 routeB 开播，当前 URL 为 routeB，调用正式 `PlaybackDownload.source()` 却得到 `flag=routeA,value=valueA`。独立 JVM 探针已确认；没有调用真实服务器。
- 影响：去重身份错误；当前地址过期后，`OfflineDownloads.refresh()` 解析 routeA，而 `OfflineService` 继续按已有文件长度请求 Range。如果两条线路指向不同编码/剪辑版本且服务器接受 206，会把不同文件内容拼接，造成损坏下载。
- 建议：在实际播放 stream 中保存准确的 config/site/flag/value，换源时跟随实际来源更新；下载使用这份身份。对无法确认同一文件的刷新应避免盲目按旧偏移续传。
- 验收：分别从第二线路首播、自动/手动切换线路后下载；确认去重键、过期刷新请求来源及最终文件哈希。

### D2 · P2：取消后才解析完成的集仍被加入队列

- 位置：`cinema-ui/src/com/guocheng/mediapocket/OfflineBatchMenu.java:72–74`。
- 需求原文：“取消时，已经解析好的会入队，还没解析的不加”。
- `cancel` 只在 `record()` 前检查。用户在一个慢解析请求期间取消，该请求返回后仍执行 `ready.add(rec)`，随后 `commitAll()` 会把它加入队列。
- 证据等级：静态控制流确认，未做本版 Android 按钮复现。
- 建议：解析返回后、接收结果前再次检查取消；保留取消前已经就绪的记录。最好给在途请求提供可取消句柄。
- 验收：第 1 集先完成，第 2 集用可控延迟阻塞，取消后再释放第 2 集响应；最终只有第 1 集入队，第 3 集不发请求。

### D3 · P2：整季下载汇总遗漏预先跳过的条目

- 位置：`cinema-ui/src/com/guocheng/mediapocket/OfflineBatchMenu.java:47–48`。
- 需求原文：“汇总为『已加入 n 集，跳过 m 集』，并按原因计数”。
- “下载本季全部”和“从当前集往后全部”先用 `allowed()` 剔除锁定条目，再传给入队流程。`run()` 因而不知道哪些集被预先跳过。
- 复现：3 集中 1 集可下、1 集已完成、1 集为 m3u8，实际规则过滤只传入 1 集；最终汇总为“已加入 1 集”，遗漏“跳过 2 集”和原因。独立规则探针已确认过滤和汇总，UI 调用链经静态核对。
- 全部不可下载时还会落入“还没有选择剧集”，无法解释一键整季操作为什么未加入。
- 建议：继续禁止不支持条目入队，但将原始范围及预过滤原因一起交给汇总；普通勾选则只统计用户所选范围。
- 验收：混合可下载/已完成/排队/不支持来源、全部锁定、从中间集往后三种范围。

Spec 共 3 项；最高风险 D1 为下载来源错配及续传文件损坏。

## 本轮验证与边界

- 使用固定提交的隔离源码副本，重新编译 stubs、runtime、扩展层和 JVM 测试；没有修改生产源码。
- 独立探针调用正式 `OfflineQueueRules.redact()`、`FusionPlaybackRules.episodeChoices()`、`PlaybackDownload.source()`，确认 S1/D1；D3 的规则结果结合实际调用点确认。
- 首轮从错误工作目录运行，并受回环网络沙箱限制，结果 72/100。这些属于评估环境问题；纠正运行目录并允许本机合成 HTTP 后，重跑全部 28 个失败项均通过，最终合并结果为 **100/100**。代码在两轮之间未变。现有测试通过不覆盖本报告列出的来源身份和 UI 编排缺陷。
- 不操作平板/手机，不使用真实服务和账号，不导出真实 URL、片名、令牌、签名文件或本地配置。不重新构建或安装 APK，不把 JVM 结果写成 Android 验收。

