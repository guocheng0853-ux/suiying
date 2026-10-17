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
 /** 双击之后继续点的累计窗口。 */
 static final long BURST_MS=1300L;
 /** 锁屏后单击、双击、拖动、长按都不再生效。 */
 static boolean gesturesOpen(boolean locked){return !locked;}
 /** 双击左右后的短暂提示。累计值直接写成 +30s / -20s。 */
 static String tapHint(int deltaMs){int sec=(int)Math.round(Math.abs(deltaMs)/1000.0);return (deltaMs<0?"-":"+")+sec+"s";}
 /**
  * 同一侧在 1.3 秒内继续点就累加 10 秒。中间区域、过期或换边都重新从这一下开始。
  * previousDelta 为 0 表示当前没有在累计。
  */
 static int burstDelta(int zone,int previousZone,int previousDelta,long elapsedMs){
  int step=tapDelta(zone);if(step==0)return 0;
  boolean live=previousDelta!=0&&zone==previousZone&&elapsedMs>=0&&elapsedMs<=BURST_MS;
  return live?previousDelta+step:step;
 }
 /** 横向滑满整个屏幕对应片长的四分之一。 */
 static int seekSpan(int durationMs){if(durationMs<=0)return 0;long span=durationMs/4L;return span>Integer.MAX_VALUE?Integer.MAX_VALUE:(int)span;}
 static int clampSeek(long target,int durationMs){if(durationMs<=0)return (int)Math.max(0,target);long max=Math.max(0,durationMs-1000L);return (int)Math.max(0,Math.min(max,target));}
 /** 从按下时的位置 start 开始，按横向位移 dx（右为正）求目标位置。 */
 static int seekTarget(int startMs,int durationMs,float dx,float width){if(width<=0||durationMs<=0)return clampSeek(startMs,durationMs);long delta=Math.round(dx/width*seekSpan(durationMs));return clampSeek(startMs+delta,durationMs);}
 static String time(long ms){long s=Math.max(0,ms)/1000,h=s/3600,m=s/60%60,sec=s%60;return h>0?String.format(Locale.ROOT,"%d:%02d:%02d",h,m,sec):String.format(Locale.ROOT,"%d:%02d",m,sec);}
 static String seekLabel(int targetMs,int startMs,int durationMs){int d=targetMs-startMs;return (d<0?"⏪ -":"⏩ +")+time(Math.abs(d))+"\n"+time(targetMs)+" / "+time(durationMs);}
 static float brightness(float initial,float fraction){float b=(initial<0?0.5f:initial)+fraction;return Math.max(0.01f,Math.min(1f,b));}
 static int volume(int initial,int max,float fraction){if(max<=0)return 0;return Math.max(0,Math.min(max,Math.round(initial+fraction*max)));}
 /** 亮度：位移按短边归一化后再乘 2/3，结果限制在 0–1。dy 向上为正。 */
 static float brightnessSwipe(float initial,float dy,float minSide){
  float base=initial<0f?0.5f:initial;float norm=minSide>0f?dy/minSide:0f;
  float b=base+norm*(2f/3f);if(b<0f)b=0f;if(b>1f)b=1f;return b;
 }
 /** 音量调节结果。percent 是相对系统最大音量的百分比，允许到 200；boost 交给 LibVLC setVolume。 */
 static final class VolumeAdjust {final int system,boost;final float percent;VolumeAdjust(int system,int boost,float percent){this.system=system;this.boost=boost;this.percent=percent;}}
 /**
  * 竖滑音量。满一条短边等于系统最大音量的 100%。
  * 系统档位不足 100 时按 10 倍细分，滑动更细。
  * allowBoost 为真（按下时已经是系统最大）才允许超过 100，直到 200。
  */
 static VolumeAdjust volumeSwipe(float startPercent,float dy,float minSide,int systemMax,boolean allowBoost){
  if(systemMax<=0)return new VolumeAdjust(0,0,0f);
  float span=minSide>0f?minSide:1f;
  float next=startPercent+(dy/span)*100f;
  float cap=allowBoost?200f:100f;
  if(next<0f)next=0f;if(next>cap)next=cap;
  int steps=systemMax<100?systemMax*10:systemMax;if(steps<1)steps=1;
  float quantum=100f/steps;
  next=Math.round(next/quantum)*quantum;
  if(next<0f)next=0f;if(next>cap)next=cap;
  int system=Math.round(Math.min(100f,next)/100f*systemMax);
  if(system<0)system=0;if(system>systemMax)system=systemMax;
  int boost=next<=100f?100:Math.round(next);if(boost>200)boost=200;if(boost<0)boost=0;
  return new VolumeAdjust(system,boost,next);
 }
}
