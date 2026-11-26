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
| fullscreen cluster (`FullscreenRules` / `VideoFullscreenRules` / `Media3ControlRules`) | diff 1644/1644 + JNI in APK (0.1.194) | shadow + `FullscreenRsNative` + `libfullscreen_rs.so` |
| Other `*Rules` (to be extracted under portable roots) | open | Later waves |
| `cinema-ui/src/com/guocheng/mediapocket/` hosts | keep | UI / Android / network |
| `cinema-ui/runtime-src/**` | open | TVBox runtime; after Rules |
| stubs / tools / scripts | keep | Not product runtime |

## Open questions
- Whether QuickJS spider host ever moves, or only config/verify stays Rust

## Next
1. Device smoke of 0.1.194 (fullscreen + config-text + rules JNI)
2. Extract next Rules unit
3. Keep packaging via `cinema-ui/build/build_win.py` on Windows (short ASCII copy path if javac 206)
