package com.guocheng.mediapocket;
public final class UiTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a){
  int[] in={9,10,11,12,13,14,15,16,17,18,20,22,24,26,29,30,34};java.util.Set<Integer> out=new java.util.TreeSet<>();for(int v:in)out.add(Ui.sp(v));
  check(out.toString().equals("[12, 14, 16, 20, 28]"),"five type steps "+out);
  check(Ui.sp(9)==12&&Ui.sp(11)==12,"12sp floor");
  check(Ui.radius(0)==0&&Ui.radius(4)==8&&Ui.radius(10)==12&&Ui.radius(14)==12&&Ui.radius(18)==20&&Ui.radius(28)==20&&Ui.radius(100)==100,"three radii");
  check(Ui.plain("热门剧集 ›").equals("热门剧集")&&Ui.plain("媒体库  ›").equals("媒体库")&&Ui.plain("继续观看").equals("继续观看")&&Ui.hadArrow("电影 ›")&&!Ui.hadArrow("电影"),"strip tail arrows");
  System.out.println("PASS: UI spec five type steps with 12sp floor, three radii, tail arrows stripped.");
 }
}
