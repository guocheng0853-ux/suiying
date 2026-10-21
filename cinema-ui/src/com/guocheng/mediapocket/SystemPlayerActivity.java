package com.guocheng.mediapocket;
import android.app.Activity;import android.content.*;import android.content.pm.ActivityInfo;import android.net.Uri;import android.os.*;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/**
 * App 内置 Media3 解复用 + 平台 MediaCodec 杜比解码；保留原先系统播放入口。
 * 给杜比视界 Profile 5 这类 VLC 显示发紫发绿的片源用。控件共用播放偏好，支持音轨、字幕、倍速、比例、弹幕和选集。
 * TrueHD / DTS 音轨系统可能解不了（没声音时改用 VLC 或服务器转码）。
 */
public final class SystemPlayerActivity extends Activity {
 Media3VideoView video;Media3Controls chrome;FrameLayout root;LinearLayout top,bottom;JSONObject meta=new JSONObject();String name="",id="",type="movie";TextView wait;long opened;boolean rendered,asked;JSONObject source;String watch,title,serverId,item,media,session;long start;boolean started,reportedStart,resumed,resumeAfterPause;final Handler tick=new Handler(Looper.getMainLooper());
 final Runnable save=new Runnable(){public void run(){persist(false);if(video!=null&&video.isPlaying())report("progress");tick.postDelayed(this,10000);}};

 /** 从其他页面打开。stream 是播放链里的 stream JSON（url、请求头、emby.* 字段）。 */
 static void open(Activity a,JSONObject stream,String watch,String title,long startMs,Intent vlc){Intent in=new Intent(a,SystemPlayerActivity.class);if(vlc!=null)in.putExtra("vlc",vlc);in.putExtra("stream",stream.toString());in.putExtra("watch",watch);in.putExtra("title",title);in.putExtra("start",startMs);a.startActivity(in);}
 static Map<String,String> headers(JSONObject stream){Map<String,String> out=new HashMap<>();JSONObject hints=stream.optJSONObject("behaviorHints");JSONObject proxy=hints==null?null:hints.optJSONObject("proxyHeaders");JSONObject req=proxy==null?null:proxy.optJSONObject("request");if(req==null)req=stream.optJSONObject("header");if(req!=null)for(Iterator<String> it=req.keys();it.hasNext();){String k=it.next();String v=req.optString(k);if(!k.isEmpty()&&!v.isEmpty())out.put(k,v);}return out;}

 @Override protected void onCreate(Bundle saved){super.onCreate(saved);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);FullscreenMode.immersive(this);
  JSONObject stream;try{stream=new JSONObject(getIntent().getStringExtra("stream"));}catch(Exception e){finish();return;}source=stream;opened=SystemClock.elapsedRealtime();
  watch=getIntent().getStringExtra("watch");title=getIntent().getStringExtra("title");start=getIntent().getLongExtra("start",0);if(saved!=null)start=saved.getLong("pos",start);
  name=title==null?"":title;id=watch==null?"":watch;Intent original=getIntent().getParcelableExtra("vlc");if(original!=null){try{meta=new JSONObject(original.getStringExtra("meta"));}catch(Exception ignored){}type=original.getStringExtra("type");if(type==null)type=meta.optString("type","movie");}
  serverId=stream.optString("emby.server");item=stream.optString("emby.item");media=stream.optString("emby.mediaSource");session=stream.optString("emby.playSession");
  root=new FrameLayout(this);root.setBackgroundColor(0xff000000);setContentView(root);
  video=new Media3VideoView(this);video.requireDolbyP5(DolbyRules.P5.equals(stream.optString("dolby.kind")));root.addView(video,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
  // 0.1.113：准备 / 缓冲时给出提示和用时；出画面前一直显示，长时间没画面就给出办法
  wait=new TextView(this);wait.setTextColor(0xddffffff);wait.setTextSize(14);wait.setGravity(Gravity.CENTER);wait.setText("内置播放器正在准备…");root.addView(wait,new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));
  chrome=new Media3Controls(this,root);String mode=PlaybackPrefs.rememberAspect(getSharedPreferences("data",0))?getSharedPreferences("data",0).getString("aspectMode","fit"):"fit";video.aspect(mode);
  tick.post(watchdog);
  video.setOnInfoListener((mp,what,extra)->{if(what==3){rendered=true;wait.setVisibility(View.GONE);DebugLog.event("系统解码","出画面 · 用时 "+(SystemClock.elapsedRealtime()-opened)/100/10.0+" 秒");}else if(what==701){wait.setText("缓冲中…");wait.setVisibility(View.VISIBLE);}else if(what==702){if(rendered)wait.setVisibility(View.GONE);}else if(what==804||what==805){DebugLog.event("系统解码","音视频轨不支持 · what="+what);}return false;});
  video.setOnPreparedListener(mp->{mp.setOnVideoSizeChangedListener((p2,w,h)->DebugLog.event("系统解码","画面 "+w+"×"+h));DebugLog.event("系统解码","准备完成 · 用时 "+(SystemClock.elapsedRealtime()-opened)/100/10.0+" 秒 · 画面 "+mp.getVideoWidth()+"×"+mp.getVideoHeight());if(mp.getVideoWidth()==0){wait.setText("只解出了声音，没有画面");}if(start>10000&&!started)video.seekTo((int)Math.min(Integer.MAX_VALUE,start));started=true;if(resumed)video.start();else resumeAfterPause=true;report("start");tick.removeCallbacks(save);tick.postDelayed(save,10000);String remembered=ExternalSubtitleStore.remembered(getSharedPreferences("data",0),id);if(!remembered.isEmpty()&&!PlaybackPrefs.subtitlesOff(getSharedPreferences("data",0))&&!getSharedPreferences("data",0).getBoolean("subtitleDisabled:"+id,false))loadSubtitleFile(remembered,"已记住的字幕");});
  video.setOnCompletionListener(mp->{persist(true);finish();});
  video.setOnErrorListener((mp,what,extra)->{asked=true;tick.removeCallbacks(watchdog);wait.setVisibility(View.GONE);DebugLog.event("系统解码","播放失败 · what="+what+" extra="+extra);new android.app.AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle(what>=2000&&what<3000?"片源连接失败":"内置杜比播放失败").setMessage(Media3ErrorRules.message(what,extra)).setPositiveButton(what>=2000&&what<3000?"重新获取地址":"改用 VLC",(d,w)->{if(what>=2000&&what<3000)retrySource();else toVlc();}).setNeutralButton("用外部播放器打开",(d,w)->external()).setNegativeButton("返回",(d,w)->finish()).setOnCancelListener(d->finish()).show();return true;});
  try{String url=stream.optString("url");DebugLog.event("系统解码","打开 · "+LinkProbe.host(url));video.setVideoURI(Uri.parse(url),headers(stream));}catch(Exception e){Toast.makeText(this,"无法打开：" + e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();finish();}
 }
 /** 每秒检查：准备超过 25 秒或开播 8 秒仍无画面，给出办法（只问一次）。 */
 final Runnable watchdog=new Runnable(){public void run(){if(isFinishing())return;long s=(SystemClock.elapsedRealtime()-opened)/1000;
  if(!rendered&&wait.getVisibility()==View.VISIBLE&&!started)wait.setText("内置播放器正在准备… "+s+" 秒\n大文件第一次打开可能要十几秒");
  if(!asked&&!rendered&&(s>=20||(started&&video!=null&&video.isPlaying()&&video.getCurrentPosition()>8000))){asked=true;DebugLog.event("系统解码","无画面 · "+s+" 秒 · 已准备="+started+" · 位置="+(video==null?-1:video.getCurrentPosition()));
   new android.app.AlertDialog.Builder(SystemPlayerActivity.this,android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("内置播放尚未显示画面").setMessage(started?"声音在走但没有画面，手机自带解码器可能不支持这个片源的画面格式。":"等了 "+s+" 秒还没准备好，可能是片源太大或格式不受支持。").setPositiveButton("改用 VLC",(d,w)->toVlc()).setNeutralButton("用外部播放器打开",(d,w)->external()).setNegativeButton("再等等",null).show();}
  if(!rendered)tick.postDelayed(this,1000);}};
 /** Refresh expiring Emby addresses without pretending an HTTP failure is a codec failure. */
 void retrySource(){
  persist(false);JSONObject server=EmbyStore.get(this,serverId);
  if(server!=null&&!item.isEmpty()){EmbyPlayback.playItem(this,server,item,media,true);return;}
  start=Math.max(start,video==null?0:video.getCurrentPosition());started=false;rendered=false;asked=false;opened=SystemClock.elapsedRealtime();wait.setText("正在重新连接…");wait.setVisibility(View.VISIBLE);
  video.setVideoURI(Uri.parse(source.optString("url")),headers(source));tick.removeCallbacks(watchdog);tick.post(watchdog);
 }
 /** 回到 VLC 播放（从当前位置；进度已写回 pos:）。 */
 void toVlc(){persist(false);Intent vlc=getIntent().getParcelableExtra("vlc");if(vlc!=null){try{startActivity(vlc);}catch(Exception e){Toast.makeText(this,"无法回到 VLC",Toast.LENGTH_LONG).show();}}finish();}
 /** 交给手机上的其他播放器（如 Just Player、MX Player）；Emby 地址自带令牌，影视仓需要请求头的片源可能打不开。 */
 void loadSubtitleFile(String path,String label){String lower=path.toLowerCase(java.util.Locale.ROOT).split("\\?")[0];String mime=lower.endsWith(".ass")||lower.endsWith(".ssa")?"text/x-ssa":lower.endsWith(".vtt")?"text/vtt":"application/x-subrip";video.addSubtitle(path,mime,label);ExternalSubtitleStore.remember(getSharedPreferences("data",0),id,path,"");}
 void external(){persist(false);long pos=video==null?0:video.getCurrentPosition();if(ExternalVideo.open(this,source.optString("url"),title,Math.max(pos,start),ExternalVideo.headers(source)))finish();else Toast.makeText(this,"手机上没有其他播放器，可以装一个 Just Player（支持杜比视界）",Toast.LENGTH_LONG).show();}
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(video!=null)out.putLong("pos",video.getCurrentPosition());}
 @Override protected void onPause(){resumed=false;super.onPause();tick.removeCallbacks(save);if(video!=null&&started){persist(false);resumeAfterPause=video.wantsPlayback();video.pause();report("pause");}}
 @Override protected void onResume(){super.onResume();resumed=true;if(started){if(resumeAfterPause){video.start();resumeAfterPause=false;}tick.postDelayed(save,10000);}}
 @Override protected void onDestroy(){tick.removeCallbacksAndMessages(null);if(started)report("stop");if(chrome!=null)chrome.dispose();if(video!=null)video.stopPlayback();CinemaMenu.dismissNow(this);super.onDestroy();}

 /** 续播位置写回和 VLC 播放共用的 pos:/dur: 记录。看到最后 3% 记为看完（位置清零）。 */
 void persist(boolean finished){try{if(watch==null||watch.isEmpty()||video==null)return;int pos=video.getCurrentPosition(),dur=video.getDuration();if(dur<=0)return;SharedPreferences.Editor e=getSharedPreferences("data",0).edit();if(finished||pos>dur*0.97)e.putInt("pos:"+watch,0);else if(pos>10000)e.putInt("pos:"+watch,pos);e.putInt("dur:"+watch,dur).apply();}catch(Exception ignored){}}
 /** Emby 进度回报（直接播放）。 */
 void report(String kind){if(serverId.isEmpty()||item.isEmpty()||video==null)return;final long ticks=EmbyApi.msToTicks(Math.max(0,video.getCurrentPosition()));final boolean paused=!video.isPlaying();Context app=getApplicationContext();
  EmbyPlayback.IO.execute(()->{try{JSONObject server=EmbyStore.get(app,serverId);if(server==null)return;JSONObject body=EmbyApi.sessionBody(item,media,session,ticks,paused,true);
   if("start".equals(kind)){if(reportedStart)return;reportedStart=true;EmbyClient.playing(app,server,body);}else if("stop".equals(kind))EmbyClient.stopped(app,server,body);else{body.put("EventName","pause".equals(kind)?"Pause":"TimeUpdate");EmbyClient.progress(app,server,body);}}catch(Exception ignored){}});}
}
