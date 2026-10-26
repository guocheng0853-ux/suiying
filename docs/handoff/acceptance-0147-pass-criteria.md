# 0.1.147 平板复测通过标准

日期：2026-10-26。承接 [本机验收交接](handoff-0147-local-acceptance.md)。  
**不要**把旧 `continued_cases_0146.py` / `continued_cleanup_0146.py` 当通过标准——它们断言的是 0.1.146 缺陷复现（排队 >5s、Media3 回到 `PlayerActivity`、删除后仍显示旧数量）。

云端已复核：93/93 JVM、`EmbyCancelTest`、fixture 独立烟测（`scripts/fixture_smoke_0147.py`）。**不能代替**平板 ADB 复测。

只操作 OPD2401；所有 ADB 带 `-s SERIAL`。合成服务 `127.0.0.1:37813`，账号/密码 `fixture`。证据写入本地忽略目录 `deliverables/tablet-0147/`；服务器管理页只记数量，不存含真实地址的截图/完整 XML。已有 `settings-before.private.json` **不要覆盖**。

## 通过门槛（相对 0.1.146 缺陷）

| 用例 | 0.1.146 失败特征 | 0.1.147 通过 |
| --- | --- | --- |
| C1 新增（已测） | 列表 4、设置仍 3 | 列表与设置均为 4，无需重启 |
| C1 删除 | 列表 3、设置仍 4 | 删除 fixture 后返回设置立即为 3，先核对再重启 |
| C2 | 第三次启动观察间隔 ~8.6s | 新任务观察间隔应接近健康基线（同计时方式），远低于 8s；且出现 `health_body_client_closed`（旧正文及时关闭）。分别写清两条结论 |
| C3 | 手动取地址后窗口变 `PlayerActivity` | 全程保持 `SystemPlayerActivity`；有视频数据与播放开始；另留出画面/进度证据。不可凭 `PlayerActivity` 断言 VLC |
| C4 | 取消后手动重连观察间隔 ~12171ms（健康对照 ~343ms） | 取消后立即手动重连，观察间隔应接近健康对照量级，远低于 12s；无迟到错误提示。`delay=25` > 15s 读超时，**不是**成功迟到回调验收 |

计时一律「先定位按钮，再点并计时」，注明 ADB/轮询开销；控制栏显隐会改卡片坐标，禁止盲用旧坐标。

## 执行顺序

1. **C2**：健康定位按钮 → `reset` `health_body_delay=20` → 正文开始后取消/重启两次 → 第三次前 `configure` `mode=health, health_body_delay=0`（保留前两次断连事件）→ 记第三次点击前到客户端观察请求的间隔，以及服务端 configure/请求事件差。可在每次取消前用同参 `configure` 打时间标记。
2. **C3**：`fail` → 系统解码重播 → Media3 失败对话框 → 服务切 `health` →「重新获取地址」→ 核窗口类、视频与开始上报、画面进度。
3. **C4**：`delayed` `delay=25` → 自动刷新挂起 → 取消 → `health` → 立即手动重连；与健康开播对照同法计时；确认无迟到错误。
4. **回归（合成数据）**：native-focus、media3-focus、cancel、completed-budget、split 抽测；正常开播与换集各一次；`playback_conflicts=1`：首次 409、约 2s 后仅重试一次、最终播放成功。次数上限、显式取消与迟到结果不重开。
5. **C1 删除与清理**：仅删 `Local recovery fixture` → 设置立即 3 → 关服务、去 reverse、逐项恢复测试前系统设置与应用开关 → 确认原有 3 台服务器仍在。
6. 更新 `cinema-ui/README.md` 0.1.147 条目、本 handoff 与 VALIDATION 说明实际真机范围；每批验证后提交推送，不 force push。

## 不做 / 不写成通过

- O1（原生侧栏系统返回退出播放）本版未改，不算 C1–C4。
- 真实杜比 P5、HDR、真实服务器转码；合成 H.264 不能代替。
- 桌面 JVM / fixture 烟测不能写成 Android 取消已通过。
- 真实地址、令牌、签名、`fusion-default.json` 不进仓库/日志/截图。
