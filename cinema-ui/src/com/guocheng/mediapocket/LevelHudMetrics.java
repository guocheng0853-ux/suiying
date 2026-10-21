package com.guocheng.mediapocket;
/** 音量 / 亮度药丸的尺寸和安全区位置。横竖屏都避开挖孔和系统栏。 */
final class LevelHudMetrics {
 static final int PAD_H=14,PAD_V=10,ICON=22,BAR_H=6,GAP=10,TEXT_W=48,WIDTH=208,HEIGHT=48,RADIUS=24;
 static final int FG=0xFFFFFFFF,TRACK=0x33FFFFFF,FILL=0xFFFFFFFF,BOOST=0xFFFFB74D,PILL=0xF21A1C22,STROKE=0x66FFFFFF;
 static final int FADE_IN_MS=160,FADE_OUT_MS=280,HOLD_MS=1000;
 private LevelHudMetrics(){}
 static int top(float density,int cutTop,boolean portrait){float d=density<=0f?1f:density;return Math.max(0,cutTop)+Math.round((portrait?28:16)*d);}
 static int side(float density,int cut){float d=density<=0f?1f:density;return Math.max(Math.round(16*d),Math.max(0,cut)+Math.round(8*d));}
 /** 药丸左边缘：在去掉左右切口后的安全区里水平居中。 */
 static int x(int hostW,int pillW,float density,int cutL,int cutR){int left=side(density,cutL),right=side(density,cutR);int safe=Math.max(0,hostW-left-right);return left+Math.max(0,(safe-pillW)/2);}
}
