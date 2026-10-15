package com.guocheng.mediapocket;
import org.json.*;import java.net.*;import java.io.*;import java.util.*;
/** Standard JSON CMS adapter. Direct media URLs only; no web parser or remote JAR. */
final class CmsApi {
 static final String MARKER="csp_MediaPocketCms";
 static String url(String api,Map<String,String> params)throws Exception{
  URI u=new URI(api);if(!("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme()))||u.getHost()==null||u.getUserInfo()!=null)throw new IOException("接口地址不支持");
  ArrayList<String> parts=new ArrayList<>();String q=u.getRawQuery();if(q!=null)for(String pair:q.split("&")){String key=URLDecoder.decode(pair.split("=",2)[0],"UTF-8");if(!params.containsKey(key)&&!Arrays.asList("ac","t","pg","wd","ids").contains(key)&&!pair.isEmpty())parts.add(pair);}
  for(Map.Entry<String,String> e:params.entrySet())parts.add(URLEncoder.encode(e.getKey(),"UTF-8")+"="+URLEncoder.encode(e.getValue(),"UTF-8"));
  return new URI(u.getScheme(),u.getRawAuthority(),u.getRawPath(),null,null).toASCIIString()+"?"+String.join("&",parts);
 }
 static JSONObject request(JSONObject site,String op,String value,String flag,int page)throws Exception{
  if(op.equals("player"))return player(value);
  JSONObject ext=site.optJSONObject("ext");if(ext==null)throw new IOException("公开源缺少接口地址");String api=ext.getString("api");Map<String,String> args=new LinkedHashMap<>();args.put("ac",op.equals("home")?"list":"detail");
  if(op.equals("category")){args.put("t",value);args.put("pg",String.valueOf(Math.max(1,page)));}
  else if(op.equals("search")){args.put("wd",value);args.put("pg",String.valueOf(Math.max(1,page)));}
  else if(op.equals("detail"))args.put("ids",value);
  else if(!op.equals("home"))throw new IOException("公开源不支持此操作");
  JSONObject response=CinemaCatalogClient.json(url(api,args));JSONArray list=response.optJSONArray("list");if(list==null)throw new IOException("公开源返回格式不支持");
  if(op.equals("home")&&(list.length()==0||list.optJSONObject(0)==null||!list.optJSONObject(0).has("vod_pic"))){Map<String,String> recent=new LinkedHashMap<>();recent.put("ac","detail");recent.put("pg","1");JSONArray full=CinemaCatalogClient.json(url(api,recent)).optJSONArray("list");if(full!=null)response.put("list",full);}
  return response;
 }
 static JSONObject player(String value)throws Exception{URI u=new URI(value);if(!("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme()))||u.getHost()==null||u.getUserInfo()!=null)throw new IOException("公开源没有返回直接播放地址");String path=u.getPath();if(path==null||!(path.toLowerCase(Locale.ROOT).endsWith(".m3u8")||path.toLowerCase(Locale.ROOT).endsWith(".mp4")))throw new IOException("此地址需要额外解析，尚未支持");return new JSONObject().put("parse",0).put("jx",0).put("url",value).put("header",new JSONObject().put("User-Agent","Mozilla/5.0"));}
 static JSONObject mergeAll(JSONObject config,JSONArray additions)throws Exception{for(int i=0;i<additions.length();i++)merge(config,additions.getJSONObject(i));return config;}
 static JSONObject merge(JSONObject config,JSONObject addition)throws Exception{JSONArray sites=config.optJSONArray("sites");if(sites==null)throw new IOException("配置没有站点");for(int i=0;i<sites.length();i++){JSONObject s=sites.optJSONObject(i);if(s!=null&&addition.optString("key").equals(s.optString("key")))return config;}sites.put(new JSONObject(addition.toString()));return config;}
}
