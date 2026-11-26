package com.guocheng.mediapocket;

/** JNI shadow of fullscreen aspect Rules (rust-anything {@code fullscreen-rs}). Pure Java in {@code portables/fullscreen}. */
final class FullscreenRules {
  static final String[] MODES = {"smart", "fit", "crop"};
  static final String[] LABELS = {
    "智能满屏 · 自动铺满屏幕", "完整画面 · 不裁切，保留黑边", "填满裁切 · 保持比例，裁掉多出的部分"
  };
  static final float MIN = 0.5f, MAX = 3f;
  static final float SMART_CROP = 0.12f, SMART_STRETCH = 0.10f;
  static final float MIN_SPAN = 40f;
  static final String[] ICONS = {"满", "▣", "⛶"};

  private FullscreenRules() {}

  static String mode(String saved, boolean migrated) {
    return rustanything.FullscreenRsNative.call_mode(saved, migrated);
  }

  static boolean tabletStretch(String mode, int smallestWidthDp) {
    return rustanything.FullscreenRsNative.call_tabletStretch(mode, smallestWidthDp);
  }

  static float[] stretchFill(int screenW, int screenH, int videoW, int videoH) {
    return (float[]) rustanything.FullscreenRsNative.invoke(
        "stretchFill", new rustanything.RustAnythingJson.TypeRef<float[]>() {}.type(), screenW, screenH, videoW, videoH);
  }

  static float[] smart(int screenW, int screenH, int videoW, int videoH) {
    return smart(screenW, screenH, videoW, videoH, 0);
  }

  static float[] smart(int screenW, int screenH, int videoW, int videoH, int smallestWidthDp) {
    return (float[]) rustanything.FullscreenRsNative.invoke(
        "smart",
        new rustanything.RustAnythingJson.TypeRef<float[]>() {}.type(),
        screenW,
        screenH,
        videoW,
        videoH,
        smallestWidthDp);
  }

  static float zoom(String mode, float saved) {
    return rustanything.FullscreenRsNative.call_zoom(mode, saved);
  }

  static String layoutMode(String mode) {
    return rustanything.FullscreenRsNative.call_layoutMode(mode);
  }

  static String next(String mode) {
    return rustanything.FullscreenRsNative.call_next(mode);
  }

  static String quickNext(String mode) {
    return rustanything.FullscreenRsNative.call_quickNext(mode);
  }

  static String cycleHud(String label) {
    return (String)
        rustanything.FullscreenRsNative.invoke(
            "cycleHud", new rustanything.RustAnythingJson.TypeRef<String>() {}.type(), label);
  }

  static float[] look(String mode, int screenW, int screenH, int videoW, int videoH) {
    return look(mode, screenW, screenH, videoW, videoH, 0);
  }

  static float[] look(String mode, int screenW, int screenH, int videoW, int videoH, int smallestWidthDp) {
    return (float[]) rustanything.FullscreenRsNative.invoke(
        "look",
        new rustanything.RustAnythingJson.TypeRef<float[]>() {}.type(),
        mode,
        screenW,
        screenH,
        videoW,
        videoH,
        smallestWidthDp);
  }

  static int displayWidth(int width, int sarNum, int sarDen) {
    return rustanything.FullscreenRsNative.call_displayWidth(width, sarNum, sarDen);
  }

  static boolean sameLook(float[] a, float[] b) {
    return (Boolean)
        rustanything.FullscreenRsNative.invoke(
            "sameLook", new rustanything.RustAnythingJson.TypeRef<Boolean>() {}.type(), a, b);
  }

  static String visibleNext(String mode, int screenW, int screenH, int videoW, int videoH) {
    return rustanything.FullscreenRsNative.call_visibleNextDefault(mode, screenW, screenH, videoW, videoH);
  }

  static String visibleNext(
      String mode, int screenW, int screenH, int videoW, int videoH, int smallestWidthDp) {
    return rustanything.FullscreenRsNative.call_visibleNext(
        mode, screenW, screenH, videoW, videoH, smallestWidthDp);
  }

  static int index(String mode) {
    return rustanything.FullscreenRsNative.call_index(mode);
  }

  static float clamp(float v) {
    return rustanything.FullscreenRsNative.call_clamp(v);
  }

  static float originalScale(int fitWidth, int videoWidth) {
    return rustanything.FullscreenRsNative.call_originalScale(fitWidth, videoWidth);
  }

  static float[] axisScale(float spanX, float prevX, float spanY, float prevY) {
    return (float[]) rustanything.FullscreenRsNative.invoke(
        "axisScale",
        new rustanything.RustAnythingJson.TypeRef<float[]>() {}.type(),
        spanX,
        prevX,
        spanY,
        prevY);
  }

  static boolean custom(float x, float y) {
    return rustanything.FullscreenRsNative.call_custom(x, y);
  }

  static String zoomLabel(float x, float y) {
    return rustanything.FullscreenRsNative.call_zoomLabel(x, y);
  }

  static float pipRatio(int w, int h) {
    return rustanything.FullscreenRsNative.call_pipRatio(w, h);
  }

  static boolean surfaceDirty(
      float pivotX,
      float pivotY,
      float tx,
      float ty,
      float sx,
      float sy,
      float wantPx,
      float wantPy,
      float wantSx,
      float wantSy) {
    return rustanything.FullscreenRsNative.call_surfaceDirty(
        pivotX, pivotY, tx, ty, sx, sy, wantPx, wantPy, wantSx, wantSy);
  }
}
