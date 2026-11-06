package com.guocheng.mediapocket;
import java.util.*;
public class SubtitleVlcRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] x){
  List<String> d=SubtitleVlcRules.options(SubtitlePrefs.defaults());
  check(d.contains("freetype-rel-fontsize=16")&&d.contains("freetype-color=16777215")&&d.contains("no-freetype-bold")&&d.contains("sub-margin=0"),"defaults: normal white, not bold, bottom "+d);
  SubtitlePrefs.Values big=new SubtitlePrefs.Values("huge","box","3","yellow","zho",true,true,"normal","2",0);List<String> b=SubtitleVlcRules.options(big);
  check(b.contains("freetype-rel-fontsize=10")&&b.contains("freetype-color="+(0xffe14a))&&b.contains("freetype-bold")&&b.contains("freetype-background-opacity=170")&&b.contains("sub-margin=288"),"huge yellow bold box, lifted "+b);
  check(SubtitleVlcRules.options(new SubtitlePrefs.Values("small","outline","0","white","zho",false,true,"normal","2",0)).contains("freetype-outline-thickness=4"),"outline look");
  check(SubtitleVlcRules.global(big).get(0).startsWith("--freetype")&&SubtitleVlcRules.media(big).get(0).startsWith(":freetype"),"prefixes");
  SubtitlePrefs.Values delayed=new SubtitlePrefs.Values("normal","shadow","0","white","eng",false,true,"large","4",1200);
  check(SubtitleVlcRules.signature(SubtitlePrefs.defaults()).equals(SubtitleVlcRules.signature(delayed)),"delay, language and second subtitle do not force a VLC reopen");
  check(!SubtitleVlcRules.signature(SubtitlePrefs.defaults()).equals(SubtitleVlcRules.signature(big)),"style change does");
  System.out.println("PASS: VLC freetype options follow subtitle settings; only style changes reopen.");
 }
}
