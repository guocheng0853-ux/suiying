package com.guocheng.mediapocket.rules;

import java.security.MessageDigest;
import java.util.Locale;

/**
 * Pure spider-component checks (0.1.189 reason codes). Public static API for rust-anything JNI.
 * Callers in {@code com.guocheng.mediapocket} go through {@code TvSpiderRules}.
 */
public final class SpiderComponentRules {
  private SpiderComponentRules() {}

  public static final int MAX_COMPONENT_BYTES = 16 * 1024 * 1024;

  /** CSP api → spider class; rejects path tricks. */
  public static String className(String api) {
    if (api == null || !api.matches("csp_[A-Za-z_$][A-Za-z0-9_$]*"))
      throw new CheckFailed("classname", -1);
    return "com.github.catvod.spider." + api.substring(4);
  }

  /** Declared MD5 after {@code ;md5;}; empty if absent. */
  public static String md5(String declaration) {
    if (declaration == null) return "";
    String[] p = declaration.split(";md5;", 2);
    if (p.length < 2) return "";
    String value = p[1].trim().toLowerCase(Locale.ROOT);
    if (!value.matches("[0-9a-f]{32}")) throw new CheckFailed("md5-format", -1);
    return value;
  }

  public static String digest(byte[] bytes, String algorithm) {
    if (bytes == null) bytes = new byte[0];
    final byte[] hash;
    try {
      hash = MessageDigest.getInstance(algorithm).digest(bytes);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    StringBuilder s = new StringBuilder();
    for (byte b : hash) s.append(String.format(Locale.ROOT, "%02x", b & 255));
    return s.toString();
  }

  /** Empty or over-cap → {@code size}; wrong MD5 → {@code checksum}. */
  public static void verify(byte[] bytes, String expected) {
    if (bytes == null) bytes = new byte[0];
    if (expected == null) expected = "";
    if (bytes.length == 0 || bytes.length > MAX_COMPONENT_BYTES)
      throw new CheckFailed("size", bytes.length);
    if (!expected.isEmpty() && !digest(bytes, "MD5").equals(expected))
      throw new CheckFailed("checksum", bytes.length);
  }

  /** null/blank payload reason code; otherwise empty string. */
  public static String payloadFault(String text) {
    if (text == null) return "empty";
    int n = text.length(), i = 0, j = n;
    while (i < n && text.charAt(i) <= ' ') i++;
    while (j > i && text.charAt(j - 1) <= ' ') j--;
    if (i < j) return "";
    return "empty bytes=" + n;
  }

  /**
   * Same cap as streaming download: read {@code source} as a single buffer with the 16MiB stop.
   * Over-cap throws {@link CheckFailed} with bytes already seen (including the overflowing chunk).
   */
  public static byte[] readAllCapped(byte[] source) throws java.io.IOException {
    if (source == null) source = new byte[0];
    return readComponent(new java.io.ByteArrayInputStream(source));
  }

  /** Streaming read used by the Java facade and by {@link #readAllCapped}. */
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

  /** Message-only reason code for a {@link CheckFailed}. */
  public static String checkLabel(String code, int bytes) {
    if ("size".equals(code)) return "size bytes=" + bytes;
    if ("checksum".equals(code)) return "checksum";
    if ("md5-format".equals(code)) return "md5-format";
    if ("classname".equals(code)) return "classname";
    return code == null || code.isEmpty() ? "unknown" : code;
  }

}
