package com.guocheng.mediapocket;
public class SceneRulesTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args){
  check(SceneRules.progress(-200,400)==.5f&&SceneRules.progress(200,400)==.5f,"independent of swipe direction");check(SceneRules.progress(900,400)==1&&SceneRules.progress(200,0)==0,"bounded and unmeasured states");
  check(SceneRules.fade(0)==0&&SceneRules.fade(1)==1&&SceneRules.fade(.5f)==.5f,"caption crossfade endpoints and midpoint");float previous=-1;for(int i=0;i<=100;i++){float p=SceneRules.fade(i/100f);check(p>=previous&&Math.abs(((1-p)+p)-1)<.0001f,"monotonic fade and conserved opacity");previous=p;}
  check(SceneRules.blend(0xff204060,0xff6080a0,0)==0xff204060&&SceneRules.blend(0xff204060,0xff6080a0,1)==0xff6080a0,"palette endpoints");check(SceneRules.blend(0xff204060,0xff6080a0,.5f)==0xff406080,"palette midpoint");
  for(int color:new int[]{0xff000000,0xffffffff,0xff00ff00,0xff0000ff}){int dark=SceneRules.tone(color,false),light=SceneRules.tone(color,true);for(int shift:new int[]{0,8,16}){check(((dark>>shift)&255)<=75,"dark caption contrast");check(((light>>shift)&255)>=225,"light caption contrast");}}
  check(SceneRules.tone(0xff00ff00,false)!=SceneRules.tone(0xff0000ff,false),"artwork changes ambient hue");
  System.out.println("PASS: independent scene progress, fixed-caption crossfade, opacity conservation, palette interpolation and light/dark contrast.");
 }
}
