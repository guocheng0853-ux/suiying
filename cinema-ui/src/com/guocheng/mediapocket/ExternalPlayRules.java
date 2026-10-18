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
 static boolean allowedVideo(String data){try{URI u=URI.create(data);String scheme=u.getScheme()==null?"":u.getScheme().toLowerCase(Locale.ROOT);return scheme.equals("http")||scheme.equals("https")||scheme.equals("file")||scheme.equals("content")||scheme.isEmpty();}catch(Exception e){return schemeVideo(data);}}
 static boolean videoPath(String data){try{URI u=URI.create(data);String scheme=u.getScheme()==null?"":u.getScheme().toLowerCase(Locale.ROOT);if(!(scheme.equals("http")||scheme.equals("https")||scheme.equals("file")))return false;String path=u.getPath()==null?"":u.getPath().toLowerCase(Locale.ROOT);return videoName(path);}catch(Exception e){return schemeVideo(data)&&videoName(rawName(data));}}
 /** java.net.URI 对未编码的中文、空格会直接抛异常；文件管理器常发这种原始路径，退回前缀判断。 */
 static boolean schemeVideo(String data){if(data==null)return false;String low=data.trim().toLowerCase(Locale.ROOT);return low.startsWith("http://")||low.startsWith("https://")||low.startsWith("file:/")||low.startsWith("content://");}
 static String rawName(String data){if(data==null)return "";int q=data.indexOf('?');String tail=q>=0?data.substring(0,q):data;int slash=Math.max(tail.lastIndexOf('/'),tail.lastIndexOf('\\'));String name=slash>=0?tail.substring(slash+1):tail;try{name=URLDecoder.decode(name,"UTF-8");}catch(Exception ignored){}return name;}
 static boolean videoName(String name){if(name==null)return false;String low=name.toLowerCase(Locale.ROOT);return low.endsWith(".mp4")||low.endsWith(".mkv")||low.endsWith(".webm")||low.endsWith(".avi")||low.endsWith(".ts")||low.endsWith(".m4v")||low.endsWith(".mov")||low.endsWith(".flv")||low.endsWith(".m3u8");}
 static String title(String data,String extra){if(extra!=null&&!extra.trim().isEmpty())return extra.trim();try{URI u=URI.create(data==null?"":data);String path=u.getPath();if(path==null||path.isEmpty())return "外部视频";int slash=path.lastIndexOf('/');String name=slash>=0?path.substring(slash+1):path;name=URLDecoder.decode(name,"UTF-8");return name.isEmpty()?"外部视频":name;}catch(Exception e){String name=rawName(data);return name.isEmpty()?"外部视频":name;}}
 static String watchId(String data){int h=data==null?0:data.hashCode();return "external:"+Integer.toUnsignedString(h,16);}
 static JSONObject stream(String data,String title,String[] headerPairs)throws JSONException{JSONObject headers=pairs(headerPairs);JSONObject stream=new JSONObject().put("url",data==null?"":data).put("name","外部视频").put("title",title);if(headers.length()>0)stream.put("header",headers).put("behaviorHints",new JSONObject().put("proxyHeaders",new JSONObject().put("request",headers)));return stream;}
 static JSONObject pairs(String[] headerPairs)throws JSONException{JSONObject out=new JSONObject();if(headerPairs==null)return out;for(int i=0;i+1<headerPairs.length;i+=2){String k=headerPairs[i],v=headerPairs[i+1];if(k==null||v==null||ExternalPlayerRules.sensitive(k))continue;out.put(k,v);}return out;}
 /** suiying://import?url=https://… 或 ?config= 。只接受 http/https，且地址里不能带账号。 */
 static String importUrl(String raw){if(raw==null||raw.isEmpty())return null;try{URI u=URI.create(raw.trim());if(!"suiying".equalsIgnoreCase(u.getScheme())||!"import".equalsIgnoreCase(u.getHost()))return null;String query=u.getRawQuery();String found=queryValue(query,"url");if(found==null)found=queryValue(query,"config");if(found==null&&u.getPath()!=null&&u.getPath().length()>1)found=URLDecoder.decode(u.getPath().substring(1),"UTF-8");return httpConfig(found);}catch(Exception e){return null;}}
 static String queryValue(String query,String key){if(query==null)return null;for(String part:query.split("&")){int eq=part.indexOf('=');if(eq<=0)continue;if(!part.substring(0,eq).equals(key))continue;try{return URLDecoder.decode(part.substring(eq+1),"UTF-8");}catch(Exception e){return null;}}return null;}
 static String httpConfig(String value){if(value==null)return null;try{URI u=URI.create(value.trim());String scheme=u.getScheme();if(scheme==null||!(scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https"))||u.getHost()==null||u.getRawUserInfo()!=null)return null;return u.toString();}catch(Exception e){return null;}}
 static JSONArray addProfile(JSONArray profiles,String url)throws JSONException{JSONArray out=profiles==null?new JSONArray():profiles;for(int i=0;i<out.length();i++)if(url.equals(out.optString(i)))return out;out.put(url);return out;}
}
