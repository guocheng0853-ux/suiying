package com.guocheng.mediapocket.rules;

/**
 * Validation failure: {@link #getMessage()} is only the reason code (no URL/digest/body).
 * rust-anything JNI remaps via the single-arg ctor; size failures use wire {@code size|&lt;bytes&gt;}
 * so the byte count survives while {@code getMessage()} stays {@code size}.
 */
public final class CheckFailed extends IllegalArgumentException {
  public final String code;
  public final int bytes;

  /** Single-arg ctor for rust-anything JNI remapping ({@code code} or {@code size|bytes}). */
  public CheckFailed(String wire) {
    super(codeOf(wire));
    this.code = codeOf(wire);
    this.bytes = bytesOf(wire);
  }

  public CheckFailed(String code, int bytes) {
    super(code);
    this.code = code;
    this.bytes = bytes;
  }

  static String codeOf(String wire) {
    if (wire == null) return "";
    int bar = wire.indexOf('|');
    return bar < 0 ? wire : wire.substring(0, bar);
  }

  static int bytesOf(String wire) {
    if (wire == null) return -1;
    int bar = wire.indexOf('|');
    if (bar < 0) return -1;
    try {
      return Integer.parseInt(wire.substring(bar + 1));
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}
