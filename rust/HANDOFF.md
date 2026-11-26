# Handoff — 随影 rust-anything → 本地 Agent

给**本地 Cursor Agent（Local）**继续：装包、收尾未提交改动、接着抽 Rules。  
云端 Cloud Agent 碰不到用户 USB 手机；本机已有 `adb` + 设备 `11a279bc`。

## 立刻要做（装包）

1. 用户本机 PowerShell 已通：
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
   # → 11a279bc        device
   ```
2. 下载签名包后覆盖安装（包名 `com.guocheng.mediapocket.next`）：
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "$env:USERPROFILE\Downloads\MediaPocket-0.1.192-config-text-arm64.apk"
   ```
3. 云端已打好的签名包路径（需用户从对话产物下载到 Downloads）：
   - `MediaPocket-0.1.192-config-text-arm64.apk`（~39MB）
   - APK SHA-256：`53fd7e76a830febbf6bf2b48ca8b9d3e8e655da173929bd21895cc86e80952f1`
   - 证书 SHA-256：`87a44d822580215fff01d604b1fedb31e52d69b42eb85ffbcd8b65aea21783d1`（与 `PROJECT-GUIDE.md` §5 一致）
4. 若本机要重签：`cinema-ui/signing.jks`（**gitignore，勿提交**）。用户有副本；别名 `mediapocket`，密码 `android`。用 `python3 cinema-ui/build_ui.py`（不要 `--unsigned`）。

## Git / PR

| 项 | 值 |
| --- | --- |
| 分支 | `cursor/rust-anything-whole-app-26d5` |
| PR | https://github.com/guocheng0853-ux/suiying/pull/61 |
| 已推 tip | `e7af48c` — **0.1.192** config-text Rules 抽出 + Rust gate（APK 当时仍是纯 Java Rules） |
| base | `main` |

**工作树有未提交内容**（云端打签名包时已编进 APK，但还没 commit/push）：

- `ConfigTextRules` → JNI shadow + `ConfigTextRsNative.java`
- `runtime-native/*/libconfig_text_rs.so`（三 ABI）
- `scripts/run_tests.sh` 同时加载 `librules_rs.so` + `libconfig_text_rs.so`
- `portables/fullscreen/` + `rust/fullscreen-rs/`（进行中，gate 未过）
- `rust/config-text-rs/jniLibs/`（Android 产物，是否入库看仓库惯例；rules 的 `.so` 在 `runtime-native/`）

本地先 `git status` / `git pull`，再决定提交或继续改。

## 已完成单元

| 单元 | 状态 |
| --- | --- |
| `rust/rules-rs` | gate **483/483**；JNI 在 APK（0.1.191+） |
| `rust/config-text-rs` | gate **1515/1515**；**JNI 接线已在工作树**（只加 `ConfigTextRsNative`，共用已有 `rustanything.*`，避免第二份 Wire/Error 冲突） |
| 门面 | `TvBoxConfigText` → `ConfigTextRules`；`TvSpiderRules` → `SpiderComponentRules` |

读：`rust/DECISIONS.md`（ADR-001…009）、`rust/MAP.md`、`rust/config-text-rs/README.md`、`PROJECT-GUIDE.md`。

## 进行中：fullscreen 簇

- Portable：`portables/fullscreen/com/guocheng/mediapocket/fullscreen/`  
  （`FullscreenRules` / `VideoFullscreenRules` / `Media3ControlRules`；重载已改成 `smartDefault`/`lookDefault`/`boxDefault` 等唯一名）
- Crate：`rust/fullscreen-rs`，约 5720 cases；port 已写大半
- **未过 diff**（上次）：
  - `sameLook`：NaN/Infinity 时 Java 短路返回 `false`，不要再取 `[1]` 抛越界
  - `stage`：`statusTop+navBottom` 用 `int` 会溢出（如 `42+2147483647`），需按 Java 32 位环绕再比 `windowH`
- cinema-ui 里原包名 `FullscreenRules` 等仍是包内类，**尚未改成门面**；gate 过后再接 JNI（同一套路：只加 `FullscreenRsNative`，勿复制整套 `rustanything`）

## 目标与规矩

- 用户要：**一个一个实现 Rules → 最后打包安装**
- 形状：Android 库 + JNI，不是 CLI；UI / `base.apk` 暂留（ADR-001/007）
- 等价：录制用例精确匹配；勿弱化校验
- **永远不要** commit `signing.jks` / 真实 `fusion-default.json` / 令牌
- 版本：改 APK 类集就 bump（现 **0.1.192 / code 193**）；下一波再升 0.1.193
- JVM：`sh scripts/run_tests.sh`（需先有 `build/classes`，通常跑过 `build_ui.py`）
- 更新 PR 时保留人类改过的 title/body，除非已完全不准

## 建议本地下一步（按序）

1. 帮用户把 **0.1.192 签名包** `adb install -r` 装上；确认启动
2. 提交并推送「config-text JNI 入 APK」工作树（或 rebase 后整理），更新 PR #61
3. 修完 `fullscreen-rs` diff → `gate` → 门面 +（可选）Android `.so` → bump → 再签包装机
4. 再抽下一批无 Android 依赖的 `*Rules`（见 `cinema-ui/src/**/*Rules.java`）

## 粘贴给本地 Agent 的一句话

> 接随影 PR #61 分支 `cursor/rust-anything-whole-app-26d5`。先按 `rust/HANDOFF.md` 用本机 adb 覆盖安装 0.1.192 签名包；再处理未提交的 config-text JNI 与未完成的 `rust/fullscreen-rs`（sameLook / stage），继续一个 Rules 一个 Rules 做完就打包安装。签名用 `cinema-ui/signing.jks`，勿提交。
