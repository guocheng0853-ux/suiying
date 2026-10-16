package com.guocheng.mediapocket;
import org.json.*;import java.util.*;import java.util.concurrent.*;
/** Parallel metadata lookup with a bounded wait; placeholder responses never overwrite real cached data. */
final class DetailMetadata {
 static String info(JSONObject m){ArrayList<String> p=new ArrayList<>();String rating=m.optString("imdbRating");if(!rating.isEmpty())p.add("★ "+rating);String date=m.optString("released",m.optString("releaseInfo"));if(date.length()>=10&&date.charAt(4)=='-')date=date.substring(0,10).replace('-','/');if(!date.isEmpty())p.add(date);if(!m.optString("runtime").isEmpty())p.add(m.optString("runtime"));return String.join("  ",p);}
 static boolean useful(JSONObject m){if(m==null)return false;String n=m.optString("name").trim();return !n.startsWith("影片资料")&&!n.isEmpty()&&!n.matches("(?i)(IMDb\\s*)?(tt)?[0-9]+")&&!m.optString("description").toLowerCase(Locale.ROOT).contains("metadata is still warming");}
 static JSONObject merge(JSONObject old,JSONObject fresh)throws Exception{JSONObject out=old==null?new JSONObject():new JSONObject(old.toString());if(fresh!=null){Iterator<String> k=fresh.keys();while(k.hasNext()){String key=k.next();Object v=fresh.opt(key);if(v==null||v==JSONObject.NULL||v instanceof String&&((String)v).trim().isEmpty()||v instanceof JSONArray&&((JSONArray)v).length()==0)continue;out.put(key,v);}}return out;}
 static JSONObject fetch(JSONArray addons,String id,String type)throws Exception{
  LinkedHashSet<String> urls=new LinkedHashSet<>();JSONArray providers=AddonRegistry.forResource(addons==null?new JSONArray():addons,"meta",type,id);
  for(int i=0;i<Math.min(providers.length(),8);i++){JSONObject p=providers.optJSONObject(i);if(p!=null&&!p.optString("base").isEmpty())urls.add(p.optString("base")+"/meta/"+type+"/"+Net.esc(id)+".json");}
  if(id.matches("tt[0-9]+"))urls.add("https://v3-cinemeta.strem.io/meta/"+type+"/"+Net.esc(id)+".json");
  return lookup(urls,22000);
 }
 static JSONObject lookup(Collection<String> urls,int budgetMs)throws Exception{
  if(urls.isEmpty())throw new Exception("没有适用的影片资料插件");
  ExecutorService pool=Executors.newFixedThreadPool(Math.min(4,urls.size()));CompletionService<JSONObject> done=new ExecutorCompletionService<>(pool);ArrayList<Future<JSONObject>> pending=new ArrayList<>();
  for(String url:urls)pending.add(done.submit(()->{JSONObject m=CinemaCatalogClient.json(url).optJSONObject("meta");if(!useful(m))throw new Exception("资料源尚未返回完整影片信息");return m;}));
  Exception failure=new Exception("影片资料请求超时");long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(budgetMs);
  try{for(int i=0;i<pending.size();i++){long remaining=end-System.nanoTime();if(remaining<=0)break;Future<JSONObject> f=done.poll(remaining,TimeUnit.NANOSECONDS);if(f==null)break;try{return f.get();}catch(ExecutionException e){Throwable cause=e.getCause();failure=new Exception(cause==null?"资料请求失败":cause.getMessage());}}throw failure;}finally{for(Future<?> f:pending)f.cancel(true);pool.shutdownNow();}
 }
}
