package com.guocheng.mediapocket;
public class AudioEffectRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  check(AudioEffectRules.normalize(null).equals("off")&&AudioEffectRules.normalize("junk").equals("off")&&AudioEffectRules.normalize("preset:3").equals("preset:3")&&AudioEffectRules.normalize("preset:x").equals("off")&&AudioEffectRules.normalize("preset:99").equals("off"),"normalize");
  check(AudioEffectRules.presetIndex("preset:7")==7&&AudioEffectRules.presetIndex("night")==-1,"preset index");
  float[] d=AudioEffectRules.curve("dialog",10);check(d.length==11&&d[0]<0&&d[1]<0&&d[7]>0,"dialog curve: preamp headroom, low cut, presence boost");
  float[] n=AudioEffectRules.curve("night",10);check(n[1]<=-10&&n[3]<0&&n[0]<=0,"night cuts deep bass");
  check(AudioEffectRules.curve("night",8).length==9&&AudioEffectRules.curve("off",10)==null&&AudioEffectRules.curve("preset:1",10)==null,"band count / non-custom");
  float[] k=AudioEffectRules.curve("akg",10),m=AudioEffectRules.curve("anime",10);
  check(k!=null&&k[1]>0&&k[10]>0&&k[5]<0,"akg: full low end, slight mid dip, airy top");
  check(m!=null&&m[4]<0&&m[8]>=3&&m[7]>0,"anime: cut 250 Hz mud, lift 2-8 kHz voices");
  check(AudioEffectRules.normalize("akg").equals("akg")&&AudioEffectRules.label("anime","").equals("动漫")&&AudioEffectRules.label("akg","").equals("AKG 风格"),"new effects saved and labelled");
  for(float[] c:new float[][]{d,n,k,m})for(int i=1;i<c.length;i++)check(c[i]+c[0]<=1.5f,"no band pushes far above 0 dB after preamp (clipping guard)");
  check(AudioEffectRules.chinese("Full bass").equals("重低音")&&AudioEffectRules.chinese("Headphones").equals("耳机")&&AudioEffectRules.chinese("Unknown").equals("Unknown"),"names");
  check(AudioEffectRules.label("preset:4","Full bass").equals("均衡器 · 重低音")&&AudioEffectRules.label("dialog","").equals("对白增强")&&AudioEffectRules.label("x","").equals("关闭"),"labels");
  System.out.println("PASS: audio effects normalize, dialog/night curves with clipping headroom, preset names and labels.");
 }
}
