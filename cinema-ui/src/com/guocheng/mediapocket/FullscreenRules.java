package com.guocheng.mediapocket;
final class FullscreenRules {
 static final String[] MODES={"fit","crop","stretch","16:9","4:3","original"};
 static final String[] LABELS={"适应屏幕 · 完整画面，不裁切","填满屏幕 · 保持比例，裁切边缘","拉伸 · 铺满屏幕，画面变形","16:9","4:3","原始 · 视频原始像素大小"};
 static final float MIN=0.5f,MAX=3f;
 static String mode(String saved,boolean migrated){if(!migrated)return "stretch";for(String mode:MODES)if(mode.equals(saved))return mode;return "stretch";}
 static float zoom(String mode,float saved){return "stretch".equals(mode)?1f:saved;}
 /** VLC 布局只认识 fit/crop/stretch/16:9/4:3；原始尺寸在适应屏幕基础上再缩放。 */
 static String layoutMode(String mode){return "original".equals(mode)?"fit":mode;}
 /** 单按钮循环：适应→填满→拉伸→16:9→4:3→原始→适应。 */
 static String next(String mode){return MODES[(index(mode)+1)%MODES.length];}
 /** 点比例按钮只切换模式，不在画面上写名称。null 表示静默。快进、亮度、双击恢复不走这里。 */
 static String cycleHud(String label){return null;}
 static int index(String mode){for(int i=0;i<MODES.length;i++)if(MODES[i].equals(mode))return i;return 0;}
 static float clamp(float v){if(Float.isNaN(v)||Float.isInfinite(v))return 1f;return Math.max(MIN,Math.min(MAX,v));}
 /** 原始像素：视频宽 / 适应屏幕后的宽，最多 1（不超出屏幕）。 */
 static float originalScale(int fitWidth,int videoWidth){if(fitWidth<=0||videoWidth<=0)return 1f;return Math.min(1f,(float)videoWidth/fitWidth);}
 static final float MIN_SPAN=40f;
 static final String[] ICONS={"▣","⛶","↔","16:9","4:3","1:1"};
 /** 自由双指缩放：X 由横向指距比、Y 由纵向指距比各自独立决定；指距过小的方向不变。 */
 static float[] axisScale(float spanX,float prevX,float spanY,float prevY){float sx=prevX>=MIN_SPAN&&spanX>=MIN_SPAN?spanX/prevX:1f,sy=prevY>=MIN_SPAN&&spanY>=MIN_SPAN?spanY/prevY:1f;return new float[]{sx,sy};}
 static boolean custom(float x,float y){return Math.abs(x-1f)>0.005f||Math.abs(y-1f)>0.005f;}
 static String zoomLabel(float x,float y){if(!custom(x,y))return "1.0x";return Math.abs(x-y)<0.005f?String.format(java.util.Locale.ROOT,"%.1fx",x):"自定义";}
 static float pipRatio(int w,int h){if(w<=0||h<=0)return 16f/9f;return Math.max(1/2.39f+0.001f,Math.min(2.39f-0.001f,(float)w/h));}
}
