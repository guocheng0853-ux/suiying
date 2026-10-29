package com.guocheng.mediapocket;
public class FullscreenRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  // 0.1.152 只留三种画面模式，旧设置（拉伸、16:9、4:3、原始、无效值）读到时按智能满屏处理
  check(String.join(",",FullscreenRules.MODES).equals("smart,fit,crop"),"three modes only");
  check(FullscreenRules.MODES.length==FullscreenRules.LABELS.length&&FullscreenRules.ICONS.length==FullscreenRules.MODES.length,"labels and icons");
  for(String old:new String[]{"stretch","16:9","4:3","original","invalid",null})check(FullscreenRules.mode(old,true).equals("smart")&&FullscreenRules.mode(old,false).equals("smart"),"legacy "+old+" becomes smart");
  check(FullscreenRules.mode("fit",true).equals("fit")&&FullscreenRules.mode("crop",false).equals("crop"),"explicit fit / crop kept");
  check(FullscreenRules.layoutMode("smart").equals("fit")&&FullscreenRules.layoutMode("fit").equals("fit")&&FullscreenRules.layoutMode("crop").equals("crop")&&FullscreenRules.layoutMode("stretch").equals("fit"),"layout mapping");
  check(FullscreenRules.next("smart").equals("fit")&&FullscreenRules.next("fit").equals("crop")&&FullscreenRules.next("crop").equals("smart")&&FullscreenRules.quickNext("stretch").equals("fit"),"cycle order");
  check(FullscreenRules.zoom("fit",1.25f)==1.25f,"manual zoom retained");
  check(FullscreenRules.originalScale(1920,1280)>0.66f&&FullscreenRules.originalScale(1000,1920)==1f,"original pixel scale capped");
  float[] h=FullscreenRules.axisScale(300,200,10,10);check(h[0]==1.5f&&h[1]==1f,"horizontal pinch only X");
  float[] v=FullscreenRules.axisScale(10,10,150,100);check(v[0]==1f&&v[1]==1.5f,"vertical pinch only Y");
  float[] dg=FullscreenRules.axisScale(240,200,120,100);check(dg[0]==1.2f&&dg[1]==1.2f,"diagonal both");
  check(FullscreenRules.zoomLabel(1f,1f).equals("1.0x")&&FullscreenRules.zoomLabel(1.2f,1f).equals("自定义")&&FullscreenRules.zoomLabel(1.5f,1.5f).equals("1.5x"),"zoom label");
  check(FullscreenRules.pipRatio(4000,100)<2.39f&&FullscreenRules.pipRatio(0,0)>1.7f,"pip ratio clamp");
  check(FullscreenRules.clamp(9f)==3f&&FullscreenRules.clamp(0.1f)==0.5f&&FullscreenRules.clamp(Float.NaN)==1f,"clamp");
  check(FullscreenRules.cycleHud(FullscreenRules.LABELS[0])==null,"aspect button stays silent (0.1.67)");
  // 0.1.153 单击跳过看起来一样的模式
  check(FullscreenRules.visibleNext("smart",3168,1440,1920,1080).equals("fit")&&FullscreenRules.visibleNext("fit",3168,1440,1920,1080).equals("crop")&&FullscreenRules.visibleNext("crop",3168,1440,1920,1080).equals("smart"),"16:9: all three look different");
  check(FullscreenRules.visibleNext("smart",3168,1440,2390,1000).equals("fit")&&FullscreenRules.visibleNext("fit",3168,1440,2390,1000).equals("crop")&&FullscreenRules.visibleNext("crop",3168,1440,2390,1000).equals("fit"),"2.39:1: smart equals crop, so the button toggles between filled and full picture");
  check(FullscreenRules.visibleNext("smart",3168,1440,1440,1080).equals("crop")&&FullscreenRules.visibleNext("crop",3168,1440,1440,1080).equals("smart"),"4:3: smart equals fit, so the button toggles smart and crop");
  check(FullscreenRules.displayWidth(1440,4,3)==1920&&FullscreenRules.displayWidth(1920,0,0)==1920&&FullscreenRules.displayWidth(0,1,1)==0,"SAR-adjusted width from the video track");
  check(FullscreenRules.visibleNext("smart",0,0,0,0).equals("fit")&&FullscreenRules.visibleNext("stretch",3168,1440,1920,1080).equals("fit"),"unknown size falls back to the plain cycle");
  // 智能满屏：一加 13 横屏 3168×1440（约 2.2:1）
  float[] wide=FullscreenRules.smart(3168,1440,1920,1080);check(Math.abs(wide[1]-1/(1-FullscreenRules.SMART_CROP))<1e-3&&Math.abs(wide[0]-(3168/1440f)/(16/9f))<1e-3,"16:9 fills: uniform up to the crop cap, rest horizontal");
  check(wide[0]/wide[1]<=1+FullscreenRules.SMART_STRETCH+1e-4,"16:9 stretch stays under 10%");
  float[] scope=FullscreenRules.smart(3168,1440,2390,1000);check(scope[0]==scope[1]&&scope[0]>1f&&scope[0]<1.1f,"2.39:1 film: crop the sides only, no stretch");
  float[] old=FullscreenRules.smart(3168,1440,1440,1080);check(old[0]==1f&&old[1]==1f,"4:3 is too far: keep full picture, do not distort");
  float[] pad=FullscreenRules.smart(3000,2120,1920,1080,600);
  check(pad[0]>1f&&pad[1]>1f,"OPD2401 16:9 now fills instead of falling back to fit");
  check(Math.max(pad[0],pad[1])/Math.min(pad[0],pad[1])<=1+FullscreenRules.SMART_STRETCH+1e-4,"tablet distortion remains bounded");
  check(Math.min(pad[0],pad[1])<=1/(1-FullscreenRules.TABLET_SMART_CROP)+1e-4,"tablet crop stays bounded");
  float[] ipad=FullscreenRules.smart(2048,1536,1920,1080,768);check(ipad[0]>1f&&ipad[1]>1f,"4:3 tablet fills 16:9");
  check(FullscreenRules.smart(3000,2120,1920,1080,599)[0]==1f,"phone classification keeps the previous crop cap");
  float[] phone=FullscreenRules.smart(3168,1440,1920,1080,411);check(phone[0]==wide[0]&&phone[1]==wide[1],"explicit phone classification preserves existing output");
  float[] filmPad=FullscreenRules.smart(3000,2120,2390,1000,800);check(filmPad[0]==1f&&filmPad[1]==1f,"very wide films keep full picture rather than extreme stretching");
  check(FullscreenRules.smart(2120,3000,1920,1080,800)[0]==1f&&FullscreenRules.smart(0,2120,1920,1080,800)[0]==1f,"portrait and unknown windows remain safe");
  check(FullscreenRules.visibleNext("smart",3000,2120,1920,1080,800).equals("fit"),"tablet quick switch compares against the actual filled picture");
  float[] same=FullscreenRules.smart(3168,1440,3168,1440);check(same[0]==1f&&same[1]==1f,"native ratio untouched");
  check(FullscreenRules.smart(0,0,1920,1080)[0]==1f,"unknown size");
  check(!FullscreenRules.surfaceDirty(10,20,0,0,1,1,10,20,1,1),"unchanged surface is not rewritten on the 250ms pulse");
  check(FullscreenRules.surfaceDirty(10,20,0,0,1,1.02f,10,20,1,1)&&FullscreenRules.surfaceDirty(10,20,3,0,1,1,10,20,1,1),"scale or translation changes still apply");
  String mode=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/FullscreenMode.java"));
  String chrome=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/PlayerChrome.java"));
  check(mode.contains("surfaceDirty")&&chrome.contains("postDelayed(this,250)")&&chrome.contains("learnSize")&&chrome.contains("PlaybackRepair.tick"),"chrome stays on a 250ms pulse and skips a no-op scale write");
  int[] box=Media3ControlRules.box(3168,1440,1920,1080,"smart");check(box[0]>=3168&&box[1]>=1440&&box[1]<=1440*1.14,"Media3 smart box covers the screen with bounded crop");
  System.out.println("PASS: three aspect modes, legacy modes to smart fill, smart fill for 2.2:1 screens, pinch axes, pip clamp.");
 }
}
