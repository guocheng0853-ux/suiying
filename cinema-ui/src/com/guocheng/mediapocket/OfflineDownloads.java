package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.Environment;import android.widget.Toast;import org.json.*;import java.io.File;import java.util.*;import java.util.concurrent.*;
/**
 * 0.1.106 离线下载：用系统下载管理器把直链片源存到本应用的影片目录（不需要存储权限，卸载时一起删除）。
 * 入口：Emby 电影 / 单集详情「⬇ 下载」，Emby 和影视仓的选集长按，播放页「更多」，季列表「批量下载」。磁力、m3u8 分段流不支持。
 * 记录在 offline.v1：[{dm 下载编号, name 片名, watch 观看记录编号, file 文件路径, created, meta}]。
 */
final class OfflineDownloads {
 private OfflineDownloads(){}
 static final String KEY="offline.v1";
 static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"offline");t.setDaemon(true);return t;});
 static final java.util.concurrent.atomic.AtomicLong IDS=new java.util.concurrent.atomic.AtomicLong();
 static long unique(long now){return IDS.updateAndGet(prev->Math.max(prev+1,now));}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("data",0);}
 static JSONArray list(Context c){try{return new JSONArray(prefs(c).getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}
 static synchronized void save(Context c,JSONArray a){prefs(c).edit().putString(KEY,a.toString()).apply();}
 static final java.util.concurrent.ConcurrentHashMap<String,long[]> LIVE=new java.util.concurrent.ConcurrentHashMap<>();
 static final java.util.concurrent.ConcurrentHashMap<String,Long> FLUSHED=new java.util.concurrent.ConcurrentHashMap<>();
 static SharedPreferences progressPrefs(Context c){return c.getSharedPreferences("offline.progress",0);}
 /** 进度只留在内存里，大约 5 秒才写入独立的 offline.progress，避免每秒重写主设置。 */
 static void progress(Context c,String id,long done,long total,long speed){LIVE.put(id,new long[]{done,total,speed});long now=android.os.SystemClock.elapsedRealtime();Long last=FLUSHED.get(id);if(!OfflineProgress.due(last==null?-1L:last,now))return;FLUSHED.put(id,now);progressPrefs(c).edit().putString(id,done+"|"+total+"|"+speed).apply();}
 static long[] live(Context c,String id){long[] m=LIVE.get(id);if(m!=null)return m;String s=progressPrefs(c).getString(id,null);if(s==null)return null;String[] p=s.split("\\|");try{return new long[]{Long.parseLong(p[0]),p.length>1?Long.parseLong(p[1]):-1L,p.length>2?Long.parseLong(p[2]):0L};}catch(Exception e){return null;}}
 static void forgetProgress(Context c,String id){if(id==null)return;LIVE.remove(id);FLUSHED.remove(id);progressPrefs(c).edit().remove(id).apply();}

 /** Emby：取直接串流地址（服务器原文件）。只能转码播放的片源不下载。 */
 static void emby(Activity a,JSONObject server,String itemId,String name,JSONObject meta){try{JSONObject src=new JSONObject().put("kind","emby").put("server",server.optString("id")).put("item",itemId);String dup=blocked(a,src,null);if(dup!=null){Toast.makeText(a,dup,Toast.LENGTH_LONG).show();return;}}catch(Exception ignored){}confirm(a,name,()->embyNow(a,server,itemId,name,meta));}
 /** 不再弹确认框。给播放页用：确认已经问过，这里只取地址、入队，不碰播放器。 */
 static void embyNow(Activity a,JSONObject server,String itemId,String name,JSONObject meta){CinemaWait wait=CinemaWait.show(a,"准备下载","正在向 Emby 获取下载地址…",true,true);
  IO.execute(()->{try{JSONObject rec=embyRecord(a,server,itemId,name,meta,System.currentTimeMillis());
    a.runOnUiThread(()->{wait.dismiss();if(a.isFinishing())return;enqueueRecord(a,rec);});
   }catch(Exception e){String why=e instanceof IllegalStateException&&e.getMessage()!=null?e.getMessage():EmbyApi.explain(e);a.runOnUiThread(()->{wait.dismiss();Toast.makeText(a,"无法下载："+why,Toast.LENGTH_LONG).show();});}});}
 static JSONObject embyRecord(Context c,JSONObject server,String itemId,String name,JSONObject meta,long now)throws Exception{String[] got=embyUrl(c,server,itemId);
  JSONObject m=meta==null?new JSONObject():new JSONObject(meta.toString());String watch=EmbyApi.watchId(server.optString("id"),itemId);m.put("id",watch);if(m.optString("name").isEmpty())m.put("name",name);
  JSONObject src=new JSONObject().put("kind","emby").put("server",server.optString("id")).put("item",itemId);
  return build(c,got[0],new JSONObject(),name,watch,got[1],m,src,now);}
 /** Emby 原文件地址和扩展名；只允许转码的片源抛出说明。下载中链接过期时也用它重新取。 */
 static String[] embyUrl(Context c,JSONObject server,String itemId)throws Exception{JSONObject info=EmbyClient.playbackInfo(c,server,itemId,0);EmbyApi.Choice choice=EmbyApi.select(server.optString("url"),itemId,server.optString("token"),info,null,EmbyApi.clientOf(server));
  if(!choice.direct)throw new IllegalStateException(OfflineQueueRules.TRANSCODE);
  String container="";JSONArray sources=info.optJSONArray("MediaSources");for(int i=0;sources!=null&&i<sources.length();i++){JSONObject s=sources.optJSONObject(i);if(s!=null&&choice.mediaSourceId.equals(s.optString("Id")))container=s.optString("Container");}
  return new String[]{choice.url,OfflineRules.extension(container,choice.url)};}
 /** 影视仓：先按线路解析出真实地址，再判断能不能下载。 */
 static void fusion(Activity a,FusionCatalog.Session session,String flag,String value,String name,String watch,JSONObject vod){try{JSONObject src=new JSONObject().put("kind","fusion").put("config",session.url).put("site",session.site.optString("key")).put("flag",flag).put("value",value);String dup=blocked(a,src,null);if(dup!=null){Toast.makeText(a,dup,Toast.LENGTH_LONG).show();return;}}catch(Exception ignored){}confirm(a,name,()->fusionNow(a,session,flag,value,name,watch,vod));}
 /** 不再弹确认框。站点按配置和站点键在下载线程上打开，避免播放页点下载时卡住界面。 */
 static void fusionNow(Activity a,String config,String site,String flag,String value,String name,String watch){CinemaWait wait=CinemaWait.show(a,"准备下载","正在解析播放地址…",true,true);
  IO.execute(()->{try{FusionCatalog.Session session=FusionCatalog.session(a,config,site);JSONObject rec=fusionRecord(a,session,flag,value,name,watch,null,System.currentTimeMillis());
    a.runOnUiThread(()->{wait.dismiss();if(a.isFinishing())return;enqueueRecord(a,rec);});
   }catch(Exception e){String why=e instanceof IllegalStateException&&e.getMessage()!=null?e.getMessage():e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();a.runOnUiThread(()->{wait.dismiss();Toast.makeText(a,"无法下载："+why,Toast.LENGTH_LONG).show();});}});}
 static void fusionNow(Activity a,FusionCatalog.Session session,String flag,String value,String name,String watch,JSONObject vod){CinemaWait wait=CinemaWait.show(a,"准备下载","正在解析播放地址…",true,true);
  IO.execute(()->{try{JSONObject rec=fusionRecord(a,session,flag,value,name,watch,vod,System.currentTimeMillis());
    a.runOnUiThread(()->{wait.dismiss();if(a.isFinishing())return;enqueueRecord(a,rec);});
   }catch(Exception e){String why=e instanceof IllegalStateException&&e.getMessage()!=null?e.getMessage():e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();a.runOnUiThread(()->{wait.dismiss();Toast.makeText(a,"无法下载："+why,Toast.LENGTH_LONG).show();});}});}
 static JSONObject fusionRecord(Context c,FusionCatalog.Session session,String flag,String value,String name,String watch,JSONObject vod,long now)throws Exception{Object[] got=fusionUrl(c,session,flag,value);String url=(String)got[0];JSONObject h=(JSONObject)got[1];
  String pic=vod==null?"":vod.optString("vod_pic");JSONObject m=new JSONObject().put("id",watch).put("name",name).put("poster",pic).put("type","movie");
  JSONObject src=new JSONObject().put("kind","fusion").put("config",session.url).put("site",session.site.optString("key")).put("flag",flag).put("value",value);
  return build(c,url,h,name,watch,OfflineRules.extension("",url),m,src,now);}
 static Object[] fusionUrl(Context c,FusionCatalog.Session session,String flag,String value)throws Exception{JSONObject stream=FusionPlaybackSources.resolve(c,session,flag,value);String url=stream.optString("url");String refuse=OfflineRules.refuse(url);if(refuse!=null)throw new IllegalStateException(refuse);
  JSONObject hints=stream.optJSONObject("behaviorHints");JSONObject proxy=hints==null?null:hints.optJSONObject("proxyHeaders");JSONObject h=proxy==null?null:proxy.optJSONObject("request");return new Object[]{url,h==null?new JSONObject():h};}
 /** 下载中链接失效（401/403/404/410）时按来源重新取一次地址。返回 {url, headers}。 */
 static Object[] refresh(Context c,JSONObject rec)throws Exception{JSONObject src=rec.optJSONObject("src");if(src==null)throw new IllegalStateException("没有可刷新的来源");
  if("emby".equals(src.optString("kind"))){JSONObject server=EmbyStore.get(c,src.optString("server"));if(server==null)throw new IllegalStateException("Emby 服务器已删除");return new Object[]{embyUrl(c,server,src.optString("item"))[0],new JSONObject()};}
  if("fusion".equals(src.optString("kind"))){FusionCatalog.Session s=FusionCatalog.session(c,src.optString("config"),src.optString("site"));return fusionUrl(c,s,src.optString("flag"),src.optString("value"));}
  throw new IllegalStateException("没有可刷新的来源");}
 static void confirm(Activity a,String name,Runnable go){new CinemaDialog(a).setTitle("下载到本机").setMessage(name+"\n\n随影在后台下载（通知栏显示进度），断网或链接过期会自动续传，完成后在「我的 → 离线下载」里播放。大文件建议连 Wi-Fi。").setPositiveButton("下载",(d,w)->go.run()).setNegativeButton("取消",null).show();}

 /** 0.1.112：不再交给系统下载管理器（它报的身份和播放器不同，部分服务器拒绝），改由随影自己下载，请求头和播放时一致。 */
 static void enqueue(Activity a,String url,JSONObject headers,String name,String watch,String ext,JSONObject meta,JSONObject src){
  try{String refuse=OfflineRules.refuse(url);if(refuse!=null){Toast.makeText(a,refuse,Toast.LENGTH_LONG).show();return;}enqueueRecord(a,build(a,url,headers,name,watch,ext,meta,src,System.currentTimeMillis()));}
  catch(Exception e){Toast.makeText(a,"下载失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()),Toast.LENGTH_LONG).show();}
 }
 static void enqueueRecord(Activity a,JSONObject rec){
  try{String refuse=OfflineRules.refuse(rec.optString("url"));if(refuse!=null){Toast.makeText(a,refuse,Toast.LENGTH_LONG).show();return;}
   String dup;synchronized(OfflineDownloads.class){dup=OfflineQueueRules.duplicate(OfflineQueueRules.index(list(a)).get(OfflineQueueRules.keyOf(rec)));if(dup==null)save(a,OfflineQueueRules.prependFifo(list(a),Collections.singletonList(rec)));}
   if(dup!=null){Toast.makeText(a,dup,Toast.LENGTH_LONG).show();return;}
   FollowUp.askDownloadNotify(a);OfflineService.start(a);Toast.makeText(a,"已开始下载，可在通知栏或「我的 → 离线下载」查看",Toast.LENGTH_LONG).show();
  }catch(Exception e){Toast.makeText(a,"下载失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()),Toast.LENGTH_LONG).show();}
 }
 static JSONObject build(Context c,String url,JSONObject headers,String name,String watch,String ext,JSONObject meta,JSONObject src,long now)throws Exception{
  now=unique(now);File dir=c.getExternalFilesDir(Environment.DIRECTORY_MOVIES);if(dir==null)dir=new File(c.getFilesDir(),"offline");if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("无法创建下载目录");
  File dest=new File(dir,OfflineRules.fileName(now,ext));
  return new JSONObject().put("id",String.valueOf(now)).put("name",name==null?"离线影片":name).put("watch",watch==null?"":watch).put("file",dest.getAbsolutePath()).put("created",now).put("meta",meta==null?new JSONObject():meta).put("url",url).put("headers",headers==null?new JSONObject():headers).put("src",src==null?new JSONObject():src).put("status","queued").put("done",0).put("total",-1);
 }
 /** 已经在队列或已下完时返回原因，否则 null。失败的记录不挡。 */
 static String blocked(Context c,JSONObject src,String url){try{JSONObject probe=new JSONObject();if(src!=null)probe.put("src",src);if(url!=null)probe.put("url",url);synchronized(OfflineDownloads.class){return OfflineQueueRules.duplicate(OfflineQueueRules.index(list(c)).get(OfflineQueueRules.keyOf(probe)));}}catch(Exception e){return null;}}
 /** 按顺序写入一整批。返回实际入队数；写入失败返回 -1。调用方在下载线程上用，不弹逐集提示。 */
 static int commitAll(Context c,List<JSONObject> ordered){
  if(ordered==null||ordered.isEmpty())return 0;
  int n;synchronized(OfflineDownloads.class){try{JSONArray all=list(c);Map<String,String> idx=OfflineQueueRules.index(all);ArrayList<JSONObject> keep=new ArrayList<>();for(JSONObject rec:ordered){if(rec==null)continue;String key=OfflineQueueRules.keyOf(rec);if(OfflineQueueRules.duplicate(idx.get(key))!=null)continue;keep.add(rec);idx.put(key,"queued");}n=keep.size();if(n==0)return 0;save(c,OfflineQueueRules.prependFifo(all,keep));}catch(Exception e){return -1;}}
  OfflineService.start(c);return n;
 }
 static String key(JSONObject rec){return rec.has("id")?rec.optString("id"):"dm:"+rec.optLong("dm");}
 static JSONObject find(Context c,String id){JSONArray all=list(c);for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o!=null&&key(o).equals(id))return o;}return null;}
 /** 改一条记录的若干字段（读-改-写，串行）。记录已被删除时返回 false。 */
 static synchronized boolean update(Context c,String id,JSONObject fields){try{JSONArray all=list(c);boolean hit=false;for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o==null||!key(o).equals(id))continue;for(Iterator<String> it=fields.keys();it.hasNext();){String k=it.next();o.put(k,fields.get(k));}hit=true;}if(hit)save(c,all);return hit;}catch(Exception e){return false;}}
 /** 下一个排队的下载。 */
 static synchronized JSONObject nextQueued(Context c){JSONArray all=list(c);for(int i=all.length()-1;i>=0;i--){JSONObject o=all.optJSONObject(i);if(o!=null&&"queued".equals(o.optString("status")))return o;}return null;}
 /** 进程重启后，上次停在「下载中」的记录改回排队。调用时没有正在进行的下载。 */
 static synchronized void reclaimRunning(Context c){try{JSONArray all=list(c);boolean changed=false;for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o!=null&&"running".equals(o.optString("status"))){o.put("status","queued");changed=true;}}if(changed)save(c,all);}catch(Exception ignored){}}
 /** App 被杀时停在「下载中」的记录改回排队；有排队的就启动下载服务。 */
 static void resumeStale(Context c){boolean any=false;synchronized(OfflineDownloads.class){JSONArray all=list(c);for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o==null)continue;String st=o.optString("status");if("running".equals(st)&&!OfflineService.running){try{o.put("status","queued");}catch(Exception ignored){}st="queued";}if("queued".equals(st))any=true;}save(c,all);}if(any)OfflineService.start(c);}
 static void pause(Context c,JSONObject rec){try{update(c,key(rec),new JSONObject().put("status","paused"));}catch(Exception ignored){}}
 static void resume(Context c,JSONObject rec){try{update(c,key(rec),new JSONObject().put("status","queued").put("error",""));OfflineService.start(c);}catch(Exception ignored){}}

 /** 进度行和能否播放。旧版（系统下载管理器）的记录照旧查询。 */
 static Object[] describe(Context c,JSONObject rec){
  if(!rec.has("status")){long[] st=legacyStatus(c,rec);return new Object[]{OfflineRules.line((int)st[0],st[1],st[2],(int)st[3]),st[0]==8};}
  String st=rec.optString("status");long done=rec.optLong("done"),total=rec.optLong("total"),speed=rec.optLong("speed");if(!"done".equals(st)){long[] live=live(c,key(rec));if(live!=null){done=live[0];if(live[1]>0)total=live[1];speed=live[2];}}File f=new File(rec.optString("file"));if(f.isFile())done=Math.max(done,"done".equals(st)?f.length():Math.min(f.length(),total>0?total:Long.MAX_VALUE));
  return new Object[]{OfflineRules.state(st,done,total,speed,rec.optString("error")),"done".equals(st)&&f.isFile()};}
 static long[] legacyStatus(Context c,JSONObject rec){long[] out={0,0,0,0};try{DownloadManager dm=(DownloadManager)c.getSystemService(Context.DOWNLOAD_SERVICE);try(Cursor cur=dm.query(new DownloadManager.Query().setFilterById(rec.optLong("dm")))){if(cur!=null&&cur.moveToFirst()){out[0]=cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));out[1]=cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));out[2]=cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));out[3]=cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));return out;}}}catch(Exception ignored){}
  File f=new File(rec.optString("file"));if(f.isFile()&&f.length()>0){out[0]=8;out[1]=out[2]=f.length();}else out[0]=16;return out;}
 static void remove(Context c,JSONObject rec){if(rec.has("dm"))try{DownloadManager dm=(DownloadManager)c.getSystemService(Context.DOWNLOAD_SERVICE);dm.remove(rec.optLong("dm"));}catch(Exception ignored){}
  String id=key(rec);synchronized(OfflineDownloads.class){JSONArray all=list(c),next=new JSONArray();for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o!=null&&!key(o).equals(id))next.put(o);}save(c,next);}forgetProgress(c,id);
  try{File f=new File(rec.optString("file"));if(f.exists())f.delete();}catch(Exception ignored){}}
 /** 用原来的播放器打开本机文件；观看进度和在线播放共用同一个编号。 */
 static void play(Activity a,JSONObject rec){try{File f=new File(rec.optString("file"));if(!f.isFile()){Toast.makeText(a,"文件不在了，可能已被系统清理",Toast.LENGTH_LONG).show();return;}
  String name=rec.optString("name","离线影片"),watch=rec.optString("watch");if(watch.isEmpty())watch="offline:"+rec.optLong("dm");
  JSONObject stream=new JSONObject().put("url",Uri.fromFile(f).toString()).put("name","离线").put("title","本机文件 · "+OfflineRules.size(f.length())).put("subtitles",new JSONArray());
  JSONObject meta=rec.optJSONObject("meta")==null?new JSONObject():new JSONObject(rec.optJSONObject("meta").toString());meta.put("id",watch).put("name",name);meta.remove("videos");meta.put("type","movie");
  Intent in=new Intent(a,PlayerActivity.class);in.putExtra("stream",stream.toString());in.putExtra("sources",new JSONArray().put(stream).toString());in.putExtra("id",watch);in.putExtra("type","movie");in.putExtra("meta",meta.toString());in.putExtra("name",name);in.putExtra("addons",prefs(a).getString("addons","[]"));
  a.startActivity(in);}catch(Exception e){Toast.makeText(a,"无法播放："+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}}
 static String label(Context c){int n=list(c).length();return "离线下载"+(n>0?" · "+n+" 个":"");}
}
