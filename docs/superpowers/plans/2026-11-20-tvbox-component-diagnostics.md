# 影视仓组件校验与分类空数据诊断

> 只记录代码里能证明的部分。用户的配置地址和当时下载的组件字节都不在仓库里，不能据此宣布那两个站点已经修好。

## 版本

`git fetch origin` 之后，`origin/main` 仍是 `1bf96171742d928eabcab279a97534d24a4d50e9`，清单 `versionName 0.1.188` / `versionCode 189`（PR #59）。

远端全部分支的 `cinema-ui/AndroidManifest.xml` 都核对过。最高就是这一对（`origin/main` 与 `origin/cursor/tvbox-config-text-c87a`）。没有 `zcode/*`。开放的只有 PR #30（0.1.147）和 issue #38（早已合入的 0.1.161 打包说明）。没有分支占用 `0.1.189` / `versionCode 190`。0.1.185–0.1.188 已分别在 `codex/tvbox-reconnect-0185`、`codex/tvbox-detail-0186`、`codex/tvbox-poster-0187` 和 main 上，本版不复用。本版使用 **0.1.189 / versionCode 190**。

## 失败 1：`IllegalArgumentException`，分不出大小还是校验

用户截图是首页「影视仓片库」、当前站点「小雅｜高清」，文案 `读取失败：线路组件错误：IllegalArgumentException`。继续观看的卡片还在。`cinema-ui/VALIDATION.json` 仍是 0.1.187 的真机记录：分类和详情在 `TvSpiderRules.verify` 抛 `IllegalArgumentException`，发生在详情绘制之前。这次不改写那份记录。

`verify` 只有两种失败，都会抛 `IllegalArgumentException`：

- 字节长度为 0，或大于 16MiB（原来的文案是「组件大小无效」）
- 声明了非空 `;md5;`，且和正文的 MD5 不一致（原来的文案是「组件校验不一致，已停止加载」）

界面看到的那句不是 `verify` 的原文。`TvSpiderRuntime.request` 把工作线程里的异常包成 `线路组件错误：` 加上 `getSimpleName()`，`FusionLibraryActivity.why` 只显示这句 message。0.1.187 的 `logComponentFailure` 同样只写异常类名和宿主调用位置，故意不写 message，因为别的异常正文可能带地址。所以 logcat 和界面都只剩 `IllegalArgumentException`。没有组件字节时，空文件、超过 16MiB、MD5 不一致这三件事无法区分。

仓库里的 `fusion-default.example.json` 带 `;md5;`，只是样例，不是用户当时下载到的 jar。不能判断用户那次是 size 还是 checksum，也不能说这份样例修了用户的源。

本次只加原因码，不放宽门槛：

- 长度不在 `1 .. 16MiB`：仍抛出，界面和日志为 `size bytes=<长度>`。空文件即使声明了 MD5，也先报 size，不去算摘要。
- 长度合格但 MD5 不一致：仍抛出，界面和日志为 `checksum`。不写期望值、实际摘要或地址。
- 其他异常仍只留类名，不读 `getMessage()`。原因链最多看 8 层，避免成环；更深的包装不再往下读，也不把外层 message 带出来。
- 宿主栈帧不在 `TvSpiderRules` / `TvBoxRules` / `TvSpiderDispatch` / `TvSpiderRuntime` 上时，仍不写 logcat，和 0.1.187 一样。校验失败的栈在 `TvSpiderRules.verify`，所以 size / checksum 会记下来。
- 同一行再进应用内调试日志（`DebugLog`，本身会再去一次网址）。日志正文只有原因码。

改之前 JVM 断言失败为：`empty component is size bytes=0, got IllegalArgumentException`。这就是旧实现分不出两种失败的证据。

## 失败 2：`线路返回空数据`，重试能出列表，退出再进又空

这和失败 1 不是同一条路径。用户后来把来源芯片切到「影视仓」，首页有内容；点「热播电影」的「全部」进入标题为「影视仓」的列表，文案 `读取失败：线路返回空数据`，有重试。第一次重试成功，副标题是「豆瓣热门 第1页」，海报出现。回到首页再点「全部」，又是同一句空数据。

代码对得上的只有 `FusionLibraryActivity`：标题写死「影视仓」；失败行是 `读取失败：` 加上异常 message；重试按钮来自 `work`。成功后的副标题 `站点名 · 第 N 页` 只在 `category()` 里设置。`豆瓣热门 · 第 1 页` 说明这次是分类第 1 页，站点名是豆瓣热门，不是失败 1 的「小雅｜高清」。

`线路返回空数据` 只在 `TvSpiderRuntime.request` 里抛出，条件是爬虫方法返回 `null`，或按 `String.trim` 的规则整段为空白。下面这些都不是这句文案：

- `{"list":[]}` 会解析成功，列表页显示「此分类暂时没有条目」
- 超过 35 秒是「线路调用超过 35 秒」
- 调用被中断是「线路调用已取消」
- 线程池拒绝是「线路任务繁忙，请稍候重试」
- 爬虫抛异常是「线路组件错误：…」

所以这次记录到的是一次正常返回的空白字符串，不是超时、取消，也不是空列表被误判。

同一次进入里，重试和第一次用的是同一组参数：`FusionCatalog.request(session, "category", id, "", page)`。`TvSpiderDispatch` 固定 `categoryContent(id, "1", true, 空 HashMap)`。重试闭包抓住的是同一个 `id` 和 `page`，不会改站点，也不会先调 `homeContent`。爬虫按 `base|site|declaration` 的摘要缓存，`init` 在放进缓存之前做完。`FusionLibraryActivity` 不读、也不写首页那份 6 小时分类缓存。首页海报可以来自缓存，同时这一次实时调用仍返回空白。重试成功的那一页没有被写回缓存，所以离开再进入必然再发一次实时请求。

因此「第一次空、重试有数据、再进入又空」不能解释成「爬虫只需要进程内热一次身」：成功之后单例还在，`onDestroy` 也不调用 `destroy()`。也没有找到宿主把「还没准备好」标成空数据、或把一次有效 JSON 丢掉的路径。搜索有另一套方法签名的补调，分类没有。

缺的证据是用户的配置地址，以及豆瓣热门那次实际执行的 jar 或 JS。没有这两样，就不能断定空白是源站自己返回的，还是组件里第一次调用吞掉了异常。自动再请求一次会把持续的空结果藏住，这次不做。

空白返回现在会多记一行不含正文的日志：`empty`（根本没有字符串）或 `empty bytes=<原文字符数>`（只有空白）。界面文案仍是「线路返回空数据」。`{"list":[]}` 不会走这条日志。

## 仍未知

- 小雅那次下载到的字节到底是 0、超过 16MiB，还是 MD5 不一致。
- 豆瓣热门在相同的 `categoryContent` 参数下，为什么第一次返回空白、紧接着的下一次又返回带 `list` 的 JSON。
- 用户机器上的配置正文。样例配置不能代替。

## 这次没有做的事

没有放宽或跳过 `verify`。没有给分类加自动重试。没有伪造列表数据。没有打签名包，也没有打未签名包，没有真机点选。`scripts/run_tests.sh` 为 117/117（新断言在原有的 `TvSpiderRulesTest` 里，测试程序个数不变）。源码按 `build_ui.py` 的编译参数通过。`cinema-ui/VALIDATION.json` 仍是 0.1.187 的签名真机记录，本版不覆盖它。
