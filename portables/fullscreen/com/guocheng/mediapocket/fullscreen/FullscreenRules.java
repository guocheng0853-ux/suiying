package com.guocheng.mediapocket.fullscreen;

/**
 * Aspect / pinch / PiP policy. Overloads renamed to unique names for rust-anything
 * ({@code smartDefault}/{@code smart}, {@code lookDefault}/{@code look}, …).
 */
public final class FullscreenRules {
  private FullscreenRules() {}

  /** 0.1.152 只留三种：智能满屏（默认）、完整画面、填满裁切。 */
  public static final String[] MODES = {"smart", "fit", "crop"};
  public static final String[] LABELS = {
    "智能满屏 · 自动铺满屏幕", "完整画面 · 不裁切，保留黑边", "填满裁切 · 保持比例，裁掉多出的部分"
  };
  public static final float MIN = 0.5f, MAX = 3f;
  public static final float SMART_CROP = 0.12f, SMART_STRETCH = 0.10f;
  public static final float MIN_SPAN = 40f;
  public static final String[] ICONS = {"满", "▣", "⛶"};

  public static String mode(String saved, boolean migrated) {
    for (String mode : MODES) if (mode.equals(saved)) return mode;
    return "smart";
  }

  public static boolean tabletStretch(String mode, int smallestWidthDp) {
    return "smart".equals(mode) && VideoFullscreenRules.tablet(smallestWidthDp);
  }

  public static float[] stretchFill(int screenW, int screenH, int videoW, int videoH) {
    int[] fit = Media3ControlRules.boxDefault(screenW, screenH, videoW, videoH, "fit");
    if (fit[0] <= 0 || fit[1] <= 0) return new float[] {1f, 1f};
    return new float[] {screenW / (float) fit[0], screenH / (float) fit[1]};
  }

  /** Former {@code smart(w,h,vw,vh)} — defaults tablet cutoff to 0 (phone path). */
  public static float[] smartDefault(int screenW, int screenH, int videoW, int videoH) {
    return smart(screenW, screenH, videoW, videoH, 0);
  }

  public static float[] smart(int screenW, int screenH, int videoW, int videoH, int smallestWidthDp) {
    if (screenW <= 0 || screenH <= 0 || videoW <= 0 || videoH <= 0) return new float[] {1f, 1f};
    if (tabletStretch("smart", smallestWidthDp)) return stretchFill(screenW, screenH, videoW, videoH);
    double s = (double) screenW / screenH, v = (double) videoW / videoH;
    if (Math.abs(s / v - 1) < 0.005) return new float[] {1f, 1f};
    boolean narrower = v < s;
    double need = narrower ? s / v : v / s, u = Math.min(need, 1 / (1 - SMART_CROP)), rest = need / u;
    if (rest > 1 + SMART_STRETCH + 1e-6) return new float[] {1f, 1f};
    return narrower ? new float[] {(float) need, (float) u} : new float[] {(float) u, (float) need};
  }

  public static float zoom(String mode, float saved) {
    return saved;
  }

  public static String layoutMode(String mode) {
    return "crop".equals(mode) ? "crop" : "fit";
  }

  public static String next(String mode) {
    return MODES[(index(mode) + 1) % MODES.length];
  }

  public static String quickNext(String mode) {
    return next(mode);
  }

  public static String cycleHud(String label) {
    return null;
  }

  public static float[] lookDefault(String mode, int screenW, int screenH, int videoW, int videoH) {
    return look(mode, screenW, screenH, videoW, videoH, 0);
  }

  public static float[] look(
      String mode, int screenW, int screenH, int videoW, int videoH, int smallestWidthDp) {
    if ("smart".equals(mode)) return smart(screenW, screenH, videoW, videoH, smallestWidthDp);
    if ("crop".equals(mode) && screenW > 0 && screenH > 0 && videoW > 0 && videoH > 0) {
      double s = (double) screenW / screenH, v = (double) videoW / videoH;
      float k = (float) Math.max(s / v, v / s);
      return new float[] {k, k};
    }
    return new float[] {1f, 1f};
  }

  public static int displayWidth(int width, int sarNum, int sarDen) {
    if (width <= 0) return 0;
    return sarNum > 0 && sarDen > 0 ? (int) ((long) width * sarNum / sarDen) : width;
  }

  /**
   * NaN/Infinity on the first axis comparison short-circuits to false (Java {@code &&}) and must
   * not evaluate {@code a[1]}/{@code b[1]}.
   */
  public static boolean sameLook(float[] a, float[] b) {
    return Math.abs(a[0] / b[0] - 1) < 0.03f && Math.abs(a[1] / b[1] - 1) < 0.03f;
  }

  public static String visibleNextDefault(
      String mode, int screenW, int screenH, int videoW, int videoH) {
    return visibleNext(mode, screenW, screenH, videoW, videoH, 0);
  }

  public static String visibleNext(
      String mode, int screenW, int screenH, int videoW, int videoH, int smallestWidthDp) {
    String cur = mode(mode, true);
    float[] now = look(cur, screenW, screenH, videoW, videoH, smallestWidthDp);
    String next = next(cur);
    for (int n = 0; n < MODES.length - 1; n++) {
      if (!sameLook(look(next, screenW, screenH, videoW, videoH, smallestWidthDp), now)) return next;
      next = next(next);
    }
    return next(cur);
  }

  public static int index(String mode) {
    for (int i = 0; i < MODES.length; i++) if (MODES[i].equals(mode)) return i;
    return 0;
  }

  public static float clamp(float v) {
    if (Float.isNaN(v) || Float.isInfinite(v)) return 1f;
    return Math.max(MIN, Math.min(MAX, v));
  }

  public static float originalScale(int fitWidth, int videoWidth) {
    if (fitWidth <= 0 || videoWidth <= 0) return 1f;
    return Math.min(1f, (float) videoWidth / fitWidth);
  }

  public static float[] axisScale(float spanX, float prevX, float spanY, float prevY) {
    float sx = prevX >= MIN_SPAN && spanX >= MIN_SPAN ? spanX / prevX : 1f,
        sy = prevY >= MIN_SPAN && spanY >= MIN_SPAN ? spanY / prevY : 1f;
    return new float[] {sx, sy};
  }

  public static boolean custom(float x, float y) {
    return Math.abs(x - 1f) > 0.005f || Math.abs(y - 1f) > 0.005f;
  }

  public static String zoomLabel(float x, float y) {
    if (!custom(x, y)) return "1.0x";
    return Math.abs(x - y) < 0.005f
        ? String.format(java.util.Locale.ROOT, "%.1fx", x)
        : "自定义";
  }

  public static float pipRatio(int w, int h) {
    if (w <= 0 || h <= 0) return 16f / 9f;
    return Math.max(1 / 2.39f + 0.001f, Math.min(2.39f - 0.001f, (float) w / h));
  }

  public static boolean surfaceDirty(
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
    return pivotX != wantPx || pivotY != wantPy || tx != 0f || ty != 0f || sx != wantSx || sy != wantSy;
  }
}
