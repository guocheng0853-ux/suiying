# Handoff — 随影 rust-anything → 本地 Agent

给**本地 Cursor Agent（Local）**继续：装包、接 fullscreen JNI、接着抽 Rules。  
云端 Cloud Agent 碰不到用户 USB 手机；本机已有 `adb` + 设备 `11a279bc`。

## 立刻要做（装包）

1. 本机 PowerShell：
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
   # → 11a279bc        device
   ```
2. 打 **0.1.193** 签名包并覆盖安装（config-text JNI 已进 tip）：
   ```powershell
   $env:MEDIAPOCKET_KEYSTORE = "…\cinema-ui\signing.jks"  # 勿提交
   # 需可用的 python3；然后 cinema-ui/build_ui.py → adb install -r
   ```
3. 证书 SHA-256：`87a44d822580215fff01d604b1fedb31e52d69b42eb85ffbcd8b65aea21783d1`（与 `PROJECT-GUIDE.md` §5 一致）
4. 本机重签：`cinema-ui/signing.jks`（**gitignore，勿提交**）。别名 `mediapocket`，密码 `android`。

## Git / PR

| 项 | 值 |
| --- | --- |
| 分支 | `cursor/rust-anything-whole-app-26d5` |
| PR | https://github.com/guocheng0853-ux/suiying/pull/61 |
| tip（config-text JNI） | **0.1.193** `ConfigTextRsNative` + 三 ABI `libconfig_text_rs.so`（ADR-010） |
| base | `main` |

## 已完成单元

| 单元 | 状态 |
| --- | --- |
| `rust/rules-rs` | gate **483/483**；JNI 在 APK（0.1.191+） |
| `rust/config-text-rs` | gate **1515/1515**；JNI 在 APK（0.1.193，ADR-010） |
| `rust/fullscreen-rs` | **diff 1644/1644**；`sameLook` NaN 短路 + `stage` int 环绕已修；门面/JNI 未接 |
| 门面 | `TvBoxConfigText` → `ConfigTextRules`；`TvSpiderRules` → `SpiderComponentRules` |

读：`rust/DECISIONS.md`（ADR-001…010）、`rust/MAP.md`、`PROJECT-GUIDE.md`。

## 本机工具备注（Windows）

- 中文用户路径会让 MinGW `ld` / 默认 MCP `gate` 失败；用 `subst` 短盘符 + `CARGO_TARGET_DIR=C:\ra-target\…` + `rust-lld`（见 `rust/fullscreen-rs/.cargo/config.toml`，本地勿强依赖入库）。
- `record` 的 `javac` 命令行过长时，把 crate/portable 拷到 `C:\ra\fs` 再录，再把 `cases.jsonl` 拷回。
- 本机缺完整 VS `link.exe` / Windows SDK 时，用 `stable-x86_64-pc-windows-gnu` + `rust-lld`。

## 建议下一步（按序）

1. 签包安装 **0.1.193**，设备冒烟
2. `FullscreenRules` 等改成门面；只加 `FullscreenRsNative`；Android `.so` → `runtime-native`；bump **0.1.194**；再装
3. 再抽下一批无 Android 依赖的 `*Rules`
4. MCP `gate` 在本机路径下仍可能因 AppData 中文路径假失败；以本地 `clippy -D warnings` + `diff` 为准

## 粘贴给本地 Agent 的一句话

> 接随影 PR #61 分支 `cursor/rust-anything-whole-app-26d5`。0.1.193 config-text JNI 已推；`fullscreen-rs` diff 1644/1644。先签包装 0.1.193，再接 fullscreen 门面+JNI（只加 FullscreenRsNative），bump 后打包安装。签名用 `cinema-ui/signing.jks`，勿提交。
