package com.guocheng.mediapocket;
/** 按播放进度和字幕偏移挑出当前该显示的文本。正偏移表示字幕更晚出现，和 VLC 的 spu delay 同一方向。 */
final class SubtitleCueClock {
 static final class Cue {
  final int start,end; final String text;
  Cue(int start,int end,String text){this.start=start;this.end=end;this.text=text==null?"":text;}
 }
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
