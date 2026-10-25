package com.guocheng.mediapocket;
public final class UiTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a){
  int[] in={9,10,11,12,13,14,15,16,17,18,20,22,24,26,29,30,34};java.util.Set<Integer> out=new java.util.TreeSet<>();for(int v:in)out.add(Ui.sp(v));
  check(out.toString().equals("[12, 14, 16, 20, 28]"),"five type steps "+out);
  check(Ui.sp(9)==12&&Ui.sp(11)==12,"12sp floor");
  check(Ui.radius(0)==0&&Ui.radius(4)==8&&Ui.radius(10)==12&&Ui.radius(14)==12&&Ui.radius(18)==20&&Ui.radius(28)==20&&Ui.radius(100)==100,"three radii");
  check(Ui.plain("热门剧集 ›").equals("热门剧集")&&Ui.plain("媒体库  ›").equals("媒体库")&&Ui.plain("继续观看").equals("继续观看")&&Ui.hadArrow("电影 ›")&&!Ui.hadArrow("电影"),"strip tail arrows");
  int clay=0xffd97757,gold=0xffe0b25c;
  check(PlayerGlassRules.contrast(0xffffffff,clay)<4.5,"regression: white on clay is below the text contrast floor");
  check(Ui.onFill(clay)==0xff141413&&Ui.onFill(gold)==0xff141413,"warm fills use warm ink");
  for(int r=0;r<256;r+=17)for(int g=0;g<256;g+=17)for(int b=0;b<256;b+=17){int fill=0xff000000|(r<<16)|(g<<8)|b;check(PlayerGlassRules.contrast(Ui.onFill(fill),fill)>=4.5,"readable button foreground for "+Integer.toHexString(fill));}
  check(PaperLayoutRules.buttonHeightDp(false)>=48&&PaperLayoutRules.buttonHeightDp(true)>=48,"phone and tablet primary touch target");
  System.out.println("PASS: UI spec five type steps with 12sp floor, three radii, tail arrows stripped.");
 }
}
