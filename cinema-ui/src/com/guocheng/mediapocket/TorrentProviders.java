package com.guocheng.mediapocket;
import org.json.*;import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;import java.util.regex.*;
/** Private Stremio stream providers; HTTP APIs supply metadata, the existing torrent engine supplies media. */
final class TorrentProviders {
 static final String EZTV="eztv",TPB="piratebay";
 static final ScheduledExecutorService TIMER=Executors.newScheduledThreadPool(2,r->{Thread t=new Thread(r,"torrent-api-budget");t.setDaemon(true);return t;});
 interface Fetch {String get(String url,int budget)throws Exception;}
 static Fetch fetch=TorrentProviders::http;
 static final class Identity {
  final String imdb,type;final int season,episode;
  Identity(String type,String id){String[] p=id.split(":",-1);if(!p[0].matches("tt[0-9]{5,12}")||!(type.equals("movie")||type.equals("series")))throw new IllegalArgumentException("需要 IMDb 影片编号");this.type=type;imdb=p[0];if(type.equals("series")){if(p.length!=3)throw new IllegalArgumentException("剧集需要季号和集号");season=Integer.parseInt(p[1]);episode=Integer.parseInt(p[2]);if(season<0||episode<1)throw new IllegalArgumentException("无效季集");}else{if(p.length!=1)throw new IllegalArgumentException("无效电影编号");season=episode=-1;}}
 }
 static JSONObject manifest(String provider)throws JSONException{
  boolean ez=EZTV.equals(provider);if(!ez&&!TPB.equals(provider))throw new IllegalArgumentException("未知插件");
  JSONArray types=ez?new JSONArray().put("series"):new JSONArray().put("movie").put("series");
  return new JSONObject().put("id","com.suiying.private."+provider).put("version","1.0.0").put("name",ez?"EZTV · 私用":"海盗湾 · 私用").put("description","P2P 播放源 · 按 IMDb 匹配，剧集严格匹配季集；可用性取决于站点和做种情况。")
   .put("types",types).put("catalogs",new JSONArray()).put("resources",new JSONArray().put(new JSONObject().put("name","stream").put("types",types).put("idPrefixes",new JSONArray().put("tt"))));
 }
 static JSONArray streams(String provider,String type,String id,int budget)throws Exception{
  Identity wanted=new Identity(type,id);long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(budget);JSONArray records=new JSONArray();
  if(EZTV.equals(provider)){
   if(!type.equals("series"))return records;
   for(int page=1;page<=10;page++){
    JSONObject response=new JSONObject(fetch.get("https://eztv.yt/api/get-torrents?imdb_id="+wanted.imdb.substring(2)+"&limit=100&page="+page,left(end)));
    JSONArray list=response.optJSONArray("torrents");if(list==null)throw new IOException("EZTV 未返回 torrents 数据");
    for(int i=0;i<list.length();i++)records.put(list.opt(i));
    if(list.length()<100||response.optLong("torrents_count",Long.MAX_VALUE)<=page*100L||normalize(provider,records,wanted).length()>0)break;
   }
  }else if(TPB.equals(provider)){
   records=new JSONArray(fetch.get("https://apibay.org/q.php?q="+URLEncoder.encode(wanted.imdb,"UTF-8")+"&cat=200",left(end)));
  }else throw new IllegalArgumentException("未知插件");
  return normalize(provider,records,wanted);
 }
 static int left(long deadline)throws IOException{long ms=TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime());if(ms<=0||Thread.currentThread().isInterrupted())throw new InterruptedIOException("插件查询超时或已取消");return (int)Math.min(ms,18000);}
 static JSONArray normalize(String provider,JSONArray records,Identity wanted)throws JSONException{
  LinkedHashMap<String,JSONObject> unique=new LinkedHashMap<>();boolean ez=EZTV.equals(provider);
  for(int i=0;i<Math.min(records.length(),1000);i++){
   JSONObject r=records.optJSONObject(i);if(r==null)continue;
   String hash=r.optString(ez?"hash":"info_hash").toLowerCase(Locale.ROOT);if(!hash.matches("[a-f0-9]{40}")||hash.matches("0{40}"))continue;
   String imdb=r.optString(ez?"imdb_id":"imdb").replaceFirst("^tt","");
   if(!imdb.isEmpty()&&!imdb.replaceFirst("^0+(?!$)","").equals(wanted.imdb.substring(2).replaceFirst("^0+(?!$)","")))continue;
   String title=r.optString(ez?"title":"name");
   if(wanted.type.equals("series")){
    if(ez){if(r.optInt("season",-1)!=wanted.season||r.optInt("episode",-1)!=wanted.episode)continue;}
    else if(!episode(title,wanted.season,wanted.episode))continue;
   }else if(Pattern.compile("(?i)(?:\\bS[0-9]{1,2}E[0-9]{1,3}\\b|\\b[0-9]{1,2}x[0-9]{1,3}\\b)").matcher(title).find())continue;
   long seeds=Math.max(0,r.optLong("seeds",r.optLong("seeders",0))),size=Math.max(0,r.optLong("size_bytes",r.optLong("size",0)));
   String quality="";Matcher q=Pattern.compile("(?i)\\b(2160p|1080p|720p|480p)\\b").matcher(title);if(q.find())quality=q.group(1).toLowerCase(Locale.ROOT);
   JSONObject stream=new JSONObject().put("name",(ez?"EZTV":"海盗湾")+(quality.isEmpty()?"":" · "+quality)).put("title",title+"\n做种 "+seeds+(size>0?String.format(Locale.ROOT," · %.2f GB",size/1073741824.0):"")).put("infoHash",hash).put("suiying.seeders",seeds).put("behaviorHints",new JSONObject().put("bingeGroup",provider+"-"+quality));
   JSONObject old=unique.get(hash);if(old==null||old.optLong("suiying.seeders")<seeds)unique.put(hash,stream);
  }
  ArrayList<JSONObject> sorted=new ArrayList<>(unique.values());sorted.sort((a,b)->Long.compare(b.optLong("suiying.seeders"),a.optLong("suiying.seeders")));JSONArray out=new JSONArray();for(JSONObject r:sorted){if(out.length()>=100)break;out.put(r);}return out;
 }
 /** Exclude packs and multi-episode releases: largest-file selection cannot safely choose an episode. */
 static boolean episode(String title,int season,int episode){
  Matcher m=Pattern.compile("(?i)(?<![A-Za-z0-9])(?:S(\\d{1,2})E(\\d{1,3})|(\\d{1,2})x(\\d{1,3}))(?![A-Za-z0-9])").matcher(title);
  if(!m.find())return false;int s=Integer.parseInt(m.group(1)==null?m.group(3):m.group(1)),e=Integer.parseInt(m.group(2)==null?m.group(4):m.group(2));
  String rest=title.substring(m.end());if(rest.matches("(?is)^(?:[ ._-]*(?:E\\d|S\\d|\\d{1,2}x\\d)|[-+]\\d).*"))return false;
  return s==season&&e==episode&&!m.find();
 }
 static String http(String url,int budget)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(Math.min(10000,budget));c.setReadTimeout(budget);c.setRequestProperty("User-Agent","Mozilla/5.0 Suiying/0.1.49");c.setRequestProperty("Accept","application/json");
  ScheduledFuture<?> kill=TIMER.schedule(c::disconnect,budget,TimeUnit.MILLISECONDS);
  try{int code=c.getResponseCode();if(code!=200)throw new IOException("站点返回 HTTP "+code);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(out.size()+n>4*1024*1024)throw new IOException("响应过大");out.write(b,0,n);}return out.toString("UTF-8");}}
  finally{kill.cancel(false);c.disconnect();}
 }
}
