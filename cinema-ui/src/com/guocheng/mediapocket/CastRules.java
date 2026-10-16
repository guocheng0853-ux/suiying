package com.guocheng.mediapocket;
import org.json.*;import java.net.*;import java.util.*;
/** A renderer fetches the URI itself; phone-only URLs and protected requests cannot be handed off. */
final class CastRules {
 static String problem(JSONObject source){if(source==null)return "请先选择片源";String raw=source.optString("url");try{URI uri=new URI(raw);String scheme=uri.getScheme(),host=uri.getHost();if(host==null||!("http".equalsIgnoreCase(scheme)||"https".equalsIgnoreCase(scheme)))return "此片源不是 HTTP(S) 直链。磁力/P2P 片源请在应用内播放，或更换直链。";String h=host.toLowerCase(Locale.ROOT).replace("[","").replace("]","");if(h.equals("localhost")||h.endsWith(".localhost")||h.startsWith("127.")||h.equals("::1")||h.equals("0.0.0.0")||h.equals("::"))return "此地址仅在手机本机可用，电视无法直接访问。请换可被电视访问的直链。";if(uri.getUserInfo()!=null)return "此地址包含账号凭据，请换无需额外登录的片源。";}catch(Exception e){return "播放地址无效，请更换片源。";}
  JSONObject hints=source.optJSONObject("behaviorHints"),proxy=hints==null?null:hints.optJSONObject("proxyHeaders"),headers=proxy==null?null:proxy.optJSONObject("request");if(headers!=null&&headers.length()>0)return "此片源需要专用请求头或登录凭据，电视无法直接取流。请换无需额外请求头的直链。";return "";}
 static String mime(String url){try{String path=new URI(url).getPath().toLowerCase(Locale.ROOT);if(path.endsWith(".m3u8"))return "application/vnd.apple.mpegurl";if(path.endsWith(".mp4")||path.endsWith(".m4v"))return "video/mp4";if(path.endsWith(".mkv"))return "video/x-matroska";if(path.endsWith(".ts"))return "video/mp2t";if(path.endsWith(".webm"))return "video/webm";}catch(Exception ignored){}return "video/*";}
}
