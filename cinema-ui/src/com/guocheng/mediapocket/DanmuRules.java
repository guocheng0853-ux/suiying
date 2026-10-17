package com.guocheng.mediapocket;
import java.util.*;
/** 弹幕配置与自绘布局。不访问网络。 */
final class DanmuRules {
 static final int SCROLL=1,BOTTOM=4,TOP=5;
 static final class Config {
  final boolean shown;final int opacity,textSp,topLines,scrollLines,bottomLines;final float speedSec;final String block,match;final boolean remember;
  Config(boolean shown,int opacity,float speedSec,int textSp,int topLines,int scrollLines,int bottomLines,String block,String match,boolean remember){this.shown=shown;this.opacity=clamp(opacity,10,100);this.speedSec=speedSec<4f?4f:speedSec>20f?20f:speedSec;this.textSp=clamp(textSp,12,32);this.topLines=clamp(topLines,0,8);this.scrollLines=clamp(scrollLines,0,16);this.bottomLines=clamp(bottomLines,0,8);this.block=block==null?"":block;this.match="manual".equals(match)?"manual":"auto";this.remember=remember;}
  static Config defaults(){return new Config(true,80,8f,16,3,8,3,"","auto",true);}
 }
 static int clamp(int v,int min,int max){return v<min?min:v>max?max:v;}
 static int alpha(int opacity){return clamp(opacity,0,100)*255/100;}
 static boolean blocked(String text,String words){if(text==null||text.isEmpty())return true;if(words==null||words.trim().isEmpty())return false;String hay=text.toLowerCase(Locale.ROOT);for(String part:words.split("[,，;；\\n\\r]+")){String w=part.trim().toLowerCase(Locale.ROOT);if(!w.isEmpty()&&hay.contains(w))return true;}return false;}
 static final class Draw {final String text;final float x,y;final int color,mode;Draw(String text,float x,float y,int color,int mode){this.text=text;this.x=x;this.y=y;this.color=color;this.mode=mode;}}
 interface Measure {float width(String text);}
 /**
  * 当前时刻要画的弹幕。滚动从右向左，顶部和底部各占自己的行数上限，超出的丢掉。
  * speedSec 是一条滚动弹幕穿过画面的秒数。
  */
 static List<Draw> layout(List<DanDanPlayRules.Cue> cues,long nowMs,int width,int height,float textPx,Measure measure,Config config){
  ArrayList<Draw> out=new ArrayList<>();if(cues==null||config==null||!config.shown||width<8||height<8)return out;
  float line=Math.max(textPx*1.35f,textPx+4f);long travel=Math.round(config.speedSec*1000f);long hold=4000L;
  long[] topFree=new long[Math.max(0,config.topLines)],bottomFree=new long[Math.max(0,config.bottomLines)],scrollFree=new long[Math.max(0,config.scrollLines)];
  float[] scrollWidth=new float[scrollFree.length];
  for(DanDanPlayRules.Cue cue:cues){if(cue==null||blocked(cue.text,config.block))continue;long start=cue.timeMs;int mode=cue.mode==TOP?TOP:cue.mode==BOTTOM?BOTTOM:SCROLL;long life=mode==SCROLL?travel:hold;if(nowMs<start||nowMs>=start+life)continue;float tw=measure==null?textPx*cue.text.length():Math.max(textPx,measure.width(cue.text));
   if(mode==TOP){int lane=lane(topFree,start);if(lane<0)continue;topFree[lane]=start+hold;out.add(new Draw(cue.text,(width-tw)/2f,line*(lane+1),cue.color,TOP));}
   else if(mode==BOTTOM){int lane=lane(bottomFree,start);if(lane<0)continue;bottomFree[lane]=start+hold;out.add(new Draw(cue.text,(width-tw)/2f,height-line*(config.bottomLines-lane)+textPx*0.2f,cue.color,BOTTOM));}
   else {int lane=scrollLane(scrollFree,scrollWidth,start,tw,width,travel);if(lane<0)continue;float span=width+tw;float moved=(nowMs-start)/(float)travel*span;out.add(new Draw(cue.text,width-moved,line*(config.topLines+lane+1),cue.color,SCROLL));}
   if(out.size()>=80)break;}
  return out;
 }
 static int lane(long[] free,long start){for(int i=0;i<free.length;i++)if(free[i]<=start)return i;return -1;}
 /** 同一行上一条还没完全让出右侧入口时，不叠第二条。 */
 static int scrollLane(long[] free,float[] widths,long start,float textWidth,int viewWidth,long travel){for(int i=0;i<free.length;i++){if(free[i]<=start){free[i]=start+clearTime(textWidth,viewWidth,travel);widths[i]=textWidth;return i;}}return -1;}
 static long clearTime(float textWidth,int viewWidth,long travel){float span=viewWidth+textWidth;if(span<=1f)return travel;return Math.max(1L,Math.round(travel*textWidth/span));}
}
