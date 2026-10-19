# 交接给 Codex：随影真机调试（2026-10-19）

你在用户自己的 Windows 电脑上运行，手机用数据线连着这台电脑。你的任务是在真机上验证随影最近几版的改动，修掉发现的问题。之前的版本都只在 JVM 单测和构建校验里过了，**没有一项在真机上点过**——这是这份交接的全部意义。

## 1. 环境

| 项 | 值 |
| --- | --- |
| 手机 | 一加 PJZ110，Android 17，1440×3168，density 640 |
| adb | `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`（已验证可连，`adb devices` 能看到这台手机） |
| 包名 | `com.guocheng.mediapocket.next`（启动：`adb shell monkey -p com.guocheng.mediapocket.next -c android.intent.category.LAUNCHER 1`） |
| 当前手机上的版本 | 0.1.97（code 98） |
| 仓库 | github.com/guocheng0853-ux/suiying，main = 0.1.97 |
| 本分支 | `wip/0.1.98-device-fixes`：3 处真机发现的小修，**还没构建、没测试**（见第 3 节） |

构建：Linux 上是 `python3 cinema-ui/build_ui.py`（工具链 `scripts/setup_build_tools.sh`），用户这台 Windows 上有本地的 `build_win.py`（不在仓库里，见 `HANDOFF-NEXT-AI.md` 的「Windows 构建 + 测试链」）。测试：`sh scripts/run_tests.sh`（Windows 按 HANDOFF-NEXT-AI 的四层编译跑），目前 65 个全过。签名文件只在本机，**不要提交**。

## 2. 真机怎么测（推荐做法）

每一屏都这样拿三样东西，不要凭坐标猜：

```
adb exec-out screencap -p > shot.png                       # 截图
adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml   # 每个控件的文字、content-desc、bounds
adb logcat -d -v time | findstr /i "mediapocket AndroidRuntime FATAL"  # 崩溃和应用日志
```

点控件：从 ui.xml 的 `bounds="[x1,y1][x2,y2]"` 取中心点，`adb shell input tap X Y`。滑动：`adb shell input swipe x1 y1 x2 y2 400`。返回键：`adb shell input keyevent 4`。
注意：播放页的视频是 SurfaceView，`uiautomator dump` 在播放中有时会超时，多试一次或先暂停。
App 内也有日志：「我的 › 关于与故障排查 › 故障排查 › 调试信息」可以复制 / 分享，网址只留主机名。

已经在真机上确认过的（0.1.97，第 2 轮）：能安装启动、无崩溃；「我的」7 组分组、开关控件、括号说明小字都正常；首页继续观看去重、副标题「S1E105 · 第 105 集 · 剩 18 分钟」「剩 2 小时 23 分」正常；「媒体库 ›」右侧「···」存在。

## 3. 本分支的 3 处小修（先构建、装上、确认）

1. 「我的」最底下多出一个「其他」组，里面只有「设置」两个字和一块空白——原版设置页开头的标题和空白提示行。`SettingsLayout.apply` 里丢掉这两行。
2. 继续观看卡片标题写成「牧神记 · S1E105 · 第 105 集」，和副标题重复。`HomeRules.cardTitle` 去掉「 · S数字E数字…」后缀（`HomeCardRulesTest` 已加用例）。
3. 首页「融合 ▾」的半透明胶囊横跨了整个顶栏。`ScrapedMainActivity` 里把它包进一个 weight=1 的容器，自身 wrap_content。

确认三处都对了再合并到 main（版本已是 0.1.98 / code 99，README 顶部补一条记录）。

## 4. 要在真机上验证的清单（按优先级）

背景见公开文档《随影交互与界面审查》和 `cinema-ui/README.md` 0.1.92–0.1.97 各条。

**P0 播放页能不能用**（找一部能播的片：首页「继续观看」第一张，或任一 Emby 片）
- [ ] 0.1.92 播放页「更多」（右上 ⋮）弹出后每一项都**点得动**（之前是看得见点不动：播放器底层 `GesturePlayerActivity.dispatchTouchEvent` 吞触摸，现在菜单在独立 Dialog 窗口里）
- [ ] 0.1.96 播放页里的弹窗都是深色的：弹幕、在线字幕、切换片源（影视仓）、播放地址检查、Whisper；画中画 / 锁屏时会退回系统对话框
- [ ] 0.1.95 底栏「1.0×」倍速按钮能选档位并记住；长按临时加速松手恢复
- [ ] 0.1.95 播放失败时显示一句原因 +「换下一个源」「选择片源」两个按钮，都点得动；加载中只显示转圈和缓冲百分比
- [ ] 0.1.95 电影不显示上一集 / 下一集，选集页叫「版本」（蜘蛛侠：崭新之日有 1080p / 2160p 两个文件，适合测）
- [ ] 0.1.97 右侧面板只有 字幕 / 音轨 / 选集或版本 / 线路（/ 章节），点面板外面能关掉
- [ ] 0.1.97 线路页的「换下一个源」「自动换源：开 / 关」「每个源最多等待」能用（默认等待 30 秒）
- [ ] 0.1.97「更多」里的 定时关闭、流信息（可复制）、用外部播放器打开
- [ ] 0.1.97 第一次真正开始播放后弹一次「播放手势」说明，之后不再出现；长按图标显示名称；解码芯片显示「硬解+ / 硬解 / 软解 / 自动」

**P1 页面**
- [ ] 0.1.97 Emby 详情页：通栏大图、主按钮「继续 / 播放 S1E3」、季胶囊切换、同页集列表
- [ ] 0.1.97 影视仓详情页：主按钮按观看记录选集、线路胶囊、一行 4 个的选集格、单集电影只有主按钮
- [ ] 0.1.97「我的」开关点完滚动位置不跳回顶部；「默认倍速」能设
- [ ] 0.1.97 首页长按分区标题先弹确认再隐藏；来源下拉每项有小字说明

**P2**
- [ ] 0.1.93 播放页字幕页顶部「获取字幕」：在线搜索、Whisper、字幕插件
- [ ] 0.1.94 继续观看同一部片子只留一条（已初步确认）

每项记录：通过 / 不通过 + 截图 + 不通过时的 logcat 片段。发现问题就修，一个问题一个提交。

## 5. 规矩（必须遵守）

- 仓库是**公开**的。不要提交：签名文件（`*.jks`）、`cinema-ui/assets/fusion-default.json`（含用户个人令牌，已 gitignore）、任何令牌 / 密码 / AppSecret / API Key、服务器公网 IP、本机用户目录路径。提交前跑 `git grep -nIE "token=|secret|password" ` 和对已知个人域名的检查。
- 用户要求删除的 TorrentClaw 专用入口和饭太硬等预置源**不要重新添加**；保留用户自定义接口、插件和观看记录。
- 调试日志 / DebugLog 里网址只留主机名。
- 不要 force push 到 main。
- 每个版本：AndroidManifest versionCode+1 / versionName，`build_ui.py` 的 APK 名和 `ui_version`，`verify_apk.py` 的 APK 名、`match.group(2/3)`、`main_version_code`，README 顶部记录——四处同步。
- 结论写清楚哪些是真机验证过的、哪些没有。

## 6. 交回来的东西

- 一份清单结果（通过 / 不通过、截图）
- 修复提交（推到 main，或推分支开 PR）
- 最新可覆盖安装的 APK 发给用户
