package com.guocheng.mediapocket;
import java.util.*;
public class SubtitlePrefsTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  SubtitlePrefs.Values fresh=SubtitlePrefs.read(null);
  check("normal".equals(fresh.size)&&"shadow".equals(fresh.look)&&"0".equals(fresh.lift)&&"white".equals(fresh.color)&&"zho".equals(fresh.language)&&!fresh.bold&&fresh.hdrDim&&"2".equals(fresh.secondaryLift)&&fresh.offsetMs==0,"defaults");
  Map<String,String> junk=new HashMap<>();
  junk.put("sub.size","huge-nope");junk.put("sub.look","glow");junk.put("sub.color","pink");junk.put("sub.lift","9");junk.put("sub.secondarySize","tiny");junk.put("sub.secondaryLift","no");junk.put("sub.offsetMs","nope");junk.put("sub.bold","maybe");junk.put("subtitleLanguage","all");
  SubtitlePrefs.Values repaired=SubtitlePrefs.read(junk);
  check("normal".equals(repaired.size)&&"shadow".equals(repaired.look)&&"white".equals(repaired.color)&&"0".equals(repaired.lift)&&"normal".equals(repaired.secondarySize)&&"2".equals(repaired.secondaryLift)&&repaired.offsetMs==0&&!repaired.bold&&"zho".equals(repaired.language),"invalid values fall back");
  SubtitlePrefs.Values edited=new SubtitlePrefs.Values("large","box","4","cyan","hant",true,false,"small","1",4800);
  Map<String,String> saved=SubtitlePrefs.write(edited);
  SubtitlePrefs.Values again=SubtitlePrefs.read(saved);
  check("large".equals(again.size)&&"box".equals(again.look)&&"4".equals(again.lift)&&"cyan".equals(again.color)&&"hant".equals(again.language)&&again.bold&&!again.hdrDim&&"small".equals(again.secondarySize)&&"1".equals(again.secondaryLift)&&again.offsetMs==4800,"round trip keeps style, traditional preference and offset");
  check("true".equals(saved.get("sub.bold"))&&"false".equals(saved.get("pref.subtitleOff"))&&"hant".equals(saved.get("subtitleLanguage")),"stored keys");
  SubtitlePrefs.Values off=SubtitlePrefs.read(SubtitlePrefs.write(new SubtitlePrefs.Values("normal","shadow","0","white","off",false,true,"normal","2",0)));
  check("off".equals(off.language)&&"true".equals(SubtitlePrefs.write(off).get("pref.subtitleOff")),"off preference sticks");
  check(SubtitlePrefs.clampOffset(9000)==5000&&SubtitlePrefs.clampOffset(-9000)==-5000&&SubtitlePrefs.parseOffset(" 120 ")==120,"offset is clamped to five seconds");
  check(SubtitlePrefs.read(Collections.singletonMap("pref.subtitleOff","true")).language.equals("off"),"off flag beats a stored language");
  check(SubtitlePrefs.language(false,"eng").equals("eng")&&SubtitlePrefs.language(false,"hant").equals("hant")&&SubtitlePrefs.language(false,"all").equals("zho"),"language normalize");
  check(SubtitlePrefs.sizeSp("huge")==30&&SubtitlePrefs.liftDp("4")==140&&SubtitlePrefs.colorArgb("yellow")==0xffffe14a,"style tokens");
  check(SubtitlePrefs.summary(edited).contains("繁体优先")&&SubtitlePrefs.summary(edited).contains("大")&&SubtitlePrefs.summary(edited).contains("青色"),"summary is the settings row");
  System.out.println("PASS: subtitle settings persist, invalid values fall back, offset clamps, traditional preference round-trips.");
 }
}
