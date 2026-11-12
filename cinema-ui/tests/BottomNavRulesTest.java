package com.guocheng.mediapocket;
public class BottomNavRulesTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 static boolean near(float a,float b){return Math.abs(a-b)<0.01f;}
 public static void main(String[] a){
  check(HomeRules.TABS.length==4,"four tabs stay inside the capsule");
  check(BottomNavRules.useRail(980,700,700),"OPD2401 landscape rail");
  check(!BottomNavRules.useRail(700,980,700),"OPD2401 portrait stays a pill");
  check(!BottomNavRules.useRail(390,844,390),"phone portrait pill");
  check(!BottomNavRules.useRail(844,390,390),"phone landscape stays a pill");
  check(!BottomNavRules.useRail(520,400,700),"split window narrower than 600 drops the rail");
  check(BottomNavRules.useRail(800,500,700),"wide tablet split keeps the rail");
  check(!BottomNavRules.useRail(980,700,599),"shortest side under 600 is not a tablet");
  check(BottomNavRules.useRail(600,599,600),"600dp landscape is the rail threshold");
  check(!BottomNavRules.useRail(599,400,800),"599dp window is still a pill");
  check(!BottomNavRules.useRail(1000,1000,800),"square window is not landscape");
  check(!BottomNavRules.useRail(844,390,0),"unknown shortest side and a short height stays a pill");
  check(BottomNavRules.useRail(980,700,0),"unknown shortest side with a tall landscape window uses the rail");
  check(BottomNavRules.keptIndex(2,4)==2&&BottomNavRules.keptIndex(-1,4)==0&&BottomNavRules.keptIndex(9,4)==0,"rotation keeps a valid tab");
  check(BottomNavRules.labelVisible(false,false)&&BottomNavRules.labelVisible(false,true)&&BottomNavRules.labelVisible(true,false),"every item keeps its label");
  check(BottomNavRules.labelSp(false)>=12&&BottomNavRules.labelSp(true)>=12&&BottomNavRules.LABEL_SP==12,"labels are 12sp");
  check(BottomNavRules.pillItemMinDp()>=48&&BottomNavRules.railItemWidthDp()>=48&&BottomNavRules.railItemMinDp()>=48,"touch targets at least 48dp");
  check(BottomNavRules.contentLeftDp(true)==112&&BottomNavRules.contentLeftDp(false)==16,"rail content inset is 112dp");
  check(BottomNavRules.contentBottomDp(false,false)>BottomNavRules.PILL_HEIGHT_DP,"scrolled content clears the capsule");
  check(BottomNavRules.contentBottomDp(false,false,true)<BottomNavRules.contentBottomDp(false,false,false),"keyboard does not keep the extra resting gap");
  check(BottomNavRules.contentBottomDp(false,false,true)==BottomNavRules.PILL_KEYBOARD_DP,"keyboard clearance is just the capsule");
  check(BottomNavRules.contentBottomDp(true,false)==24&&BottomNavRules.contentBottomDp(false,true)==24,"rail and detail leave the bottom to the gesture inset");
  check(BottomNavRules.bottomObstacle(48,0)==48&&BottomNavRules.bottomObstacle(48,320)==320&&BottomNavRules.bottomObstacle(48,320)!=368,"ime replaces the nav inset instead of stacking on it");
  check(BottomNavRules.SLIDE_MS>=220&&BottomNavRules.SLIDE_MS<=280,"slide lasts 220 to 280ms");
  check(BottomNavRules.slideMs(false)==0&&BottomNavRules.slideMs(true)==BottomNavRules.SLIDE_MS,"remove animations skips the slide");
  check(BottomNavRules.motionEnabled(1f,1f,1f)&&!BottomNavRules.motionEnabled(0f,1f,1f)&&!BottomNavRules.motionEnabled(1f,0f,1f)&&!BottomNavRules.motionEnabled(1f,1f,0f),"any zero scale disables motion");
  BottomNavRules.Box a0=BottomNavRules.indicator(false,0,4,328,64,4,3);
  BottomNavRules.Box a1=BottomNavRules.indicator(false,1,4,328,64,4,3);
  BottomNavRules.Box a3=BottomNavRules.indicator(false,3,4,328,64,4,3);
  check(near(a0.x,7f)&&near(a0.w,74f)&&near(a0.y,7f)&&near(a0.h,50f),"first phone slot");
  check(near(a1.x,87f)&&near(a3.x,247f)&&near(a0.w,a1.w)&&near(a1.w,a3.w),"equal slots, slide is a move not an expand");
  BottomNavRules.Box mid=BottomNavRules.lerp(a0,a1,0.5f);
  check(near(mid.x,(a0.x+a1.x)/2f)&&near(mid.w,a0.w),"halfway between neighbours");
  check(near(BottomNavRules.lerp(a0,a3,0f).x,a0.x)&&near(BottomNavRules.lerp(a0,a3,1f).x,a3.x),"lerp endpoints");
  BottomNavRules.Box retarget=BottomNavRules.lerp(mid,a3,0f);
  check(near(retarget.x,mid.x)&&near(retarget.w,mid.w),"a new slide can start from the current block");
  BottomNavRules.Box r0=BottomNavRules.indicator(true,0,4,280,80,4,3);
  BottomNavRules.Box r1=BottomNavRules.indicator(true,1,4,280,80,4,3);
  check(near(r0.y,7f)&&near(r0.h,62f)&&near(r0.x,7f)&&near(r1.y,75f)&&near(r0.h,r1.h),"rail block slides vertically with equal height");
  check(PlayerGlassRules.contrast(BottomNavRules.DAY_INK,BottomNavRules.DAY_MARK)>=4.5,"day ink on the neutral block");
  check(PlayerGlassRules.contrast(BottomNavRules.NIGHT_INK,BottomNavRules.NIGHT_MARK)>=4.5,"night ink on the neutral block");
  check(PlayerGlassRules.contrast(BottomNavRules.DAY_MUTED,BottomNavRules.DAY_SURF)>=4.5,"day unselected label");
  check(PlayerGlassRules.contrast(BottomNavRules.NIGHT_MUTED,BottomNavRules.NIGHT_SURF)>=4.5,"night unselected label");
  check(PlayerGlassRules.contrast(BottomNavRules.DAY_FOCUS,BottomNavRules.DAY_SURF)>=3,"day focus ring");
  check(PlayerGlassRules.contrast(BottomNavRules.NIGHT_FOCUS,BottomNavRules.NIGHT_SURF)>=3,"night focus ring");
  System.out.println("PASS: equal slots, sliding block, keyboard inset, contrast.");
 }
}
