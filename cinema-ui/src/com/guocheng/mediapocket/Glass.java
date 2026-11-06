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
 /**
  * 0.1.173 面板行的背景。原来用系统的无边界水波纹（selectableItemBackgroundBorderless），鼠标 / 触控板悬停时
  * 会有一个跟着光标跑的圆圈。改成圆角、有边界的水波纹：悬停或遥控器聚焦时整行淡淡亮起、左边出现一条强调色短竖条，
  * 行里的内容向右让出 4dp（150ms 渐变）；按下才有水波纹；activated 表示「刚点过、片源列在左边」的那一集。
  * slide 为 null 时移动整行的子视图；accent 为 0 时不画竖条（例如关闭按钮）。
  */
 static void row(Activity a,View row,View slide,int accent){
  float r=dp(a,GlassRules.ROW_RADIUS_DP);
  android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
  states.setEnterFadeDuration(GlassRules.HOVER_MS);states.setExitFadeDuration(GlassRules.HOVER_MS);
  states.addState(new int[]{android.R.attr.state_activated},lit(a,GlassRules.ACTIVE_FILL,accent,r));
  states.addState(new int[]{android.R.attr.state_hovered},lit(a,GlassRules.HOVER_FILL,accent,r));
  states.addState(new int[]{android.R.attr.state_focused},lit(a,GlassRules.HOVER_FILL,accent,r));
  states.addState(new int[0],new android.graphics.drawable.ColorDrawable(0));
  GradientDrawable mask=new GradientDrawable();mask.setColor(0xffffffff);mask.setCornerRadius(r);
  row.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(GlassRules.PRESS_RIPPLE),states,mask));
  row.setFocusable(true);
  final int shift=dp(a,GlassRules.HOVER_SHIFT_DP);
  java.util.function.Consumer<Boolean> move=on->{if(slide!=null){slide.animate().translationX(on?shift:0).setDuration(GlassRules.HOVER_MS).start();return;}if(row instanceof ViewGroup){ViewGroup g=(ViewGroup)row;for(int i=0;i<g.getChildCount();i++)g.getChildAt(i).animate().translationX(on?shift:0).setDuration(GlassRules.HOVER_MS).start();}};
  row.setOnHoverListener((v,e)->{int act=e.getActionMasked();if(act==android.view.MotionEvent.ACTION_HOVER_ENTER)move.accept(true);else if(act==android.view.MotionEvent.ACTION_HOVER_EXIT)move.accept(false);return false;});
  row.setOnFocusChangeListener((v,has)->move.accept(has));
 }
 static android.graphics.drawable.Drawable lit(Activity a,int fill,int accent,float radius){GradientDrawable bg=new GradientDrawable();bg.setColor(fill);bg.setCornerRadius(radius);if(accent==0)return bg;
  GradientDrawable bar=new GradientDrawable();bar.setColor(accent);bar.setCornerRadius(dp(a,2));
  android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{bg,bar});
  layers.setLayerGravity(1,android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);layers.setLayerWidth(1,dp(a,3));layers.setLayerHeight(1,dp(a,GlassRules.BAR_DP));layers.setLayerInsetStart(1,dp(a,3));return layers;}
 /** 和 CinemaMenu 一样：先不可聚焦地显示并复制沉浸式标志，再恢复聚焦，避免状态栏闪出来。 */
 static boolean showImmersive(Activity a,Dialog d){Window w=d.getWindow();View decor=a.getWindow().getDecorView();if(w!=null){w.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);if(Build.VERSION.SDK_INT>=28){WindowManager.LayoutParams lp=w.getAttributes();lp.layoutInDisplayCutoutMode=a.getWindow().getAttributes().layoutInDisplayCutoutMode;w.setAttributes(lp);}}
  try{d.show();}catch(RuntimeException e){return false;}
  if(w!=null){w.getDecorView().setSystemUiVisibility(decor.getSystemUiVisibility());w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);}return true;}
}
