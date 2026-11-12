package com.guocheng.mediapocket;
public class BottomNavMotionTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 static boolean near(float a,float b){return Math.abs(a-b)<0.01f;}
 static BottomNavRules.Box at(int index){return BottomNavRules.indicator(false,index,4,328,64,4,3);}
 public static void main(String[] a){
  BottomNavRules.Box A=at(0),B=at(1),C=at(3);
  BottomNavRules.Slide slide=BottomNavRules.Slide.resting(A).play(B,true);
  check(slide.running&&near(slide.from.x,A.x)&&near(slide.shown.x,A.x)&&near(slide.to.x,B.x),"slide starts at A toward B");
  slide=slide.advance(0.5f);
  BottomNavRules.Box mid=slide.shown;
  check(slide.running&&near(mid.x,BottomNavRules.lerp(A,B,0.5f).x),"half way is between A and B");
  check(!near(mid.x,A.x)&&!near(mid.x,B.x),"half way is neither endpoint");
  slide=slide.retarget(C,true);
  check(slide.running&&slide.progress==0f&&near(slide.from.x,mid.x)&&near(slide.shown.x,mid.x)&&near(slide.to.x,C.x),"retarget starts from the shown half-way point");
  check(!near(slide.from.x,A.x)&&!near(slide.from.x,B.x),"retarget does not restart at A or B");
  slide=slide.advance(1f);
  check(!slide.running&&near(slide.shown.x,C.x),"finishing the new slide lands on C");
  BottomNavRules.Slide cut=BottomNavRules.Slide.resting(A).play(B,true).advance(0.5f);
  cut=cut.settle();
  check(!cut.running&&near(cut.shown.x,B.x),"settle jumps to the current target");
  BottomNavRules.Slide reduced=BottomNavRules.Slide.resting(A).play(B,false);
  check(!reduced.running&&near(reduced.shown.x,B.x),"reduced motion jumps to B");
  BottomNavRules.Slide reducedRetarget=BottomNavRules.Slide.resting(A).play(B,true).advance(0.5f).retarget(C,false);
  check(!reducedRetarget.running&&near(reducedRetarget.shown.x,C.x),"reduced motion retarget jumps to C");
  check(!BottomNavRules.markVisible(new BottomNavRules.Box(0,0,0,0))&&!BottomNavRules.markVisible(new BottomNavRules.Box(0,0,.5f,20))&&BottomNavRules.markVisible(A),"a zero slot stays hidden");
  int safe=36,railRight=BottomNavRules.railRightDp()+safe,content=BottomNavRules.contentScreenLeftDp(true,safe);
  check(content>=railRight,"poster strips start at or past the rail's right edge");
  int pad=content,margin=BottomNavRules.fullBleedLeftMargin(pad,railRight,true);
  check(pad+margin==railRight,"full-bleed poster stops at the rail instead of sliding underneath the tabs");
  check(BottomNavRules.fullBleedLeftMargin(pad,railRight,false)==-pad,"without a rail the hero still reaches the screen edge");
  float box=48f;
  check(BottomNavRules.insideHit(36,32,72,64,box),"center of a rail tab is a hit");
  check(!BottomNavRules.insideHit(2,2,72,64,box),"the corner of the tab cell lets the poster through");
  check(BottomNavRules.insideHit(1,1,40,40,box),"a view smaller than 48dp stays fully tappable");
  check(!BottomNavRules.drawsTvFocusRing(false,false,80,true),"home nav keeps its own ring");
  check(BottomNavRules.drawsTvFocusRing(false,false,80,false),"other focused controls still get the TV ring");
  check(!BottomNavRules.drawsTvFocusRing(true,false,80,false)&&!BottomNavRules.drawsTvFocusRing(false,true,80,false)&&!BottomNavRules.drawsTvFocusRing(false,false,0,false),"touch, text fields, and unmeasured views stay plain");
  System.out.println("PASS: retarget from mid-slide, rail clearance, single focus ring.");
 }
}
