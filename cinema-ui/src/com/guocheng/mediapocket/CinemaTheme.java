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
 static final int PAPER=0xfffaf9f5,SHEET=0xfffffefa,SLATE=0xff141413,STONE=0xff68665f,CLAY=0xffd97757,CLAY_DEEP=0xffb5573a,CLAY_CHIP=0xfff1e6de,LINE=0xffdedbd2,TILE_C=0xffe9e5dc,TILE_D=0xffe2ddd2;
 static boolean clay(Context c){return c.getSharedPreferences("data",0).getBoolean("ui.clay",false);}
 static boolean gold(Context c){return c.getSharedPreferences("data",0).getBoolean("ui.gold",false);}
 private static volatile Typeface paperFace;
 /** Bundled Chinese serif; load once so titles do not depend on an OEM's serif fallback. */
 static Typeface paperTitle(Context c){
  Typeface face=paperFace;if(face!=null)return face;
  synchronized(CinemaTheme.class){
   if(paperFace==null){try{paperFace=Typeface.createFromAsset(c.getApplicationContext().getAssets(),"fonts/NotoSerifSC-Regular.otf");}catch(RuntimeException missing){paperFace=Typeface.create(Typeface.SERIF,Typeface.NORMAL);}}
   return paperFace;
  }
 }
 static Typeface serif(){return Typeface.create(Typeface.SERIF,Typeface.BOLD);}
}
