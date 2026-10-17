package com.guocheng.mediapocket;
/**
 * 横屏控件的位置。左侧截图和锁屏同一条中线、成组垂直居中；
 * 倍速药丸的右边距等于左边距；底栏三个键同一条基线、间距相同；顶栏右侧同一高度。
 */
final class PlayerLayoutMetrics {
 static final int TOUCH_DP=40,ICON_DP=24,PILL_W_DP=40,PILL_H_DP=120,PILL_RADIUS_DP=8;
 static final int SEEK_DP=3,SEEK_DRAG_DP=4,THUMB_DP=12;
 static final int TRANSPORT_GAP_DP=12,SIDE_GAP_DP=16,MARGIN_DP=16,TOP_H_DP=40,TOP_GAP_DP=4;
 static final int PILL_COLOR=0x80C8C8D0;
 private PlayerLayoutMetrics(){}
 static int dp(float density,int v){float d=density<=0f?1f:density;return Math.round(v*d);}
 /** 左右用同一个边距，并且都不小于切口。 */
 static int sideMargin(float density,int cutLeft,int cutRight){int base=dp(density,MARGIN_DP);int left=Math.max(base,Math.max(0,cutLeft)+dp(density,8));int right=Math.max(base,Math.max(0,cutRight)+dp(density,8));return Math.max(left,right);}
 static final class Box {
  final int x,y,w,h;
  Box(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;}
  int cx(){return x+w/2;}
  int cy(){return y+h/2;}
  int right(){return x+w;}
  int bottom(){return y+h;}
 }
 static final class Overlay {
  final int margin,touch,gap,seekTrack,thumb;
  final Box camera,lock,speed,prev,play,next,audio,cc,episodes,back,decoder,aspect,pip,more,status;
  Overlay(int margin,int touch,int gap,int seekTrack,int thumb,Box camera,Box lock,Box speed,Box prev,Box play,Box next,Box audio,Box cc,Box episodes,Box back,Box decoder,Box aspect,Box pip,Box more,Box status){
   this.margin=margin;this.touch=touch;this.gap=gap;this.seekTrack=seekTrack;this.thumb=thumb;this.camera=camera;this.lock=lock;this.speed=speed;this.prev=prev;this.play=play;this.next=next;this.audio=audio;this.cc=cc;this.episodes=episodes;this.back=back;this.decoder=decoder;this.aspect=aspect;this.pip=pip;this.more=more;this.status=status;
  }
 }
 static Overlay measure(int width,int height,float density,int cutL,int cutT,int cutR,int cutB,boolean prevOn,boolean nextOn,boolean episodesOn,boolean dragging,int topBottom,int bottomTop){
  int margin=sideMargin(density,cutL,cutR),touch=dp(density,TOUCH_DP),gap=dp(density,TRANSPORT_GAP_DP),sideGap=dp(density,SIDE_GAP_DP);
  int topY=Math.max(dp(density,8),Math.max(0,cutT)+dp(density,4)),topH=dp(density,TOP_H_DP);
  int gapTop=topBottom>0?topBottom:topY+topH;
  int bottomEdge=height-Math.max(dp(density,8),Math.max(0,cutB)+dp(density,8));
  int contentBottom=bottomTop>0?bottomTop:bottomEdge-dp(density,112);
  if(contentBottom<gapTop+touch)contentBottom=Math.min(height,gapTop+touch*3);
  int stack=touch*2+sideGap;
  int stackTop=gapTop+Math.max(0,(contentBottom-gapTop-stack)/2);
  Box camera=new Box(margin,stackTop,touch,touch);
  Box lock=new Box(margin,stackTop+touch+sideGap,touch,touch);
  int pillW=dp(density,PILL_W_DP),pillH=dp(density,PILL_H_DP);
  int speedY=gapTop+Math.max(0,(contentBottom-gapTop-pillH)/2);
  Box speed=new Box(width-margin-pillW,speedY,pillW,pillH);
  int ty=bottomEdge-touch;
  int x=margin;
  Box prev=prevOn?new Box(x,ty,touch,touch):null;if(prevOn)x+=touch+gap;
  Box play=new Box(x,ty,touch,touch);x+=touch+gap;
  Box next=nextOn?new Box(x,ty,touch,touch):null;
  int rx=width-margin-touch;
  Box episodes=episodesOn?new Box(rx,ty,touch,touch):null;if(episodesOn)rx-=touch+gap;
  Box cc=new Box(rx,ty,touch,touch);rx-=touch+gap;
  Box audio=new Box(rx,ty,touch,touch);
  int tg=dp(density,TOP_GAP_DP),cursor=width-margin-touch;
  Box more=new Box(cursor,topY,touch,topH);cursor-=tg+touch;
  Box pip=new Box(cursor,topY,touch,topH);cursor-=tg+touch;
  Box aspect=new Box(cursor,topY,touch,topH);
  int decW=dp(density,48);cursor-=tg+decW;
  Box decoder=new Box(cursor,topY,decW,topH);
  Box back=new Box(margin,topY,touch,topH);
  int sw=dp(density,180),sh=dp(density,32);
  Box status=new Box((width-sw)/2,(height-sh)/2,sw,sh);
  return new Overlay(margin,touch,gap,dp(density,dragging?SEEK_DRAG_DP:SEEK_DP),dp(density,THUMB_DP),camera,lock,speed,prev,play,next,audio,cc,episodes,back,decoder,aspect,pip,more,status);
 }
}
