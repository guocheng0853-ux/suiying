package com.guocheng.mediapocket;
import android.app.Activity;import android.content.*;import android.content.pm.ActivityInfo;import android.net.Uri;import android.os.*;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/**
 * 0.1.111 系统解码播放：用安卓自带的播放器（MediaPlayer / MediaCodec）播放，手机有杜比视界解码器时颜色正确。
 * 给杜比视界 Profile 5 这类 VLC 显示发紫发绿的片源用。功能精简：播放 / 暂停 / 拖动、续播位置、Emby 进度回报；
 * 没有弹幕、外挂字幕和倍速。TrueHD / DTS 音轨系统可能解不了（没声音时改用 VLC 或服务器转码）。
 */
public final class SystemPlayerActivity extends Activity {
 VideoView video;MediaController controls;String watch,title,serverId,item,media,session;long start;boolean started,reportedStart;final Handler tick=new Handler(Looper.getMainLooper());
 final Runnable save=new Runnable(){public void run(){persist(false);if(video!=null&&video.isPlaying())report("progress");tick.postDelayed(this,10000);}};

 /** 从其他页面打开。stream 是播放链里的 stream JSON（url、请求头、emby.* 字段）。 */
 static void open(Activity a,JSONObject stream,String watch,String title,long startMs){Intent in=new Intent(a,SystemPlayerActivity.class);in.putExtra("stream",stream.toString());in.putExtra("watch",watch);in.putExtra("title",title);in.putExtra("start",startMs);a.startActivity(in);}
 static Map<String,String> headers(JSONObject stream){Map<String,String> out=new HashMap<>();JSONObject hints=stream.optJSONObject("behaviorHints");JSONObject proxy=hints==null?null:hints.optJSONObject("proxyHeaders");JSONObject req=proxy==null?null:proxy.optJSONObject("request");if(req==null)req=stream.optJSONObject("header");if(req!=null)for(Iterator<String> it=req.keys();it.hasNext();){String k=it.next();String v=req.optString(k);if(!k.isEmpty()&&!v.isEmpty())out.put(k,v);}return out;}

 @Override protected void onCreate(Bundle saved){super.onCreate(saved);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);FullscreenMode.immersive(this);
  JSONObject stream;try{stream=new JSONObject(getIntent().getStringExtra("stream"));}catch(Exception e){finish();return;}
  watch=getIntent().getStringExtra("watch");title=getIntent().getStringExtra("title");start=getIntent().getLongExtra("start",0);if(saved!=null)start=saved.getLong("pos",start);
  serverId=stream.optString("emby.server");item=stream.optString("emby.item");media=stream.optString("emby.mediaSource");session=stream.optString("emby.playSession");
  FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xff000000);setContentView(root);
  video=new VideoView(this);root.addView(video,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
  controls=new MediaController(this);controls.setAnchorView(video);video.setMediaController(controls);
  TextView label=new TextView(this);label.setText((title==null?"":title)+" · 系统解码");label.setTextColor(0xccffffff);label.setTextSize(13);label.setPadding(dp(24),dp(16),dp(24),0);root.addView(label,new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START));
  label.postDelayed(()->label.animate().alpha(0f).setDuration(600).start(),4000);
  video.setOnPreparedListener(mp->{if(start>10000&&!started)video.seekTo((int)Math.min(Integer.MAX_VALUE,start));started=true;video.start();report("start");tick.removeCallbacks(save);tick.postDelayed(save,10000);});
  video.setOnCompletionListener(mp->{persist(true);finish();});
  video.setOnErrorListener((mp,what,extra)->{DebugLog.event("系统解码","播放失败 · what="+what+" extra="+extra);new CinemaDialog(this).setTitle("系统解码无法播放").setMessage("安卓自带解码器打不开这个片源（错误 "+what+" / "+extra+"）。\n\n可以返回后改用 VLC，或在「线路」里换「Emby 服务器转码」。").setPositiveButton("返回",(d,w)->finish()).setOnCancelListener(d->finish()).show();return true;});
  try{String url=stream.optString("url");DebugLog.event("系统解码","打开 · "+LinkProbe.host(url));video.setVideoURI(Uri.parse(url),headers(stream));}catch(Exception e){Toast.makeText(this,"无法打开：" + e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();finish();}
 }
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(video!=null)out.putLong("pos",video.getCurrentPosition());}
 @Override protected void onPause(){super.onPause();tick.removeCallbacks(save);if(video!=null&&started){persist(false);if(video.isPlaying())video.pause();report("pause");}}
 @Override protected void onResume(){super.onResume();if(started){tick.postDelayed(save,10000);}}
 @Override protected void onDestroy(){tick.removeCallbacksAndMessages(null);if(started)report("stop");if(video!=null)video.stopPlayback();super.onDestroy();}

 /** 续播位置写回和 VLC 播放共用的 pos:/dur: 记录。看到最后 3% 记为看完（位置清零）。 */
 void persist(boolean finished){try{if(watch==null||watch.isEmpty()||video==null)return;int pos=video.getCurrentPosition(),dur=video.getDuration();if(dur<=0)return;SharedPreferences.Editor e=getSharedPreferences("data",0).edit();if(finished||pos>dur*0.97)e.putInt("pos:"+watch,0);else if(pos>10000)e.putInt("pos:"+watch,pos);e.putInt("dur:"+watch,dur).apply();}catch(Exception ignored){}}
 /** Emby 进度回报（直接播放）。 */
 void report(String kind){if(serverId.isEmpty()||item.isEmpty()||video==null)return;final long ticks=EmbyApi.msToTicks(Math.max(0,video.getCurrentPosition()));final boolean paused=!video.isPlaying();Context app=getApplicationContext();
  EmbyPlayback.IO.execute(()->{try{JSONObject server=EmbyStore.get(app,serverId);if(server==null)return;JSONObject body=EmbyApi.sessionBody(item,media,session,ticks,paused,true);
   if("start".equals(kind)){if(reportedStart)return;reportedStart=true;EmbyClient.playing(app,server,body);}else if("stop".equals(kind))EmbyClient.stopped(app,server,body);else{body.put("EventName","pause".equals(kind)?"Pause":"TimeUpdate");EmbyClient.progress(app,server,body);}}catch(Exception ignored){}});}
}
