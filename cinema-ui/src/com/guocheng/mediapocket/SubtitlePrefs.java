package com.guocheng.mediapocket;
import java.util.*;
/** 字幕偏好和样式。用字符串表读写，JVM 测试不用 Android。 */
final class SubtitlePrefs {
 static final String[] SIZES={"small","normal","large","huge"},SIZE_LABELS={"小","标准","大","特大"};
 static final int[] SIZE_SP={17,21,25,30};
 static final String[] LOOKS={"shadow","outline","box"},LOOK_LABELS={"阴影","描边","底框"};
 static final String[] COLORS={"white","yellow","cyan","green"},COLOR_LABELS={"白色","黄色","青色","绿色"};
 static final int[] COLOR_ARGB={0xffffffff,0xffffe14a,0xff7fdbff,0xffb6f5c2};
 static final String[] LIFTS={"0","1","2","3","4"},LIFT_LABELS={"贴底","稍高","中等","较高","靠上"};
 static final int[] LIFT_DP={0,24,56,96,140};
 static final String[] LANGS={"zho","hant","eng","off"},LANG_LABELS={"简体优先","繁体优先","英文优先","关闭"};
 static final class Values {
  final String size,look,lift,color,language,secondarySize,secondaryLift; final boolean bold,hdrDim; final int offsetMs;
  Values(String size,String look,String lift,String color,String language,boolean bold,boolean hdrDim,String secondarySize,String secondaryLift,int offsetMs){this.size=size;this.look=look;this.lift=lift;this.color=color;this.language=language;this.bold=bold;this.hdrDim=hdrDim;this.secondarySize=secondarySize;this.secondaryLift=secondaryLift;this.offsetMs=offsetMs;}
 }
 static Values defaults(){return new Values("normal","shadow","0","white","zho",false,true,"normal","2",0);}
 static int index(String[] values,String v,int def){if(values==null)return def;for(int i=0;i<values.length;i++)if(values[i].equals(v))return i;return def;}
 static String pick(String[] values,String v,String def){return values[index(values,v,index(values,def,0))];}
 static int clampOffset(int ms){if(ms<-5000)return -5000;if(ms>5000)return 5000;return ms;}
 /** off 开关优先。eng / hant 原样保留，其余（含 all、空）回到简体优先。 */
 static String language(boolean off,String stored){if(off)return "off";if("eng".equals(stored))return "eng";if("hant".equals(stored))return "hant";if("off".equals(stored))return "off";return "zho";}
 static String languageLabel(String lang){return LANG_LABELS[index(LANGS,language(false,lang),0)];}
 static String sizeLabel(String v){return SIZE_LABELS[index(SIZES,v,1)];}
 static String lookLabel(String v){return LOOK_LABELS[index(LOOKS,v,0)];}
 static String colorLabel(String v){return COLOR_LABELS[index(COLORS,v,0)];}
 static String liftLabel(String v){return LIFT_LABELS[index(LIFTS,v,0)];}
 static int sizeSp(String v){return SIZE_SP[index(SIZES,v,1)];}
 static int liftDp(String v){return LIFT_DP[index(LIFTS,v,0)];}
 static int colorArgb(String v){return COLOR_ARGB[index(COLORS,v,0)];}
 static int parseOffset(String raw){if(raw==null)return 0;try{return clampOffset(Integer.parseInt(raw.trim()));}catch(Exception e){return 0;}}
 static boolean flag(String raw,boolean def){if(raw==null)return def;return "true".equals(raw)||"1".equals(raw);}
 static Values read(Map<String,String> m){
  if(m==null)return defaults();
  return new Values(pick(SIZES,m.get("sub.size"),"normal"),pick(LOOKS,m.get("sub.look"),"shadow"),pick(LIFTS,m.get("sub.lift"),"0"),pick(COLORS,m.get("sub.color"),"white"),language(flag(m.get("pref.subtitleOff"),false),m.get("subtitleLanguage")),flag(m.get("sub.bold"),false),flag(m.get("sub.hdrDim"),true),pick(SIZES,m.get("sub.secondarySize"),"normal"),pick(LIFTS,m.get("sub.secondaryLift"),"2"),parseOffset(m.get("sub.offsetMs")));
 }
 static Map<String,String> write(Values v){
  Values d=v==null?defaults():v;LinkedHashMap<String,String> m=new LinkedHashMap<>();
  m.put("sub.size",d.size);m.put("sub.look",d.look);m.put("sub.lift",d.lift);m.put("sub.color",d.color);
  m.put("sub.bold",d.bold?"true":"false");m.put("sub.hdrDim",d.hdrDim?"true":"false");
  m.put("sub.secondarySize",d.secondarySize);m.put("sub.secondaryLift",d.secondaryLift);m.put("sub.offsetMs",String.valueOf(clampOffset(d.offsetMs)));
  boolean off="off".equals(d.language);m.put("pref.subtitleOff",off?"true":"false");m.put("subtitleLanguage",off?"zho":d.language);return m;
 }
 static String summary(Values v){Values d=v==null?defaults():v;return languageLabel(d.language)+" · "+sizeLabel(d.size)+" · "+colorLabel(d.color)+" · "+lookLabel(d.look)+" · "+liftLabel(d.lift);}
}
