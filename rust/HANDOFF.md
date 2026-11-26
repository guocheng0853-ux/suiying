# Handoff — 随影 rust-anything

## Stand
- Goal: whole-app portable logic → Rust (ADR-001…009 in `DECISIONS.md`)
- Wave 0 unit `rules` / `rust/rules-rs`: **gate passed** (483/483) + JNI in APK
- Wave 1 unit `config-text` / `rust/config-text-rs`: **gate passed** (1515/1515); APK still pure Java Rules (JNI deferred — `rustanything` package clash with rules-rs)
- **0.1.192**: `TvBoxConfigText` → facade; `ConfigTextRules` + `ConfigTextFailed` in `portables/config-text` and `cinema-ui`
- JVM tests load host `librules_rs.so`; no device soak yet

## Next
1. Unify / namespace JNI natives so a second `.so` (config-text) can ship alongside rules-rs
2. Device smoke: install unsigned/signed and open a 影视仓 site that loads a jar + a config with comments/Base64
3. Extract next `*Rules` (e.g. Fullscreen / reconnect) under portable roots
4. Keep UI / runtime / base.apk as `keep`

## Read
`MAP.md`, `DECISIONS.md`, `GLOSSARY.md`, `rust/rules-rs/README.md`, `rust/config-text-rs/README.md`
