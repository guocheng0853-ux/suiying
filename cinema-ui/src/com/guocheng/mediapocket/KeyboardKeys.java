package com.guocheng.mediapocket;
import android.app.Activity;import android.content.pm.PackageManager;import android.media.AudioManager;import android.view.*;import android.widget.FrameLayout;
/** 0.1.169 把键盘方向键接到两个播放器上。返回 true 表示已处理。 */
final class KeyboardKeys {
 private KeyboardKeys(){}
 static boolean keyboard(Activity a,KeyEvent e){InputDevice d=e.getDevice();boolean alpha=d!=null&&d.getKeyboardType()==InputDevice.KEYBOARD_TYPE_ALPHABETIC;boolean touch=false;try{touch=a.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);}catch(RuntimeException ignored){}return KeyboardRules.keyboard(alpha,touch);}
 /** VLC 播放页（TvSupport 的按键代理先调这里）。 */
 static boolean vlc(Activity a,KeyEvent e){int act=KeyboardRules.action(e.getKeyCode(),keyboard(a,e),PlayerSidePanel.open(a),PlayerGestures.locked(a));if(act==KeyboardRules.NONE)return false;if(e.getAction()!=KeyEvent.ACTION_DOWN)return true;
  if(act==KeyboardRules.VOLUME_UP||act==KeyboardRules.VOLUME_DOWN){Object root=OnlineSubtitleSearch.field(a,"root");volume(a,root instanceof FrameLayout?(FrameLayout)root:null,act==KeyboardRules.VOLUME_UP);return true;}
  int step=KeyboardRules.seekStepMs(PlaybackPrefs.seekMs(a.getSharedPreferences("data",0)),e.getRepeatCount())*(act==KeyboardRules.FORWARD?1:-1);int pos=EmbyPlayback.position(a);if(pos>=0){int to=Math.max(0,pos+step);EmbyPlayback.seek(a,to);FullscreenMode.hud(a,(step>0?"快进 ":"快退 ")+Math.abs(step)/1000+" 秒 · "+TvSupport.clock(to));}PlayerChrome.bump(a);return true;}
 /** 内置杜比播放页。 */
 static boolean media3(SystemPlayerActivity a,KeyEvent e){Media3Controls c=a.chrome;int act=KeyboardRules.action(e.getKeyCode(),keyboard(a,e),false,c!=null&&c.locked);if(act==KeyboardRules.NONE||c==null)return false;if(e.getAction()!=KeyEvent.ACTION_DOWN)return true;
  if(act==KeyboardRules.VOLUME_UP||act==KeyboardRules.VOLUME_DOWN){volume(a,a.root,act==KeyboardRules.VOLUME_UP);return true;}
  c.jump(act==KeyboardRules.FORWARD?1:-1);return true;}
 static void volume(Activity a,FrameLayout host,boolean up){try{AudioManager am=(AudioManager)a.getSystemService(Activity.AUDIO_SERVICE);if(am==null)return;am.adjustStreamVolume(AudioManager.STREAM_MUSIC,up?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER,0);int max=am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),now=am.getStreamVolume(AudioManager.STREAM_MUSIC);if(host!=null){LevelHud.show(a,host,false,max<=0?0:Math.round(now*100f/max),100);LevelHud.hideSoon(a,LevelHudMetrics.HOLD_MS);}else FullscreenMode.hud(a,"音量 "+(max<=0?0:Math.round(now*100f/max))+"%");}catch(RuntimeException ignored){}}
}
