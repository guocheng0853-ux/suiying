package com.guocheng.mediapocket.config;

/**
 * Portable config-text failure. Message matches the {@link java.io.IOException} text used by
 * {@code TvBoxConfigText}. RuntimeException so rust-anything JNI can remap it.
 */
public final class ConfigTextFailed extends RuntimeException {
  public ConfigTextFailed(String message) {
    super(message, null, false, false);
  }
}
