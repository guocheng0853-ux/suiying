package com.guocheng.mediapocket;
import android.app.Activity;
import java.util.WeakHashMap;
/**
 * 进入播放页立刻请求传感器横屏（两个横屏方向都可用），离开播放页时恢复进入前的方向。
 * 全屏铺满仍由 FullscreenMode 的画面比例和沉浸式处理，不靠清单里的 screenOrientation。
 * 画中画开始前暂时放开方向锁，失败或回到全屏播放时再收回，避免方向锁挡住 PiP。
 */
public final class PlaybackOrientation {
 static final int UNSPECIFIED=-1,LANDSCAPE=0,PORTRAIT=1,SENSOR_LANDSCAPE=6,SENSOR_PORTRAIT=7,NONE=Integer.MIN_VALUE;
 static final class State {int restore=NONE,pip=NONE,videoW,videoH;}
 static final WeakHashMap<Activity,State> ALL=new WeakHashMap<>();
 private PlaybackOrientation(){}
 /** 播放中使用的方向。6 = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE。 */
 static int playback(){return SENSOR_LANDSCAPE;}
 /** 进入前的请求。已经是传感器横屏说明播放页曾重建，其余页面用的是未指定方向。 */
 static int remember(int requested){return requested==SENSOR_LANDSCAPE||requested==SENSOR_PORTRAIT?UNSPECIFIED:requested;}
 /** 仅在播放页真正关闭时恢复。画中画和切到后台的 onPause 不能把方向放回去。 */
 static boolean restoreOnPause(boolean finishing){return finishing;}
 /** 画中画期间不锁方向，让系统按画面比例摆窗口。 */
 static int duringPip(){return UNSPECIFIED;}
 /** 退出画中画回到播放页时，恢复进入画中画之前的请求（含用户按过旋转）。 */
 static int afterPip(int suspended){return suspended;}
 /** 底下的页面只清掉播放器可能留下的横屏/竖屏锁，不动系统传感器或用户锁定。 */
 static boolean hostShouldRelease(int requested){return requested==SENSOR_LANDSCAPE||requested==SENSOR_PORTRAIT||requested==LANDSCAPE||requested==PORTRAIT;}
 /** GesturePlayerActivity.onCreate 第一条指令：先于 super.onCreate / setContentView。 */
 public static void enter(Object owner){Activity a=activity(owner);if(a==null)return;State s=state(a);if(s.restore!=NONE)return;s.restore=remember(a.getRequestedOrientation());apply(a,playback());}
 /** GesturePlayerActivity.onPause 开头：正在 finish 才恢复。 */
 public static void onPause(Object owner){Activity a=activity(owner);if(a==null||!restoreOnPause(a.isFinishing()))return;restore(a);}
 /** 已经 stop 之后再被关掉时不会再进 onPause，在 onDestroy 再补一次。 */
 public static void onDestroy(Object owner){Activity a=activity(owner);if(a!=null)restore(a);}
 static void restore(Activity a){State s=ALL.remove(a);if(s==null||s.restore==NONE)return;apply(a,s.restore);}
 /** 调用 enterPictureInPictureMode 之前放开方向锁。 */
 static void suspendForPip(Activity a){if(a==null)return;State s=state(a);if(s.pip!=NONE)return;s.pip=a.getRequestedOrientation();apply(a,duringPip());}
 /** 画中画没进去、或从画中画回到播放页。正在关闭时不再锁回横屏。 */
 static void resumeAfterPip(Activity a){if(a==null||a.isFinishing())return;State s=ALL.get(a);if(s==null||s.pip==NONE)return;int back=s.pip;s.pip=NONE;apply(a,afterPip(back));}
 /** 播放页底下的 Activity 回到前台时，清掉泄漏的横竖屏锁。 */
 public static void releaseHost(Activity a){if(a==null||a.isFinishing())return;try{if(hostShouldRelease(a.getRequestedOrientation()))apply(a,UNSPECIFIED);}catch(Exception ignored){}}
 /** 0.1.65：竖屏视频（高明显大于宽，如 9:16 短剧）用传感器竖屏，其余横屏。尺寸未知返回 NONE。 */
 static int forVideo(int w,int h){if(w<=0||h<=0)return NONE;return h>w*1.05f?SENSOR_PORTRAIT:SENSOR_LANDSCAPE;}
 /** 播放器报告视频尺寸后调用（PlayerChrome 每 250ms）。只在尺寸变化（换了视频）时判断一次，手动「旋转」后本集不再改回；画中画期间不动。 */
 static void fitVideo(Activity a,int w,int h){if(a==null||a.isFinishing())return;State s=ALL.get(a);if(s==null||s.restore==NONE||s.pip!=NONE)return;if(w<=0||h<=0||(w==s.videoW&&h==s.videoH))return;s.videoW=w;s.videoH=h;int want=forVideo(w,h);if(want!=NONE){DebugLog.event("方向","视频 "+w+"x"+h+" → "+(want==SENSOR_PORTRAIT?"竖屏":"横屏"));apply(a,want);}}
 static Activity activity(Object owner){return owner instanceof Activity?(Activity)owner:null;}
 static State state(Activity a){State s=ALL.get(a);if(s==null)ALL.put(a,s=new State());return s;}
 static void apply(Activity a,int orientation){try{if(a.getRequestedOrientation()!=orientation)a.setRequestedOrientation(orientation);}catch(Exception ignored){}}
}
