package com.guocheng.mediapocket;
/** Dimensions use the same aspect mode names as the VLC page. */
final class Media3ControlRules {
 static int[] box(int parentW,int parentH,int videoW,int videoH,String mode){
  if(parentW<=0||parentH<=0)return new int[]{0,0};
  if(videoW<=0||videoH<=0||"stretch".equals(mode))return new int[]{parentW,parentH};
  if("original".equals(mode)){double scale=Math.min(1,Math.min(parentW/(double)videoW,parentH/(double)videoH));return new int[]{Math.max(1,(int)Math.round(videoW*scale)),Math.max(1,(int)Math.round(videoH*scale))};}
  double ratio="16:9".equals(mode)?16.0/9:"4:3".equals(mode)?4.0/3:videoW/(double)videoH;
  boolean crop="crop".equals(mode);double width=parentW,height=width/ratio;
  if(crop?height<parentH:height>parentH){height=parentH;width=height*ratio;}
  return new int[]{Math.max(1,(int)Math.round(width)),Math.max(1,(int)Math.round(height))};
 }
 static int seek(int current,int duration,int delta){long target=(long)current+delta;return (int)Math.max(0,Math.min(duration>0?duration:Integer.MAX_VALUE,target));}
}
