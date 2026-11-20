# 影视仓配置文本兼容 Implementation Plan

> **For agentic workers:** 本会话按已批准范围直接实现。步骤用 `- [ ]` 跟踪。

**Goal:** 让影视仓配置在导入、首次读取和「更新当前配置」时，能接受普通 JSON、UTF-8 BOM、字符串感知的 `//` 与 `/* */` 注释，以及一层严格校验的 Base64 JSON；失败时不覆盖已保存的好配置。

**Architecture:** 纯文本归一化放在 `TvBoxConfigText`（不依赖 Android，JVM 可测）。`FusionCatalog.session` 在「没有已存配置、且不是内置配置」时，把 `TvBoxNet.fetch` 的正文交给 `parseFetched` → `normalize`。底座 `TvBoxActivity.lambda$6` 是导入和「更新当前配置」的唯一网络解析点：`TvBoxNet.fetch` 之后立刻 `new JSONObject`。用 dexlib2 只把这一处 `invoke-static` 改到同签名的 `TvBoxConfigText.fetchConfig`，后者内部仍调用 `TvBoxNet.fetch` 再 `normalize`。地址解析、站点/直播空值判断、失败不写入，都留在原调用点。

**Tech Stack:** Java 8 源码、org.json、`java.util.Base64`、dexlib2 补丁、`scripts/run_tests.sh`。

## Global Constraints

- 基线：`origin/main` `422acf5d`，versionName `0.1.187`，versionCode `188`。
- 版本：远端全部分支清单最高就是这一对；没有 `zcode/*`；开放 PR 只有 #30（0.1.147）。`0.1.188` / versionCode `189` 未被占用，本版使用这一对。
- 不使用已废弃且未推送的 `cursor/tvbox-config-text-51c8`。
- 不改详情页、海报、播放、重连、网络栈和鉴权。不解密图片、`clan://`、`**` 或其他自定义加密。Base64 只解一层。
- 不把本次改动说成用户那份未知配置已经修好。不打正式签名包，不合并。
- 错误文案固定、短，不拼接正文，不把 `JSONException` 设为 cause（其 message 会带原文片段）。不记录配置或令牌。
- 输入上限 8 MiB 字符（与 `TvBoxNet.fetch` 的 8 MiB 字节上限同量级）。对象嵌套上限 32。单次扫描，无递归。

## 调查结论（实现前）

- `TvBoxActivity.importConfig` 只解析地址并显示「正在检查配置…」。真正下载在 `lambda$6$com-guocheng-mediapocket-TvBoxActivity(String)Runnable`：`TvBoxNet.fetch(url, null, null)` → `new JSONObject(text)`。`sites` 或 `lives` 数组非 null 才交给保存 runnable；两者都缺则抛「这不是影视仓配置，原配置已保留」。空数组算非 null，保持这个旧行为。
- 「更新当前配置」会再次进入 `importConfig`，所以和导入是同一处。
- `FusionCatalog.session` 有已存 `tvbox.config:` 时直接 `new JSONObject(stored)`，成功后才 `putString`。没有已存、且不是内置地址时，才 `new JSONObject(TvBoxNet.fetch(...))`。这是首次加载的缺口。
- `suiying://import` 只写入地址，不下载；下次 `FusionCatalog.session` 走上面的 fetch。
- 影视仓/FongMi 的常见正文是：带 `//`、`/* */` 的 JSON，或整份 Base64（有时内部仍带注释）。图片藏配置和 `**` 解密不在本次范围。org.json 不认注释，字符串里的 `//` 不能用正则删。

## 文件

- 创建：`cinema-ui/src/com/guocheng/mediapocket/TvBoxConfigText.java` — `normalize`、`fetchConfig`。
- 创建：`cinema-ui/tests/TvBoxConfigTextTest.java`。
- 创建：`cinema-ui/tools/PatchTvBoxConfig.java` — 只改 `lambda$6` 的那一条 `invoke-static`。
- 修改：`cinema-ui/src/com/guocheng/mediapocket/FusionCatalog.java` — fetch 分支改 `parseFetched`。
- 修改：`cinema-ui/build_ui.py`、`cinema-ui/build/build_win.py`、`cinema-ui/verify_apk.py` — 补丁链与第 10 个补丁标记。
- 修改：`cinema-ui/AndroidManifest.xml`、上述构建脚本里的版本与 APK 名、`cinema-ui/README.md` 顶部一条。

## 行为

`normalize(String) -> String`：

1. null、空、只有 BOM/空白 → `配置内容是空的`。长度大于 `8 * 1024 * 1024` → `配置过大`。
2. 去掉至多一个开头 BOM，再 `trim`。若剩余字符（去掉空格、`\t`、`\n`、`\r` 之后）全部属于 Base64 字母表，走第 3 步；否则走第 4 步。`{`、`"`、`:`、`<` 都会把正文留在 JSON 路径，因此 `https://` 和注释 JSON 不会被当成 Base64。
3. Base64：只去掉上述空白；`=` 只能出现在末尾且最多两个；去掉填充后长度模 4 为 1 则拒绝；缺填充时补到 4 的倍数；`+`/`/` 与 `-`/`_` 不能混用；URL-safe 先换回标准字母再 `Base64.getDecoder()`。解码字节必须是严格 UTF-8，且字符数不超过上限。解码结果只走第 4 步，不再 Base64。
4. 字符串感知地删除注释：双引号字符串内保留一切，`\` 只转义下一个字符。行注释保留换行（`\n`、`\r\n`、`\u2028`、`\u2029`）。块注释换成一个空格。块注释没闭合 → `配置注释不完整`。字符串没闭合 → `配置不是合法 JSON`。
5. 结构扫描：必须是对象；括号用定长数组匹配；深度超过 32 → `配置结构过深`；顶层结束后只允许空白。再用 `new JSONObject` 校验，失败 → `配置不是合法 JSON`，且不附带原文。通过后返回去掉注释和外围空白的文本，不重新序列化。

`fetchConfig(String, JSONObject, String)` 调用 `TvBoxNet.fetch` 后把正文交给 `normalize`。HTTP 错误仍是原来的 `接口返回 HTTP …`，不会读到错误页。

导入失败时 `lambda$6` 在保存之前抛出，已存配置不动。`FusionCatalog` 在 `putString` 之前抛出，已存配置不动。

## 任务

### Task 1: 先写会失败的测试

**Files:** `cinema-ui/tests/TvBoxConfigTextTest.java`

- [x] 覆盖：普通 JSON 原文不变；BOM 与外围空白；字符串里的 URL、`//`、`/* */`、转义引号和反斜杠；行注释与块注释；Base64 填充、不填充、空白、URL-safe；Base64 里的带注释 JSON；坏 Base64、坏注释、HTML、401 文本、错误 JSON；双层 Base64、`clan://` 与图片头不解密；8 MiB 上限与深度 33；导入失败保持原字符串且文案不含 `SECRET_TOKEN`；`FusionCatalog.parseFetched` 与 `normalize` 结果一致。

- [x] 在归一化尚未实现时运行该测试，确认失败原因是注释/Base64 仍按原始 JSON 解析。首个失败是带 BOM 的正文抛 `JSONException`（`AssertionError: parse JSONException`）。

### Task 2: 实现归一化并接到 FusionCatalog

- [x] 实现 `TvBoxConfigText`，使 Task 1 通过。
- [x] `FusionCatalog.session` 的网络分支改为 `parseFetched(TvBoxNet.fetch(...))`。已存配置和内置配置分支不改。地址仍由 `TvBoxNet.fetch` 解析。

### Task 3: 导入与刷新补丁

- [x] `PatchTvBoxConfig` 把 `lambda$6` 里唯一的 `TvBoxNet.fetch` 调用改成 `TvBoxConfigText.fetchConfig`，寄存器与其他指令不变。自检：其余方法逐条指纹相同，且全 dex 只有这一处替换。在原始 `classes.dex` 上通过：16082 个方法其余指令不变。
- [x] Linux 与 Windows 构建链都在最后一道补丁之后跑它。`verify_apk.py` 增加第 10 个标记：含 `TvBoxActivity` 的 dex 必须引用 `TvBoxConfigText`。

### Task 4: 版本、说明、全量验证

- [x] `0.1.188` / versionCode `189`。README 写明碰撞证据、没测到用户配置、不解密图片/clan、没有签名包。
- [x] `scripts/run_tests.sh`：117/117。补丁自检通过。不打正式签名包。
- [x] 复查崩溃、ANR、信息泄漏和回归。连续 BOM 会漏掉第二个，已改为去掉开头全部 BOM，并补了失败后再通过的测试。错误不挂 cause。深度 32 在调用 `JSONObject` 之前拒绝。

## 2026-11-19 再次核对 origin/main

`git fetch origin` 之后 `origin/main` 仍是 `422acf5d5c9d653e3c9be1eb0aedd61c0c962c37`（0.1.187 / versionCode 188）。0.1.184 之后合入 main 的只有 #57 `3b27e67` 和 #58 `422acf5d`。开放 PR 仍只有 #30（0.1.147）。`codex/tvbox-poster-0187` 的版本号仍是 0.1.187 / 188。没有配置正文兼容实现，也没有 0.1.188 / 189。

0.1.186 只在 `FusionCatalog.open` 补了详情标题和海报参数，本改动保留。0.1.187 给 `TvSpiderRuntime` 加了不含正文的组件失败日志，并记录真机：分类/详情在 `TvSpiderRules.verify` 抛 `IllegalArgumentException`。那是组件校验，不是配置正文解析，这次不改。

本分支就建在当前 `origin/main` 上，还没有推送，不需要变基或强推。

## 不在范围内

用户的真实接口正文仍然未知。图片隐写、`**`、多层 Base64、尾逗号、`#` 注释、播放重连和详情页都不做。
