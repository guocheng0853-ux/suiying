package com.guocheng.mediapocket.rules;

/**
 * JNI shadow of {@code SpiderComponentRules} (rust-anything {@code rules-rs}).
 * Pure Java source of truth lives in {@code portables/spider-component}.
 * {@link #readComponent} stays on the JVM (InputStream); other exports call Rust.
 */
public final class SpiderComponentRules {
    private SpiderComponentRules() {}

    public static final int MAX_COMPONENT_BYTES = 16 * 1024 * 1024;

    /** The Rust port of `com.guocheng.mediapocket.rules.SpiderComponentRules.className(String api)` (`com/guocheng/mediapocket/rules/SpiderComponentRules.java:16`). */
    @SuppressWarnings("unchecked")
    public static String className(String api) {
        return rustanything.RulesRsNative.call_className(api);
    }

    /** The Rust port of `com.guocheng.mediapocket.rules.SpiderComponentRules.md5(String declaration)` (`com/guocheng/mediapocket/rules/SpiderComponentRules.java:23`). */
    @SuppressWarnings("unchecked")
    public static String md5(String declaration) {
        return rustanything.RulesRsNative.call_md5(declaration);
    }

    /** The Rust port of `com.guocheng.mediapocket.rules.SpiderComponentRules.digest(byte[] bytes, String algorithm)` (`com/guocheng/mediapocket/rules/SpiderComponentRules.java:32`). */
    @SuppressWarnings("unchecked")
    public static String digest(byte[] bytes, String algorithm) {
        return (String) rustanything.RulesRsNative.invoke("digest", new rustanything.RustAnythingJson.TypeRef<String>() {}.type(), bytes, algorithm);
    }

    /** The Rust port of `com.guocheng.mediapocket.rules.SpiderComponentRules.verify(byte[] bytes, String expected)` (`com/guocheng/mediapocket/rules/SpiderComponentRules.java:41`). */
    @SuppressWarnings("unchecked")
    public static void verify(byte[] bytes, String expected) {
        rustanything.RulesRsNative.invoke("verify", void.class, bytes, expected);
    }

    /** The Rust port of `com.guocheng.mediapocket.rules.SpiderComponentRules.payloadFault(String text)` (`com/guocheng/mediapocket/rules/SpiderComponentRules.java:51`). */
    @SuppressWarnings("unchecked")
    public static String payloadFault(String text) {
        return (String) rustanything.RulesRsNative.invoke("payloadFault", new rustanything.RustAnythingJson.TypeRef<String>() {}.type(), text);
    }

    /** The Rust port of `com.guocheng.mediapocket.rules.SpiderComponentRules.readAllCapped(byte[] source)` (`com/guocheng/mediapocket/rules/SpiderComponentRules.java:64`). */
    @SuppressWarnings("unchecked")
    public static byte[] readAllCapped(byte[] source) {
        return (byte[]) rustanything.RulesRsNative.invoke("readAllCapped", new rustanything.RustAnythingJson.TypeRef<byte[]>() {}.type(), source);
    }

    /** The Rust port of `com.guocheng.mediapocket.rules.SpiderComponentRules.checkLabel(String code, int bytes)` (`com/guocheng/mediapocket/rules/SpiderComponentRules.java:85`). */
    @SuppressWarnings("unchecked")
    public static String checkLabel(String code, int bytes) {
        return rustanything.RulesRsNative.call_checkLabel(code, bytes);
    }

    /** Streaming download path: still Java (InputStream). Cap matches Rust {@code readAllCapped}. */
    public static byte[] readComponent(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new java.io.IOException("加载已取消");
            long seen = (long) out.size() + n;
            if (seen > MAX_COMPONENT_BYTES)
                throw new CheckFailed("size", seen > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seen);
            out.write(b, 0, n);
        }
        return out.toByteArray();
    }
}
