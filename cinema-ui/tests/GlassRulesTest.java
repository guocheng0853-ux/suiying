package com.guocheng.mediapocket;
public class GlassRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] x){
  check(GlassRules.worstContrast(true)>=4.5,"white text on blurred glass stays readable even over a white frame: "+GlassRules.worstContrast(true));
  check(GlassRules.worstContrast(false)>=7,"no blur: denser glass");
  int[] w=GlassRules.sideWindow(1600,900,20);check(w[0]==900&&w[1]==1560,"side panel window leaves an edge top and bottom");
  check(GlassRules.sideWindow(0,900,20)[1]==0,"unknown height");
  check(GlassRules.player(true,false)&&!GlassRules.player(true,true)&&!GlassRules.player(false,false),"glass only in the dark player");
  System.out.println("PASS: frosted glass tint keeps contrast; side window sizing; player-only glass.");
 }
}
