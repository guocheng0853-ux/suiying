package com.guocheng.mediapocket;

/** JNI shadow of Media3 control Rules (rust-anything {@code fullscreen-rs}). */
final class Media3ControlRules {
  private Media3ControlRules() {}

  static int[] box(int parentW, int parentH, int videoW, int videoH, String mode) {
    return rustanything.FullscreenRsNative.call_boxDefault(parentW, parentH, videoW, videoH, mode);
  }

  static int[] box(int parentW, int parentH, int videoW, int videoH, String mode, int smallestWidthDp) {
    return rustanything.FullscreenRsNative.call_box(
        parentW, parentH, videoW, videoH, mode, smallestWidthDp);
  }

  static float[] place(
      int parentW, int parentH, int videoW, int videoH, String mode, int smallestWidthDp) {
    return (float[]) rustanything.FullscreenRsNative.invoke(
        "place",
        new rustanything.RustAnythingJson.TypeRef<float[]>() {}.type(),
        parentW,
        parentH,
        videoW,
        videoH,
        mode,
        smallestWidthDp);
  }

  static int seek(int current, int duration, int delta) {
    return rustanything.FullscreenRsNative.call_seek(current, duration, delta);
  }
}
