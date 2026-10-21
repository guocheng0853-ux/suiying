package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.Typeface;
/**
 * 0.1.125「影院暗金」外观：纯黑底、暖白字、金色强调，片名用衬线体（参考 Infuse / Apple TV）。
 * 主页在 onCreate 里把是否启用写进 data/ui.gold，Emby、影视仓、下载页读同一个值。
 */
final class CinemaTheme {
 private CinemaTheme(){}
 static final int BG=0xff0b0b0c,CARD=0xff19191c,INK=0xfff4f1ea,MUTED=0xffa8a397,SOFT=0xffd8d2c4,GOLD=0xffe0b25c,ON_GOLD=0xff1a1408,CHIP=0xff2e2920,TILE_A=0xff1d1c1a,TILE_B=0xff252320;
 /** 0.1.128「暖白陶土」：暖白纸色底、墨色字、陶土橙强调，标题衬线、正文无衬线，留白多、不用渐变（参考 Anthropic 的设计取向，未用其字体与标识）。 */
 static final int PAPER=0xfffaf9f5,SHEET=0xfff0eee6,SLATE=0xff141413,STONE=0xff5e5d59,CLAY=0xffd97757,CLAY_DEEP=0xffb5573a,CLAY_CHIP=0xffefe6dc,LINE=0xffd9d4c7,TILE_C=0xffe9e5dc,TILE_D=0xffe2ddd2;
 static boolean clay(Context c){return c.getSharedPreferences("data",0).getBoolean("ui.clay",false);}
 static boolean gold(Context c){return c.getSharedPreferences("data",0).getBoolean("ui.gold",false);}
 static Typeface serif(){return Typeface.create(Typeface.SERIF,Typeface.BOLD);}
}
