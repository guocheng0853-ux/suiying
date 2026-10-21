package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.graphics.drawable.Icon;import android.media.MediaMetadata;import android.media.session.*;import android.os.Build;import android.os.SystemClock;import android.view.KeyEvent;import org.json.*;import java.util.*;
/**
 * 0.1.106 系统媒体控制：耳机线控（单击暂停 / 播放、双击下一集、三击上一集，系统负责识别）、蓝牙耳机和车机按键、
 * 通知栏和锁屏上的「上一集 / 播放暂停 / 下一集」，以及电视遥控器的媒体键。
 * 播放页每次轮询时同步状态；离开播放页时收起通知、释放会话。
 */
final class PlayerMedia {
 static final Map<Activity,PlayerMedia> ALL=new WeakHashMap<>();
 static final String ACTION="com.guocheng.mediapocket.MEDIA_CONTROL",CHANNEL="playback";static final int NOTE_ID=73201;
 final Activity a;final MediaSession session;BroadcastReceiver receiver;Boolean shownPlaying;String shownTitle;long lastState;
 private PlayerMedia(Activity a){this.a=a;session=new MediaSession(a,"suiying");session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
  session.setCallback(new MediaSession.Callback(){
   @Override public void onPlay(){setPlaying(a,true);}
   @Override public void onPause(){setPlaying(a,false);}
   @Override public void onStop(){setPlaying(a,false);}
   @Override public void onSkipToNext(){neighbor(a,1);}
   @Override public void onSkipToPrevious(){neighbor(a,-1);}
   @Override public void onFastForward(){skip(15000);}
   @Override public void onRewind(){skip(-15000);}
   @Override public void onSeekTo(long pos){EmbyPlayback.seek(a,(int)Math.max(0,Math.min(Integer.MAX_VALUE,pos)));}
  });
  Intent open=new Intent(a,a.getClass()).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  session.setSessionActivity(PendingIntent.getActivity(a,NOTE_ID,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  session.setActive(true);
  receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(!ACTION.equals(i.getAction()))return;String cmd=i.getStringExtra("cmd");if("toggle".equals(cmd))toggle(a);else if("next".equals(cmd))neighbor(a,1);else if("prev".equals(cmd))neighbor(a,-1);else if("close".equals(cmd))a.finish();}};
  IntentFilter f=new IntentFilter(ACTION);if(Build.VERSION.SDK_INT>=33)a.registerReceiver(receiver,f,4);else a.registerReceiver(receiver,f);}

 static void setPlaying(Activity a,boolean play){if(a instanceof SystemPlayerActivity){SystemPlayerActivity n=(SystemPlayerActivity)a;n.resumeAfterPause=false;if(play)n.video.start();else n.video.pause();}else if(EmbyPlayback.playing(a)!=play)PlayerOverlay.pause(a);}
 static void toggle(Activity a){if(a instanceof SystemPlayerActivity){SystemPlayerActivity n=(SystemPlayerActivity)a;n.resumeAfterPause=false;if(n.video.wantsPlayback())n.video.pause();else n.video.start();}else PlayerOverlay.pause(a);}
 static void neighbor(Activity a,int direction){if(a instanceof SystemPlayerActivity)((SystemPlayerActivity)a).playNeighbor(direction);else PlayerOverlay.jump(a,direction);}
 static void tick(Activity a){try{if(!a.getSharedPreferences("data",0).getBoolean("media.controls",true)){dispose(a);return;}PlayerMedia m=ALL.get(a);if(m==null){m=new PlayerMedia(a);ALL.put(a,m);}m.sync();}catch(Throwable ignored){}}
 static void dispose(Activity a){PlayerMedia m=ALL.remove(a);if(m==null)return;try{if(m.receiver!=null)a.unregisterReceiver(m.receiver);}catch(Exception ignored){}try{m.session.setActive(false);m.session.release();}catch(Exception ignored){}try{NotificationManager nm=(NotificationManager)a.getSystemService(Context.NOTIFICATION_SERVICE);if(nm!=null&&ALL.isEmpty())nm.cancel(NOTE_ID);}catch(Exception ignored){}}

 void skip(int delta){int pos=EmbyPlayback.position(a);if(pos<0)return;EmbyPlayback.seek(a,Media3ControlRules.seek(pos,(int)Math.min(Integer.MAX_VALUE,duration()),delta));}
 boolean series(){try{return PlaybackReviewRules.series(PlayerOverlay.meta(a),PlayerOverlay.type(a));}catch(Throwable e){return false;}}
 String title(){Object n=OnlineSubtitleSearch.field(a,"name");String s=n==null?"":n.toString();if(s.isEmpty())s=PlayerOverlay.meta(a).optString("name","随影");return s;}

 void sync(){boolean playing=a instanceof SystemPlayerActivity?((SystemPlayerActivity)a).video.wantsPlayback():EmbyPlayback.playing(a);int pos=EmbyPlayback.position(a);long now=SystemClock.elapsedRealtime();String title=title();
  boolean changed=!Boolean.valueOf(playing).equals(shownPlaying)||!title.equals(shownTitle);
  if(changed||now-lastState>5000){lastState=now;long actions=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_SEEK_TO|PlaybackState.ACTION_FAST_FORWARD|PlaybackState.ACTION_REWIND|PlaybackState.ACTION_STOP;if(series())actions|=PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS;
   float speed=playing?PlaybackPrefs.speed(a.getSharedPreferences("data",0)):0f;
   session.setPlaybackState(new PlaybackState.Builder().setActions(actions).setState(playing?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,Math.max(0,pos),speed<=0&&playing?1f:speed).build());}
  if(!title.equals(shownTitle)||changed||now-lastState<50){long dur=duration();MediaMetadata.Builder md=new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,title).putString(MediaMetadata.METADATA_KEY_ARTIST,"随影");if(dur>0)md.putLong(MediaMetadata.METADATA_KEY_DURATION,dur);session.setMetadata(md.build());}
  if(changed){shownPlaying=playing;shownTitle=title;notice(playing,title);}}
 long duration(){try{Object video=OnlineSubtitleSearch.field(a,"video");Object r=video==null?null:OnlineSubtitleSearch.call(video,"getDuration",new Class<?>[0]);return r instanceof Number?((Number)r).longValue():0;}catch(Exception e){return 0;}}

 /** 通知栏 / 锁屏的媒体卡片（Android 13 起媒体会话通知不需要单独申请通知权限）。 */
 void notice(boolean playing,String title){try{NotificationManager nm=(NotificationManager)a.getSystemService(Context.NOTIFICATION_SERVICE);if(nm==null)return;
  if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"正在播放",NotificationManager.IMPORTANCE_LOW);ch.setShowBadge(false);ch.setSound(null,null);nm.createNotificationChannel(ch);}
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(a,CHANNEL):new Notification.Builder(a);
  boolean s=series();List<Notification.Action> acts=new ArrayList<>();
  if(s)acts.add(action(android.R.drawable.ic_media_previous,"上一集","prev",1));
  acts.add(action(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing?"暂停":"播放","toggle",2));
  if(s)acts.add(action(android.R.drawable.ic_media_next,"下一集","next",3));
  acts.add(action(android.R.drawable.ic_menu_close_clear_cancel,"关闭","close",4));
  for(Notification.Action x:acts)b.addAction(x);
  int[] compact=s?new int[]{0,1,2}:new int[]{0};
  b.setSmallIcon(playing?android.R.drawable.ic_media_play:android.R.drawable.ic_media_pause).setContentTitle(title).setContentText(playing?"正在播放":"已暂停").setVisibility(Notification.VISIBILITY_PUBLIC).setOngoing(playing).setShowWhen(false).setOnlyAlertOnce(true)
   .setContentIntent(session.getController().getSessionActivity()).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(compact));
  nm.notify(NOTE_ID,b.build());}catch(Throwable ignored){}}
 Notification.Action action(int icon,String label,String cmd,int code){Intent i=new Intent(ACTION).setPackage(a.getPackageName()).putExtra("cmd",cmd);PendingIntent pi=PendingIntent.getBroadcast(a,NOTE_ID+code,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new Notification.Action.Builder(Icon.createWithResource(a,icon),label,pi).build();}

 /** 播放页按键：遥控器 / 键盘的媒体键直接交给会话处理（未被播放页消费时）。 */
 static boolean mediaKey(Activity a,KeyEvent e){PlayerMedia m=ALL.get(a);if(m==null)return false;int k=e.getKeyCode();if(k==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE||k==KeyEvent.KEYCODE_HEADSETHOOK||k==KeyEvent.KEYCODE_MEDIA_PLAY||k==KeyEvent.KEYCODE_MEDIA_PAUSE||k==KeyEvent.KEYCODE_MEDIA_NEXT||k==KeyEvent.KEYCODE_MEDIA_PREVIOUS||k==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD||k==KeyEvent.KEYCODE_MEDIA_REWIND){return m.session.getController().dispatchMediaButtonEvent(e);}return false;}
}
