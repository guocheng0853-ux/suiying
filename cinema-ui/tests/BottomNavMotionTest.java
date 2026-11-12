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
  check(margin==-pad&&BottomNavRules.heroScreenLeft(pad)==0,"hero bleeds from the screen edge under the rail");
  check(BottomNavRules.fullBleedLeftMargin(pad,railRight,false)==-pad,"phone hero also bleeds from the screen edge");
  int rowAtRest=BottomNavRules.rowRestScreenLeft(0,railRight,true);
  check(rowAtRest>=railRight&&BottomNavRules.rowStartPadding(0,railRight,true)==railRight,"poster rows rest at the rail's right edge and can scroll underneath");
  check(BottomNavRules.rowStartPadding(0,railRight,false)==0,"phone rows do not pick up the rail padding");
  int bodyRail=148,bodyPhone=52;
  int railMargin=BottomNavRules.rowLeftMarginFor(true,bodyRail,0),railPad=BottomNavRules.rowStartPaddingFor(true,0,railRight);
  int phoneMargin=BottomNavRules.rowLeftMarginFor(false,bodyPhone,0),phonePad=BottomNavRules.rowStartPaddingFor(false,0,railRight);
  check(railMargin==-bodyRail&&railPad==railRight,"rail rows use a negative margin and start padding");
  check(phoneMargin==0&&phonePad==0,"leaving the rail clears margin and start padding");
  check(bodyPhone+phoneMargin+phonePad==bodyPhone,"phone first card sits on the body inset, not 12dp off");
  int night=BottomNavRules.railSurface(false),day=BottomNavRules.railSurface(true);
  check(((night>>>24)&255)==BottomNavRules.RAIL_SCRIM_ALPHA&&(night&0xffffff)==(BottomNavRules.surface(false)&0xffffff),"night rail is a translucent copy of the surface");
  check(((day>>>24)&255)==BottomNavRules.RAIL_SCRIM_ALPHA_LIGHT,"light rail is more opaque over bright posters");
  int onWhite=BottomNavRules.composite(day,0xffffffff);
  check(PlayerGlassRules.contrast(BottomNavRules.DAY_MUTED,onWhite)>=4.5&&PlayerGlassRules.contrast(BottomNavRules.DAY_INK,onWhite)>=4.5,"light rail labels clear 4.5:1 on a white poster");
  check(((BottomNavRules.surface(true)>>>24)&255)==255,"phone capsule stays opaque");
  float box=48f;
  BottomNavRules.Slide half=BottomNavRules.Slide.resting(A).play(B,true).advance(0.5f);
  BottomNavRules.Slide picked=BottomNavRules.selectMark(half,C,false);
  check(!picked.running&&near(picked.shown.x,C.x)&&!near(picked.shown.x,B.x)&&!near(picked.shown.x,half.shown.x),"reduced motion rests on the new slot, not the old target");
  BottomNavRules.Slide held=BottomNavRules.selectMark(BottomNavRules.Slide.resting(A),C,false);
  check(!held.running&&near(held.shown.x,C.x)&&near(held.to.x,C.x),"animate=false rests on the newly selected slot");
  BottomNavRules.Slide onward=BottomNavRules.selectMark(half,C,true);
  check(onward.running&&near(onward.from.x,half.shown.x)&&near(onward.to.x,C.x),"a moving select still retargets from the shown point");
  check(BottomNavRules.insideHit(true,36,32,72,64,box),"center of a rail tab is a hit");
  check(!BottomNavRules.insideHit(true,2,2,72,64,box),"the corner of the rail tab lets the poster through");
  check(BottomNavRules.insideHit(true,1,1,40,40,box),"a rail tab smaller than 48dp stays fully tappable");
  check(BottomNavRules.insideHit(false,2,2,90,56,box)&&BottomNavRules.insideHit(false,89,55,90,56,box),"phone capsule slots are tappable edge to edge");
  check(!BottomNavRules.drawsTvFocusRing(false,false,80,true),"home nav keeps its own ring");
  check(BottomNavRules.drawsTvFocusRing(false,false,80,false),"other focused controls still get the TV ring");
  check(!BottomNavRules.drawsTvFocusRing(true,false,80,false)&&!BottomNavRules.drawsTvFocusRing(false,true,80,false)&&!BottomNavRules.drawsTvFocusRing(false,false,0,false),"touch, text fields, and unmeasured views stay plain");
  System.out.println("PASS: retarget from mid-slide, rail clearance, single focus ring.");
 }
}
