package com.guocheng.mediapocket;
/** Bounds for the recovery panel. Values are dp, independent of Android. */
final class PlaybackFailureRules {
 private PlaybackFailureRules(){}
 static final int SIDE=24,MAX_WIDTH=520,TOUCH=48;
 static int width(int viewport,int left,int right){return Math.min(MAX_WIDTH,Math.max(0,viewport-Math.max(0,left)-Math.max(0,right)-SIDE*2));}
 /** Leave playback controls usable when they are visible, without moving the panel into the top bar. */
 static int bottom(int height,int top,int inset,int controls,int panel){int base=Math.max(0,inset)+24;int room=Math.max(0,height-Math.max(0,top)-24-base-Math.max(0,panel));return base+Math.min(room,controls>0?controls+12:0);}
 static String title(){return "暂时无法播放";}
 static String hint(){return "可以重新连接当前片源，或选择其他片源继续观看。";}
}
