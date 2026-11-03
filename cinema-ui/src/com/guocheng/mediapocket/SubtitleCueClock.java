package com.guocheng.mediapocket;
/** 按播放进度和字幕偏移挑出当前该显示的文本。正偏移表示字幕更晚出现，和 VLC 的 spu delay 同一方向。 */
final class SubtitleCueClock {
 static final class Cue {
  final int start,end; final String text;
  Cue(int start,int end,String text){this.start=start;this.end=end;this.text=text==null?"":text;}
 }
 /** 一句字幕到达后，按偏移单独排期。后一句不会取消前一句。正偏移才推迟，负偏移当场显示。 */
 static final class Delayed {final long showAt;final String text;Delayed(long showAt,String text){this.showAt=showAt;this.text=text==null?"":text;}}
 static Delayed schedule(long arrivedAtMs,int offsetMs,String text){return new Delayed(arrivedAtMs+(offsetMs>0?offsetMs:0L),text);}
 /** 已经到点的事件里，取最晚的一条。空字符串是合法的清屏，不会把还没到点的句子抹掉。 */
 static String showing(Delayed[] events,long nowMs){if(events==null)return "";String text="";long best=Long.MIN_VALUE;boolean any=false;for(Delayed e:events){if(e==null||e.showAt>nowMs)continue;if(!any||e.showAt>=best){any=true;best=e.showAt;text=e.text;}}return text;}
 /** 只留最近一条已经显示的，加上还没到点的。播放中每来一条就剪一次。 */
 static Delayed[] trim(Delayed[] events,long nowMs){if(events==null||events.length==0)return new Delayed[0];Delayed latest=null;java.util.ArrayList<Delayed> future=new java.util.ArrayList<>();for(Delayed e:events){if(e==null)continue;if(e.showAt>nowMs)future.add(e);else if(latest==null||e.showAt>=latest.showAt)latest=e;}if(latest!=null)future.add(0,latest);return future.toArray(new Delayed[0]);}
 static String textAt(Cue[] cues,int positionMs,int offsetMs){
  if(cues==null||positionMs<0)return "";
  StringBuilder out=new StringBuilder();
  for(Cue cue:cues){
   if(cue==null||cue.text.isEmpty())continue;
   long start=(long)cue.start+offsetMs,end=(long)cue.end+offsetMs;
   if(end<start)continue;
   if(positionMs>=start&&positionMs<end){if(out.length()>0)out.append('\n');out.append(cue.text);}
  }
  return out.toString();
 }
}
