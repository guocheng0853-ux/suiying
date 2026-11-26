# config-text-rs

Rust port of `com.guocheng.mediapocket.config.ConfigTextRules` (portable root: `portables/config-text`). Bridged with a JSON dispatcher by rust-anything.

**Status (0.1.192):** `gate` passed — **1515/1515** recorded cases. APK still runs the pure Java Rules via `TvBoxConfigText` facade; JNI is deferred (ADR-009: `rustanything` package clash with `rules-rs`).

## Functions

- `normalizeBody` — comment strip + one Base64 layer + structure check (no `JSONObject`)
- `bom` / `preparedJson` / `base64` / `decode` / `strip` / `structure`

## Notes for the port

- Java `String` indices are UTF-16; the port walks `encode_utf16` units.
- `bom` only trims when a BOM was stripped (matches Java).
- Base64 decode uses trailing-bit-lenient settings to match `Base64.getDecoder()`.
- Failures surface as `ConfigTextFailed: …` strings for the bridge.

## Loop

```sh
rust-anything record . --inputs extra-inputs.jsonl
rust-anything build .
rust-anything diff .
rust-anything gate .
```
