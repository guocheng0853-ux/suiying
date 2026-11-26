# Handoff — 随影 rust-anything

## Stand
- Goal: whole-app portable logic → Rust (ADR-001…008 in `DECISIONS.md`)
- Wave 0 unit `rules` / `rust/rules-rs`: **gate passed** (483/483)
- **0.1.191**: `librules_rs.so` in APK (3 ABIs); JNI shadow in `cinema-ui`; pure Java in `portables/`
- JVM 117/117 with host `.so`; no device soak yet

## Next
1. Extract next `*Rules` (e.g. `TvBoxConfigText` / `FullscreenRules`) under portable roots
2. Device smoke: install 0.1.191 unsigned/signed and open a 影视仓 site that loads a jar
3. Keep UI / runtime / base.apk as `keep`

## Read
`MAP.md`, `DECISIONS.md`, `GLOSSARY.md`, `rust/rules-rs/README.md`
