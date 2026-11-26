package com.guocheng.mediapocket.config;

/**
 * JNI shadow of {@code ConfigTextRules} (rust-anything {@code config-text-rs}).
 * Pure Java source of truth lives in {@code portables/config-text}.
 * Only {@link rustanything.ConfigTextRsNative} is added; shared {@code rustanything.*}
 * wire/error classes stay with rules-rs (ADR-009 clash avoided).
 */
public final class ConfigTextRules {
    private ConfigTextRules() {}

    public static final int MAX_CHARS = 8 * 1024 * 1024;
    public static final int MAX_DEPTH = 32;

    /** The Rust port of `com.guocheng.mediapocket.config.ConfigTextRules.normalizeBody(String raw)`. */
    @SuppressWarnings("unchecked")
    public static String normalizeBody(String raw) {
        return (String) rustanything.ConfigTextRsNative.invoke("normalizeBody", new rustanything.RustAnythingJson.TypeRef<String>() {}.type(), raw);
    }

    /** The Rust port of `com.guocheng.mediapocket.config.ConfigTextRules.bom(String text)`. */
    @SuppressWarnings("unchecked")
    public static String bom(String text) {
        return rustanything.ConfigTextRsNative.call_bom(text);
    }

    /** The Rust port of `com.guocheng.mediapocket.config.ConfigTextRules.preparedJson(String text)`. */
    @SuppressWarnings("unchecked")
    public static String preparedJson(String text) {
        return rustanything.ConfigTextRsNative.call_preparedJson(text);
    }

    /** The Rust port of `com.guocheng.mediapocket.config.ConfigTextRules.base64(String text)`. */
    @SuppressWarnings("unchecked")
    public static boolean base64(String text) {
        return rustanything.ConfigTextRsNative.call_base64(text);
    }

    /** The Rust port of `com.guocheng.mediapocket.config.ConfigTextRules.decode(String text)`. */
    @SuppressWarnings("unchecked")
    public static String decode(String text) {
        return rustanything.ConfigTextRsNative.call_decode(text);
    }

    /** The Rust port of `com.guocheng.mediapocket.config.ConfigTextRules.strip(String text)`. */
    @SuppressWarnings("unchecked")
    public static String strip(String text) {
        return rustanything.ConfigTextRsNative.call_strip(text);
    }

    /** The Rust port of `com.guocheng.mediapocket.config.ConfigTextRules.structure(String text)`. */
    @SuppressWarnings("unchecked")
    public static void structure(String text) {
        rustanything.ConfigTextRsNative.call_structure(text);
    }
}
