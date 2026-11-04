package com.guocheng.mediapocket;
import java.util.*;
/**
 * 0.1.169：把「字幕设置」翻成 VLC 字幕渲染（freetype）的选项，不依赖 Android。
 * 只作用于 VLC 自己画的文字字幕（片子里内嵌的 SRT 等）。ASS 由 libass 按字幕文件自己的样式画，PGS 是图片，都不受影响。
 */
final class SubtitleVlcRules {
 private SubtitleVlcRules(){}
 /** freetype-rel-fontsize：字高 = 画面高 / 这个数，数越小字越大。对应 小 / 标准 / 大 / 特大。 */
 static final int[] REL_SIZE={20,16,13,10};
 static List<String> options(SubtitlePrefs.Values v){
  SubtitlePrefs.Values d=v==null?SubtitlePrefs.defaults():v;ArrayList<String> o=new ArrayList<>();
  o.add("freetype-rel-fontsize="+REL_SIZE[SubtitlePrefs.index(SubtitlePrefs.SIZES,d.size,1)]);
  o.add("freetype-color="+(SubtitlePrefs.colorArgb(d.color)&0xffffff));
  o.add(d.bold?"freetype-bold":"no-freetype-bold");
  if("outline".equals(d.look)){o.add("freetype-outline-thickness=4");o.add("freetype-shadow-opacity=0");o.add("freetype-background-opacity=0");}
  else if("box".equals(d.look)){o.add("freetype-outline-thickness=0");o.add("freetype-shadow-opacity=0");o.add("freetype-background-color=0");o.add("freetype-background-opacity=170");}
  else{o.add("freetype-outline-thickness=2");o.add("freetype-shadow-opacity=160");o.add("freetype-background-opacity=0");}
  o.add("sub-margin="+SubtitlePrefs.liftDp(d.lift)*3);
  return o;
 }
 static List<String> global(SubtitlePrefs.Values v){ArrayList<String> o=new ArrayList<>();for(String s:options(v))o.add("--"+s);return o;}
 static List<String> media(SubtitlePrefs.Values v){ArrayList<String> o=new ArrayList<>();for(String s:options(v))o.add(":"+s);return o;}
 /** 设置有没有改到 VLC 用的部分（延迟、语言、第二字幕不算）。 */
 static String signature(SubtitlePrefs.Values v){return String.join("|",options(v));}
}
