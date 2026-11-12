package com.guocheng.mediapocket;
import java.util.*;
/**
 * 播放页上下滑（亮度 / 音量）。不依赖 Android。
 * 平板智能满屏把 SurfaceView 用缩放拉满，布局框仍是「适应屏幕」那一块：
 * 手指落在被拉大的画面上时，事件可能到不了原来的手势检测器。
 * 系统返回和状态栏下拉也会抢走贴边的垂直滑动。第二指或 ACTION_CANCEL 之后必须把这一下作废，否则下一滑还记着上一回的方向。
 */
final class PlayerTouchRules {
 private PlayerTouchRules(){}
 static final int NONE=0,HORIZONTAL=1,VERTICAL=2;
 static final int ACTION_UP=1,ACTION_CANCEL=3,ACTION_POINTER_DOWN=5,ACTION_POINTER_UP=6;
 /**
  * 过了 slop 之后，垂直不小于水平就交给亮度 / 音量。
  * 旧的 1.2 倍门槛会把稍斜的上下滑判成「还没决定」，整段滑动没有音量也没有亮度。
  * 水平严格更大才快进。
  */
 static int axis(float dx,float dy,float slop){float ax=Math.abs(dx),ay=Math.abs(dy),gate=Math.max(0f,slop);if(ay>=gate&&ay>=ax)return VERTICAL;if(ax>=gate&&ax>ay)return HORIZONTAL;return NONE;}
 /** 松手、系统取消、第二指按下或抬起，都结束这一下。下一滑从按下重新算。 */
 static boolean reset(int action){return action==ACTION_UP||action==ACTION_CANCEL||action==ACTION_POINTER_DOWN||action==ACTION_POINTER_UP;}
 /**
  * 左右两半各一条排除区，让亮度 / 音量滑出系统返回和状态栏下拉。
  * 贴边的排除区高于 maxBandPx 时系统会整段丢掉，所以按这个高度切开。
  * edgePx 从左右外沿让出一条，返回手势还在。返回每条 [left, top, right, bottom]。
  */
 static int[][] exclusion(int width,int height,int edgePx,int maxBandPx){if(width<=1||height<=1)return new int[0][];int edge=Math.max(0,Math.min(edgePx,width/4));int band=maxBandPx<=0?height:maxBandPx;int mid=width/2;int left0=edge,left1=Math.max(left0+1,mid);int right1=width-edge,right0=Math.min(right1-1,Math.max(mid,left1));if(right0>=right1)right0=Math.max(0,right1-1);ArrayList<int[]> out=new ArrayList<>();for(int top=0;top<height;top+=band){int bot=Math.min(height,top+band);out.add(new int[]{left0,top,left1,bot});out.add(new int[]{right0,top,right1,bot});}return out.toArray(new int[0][]);}
}
