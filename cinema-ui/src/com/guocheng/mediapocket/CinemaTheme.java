package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.Typeface;
/**
 * 0.1.125「影院暗金」外观：纯黑底、暖白字、金色强调，片名用衬线体（参考 Infuse / Apple TV）。
 * 主页在 onCreate 里把是否启用写进 data/ui.gold，Emby、影视仓、下载页读同一个值。
 */
final class CinemaTheme {
 private CinemaTheme(){}
 static final int BG=0xff0b0b0c,CARD=0xff19191c,INK=0xfff4f1ea,MUTED=0xffa8a397,SOFT=0xffd8d2c4,GOLD=0xffe0b25c,ON_GOLD=0xff1a1408,CHIP=0xff2e2920,TILE_A=0xff1d1c1a,TILE_B=0xff252320;
 /** 0.1.128「暖白陶土」：暖白纸色底、墨色字、陶土橙强调，标题使用内置 Noto Serif SC、正文系统无衬线，留白多、不用装饰渐变（参考 Anthropic 的设计取向，未用其商标与商业字体）。 */
 static final int PAPER=0xfffaf9f5,SHEET=0xfffffefa,SLATE=0xff141413,STONE=0xff45423c,PROSE=0xff322f2a,CLAY=0xffd97757,CLAY_DEEP=0xffb5573a,CLAY_CHIP=0xfff1e6de,CLAY_TRACK=0xffe8c4b3,LINE=0xffa58b74,TILE_C=0xffe9e5dc,TILE_D=0xffe2ddd2;
 /** 0.1.132：外观按「存储的选择 + 当前系统深色模式」现算，跟随系统时各页面打开即是对的一套，不依赖主页先写标记。 */
 static boolean night(Context c){return (c.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;}
 static String effective(Context c){return InstallDefaults.effective(c.getSharedPreferences("data",0).getString("cinemaAppearance",InstallDefaults.SYSTEM),night(c));}
 static boolean clay(Context c){return InstallDefaults.CLAY.equals(effective(c));}
 static boolean gold(Context c){return InstallDefaults.GOLD.equals(effective(c));}
 static boolean light(Context c){return clay(c);}
 private static volatile Typeface paperFace;
 private static volatile boolean warming;
 /** Decode the title face off the main thread. The first draw still waits on the same lock, but only for the small subset. */
 static void warm(Context c){
  if(c==null||paperFace!=null)return;
  Context app=c.getApplicationContext();
  synchronized(CinemaTheme.class){if(paperFace!=null||warming)return;warming=true;}
  Thread t=new Thread(()->paperTitle(app),"paper-font");t.setDaemon(true);t.start();
 }
 /** Bundled Chinese serif subset; load once so titles do not depend on an OEM's serif fallback. Missing glyphs fall back to the system serif on API 29+. */
 static Typeface paperTitle(Context c){
  Typeface face=paperFace;if(face!=null)return face;
  synchronized(CinemaTheme.class){
   if(paperFace==null)paperFace=loadPaper(c.getApplicationContext());
   return paperFace;
  }
 }
 static Typeface loadPaper(Context c){
  try{
   if(android.os.Build.VERSION.SDK_INT>=29){
    android.graphics.fonts.Font font=new android.graphics.fonts.Font.Builder(c.getAssets(),"fonts/NotoSerifSC-Regular.otf").build();
    android.graphics.fonts.FontFamily family=new android.graphics.fonts.FontFamily.Builder(font).build();
    return new Typeface.CustomFallbackBuilder(family).setSystemFallback("serif").build();
   }
   return Typeface.createFromAsset(c.getAssets(),"fonts/NotoSerifSC-Regular.otf");
  }catch(Exception missing){return Typeface.create(Typeface.SERIF,Typeface.NORMAL);}
 }
 /**
  * 0.1.132 两套外观共用一套字体规范（只换颜色不换字）：
  * 片名 28–30sp、栏目 / 页面 / 菜单标题 20–24sp（平板栏目 25sp）用内置 Noto Serif SC，略收字距；
  * 小标题 12sp 粗体无衬线、字距 0.08；正文、按钮、信息行、海报名用系统无衬线；播放器内不改字体。
  */
 /** 0.1.133：浅色页面用深色状态栏 / 导航栏图标（暖白底上白色图标看不见）；深色页面恢复白色图标。 */
 static void barIcons(android.app.Activity a,boolean darkIcons){if(a==null||a.getWindow()==null)return;android.view.View d=a.getWindow().getDecorView();
  if(android.os.Build.VERSION.SDK_INT>=30){android.view.WindowInsetsController c=a.getWindow().getInsetsController();if(c!=null){int m=android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;c.setSystemBarsAppearance(darkIcons?m:0,m);}}
  else{int m=android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|(android.os.Build.VERSION.SDK_INT>=26?android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0);int f=d.getSystemUiVisibility();d.setSystemUiVisibility(darkIcons?f|m:f&~m);}}
 static void section(android.widget.TextView t,Context c,float sp){t.setTypeface(paperTitle(c));t.setTextSize(sp);t.setLetterSpacing(-.01f);}
 static Typeface serif(){return Typeface.create(Typeface.SERIF,Typeface.BOLD);}
}
