# Handoff — 随影 rust-anything

## Stand
- Goal: whole-app portable logic → Rust (ADR-001…007 in `DECISIONS.md`)
- Wave 0 unit `rules` / `rust/rules-rs`: **gate passed** (483/483)
- APK still executes Java `SpiderComponentRules`; JNI shadow not wired into `build_ui.py` yet

## Next
1. Wire `librules_rs.so` into the APK (`build --profile android` when NDK exists) or host shadow for JVM tests
2. Extract next `*Rules` package under `com.guocheng.mediapocket.rules` (or sibling packages)
3. Keep UI / runtime / base.apk as `keep`

## Read
`MAP.md`, `DECISIONS.md`, `GLOSSARY.md`, `rust/rules-rs/README.md`
