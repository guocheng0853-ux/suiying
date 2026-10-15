package com.guocheng.mediapocket;
import java.util.*;import java.util.regex.*;import org.json.*;
/** 网页嗅探与解析接口的纯规则部分（可在 JVM 单测）。 */
final class SniffRules {
 // 参考 TVBox/FongMi 嗅探规则：地址中只出现一次 http，路径以媒体扩展名结尾；不把 .ts 分片当成整片地址
 static final Pattern MEDIA=Pattern.compile("^https?://((?!https?://).){8,}?\\.(m3u8|mp4|mkv|flv|m4a|mp3|aac|webm)(\\?.*)?$|^https?://((?!https?://).)*?(video/tos|obj/tos)/.*",Pattern.CASE_INSENSITIVE);
 static final Pattern NOISE=Pattern.compile("(?i)\\.(js|css|png|jpe?g|gif|webp|ico|svg|woff2?|ttf)(\\?|$)|[?&](url|v|vid)=https?%3A|[?&](url|v)=https?://|/(ad|ads|adv|advert|guanggao)/|googlesyndication|doubleclick|cnzz|umeng|hm\\.baidu");
 static boolean isMedia(String url){return url!=null&&url.length()<4096&&!NOISE.matcher(url).find()&&MEDIA.matcher(url).matches();}
 /** TVBox 解析接口：直接把原地址拼在解析地址后面。 */
 static String parserTarget(String parser,String page){return parser+page;}
 /** 解析接口返回的 JSON：url 字段（或 data.url），可附带 header/headers。 */
 static JSONObject parseJson(String body)throws Exception{
  JSONObject o=new JSONObject(body.trim());JSONObject data=o.optJSONObject("data");
  String url=o.optString("url",data==null?"":data.optString("url"));
  if(url.isEmpty()||!url.matches("(?i)^https?://.+"))throw new Exception("解析接口没有返回视频地址");
  JSONObject h=o.optJSONObject("header");if(h==null)h=o.optJSONObject("headers");if(h==null&&data!=null)h=data.optJSONObject("header");
  JSONObject out=new JSONObject().put("url",url).put("header",h==null?new JSONObject():h);
  String ua=o.optString("user-agent",o.optString("ua",""));if(!ua.isEmpty()&&!out.getJSONObject("header").has("User-Agent"))out.getJSONObject("header").put("User-Agent",ua);
  String ref=o.optString("referer","");if(!ref.isEmpty()&&!out.getJSONObject("header").has("Referer"))out.getJSONObject("header").put("Referer",ref);
  return out;
 }
 /** 配置里的 parses：type 1 为 JSON 接口，其余按网页嗅探；最多 3 个，跳过 Demo/空地址。 */
 static List<JSONObject> parsers(JSONObject config){
  ArrayList<JSONObject> out=new ArrayList<>();JSONArray p=config==null?null:config.optJSONArray("parses");
  for(int i=0;p!=null&&i<p.length()&&out.size()<3;i++){JSONObject x=p.optJSONObject(i);if(x==null)continue;String u=x.optString("url");if(!u.matches("(?i)^https?://.+"))continue;out.add(x);}
  return out;
 }
}
