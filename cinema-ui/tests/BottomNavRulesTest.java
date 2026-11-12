package com.guocheng.mediapocket;
public class BottomNavRulesTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] a){
  check(HomeRules.TABS.length==4,"four tabs stay");
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
  check(!BottomNavRules.labelVisible(false,false)&&BottomNavRules.labelVisible(false,true)&&BottomNavRules.labelVisible(true,false),"pill hides unselected labels; the rail does not");
  check(BottomNavRules.labelSp(false)>=12&&BottomNavRules.labelSp(true)>=12&&BottomNavRules.PHONE_LABEL_SP==14&&BottomNavRules.RAIL_LABEL_SP==12,"labels at least 12sp");
  check(BottomNavRules.pillItemMinDp()>=48&&BottomNavRules.railItemWidthDp()>=48&&BottomNavRules.railItemHeightDp()>=48,"touch targets at least 48dp");
  check(BottomNavRules.ICON_DP==24,"icon 24dp");
  check(BottomNavRules.contentLeftDp(true)==112&&BottomNavRules.contentLeftDp(false)==16,"rail content inset is 112dp");
  check(BottomNavRules.contentBottomDp(false,false)==BottomNavRules.PILL_CLEARANCE_DP&&BottomNavRules.PILL_CLEARANCE_DP>BottomNavRules.PILL_HEIGHT_DP,"scrolled content clears the capsule");
  check(BottomNavRules.contentBottomDp(true,false)==24&&BottomNavRules.contentBottomDp(false,true)==24&&BottomNavRules.contentBottomDp(true,true)==24,"rail and detail leave the bottom to the gesture inset");
  check(BottomNavRules.pillBottomMarginDp(48)==48+BottomNavRules.NAV_PAD_BOTTOM_DP,"pill sits above the navigation bar");
  check(BottomNavRules.fadeHeightDp(48)==BottomNavRules.NAV_PAD_TOP_DP+BottomNavRules.PILL_HEIGHT_DP+BottomNavRules.NAV_PAD_BOTTOM_DP+48,"fade covers the capsule and the gesture inset");
  check(BottomNavRules.railLeftDp(36)==36+BottomNavRules.RAIL_LEFT_DP,"rail clears a left cutout");
  check(BottomNavRules.railTop(700,308,24,48)==24+(700-24-48-308)/2,"rail centers between status and navigation bars");
  check(BottomNavRules.railTop(200,308,24,20)==24,"short window pins the rail under the status bar");
  check(BottomNavRules.EXPAND_MS==320&&BottomNavRules.COLOR_MS==160&&BottomNavRules.EASE_X1==.2f&&BottomNavRules.EASE_Y2==1f,"expand is 320ms on the design curve");
  check(BottomNavRules.motionEnabled(1f,1f,1f)&&!BottomNavRules.motionEnabled(0f,1f,1f)&&!BottomNavRules.motionEnabled(1f,0f,1f)&&!BottomNavRules.motionEnabled(1f,1f,0f),"remove animations disables the expand");
  check(BottomNavRules.expandMs(false)==0&&BottomNavRules.expandMs(true)==320,"reduced motion cuts straight to the selected size");
  check(BottomNavRules.PILL_HEIGHT_DP==64&&BottomNavRules.PILL_RADIUS_DP==32&&BottomNavRules.RAIL_WIDTH_DP==80&&BottomNavRules.RAIL_RADIUS_DP==40,"comp sizes");
  check(PlayerGlassRules.contrast(BottomNavRules.DAY_ON_ACCENT,BottomNavRules.DAY_ACCENT)>=4.5,"day ink on terracotta");
  check(PlayerGlassRules.contrast(BottomNavRules.NIGHT_ON_ACCENT,BottomNavRules.NIGHT_ACCENT)>=4.5,"night ink on gold");
  check(PlayerGlassRules.contrast(BottomNavRules.DAY_MUTED,BottomNavRules.DAY_SURF)>=4.5,"day unselected label");
  check(PlayerGlassRules.contrast(BottomNavRules.NIGHT_MUTED,BottomNavRules.NIGHT_SURF)>=4.5,"night unselected label");
  check(PlayerGlassRules.contrast(BottomNavRules.DAY_FOCUS,BottomNavRules.DAY_PAGE)>=3,"day focus ring");
  check(PlayerGlassRules.contrast(BottomNavRules.NIGHT_FOCUS,BottomNavRules.NIGHT_PAGE)>=3,"night focus ring");
  check(PlayerGlassRules.contrast(BottomNavRules.DAY_CLAY_DEEP,BottomNavRules.DAY_CHIP)<4.5,"old #B5573A on the chip is short of 4.5");
  check(PlayerGlassRules.contrast(BottomNavRules.DAY_CHIP_TEXT,BottomNavRules.DAY_CHIP)>=4.5,"#9B4A2F clears the light chip");
  check(PlayerGlassRules.contrast(BottomNavRules.DAY_CHIP_TEXT,BottomNavRules.DAY_ACCENT)<4.5,"#9B4A2F does not clear the solid capsule");
  check(BottomNavRules.onAccent(true)==BottomNavRules.DAY_ON_ACCENT&&BottomNavRules.onAccent(true)!=BottomNavRules.DAY_CLAY_DEEP,"selected day text is ink, not #B5573A");
  check(BottomNavRules.fadeColor(BottomNavRules.NIGHT_PAGE,0f)==0x000b0b0c,"fade starts transparent");
  check((BottomNavRules.fadeColor(BottomNavRules.DAY_PAGE,1f)&0xffffff)==(BottomNavRules.DAY_PAGE&0xffffff),"fade ends on the page color");
  int mid=BottomNavRules.fadeColor(BottomNavRules.NIGHT_PAGE,BottomNavRules.FADE_MID_ALPHA);
  check(((mid>>>24)&255)>=218&&((mid>>>24)&255)<=220,"fade is 86% at the middle stop");
  System.out.println("PASS: rail threshold, insets, touch size, contrast, reduced motion.");
 }
}
