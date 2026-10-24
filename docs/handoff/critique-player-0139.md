# 随影 0.1.139 播放器「三层悬浮面板」— 设计评审报告

**对象**：main `bd6dc0f` / versionName **0.1.139** / versionCode **140**  
**范围**：界面设计（非 crash/安全审查）；依据 `PlayerGlass` / `PlayerGlassRules` / `Media3Controls` / `PlayerOverlay` / `PlayerTheme` / `DESIGN-PAPER-CINEMA.md` / `cinema-ui/README.md`  
**真机截图**：仓库与盒子均无 0.1.139 播放页截图；`/workspace/handoff/player-0139/` 已建目录但为空。未装包、未 emulator 截屏。  
**方法**：花叔 Design `critique-guide` + Impeccable v4.5.1（插件缓存）`critique` / `audit.native` / `android` / `craft-floor` / `quieter`。  
**Impeccable 加载说明**：Cursor Skill 目录尚未挂上 `impeccable` 名；本轮从 `/home/box/sand-data/plugins/cache/cursor-public/impeccable/.../skills/impeccable` 直接读手册并跑了 `impeccable detect`（对 `.java` 返回 `[]`，原生 Views 无 HTML 规则命中）。无 PRODUCT.md / DESIGN.md。  
`⚠️ DEGRADED: single-context (无子代理拆 A/B；无真机/模拟器可视证据；detect 对 Android Views 空结果)`

---

## 一句话结论

**总评 6.8 / 10（良好）**。立意清楚——「把控件拆成悬浮岛，让画面透气」——并已统一 Media3 / VLC 的三层骨架；但辨识度仍偏 Infuse 类目模板，底栏认知负载偏高，亮画面对比与 48dp 触控未达标，影院暗金主题未进播放页。**不必重开三方向**；应在方向 2 内做一轮 quieter / layout / theming 精修。

| 框架 | 分数 | 档位 |
|------|------|------|
| 花叔 Design 总评 | **6.8 / 10** | 良好（6–7.9） |
| Impeccable Nielsen 启发式 | **26 / 40** | Good（约 20–32 常见带） |
| Impeccable Native Audit | **11 / 20** | Acceptable |

概念维 **7**（未触发 ≤5 → 总评封顶 6.0 的规则）。

---

## 三层结构图解（文字）

```
┌────────────────── 画面（全出血，无通栏黑渐变） ──────────────────┐
│  [顶层 · 导航岛]                                                   │
│   ◯返回  【片名胶囊】          【DV/解码胶囊】 ◯比例 ◯锁/PiP ◯更多 │
│   背景仅 topFade 0x66→透明；控件 = 暖炭玻璃圆/胶囊                  │
│                                                                    │
│                         [中层 · 主操作岛]                            │
│                      ◯−N    ◉播放(乳白盘)    ◯+N                   │
│                   56 / 76 / 56 dp，间距 36dp；拖进度时收起           │
│                                                                    │
│  [底层 · 进度岛 · 一整块圆角玻璃面板 r=22]                           │
│   集名(16 bold) .................... 剩 X 分钟，HH:MM 看完 (13)    │
│   ═══════════════ SeekBar ════════════════════════════════         │
│   00:00/…   [上一集][下一集][选集][音轨][字幕][倍速]  (Media3 文字) │
│                                                                    │
│  字幕：底栏可见时 bottomMargin≈max(150dp, bottomH+22)               │
│  侧：截图圆（左中）；锁屏在 VLC 侧栏体系 / Media3 在顶栏             │
└────────────────────────────────────────────────────────────────────┘
```

来源：`PlayerGlass` 类注释；`Media3Controls` 构造；`PlayerOverlay.top` / `glassCenter` / `panel`。  
设计叙事（README）：花叔三方向里选 **方向 2（Infuse 参照的三层悬浮）**，并借入方向 3 的「剩余时间」一行。

色值锚点：`GLASS=0x8C1C1A18`（约 55% 暖炭）、`DISC=#F4F1EA`、进度/倍速强调 `PlayerTheme.ACCENT=#e6a58d`；播放页**强制深色**，不跟纸面浅色。

---

# 栏 A · 花叔 Design

## 分项评分

| 维 | 分 | 一句话 |
|----|----|--------|
| 0 概念/立意 | **7** | idea：「悬浮岛让影片当主角」——来自暗环境横屏观影，不是纯装饰；但母题可原样搬到任意播放器（Infuse 参照），随影纸面身份未进此页。 |
| 1 哲学一致性 | **6** | 与 DESIGN-PAPER-CINEMA「播放保留深炭 + 暖陶选中」大体一致；**影院暗金 `#e0b25c` 完全缺席**，夜间外观用户在首页见金、进播放见陶土，哲学断裂。 |
| 2 视觉层级 | **7** | 播放盘 76dp 明显主焦点；顶 44 / 跳秒 56 次之；「剩…看完」13sp 次要色意图对，但长中文句与 16sp 集名同排，眯眼时易并成一条噪音带。 |
| 3 细节执行 | **6.5** | `PlayerGlass` 常量统一好；无真 backdrop-blur（注释诚实），实底 + 1dp 描边更像贴纸；Media3 直接设 panel、VLC 走 `InsetDrawable` 路径不完全同构；`−N` 用 MONOSPACE 偏「技术戏服」。 |
| 4 功能性 | **7.5** | 锁屏/PiP/侧栏/拖进度收正中键；字幕上抬；剩余时间按倍速折算有测试。触控与双击分区叠在中心时仍可能误触；Media3 底栏文字操作一排偏挤。 |
| 5 原创性 | **6** | 方向 2 自承 Infuse；「剩 X 分钟，HH:MM 看完」是少数产品味触感；整体仍像优质类目皮肤而非随影独有。 |

**总评算法**：App UI 侧重功能性与细节；概念未踩封顶。综合 **6.8**。

## 优点（Keep）

1. **去掉通栏黑渐变**——画面重新成为最大色块，符合「暗房观影」场景，比旧通栏 chrome 更像电影而不是工具条。
2. **乳白播放盘 + 深色图标**（`DISC` / `DISC_ICON`）——在炭玻璃岛里唯一高反差实体，主操作一眼可寻。
3. **剩余时间文案**（`PlayerGlassRules.remaining`）——「马上看完」、倍速折算、向上取整分钟，是方向 3 借来的好句子，比单纯 `—28:00` 更贴中文观影习惯。
4. **双播放器共骨架**——顶岛 / 中岛 / 底岛同一套 `PlayerGlass`，叙事统一，比各改各的强。

## 必须改（花叔视角）

1. **亮场面对比**：白字叠在 `GLASS` 透到白雪/字幕高光时，估算对比约 **3.9:1**（&lt; 4.5）。须加厚面板（用 `GLASS_STRONG` 或动态加不透明）或加强 `topFade`，并真机拍亮场景验证。
2. **底栏认知过载**：「集名 + 长剩余句 + 进度 + 时钟 + 最多 6 个文字操作」同岛同时亮；Operate 场景应让进度与播放优先，其余进「更多」或二级。
3. **正中三键常驻抢戏**：控件显现时 76dp 盘钉在画面几何中心，与「看片」冲突。改为：点按唤出后短暂显示，或默认只显播放、±N 收入双击/手势（Infuse 也更克制）。
4. **主题哲学**：`cinemaAppearance=gold` 时播放强调色仍应能落到 `#e0b25c`（或文档明确「播放永远陶土」并改掉用户预期）。
5. **Media3 文字底栏 vs VLC 图标底栏**：同一「方向 2」却两套操作语言，削弱统一感；择一为主。

## 建议改

- 顶栏「DV」胶囊对普通用户噪音大，可缩成图标或塞进「更多」。
- 片名胶囊与解码胶囊同为白字 pill，权重接近；片名可略大/略实，解码更淡。
- 跳秒标签去掉 MONOSPACE，改用与面板一致的无衬线数字（tabular）。
- VLC `glassCenter` 依赖 `refresh`：首帧空窗属体验毛刺（代码审查 L4），设计上应保证进页即有中岛或明确延迟策略。

## 可保持

- 播放页不跟纸面浅色、不用 Noto Serif（说明书已写明）。
- 拖进度收中岛、字幕随底栏上抬。
- 圆角 22 / 触控意图 44 的尺度系统（但 Android 规范见栏 B，建议提到 48）。

## 是否重开三方向？

**否。** 方向 2 的信息架构（三岛）成立，问题在材质深度、主题一致性与 Operate 静音，不在「选错了哲学家族」。若用户明确要「纸面身份进播放器」或「彻底去 Infuse 感」，再开三方向。

---

# 栏 B · Impeccable

**Visitor mode**：Operate（任务：看片 + 偶发控制）。  
**平台**：Android native（`audit.native` + `android.md`）。  
**Craft-floor 命中**：Glass 作默认表面（接近「glass as decoration」）；MONOSPACE 作跳秒戏服。

## Design Specificity Verdict

**偏类目可替换（Infuse / 现代播放器悬浮玻璃）**，尚未写进随影独有世界。纸面陶土与暗金是产品资产，但播放页只用了通用暖炭 + 陶土强调，封面/品牌字形未参与。  
Deterministic scan：`impeccable detect` 对 Java 目标返回空数组——**无 HTML 规则命中，不能当作干净**；以源码与对比估算为准。

## Nielsen 启发式（0–4）

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | 进度/剩余/播放态清楚；亮场玻璃可读性不稳 |
| 2 | Match System / Real World | 3 | 「剩…看完」极好；「DV」偏行话 |
| 3 | User Control and Freedom | 3 | 单击显隐、解锁菜单；系统 Back 仍可用 |
| 4 | Consistency and Standards | 2 | Media3↔VLC 底栏语言分裂；锁/PiP 位不一致 |
| 5 | Error Prevention | 3 | 拖进度藏中键；时长未知不瞎写剩余 |
| 6 | Recognition Rather Than Recall | 3 | 图标+文案大体可认 |
| 7 | Flexibility and Efficiency | 3 | 双击跳、长按临时倍速、偏好跳秒 |
| 8 | Aesthetic and Minimalist Design | 2 | 控件显现时三岛同时抢注意力（Operate 过噪） |
| 9 | Error Recovery | 2 | 本表面错误恢复弱（依赖既有失败 HUD） |
| 10 | Help and Documentation | 2 | 仅首次手势说明 |
| **Total** | | **26/40** | **Good** |

## Cognitive Load

失败项（约 3/8 → moderate）：

- **Single focus**：控件开时顶/中/底 + 侧截图同时争视线。  
- **Minimal choices**：Media3 底栏操作常 **≥5**（上一集/下一集/选集/音轨/字幕/倍速）。  
- **Progressive disclosure**：次要解码档位、多操作未收进「更多」。

## Persona Red Flags

- **暗房连续剧观众**：中岛常驻挡住人脸/字幕区情绪点；5s 自动藏虽有，点一下又全开。  
- **平板横屏用户**：底岛加长剩余句 + 多按钮，窄高密度风险（无平板播放截图可证）。  
- **TalkBack 用户**：有 `contentDescription`，但 44dp &lt; 48dp 规范；亮场对比可能听得见点不准则。

## Native Audit（0–4 × 5）

| # | Dimension | Score | Key Finding |
|---|-----------|-------|-------------|
| 1 | Accessibility | 2 | 亮场对比估算失败；触控 44dp &lt; **48dp**（android.md） |
| 2 | Performance | 3 | dress/paintPlay WeakHashMap；300ms pulse 可接受 |
| 3 | Appearance & Theming | 2 | 大量硬编码 hex；暗金主题不映射；非 Material roles |
| 4 | Platform Conformance | 2 | 全定制播放 chrome（行业常见）但触控与系统字号缩放未系统验证 |
| 5 | Adaptivity | 2 | 横屏播放为主；平板/字号放大无专项布局 |
| **Total** | | **11/20** | **Acceptable** |

## Impeccable 红旗清单

| 严重度 | 红旗 | 建议命令 |
|--------|------|----------|
| **P1** | 亮画面上白字/次要字对比不足（估算 white@glass/white ≈ 3.9） | `/impeccable harden`（对比）+ 真机截屏 |
| **P1** | 触控圆 44dp &lt; Android 48×48 | `/impeccable adapt` |
| **P1** | 控件显现时视觉过噪（三岛同开） | `/impeccable quieter` |
| **P1** | 底栏选项墙（5–6 操作） | `/impeccable distill` / `layout` |
| **P2** | 影院暗金未进入播放强调色 | `/impeccable colorize`（按品牌 token） |
| **P2** | Media3 / VLC 不一致 | `/impeccable clarify` + 统一组件 |
| **P2** | Glass 无深度、描边贴纸感 | `/impeccable polish`（材质，非重做世界） |
| **P3** | 跳秒 MONOSPACE 戏服 | `/impeccable typeset` |
| **P3** | 顶栏 DV 胶囊噪音 | `/impeccable quieter` |

## Impeccable 正向

- 剩余时间文案是 Operate 模式里少有的「产品句子」。  
- 状态显隐与拖进度互斥逻辑清楚。  
- 双引擎共享 `PlayerGlass` 常量，利于后续 token 化。

---

# 合并建议（给下一轮改版）

按影响排序，**不必重开三方向**：

1. **Quieter 中岛**：控件显示时默认只保留播放盘，或缩短中岛可见时长；±N 依赖双击分区（已存在）→ 对齐花叔「必须改 3」+ Impeccable P1 quieter。  
2. **加厚玻璃 / 动态不透明**：亮场对比达标；底岛可用 `GLASS_STRONG` → 花叔必须改 1 + Impeccable P1 harden。  
3. **蒸馏底栏**：保留进度+时钟+剩余（剩余可改为单行缩短或仅拖进度时展开）；音轨/字幕/选集进「更多」或溢出菜单 → 花叔必须改 2 + distill。  
4. **触控 48dp** + 验证系统字体缩放 → adapt。  
5. **主题映射**：gold → 播放强调 `#e0b25c`（或文档冻结「播放恒陶土」）→ colorize / 哲学一致性。  
6. **统一 Media3/VLC 操作语言**（优先图标或优先短文案，二选一）。  
7. **真机证据**：亮雪景、暗夜戏、字幕开、平板横屏、TalkBack 各一帧，落入 `/workspace/handoff/player-0139/`。

建议命令序：`quieter` → `distill`/`layout` → `adapt` → `harden` → `colorize` → `polish`。

---

## 资料与局限

- 代码：`/workspace/wt-review-0139/cinema-ui/src/.../PlayerGlass*.java`、`Media3Controls.java`、`PlayerOverlay.java`  
- 设计说明：`cinema-ui/README.md` 文首、`DESIGN-PAPER-CINEMA.md`  
- 代码审查背景：`/workspace/handoff/review-0139.md`（未复述 crash 清单）  
- 画布「随影界面样图」：本环境 `ListCanvases` 为空，未见方向稿截图  
- Impeccable：插件缓存 v4.5.1；Skill 名未进工作流目录；无 PRODUCT.md  
- Questions skipped: 本轮为父代理委托的离线报告交付，不向终端用户发问；建议父代理转问：优先 quieter 中岛 / 加厚玻璃 / 蒸馏底栏 哪条先做。

