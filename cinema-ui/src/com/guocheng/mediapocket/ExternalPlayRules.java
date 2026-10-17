package com.guocheng.mediapocket;
import org.json.*;import java.net.*;import java.util.*;
/** 外部打开视频，以及 suiying://import 配置地址的解析。 */
final class ExternalPlayRules {
 static final String LABEL="随影 (VLC)";
 private ExternalPlayRules(){}
 static boolean playable(String action,String data,String mime){
  if(action==null||!action.equals("android.intent.action.VIEW")||data==null||data.isEmpty())return false;
  if(mime!=null&&mime.toLowerCase(Locale.ROOT).startsWith("video/"))return allowedVideo(data);
  return videoPath(data);
 }
 static boolean allowedVideo(String data){try{URI u=URI.create(data);String scheme=u.getScheme()==null?"":u.getScheme().toLowerCase(Locale.ROOT);return scheme.equals("http")||scheme.equals("https")||scheme.equals("file")||scheme.equals("content")||scheme.isEmpty();}catch(Exception e){return false;}}
 static boolean videoPath(String data){try{URI u=URI.create(data);String scheme=u.getScheme()==null?"":u.getScheme().toLowerCase(Locale.ROOT);if(!(scheme.equals("http")||scheme.equals("https")||scheme.equals("file")))return false;String path=u.getPath()==null?"":u.getPath().toLowerCase(Locale.ROOT);return path.endsWith(".mp4")||path.endsWith(".mkv")||path.endsWith(".webm")||path.endsWith(".avi")||path.endsWith(".ts")||path.endsWith(".m4v")||path.endsWith(".mov")||path.endsWith(".flv")||path.endsWith(".m3u8");}catch(Exception e){return false;}}
 static String title(String data,String extra){if(extra!=null&&!extra.trim().isEmpty())return extra.trim();try{URI u=URI.create(data==null?"":data);String path=u.getPath();if(path==null||path.isEmpty())return "外部视频";int slash=path.lastIndexOf('/');String name=slash>=0?path.substring(slash+1):path;name=URLDecoder.decode(name,"UTF-8");return name.isEmpty()?"外部视频":name;}catch(Exception e){return "外部视频";}}
 static String watchId(String data){int h=data==null?0:data.hashCode();return "external:"+Integer.toUnsignedString(h,16);}
 static JSONObject stream(String data,String title,String[] headerPairs)throws JSONException{JSONObject headers=pairs(headerPairs);JSONObject stream=new JSONObject().put("url",data==null?"":data).put("name","外部视频").put("title",title);if(headers.length()>0)stream.put("header",headers).put("behaviorHints",new JSONObject().put("proxyHeaders",new JSONObject().put("request",headers)));return stream;}
 static JSONObject pairs(String[] headerPairs)throws JSONException{JSONObject out=new JSONObject();if(headerPairs==null)return out;for(int i=0;i+1<headerPairs.length;i+=2){String k=headerPairs[i],v=headerPairs[i+1];if(k==null||v==null||ExternalPlayerRules.sensitive(k))continue;out.put(k,v);}return out;}
 /** suiying://import?url=https://… 或 ?config= 。只接受 http/https，且地址里不能带账号。 */
 static String importUrl(String raw){if(raw==null||raw.isEmpty())return null;try{URI u=URI.create(raw.trim());if(!"suiying".equalsIgnoreCase(u.getScheme())||!"import".equalsIgnoreCase(u.getHost()))return null;String query=u.getRawQuery();String found=queryValue(query,"url");if(found==null)found=queryValue(query,"config");if(found==null&&u.getPath()!=null&&u.getPath().length()>1)found=URLDecoder.decode(u.getPath().substring(1),"UTF-8");return httpConfig(found);}catch(Exception e){return null;}}
 static String queryValue(String query,String key){if(query==null)return null;for(String part:query.split("&")){int eq=part.indexOf('=');if(eq<=0)continue;if(!part.substring(0,eq).equals(key))continue;try{return URLDecoder.decode(part.substring(eq+1),"UTF-8");}catch(Exception e){return null;}}return null;}
 static String httpConfig(String value){if(value==null)return null;try{URI u=URI.create(value.trim());String scheme=u.getScheme();if(scheme==null||!(scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https"))||u.getHost()==null||u.getRawUserInfo()!=null)return null;return u.toString();}catch(Exception e){return null;}}
 static JSONArray addProfile(JSONArray profiles,String url)throws JSONException{JSONArray out=profiles==null?new JSONArray():profiles;for(int i=0;i<out.length();i++)if(url.equals(out.optString(i)))return out;out.put(url);return out;}
}
