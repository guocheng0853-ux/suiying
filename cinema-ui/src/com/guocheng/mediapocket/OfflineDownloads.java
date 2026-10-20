package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.Environment;import android.widget.Toast;import org.json.*;import java.io.File;import java.util.*;import java.util.concurrent.*;
/**
 * 0.1.106 离线下载：用系统下载管理器把直链片源存到本应用的影片目录（不需要存储权限，卸载时一起删除）。
 * 入口：Emby 电影 / 单集详情右上角「⬇ 下载」，Emby 和影视仓的选集长按。磁力、m3u8 分段流不支持。
 * 记录在 offline.v1：[{dm 下载编号, name 片名, watch 观看记录编号, file 文件路径, created, meta}]。
 */
final class OfflineDownloads {
 private OfflineDownloads(){}
 static final String KEY="offline.v1";
 static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"offline");t.setDaemon(true);return t;});
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("data",0);}
 static JSONArray list(Context c){try{return new JSONArray(prefs(c).getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}
 static synchronized void save(Context c,JSONArray a){prefs(c).edit().putString(KEY,a.toString()).apply();}

 /** Emby：取直接串流地址（服务器原文件）。只能转码播放的片源不下载。 */
 static void emby(Activity a,JSONObject server,String itemId,String name,JSONObject meta){
  confirm(a,name,()->{CinemaWait wait=CinemaWait.show(a,"准备下载","正在向 Emby 获取下载地址…",true,true);
   IO.execute(()->{try{JSONObject info=EmbyClient.playbackInfo(a,server,itemId,0);EmbyApi.Choice choice=EmbyApi.select(server.optString("url"),itemId,server.optString("token"),info,null,EmbyApi.clientOf(server));
     if(!choice.direct)throw new IllegalStateException("这个片源服务器只允许转码播放，无法下载原文件");
     String container="";JSONArray sources=info.optJSONArray("MediaSources");for(int i=0;sources!=null&&i<sources.length();i++){JSONObject s=sources.optJSONObject(i);if(s!=null&&choice.mediaSourceId.equals(s.optString("Id")))container=s.optString("Container");}
     JSONObject m=meta==null?new JSONObject():new JSONObject(meta.toString());String watch=EmbyApi.watchId(server.optString("id"),itemId);m.put("id",watch);if(m.optString("name").isEmpty())m.put("name",name);
     String ext=OfflineRules.extension(container,choice.url);a.runOnUiThread(()->{wait.dismiss();enqueue(a,choice.url,new JSONObject(),name,watch,ext,m);});
    }catch(Exception e){a.runOnUiThread(()->{wait.dismiss();Toast.makeText(a,"无法下载："+EmbyApi.explain(e),Toast.LENGTH_LONG).show();});}});});
 }
 /** 影视仓：先按线路解析出真实地址，再判断能不能下载。 */
 static void fusion(Activity a,FusionCatalog.Session session,String flag,String value,String name,String watch,JSONObject vod){
  confirm(a,name,()->{CinemaWait wait=CinemaWait.show(a,"准备下载","正在解析播放地址…",true,true);
   IO.execute(()->{try{JSONObject stream=FusionPlaybackSources.resolve(a,session,flag,value);String url=stream.optString("url");String refuse=OfflineRules.refuse(url);if(refuse!=null)throw new IllegalStateException(refuse);
     JSONObject headers=stream.optJSONObject("behaviorHints")==null?null:stream.optJSONObject("behaviorHints").optJSONObject("proxyHeaders");headers=headers==null?new JSONObject():headers.optJSONObject("request");if(headers==null)headers=new JSONObject();
     String pic=vod==null?"":vod.optString("vod_pic");JSONObject m=new JSONObject().put("id",watch).put("name",name).put("poster",pic).put("type","movie");final JSONObject h=headers;
     a.runOnUiThread(()->{wait.dismiss();enqueue(a,url,h,name,watch,OfflineRules.extension("",url),m);});
    }catch(Exception e){String why=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();a.runOnUiThread(()->{wait.dismiss();Toast.makeText(a,"无法下载："+why,Toast.LENGTH_LONG).show();});}});});
 }
 static void confirm(Activity a,String name,Runnable go){new CinemaDialog(a).setTitle("下载到本机").setMessage(name+"\n\n用系统下载管理器在后台下载，完成后在「我的 → 离线下载」里播放。大文件建议连 Wi-Fi。").setPositiveButton("下载",(d,w)->go.run()).setNegativeButton("取消",null).show();}

 static void enqueue(Activity a,String url,JSONObject headers,String name,String watch,String ext,JSONObject meta){
  try{String refuse=OfflineRules.refuse(url);if(refuse!=null){Toast.makeText(a,refuse,Toast.LENGTH_LONG).show();return;}
   DownloadManager dm=(DownloadManager)a.getSystemService(Context.DOWNLOAD_SERVICE);long now=System.currentTimeMillis();String file=OfflineRules.fileName(now,ext);
   DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url));for(Iterator<String> it=headers.keys();it.hasNext();){String k=it.next();String v=headers.optString(k);if(!k.isEmpty()&&!v.isEmpty())try{r.addRequestHeader(k,v);}catch(Exception ignored){}}
   r.setTitle(name).setDescription("随影离线下载").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationInExternalFilesDir(a,Environment.DIRECTORY_MOVIES,file).setAllowedOverMetered(true).setAllowedOverRoaming(false);
   long id=dm.enqueue(r);File dest=new File(a.getExternalFilesDir(Environment.DIRECTORY_MOVIES),file);
   JSONArray all=list(a);JSONArray next=new JSONArray().put(new JSONObject().put("dm",id).put("name",name).put("watch",watch).put("file",dest.getAbsolutePath()).put("created",now).put("meta",meta));for(int i=0;i<all.length();i++)next.put(all.opt(i));save(a,next);
   Toast.makeText(a,"已开始下载，可在「我的 → 离线下载」查看",Toast.LENGTH_LONG).show();
  }catch(Exception e){Toast.makeText(a,"下载失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()),Toast.LENGTH_LONG).show();}
 }
 /** {status, done, total, reason}；查不到（被系统清掉）时看文件在不在。 */
 static long[] status(Context c,JSONObject rec){long[] out={0,0,0,0};try{DownloadManager dm=(DownloadManager)c.getSystemService(Context.DOWNLOAD_SERVICE);try(Cursor cur=dm.query(new DownloadManager.Query().setFilterById(rec.optLong("dm")))){if(cur!=null&&cur.moveToFirst()){out[0]=cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));out[1]=cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));out[2]=cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));out[3]=cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));return out;}}}catch(Exception ignored){}
  File f=new File(rec.optString("file"));if(f.isFile()&&f.length()>0){out[0]=8;out[1]=out[2]=f.length();}else out[0]=16;return out;}
 static void remove(Context c,JSONObject rec){try{DownloadManager dm=(DownloadManager)c.getSystemService(Context.DOWNLOAD_SERVICE);dm.remove(rec.optLong("dm"));}catch(Exception ignored){}try{File f=new File(rec.optString("file"));if(f.exists())f.delete();}catch(Exception ignored){}
  JSONArray all=list(c),next=new JSONArray();for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o!=null&&o.optLong("dm")!=rec.optLong("dm"))next.put(o);}save(c,next);}
 /** 用原来的播放器打开本机文件；观看进度和在线播放共用同一个编号。 */
 static void play(Activity a,JSONObject rec){try{File f=new File(rec.optString("file"));if(!f.isFile()){Toast.makeText(a,"文件不在了，可能已被系统清理",Toast.LENGTH_LONG).show();return;}
  String name=rec.optString("name","离线影片"),watch=rec.optString("watch");if(watch.isEmpty())watch="offline:"+rec.optLong("dm");
  JSONObject stream=new JSONObject().put("url",Uri.fromFile(f).toString()).put("name","离线").put("title","本机文件 · "+OfflineRules.size(f.length())).put("subtitles",new JSONArray());
  JSONObject meta=rec.optJSONObject("meta")==null?new JSONObject():new JSONObject(rec.optJSONObject("meta").toString());meta.put("id",watch).put("name",name);meta.remove("videos");meta.put("type","movie");
  Intent in=new Intent(a,PlayerActivity.class);in.putExtra("stream",stream.toString());in.putExtra("sources",new JSONArray().put(stream).toString());in.putExtra("id",watch);in.putExtra("type","movie");in.putExtra("meta",meta.toString());in.putExtra("name",name);in.putExtra("addons",prefs(a).getString("addons","[]"));
  a.startActivity(in);}catch(Exception e){Toast.makeText(a,"无法播放："+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}}
 static String label(Context c){int n=list(c).length();return "离线下载"+(n>0?" · "+n+" 个":"");}
}
