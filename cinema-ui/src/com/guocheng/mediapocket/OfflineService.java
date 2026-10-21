package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.content.pm.ServiceInfo;import android.os.*;import org.json.*;import java.io.*;import java.net.*;import java.util.*;
/**
 * 0.1.112 随影自己的离线下载（前台服务，通知栏显示进度）。
 * 0.1.120：Android 15 的 dataSync 超时会暂停当前任务并停止服务，避免系统因未实现 onTimeout 而崩溃。
 * 进度写入独立存储并节流；进程被杀后用 START_STICKY 拉起，入队与线程退出用同一把锁衔接。
 */
public final class OfflineService extends Service {
 static final String CHANNEL="offline";static final int NOTE=73301;
 static volatile boolean running;
 static volatile Thread worker;
 static volatile boolean giveUp;
 static volatile int lastStartId;
 static volatile String activeId;
 static volatile HttpURLConnection currentConn;
 NotificationManager nm;long lastNote;

 static void start(Context c){try{Intent i=new Intent(c,OfflineService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}catch(Exception e){DebugLog.event("离线下载","无法启动下载服务 · "+e.getClass().getSimpleName());}}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26&&nm!=null){NotificationChannel ch=new NotificationChannel(CHANNEL,"离线下载",NotificationManager.IMPORTANCE_LOW);ch.setShowBadge(false);nm.createNotificationChannel(ch);}
  Notification n=note("准备下载…",-1,-1);boolean foreground=false;
  try{if(Build.VERSION.SDK_INT>=29)startForeground(NOTE,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);else startForeground(NOTE,n);foreground=true;}catch(Exception e){DebugLog.event("离线下载","前台服务被拒绝 · "+e.getClass().getSimpleName());}
  synchronized(OfflineDownloads.class){lastStartId=startId;if(!foreground){if(!running)stopSelf(startId);return START_NOT_STICKY;}giveUp=false;if(!running){running=true;Thread t=new Thread(this::loop,"offline-download");worker=t;t.start();}}
  return START_STICKY;}
 @Override public void onTimeout(int startId){timeout(startId);}
 @Override public void onTimeout(int startId,int fgsType){timeout(startId);}
 /** 系统限时到了：暂停正在下的那一条，不再续上新的前台任务。 */
 void timeout(int startId){String id;int stopId;Thread t;HttpURLConnection conn;synchronized(OfflineDownloads.class){giveUp=true;running=false;t=worker;worker=null;id=activeId;stopId=lastStartId>0?lastStartId:startId;conn=currentConn;currentConn=null;}
  if(id!=null)try{OfflineDownloads.update(this,id,new JSONObject().put("status","paused").put("speed",0).put("error","系统限制了后台下载时长，已暂停"));OfflineDownloads.forgetProgress(this,id);}catch(Exception ignored){}
  if(conn!=null)try{conn.disconnect();}catch(Exception ignored){}
  if(t!=null)t.interrupt();try{stopForeground(true);}catch(Exception ignored){}stopSelf(stopId);DebugLog.event("离线下载","系统限制了后台下载时长，已暂停");}

 Notification note(String text,long done,long total){
  Intent open=new Intent(this,DownloadsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);PendingIntent pi=PendingIntent.getActivity(this,NOTE,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
  b.setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("随影离线下载").setContentText(text).setOngoing(true).setOnlyAlertOnce(true).setContentIntent(pi);
  if(total>0)b.setProgress(1000,(int)Math.min(1000,done*1000/total),false);else if(done>=0)b.setProgress(0,0,true);
  return b.build();}
 void show(String text,long done,long total){long now=SystemClock.elapsedRealtime();if(now-lastNote<1000&&done>=0)return;lastNote=now;try{if(nm!=null)nm.notify(NOTE,note(text,done,total));}catch(Exception ignored){}}

 void loop(){Thread me=Thread.currentThread();try{OfflineDownloads.reclaimRunning(this);while(!giveUp){JSONObject rec;synchronized(OfflineDownloads.class){if(giveUp)break;rec=OfflineDownloads.nextQueued(this);if(rec==null)break;}download(rec);}}catch(Throwable t){DebugLog.event("离线下载","下载线程异常 · "+t.getClass().getSimpleName());}
  finally{int stopId=-1;boolean stop=false;synchronized(OfflineDownloads.class){if(worker==me){if(!giveUp&&OfflineDownloads.nextQueued(this)!=null){Thread t=new Thread(this::loop,"offline-download");worker=t;running=true;t.start();}else{running=false;worker=null;stop=true;stopId=lastStartId;}}}
   if(stop){try{stopForeground(true);}catch(Exception ignored){}if(stopId>0)stopSelf(stopId);else stopSelf();}}}

 /** 记录被暂停或删除时返回 true。 */
 boolean stopped(String id){JSONObject r=OfflineDownloads.find(this,id);return r==null||!"running".equals(r.optString("status"));}
 void set(String id,Object... kv){try{JSONObject f=new JSONObject();for(int i=0;i+1<kv.length;i+=2)f.put((String)kv[i],kv[i+1]);OfflineDownloads.update(this,id,f);}catch(Exception ignored){}}

 void download(JSONObject rec){String id=OfflineDownloads.key(rec),name=rec.optString("name","离线影片");File file=new File(rec.optString("file"));
  String url=rec.optString("url");JSONObject headers=rec.optJSONObject("headers");if(headers==null)headers=new JSONObject();
  activeId=id;set(id,"status","running","error","");boolean refreshed=false;int attempt=0;long total=rec.optLong("total",-1);
  try{while(true){
   if(giveUp)return;if(stopped(id))return;
   long done=file.isFile()?file.length():0;HttpURLConnection c=null;
   try{c=OfflineHttp.open(url,headers,done);currentConn=c;int code=c.getResponseCode();String kind=OfflineRules.classify(code);
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
     byte[] buf=new byte[256*1024];int n;while((n=in.read(buf))>0){if(giveUp){out.getFD().sync();return;}out.write(buf,0,n);done+=n;windowBytes+=n;attempt=0;refreshed=false;
      long now=SystemClock.elapsedRealtime();if(now-lastSave>=1000){lastSave=now;long speed=now>windowStart?windowBytes*1000/(now-windowStart):0;if(now-windowStart>5000){windowStart=now;windowBytes=0;}
       OfflineDownloads.progress(this,id,done,total,speed);show(name+(total>0?" · "+OfflineRules.percent(done,total):"")+(speed>0?" · "+OfflineRules.size(speed)+"/s":""),done,total);
       if(stopped(id)){out.getFD().sync();set(id,"done",done,"speed",0);OfflineDownloads.forgetProgress(this,id);show(name+" · 已暂停",-1,-1);return;}}}
     out.getFD().sync();}
    if(giveUp)return;
    if(total>0&&done<total)throw new IOException("连接中断（"+OfflineRules.size(done)+" / "+OfflineRules.size(total)+"）");
    finish(id,file,done);return;
   }catch(IllegalStateException e){if(giveUp||stopped(id))return;fail(id,name,url,e.getMessage());return;}
   catch(Exception e){if(giveUp||stopped(id))return;attempt++;String why=e instanceof SocketTimeoutException?"连接超时":e instanceof UnknownHostException?"域名解析失败":e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
    if(attempt>6){fail(id,name,url,why);return;}DebugLog.event("离线下载","第 "+attempt+" 次重试 · "+why+" · "+LinkProbe.host(url));set(id,"error",why+"，"+(OfflineRules.backoff(attempt-1)/1000)+" 秒后重试","speed",0);show(name+" · "+why+"，稍后重试",-1,-1);
    try{Thread.sleep(OfflineRules.backoff(attempt-1));}catch(InterruptedException ie){return;}}
   finally{if(currentConn==c)currentConn=null;if(c!=null)c.disconnect();}}
  }finally{if(id.equals(activeId))activeId=null;}}
 void finish(String id,File file,long done){set(id,"status","done","done",file.length(),"total",file.length(),"speed",0,"error","");OfflineDownloads.forgetProgress(this,id);DebugLog.event("离线下载","完成 · "+OfflineRules.size(file.length()));done(OfflineDownloads.find(this,id));}
 void fail(String id,String name,String url,String why){set(id,"status","failed","error",why==null?"未知错误":why,"speed",0);OfflineDownloads.forgetProgress(this,id);DebugLog.event("离线下载","失败 · "+why+" · "+LinkProbe.host(url));finished(name+" 下载失败",why);}
 void done(JSONObject rec){finished((rec==null?"影片":rec.optString("name"))+" 已下载","可以在「我的 → 离线下载」里离线播放");}
 void finished(String title,String text){try{if(nm==null)return;Intent open=new Intent(this,DownloadsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);PendingIntent pi=PendingIntent.getActivity(this,NOTE+1,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);b.setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle(title).setContentText(text).setAutoCancel(true).setContentIntent(pi);nm.notify(NOTE+2+(int)(System.currentTimeMillis()%1000),b.build());}catch(Exception ignored){}}
}
