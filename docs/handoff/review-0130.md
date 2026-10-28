# 随影 0.1.130 (PR #24, 80e6e40) 复审 —— 对比 2430e08
只审未打包。工作树 /workspace/wt-review-0130。
测试：JVM 82/82 通过；test_media3_offline.py 通过；build_ui.py 未签名构建通过（新逻辑：找不到 keystore 时直接输出未签名包）；verify_apk.py 通过（131 / 0.1.130）。
0.1.120 修复未回退：RequestHeaders、OfflineService、OfflineDownloads 未改；BackupRules 只多跳过 cinema.catalog. / fusion.home:；ScopedHttpDataSource 仍是 transferStarted 在前。
结论：卡顿主因基本都真的修了；有 2 个新问题建议合并前修（M1、M2），其余可后续。

## 卡顿修复核对
- J1 合并去重：已修。预编译正则 + 每条只算一次 key/year + 按 key 分桶 + 浅拷贝，放 agg-merge 线程。实测同样数据 15–24ms → 0.28–0.43ms。
- J2 图片：已修。按目标宽抽样 + 密度缩放到目标宽，海报 RGB_565，缓存 = maxMemory/8，大图背景按 1× 屏宽解码。
- J3 整行重建：已修（300ms 合并，来源到齐立即画；内容签名相同不重画）。正常每行 1–2 次重画。
- J4 主线程解析 JSON：已修（Stremio/影视仓缓存读取、Emby metas/asCards/slice 都在后台，进度写入合并为一次 apply）。
- J5 prefs 大缓存：已修（cacheDir/mp-json-v1，先写 .tmp 再改名，meta 上限 80，启动时后台迁移）。剩余：见 L1。
- J6 大图重置：大部分已修（offerHeroes：只升不降、同级不重画、滑动中先记下、保留页码）。
- J7 FadeFrame saveLayer：已修（改为底色渐变叠加）；默认外观的模糊光晕也去掉了。
- J8 剧集：已修（每页 30 集 +「继续加载」；标记已看只替换那一行）。
- J9 骨架：已修（只对小块做动画、空行先清骨架、onPause/onResume 暂停/恢复）。
- J10：已修（HeroSwipe 读内存缓存，SeekPreview 复用 server，下拉刷新不再遍历 prefs）。

## 问题（按优先级）
M1 中高｜PreviewFetch.java:13-17 —— DONE 是静态 Map，每部片的 BifIndex（BifIndex.file 含整个 BIF，几 MB 到十几 MB）永不释放。连看多集/多部会一直涨，长时间后 OOM。修：DONE 改成只留 1–2 个的 LRU，播放页退出时移除。
M2 中｜EmbyStore.java:9 —— list() 现在返回全局共享的 JSONArray，get() 返回共享的 server JSONObject。EmbyClient.call（:72 server.put("client")；401 时 authenticate 会改 url/token/userId 等）在后台线程直接改这个共享对象，同时 UI / emby-home×2 / emby-play / emby-preview 在读或 toString()（EmbyPlayback 每个地址都 new JSONObject(server.toString())）。后果：偶发 ConcurrentModificationException / HashMap 损坏；登录失败时改了一半的字段（例如 url 换成备用地址）留在内存缓存里，listRaw 不变，所有地方都继续用错的值，直到 prefs 改变。以前每次 get() 都重新解析，所以没这个问题。修：get() 返回拷贝（new JSONObject(s.toString())，便宜），或只缓存原始字符串。
M3 中｜ScopedHttpDataSource.java:39 + Media3VideoView.java:63 —— 64MB 上限改成「用户拖动后 2.5 秒内」，但抛的是普通 IOException，Exo 默认策略会重试（大约在第 0、1、3 秒），第 3 秒左右那次已过 2.5 秒窗口，于是照样整段跳过下载。实际效果：拖动后多卡约 3 秒，然后还是白下几百 MB 到几 GB，提示永远不显示。不会崩溃。修：在发起拖动时记录目标位置/代号，让该位置的所有重试都受限（不按时间）；或抛不可重试的异常（例如 DataSourceException(ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)）。另外 PlayerGestures 横滑和双击快进走 seekTo(int)，不受上限，按需决定。
M4 中低｜JellyfinTrickplay.java:12/:17 —— 大图已缓存时，frame() 在主线程每个 MOVE 事件都新建 BitmapRegionDecoder 并解码区域（基线 JPEG 要从顶部解到该行，约半张 3200×1800），Jellyfin 拖动预览会掉帧。catch 分支用 inSampleSize=tileW 解出整张拼图缩小版，会显示错误的画面。修：区域解码放 PreviewFetch.IO，只解最新的位置；每张拼图保留一个解码器；失败时返回 null。
M5 低｜ScrapedMainActivity.java:203 —— setHeroes 在主线程把完整 items（Stremio 一整行，含简介，可达几百 KB）toString 成缓存内容并比较，每次 offerHeroes 都做。修：只取前 5 条，并在 job() 里序列化。
M6 低｜InstallDefaults / ScrapedMainActivity.java:12 —— 升级判断逻辑正确（有历史、插件、服务器、来源、外观或旧引导标记就不改），测试覆盖了。但 0.1.125–0.1.129 已经被强制改成暗金 + 聚合首页的用户（包括本用户）不会恢复；如有需要可在设置里提示。hadUse 没看 themeMode（每次启动都会写），可作为额外信号。
M7 低｜CacheFiles.java:15-16/:43 —— MEMORY 最多 160 条原始字符串（含 100–300KB 的目录 JSON，按 UTF-16 存储），可能占十几 MB；remember() 对每次读写都整体解析一次 JSONObject（目录是数组，会快速失败，没问题）。非 meta 的缓存文件没有总量上限（每个影视仓站点/分类各一份）。修：MEMORY 按字节限制；非 meta 缓存也做总量清理。
M8 低｜HomeSources.java:10 —— seed() 清理已下线公共源的缓存时只看 prefs，迁移后这些缓存在 CacheFiles 里，清不到（只是陈旧缓存）。
M9 低｜EmbyLibraryActivity episodeMenu —— 标记已看/未看后只换那一行，但顶部「继续/播放 第 X 集」按钮不会更新。
M10 低｜build_ui.py:116-121 —— 没有 keystore 时静默输出未签名包，用的还是发布文件名，verify_apk 也不查签名。打正式包时如果环境变量没设，会得到一个装不上的包。建议：加显式 --unsigned 参数，否则直接报错。
M11 细节｜Skeleton.TAG_ANIM 与 SubtitleStyle.TAG 都是 0x7f0a7302（目前作用在不同的 View 上，不冲突，建议改掉）；FadeFrame 叠加色只在 syncHeroFade 时更新，默认外观取色变化后底部渐变可能和背景有色差。

## 其余已核对没问题
- 合并线程 / 合并画 / epoch：paintGen 丢弃过期结果，ui() 检查 epoch + homeCanvas；旧页面延迟执行的 paintLater 会被 alive() 挡掉；合并读的是浅拷贝，主线程只改拷贝的顶层字段。
- 文件缓存：所有读写都在后台线程，并且在同一把实例锁里；.tmp + 改名；系统清理 cacheDir 只会导致重新请求；迁移在启动后台进行，导入后才从 prefs 删除（删的都是可重新下载的缓存，不会丢用户数据）；isBlob 前缀不会误伤 fusion.homeProvider 等设置。
- 409：改为 RETRY 调度器 2 秒后重新投递到 IO，只重试一次，不再 sleep，也不再重复 release；重试前检查取消和 isFinishing。
- Trickplay 区域坐标：x = (n % TileWidth) × Width，y = (n / TileWidth) × Height，并按解码器尺寸裁剪，正确。
- 内置播放器 UA 恢复为 Mozilla/5.0；VLC/离线下载仍用 MediaPocket。

---
# 复审 2：06b5a50（对比 80e6e40）
测试：JVM 85/85 通过；test_media3_offline.py 通过；build_ui.py --unsigned 通过；verify_apk.py 通过（显示 apk signature: absent）。不加 --unsigned 又没有 keystore 时，build_ui 会拒绝输出（EXIT 1）。已核对：0.1.120 签名包里有 v1 的 .RSA/.SF，所以 verify 的签名检测对正式包有效。
0.1.120 修复：本次没改 RequestHeaders / OfflineService / BackupRules。
结论：需要修 1 处（N1）再合并，N2 建议一起修。其余各项都已真正修好。

## 已修好的项
- M1 BIF 内存：已修。BifCache 只留 2 份 LRU；Media3Controls.dispose、PlayerChrome.dispose（VLC）、SystemPlayerActivity.onDestroy 都会调用 PreviewFetch.evict()。播放页自己持有的 bif 字段随页面释放。
- M2 共享对象：已修。list()/get() 每次返回深拷贝；切换客户端后会 save；authenticate 失败时只改了调用方自己的那份，不会进缓存。查了所有调用方，没有发现依赖「原地修改共享对象」的写入（编辑页登录走 authenticate + save；401 走 authenticate + save）。但见 N1、N2。
- M3 跳读上限：已修。错误码 2008（READ_POSITION_OUT_OF_RANGE），Exo 不会重试；同一字节位置的重试仍然被拦；位置 0 和没 arm 的打开（续播、首次打开、读 moov）不拦；出错 / stopPlayback 时 disarm，READY 且离目标 3 秒以内时 landed。出错后点「重新获取地址」走 seekTo(int)，不受限，不会永久卡死。错误提示会显示 SKIP_MESSAGE。见 N3。
- M4 Trickplay：已修。解码在预览线程，最新请求优先（wantedMs/busy），每张拼图一个解码器，Tile.crop/close 加锁，LRU 淘汰和 close() 时会 recycle，失败返回 null；过期的 bitmap 会被 recycle。
- 低优先级项：setHeroes 只序列化前 5 条，并在 job 里做（按 gen 去重）✓；CacheFiles 内存约 512K 字符、非 meta 文件总量约 8MB、≤64K 才解析 ✓；已下线源的缓存文件会清理 ✓（见 N4）；标记已看后主按钮会重算 ✓；build_ui --unsigned ✓；verify 会记录签名 ✓；Skeleton 标记改成 0x7f0a7303 ✓。

## 新问题
N1 中高｜EmbyStore.java:14 raw()（以及 :17 save、:18 delete） —— listRaw 第一次读取后就不再和 prefs 对比。BackupActivity.apply（BackupActivity.java:58-71）直接用 Editor 写回 emby.servers，然后 restart()（:73）只在同一个进程里重开首页。结果：恢复备份后 Emby 服务器列表仍显示旧的；之后任何一次 EmbyStore.save（编辑、切换客户端、401 重新登录）都会以旧的 listRaw 为底写回，把刚恢复的服务器覆盖掉（数据丢失）。80e6e40 版本每次都和 prefs 字符串比较，没有这个问题。修：raw() 每次读 prefs.getString（在内存里，很便宜），不一样就更新；或者在 BackupActivity.apply 后把 listRaw 置空。
N2 中｜EmbyClient.java:72（新增的 save）和 :73（原有的）+ EmbyPlayback.java:28 —— EmbyPlayback 按地址逐个尝试时，用的是 attempt 拷贝（url 换成当前地址，删掉 backups）。在这个拷贝上触发切换客户端或 401 重新登录时，EmbyStore.save(c, attempt) 会把整条服务器替换掉：主地址变成正在尝试的那个备用地址，备用地址列表丢失。401 那条以前就有，这次切换客户端也会触发。修：save 前把 url/backups 合并回存储里的原记录（例如 EmbyStore.update(id, 只更新 client/token/userId)），而不是整条替换。
N3 低｜HttpRange.java:22-28 SeekCap —— 在缓冲区内拖动时，Exo 可能一直是 READY，不会触发 onPlaybackStateChanged，于是 gate 一直处于 armed。之后对「忽略 Range」的服务器，如果在 64MB 之后的位置重新连接（断线重连），会被误判成这次拖动而报错。只影响忽略 Range 的服务器。修：在 onPositionDiscontinuity（SEEK）或第一次被认领的请求成功后就 disarm，或加一个较长的超时作兜底。
N4 低｜HomeSources.java:12 seed()（:11 pruneRetiredCaches） 在 onCreate 主线程调用 pruneRetiredCaches → CacheFiles.of() 第一次会在主线程读 index.json，并且拿 CacheFiles 实例锁；如果后台正在写大缓存，主线程会等。修：放进 job()。
N5 细节｜heroSaveGen 跨线程读写但不是 volatile（最坏只是多写一次缓存）；JellyfinTrickplay.close() 在主线程等 Tile 锁，可能等一次 decodeRegion 做完（几十毫秒）。

---
# 复审 3：cc7bff4（对比 06b5a50）
测试：JVM 85/85 通过（0 失败；新增用例放在已有的 EmbyStoreCopyTest / HttpRangeSeekTest 里）；test_media3_offline.py 通过；build_ui.py --unsigned 通过（0.1.130 / 131）；verify_apk.py 通过（signature: absent，未签名包符合预期）。
结论：**可以合并**。复审 2 的 5 项都已修好，没有发现中等及以上的新问题。下面只是非阻塞的低优先级说明。

## 复审 2 各项核对
- N1 恢复备份后旧列表覆盖：已修。EmbyStore.java 里 raw()/save()/mergeAuth()/delete() 都在锁内先用 fresh(listRaw, prefs) 和偏好对比；BackupActivity.apply 在 finally 里调用 EmbyStore.invalidate()（BackupActivity.java:72）。所有写入方都检查过：emby.servers 只有 EmbyStore 自己和备份恢复会写（BackupRules:64 只计数，ScrapedMainActivity:12 只调用 contains）。save 之后 getString 返回的是同一个字符串对象，所以比较很便宜。
- N2 试播副本回写：已修。EmbyClient.java:72/73 改用 EmbyStore.mergeAuth：按 id 只写回 token/userId/client（副本没有 client 就移除，和 authenticate 的行为一致）。url、backups、name 都不动。副本没有 id 或找不到对应记录时，原样返回，不新增，也不会生成重复记录（旧的 publish 会随机生成 id 再追加一条）。完整 save 只在编辑页（EmbyLibraryActivity:46/49）。
- N3 跳读闸：已修，而且不会挡正常情况。放下闸的条件：READY 时落在目标附近；SEEK/SEEK_ADJUSTMENT 的位置跳变，且 ready 并在目标附近；任何不是拖动造成的跳变；被认领的打开读到数据（服务器接受 Range，或忽略 Range 但跳过量在 64KB 到 64MB 之间）；超过 30 秒（rejects 里会自己检查 deadline，另外 postDelayed 调 expireSeek）。超时只会取消上限，不会挡住正常播放；出错 / stopPlayback 照旧放下。
- N4 pruneRetiredCaches：已改到 job()（ScrapedMainActivity.java:16，和 migrateAll 一起，在后台）；HomeSources.seed 里已经没有这个调用。
- N5 heroSaveGen 改成 volatile（ScrapedMainActivity.java:9；只在主线程 ++，后台只读，没问题）。JellyfinTrickplay.close()（JellyfinTrickplay.java:17-18）在主线程只作废请求，releaseSheets 放到 PreviewFetch.IO。IO 是单线程执行器，所以释放一定排在正在跑的 pump 之后，不会出现解码中途被 recycle 或关闭后再重建解码器的竞态。

## 非阻塞的低优先级说明
L1 HttpRange.java:41 SeekCap.discontinuity：Media3 在 seekTo 的时候会先把状态切到 BUFFERING，onPositionDiscontinuity(SEEK) 被调用时 getPlaybackState() 多半不是 READY（Media3VideoView.java:42）。所以这一条基本不会生效，实际靠 landed（缓冲里的拖动也会走 BUFFERING→READY）、opened 或超时来放下。这只是多余的兜底，不是 bug；建议真机打日志确认一次。
L2 HttpRange.java:27/46 超时 30 秒用的是 System.currentTimeMillis（墙上时钟），改系统时间会提前或推后超时，建议改成 SystemClock.elapsedRealtime。另外，如果同一认领位置的 Exo 重试（连接超时 15 秒 ×2 次以上）拖过了 30 秒，后面那次就不受上限约束了，只影响忽略 Range 的服务器，而且很少见。
L3 闸仍是全局静态，opened/rejects 所有 ScopedHttpDataSource 共用；只有一个播放器时没有影响。
L4 EmbyLibraryActivity.java:49 隐藏媒体库那次 save 是用之前拿到的副本整条写回：如果中途 mergeAuth 刷新过 token，会被旧 token 盖回去，下一次请求 401 会自动重新登录，可以自愈。建议这里也改成按字段合并。
