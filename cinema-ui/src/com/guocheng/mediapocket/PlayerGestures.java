package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.media.AudioManager;import android.provider.Settings;import android.view.*;import android.widget.*;import java.util.*;
/** 单指手势与锁屏。替换 GesturePlayerActivity.taps；双指缩放仍由 FullscreenMode 的 pinch 处理（多指时宿主不会把事件交给 taps）。 */
final class PlayerGestures extends GestureDetector.SimpleOnGestureListener {
 static final Map<Activity,PlayerGestures> ALL=new WeakHashMap<>();
 final Activity a;final AudioManager audio;int axis;float initialBrightness,holdDownX,heldSpeed=1f;int initialVolume,seekStart=-1,seekTarget=-1,seekDuration;boolean locked,holding;Button lockButton;
 PlayerGestures(Activity a){this.a=a;audio=(AudioManager)a.getSystemService(Context.AUDIO_SERVICE);}
 static boolean locked(Activity a){PlayerGestures g=ALL.get(a);return g!=null&&g.locked;}
 static void install(Activity a){try{if(ALL.containsKey(a))return;PlayerGestures g=new PlayerGestures(a);GestureDetector d=new GestureDetector(a,g){@Override public boolean onTouchEvent(MotionEvent e){boolean r=super.onTouchEvent(e);int m=e.getActionMasked();if(g.holding&&m==MotionEvent.ACTION_MOVE)g.dragHold(e);if(m==MotionEvent.ACTION_UP){g.endHold();g.finish(true);EmbyPlayerUi.preview(a,-1);}else if(m==MotionEvent.ACTION_CANCEL){g.endHold();g.finish(false);EmbyPlayerUi.preview(a,-1);}return r;}};SourceSubtitleLoader.set(a,"taps",d);ALL.put(a,g);g.buildLock();}catch(Exception ignored){}}
 int dp(int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 float width(){Object r=OnlineSubtitleSearch.field(a,"root");return r instanceof View&&((View)r).getWidth()>0?((View)r).getWidth():a.getResources().getDisplayMetrics().widthPixels;}
 float height(){Object r=OnlineSubtitleSearch.field(a,"root");return r instanceof View&&((View)r).getHeight()>0?((View)r).getHeight():a.getResources().getDisplayMetrics().heightPixels;}
 void controls(boolean on){try{OnlineSubtitleSearch.call(a,"controls",new Class<?>[]{boolean.class},on);}catch(Exception ignored){}}
 boolean controlsVisible(){return Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"controlsVisible"));}
 Object video(){return OnlineSubtitleSearch.field(a,"video");}
 int intCall(Object v,String name){try{Object r=v==null?null:OnlineSubtitleSearch.call(v,name,new Class<?>[0]);return r instanceof Integer?(Integer)r:-1;}catch(Exception e){return -1;}}
 void markSeek(){try{Object s=OnlineSubtitleSearch.field(a,"session");if(s!=null)OnlineSubtitleSearch.call(s,"seek",new Class<?>[0]);}catch(Exception ignored){}}
 void seekTo(int ms){try{markSeek();OnlineSubtitleSearch.call(video(),"seekTo",new Class<?>[]{int.class},ms);}catch(Exception ignored){}}
 @Override public void onLongPress(MotionEvent e){if(locked||axis!=PlayerGestureRules.NONE)return;holding=true;holdDownX=e.getX();heldSpeed=PlaybackPrefs.speed(a.getSharedPreferences("data",0));PlayerChrome.temporarySpeed(a,SpeedHold.HOLD);FullscreenMode.hud(a,"2×  松开恢复");}
 @Override public boolean onDown(MotionEvent e){axis=PlayerGestureRules.NONE;seekStart=-1;seekTarget=-1;if(locked)return true;initialVolume=audio==null?0:audio.getStreamVolume(AudioManager.STREAM_MUSIC);float b=a.getWindow().getAttributes().screenBrightness;if(b<0)try{b=Settings.System.getInt(a.getContentResolver(),"screen_brightness")/255f;}catch(Exception ex){b=0.5f;}initialBrightness=b;return true;}
 @Override public boolean onSingleTapConfirmed(MotionEvent e){if(locked){flashLock();return true;}controls(!controlsVisible());return true;}
 @Override public boolean onDoubleTap(MotionEvent e){if(locked){flashLock();return true;}int zone=PlayerGestureRules.third(e.getX(),width());if(zone==PlayerGestureRules.CENTER){if(FullscreenMode.doubleTapCenter(a))return true;Object b=OnlineSubtitleSearch.field(a,"pauseButton");if(b instanceof View)((View)b).performClick();return true;}Object v=video();int dur=intCall(v,"getDuration"),pos=intCall(v,"getCurrentPosition");if(dur<=0||pos<0){FullscreenMode.hud(a,"等待视频准备完成");return true;}int delta=PlayerGestureRules.tapDelta(zone);seekTo(PlayerGestureRules.clampSeek((long)pos+delta,dur));PlayerRedesign.hideSeekButtons(a);FullscreenMode.hud(a,PlayerGestureRules.tapHint(delta));return true;}
 void dragHold(MotionEvent e){float rate=SpeedHold.tuned(e.getX()-holdDownX,width());PlayerChrome.temporarySpeed(a,rate);FullscreenMode.hud(a,PlaybackPrefs.speedLabel(rate)+"  松开恢复");}
 void endHold(){if(!holding)return;holding=false;PlayerChrome.temporarySpeed(a,heldSpeed);FullscreenMode.hud(a,"倍速 "+PlaybackPrefs.speedLabel(heldSpeed));}
 @Override public boolean onScroll(MotionEvent down,MotionEvent e,float distanceX,float distanceY){if(holding){dragHold(e);return true;}if(locked||down==null)return true;float dx=e.getX()-down.getX(),dy=down.getY()-e.getY();
  if(axis==PlayerGestureRules.NONE){axis=PlayerGestureRules.axis(dx,dy,dp(24));if(axis==PlayerGestureRules.NONE)return true;controls(false);if(axis==PlayerGestureRules.HORIZONTAL){Object v=video();seekDuration=intCall(v,"getDuration");seekStart=intCall(v,"getCurrentPosition");if(seekDuration<=0||seekStart<0){axis=-1;FullscreenMode.hud(a,"等待视频准备完成");return true;}}}
  if(axis==PlayerGestureRules.HORIZONTAL){seekTarget=PlayerGestureRules.seekTarget(seekStart,seekDuration,dx,width());FullscreenMode.hud(a,PlayerGestureRules.seekLabel(seekTarget,seekStart,seekDuration));EmbyPlayerUi.preview(a,seekTarget);}
  else if(axis==PlayerGestureRules.VERTICAL){float f=dy/Math.max(dp(120),height()*0.65f);if(PlayerGestureRules.brightnessSide(down.getX(),width())){float b=PlayerGestureRules.brightness(initialBrightness,f);WindowManager.LayoutParams w=a.getWindow().getAttributes();w.screenBrightness=b;a.getWindow().setAttributes(w);FullscreenMode.hud(a,"☀ 亮度 "+Math.round(b*100)+"%");}else if(audio!=null){int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),vol=PlayerGestureRules.volume(initialVolume,max,f);audio.setStreamVolume(AudioManager.STREAM_MUSIC,vol,0);FullscreenMode.hud(a,"🔊 音量 "+(max==0?0:Math.round(vol*100f/max))+"%");}}
  return true;}
 /** 抬手时提交横向拖动的目标位置；取消（如第二根手指按下转为缩放）时放弃。 */
 void finish(boolean commit){if(axis==PlayerGestureRules.HORIZONTAL&&seekTarget>=0&&commit)seekTo(seekTarget);axis=PlayerGestureRules.NONE;seekTarget=-1;}
 void buildLock(){Object root=OnlineSubtitleSearch.field(a,"root");if(!(root instanceof FrameLayout))return;lockButton=new Button(a);lockButton.setAllCaps(false);lockButton.setTextColor(0xffffffff);lockButton.setTextSize(18);lockButton.setBackground(PlayerRedesign.circle(0x59000000));lockButton.setMinWidth(0);lockButton.setMinimumWidth(0);lockButton.setOnClickListener(v->toggleLock());FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.START|Gravity.CENTER_VERTICAL);lp.leftMargin=dp(16);((FrameLayout)root).addView(lockButton,lp);lockButton.setVisibility(View.GONE);label();}
 void label(){if(lockButton==null)return;lockButton.setText("");int side=Math.round(22*a.getResources().getDisplayMetrics().density);PlayerIcons icon=new PlayerIcons(locked?PlayerIcons.LOCK:PlayerIcons.UNLOCK,side);icon.setBounds(0,0,side,side);lockButton.setCompoundDrawables(icon,null,null,null);lockButton.setPadding((dp(48)-side)/2,0,0,0);lockButton.setContentDescription(locked?"解锁屏幕":"锁定屏幕");}
 final Runnable hideLock=()->{if(locked&&lockButton!=null)lockButton.setVisibility(View.GONE);};
 void flashLock(){if(lockButton==null)return;lockButton.setVisibility(View.VISIBLE);lockButton.removeCallbacks(hideLock);lockButton.postDelayed(hideLock,2500);}
 void toggleLock(){locked=!locked;label();if(locked){controls(false);FullscreenMode.hud(a,"已锁定 · 点击屏幕显示解锁按钮");flashLock();}else{controls(true);FullscreenMode.hud(a,"已解锁");}}
 /** PlayerChrome 每 250ms 调用：锁定时控件保持隐藏；锁按钮跟随控件显隐；画中画内隐藏。 */
 void poll(boolean pip){if(lockButton==null)return;if(pip){lockButton.setVisibility(View.GONE);return;}if(locked){if(controlsVisible())controls(false);return;}lockButton.setVisibility(controlsVisible()?View.VISIBLE:View.GONE);}
 static void tick(Activity a,boolean pip){PlayerGestures g=ALL.get(a);if(g!=null)g.poll(pip);}
 static void dispose(Activity a){ALL.remove(a);}
}
