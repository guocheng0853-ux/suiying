# PR 34 review — tablet video fullscreen (0.1.158 / versionCode 159)

> Later: PR #34 was squash-merged as main `30b196b` (0.1.158 / code 159). The text below is the pre-merge review.

**PR:** https://github.com/guocheng0853-ux/suiying/pull/34  
**Head:** `d1cc65473e80305ba71308cb084e99b5edf0271f` (`cursor/tablet-video-fullscreen-536e`)  
**Base:** `e40097a92d35ddb37dc26bc60665df5aa988f444` (main, 0.1.157 / code 158)  
**Scope:** review only. Not merged. No signed APK. No commits pushed.

**Merge recommendation: conditional yes.**

No High crash, ANR, or security findings. The automatic phone path is unchanged, and the automatic tablet path matches the intent (do not force sensor landscape; immersive chrome uses the cutout). Two Mediums are real and should be checked on OPD2401 before this is treated as accepted. Lows do not block.

## What the diff actually does

16 files, +174 / −34. One commit.

The behavior change that removes the OPD2401 compat hole is `PlaybackOrientation`: a window whose `smallestScreenWidthDp` is at least 600 stays `SCREEN_ORIENTATION_UNSPECIFIED` (`-1`), including enter, picture-in-picture resume, and split. Phones (`sw < 600`) still get sensor landscape on enter and the video orientation from `fitVideo`. Split windows stay unspecified for both.

`VideoFullscreen` then, on a tablet only:

- hides system bars and draws into the cutout (`hideBars` + `FullscreenMode.immersive`)
- asks fullscreen menus to use the maximum window (or the split window)
- pads Media3 chrome to the cutout

VLC chrome padding goes through `PlayerOverlay.chromeInsets`, which returns the old system-bars-plus-cutout insets for anything under 600dp.

Base `PlayerActivity` already adds `frame` as match-parent and already calls `edgeToEdge()` on window focus (API 30+ `setDecorFitsSystemWindows(false)` and hide system bars). `VlcPlayback.layoutVideo` sizes the surface inside the current view. Once the activity window is the real pad window, the existing geometry letterboxes 16:9 / 9:16 inside that window. That is the right outcome. `VideoFullscreen.stage()` is not called from production; it only documents the letterbox math for tests.

## Findings

### High

None.

### Medium

1. **旋转 still re-enters the compat hole, and the new tick will not undo it.**  
   `PlaybackOrientation.fitVideo` returns immediately on a tablet, before it can store video size or request an orientation (`PlaybackOrientation.java` around the `tablet(smallest(a))` return). `windowChanged` only rewrites orientation when multi-window flips.  
   The visible control is unchanged: base `PlayerActivity.rotate()` calls `setRequestedOrientation(PORTRAIT or LANDSCAPE)` (fixed, not sensor). Media3 「更多 → 旋转屏幕」 does the same with sensor portrait / sensor landscape (`Media3Controls.more`).  
   On OPD2401 that fixed request is the failure mode this PR exists to stop. After one tap, playback can sit in the centered hole again for the rest of the session.  
   **Fix:** on a tablet, ignore 旋转 or map it to unspecified. If 旋转 stays, `fitVideo` / the 250ms poll should put the pad back to unspecified.

2. **Tablet dialogs turn off IME resize for every CinemaMenu, not only the player.**  
   `CinemaMenu.open` always calls `VideoFullscreen.dialog` (`CinemaMenu.java` before show and again after show). On a tablet that sets `setDecorFitsSystemWindows(false)` (`VideoFullscreen.dialog`). The same method just set `SOFT_INPUT_ADJUST_RESIZE` when the body contains an `EditText`. On API 30+ (OPD2401 is Android 16) decor-not-fitting wins, and nothing applies IME insets to the panel.  
   Affected forms include player subtitle search (`OnlineSubtitleSearch`), backup password, Emby server edit, and Fusion search. Phone menus return before any of this.  
   The window is also forced to maximum display bounds with gravity `TOP|START`. A centered compat hole would not be covered: the dialog origin is the activity window’s top-left, with no negative offset. The orientation fix should keep the player out of that hole; home and settings menus still take the new size and hide bars while open.  
   **Fix:** limit `dialog()` to the two player activities, and do not call `setDecorFitsSystemWindows(false)` when `hasInput(body)` is true (or pad the panel by the IME inset).

### Low

1. **`VideoFullscreen.fill` does not resize the VLC picture.**  
   `Activity.video` is the `Playback` interface, not a `View`, so `view(a, "video")` is null. `frame` is already match-parent with zero margins, so `match()` is a no-op and `layoutVideo()` is not called from `fill()`. Media3’s `Media3VideoView` is already match-parent; `fit()` letterboxes the surface. Harmless, but the comment overstates what `fill` does. `VideoFullscreenRules.stage()` is unused in production.

2. **Volume HUD and the failure card still use full system-bar insets.**  
   `LevelHud.place` and `PlaybackFailureCard.position` call `PlayerOverlay.insets` (system bars + cutout). Player chrome uses `chromeInsets` (cutout only on a tablet). With bars hidden, the system-bar inset is often 0, so the HUD can sit in the punch-hole while the back button does not.

3. **Bar re-hide is keyed by a packed int.**  
   `VideoFullscreen.apply` only calls `hideBars` when `orientation * 100000 + widthPixels * 10000 + heightPixels` changes. VLC `onWindowFocusChanged` still calls `edgeToEdge()` every focus gain, so the player window is fine. A Media3 window that returns from the notification shade at the same size relies on the legacy `setSystemUiVisibility` shim in `onWindowFocusChanged`. Worth a glance, not a logic error for OPD2401’s 2120×3000.

4. **Split-window dialog size falls back to the full display if the activity decor has not been measured** (`bounds()` substitutes screen size when decor width is 0). Menus opened during playback should already have a measured decor.

## Phone path

Unchanged in code:

- `VideoFullscreen.apply`, `dialog`, and `padMedia3` return when `smallestScreenWidthDp < 600`.
- `PlayerOverlay.chromeInsets` returns the existing `systemBars | displayCutout` insets for a phone, including portrait.
- `PlaybackOrientation.enter` still requests sensor landscape. `fitVideo` still requests sensor portrait for a clearly tall frame (`h > w * 1.05`) and sensor landscape otherwise.
- Phone dialogs stay `setLayout(-1, -1)`.

The 600dp cutoff matches `PaperLayoutRules.TABLET_SW`. PJZ110-class width 411 stays a phone. `smallest()` on failure returns 0, which is treated as a phone (locks orientation). That is the safe direction if resources throw; it would reintroduce the hole only if a real pad reported 0.

## Split window

`windowRequest` returns unspecified when `multi` is true, for phone and tablet. `fitVideo` does not lock orientation while `isInMultiWindowMode()` is true. Exiting split on a phone restores the video orientation; exiting split on a tablet stays unspecified. `dialogWindow` uses the activity window size in multi-window, and the full display otherwise. Picture-in-picture resume on a tablet also stays unspecified, so leaving PiP does not re-lock landscape.

## Version and dex gate

Consistent. No patch-chain edit.

| Place | Value |
| --- | --- |
| `AndroidManifest.xml` | versionName 0.1.158, versionCode 159 |
| `build_ui.py`, `build/build_win.py` | `MediaPocket-0.1.158-tablet-fullscreen-arm64.apk`, stub `ui_version` 0.1.158 |
| `verify_apk.py` | badging assert code 159 / name 0.1.158, `main_version_code` 159, class list adds `VideoFullscreen` and `VideoFullscreenRules` |
| `README.md` | new 0.1.158 section; 0.1.157 section kept as history |
| `VALIDATION.json` | `ui_version` 0.1.158, `main_version_code` 159, `jvm_test_programs_passed` 97 |

`signed_apk_sha256` is still the 0.1.155 package, and `validation_flags_note` says so. `dolby_android_device_tested` stays false. `verify_apk.py` still asserts all 7 `PATCH_MARKS` (ClawNet, SubtitleSearch, HeaderRoute, TorrentBoost, PieceReads, PlaybackOrientation, PlaybackBackend). Both build scripts still compile that same list. Sources are picked up by `rglob('*.java')`, so the new classes are in the dex if the APK is built. `verify_apk.py` will fail until that 0.1.158 APK exists. That matches “no signed APK in this change.”

`source_rank_rule` still describes the 0.1.157 Dolby ranking rule. That is the rule text, not the version stamp.

## Tests

Added `VideoFullscreenRulesTest` (96 → 97 programs). `PlaybackOrientationRulesTest` gained two checks: tablet 807dp does not force landscape, phone 411dp still does, and a tablet split stays unspecified.

Ran both from `cinema-ui/` against `d1cc654` (Android types stubbed only so `PlaybackOrientation` would compile; the assertions do not need a device):

- `VideoFullscreenRulesTest` PASS
- `PlaybackOrientationRulesTest` PASS

They cover the pure rules (cutoff, stage math, chrome edge, dialog size, orientation) plus string-presence checks that the player, system player, and menu call the new helpers. They do not execute `VideoFullscreen.apply`, window insets, or `setRequestedOrientation`. The source reads use paths relative to `cinema-ui/`, same as `OfflineHttpTest`, `PaperPolishTest`, and `PlayerMotionRulesTest`. `scripts/run_tests.sh` already `cd`s there.

The other 95 programs were not re-run here: this checkout has no `android.jar` and no `cinema-ui/build/classes`. Main’s `VALIDATION.json` records 96/96. Nothing in the current tree documents a red JVM program on main. The historical CWD failures of those three source-reading tests are a runner-directory issue, not a product failure, when `run_tests.sh` is used.

## How to verify

No device was available for this review. The PR states the same.

**OPD2401** (about 2120×3000, density 420, shortest side ~807dp). Cover-install 0.1.158 over the current same-signature build. Auto-rotate off, pad held in portrait, play a normal 16:9 title.

- Picture uses the full pad window. System bars hide. Back control clears the camera hole. No centered landscape or phone-sized hole.
- Turn the pad (or enable auto-rotate). The same immersive fill follows the window.
- Open 更多. Scrim covers the pad. The card stays centered.
- A 9:16 clip stays inside the full window, letterboxed by aspect, and does not lock portrait.
- Then tap 旋转 once. This is the Medium: confirm whether the compat hole comes back and stays.
- Open subtitle search and an Emby/server field. Confirm the keyboard does not cover the field.
- Split the pad and open 更多. The dialog should fill that pane, not the other app.
- Enter and leave picture-in-picture. Playback should not lock sensor landscape afterward.

**PJZ110** (shortest side under 600dp).

- A normal title still requests sensor landscape and plays edge to edge.
- A 9:16 clip still requests sensor portrait. Portrait letterbox and the status-bar band match 0.1.157.
- Player menus stay the existing card, not a pad-sized window.
- Split-screen still leaves orientation to the system.

## Files that matter

- `cinema-ui/src/com/guocheng/mediapocket/VideoFullscreenRules.java` — cutoff, orientation, chrome edge, dialog size
- `cinema-ui/src/com/guocheng/mediapocket/VideoFullscreen.java` — apply / hide / dialog / Media3 padding
- `cinema-ui/src/com/guocheng/mediapocket/PlaybackOrientation.java` — tablet and split stay unspecified
- `cinema-ui/src/com/guocheng/mediapocket/PlayerOverlay.java` — `chromeInsets` / `cutoutInsets`
- `cinema-ui/src/com/guocheng/mediapocket/CinemaMenu.java` — dialog hook (all menus)
- `cinema-ui/src/com/guocheng/mediapocket/PlayerChrome.java`, `SystemPlayerActivity.java`, `Media3Controls.java` — call sites
- `cinema-ui/tests/VideoFullscreenRulesTest.java`, `PlaybackOrientationRulesTest.java`
- Version: `AndroidManifest.xml`, `build_ui.py`, `build/build_win.py`, `verify_apk.py`, `README.md`, `VALIDATION.json`
