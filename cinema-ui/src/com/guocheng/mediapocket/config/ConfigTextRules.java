package com.guocheng.mediapocket.config;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Pure 影视仓配置正文（注释剥离 + 一层 Base64 + 结构检查）。
 * 最终 {@code JSONObject} 校验留在 {@code TvBoxConfigText} 门面（org.json / 单引号）。
 */
public final class ConfigTextRules {
  private ConfigTextRules() {}

  public static final int MAX_CHARS = 8 * 1024 * 1024;
  public static final int MAX_DEPTH = 32;

  /** Everything except the platform {@code JSONObject} accept check. */
  public static String normalizeBody(String raw) {
    if (raw == null || raw.isEmpty()) throw new ConfigTextFailed("配置内容是空的");
    if (raw.length() > MAX_CHARS) throw new ConfigTextFailed("配置过大");
    String text = bom(raw.trim());
    if (text.isEmpty()) throw new ConfigTextFailed("配置内容是空的");
    if (base64(text)) {
      String decoded = decode(text);
      if (decoded.length() > MAX_CHARS) throw new ConfigTextFailed("配置过大");
      return preparedJson(decoded);
    }
    return preparedJson(text);
  }

  public static String bom(String text) {
    int i = 0;
    while (i < text.length() && text.charAt(i) == '\uFEFF') i++;
    return i == 0 ? text : text.substring(i).trim();
  }

  public static String preparedJson(String text) {
    text = strip(bom(text)).trim();
    if (text.isEmpty() || text.charAt(0) != '{') throw new ConfigTextFailed("配置不是合法 JSON");
    structure(text);
    return text;
  }

  public static boolean base64(String text) {
    int body = 0, pad = 0;
    boolean padding = false, url = false, std = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;
      if (c == '=') {
        if (pad == 2) return false;
        padding = true;
        pad++;
        continue;
      }
      if (padding) return false;
      if (c == '+' || c == '/') std = true;
      else if (c == '-' || c == '_') url = true;
      else if (!alnum(c)) return false;
      body++;
    }
    return body > 0 && body % 4 != 1 && !(std && url);
  }

  public static String decode(String text) {
    StringBuilder body = new StringBuilder(text.length());
    int pad = 0;
    boolean padding = false, url = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;
      if (c == '=') {
        padding = true;
        pad++;
        continue;
      }
      if (c == '-' || c == '_') url = true;
      body.append(c);
    }
    int need = (4 - body.length() % 4) % 4;
    if (pad != 0 && pad != need) throw new ConfigTextFailed("配置不是合法 JSON");
    if (url) {
      for (int i = 0; i < body.length(); i++) {
        char c = body.charAt(i);
        if (c == '-') body.setCharAt(i, '+');
        else if (c == '_') body.setCharAt(i, '/');
      }
    }
    for (int i = 0; i < need; i++) body.append('=');
    byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(body.toString());
    } catch (IllegalArgumentException e) {
      throw new ConfigTextFailed("配置不是合法 JSON");
    }
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString();
    } catch (CharacterCodingException e) {
      throw new ConfigTextFailed("配置编码无法识别");
    }
  }

  public static String strip(String text) {
    StringBuilder out = new StringBuilder(text.length());
    char quote = 0;
    boolean escape = false;
    int n = text.length(), i = 0;
    while (i < n) {
      char c = text.charAt(i);
      if (quote != 0) {
        out.append(c);
        i++;
        if (escape) {
          escape = false;
          continue;
        }
        if (c == '\\') {
          escape = true;
          continue;
        }
        if (c == quote) quote = 0;
        continue;
      }
      if (c == '"' || c == '\'') {
        quote = c;
        out.append(c);
        i++;
        continue;
      }
      if (c == '#') {
        i = lineComment(text, i + 1, out);
        continue;
      }
      if (c == '/' && i + 1 < n) {
        char next = text.charAt(i + 1);
        if (next == '/') {
          i += 2;
          while (i < n) {
            char d = text.charAt(i);
            if (d == '\n' || d == '\u2028' || d == '\u2029') {
              out.append(d);
              i++;
              break;
            }
            if (d == '\r') {
              out.append('\n');
              i++;
              if (i < n && text.charAt(i) == '\n') i++;
              break;
            }
            i++;
          }
          continue;
        }
        if (next == '*') {
          int j = i + 2;
          while (j + 1 < n && !(text.charAt(j) == '*' && text.charAt(j + 1) == '/')) j++;
          if (j + 1 >= n) throw new ConfigTextFailed("配置注释不完整");
          out.append(' ');
          i = j + 2;
          continue;
        }
      }
      out.append(c);
      i++;
    }
    if (quote != 0 || escape) throw new ConfigTextFailed("配置不是合法 JSON");
    return out.toString();
  }

  public static void structure(String text) {
    int n = text.length(), i = 0;
    while (i < n && ws(text.charAt(i))) i++;
    if (i >= n || text.charAt(i) != '{') throw new ConfigTextFailed("配置不是合法 JSON");
    int[] stack = new int[MAX_DEPTH];
    int sp = 0;
    char quote = 0;
    boolean escape = false;
    for (; i < n; i++) {
      char c = text.charAt(i);
      if (quote != 0) {
        if (escape) {
          escape = false;
          continue;
        }
        if (c == '\\') {
          escape = true;
          continue;
        }
        if (c == quote) quote = 0;
        continue;
      }
      if (c == '"' || c == '\'') {
        quote = c;
        continue;
      }
      if (c == '{' || c == '[') {
        if (sp == MAX_DEPTH) throw new ConfigTextFailed("配置结构过深");
        stack[sp++] = c == '{' ? '}' : ']';
        continue;
      }
      if (c == '}' || c == ']') {
        if (sp == 0 || stack[sp - 1] != c) throw new ConfigTextFailed("配置不是合法 JSON");
        if (--sp == 0) return;
      }
    }
    throw new ConfigTextFailed("配置不是合法 JSON");
  }

  private static int lineComment(String text, int i, StringBuilder out) {
    int n = text.length();
    while (i < n) {
      char d = text.charAt(i);
      if (d == '\n' || d == '\u2028' || d == '\u2029') {
        out.append(d);
        return i + 1;
      }
      if (d == '\r') {
        out.append('\n');
        if (i + 1 < n && text.charAt(i + 1) == '\n') return i + 2;
        return i + 1;
      }
      i++;
    }
    return i;
  }

  private static boolean alnum(char c) {
    return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9';
  }

  private static boolean ws(char c) {
    return c == ' ' || c == '\n' || c == '\r' || c == '\t';
  }
}
