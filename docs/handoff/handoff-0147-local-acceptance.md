# 随影 0.1.147 本机验收交接

日期：2026-10-26。用户本轮要求：合入云端补丁、用原签名出包、平板复测C1–C4与回归。  
复测通过标准见 [acceptance-0147-pass-criteria.md](acceptance-0147-pass-criteria.md)。

## 1. 当前结论（已完成本机平板复测）

- 补丁已用`git am`合入并正常推送GitHub；没有force push。
- 本机原签名构建、93/93 JVM、APK校验、原证书与16KB ZIP对齐全部通过；APK已同签名覆盖安装OPD2401。
- **C1–C4、正常开播/换集/409及7项恢复边界补测在OPD2401上通过；分屏部分通过，早期失焦裁切观察O2尚待确认。** 详细范围见[本轮补测](continued-test-0147.md)与[脱敏汇总](acceptance-0147-summary.json)。合成fixture已删除，服务关闭、reverse移除、系统设置恢复、退出分屏，原有3台服务器保留。
- O1 本版未改，不算通过项。真实杜比 P5、HDR、真实服务器转码仍未验收。

| 用例 | 结果 | 关键证据 |
| --- | --- | --- |
| C1 新增 | 通过（此前） | `deliverables/tablet-0147/C1-add.json`：列表与设置均为 4 |
| C1 删除 | 通过 | `C1-delete.json`：删除后设置立即为 3，未靠重启 |
| C2 | 通过 | 原703ms；补测两次正文首字节到关闭297/313ms，第三次请求观察间隔**343ms**，最后五项通过、0次开始 |
| C3 | 通过 | `C3-media3-manual.json`：失败与恢复均保持 `SystemPlayerActivity`；`media_ok=1`、`sessions_started=1` |
| C4 | 通过（补齐在途取消） | 原671ms发生在旧请求15s超时之后；补测挂起期间取消，旧请求存活**875ms**，立即手动重连**391ms**（健康对照312ms），观察超过原延迟后无误重开或迟到错误 |
| 健康开播 | 通过 | `R-healthy-play.json` |
| 换集 | 通过 | `R-episode-switch.json`：`fixture-episode-1` → `fixture-episode-2` |
| 409 一次重试 | 通过 | `R-conflict-409.json`：响应 `[409, 200]`，间隔约 2s |
| 清理 | 通过 | `cleanup-0147.json`：3 台服务器、无 fixture、端口关闭、reverse 空、夜间 auto、原输入法 |
| 恢复边界补测 | 7项通过 | native/media3失焦、取消/退出后无重开、已完成失败不退还次数，以及C2/C4快速取消时序；实际画面推进已核对 |
| 分屏 | 部分通过 | 禁止失焦自动刷新、稳定后的失败卡片和片源按钮通过；首次进入后快速失焦曾裁切，记录为O2，未改代码掩盖 |

## 2. 代码与工作树

本机验收工作树：`cinema-ui/build/polish-0141`，分支 `codex/acceptance-0147`。  
云端补丁 `c2b26f3`（0.1.147 / code 148）。验收脚本为本地忽略文件，不进仓库：

- `cinema-ui/build/accept_c2_0147.py` / `accept_c3_0147.py` / `accept_c4_0147.py`
- `cinema-ui/build/accept_regress_0147.py` / `accept_c1_cleanup_0147.py`
- `cinema-ui/build/device_0147.py` 及既有 `build_win.py` 链

没有在补丁之后再改应用运行时代码，也没有再换包。

后续补测脚本和证据在本地忽略目录 `cinema-ui/build/*boundary*0147.py` 与 `deliverables/tablet-0147-boundary/`。最后清理不仅核对night与输入法，也逐项比较所有原始系统设置、检查全屏尺寸，并确认已安装APK哈希与签名构建一致。

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
