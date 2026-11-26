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
| `cinema-ui/src/com/guocheng/mediapocket/rules/` (via `portables/spider-component`) | done — gate 483/483 | `rust/rules-rs`; APK still on Java until JNI wire |
| Other `*Rules` (to be extracted under `rules/`) | open | Later waves |
| `cinema-ui/src/com/guocheng/mediapocket/` hosts | keep | UI / Android / network |
| `cinema-ui/runtime-src/**` | open | TVBox runtime; after Rules |
| stubs / tools / scripts | keep | Not product runtime |

## Open questions
- When to install NDK and run `--profile android`
- Whether QuickJS spider host ever moves, or only config/verify stays Rust

## Next
1. Land `rules` package + facade
2. `bridge` → `record` → port → `diff`/`gate`
3. Checkpoint with user before next Rules extraction
