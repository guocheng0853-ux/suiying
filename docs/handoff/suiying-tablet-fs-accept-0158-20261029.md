# suiying 0.1.158 tablet fullscreen accept — OPD2401

**When:** 2026-10-29 Asia/Shanghai (~01:31–01:50)  
**Device:** OPD2401 `cf1de64e` — 2120×3000 @420dpi (sw≈808dp), Android 16  
**Package:** `com.guocheng.mediapocket.next` versionName **0.1.158** / versionCode **159** (pre-installed; no reinstall)  
**Title under test:** Emby 继续观看 → 火柴盒大电影 (≈2.39:1 / cinematic 16:9-class) via `GesturePlayerActivity` / `PlayerActivity`  
**Artifacts:** `/workspace/out/suiying-0158-verify/`

## Checklist

| # | Item | Result | Evidence |
|---|---|---|---|
| 1 | Full pad window; bars hide; back clears cutout; **no** centered phone/landscape hole | **PASS** (note*) | dumpsys: `frame=[0,0][2120,3000]`, `requestedOrientation=UNSPECIFIED`, `areBoundsLetterboxed=false`, `vsysui=…IMMERSIVE_STICKY`, `layoutInDisplayCutoutMode=always`. Pixels: video band full width (L/R margin 0), letterboxed by aspect only. Screencaps: `03-player-portrait.png`, `04-player-immersive.png` |
| 2 | Rotate / auto-rotate: immersive fill follows window | **PASS** | Landscape `frame=[0,0][3000,2120]`, still UNSPECIFIED, not letterboxed; full-width video. `05-landscape.png` |
| 3 | 更多: scrim covers pad; card centered | **PASS** | UI dump title `更多` bounds `[1064,265]–[1935,…]` on 3000-wide; scrim darkens margins. `11-more-menu.png` |
| 4 | 9:16 letterbox in full window, no portrait lock | **SKIP** | No convenient 9:16 title in continue/history this session |
| 5 | Tap 旋转 once — does compat hole return and stay? (Medium #1) | **FAIL-as-hole: NO; Medium still real as orientation lock** | See Medium #1 below. `16-rotate-landscape-lock.png`, dumpsys after tap |
| 6 | Subtitle search / Emby field + keyboard cover? (Medium #2) | **SKIP** | Could not stably open subtitle-search EditText / Emby form under player chrome timing |
| 7 | Split + 更多 fills pane only | **SKIP** | Not practical this run |
| 8 | PiP enter/leave | **SKIP** | Not practical this run |

\*ColorOS still paints status glyphs (time/speed/battery) on the black letterbox even with `IMMERSIVE_STICKY`; nav/taskbar hidden. Not the old centered compat hole.

## Mediums

### Medium #1 — 旋转 / fixed orientation
**Reproduced as orientation lock; classic centered hole NOT seen on OPD2401 ColorOS.**

- From landscape 更多 → 旋转: `requestedOrientation` became `PORTRAIT`, display rotated to `ROTATION_0`, window stayed full `2120×3000`, `areBoundsLetterboxed=false`.
- With `accelerometer_rotation=0` + `user_rotation=0`, 旋转 again → `LANDSCAPE`, display went to `ROTATION_90`, window full `3000×2120`, still `areBoundsLetterboxed=false`, mid-row non-black span 0–2998 (no side pillars). Lock **stays** (`LANDSCAPE` still set +3s later).
- So on this OEM, 旋转 forces a real display rotation rather than a letterboxed phone hole. It still **defeats** the tablet `UNSPECIFIED` policy for the rest of the session (review intent). Fix (ignore 旋转 / map to unspecified on tablet) remains warranted; hole symptom may be OEM-specific.

### Medium #2 — IME / dialog decor
**Not exercised.** Skip.

## Phone smoke (PJZ110 `11a279bc`)
versionName 0.1.158 / versionCode 159 installed; wm 1440×3168 @640dpi. No playback path exercised this run.

## Overall tablet accept
**Conditional YES** for the PR’s primary goal (remove OPD2401 centered compat hole; immersive full-window playback in portrait and landscape).

- Core items 1–3: **PASS**
- Medium #1: orientation lock confirmed; visual hole **not** reproduced here — track as known follow-up, not a tablet-fullscreen regress
- Medium #2 / 9:16 / split / PiP: untested

No code changes. No reinstall.
