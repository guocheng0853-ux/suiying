package com.guocheng.mediapocket;

/**
 * 首页底栏：手机展开胶囊，平板横屏左缘侧轨。
 * 数值跟方向三稿（dir-3）：胶囊 64 / 圆角 32，侧轨宽 80，内容左缩 112。
 * 不引用 Android，JVM 测试可以直接锁规则。
 */
final class BottomNavRules {
 private BottomNavRules(){}

 /** 宽屏门槛。平板横屏才上侧轨；手机横屏宽度够、shortest 边不够，仍用胶囊。 */
 static final int RAIL_MIN_DP=600;
 static final int PILL_HEIGHT_DP=64;
 static final int PILL_RADIUS_DP=32;
 static final int PILL_PAD_DP=7;
 static final int PILL_ITEM_MIN_DP=50;
 static final int PILL_ITEM_RADIUS_DP=25;
 static final int PILL_PAD_H_DP=13;
 static final int PILL_SEL_PAD_START_DP=15;
 static final int PILL_SEL_PAD_END_DP=20;
 static final int LABEL_GAP_DP=9;
 static final int NAV_PAD_TOP_DP=18;
 static final int NAV_PAD_H_DP=12;
 static final int NAV_PAD_BOTTOM_DP=8;
 static final int RAIL_WIDTH_DP=80;
 static final int RAIL_PAD_DP=8;
 static final int RAIL_LEFT_DP=20;
 static final int RAIL_RADIUS_DP=40;
 static final int RAIL_ITEM_HEIGHT_DP=70;
 static final int RAIL_ITEM_RADIUS_DP=32;
 static final int RAIL_GAP_DP=4;
 static final int RAIL_LABEL_GAP_DP=3;
 static final int CONTENT_LEFT_DP=112;
 static final int PAGE_INSET_DP=16;
 static final int DETAIL_BOTTOM_DP=24;
 /** 胶囊高度 + 底边距 + 一行呼吸，滚到底时最后一行停在胶囊上方。 */
 static final int PILL_CLEARANCE_DP=PILL_HEIGHT_DP+NAV_PAD_BOTTOM_DP+16;
 static final int ICON_DP=24;
 static final int PHONE_LABEL_SP=14;
 static final int RAIL_LABEL_SP=12;
 static final int EXPAND_MS=320;
 static final int COLOR_MS=160;
 static final float EASE_X1=.2f,EASE_Y1=0f,EASE_X2=0f,EASE_Y2=1f;
 static final float FADE_MID=0.45f,FADE_MID_ALPHA=0.86f;

 static final int NIGHT_PAGE=0xff0b0b0c,NIGHT_SURF=0xff141416,NIGHT_ACCENT=0xffe0b25c,NIGHT_ON_ACCENT=0xff1a1408,NIGHT_MUTED=0xffa8a397,NIGHT_EDGE=0x21f4f1ea,NIGHT_FOCUS=0xffe0b25c,NIGHT_SHADOW=0x80000000;
 static final int DAY_PAGE=0xfffaf9f5,DAY_SURF=0xfffaf9f5,DAY_ACCENT=0xffd97757,DAY_ON_ACCENT=0xff141413,DAY_MUTED=0xff45423c,DAY_EDGE=0x8ca58b74,DAY_FOCUS=0xffb5573a,DAY_SHADOW=0x383c2814;
 /**
  * 浅陶土底 #F1E6DE 上的字色。方向三选中态是实心陶土块，字用墨色才能过 4.5:1；
  * #9B4A2F 和旧的 #B5573A 都只适合浅底，压在 #D97757 上不够。
  */
 static final int DAY_CHIP=0xfff1e6de,DAY_CHIP_TEXT=0xff9b4a2f,DAY_CLAY_DEEP=0xffb5573a;

 /**
  * 侧轨：横屏，且当前窗口宽 ≥600dp，并且设备最短边也 ≥600dp。
  * 分屏把窗口收窄到 600 以下时回到胶囊。最短边未知时，要求窗口高度也 ≥600，避免手机横屏误判。
  */
 static boolean useRail(int windowWidthDp,int windowHeightDp,int smallestWidthDp){
  if(windowWidthDp<=windowHeightDp)return false;
  if(windowWidthDp<RAIL_MIN_DP)return false;
  if(smallestWidthDp>=RAIL_MIN_DP)return true;
  return smallestWidthDp<=0&&windowHeightDp>=RAIL_MIN_DP;
 }
 /** 旋转、分屏只换形态，不改当前页。越界时回到首页。 */
 static int keptIndex(int index,int count){if(count<=0)return 0;if(index<0||index>=count)return 0;return index;}
 /** 胶囊未选中只剩图标，侧轨始终有字。 */
 static boolean labelVisible(boolean rail,boolean selected){return rail||selected;}
 static int labelSp(boolean rail){return rail?RAIL_LABEL_SP:PHONE_LABEL_SP;}
 static int contentLeftDp(boolean railVisible){return railVisible?CONTENT_LEFT_DP:PAGE_INSET_DP;}
 static int contentBottomDp(boolean railVisible,boolean detail){if(detail||railVisible)return DETAIL_BOTTOM_DP;return PILL_CLEARANCE_DP;}
 static int pillBottomMarginDp(int safeBottomDp){return safeBottomDp+NAV_PAD_BOTTOM_DP;}
 static int fadeHeightDp(int safeBottomDp){return NAV_PAD_TOP_DP+PILL_HEIGHT_DP+NAV_PAD_BOTTOM_DP+safeBottomDp;}
 static int railLeftDp(int safeLeftDp){return safeLeftDp+RAIL_LEFT_DP;}
 /** 侧轨在状态栏和导航栏之间垂直居中；放不下时贴着安全区上沿，不画进状态栏。 */
 static int railTop(int parentHeight,int railHeight,int safeTop,int safeBottom){
  int space=parentHeight-safeTop-safeBottom;
  if(space<=railHeight)return safeTop;
  return safeTop+(space-railHeight)/2;
 }
 static int pillItemMinDp(){return PILL_ITEM_MIN_DP;}
 static int railItemWidthDp(){return RAIL_WIDTH_DP-2*RAIL_PAD_DP;}
 static int railItemHeightDp(){return RAIL_ITEM_HEIGHT_DP;}
 static int page(boolean day){return day?DAY_PAGE:NIGHT_PAGE;}
 static int surface(boolean day){return day?DAY_SURF:NIGHT_SURF;}
 static int accent(boolean day){return day?DAY_ACCENT:NIGHT_ACCENT;}
 static int onAccent(boolean day){return day?DAY_ON_ACCENT:NIGHT_ON_ACCENT;}
 static int muted(boolean day){return day?DAY_MUTED:NIGHT_MUTED;}
 static int edge(boolean day){return day?DAY_EDGE:NIGHT_EDGE;}
 static int focus(boolean day){return day?DAY_FOCUS:NIGHT_FOCUS;}
 static int shadow(boolean day){return day?DAY_SHADOW:NIGHT_SHADOW;}
 static int fadeColor(int pageRgb,float alpha){
  float u=alpha<0?0:alpha>1?1:alpha;
  return (Math.round(u*255f)<<24)|(pageRgb&0xffffff);
 }
 /** 系统「移除动画」会把这三档时长一起拨到 0。 */
 static boolean motionEnabled(float animatorScale,float transitionScale,float windowScale){
  return animatorScale>0f&&transitionScale>0f&&windowScale>0f;
 }
 static int expandMs(boolean motion){return motion?EXPAND_MS:0;}
}
