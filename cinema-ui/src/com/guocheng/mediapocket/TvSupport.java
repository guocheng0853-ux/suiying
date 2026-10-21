package com.guocheng.mediapocket;
import android.app.*;import android.graphics.drawable.*;import android.os.Bundle;import android.view.*;import java.lang.reflect.*;import java.util.*;
/**
 * 0.1.106 电视盒子 / 遥控器 / 键盘适配。
 * 1. 用方向键移动时，当前选中的按钮、海报卡片画一圈高亮框（触屏操作时不画）。
 * 2. 播放页：控制栏收起时，确认键 = 播放 / 暂停，左右键 = 快退 / 快进 10 秒（按住连续），上下键 / 菜单键 = 呼出控制栏；
 *    控制栏打开时方向键照常在按钮间移动。媒体键交给系统媒体会话（PlayerMedia）。
 * 清单里另加了电视桌面入口（LEANBACK_LAUNCHER）并声明触屏不是必需的。
 */
final class TvSupport {
 private TvSupport(){}
 static boolean installed;static final Map<Activity,Boolean> FOCUS=new WeakHashMap<>(),KEYS=new WeakHashMap<>();static final Map<View,Drawable> SAVED=new WeakHashMap<>();
 static void install(Application app){if(installed)return;installed=true;app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
  public void onActivityResumed(Activity a){focus(a);}
  public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){FOCUS.remove(a);KEYS.remove(a);}});}

 static Drawable ring(Activity a){float d=a.getResources().getDisplayMetrics().density;GradientDrawable g=new GradientDrawable();g.setColor(0x1fffffff);g.setStroke(Math.round(3*d),0xff78b7ff);g.setCornerRadius(12*d);return g;}
 /** 方向键焦点高亮：只在非触屏模式下画，换焦点时恢复原来的前景。 */
 static void focus(Activity a){try{if(FOCUS.containsKey(a))return;FOCUS.put(a,true);View decor=a.getWindow().getDecorView();
  decor.getViewTreeObserver().addOnGlobalFocusChangeListener((old,now)->{try{if(old!=null&&SAVED.containsKey(old)){old.setForeground(SAVED.remove(old));}
   if(now!=null&&!now.isInTouchMode()&&!(now instanceof android.widget.EditText)&&now.getWidth()>0){SAVED.put(now,now.getForeground());now.setForeground(ring(a));}}catch(Throwable ignored){}});}catch(Throwable ignored){}}

 /** 播放页按键，由 PlayerChrome 每次轮询调用；只装一次。 */
 static void player(Activity a){if(KEYS.containsKey(a))return;KEYS.put(a,true);try{Window w=a.getWindow();Window.Callback original=w.getCallback();if(original==null)return;Set<Integer> held=new HashSet<>();
  Object proxy=Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),new Class<?>[]{Window.Callback.class},(p,m,args)->{
   if("dispatchKeyEvent".equals(m.getName())&&args!=null&&args.length==1&&args[0] instanceof KeyEvent){KeyEvent e=(KeyEvent)args[0];
    try{if(PlayerMedia.mediaKey(a,e))return true;Boolean mine=playerKey(a,e,held);if(mine!=null)return mine;}catch(Throwable ignored){}}
   try{return m.invoke(original,args);}catch(InvocationTargetException ex){throw ex.getCause();}});
  w.setCallback((Window.Callback)proxy);}catch(Throwable ignored){}}

 static boolean controlsShown(Activity a){Object top=OnlineSubtitleSearch.field(a,"top");return top instanceof View&&((View)top).getVisibility()==View.VISIBLE;}
 /** 返回 null 表示交给原来的处理。 */
 static Boolean playerKey(Activity a,KeyEvent e,Set<Integer> held){int k=e.getKeyCode();boolean down=e.getAction()==KeyEvent.ACTION_DOWN;
  if(e.getAction()==KeyEvent.ACTION_UP&&held.remove(k))return true;
  if(!down||controlsShown(a)||PlayerSidePanel.open(a)||PlayerGestures.locked(a))return null;
  switch(k){
   case KeyEvent.KEYCODE_DPAD_CENTER:case KeyEvent.KEYCODE_ENTER:case KeyEvent.KEYCODE_NUMPAD_ENTER:case KeyEvent.KEYCODE_SPACE:
    held.add(k);if(e.getRepeatCount()==0){boolean was=EmbyPlayback.playing(a);PlayerOverlay.pause(a);FullscreenMode.hud(a,was?"已暂停":"继续播放");}return true;
   case KeyEvent.KEYCODE_DPAD_LEFT:case KeyEvent.KEYCODE_DPAD_RIGHT:{held.add(k);int step=(k==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)*(e.getRepeatCount()>8?30000:10000);int pos=EmbyPlayback.position(a);if(pos>=0){int to=Math.max(0,pos+step);EmbyPlayback.seek(a,to);FullscreenMode.hud(a,(step>0?"快进 ":"快退 ")+Math.abs(step)/1000+" 秒 · "+clock(to));}return true;}
   case KeyEvent.KEYCODE_DPAD_UP:case KeyEvent.KEYCODE_DPAD_DOWN:case KeyEvent.KEYCODE_MENU:
    held.add(k);try{OnlineSubtitleSearch.call(a,"controls",new Class<?>[]{boolean.class},true);}catch(Exception ignored){}return true;
   default:return null;}
 }
 static String clock(int ms){int s=Math.max(0,ms/1000),h=s/3600,m=s/60%60,sec=s%60;return h>0?String.format(Locale.ROOT,"%d:%02d:%02d",h,m,sec):String.format(Locale.ROOT,"%02d:%02d",m,sec);}
}
