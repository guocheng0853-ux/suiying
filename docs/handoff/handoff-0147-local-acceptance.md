# 随影 0.1.147 本机验收交接

日期：2026-10-26。用户本轮要求：合入云端补丁、用原签名出包、平板复测C1–C4与回归；随后要求写handoff。本文件记录当前接续点，**完整平板验收尚未完成**。

平板复测通过标准（相对 0.1.146 缺陷断言的改写）见 [acceptance-0147-pass-criteria.md](acceptance-0147-pass-criteria.md)。云端已追加 `scripts/fixture_smoke_0147.py` 与该标准文档；**无 OPD2401 / 本机 ADB**，不能执行 C2–C4 与清理。

## 1. 当前结论

- 补丁已用`git am`合入并正常推送GitHub main；没有force push。
- 本机原签名构建、93/93 JVM程序、APK校验、原证书与16KB ZIP对齐全部通过。
- 云端复核（2026-10-26）：未签名构建 + `scripts/run_tests.sh` 93/93（含 EmbyCancelTest）+ `verify_apk.py` 通过；`scripts/fixture_smoke_0147.py` 独立烟测通过。仍不能写成 Android 取消已通过。
- 已同签名覆盖安装OPD2401平板，保留用户数据。
- **平板只完成C1新增场景**：管理列表4条，返回设置后概览与入口立即显示4。
- C1删除、C2/C3/C4及本版播放回归均尚未执行，不能写成通过。O1没有修改。
- 合成测试服务器、桌面服务和ADB reverse仍保留用于接续；本轮尚未最终清理。

## 2. 代码与工作树

基线main为`363d848`，fetch后仍是该提交，没有0.1.147版本冲突。

云端补丁原提交`d30d01c4880e370dabb568438b1a69846493e2a9`，在本机git am后成为`c2b26f3`，版本0.1.147 / code148。作者和补丁内容已保留；本地committer信息使提交SHA不同。

本机当前验收工作树：从原仓库根目录进入`cinema-ui/build/polish-0141`，分支`codex/acceptance-0147`。后续命令使用这个工作树。原主工作目录仍有8处既有未提交改动，不要reset、clean或覆盖它们。

本次handoff还保存了测试服务增强、部分验收说明和本地构建验证摘要；没有在补丁之后改应用运行时代码，也没有再换包。

## 3. 构建产物与证据

| 项 | 结果 |
| --- | --- |
| APK | `deliverables/MediaPocket-0.1.147-cancel-release-arm64.apk` |
| 版本 | 0.1.147 / 148 |
| 大小 | 37,050,933字节 |
| APK SHA-256 | `2b13ca5985635e8c007a21bcd3c139362cb0d1240b7f3bce375057610b48dbfb` |
| 原证书SHA-256 | `87a44d822580215fff01d604b1fedb31e52d69b42eb85ffbcd8b65aea21783d1` |
| JVM | 93/93，包含新增EmbyCancelTest |
| 包校验 | 9252个唯一类、3个dex、ABI/资源/原生库保留检查通过 |
| 签名与对齐 | 原证书验证、16KB ZIP对齐通过 |

本机采用既有Windows适配链：`build_win.py` → `test_win.py` → `verify_win.py`，运行的是与`scripts/run_tests.sh`相同的93个`*Test.java`程序，并在通过后写入93的JVM戳。没有把Linux shell执行情况写成本机结果。

本机EmbyCancelTest单轮实测：取消等待响应头的请求3ms退出，新重连141ms，两个旧正文旁的新体检16ms。**这些是桌面JVM数据，不能当成Android取消已通过。**

以下均为本机忽略文件，不在Git中：

- `cinema-ui/build/validate_build_0147.py`：此次串行构建/测试/验包驱动。
- `cinema-ui/build/build_win.py`、`test_win.py`、`verify_win.py`：既有适配器，输出版本已调整到0.1.147。
- `cinema-ui/build/accept-0147-build.log`、`accept-0147-tests.log`、`accept-0147-apk.log`、`accept-0147-package.json`。
- `cinema-ui/build/fixture-smoke-0147.json`：测试服务增强的独立烟测。

签名文件仍在本机既有私有位置，按PROJECT-GUIDE第5节使用，不生成新签名、不提交密钥或真实默认配置。APK也是本地产物，不上传公开仓库。

## 4. 平板现场与隐私

只操作OPD2401平板；手机没有操作。所有ADB命令继续带`-s SERIAL`，先用`adb devices -l`确认model，不要选手机。adb在`%LOCALAPPDATA%/Android/Sdk/platform-tools/adb.exe`。

当前前台为随影MainActivity的设置页，服务器显示4：原有3个服务器加`Local recovery fixture`。合成服务只监听电脑`127.0.0.1:37813`，当前`mode=health`、`catalog=movies`，尚无播放信息或播放开始请求。平板有`tcp:37813 → tcp:37813`的专用reverse。服务已运行，不要重复启动占用端口；若服务消失，再按脚本重启并恢复reverse。

本机`cinema-ui/build/device_0147.py`提供设备/合成服务/页面辅助函数，尚未写完整0.1.147批量验收driver。它的截图策略是：原服务器管理页只保存数量与合成项是否存在，不保存包含真实地址的截图或完整XML；安全的设置计数页和合成播放页才截图。读取UI时的临时设备XML立即删除。

本轮证据目录`deliverables/tablet-0147/`：

- `settings-before.private.json`：测试前设置，**已存在，不要重新执行before覆盖它**。
- `C1-add.json`：passed=true、列表4、设置4。
- `C1-settings-after-add.png/.xml`：安全的计数页证据。
- `C1-added-manager-safe-facts.json`：只含计数，没有真实地址截图。

字体1.0、density420、rotation free、原WeType、启用输入法列表、硬键盘显示设置0、night auto已记录。添加合成地址时短暂启用已安装ADB输入法，已在finally恢复并核对；没有改字体、density或night mode。连接时电量89%、USB供电。

## 5. 测试服务增强与接续方法

已增强并烟测`scripts/android_emby_fixture.py`：

- `health_body_delay`期间用socket peek观察EOF/reset，记录`health_body_client_closed`及事件；只观察、不消费请求字节。
- `mode=delayed`等待播放信息期间同样观察`playback_client_closed`。
- `POST /__fixture__/configure`更换模式/参数但保留计数、事件和reset时钟；未给出的参数仍回默认值，要显式传入所需delay/catalog。
- 事件附带相对reset的`at_ms`，reset代际隔离避免旧请求污染新一轮。
- `playback_conflicts=1`：下一次实际PlaybackInfo返回409，随后恢复正常；事件记录响应码与请求时间。
- `catalog=series`：Shows媒体库、Recovery series、Season 1、两集Fixture episode 1/2，支持换集。
- 播放开始/停止事件带合成item/session，便于核对换集和会话清理。

独立烟测通过：默认/正文首字节、主动断连观察、configure保留事件、双集目录、一次409后200。它证明测试服务能力，不证明Android客户端修复。

**不要直接把旧`continued_cases_0146.py`当新版通过标准。** 它含复现旧缺陷的断言（如Media3返回PlayerActivity、等待大于5秒），需要改为0.1.147预期。旧`continued_cleanup_0146.py`还会期望删除后出现旧数量，不能照搬。通过门槛与顺序以 [acceptance-0147-pass-criteria.md](acceptance-0147-pass-criteria.md) 为准。fixture 能力可用 `python3 scripts/fixture_smoke_0147.py /path/to/clip.mp4` 自检。

## 6. 接下来按顺序做

1. **C2**：先正常体检定位按钮；reset到`health_body_delay=20`，正文开始后连续取消/重启两次。第三次启动前用configure切到`mode=health, health_body_delay=0`，保留前两次断连事件。记录第三次点击前到客户端观察请求的间隔，以及服务端configure/请求事件差值；原版对应约8.6秒。每次取消前可用同参数configure留时间标记，再比较客户端关闭事件，注明控制请求/ADB开销。必须分别给出“新任务不排队”和“旧正文连接及时关闭”的结论。
2. **C3**：fail → 系统解码重播 → Media3失败对话框 → 服务切health → 重新获取地址；窗口类应保持SystemPlayerActivity。确认视频数据与开始上报，另保留实际出画面和进度证据。仅凭PlayerActivity不能断言VLC引擎。
3. **C4**：delayed、delay25 → 自动刷新挂起 → 取消 → health → 立即手动重连；先定位再计时，避免把uiautomator查找时间算作排队。控制栏可见/隐藏会改变卡片位置，不能盲用旧坐标。与健康开播对照，原版客户端观察间隔12171ms、健康343ms。观察取消后没有迟到错误提示。25秒大于15秒读取超时，不应把它说成成功迟到响应回调验收。
4. **回归**：native-focus、media3-focus、cancel、completed-budget、split抽测，核对次数上限、显式取消与迟到结果不重开。正常开播、换集各一次；409模式核对首次409、约2秒后仅重试一次、最终播放成功。所有内容和账号只用合成数据。
5. **C1删除与最终清理**：在应用中仅删除Local recovery fixture；返回设置后应立即显示3，先核对再重启，不能靠重启掩盖刷新失败。关闭合成服务、移除专用reverse；逐项恢复测试前系统设置和应用开关，并确认原有3个服务器保留。
6. 更新`cinema-ui/README.md`0.1.147条目、VALIDATION与本handoff，写明实际真机范围、失败/未测项及清理结果。每完成并验证一批改动就提交、推送main，不force push。

## 7. 剩余约束

真实地址、令牌、签名文件与真实fusion-default.json不进入仓库、日志或截图。不要采集原服务器管理页的原始截图/完整XML；仅记录数量。新增事件只记录合成item/session和相对时间，不记录请求URL或鉴权头。

本版没有改O1（原生侧栏按系统返回直接退出播放），不要算作C1–C4回归；测试时可点击侧栏外部关闭。真实杜比P5、HDR色彩和真实服务器转码仍未验收，合成H.264播放不能代替这些结论。
