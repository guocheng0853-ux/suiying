package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.Typeface;
/**
 * 0.1.125「影院暗金」外观：纯黑底、暖白字、金色强调，片名用衬线体（参考 Infuse / Apple TV）。
 * 主页在 onCreate 里把是否启用写进 data/ui.gold，Emby、影视仓、下载页读同一个值。
 */
final class CinemaTheme {
 private CinemaTheme(){}
 static final int BG=0xff0b0b0c,CARD=0xff19191c,INK=0xfff4f1ea,MUTED=0xffa8a397,SOFT=0xffd8d2c4,GOLD=0xffe0b25c,ON_GOLD=0xff1a1408,CHIP=0xff2e2920,TILE_A=0xff1d1c1a,TILE_B=0xff252320;
 static boolean gold(Context c){return c.getSharedPreferences("data",0).getBoolean("ui.gold",false);}
 static Typeface serif(){return Typeface.create(Typeface.SERIF,Typeface.BOLD);}
}
