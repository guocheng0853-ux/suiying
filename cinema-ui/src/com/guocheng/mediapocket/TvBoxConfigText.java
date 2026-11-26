package com.guocheng.mediapocket;
import com.guocheng.mediapocket.config.ConfigTextFailed;
import com.guocheng.mediapocket.config.ConfigTextRules;
import org.json.*;
import java.io.*;
/**
 * 影视仓配置正文门面。纯逻辑在 {@link ConfigTextRules}（rust-anything 可移植单元）；
 * 这里只补平台 {@link JSONObject} 验收，并把 {@link ConfigTextFailed} 转成无 cause 的 IOException。
 */
final class TvBoxConfigText {
 static final int MAX_CHARS=ConfigTextRules.MAX_CHARS;
 static final int MAX_DEPTH=ConfigTextRules.MAX_DEPTH;
 static String fetchConfig(String address,JSONObject body,String ua)throws Exception{return normalize(TvBoxNet.fetch(address,body,ua));}
 static String normalize(String raw)throws IOException{
  final String text;
  try{text=ConfigTextRules.normalizeBody(raw);}
  catch(ConfigTextFailed e){throw new IOException(e.getMessage());}
  try{new JSONObject(text);}catch(JSONException e){throw new IOException("配置不是合法 JSON");}
  return text;
 }
}
