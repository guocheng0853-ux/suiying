# fullscreen-rs

Rust port of `portables/fullscreen/com/guocheng/mediapocket/fullscreen/` (`FullscreenRules` / `VideoFullscreenRules` / `Media3ControlRules`), bridged with a JSON dispatcher.

**Status:** `diff` **1644/1644**. `sameLook` short-circuits on NaN/Infinity (Java `&&`); `stage` uses Java 32-bit int wrap for `statusTop+navBottom`. APK facades / JNI still pending.

## Functions

- `mode` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:20`)
- `tabletStretch` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:25`)
- `stretchFill` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:29`)
- `smartDefault` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:36`)
- `smart` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:40`)
- `zoom` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:51`)
- `layoutMode` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:55`)
- `next` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:59`)
- `quickNext` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:63`)
- `cycleHud` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:67`)
- `lookDefault` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:71`)
- `look` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:75`)
- `displayWidth` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:86`)
- `sameLook` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:95`)
- `visibleNextDefault` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:99`)
- `visibleNext` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:104`)
- `index` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:116`)
- `clamp` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:121`)
- `originalScale` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:126`)
- `axisScale` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:131`)
- `custom` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:137`)
- `zoomLabel` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:141`)
- `pipRatio` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:148`)
- `surfaceDirty` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/FullscreenRules.java:153`)
- `boxDefault` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/Media3ControlRules.java:8`)
- `box` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/Media3ControlRules.java:12`)
- `place` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/Media3ControlRules.java:43`)
- `seek` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/Media3ControlRules.java:64`)
- `tablet` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/VideoFullscreenRules.java:12`)
- `immersive` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/VideoFullscreenRules.java:17`)
- `reserveStatusBar` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/VideoFullscreenRules.java:22`)
- `stage` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/VideoFullscreenRules.java:31`)
- `chromeEdge` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/VideoFullscreenRules.java:61`)
- `dialogWindow` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/VideoFullscreenRules.java:70`)
- `orientation` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/VideoFullscreenRules.java:84`)
- `clearOf` (`portables/fullscreen/com/guocheng/mediapocket/fullscreen/VideoFullscreenRules.java:90`)

## Loop

```sh
rust-anything record .   # record what the Java code does
rust-anything build .    # build the shared library
rust-anything diff .     # run the recorded cases against Rust
rust-anything shadow .   # run both in the app, log mismatches
rust-anything gate .     # fmt, clippy, tests, unsafe, API, diff
```
