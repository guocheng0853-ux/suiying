# MAP — 随影 → Rust

Goal: portable MediaPocket logic in Rust, proven by rust-anything, loaded through JNI into the existing APK.

## ADRs
See `DECISIONS.md` (ADR-001 … ADR-007).

## Out of scope
- `cinema-ui/base.apk` natives (VLC, jlibtorrent, QuickJS host)
- Android Activity / View UI hosts
- `cinema-ui/tools/*` dex patch CLIs
- Compile-only stubs

## Units

| Unit | State | Notes |
| --- | --- | --- |
| `cinema-ui/.../rules/` (via `portables/spider-component`) | done — gate 483/483 + JNI in APK (0.1.191) | pure Java in `portables/`; shadow + `.so` in cinema-ui |
| `cinema-ui/.../config/` (via `portables/config-text`) | done — gate 1515/1515 + JNI wiring (ADR-010) | shadow + `ConfigTextRsNative` + `libconfig_text_rs.so` in cinema-ui |
| fullscreen cluster (`FullscreenRules` / `VideoFullscreenRules` / `Media3ControlRules`) | diff 1644/1644; JNI pending | `portables/fullscreen` + `rust/fullscreen-rs` |
| Other `*Rules` (to be extracted under portable roots) | open | Later waves |
| `cinema-ui/src/com/guocheng/mediapocket/` hosts | keep | UI / Android / network |
| `cinema-ui/runtime-src/**` | open | TVBox runtime; after Rules |
| stubs / tools / scripts | keep | Not product runtime |

## Open questions
- Whether QuickJS spider host ever moves, or only config/verify stays Rust

## Next
1. Device smoke after packaging with both `.so`s (0.1.193)
2. Wire `fullscreen-rs` JNI (FullscreenRsNative only) + facades; bump; install
3. Extract next Rules unit
