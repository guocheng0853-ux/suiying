package com.guocheng.mediapocket;
public class ReboundRulesTest {
 static void check(boolean b,String n){if(!b)throw new AssertionError(n);}
 public static void main(String[] a){
  int w=1000;
  // 翻到下一页（目标 -w，向左），冲过头 -1060：没有再下一页 → 截住 60
  check(HomeMotionRules.overshoot(-1060,-w,-1,true,true)==-60,"clamp past next page");
  check(HomeMotionRules.overshoot(1040,w,1,true,true)==40,"clamp past previous page");
  // 回到当前页（目标 0）且越界方向有相邻页：允许露出相邻页，不截
  check(HomeMotionRules.overshoot(-30,0,-1,true,true)==0,"overshoot into existing next allowed");
  // 单张海报（无相邻页）回到 0 时冲过头：截住
  check(HomeMotionRules.overshoot(-30,0,-1,false,false)==-30,"single hero overshoot clamped");
  // 拖动中（direction 0）不截，保留橡皮筋
  check(HomeMotionRules.overshoot(120,0,0,false,false)==0,"no clamp while dragging");
  // 未越过目标不截
  check(HomeMotionRules.overshoot(-500,-w,-1,true,true)==0,"before target untouched");
  // 框内晃动最多 3% 屏宽，且缩放足以盖住两侧
  float b=HomeMotionRules.absorb(-200,w);check(b==-30,"absorb capped at 3%: "+b);
  float s=HomeMotionRules.pageScale(0,b,w);check(s*w>=w+2*Math.abs(b),"scale covers bounce: "+s);
  // 缩放连续：0 处为 1，微小位移不跳到 1.04
  check(HomeMotionRules.pageScale(0,0,w)==1f,"rest scale 1");
  check(HomeMotionRules.pageScale(3,0,w)<1.002f,"tiny drag no jump");
  check(Math.abs(HomeMotionRules.pageScale(400,0,w)-1.04f)<1e-6,"full drag 1.04");
  System.out.println("PASS: rebound clamps only where no neighbour page exists, keeps drag rubber-band, absorbs overshoot ≤3% inside the image with covering scale, continuous image scale.");
 }
}
