package com.guocheng.mediapocket;
import android.content.*;import org.json.*;import java.io.*;import java.util.concurrent.*;
/** Shared TVBox configuration and route identity. Only genuine returned entries are displayed. */
final class FusionCatalog {
 static final ExecutorService IO=Executors.newSingleThreadExecutor();
 static final class Session {final String url;final JSONObject config,site;Session(String u,JSONObject c,JSONObject s){url=u;config=c;site=s;}}
 static Session session(Context c,String requestedUrl,String requestedSite)throws Exception {
  SharedPreferences p=c.getSharedPreferences("data",0);JSONObject bundled;
  try(InputStream in=c.getAssets().open("fusion-default.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bundled=new JSONObject(out.toString("UTF-8"));}
  String url=requestedUrl==null||requestedUrl.isEmpty()?p.getString("tvbox.active",bundled.getString("base")):requestedUrl;
  JSONObject config;String stored=p.getString("tvbox.config:"+url,"");
  if(!stored.isEmpty())config=new JSONObject(stored);else if(url.equals(bundled.getString("base")))config=bundled.getJSONObject("config");else config=new JSONObject(TvBoxNet.fetch(url,null,null));
  config=HomeSources.merge(c,config);JSONArray sites=config.optJSONArray("sites");if(sites==null)throw new IOException("配置没有站点");
  String selected=requestedSite==null||requestedSite.isEmpty()?p.getString("fusion.site:"+url,""):requestedSite;
  JSONObject found=null,first=null;
  for(int i=0;i<sites.length();i++){JSONObject s=sites.optJSONObject(i);if(s==null||s.optInt("type",3)!=3)continue;if(first==null)first=s;if(s.optString("key").equals(selected))found=s;}
  if(found==null&&!selected.isEmpty()&&requestedSite!=null&&!requestedSite.isEmpty())throw new IOException("该站点已从配置移除，请更新首页");
  if(found==null)for(int i=0;i<sites.length();i++){JSONObject s=sites.optJSONObject(i);if(s!=null&&s.optString("api").equals("csp_AListSh")){found=s;break;}}
  if(found==null)found=first;if(found==null)throw new IOException("配置中没有支持的站点");
  p.edit().putString("tvbox.config:"+url,config.toString()).apply();
  if(found.optString("api").equals("csp_AListSh")){found=new JSONObject(found.toString());JSONArray ext=found.optJSONArray("ext");for(int i=0;ext!=null&&i<ext.length();i++){JSONObject entry=ext.optJSONObject(i);if(entry!=null&&entry.has("noPoster"))entry.put("noPoster",false);}}
  return new Session(url,config,found);
 }
 static JSONObject request(Context context,Session s,String op,String value,String flag,int page)throws Exception{if(CmsApi.MARKER.equals(s.site.optString("api")))return CmsApi.request(s.site,op,value,flag,page);return TvSpiderRuntime.get(context).request(s.config,s.site,s.url,op,value,flag,page);}
 static String cacheKey(Session s,String category){return "fusion.catalog.v3:"+s.url+"|"+s.site.optString("key")+"|"+category;}
 static JSONArray normalize(Session s,JSONArray list,String category)throws Exception{
  JSONArray out=new JSONArray();if(list==null)return out;
  for(int i=0;i<Math.min(list.length(),72);i++){JSONObject v=list.optJSONObject(i);if(v==null||v.optString("vod_id").isEmpty())continue;
   String pic=v.optString("vod_pic");try{if(!pic.isEmpty())pic=TvPosterRules.resolve(s.url,pic);}catch(Exception e){pic="";}
   JSONObject m=new JSONObject().put("id",v.optString("vod_id")).put("type","movie").put("name",v.optString("vod_name","影片")).put("poster",pic).put("background",pic).put("description",plain(v.optString("vod_content"))).put("releaseInfo",v.optString("vod_year")).put("fusion.config",s.url).put("fusion.site",s.site.optString("key")).put("fusion.source",s.site.optString("name","影视仓")).put("fusion.category",category);
   String rating=v.optString("vod_douban_score");try{double score=Double.parseDouble(rating);if(score>0&&score<=10)m.put("imdbRating",rating);}catch(Exception ignored){}out.put(m);
  }return out;
 }
 static String plain(String html){return SynopsisText.plain(html);}
 static void open(Context c,JSONObject m){Intent in=new Intent(c,FusionLibraryActivity.class);in.putExtra("config",m.optString("fusion.config"));in.putExtra("site",m.optString("fusion.site"));in.putExtra("id",m.optString("id"));in.putExtra("category",m.optString("fusion.category"));c.startActivity(in);}
}
