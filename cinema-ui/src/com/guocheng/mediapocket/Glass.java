package com.guocheng.mediapocket;
import android.app.Activity;import android.app.Dialog;import android.graphics.drawable.GradientDrawable;import android.os.Build;import android.view.*;
/**
 * 0.1.169 雾面玻璃面板。面板做成一个只有面板大小的独立窗口，用系统的窗口背景模糊（Android 12+，手机 / 平板开着跨窗口模糊时）
 * 把后面的视频画面磨砂化，再盖一层半透明深色；视频是 SurfaceView，只有窗口级模糊能透过去。不支持时退回更不透明的深色。
 */
final class Glass {
 private Glass(){}
 static boolean blur(Activity a){if(Build.VERSION.SDK_INT<31)return false;try{WindowManager wm=a.getSystemService(WindowManager.class);return wm!=null&&wm.isCrossWindowBlurEnabled();}catch(RuntimeException e){return false;}}
 static int dp(Activity a,float v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 /** 模糊开着时 62% 深色，关着时 90%，保证文字对比度。 */
 static int tint(boolean blur){return GlassRules.tint(blur);}
 static GradientDrawable surface(Activity a,boolean blur,int radiusDp){GradientDrawable g=new GradientDrawable();g.setColor(tint(blur));g.setCornerRadius(dp(a,radiusDp));g.setStroke(Math.max(1,dp(a,0.6f)),0x1fffffff);return g;}
 /** 把对话框窗口变成玻璃：背景是圆角半透明，窗口后面模糊；不压暗整屏。 */
 static void window(Activity a,Window w,int radiusDp){if(w==null)return;boolean b=blur(a);w.setBackgroundDrawable(surface(a,b,radiusDp));w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);if(b&&Build.VERSION.SDK_INT>=31){try{w.setBackgroundBlurRadius(dp(a,GlassRules.BLUR_DP));}catch(RuntimeException ignored){}}}
 /** 和 CinemaMenu 一样：先不可聚焦地显示并复制沉浸式标志，再恢复聚焦，避免状态栏闪出来。 */
 static boolean showImmersive(Activity a,Dialog d){Window w=d.getWindow();View decor=a.getWindow().getDecorView();if(w!=null){w.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);if(Build.VERSION.SDK_INT>=28){WindowManager.LayoutParams lp=w.getAttributes();lp.layoutInDisplayCutoutMode=a.getWindow().getAttributes().layoutInDisplayCutoutMode;w.setAttributes(lp);}}
  try{d.show();}catch(RuntimeException e){return false;}
  if(w!=null){w.getDecorView().setSystemUiVisibility(decor.getSystemUiVisibility());w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);}return true;}
}
