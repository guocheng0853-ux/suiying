package com.guocheng.mediapocket;
public final class Media3ControlRulesTest {
 public static void main(String[] args){
  dimensions("fit",1920,1080,1440,1080,1440,1080);
  dimensions("crop",1920,1080,1440,1080,1920,1440);
  dimensions("stretch",1920,1080,1440,1080,1920,1080);
  dimensions("16:9",1080,1920,1000,1000,1080,608);
  dimensions("4:3",1920,1080,1920,1080,1440,1080);
  dimensions("original",1920,1080,640,480,640,480);
  dimensions("original",1280,720,3840,2160,1280,720);
  dimensions("fit",0,1080,1920,1080,0,0);
  for(int[] screen:new int[][]{{3000,2120},{2048,1536},{2560,1600}}){
   int[] smart=Media3ControlRules.box(screen[0],screen[1],1920,1080,"smart",800);
   check(smart[0]>=screen[0]-1&&smart[1]>=screen[1]-1,"tablet smart box covers the window");
   int[] full=Media3ControlRules.box(screen[0],screen[1],1920,1080,"fit",800);
   check(full[0]<=screen[0]&&full[1]<=screen[1],"explicit full-picture mode remains inside the window");
   float[] nativeScale=FullscreenRules.smart(screen[0],screen[1],1920,1080,800);
   check(smart[0]==Math.round(full[0]*nativeScale[0])&&smart[1]==Math.round(full[1]*nativeScale[1]),"Media3 and VLC share tablet scale factors");
  }
  int[] phoneSmart=Media3ControlRules.box(3168,1440,1920,1080,"smart",411),legacySmart=Media3ControlRules.box(3168,1440,1920,1080,"smart");
  check(phoneSmart[0]==legacySmart[0]&&phoneSmart[1]==legacySmart[1],"phone Media3 output unchanged");
  check(Media3ControlRules.seek(10000,60000,-15000)==0,"rewind clamps to start");
  check(Media3ControlRules.seek(55000,60000,15000)==60000,"seek clamps to duration");
  check(Media3ControlRules.seek(Integer.MAX_VALUE-10,0,60000)==Integer.MAX_VALUE,"unknown duration avoids overflow");
  check(Media3ControlRules.seek(15000,0,-10000)==5000,"seek before duration is known");
  System.out.println("PASS: Media3 fit/crop/original dimensions and seek boundaries.");
 }
 static void dimensions(String mode,int w,int h,int vw,int vh,int ew,int eh){int[] box=Media3ControlRules.box(w,h,vw,vh,mode);check(box[0]==ew&&box[1]==eh,mode+" dimensions: "+box[0]+"x"+box[1]);}
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
