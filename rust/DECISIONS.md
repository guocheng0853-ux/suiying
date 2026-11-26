# Decisions — 随影 rust-anything 整应用迁移

Goal: rewrite MediaPocket's portable logic to Rust with rust-anything proofs; ship via JNI into the existing Android APK until a separate UI cutover.

## ADR-001: Shape is Android library + JNI, not the tools CLIs
Status: accepted (2026-11-26)
Context: `analyze --whole-app` mis-labeled the repo as a CLI because `cinema-ui/tools/*` and `scripts/EcjShim` expose `main`. The product users run is the Android APK (`base.apk` + `cinema-ui` extension), not those patch helpers.
Decision: Treat the app as an **Android Java library / extension layer**. Unit bridge is JNI; `build --profile android` later. Do not make a clap front door over the dex patch tools. Those tools stay Java and are out of the lift target.
Consequences: Whole-app `app record` / CLI front-door do not apply. Proof is per-unit `record`/`diff`/`gate` on the host JVM first, then Android device when NDK/adb are available. Lift means pure-Rust crates behind JNI shadows inside the APK, not a standalone desktop binary.

## ADR-002: Whole-app goal, phased implementation
Status: accepted (2026-11-26)
Context: User asked for 整应用重写 and to follow the careful plan (pure logic first, host JVM, UI stays).
Decision: **End state** is all portable units in Rust. **Path**: extract `public static` Rules packages → bridge → record → port → gate → shadow into callers → later Android ABI. Activity/View/UI hosts, VLC, jlibtorrent, and `base.apk` stay native/Java until a later ADR replaces them.
Consequences: Analyzer `keep` will be applied manually to UI/runtime hosts. First waves are Rules-only packages under `com.guocheng.mediapocket.rules`.

## ADR-003: First unit — spider component diagnostics
Status: accepted (2026-11-26)
Context: Latest main is 0.1.189 (component failure reason codes). That logic lives in package-private `TvSpiderRules`.
Decision: First bridged unit is `cinema-ui/src/com/guocheng/mediapocket/rules/` (`SpiderComponentRules`). `TvSpiderRules` becomes a thin same-package facade so existing callers and JVM tests keep working. Throwable/stream-only helpers (`failureLabel(Throwable)`, `readComponent(InputStream)`, `componentReport`, `logLine`) stay on the Java facade until a later unit models them.
Consequences: Bridge proves className/md5/digest/verify/payloadFault/readAllCapped. Facade methods that wrap Throwable remain Java until wave 2.

## ADR-004: Equivalence bar
Status: accepted (2026-11-26)
Context: Reason codes and sanitization must not leak URLs/tokens; labels are user-visible.
Decision: Byte-for-byte / exact string match on recorded cases. No float tolerance. Exception type and message string from `Err("TypeName: message")` must match Java. Do not weaken checks or auto-retry.
Consequences: Known intentional diffs require a new ADR and `known-diffs.jsonl`.

## ADR-005: Verification environment
Status: accepted (2026-11-26)
Context: Cloud agent has JDK 21 and rust-anything; no Android NDK/adb in doctor.
Decision: Host JVM `record`/`diff`/`gate` for every unit before Android. Android profile is a later checkpoint when NDK + device exist.
Consequences: Shipping JNI `.so` into the APK waits on Android profile green.

## ADR-006: Versioning
Status: accepted (2026-11-26)
Context: Extracting a new package changes the APK class set.
Decision: This wave ships as **0.1.190 / versionCode 191**. User-visible spider behavior matches 0.1.189; changelog states rust-anything migration start + Rules extraction.
Consequences: Update Manifest, README, PROJECT-GUIDE, build apk name, VALIDATION notes.

## ADR-007: Out of scope for lift
Status: accepted (2026-11-26)
Context: base.apk embeds VLC 3.0.22 and jlibtorrent; UI is programmatic Android Views.
Decision: Do not port `base.apk` natives, Media3, or Activity UI in this migration. Dex patch tools under `cinema-ui/tools/` stay Java. Stubs stay stubs.
Consequences: Analyzer units for stubs/tools are skipped (`keep` / not claimed).

## ADR-008: JNI wire for rules-rs (0.1.191)
Status: accepted (2026-11-26)
Context: First unit gated; APK packs new `lib/*/librules_rs.so` via `runtime-native` without overwriting base.apk libs.
Decision: Ship the rust-anything JNI shadow as `cinema-ui` `SpiderComponentRules`; keep pure Java in `portables/spider-component` for `record`. `readComponent(InputStream)` stays Java. `CheckFailed(String)` accepts `size|<bytes>` so JNI remapping preserves byte counts while `getMessage()` stays `size`. JVM tests set `-Drust-anything.lib.rules_rs` to the host `.so`.
Consequences: Rebuild host + Android `.so` after port changes; copy Android libs into `runtime-native` before `build_ui.py`.

## ADR-009: Config-text unit without second JNI yet (0.1.192)
Status: accepted (2026-11-26)
Context: `TvBoxConfigText` logic extracted to `ConfigTextRules` + `ConfigTextFailed` under `portables/config-text`. Rust port in `rust/config-text-rs` gates 1515/1515. rust-anything generates a second `rustanything.*` package that would clash with rules-rs JNI classes if both shadows land in the same APK dex.
Decision: Ship the Java Rules + facade in 0.1.192; keep the Rust port gated and recorded; defer `libconfig_text_rs.so` until natives are namespaced or merged. `ConfigTextFailed` is a `RuntimeException` so a future JNI remap can throw it; the facade still converts to cause-less `IOException` for callers.
Consequences: APK behavior matches pre-extract for config normalize; product code path is portable and ready for JNI once the clash is solved.
