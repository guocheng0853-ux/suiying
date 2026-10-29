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
  // 0.1.160 平板智能满屏：宽高各自缩放到窗口，不裁切、不留黑边。手机仍用 12% / 10%。
  check(FullscreenRules.tabletStretch("smart",VideoFullscreenRules.TABLET_SW)&&!FullscreenRules.tabletStretch("smart",VideoFullscreenRules.TABLET_SW-1)&&!FullscreenRules.tabletStretch("fit",800)&&!FullscreenRules.tabletStretch("crop",800),"only tablet smart stretches");
  stretchFills(3000,2120,1920,1080,600,"OPD2401 16:9 at the 600dp cutoff");
  stretchFills(3000,2120,1920,1080,808,"OPD2401 16:9");
  stretchFills(2048,1536,1920,1080,768,"4:3 tablet 16:9");
  stretchFills(2560,1600,1920,1080,800,"16:10 tablet 16:9");
  stretchFills(3000,2120,2390,1000,800,"ultra-wide film still stretches");
  stretchFills(3000,2120,1440,1080,800,"4:3 picture stretches");
  stretchFills(2120,3000,1920,1080,800,"portrait window stretches");
  stretchFills(3000,2120,1080,1920,800,"portrait picture stretches");
  float[] pad=FullscreenRules.smart(3000,2120,1920,1080,800);
  check(pad[0]==1f&&pad[1]>1f,"16:9 on OPD2401 stretches height and does not crop the sides");
  check(pad[1]>1f/(1f-FullscreenRules.SMART_CROP),"tablet stretch is not limited by the phone crop cap");
  float[] filmPad=FullscreenRules.smart(3000,2120,2390,1000,800);
  check(filmPad[0]==1f&&filmPad[1]/filmPad[0]>1f+FullscreenRules.SMART_STRETCH,"ultra-wide stretch is not capped at 10% and does not fall back to letterbox");
  float[] portraitPic=FullscreenRules.smart(3000,2120,1080,1920,800);
  check(portraitPic[1]==1f&&portraitPic[0]>2f,"a tall picture stretches across the width instead of pillarboxing");
  check(FullscreenRules.smart(3000,2120,1920,1080,599)[0]==1f&&FullscreenRules.smart(3000,2120,1920,1080,599)[1]==1f,"just under 600dp keeps the phone letterbox fallback");
  float[] phone=FullscreenRules.smart(3168,1440,1920,1080,411);check(phone[0]==wide[0]&&phone[1]==wide[1],"explicit phone classification preserves existing output");
  float[] matched=FullscreenRules.smart(3000,2120,3000,2120,800);check(Math.abs(matched[0]-1f)<1e-4&&Math.abs(matched[1]-1f)<1e-4,"matching tablet aspect is not scaled");
  check(FullscreenRules.smart(0,2120,1920,1080,800)[0]==1f,"unknown window stays at 1");
  check(FullscreenRules.visibleNext("smart",3000,2120,1920,1080,800).equals("fit"),"tablet quick switch compares against the stretched picture");
  check(FullscreenRules.visibleNext("smart",3000,2120,2390,1000,800).equals("fit")&&FullscreenRules.visibleNext("fit",3000,2120,2390,1000,800).equals("crop"),"ultra-wide tablet smart differs from both letterbox and crop");
  float[] same=FullscreenRules.smart(3168,1440,3168,1440);check(same[0]==1f&&same[1]==1f,"native ratio untouched");
  check(FullscreenRules.smart(0,0,1920,1080)[0]==1f,"unknown size");
  check(!FullscreenRules.surfaceDirty(10,20,0,0,1,1,10,20,1,1),"unchanged surface is not rewritten on the 250ms pulse");
  check(FullscreenRules.surfaceDirty(10,20,0,0,1,1.02f,10,20,1,1)&&FullscreenRules.surfaceDirty(10,20,3,0,1,1,10,20,1,1),"scale or translation changes still apply");
  String mode=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/FullscreenMode.java"));
  String chrome=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/PlayerChrome.java"));
  String rules=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/FullscreenRules.java"));
  String view=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/Media3VideoView.java"));
  check(mode.contains("surfaceDirty")&&chrome.contains("postDelayed(this,250)")&&chrome.contains("learnSize")&&chrome.contains("PlaybackRepair.tick"),"chrome stays on a 250ms pulse and skips a no-op scale write");
  check(mode.contains("smallestScreenWidthDp"),"VLC smart scale uses the tablet cutoff");
  check(!rules.contains("TABLET_SMART_CROP")&&rules.contains("stretchFill"),"tablet crop cap is not kept");
  check(view.contains("Media3ControlRules.place")&&view.contains("setScaleX(zoom*place[2])")&&view.contains("setScaleY(zoom*place[3])"),"Media3 stretches the fit surface instead of resizing it to the window");
  int[] box=Media3ControlRules.box(3168,1440,1920,1080,"smart");check(box[0]>=3168&&box[1]>=1440&&box[1]<=1440*1.14,"Media3 smart box covers the screen with bounded crop");
  System.out.println("PASS: three aspect modes, legacy modes to smart fill, smart fill for 2.2:1 screens, tablet stretch-to-fill, pinch axes, pip clamp.");
 }
 static void stretchFills(int screenW,int screenH,int videoW,int videoH,int sw,String why){
  float[] f=FullscreenRules.smart(screenW,screenH,videoW,videoH,sw);
  int[] fit=Media3ControlRules.box(screenW,screenH,videoW,videoH,"fit");
  float[] placed=Media3ControlRules.place(screenW,screenH,videoW,videoH,"smart",sw);
  check(fit[0]<=screenW&&fit[1]<=screenH,why+" fit surface stays inside the window");
  check(Math.abs(Math.round(fit[0]*f[0])-screenW)<=1&&Math.abs(Math.round(fit[1]*f[1])-screenH)<=1,why+" scale covers the window");
  check(placed[0]==fit[0]&&placed[1]==fit[1]&&Math.abs(placed[2]-f[0])<1e-5&&Math.abs(placed[3]-f[1])<1e-5,why+" Media3 places the same stretch");
  check(f[0]>=1f-1e-4&&f[1]>=1f-1e-4,why+" does not shrink into a letterbox");
  boolean widthFit=fit[0]==screenW,heightFit=fit[1]==screenH;
  check(widthFit||heightFit,why+" one axis already meets the window");
  if(widthFit)check(f[0]==1f,why+" does not crop the sides");
  if(heightFit)check(f[1]==1f,why+" does not crop the top and bottom");
  double screen=(double)screenW/screenH,video=(double)videoW/videoH;
  if(Math.abs(screen/video-1)>0.02)check(Math.abs(f[0]-f[1])>0.02f,why+" scales X and Y independently");
 }
}
