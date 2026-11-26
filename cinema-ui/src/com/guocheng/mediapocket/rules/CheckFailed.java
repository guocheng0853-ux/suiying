package com.guocheng.mediapocket.rules;

/** Validation failure: message is only the reason code (no URL/digest/body). */
public final class CheckFailed extends IllegalArgumentException {
  public final String code;
  public final int bytes;

  /** Single-arg ctor for rust-anything JNI remapping. */
  public CheckFailed(String code) {
    this(code, -1);
  }

  public CheckFailed(String code, int bytes) {
    super(code);
    this.code = code;
    this.bytes = bytes;
  }
}
