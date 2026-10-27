package com.guocheng.mediapocket;
final class FullscreenRules {
 /** 0.1.152 只留三种：智能满屏（默认）、完整画面、填满裁切。拉伸、强制 16:9 / 4:3、原始像素删掉，旧设置读到时按智能满屏处理；临时放大用双指缩放。 */
 static final String[] MODES={"smart","fit","crop"};
 static final String[] LABELS={"智能满屏 · 微裁上下加轻微拉宽，铺满全面屏","完整画面 · 不裁切，两边留黑","填满裁切 · 保持比例，裁掉多出的部分"};
 static final float MIN=0.5f,MAX=3f;
 static String mode(String saved,boolean migrated){for(String mode:MODES)if(mode.equals(saved))return mode;return "smart";}
 /**
  * 0.1.152 智能满屏（为一加 13 这类约 2.2:1 的全面屏设计）。在「适应屏幕」的基础上返回 {横向, 纵向} 缩放：
  *  先等比放大，最多裁掉 SMART_CROP（上下或左右各一半）；剩下的差距用单向拉伸补，最多 SMART_STRETCH。
  *  补不满（4:3 这类差距太大的）就保持适应屏幕，不硬拉。
  *  例：16:9 → 2.2:1，等比放大 1.136（上下各裁 6%）、横向再拉 8.9%；2.39:1 电影 → 左右各裁约 4%，不拉伸。
  */
 static final float SMART_CROP=0.12f,SMART_STRETCH=0.10f;
 static float[] smart(int screenW,int screenH,int videoW,int videoH){
  if(screenW<=0||screenH<=0||videoW<=0||videoH<=0)return new float[]{1f,1f};
  double s=(double)screenW/screenH,v=(double)videoW/videoH;if(Math.abs(s/v-1)<0.005)return new float[]{1f,1f};
  boolean narrower=v<s;double need=narrower?s/v:v/s,u=Math.min(need,1/(1-SMART_CROP)),rest=need/u;
  if(rest>1+SMART_STRETCH+1e-6)return new float[]{1f,1f};
  return narrower?new float[]{(float)need,(float)u}:new float[]{(float)u,(float)need};
 }
 static float zoom(String mode,float saved){return saved;}
 /** VLC 布局只认识 fit/crop/stretch/16:9/4:3；原始尺寸在适应屏幕基础上再缩放。 */
 static String layoutMode(String mode){return "crop".equals(mode)?"crop":"fit";}
 /** 循环：智能满屏 → 完整画面 → 填满裁切 → 智能满屏。 */
 static String next(String mode){return MODES[(index(mode)+1)%MODES.length];}
 /** 顶栏比例按钮单击与长按面板同一顺序。 */
 static String quickNext(String mode){return next(mode);}
 /** 点比例按钮只切换模式，不在画面上写名称。null 表示静默。快进、亮度、双击恢复不走这里。 */
 static String cycleHud(String label){return null;}
 static int index(String mode){for(int i=0;i<MODES.length;i++)if(MODES[i].equals(mode))return i;return 0;}
 static float clamp(float v){if(Float.isNaN(v)||Float.isInfinite(v))return 1f;return Math.max(MIN,Math.min(MAX,v));}
 /** 原始像素：视频宽 / 适应屏幕后的宽，最多 1（不超出屏幕）。 */
 static float originalScale(int fitWidth,int videoWidth){if(fitWidth<=0||videoWidth<=0)return 1f;return Math.min(1f,(float)videoWidth/fitWidth);}
 static final float MIN_SPAN=40f;
 static final String[] ICONS={"满","▣","⛶"};
 /** 自由双指缩放：X 由横向指距比、Y 由纵向指距比各自独立决定；指距过小的方向不变。 */
 static float[] axisScale(float spanX,float prevX,float spanY,float prevY){float sx=prevX>=MIN_SPAN&&spanX>=MIN_SPAN?spanX/prevX:1f,sy=prevY>=MIN_SPAN&&spanY>=MIN_SPAN?spanY/prevY:1f;return new float[]{sx,sy};}
 static boolean custom(float x,float y){return Math.abs(x-1f)>0.005f||Math.abs(y-1f)>0.005f;}
 static String zoomLabel(float x,float y){if(!custom(x,y))return "1.0x";return Math.abs(x-y)<0.005f?String.format(java.util.Locale.ROOT,"%.1fx",x):"自定义";}
 static float pipRatio(int w,int h){if(w<=0||h<=0)return 16f/9f;return Math.max(1/2.39f+0.001f,Math.min(2.39f-0.001f,(float)w/h));}
}
