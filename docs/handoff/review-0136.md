# 随影 0.1.136 审查（复审）—— 对比 600df6f（0.1.135）→ d012e49（PR #27）
工作树 /workspace/wt-review-0136。
测试：JVM **86/86** 通过；test_media3_offline.py 先前已过（本轮 JVM 全量复跑）。
首审（7e0253d）有 **M1**；复审针对 d012e49「空的 Emby 最新列表不再覆盖已有缓存」。
结论：**可合并**。M1 已关闭；无中等及以上阻塞项。

## M1 关闭核对（d012e49）
- `AggregateRules.embyKeep` / `weakerEmbyLatest` / `latestCount`：已有非空 latest，本次空或 `<一半` → `writeCache=false, keepRows=true`；失败一律不写盘；无旧缓存时可写。✓
- 聚合 `emby()`：仅 `plan.writeCache` 时 `writeCache`；`applyEmby(..., keepRows)` 为真时只 `Row.complete`，不改 `embyCatalog`、不往行里塞空列表；resume 仍更新。✓
- `finishEmby`：只 `complete`，不再 `put(null)`。✓
- 测试覆盖空/弱/接近可替换/无缓存可写/失败保留。✓
- 纯 Emby 首页 `fetch(..., budget=0)` 未改。✓

## 首审范围（仍成立）
八项逻辑、硬约束（agg-merge / 300ms 首画 / signature / CacheFiles 等）、版本 0.1.136/137 —— 见下节首审摘要。无新 H/M。

## Low（不阻塞）
L1 `finishEmby(src, keepRows)` 的 `keepRows` 未使用（两条路径都只 complete），可删参数。
L2 `blockedLibrary` 未用 name。
L3 真机未验证。

---

## 附录：首审全文（7e0253d，结论已由复审取代）

# 随影 0.1.136 审查 —— 对比 600df6f（0.1.135）→ 7e0253d（PR #27）
只审后打包。工作树 /workspace/wt-review-0136。
测试：JVM **86/86** 通过（含新增 AggregateRulesTest 断言）；test_media3_offline.py 通过。未跑签名包（有 M 阻塞）。
0.1.130 硬约束：agg-merge 单线程、300ms 合并（首画）、content-signature、CacheFiles/readMigrating、EmbyStore/BifCache/FadeFrame 等均未改动相关文件。✓
结论：**不可合并**。存在 1 个 Medium；修复前不要合 main / 不要打签名包。

## 范围摘要
1 个提交、12 个文件（+371 / −109）。主线是聚合首页 8 项：分行按类型取片+跨行去重、大图规则、Emby 12s 总时限与继续观看先画、影视仓分类顺序与下拉失效、少闪（首画后再等约 500ms）、继续观看按标题去重、续集无年不合并与海报回补、历史/收藏排序。版本 0.1.136 / code 137 已对齐清单、build_ui、verify、VALIDATION、README。

## 八项核对
1. **分行去重** `FETCH_ROWS` / `withoutShown` / `collectShown`；Emby `rowForEmby` 按库名/题材归动漫·纪录；影视仓 `groupCategories` 收齐同类。✓
2. **大图** `pickHomeHeroes`：继续观看最多 1、要横图、排除已看完、Emby/影视仓优先，两边就绪才用 Stremio；`sameHeroIds` + 角标 `heroKicker`。✓
3. **Emby 继续观看先画 + 12s** partial 回调 `rowsOn=false`；`waitMs`/`EMBY_BUDGET_MS=12000`。见 **M1**。
4. **影视仓** 先 movie/series；`forceRefresh`→`CacheFiles.invalidateFreshness` 使 `fusion.home` 过期；同内容 `touch` 刷新时间戳。✓
5. **少闪** 未 `opened` 时 300ms/立即；已开画后 500ms 安静；签名不变不重画；`cascade` 仅内容变化时触发。✓
6. **继续观看** `Ident(…,true)` + `resumeTitle` 去「· 第 N 集」；`orderedResume` 按服务器列表顺序；`almostFinished` 90%。✓
7. **续集/海报** `sequelTag` 不一致或年份差>1 不合并；`fillEmpty` 只补海报/简介/横图，不换播放入口。✓
8. **排序** `Mix`/`arrange`：收藏↑、看完↓；fresh 按 `agg.time`；档内轮流。`merge(lists,limit)` 无 Mix 时行为与旧轮流一致。✓

## 硬约束
- `MERGE=newSingleThreadExecutor("agg-merge")`；合并/大图/继续观看/history 去重均在其上。✓
- 首画仍 300ms 合并；开画后改为 500ms（有意）。✓
- `painted` 内容签名跳过重绘；继续观看也有 id 签名。✓
- CacheFiles / EmbyStore / BifCache / FadeFrame 等不在本 diff。✓

## 问题（按优先级）

### M1 中｜Emby｜12 秒预算「空库成功」会写坏缓存并清空已画出行
路径：`AggregateHome.emby` 在 `EmbyHome.fetch(..., EMBY_BUDGET_MS)` **成功返回**后无条件 `writeCache`，再 `applyEmby(..., rowsOn=true, last=true)`。

当预算内只拿到 resume（或 views），各库 `latest` 全超时变成空数组时：
- `slice.fresh.length()==0` → 走 `else if(rowsOn&&last) r.put(src,null,true)`，把缓存先画好的电影/剧集/动漫/纪录 Emby 段清掉；
- 同时把这份「空 latest」写进 `emby-home-*.json`，下次冷启动也读到空库，直到某次完整拉成功。

这与 PR 自述「超时不会把上次缓存清成空白」不符。纯 Emby 首页仍用 budget=0（每步 30s），不受影响；问题只在聚合路径的 12s 总时限。

旧代码同样 `writeCache`+失败 `put(null)`，但每步 30s，空库「成功返回」极少见；12s 总预算把该路径变成常态风险。

**建议修法（任选其清晰者）：**
- 若本次 `latest` 全空且磁盘/内存已有非空缓存：跳过 `writeCache`，`applyEmby` 不要用 null 覆盖已有 `embyCatalog`（只 `embyClosed`+更新 resume）；或
- `finish`/last 时若 `fresh` 空且 `embyCatalog` 已有数据：保留目录，仅标记来源结束；或
- 比较新旧 latest 条目数，弱结果不覆盖强缓存。

fetch 抛错时的 `finishEmby`→`put(null)` 仍会清 UI（磁盘未写），同属「清空白」范畴，可一并改成「有缓存则保持」。

## 其余已核对没问题
- 主线程：history 去重改到 MERGE；JSON 合并不在 UI。无新增明文密钥。
- `EmbyApi.library` 放行动漫/纪录库名；音乐等仍 `blockedLibrary`。
- `offerHeroes` 同优先级改 `sameHeroIds`，减少大图抖动。
- 版本号三处一致：0.1.136 / 137。

## Low（不阻塞，可随后改）
L1 低｜`blockedLibrary` 未使用 `name` 参数（仅按 CollectionType）。  
L2 低｜`cascade` 在上行变更时会给后续行各排一次 500ms 重画，行多时略吵，有签名短路。  
L3 低｜真机未验证：多 Emby 冷启动、大图切换、「已看完」角标位置、影视仓多分类合并观感。  
L4 低｜`finishEmby` 失败清 UI 与 M1 同类，磁盘仍保留；修 M1 时建议一起处理。

## 交付（复审通过后）
- 已合并 PR #27 → main `60ef75e77f29c47a307ab40c9b9d4068f7a47abe`
- 签名包：`/workspace/out/suiying-0.1.136-signed.apk`（36,997,685 字节）
- 证书 SHA-256：`87a44d822580215fff01d604b1fedb31e52d69b42eb85ffbcd8b65aea21783d1`（匹配）
- verify_apk：PASS（signature present；86 JVM stamp）
- Drive：https://drive.google.com/file/d/1oeAIEDSu2KdcLV8Q4Fe1p-njTjQMmoeF/view?usp=drivesdk
