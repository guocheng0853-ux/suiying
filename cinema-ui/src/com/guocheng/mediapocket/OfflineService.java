package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.content.pm.ServiceInfo;import android.os.*;import org.json.*;import java.io.*;import java.net.*;import java.util.*;
/**
 * 0.1.112 随影自己的离线下载（前台服务，通知栏显示进度）。
 * - 请求头和播放时一致：默认 User-Agent 与播放器相同，片源要求的 Referer / Cookie 等原样带上；
 * - 断点续传（Range）；断网、超时、5xx 自动重试，最多 6 次，间隔 2–30 秒；
 * - 401/403/404/410 视为链接过期，按来源（Emby / 影视仓线路）重新取一次地址再接着下；
 * - 开头拿到网页而不是视频时直接报错；失败原因写进记录和「调试信息」（只记主机名）。
 * 一次只下一个，排队的按加入顺序下载。
 */
public final class OfflineService extends Service {
 static final String CHANNEL="offline";static final int NOTE=73301;
 static volatile boolean running;
 NotificationManager nm;long lastNote;

 static void start(Context c){try{Intent i=new Intent(c,OfflineService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}catch(Exception e){DebugLog.event("离线下载","无法启动下载服务 · "+e.getClass().getSimpleName());}}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26&&nm!=null){NotificationChannel ch=new NotificationChannel(CHANNEL,"离线下载",NotificationManager.IMPORTANCE_LOW);ch.setShowBadge(false);nm.createNotificationChannel(ch);}
  Notification n=note("准备下载…",-1,-1);
  try{if(Build.VERSION.SDK_INT>=29)startForeground(NOTE,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);else startForeground(NOTE,n);}catch(Exception e){DebugLog.event("离线下载","前台服务被拒绝 · "+e.getClass().getSimpleName());}
  synchronized(OfflineService.class){if(!running){running=true;new Thread(this::loop,"offline-download").start();}}
  return START_NOT_STICKY;}

 Notification note(String text,long done,long total){
  Intent open=new Intent(this,DownloadsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);PendingIntent pi=PendingIntent.getActivity(this,NOTE,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
  b.setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("随影离线下载").setContentText(text).setOngoing(true).setOnlyAlertOnce(true).setContentIntent(pi);
  if(total>0)b.setProgress(1000,(int)Math.min(1000,done*1000/total),false);else if(done>=0)b.setProgress(0,0,true);
  return b.build();}
 void show(String text,long done,long total){long now=SystemClock.elapsedRealtime();if(now-lastNote<1000&&done>=0)return;lastNote=now;try{if(nm!=null)nm.notify(NOTE,note(text,done,total));}catch(Exception ignored){}}

 void loop(){try{while(true){JSONObject rec=OfflineDownloads.nextQueued(this);if(rec==null)break;download(rec);}}catch(Throwable t){DebugLog.event("离线下载","下载线程异常 · "+t.getClass().getSimpleName());}
  finally{running=false;try{stopForeground(true);}catch(Exception ignored){}stopSelf();}}

 static final class Stop extends Exception{Stop(){super("stop");}}
 /** 记录被暂停或删除时返回 true。 */
 boolean stopped(String id){JSONObject r=OfflineDownloads.find(this,id);return r==null||!"running".equals(r.optString("status"));}
 void set(String id,Object... kv){try{JSONObject f=new JSONObject();for(int i=0;i+1<kv.length;i+=2)f.put((String)kv[i],kv[i+1]);OfflineDownloads.update(this,id,f);}catch(Exception ignored){}}

 void download(JSONObject rec){String id=OfflineDownloads.key(rec),name=rec.optString("name","离线影片");File file=new File(rec.optString("file"));
  String url=rec.optString("url");JSONObject headers=rec.optJSONObject("headers");if(headers==null)headers=new JSONObject();
  set(id,"status","running","error","");boolean refreshed=false;int attempt=0;long total=rec.optLong("total",-1);
  while(true){
   if(stopped(id))return;
   long done=file.isFile()?file.length():0;HttpURLConnection c=null;
   try{c=OfflineHttp.open(url,headers,done);int code=c.getResponseCode();String kind=OfflineRules.classify(code);
    if(code==416&&done>0){if(total>0&&done>=total){finish(id,file,done);return;}file.delete();continue;}
    if(kind.equals("refresh")){if(refreshed)throw new IllegalStateException("HTTP "+code+"，重新获取地址后仍被拒绝");refreshed=true;DebugLog.event("离线下载","HTTP "+code+"，重新获取地址 · "+LinkProbe.host(url));Object[] fresh=OfflineDownloads.refresh(this,rec);url=(String)fresh[0];headers=(JSONObject)fresh[1];set(id,"url",url,"headers",headers);continue;}
    if(kind.equals("retry"))throw new IOException("HTTP "+code);
    if(kind.equals("fail"))throw new IllegalStateException("HTTP "+code);
    if(OfflineRules.looksLikePage(c.getContentType(),done))throw new IllegalStateException("服务器返回的是网页而不是视频");
    boolean append=code==206&&done>0;if(!append)done=0;
    long length=c.getContentLengthLong();long whole=code==206?OfflineRules.totalFromContentRange(c.getHeaderField("Content-Range")):length;if(whole<=0&&length>0)whole=done+length;if(whole>0){total=whole;set(id,"total",total);}
    File parent=file.getParentFile();if(parent!=null&&!parent.exists())parent.mkdirs();
    if(parent!=null&&total>0&&parent.getUsableSpace()<total-done+64L*1024*1024)throw new IllegalStateException("存储空间不足（还需 "+OfflineRules.size(total-done)+"）");
    long windowStart=SystemClock.elapsedRealtime(),windowBytes=0,lastSave=0;
    try(InputStream in=new BufferedInputStream(c.getInputStream(),256*1024);FileOutputStream out=new FileOutputStream(file,append)){
     byte[] buf=new byte[256*1024];int n;while((n=in.read(buf))>0){out.write(buf,0,n);done+=n;windowBytes+=n;attempt=0;
      long now=SystemClock.elapsedRealtime();if(now-lastSave>=1000){lastSave=now;long speed=now>windowStart?windowBytes*1000/(now-windowStart):0;if(now-windowStart>5000){windowStart=now;windowBytes=0;}
       set(id,"done",done,"speed",speed);show(name+(total>0?" · "+OfflineRules.percent(done,total):"")+(speed>0?" · "+OfflineRules.size(speed)+"/s":""),done,total);
       if(stopped(id)){out.getFD().sync();set(id,"done",done,"speed",0);show(name+" · 已暂停",-1,-1);return;}}}
     out.getFD().sync();}
    if(total>0&&done<total)throw new IOException("连接中断（"+OfflineRules.size(done)+" / "+OfflineRules.size(total)+"）");
    finish(id,file,done);return;
   }catch(IllegalStateException e){fail(id,name,url,e.getMessage());return;}
   catch(Exception e){attempt++;String why=e instanceof SocketTimeoutException?"连接超时":e instanceof UnknownHostException?"域名解析失败":e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
    if(attempt>6){fail(id,name,url,why);return;}DebugLog.event("离线下载","第 "+attempt+" 次重试 · "+why+" · "+LinkProbe.host(url));set(id,"error",why+"，"+(OfflineRules.backoff(attempt-1)/1000)+" 秒后重试","speed",0);show(name+" · "+why+"，稍后重试",-1,-1);
    try{Thread.sleep(OfflineRules.backoff(attempt-1));}catch(InterruptedException ie){return;}}
   finally{if(c!=null)c.disconnect();}}
 }
 void finish(String id,File file,long done){set(id,"status","done","done",file.length(),"total",file.length(),"speed",0,"error","");DebugLog.event("离线下载","完成 · "+OfflineRules.size(file.length()));done(OfflineDownloads.find(this,id));}
 void fail(String id,String name,String url,String why){set(id,"status","failed","error",why==null?"未知错误":why,"speed",0);DebugLog.event("离线下载","失败 · "+why+" · "+LinkProbe.host(url));finished(name+" 下载失败",why);}
 void done(JSONObject rec){finished((rec==null?"影片":rec.optString("name"))+" 已下载","可以在「我的 → 离线下载」里离线播放");}
 void finished(String title,String text){try{if(nm==null)return;Intent open=new Intent(this,DownloadsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);PendingIntent pi=PendingIntent.getActivity(this,NOTE+1,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);b.setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle(title).setContentText(text).setAutoCancel(true).setContentIntent(pi);nm.notify(NOTE+2+(int)(System.currentTimeMillis()%1000),b.build());}catch(Exception ignored){}}

}
