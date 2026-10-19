# HANDOFF — 随影下一棒（2026-10-19 写）

## 一句话现状

0.1.82→0.1.89 连续七版已提交并推送，手机上装的是 0.1.89（code 90，含全部修复）；云端 Whisper 字幕服务已在腾讯云部署并端到端验证，**卡在最后一步：需要控制台放行入口**（见「待办 P0」）。60/60 测试全绿。

## 版本线（本段全部在 GitHub main）

| 版本 | 内容 | 提交 |
|---|---|---|
| 0.1.82 | 弹幕同集匹配改数字环视（`\b` 在本机 JDK 上对 CJK 失效）；字幕按内容嗅探扩展名（ASS 不再存成 .srt）；外部打开接受原始中文路径（URI.create 兜底）；深链 singleTask；弹幕轮询不再每 250ms 整帧重绘 | cf7c920 |
| 0.1.83 | 播放器按 Hills 真机基准重排：状态行贴顶（时间居中、网速电量+矢量电池图标靠右）、meta.logo 时顶栏显示剧 logo、集标题 20sp/拇指 18dp、侧键挂内容区 30% 高、CinemaMenu 自绘深色菜单替换 13 处系统对话框、PlayerTheme+PlayerMotionRules token 收敛、卡片圆角化 | f6590f8 |
| 0.1.84 | 首页大图下加评分行（★/年/类型 + 分级徽章 heroCert）与两行居中简介（Hills 版式），圆点随移；播放跳转淡入淡出（PlayerChrome 启动/返回挂 overridePendingTransition 覆盖全部路径） | 0b09cda |
| 0.1.85 | CinemaMenu 套 ScrollView 限高 78%——**此版埋了量高时机 bug，0.1.89 修** | ee838b1 |
| 0.1.86 | OpenSubtitles/射手接口错误 JSON 透传为可读中文（403 You-cannot-consume 不再伪装成零结果）；下载端点修正 /api/v1/downloads | 32f25dc |
| 0.1.87 | 接入迅雷免密钥字幕源（oracle/subtitle 按名搜索、.srt 直链、simple_name 标签）；搜索改为迅雷必搜+密钥源可选 | 941815f |
| 0.1.88 | 云端 Whisper 语音生成字幕（见下）+ 补 0.1.85-87 更新记录 | c302812 |
| 0.1.89 | 修 CinemaMenu 量高时机 bug：面板在菜单行加入前被测量钉死成 72px 标题条（表现为菜单只剩一条黑带、其余项不可见不可点、像死机）；改为行加完后测量+套滚动 | d99bd12 |
| 0.1.90 | CinemaMenu 被播放器根视图压住的 z-order 问题：VLC 视频面初始化把根视图重挂 content 末尾（最上层），菜单看得见但全被吞触摸；菜单打开期间持续 bringToFront 保活 | d99bd12 后一笔（同日） |

注意：0.1.90 的 APK 文件名叫 `MediaPocket-0.1.89-menu-fixed-arm64.apk`（内含 zorder 修复），版本名 0.1.89/code 90——sed 升版没跳成，无实害，下一版从 code 91 / 0.1.90 顺延。

## 云端 Whisper 服务（已部署，等用户放行入口）

- 位置：用户自己的腾讯云服务器（地址不写进公开仓库）systemd 服务 `whisper-srt`，端口 8765，源码=仓库 `cloud/whisper-srt/server.py`
- 栈：faster-whisper 1.2.1 base int8 + **av==14.2.0（钉死，19.x 删了 metadata_errors 会炸）** + huggingface_hub 0.36.2；模型走 hf-mirror.com 且 `HF_HUB_DISABLE_XET=1`（Xet 走镜像 401）
- 令牌：在服务器 `~/whisper-srt/token.txt`（**不要写进仓库**；旧令牌曾在 2026-10-19 公开过，已要求用户更换）
- 管道已端到端验证：POST /job → 下载 → ffmpeg 抽音轨 → 转写 → GET /srt/{id}
- **卡点**：入口还没对外开放。做法（0.1.91 起服务默认只监听 127.0.0.1）：
  - 推荐：用服务器上已有的 Caddy 加一个子域名反代到 `127.0.0.1:8765`（自动 HTTPS），App 填 `https://<子域名>`。令牌走 HTTPS，不明文。
  - 不要直接在安全组放行 8765 用 http 访问：令牌会明文传输，服务还会替任何拿到令牌的人下载任意地址。
- 用户配置后：播放 http 直链视频 → 更多 → Whisper 生成字幕 → 等转写（base 模型 ≈ 片长一半时间）→ 自动加载。磁力/P2P 源不支持（云端取不到）

## Windows 构建 + 测试链（全部本机可用）

- 测试：`javac` 四层编译（stubs→runtime-src→src→tests，classpath 分号）+ 逐个跑 tests/*Test 的 main；android.jar 用本地 SDK 的 platforms/android-35（已拷到 build-tools/android-35）
- 出包：`uv.exe run --no-project python cinema-ui/build/build_win.py`（build_win.py=build_ui.py 的 Windows 适配：SDK aapt2/zipalign/d8/apksigner + 本地 jadx-1.5.6 的 dexlib2；产物在 deliverables/）
- 60/60 测试基线；跑套件必须先有 build/classes（重编 src 即可）

## 大坑清单（都真实踩过）

1. **GBK**：JDK17 中文 Windows 默认编码 GBK——无参 `getBytes()` 写中文文件必炸（HeaderRouteTest 中招）；一律 StandardCharsets.UTF_8
2. **`\b` 正则**：本机 JDK 把 CJK 当词字符，`第12话` 匹配不到 12——数字边界用环视 `(?<!\d)0*N(?!\d)`
3. **量高时机**：容器 measure 必须在子元素全部 add 之后（0.1.85 菜单黑带事故）
4. **z-order**：VLC 视频面初始化会重挂根视图到 content 末尾——叠加层菜单需 bringToFront 保活（PlayerChrome.poll 每拍调 CinemaMenu.front）
5. **CRLF + heredoc**：批量补丁脚本走 Write 工具落文件再 node 执行；行尾按 \r\n 匹配；跑完必做括号余额扫描
6. **弹幕未配是预期**：AppId/AppSecret 要用户自己去弹弹play开放平台申请，随影刻意不内置

## 待办（按优先级）

- **P0**：用户控制台放行 Whisper 入口（A 或 B）→ App 里填服务器+令牌 → 真机播一集验证转写质量与耗时；base 中文不够就升 small（服务端 WHISPER_MODEL 环境变量或请求体 model 字段）
- **P0**：用户回归验证 0.1.89 的菜单（19 项可滚动、各项可点）与蜘蛛侠的迅雷字幕搜索
- **P1**：主页 Hills 化第二批（海报行视觉节奏、hero 文案层级、首页画图测试——参照 PlayerMotionRulesTest 的 PNG 渲染模式）；Hills 基准帧在用户本机
- **P2**：Whisper small 档位选项透出到设置；YIFY 免密钥兜底源（仅电影）；Emby 直链的 Logo 顶栏扩展到 Emby 源（现仅 Stremio meta.logo）
- **版本习惯**：每版升 code+1、README 顶部加记录、VALIDATION.json 同步、build_win.py 产物名同步——0.1.85-87 三版记录已在 0.1.88 一并补齐

## 相关文件

- Hills 拆解报告、反编译目录、基准帧都在用户本机（路径不写进公开仓库），需要时向用户要。
- 本机 python 是占位符：跑 .py 一律 `uv.exe run --no-project python`

## 公开仓库注意

本仓库是公开的。不要提交：签名文件、`fusion-default.json`、任何令牌/密码/AppSecret、服务器公网 IP、本机用户目录路径。提交前跑 `git grep -nIE "token|secret|[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+"` 自查。
