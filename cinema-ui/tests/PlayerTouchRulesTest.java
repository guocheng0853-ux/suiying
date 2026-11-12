package com.guocheng.mediapocket;
public class PlayerTouchRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  int slop=24;
  check(PlayerTouchRules.axis(10,2,slop)==PlayerTouchRules.NONE,"inside the slop decides nothing");
  check(PlayerTouchRules.axis(60,10,slop)==PlayerTouchRules.HORIZONTAL,"a sideways drag still seeks");
  check(PlayerTouchRules.axis(5,-60,slop)==PlayerTouchRules.VERTICAL,"a vertical drag is brightness or volume");
  check(PlayerTouchRules.axis(45,50,slop)==PlayerTouchRules.VERTICAL,"a slightly diagonal swipe is not dropped");
  check(PlayerTouchRules.axis(50,48,slop)==PlayerTouchRules.HORIZONTAL,"the more horizontal of two past-slop moves seeks");
  check(PlayerTouchRules.reset(PlayerTouchRules.ACTION_UP)&&PlayerTouchRules.reset(PlayerTouchRules.ACTION_CANCEL)&&PlayerTouchRules.reset(PlayerTouchRules.ACTION_POINTER_DOWN)&&PlayerTouchRules.reset(PlayerTouchRules.ACTION_POINTER_UP),"lift, cancel, and a second finger end the gesture");
  check(!PlayerTouchRules.reset(0)&&!PlayerTouchRules.reset(2),"down and move keep the current gesture");
  check(PlayerTouchRules.exclusion(0,100,0,200).length==0,"no surface, no exclusion");
  int[][] bands=PlayerTouchRules.exclusion(1000,500,0,200);
  check(bands.length==6,"500px splits into three 200dp-capped bands on each half");
  check(bands[0][0]==0&&bands[0][2]==500&&bands[0][3]-bands[0][1]<=200,"left half stays inside one band");
  check(bands[1][0]==500&&bands[1][2]==1000,"right half is the volume side");
  check(bands[4][1]==400&&bands[4][3]==500,"the last band ends at the screen");
  int[][] inset=PlayerTouchRules.exclusion(1000,100,16,200);
  check(inset[0][0]==16&&inset[1][2]==984,"a thin edge stays with the system back gesture");
  System.out.println("PASS: player vertical swipe wins over a slight diagonal, resets after cancel, and claims both halves.");
 }
}
