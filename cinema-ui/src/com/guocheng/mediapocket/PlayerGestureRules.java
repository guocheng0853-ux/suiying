package com.guocheng.mediapocket;
import java.util.Locale;
/** 单指手势纯规则：三分区双击、半屏亮度/音量、横向拖动快进快退的映射。双指缩放不经过这里。 */
final class PlayerGestureRules {
 static final int LEFT=0,CENTER=1,RIGHT=2;
 static final int NONE=0,HORIZONTAL=1,VERTICAL=2;
 static final int TAP_SEEK_MS=10000;
 /** 双击分区：左三分之一 / 中间 / 右三分之一。 */
 static int third(float x,float width){if(width<=0)return CENTER;if(x<width/3f)return LEFT;if(x>width*2f/3f)return RIGHT;return CENTER;}
 /** 竖向拖动：左半屏亮度，右半屏音量。 */
 static boolean brightnessSide(float x,float width){return width<=0||x<width/2f;}
 /** 一次手势只锁定一个方向；夹角不明确（比值 < 1.2）时继续等待。 */
 static int axis(float dx,float dy,float slop){float ax=Math.abs(dx),ay=Math.abs(dy);if(ax>=slop&&ax>=ay*1.2f)return HORIZONTAL;if(ay>=slop&&ay>=ax*1.2f)return VERTICAL;return NONE;}
 static int tapDelta(int third){return third==LEFT?-TAP_SEEK_MS:third==RIGHT?TAP_SEEK_MS:0;}
 /** 横向滑满整个屏幕对应的时长：片长 / 20，限制在 60 秒～3 分钟，且不超过片长。 */
 static int seekSpan(int durationMs){if(durationMs<=0)return 0;return Math.min(durationMs,Math.max(60000,Math.min(180000,durationMs/20)));}
 static int clampSeek(long target,int durationMs){if(durationMs<=0)return (int)Math.max(0,target);long max=Math.max(0,durationMs-1000L);return (int)Math.max(0,Math.min(max,target));}
 /** 从按下时的位置 start 开始，按横向位移 dx（右为正）求目标位置。 */
 static int seekTarget(int startMs,int durationMs,float dx,float width){if(width<=0||durationMs<=0)return clampSeek(startMs,durationMs);long delta=Math.round(dx/width*seekSpan(durationMs));return clampSeek(startMs+delta,durationMs);}
 static String time(long ms){long s=Math.max(0,ms)/1000,h=s/3600,m=s/60%60,sec=s%60;return h>0?String.format(Locale.ROOT,"%d:%02d:%02d",h,m,sec):String.format(Locale.ROOT,"%d:%02d",m,sec);}
 static String seekLabel(int targetMs,int startMs,int durationMs){int d=targetMs-startMs;return (d<0?"⏪ -":"⏩ +")+time(Math.abs(d))+"\n"+time(targetMs)+" / "+time(durationMs);}
 static float brightness(float initial,float fraction){float b=(initial<0?0.5f:initial)+fraction;return Math.max(0.01f,Math.min(1f,b));}
 static int volume(int initial,int max,float fraction){if(max<=0)return 0;return Math.max(0,Math.min(max,Math.round(initial+fraction*max)));}
}
