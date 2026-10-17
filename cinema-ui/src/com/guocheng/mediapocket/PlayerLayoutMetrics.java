package com.guocheng.mediapocket;
/**
 * 横屏控件的位置，和 PlayerOverlay / PlayerChrome 用同一套 dp、sp。
 * 顶栏：返回、标题、时间/网速/电量、解码、比例、画中画、更多。
 * 底栏：集标题、时间文字、进度条拉到右侧安全边距、传输键。
 * 左侧截图和锁屏成组，和右侧倍速药丸落在同一条中线上。
 */
final class PlayerLayoutMetrics {
 static final int TOUCH_DP=40,ICON_DP=24,PILL_W_DP=44,PILL_H_DP=132,PILL_RADIUS_DP=8;
 static final int SEEK_DP=3,SEEK_DRAG_DP=4,THUMB_DP=12,TIME_ROW_DP=24,TIME_GAP_DP=8;
 static final int TRANSPORT_GAP_DP=12,SIDE_GAP_DP=16,MARGIN_DP=16,TOP_H_DP=40,TOP_GAP_DP=4;
 static final int TOP_PAD_DP=8,TOP_CUT_PAD_DP=4,TOP_BOTTOM_PAD_DP=6,TITLE_GAP_DP=4,DECODER_PAD_DP=6;
 static final int BOTTOM_TOP_PAD_DP=12,BOTTOM_PAD_DP=8,TRANSPORT_PAD_DP=4;
 static final int TITLE_SP=16,STATUS_SP=11,DECODER_SP=13,EPISODE_SP=14,TIME_SP=13,SPEED_SP=14,SPEED_BTN_SP=18;
 /** 黑、约 45% 不透明。不是浅灰。 */
 static final int PILL_COLOR=0x73000000;
 static final float EPISODE_LINE_EM=1.35f;
 private PlayerLayoutMetrics(){}
 static int dp(float density,int v){float d=density<=0f?1f:density;return Math.round(v*d);}
 static int episodeLinePx(float density){float d=density<=0f?1f:density;return Math.max(1,Math.round(EPISODE_SP*d*EPISODE_LINE_EM));}
 static int topY(float density,int cutT){return Math.max(dp(density,TOP_PAD_DP),Math.max(0,cutT)+dp(density,TOP_CUT_PAD_DP));}
 static int topBarBottom(float density,int cutT){return topY(density,cutT)+dp(density,TOP_H_DP)+dp(density,TOP_BOTTOM_PAD_DP);}
 static int bottomPad(float density,int cutB){return Math.max(dp(density,BOTTOM_PAD_DP),Math.max(0,cutB)+dp(density,BOTTOM_PAD_DP));}
 /** 底栏上沿：上内边距 + 集标题 + 时间行 + 传输行，再加底部安全边距。 */
 static int bottomBarTop(int height,float density,int cutB){int buttonY=height-bottomPad(density,cutB)-dp(density,TOUCH_DP);int timeY=buttonY-dp(density,TRANSPORT_PAD_DP)-dp(density,TIME_ROW_DP);return timeY-episodeLinePx(density)-dp(density,BOTTOM_TOP_PAD_DP);}
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
  final Box camera,lock,speed,prev,play,next,audio,cc,episodes,back,title,topStatus,decoder,aspect,pip,more,episode,time,seek,status;
  Overlay(int margin,int touch,int gap,int seekTrack,int thumb,Box camera,Box lock,Box speed,Box prev,Box play,Box next,Box audio,Box cc,Box episodes,Box back,Box title,Box topStatus,Box decoder,Box aspect,Box pip,Box more,Box episode,Box time,Box seek,Box status){
   this.margin=margin;this.touch=touch;this.gap=gap;this.seekTrack=seekTrack;this.thumb=thumb;this.camera=camera;this.lock=lock;this.speed=speed;this.prev=prev;this.play=play;this.next=next;this.audio=audio;this.cc=cc;this.episodes=episodes;this.back=back;this.title=title;this.topStatus=topStatus;this.decoder=decoder;this.aspect=aspect;this.pip=pip;this.more=more;this.episode=episode;this.time=time;this.seek=seek;this.status=status;
  }
 }
 static Overlay measure(int width,int height,float density,int cutL,int cutT,int cutR,int cutB,boolean prevOn,boolean nextOn,boolean episodesOn,boolean dragging,int topBottom,int bottomTop){
  return measure(width,height,density,cutL,cutT,cutR,cutB,prevOn,nextOn,episodesOn,dragging,topBottom,bottomTop,0,0,0);
 }
 /**
  * timePx / statusPx / decoderPx 是实测文字宽度（解码含左右内边距）。传 0 时用一档够放下样例的估计。
  * topBottom、bottomTop 是顶栏底和底栏顶；传 0 时用这里算出来的同一套边距。
  */
 static Overlay measure(int width,int height,float density,int cutL,int cutT,int cutR,int cutB,boolean prevOn,boolean nextOn,boolean episodesOn,boolean dragging,int topBottom,int bottomTop,int timePx,int statusPx,int decoderPx){
  int margin=sideMargin(density,cutL,cutR),touch=dp(density,TOUCH_DP),gap=dp(density,TRANSPORT_GAP_DP),sideGap=dp(density,SIDE_GAP_DP);
  int yTop=topY(density,cutT),topH=dp(density,TOP_H_DP),tg=dp(density,TOP_GAP_DP);
  int gapTop=topBottom>0?topBottom:topBarBottom(density,cutT);
  int contentBottom=bottomTop>0?bottomTop:bottomBarTop(height,density,cutB);
  if(contentBottom<gapTop+touch)contentBottom=Math.min(height,gapTop+touch*3);
  int stack=touch*2+sideGap;
  int stackTop=gapTop+Math.max(0,(contentBottom-gapTop-stack)/2);
  Box camera=new Box(margin,stackTop,touch,touch);
  Box lock=new Box(margin,stackTop+touch+sideGap,touch,touch);
  int pillW=dp(density,PILL_W_DP),pillH=dp(density,PILL_H_DP);
  int speedY=gapTop+Math.max(0,(contentBottom-gapTop-pillH)/2);
  Box speed=new Box(width-margin-pillW,speedY,pillW,pillH);
  int buttonY=height-bottomPad(density,cutB)-touch;
  int x=margin;
  Box prev=prevOn?new Box(x,buttonY,touch,touch):null;if(prevOn)x+=touch+gap;
  Box play=new Box(x,buttonY,touch,touch);x+=touch+gap;
  Box next=nextOn?new Box(x,buttonY,touch,touch):null;
  int rx=width-margin-touch;
  Box episodes=episodesOn?new Box(rx,buttonY,touch,touch):null;if(episodesOn)rx-=touch+gap;
  Box cc=new Box(rx,buttonY,touch,touch);rx-=touch+gap;
  Box audio=new Box(rx,buttonY,touch,touch);
  int cursor=width-margin-touch;
  Box more=new Box(cursor,yTop,touch,topH);cursor-=tg+touch;
  Box pip=new Box(cursor,yTop,touch,topH);cursor-=tg+touch;
  Box aspect=new Box(cursor,yTop,touch,topH);
  int decW=decoderPx>0?decoderPx:dp(density,DECODER_SP)+dp(density,DECODER_PAD_DP)*2+dp(density,8);cursor-=tg+decW;
  Box decoder=new Box(cursor,yTop,decW,topH);
  int stW=statusPx>0?statusPx:dp(density,STATUS_SP)*8;cursor-=tg+stW;
  Box topStatus=new Box(cursor,yTop,stW,topH);
  Box back=new Box(margin,yTop,touch,topH);
  int titleX=back.right()+dp(density,TITLE_GAP_DP);
  int titleW=Math.max(0,topStatus.x-tg-titleX);
  Box title=new Box(titleX,yTop,titleW,topH);
  int timeH=dp(density,TIME_ROW_DP);
  int timeY=buttonY-dp(density,TRANSPORT_PAD_DP)-timeH;
  int timeW=timePx>0?timePx:dp(density,TIME_SP)*6;
  Box time=new Box(margin,timeY,timeW,timeH);
  int seekX=time.right()+dp(density,TIME_GAP_DP);
  Box seek=new Box(seekX,timeY,Math.max(0,width-margin-seekX),timeH);
  int epH=episodeLinePx(density);
  Box episode=new Box(margin,timeY-epH,Math.max(0,width-margin*2),epH);
  int sw=dp(density,180),sh=dp(density,32);
  Box status=new Box((width-sw)/2,(height-sh)/2,sw,sh);
  return new Overlay(margin,touch,gap,dp(density,dragging?SEEK_DRAG_DP:SEEK_DP),dp(density,THUMB_DP),camera,lock,speed,prev,play,next,audio,cc,episodes,back,title,topStatus,decoder,aspect,pip,more,episode,time,seek,status);
 }
}
