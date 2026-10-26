# 随影 0.1.147 本机验收交接

日期：2026-10-26。用户本轮要求：合入云端补丁、用原签名出包、平板复测C1–C4与回归。  
复测通过标准见 [acceptance-0147-pass-criteria.md](acceptance-0147-pass-criteria.md)。

## 1. 当前结论（已完成本机平板复测）

- 补丁已用`git am`合入并正常推送GitHub；没有force push。
- 本机原签名构建、93/93 JVM、APK校验、原证书与16KB ZIP对齐全部通过；APK已同签名覆盖安装OPD2401。
- **C1–C4 与合成回归在 OPD2401 上通过**；合成 fixture 已删除，服务已关闭，ADB reverse 已移除，测试前系统设置已恢复；原有 3 台服务器保留。
- O1 本版未改，不算通过项。真实杜比 P5、HDR、真实服务器转码仍未验收。

| 用例 | 结果 | 关键证据 |
| --- | --- | --- |
| C1 新增 | 通过（此前） | `deliverables/tablet-0147/C1-add.json`：列表与设置均为 4 |
| C1 删除 | 通过 | `C1-delete.json`：删除后设置立即为 3，未靠重启 |
| C2 | 通过 | `C2-cancel-queue.json`：两次 `health_body_client_closed`；第三次定位后观察间隔 **703ms**（0146 约 8.6s） |
| C3 | 通过 | `C3-media3-manual.json`：失败与恢复均保持 `SystemPlayerActivity`；`media_ok=1`、`sessions_started=1` |
| C4 | 通过 | `C4-cancel-requeue.json`：取消后手动重连观察间隔 **671ms**（健康对照 906ms；0146 失败约 12171ms）；有 `playback_client_closed` |
| 健康开播 | 通过 | `R-healthy-play.json` |
| 换集 | 通过 | `R-episode-switch.json`：`fixture-episode-1` → `fixture-episode-2` |
| 409 一次重试 | 通过 | `R-conflict-409.json`：响应 `[409, 200]`，间隔约 2s |
| 清理 | 通过 | `cleanup-0147.json`：3 台服务器、无 fixture、端口关闭、reverse 空、夜间 auto、原输入法 |

## 2. 代码与工作树

本机验收工作树：`cinema-ui/build/polish-0141`，分支 `codex/acceptance-0147`。  
云端补丁 `c2b26f3`（0.1.147 / code 148）。验收脚本为本地忽略文件，不进仓库：

- `cinema-ui/build/accept_c2_0147.py` / `accept_c3_0147.py` / `accept_c4_0147.py`
- `cinema-ui/build/accept_regress_0147.py` / `accept_c1_cleanup_0147.py`
- `cinema-ui/build/device_0147.py` 及既有 `build_win.py` 链

没有在补丁之后再改应用运行时代码，也没有再换包。

## 3. 构建产物

| 项 | 结果 |
| --- | --- |
| APK | `deliverables/MediaPocket-0.1.147-cancel-release-arm64.apk` |
| 版本 | 0.1.147 / 148 |
| 大小 | 37,050,933字节 |
| APK SHA-256 | `2b13ca5985635e8c007a21bcd3c139362cb0d1240b7f3bce375057610b48dbfb` |
| JVM | 93/93，含 EmbyCancelTest |
| 签名与对齐 | 原证书验证、16KB ZIP对齐通过 |

## 4. 不做 / 未写成通过

- O1（原生侧栏系统返回退出播放）本版未改。
- 真实杜比 P5、HDR、真实服务器转码；合成 H.264 不能代替。
- 桌面 JVM / fixture 烟测不能单独写成 Android 取消已通过（现已有平板 C2/C4 证据）。
- 真实地址、令牌、签名、`fusion-default.json` 不进仓库/日志/截图；服务器管理页只记数量。
