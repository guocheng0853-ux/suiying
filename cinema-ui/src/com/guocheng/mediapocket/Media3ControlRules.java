package com.guocheng.mediapocket;
/** Dimensions use the same aspect mode names as the VLC page. */
final class Media3ControlRules {
 static int[] box(int parentW,int parentH,int videoW,int videoH,String mode){
  return box(parentW,parentH,videoW,videoH,mode,0);
 }
 static int[] box(int parentW,int parentH,int videoW,int videoH,String mode,int smallestWidthDp){
  if(parentW<=0||parentH<=0)return new int[]{0,0};
  if(videoW<=0||videoH<=0||"stretch".equals(mode))return new int[]{parentW,parentH};
  if("original".equals(mode)){double scale=Math.min(1,Math.min(parentW/(double)videoW,parentH/(double)videoH));return new int[]{Math.max(1,(int)Math.round(videoW*scale)),Math.max(1,(int)Math.round(videoH*scale))};}
  if("smart".equals(mode)){int[] fit=box(parentW,parentH,videoW,videoH,"fit");float[] f=FullscreenRules.smart(parentW,parentH,videoW,videoH,smallestWidthDp);return new int[]{Math.max(1,Math.round(fit[0]*f[0])),Math.max(1,Math.round(fit[1]*f[1]))};}
  double ratio="16:9".equals(mode)?16.0/9:"4:3".equals(mode)?4.0/3:videoW/(double)videoH;
  boolean crop="crop".equals(mode);double width=parentW,height=width/ratio;
  if(crop?height<parentH:height>parentH){height=parentH;width=height*ratio;}
  return new int[]{Math.max(1,(int)Math.round(width)),Math.max(1,(int)Math.round(height))};
 }
 /**
  * What Media3 actually applies: layout width, layout height, scaleX, scaleY (before pinch zoom).
  * Tablet smart keeps an aspect-correct fit surface and stretches it with independent scales.
  * Putting the stretch into the surface size would letterbox inside the decoder. Phone smart still
  * bakes its capped scale into the surface size and leaves the view scale at 1.
  */
 static float[] place(int parentW,int parentH,int videoW,int videoH,String mode,int smallestWidthDp){
  boolean stretch=FullscreenRules.tabletStretch(mode,smallestWidthDp)&&parentW>0&&parentH>0&&videoW>0&&videoH>0;
  int[] size=stretch?box(parentW,parentH,videoW,videoH,"fit"):box(parentW,parentH,videoW,videoH,mode,smallestWidthDp);
  float sx=1f,sy=1f;
  if(stretch){float[] f=FullscreenRules.smart(parentW,parentH,videoW,videoH,smallestWidthDp);sx=f[0];sy=f[1];}
  return new float[]{size[0],size[1],sx,sy};
 }
 static int seek(int current,int duration,int delta){long target=(long)current+delta;return (int)Math.max(0,Math.min(duration>0?duration:Integer.MAX_VALUE,target));}
}
