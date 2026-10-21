package com.guocheng.mediapocket;
import android.app.*;import android.app.job.*;import android.content.*;import android.os.Build;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;import java.util.concurrent.*;
/**
 * 0.1.106 追剧更新提醒。
 * 收藏里的 Emby 剧集和影视仓条目，每 6 小时（联网时）在后台查一次集数；变多了就在首页顶部「追剧更新」里标出来，
 * 并发一条系统通知。打开该片的详情页就算看过。设置里可以关掉。
 */
final class FollowUp {
 private FollowUp(){}
 static final String STATE="followup.state",ENABLED="followup.enabled",LAST="followup.lastRun",CHANNEL="followup";static final int JOB_ID=73101,NOTE_ID=73102;
 static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"followup");t.setDaemon(true);return t;});
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("data",0);}
 static boolean enabled(Context c){return prefs(c).getBoolean(ENABLED,true);}
 static JSONObject state(Context c){try{return new JSONObject(prefs(c).getString(STATE,"{}"));}catch(Exception e){return new JSONObject();}}
 static synchronized void saveState(Context c,JSONObject s){prefs(c).edit().putString(STATE,s.toString()).apply();}
 static JSONArray favorites(Context c){try{return new JSONArray(prefs(c).getString("favorites","[]"));}catch(Exception e){return new JSONArray();}}

 // ---------- 收藏按钮（Emby / 影视仓详情页右上角） ----------
 static TextView heart(Activity a,JSONObject meta,int ink){TextView t=new TextView(a);t.setTextSize(15);t.setTextColor(ink);t.setGravity(Gravity.CENTER);int p=Math.round(12*a.getResources().getDisplayMetrics().density);t.setPadding(p,0,p,0);t.setTag("mp-fav");
  Runnable paint=()->{boolean on=FollowRules.contains(favorites(a),meta.optString("id"));t.setText(on?"♥ 已收藏":"♡ 收藏");t.setContentDescription(on?"取消收藏":"收藏，有更新时提醒");};paint.run();
  t.setOnClickListener(v->{try{JSONObject entry=FollowRules.favoriteEntry(meta);JSONArray next=FollowRules.toggle(favorites(a),entry);boolean on=FollowRules.contains(next,entry.optString("id"));prefs(a).edit().putString("favorites",next.toString()).apply();paint.run();
    Toast.makeText(a,on?(FollowRules.trackable(entry)&&enabled(a)?"已收藏，有新的剧集会提醒你":"已收藏"):"已取消收藏",Toast.LENGTH_SHORT).show();if(on)askNotify(a);}catch(Exception e){Toast.makeText(a,"收藏失败",Toast.LENGTH_SHORT).show();}});
  return t;}
 /** 详情页打开时调用：集数记为已看过，并收起「有更新」标记和通知。 */
 static void opened(Context c,String id,int count){if(id==null||id.isEmpty())return;synchronized(FollowUp.class){try{JSONObject s=state(c);if(!s.has(id)&&!FollowRules.contains(favorites(c),id))return;FollowRules.seen(s,id,count);saveState(c,s);}catch(Exception ignored){}}}

 /** Emby 剧集详情：只对已收藏 / 已在追的剧在后台数一次集数。 */
 static void openedEmby(Context c,JSONObject server,String seriesId,String id){if(!state(c).has(id)&&!FollowRules.contains(favorites(c),id))return;Context app=c.getApplicationContext();IO.execute(()->{try{int n=EmbyClient.episodesAll(app,server,seriesId,"").length();synchronized(FollowUp.class){opened(app,id,n);}}catch(Exception ignored){}});}
 // ---------- 首页「追剧更新」 ----------
 static void homeSection(ScrapedMainActivity a){if(!enabled(a))return;JSONObject s=state(a);JSONArray fav=favorites(a),row=new JSONArray();
  for(int i=0;i<fav.length();i++){JSONObject f=fav.optJSONObject(i);if(f==null||!FollowRules.hasNew(s,f.optString("id")))continue;try{JSONObject m=new JSONObject(f.toString());JSONObject st=s.optJSONObject(f.optString("id"));String latest=st==null?"":st.optString("latest");m.put("releaseInfo",latest.isEmpty()?"有更新":"更新到 "+latest);row.put(m);}catch(Exception ignored){}}
  if(row.length()==0)return;LinearLayout target=new LinearLayout(a);target.setOrientation(1);a.body.addView(target);target.addView(a.heading("追剧更新 · "+row.length()));LinearLayout shelf=new LinearLayout(a);shelf.setOrientation(1);target.addView(shelf);a.shelf(shelf,row,"series",false);}

 // ---------- 调度 ----------
 /** App 打开时调用：登记后台定时任务；距上次检查超过 3 小时就顺便查一次。 */
 static void start(Context c){try{schedule(c);if(enabled(c)&&FollowRules.due(prefs(c).getLong(LAST,0),System.currentTimeMillis(),3))IO.execute(()->check(c.getApplicationContext()));}catch(Exception ignored){}}
 static void schedule(Context c){try{JobScheduler js=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(js==null)return;if(!enabled(c)){js.cancel(JOB_ID);return;}for(JobInfo j:js.getAllPendingJobs())if(j.getId()==JOB_ID)return;
  js.schedule(new JobInfo.Builder(JOB_ID,new ComponentName(c,FollowUpJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(6*3600_000L).setPersisted(c.checkSelfPermission("android.permission.RECEIVE_BOOT_COMPLETED")==android.content.pm.PackageManager.PERMISSION_GRANTED).build());}catch(Exception ignored){}}
 static void setEnabled(Activity a,boolean on){prefs(a).edit().putBoolean(ENABLED,on).apply();schedule(a);if(on){askNotify(a);IO.execute(()->check(a.getApplicationContext()));}}

 /** 查一遍全部可追更的收藏。网络错误按条跳过。返回发现更新的条数。 */
 static int check(Context c){if(!enabled(c))return 0;prefs(c).edit().putLong(LAST,System.currentTimeMillis()).apply();JSONArray fav=favorites(c);List<String[]> updates=new ArrayList<>();long now=System.currentTimeMillis();
  for(int i=0;i<fav.length()&&i<60;i++){JSONObject f=fav.optJSONObject(i);if(!FollowRules.trackable(f))continue;String id=f.optString("id");
   try{int count;String latest;
    if(f.has("emby.server")){JSONObject server=EmbyStore.get(c,f.optString("emby.server"));if(server==null)continue;JSONArray eps=EmbyClient.episodesAll(c,server,f.optString("emby.item"),"");count=eps.length();JSONObject last=count>0?eps.optJSONObject(count-1):null;latest=last==null?"":EmbyApi.line(last);}
    else{FusionCatalog.Session s=FusionCatalog.session(c,f.optString("fusion.config"),f.optString("fusion.site"));String vod=f.has("fusion.video")?f.optString("fusion.video"):f.optString("id");JSONObject r=FusionCatalog.request(c,s,"detail",vod,"",1);JSONArray list=r.optJSONArray("list");JSONObject m=list==null?null:list.optJSONObject(0);if(m==null)continue;String url=m.optString("vod_play_url");count=FollowRules.fusionCount(url);latest=FollowRules.fusionLatest(url);}
    synchronized(FollowUp.class){JSONObject st=state(c);if(FollowRules.record(st,id,count,latest,now))updates.add(new String[]{f.optString("name","收藏"),latest});saveState(c,st);}
   }catch(Throwable ignored){}}
  if(!updates.isEmpty())notifyUpdates(c,updates);return updates.size();}

 static void notifyUpdates(Context c,List<String[]> updates){try{String[] text=FollowRules.notice(updates);if(text==null)return;NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(nm==null)return;
  if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"追剧更新",NotificationManager.IMPORTANCE_DEFAULT));
  Intent open=new Intent(c,ScrapedMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent pi=PendingIntent.getActivity(c,NOTE_ID,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
  b.setSmallIcon(android.R.drawable.stat_notify_sync_noanim).setContentTitle(text[0]).setContentText(text[1]).setStyle(new Notification.BigTextStyle().bigText(text[1])).setContentIntent(pi).setAutoCancel(true);
  nm.notify(NOTE_ID,b.build());}catch(Exception ignored){}}
 /** Android 13 起发通知要用户同意。追剧和离线下载分开记，拒绝其中一次不影响另一次再问。 */
 static void askNotify(Activity a){askNotify(a,"followup.asked",73103);}
 static void askDownloadNotify(Activity a){askNotify(a,"download.notifyAsked",73104);}
 static void askNotify(Activity a,String flag,int code){try{if(Build.VERSION.SDK_INT<33||a==null)return;if(a.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==android.content.pm.PackageManager.PERMISSION_GRANTED)return;if(prefs(a).getBoolean(flag,false))return;prefs(a).edit().putBoolean(flag,true).apply();a.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},code);}catch(Exception ignored){}}
 static String label(Context c){return "追剧更新提醒："+(enabled(c)?"开":"关")+"（收藏的剧有新集时通知）";}
}
