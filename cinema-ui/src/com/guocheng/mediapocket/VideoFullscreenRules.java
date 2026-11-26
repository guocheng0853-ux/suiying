package com.guocheng.mediapocket;

/** JNI shadow of video fullscreen Rules (rust-anything {@code fullscreen-rs}). */
final class VideoFullscreenRules {
  static final int TABLET_SW = 600;

  private VideoFullscreenRules() {}

  static boolean tablet(int smallestWidthDp) {
    return rustanything.FullscreenRsNative.call_tablet(smallestWidthDp);
  }

  static boolean immersive(int smallestWidthDp, boolean landscape) {
    return rustanything.FullscreenRsNative.call_immersive(smallestWidthDp, landscape);
  }

  static boolean reserveStatusBar(int smallestWidthDp, boolean portrait) {
    return rustanything.FullscreenRsNative.call_reserveStatusBar(smallestWidthDp, portrait);
  }

  static int[] stage(
      int windowW,
      int windowH,
      int statusTop,
      int navBottom,
      int smallestWidthDp,
      boolean landscape,
      int videoW,
      int videoH) {
    return rustanything.FullscreenRsNative.call_stage(
        windowW, windowH, statusTop, navBottom, smallestWidthDp, landscape, videoW, videoH);
  }

  static int chromeEdge(int barAndCutout, int cutoutOnly, int smallestWidthDp, boolean portrait) {
    return rustanything.FullscreenRsNative.call_chromeEdge(
        barAndCutout, cutoutOnly, smallestWidthDp, portrait);
  }

  static int[] dialogWindow(
      int screenW, int screenH, int windowW, int windowH, boolean multi, int smallestWidthDp) {
    return rustanything.FullscreenRsNative.call_dialogWindow(
        screenW, screenH, windowW, windowH, multi, smallestWidthDp);
  }

  static int orientation(boolean multi, int smallestWidthDp, int desired) {
    return rustanything.FullscreenRsNative.call_orientation(multi, smallestWidthDp, desired);
  }

  static int clearOf(int basePx, int cutoutPx) {
    return rustanything.FullscreenRsNative.call_clearOf(basePx, cutoutPx);
  }
}
