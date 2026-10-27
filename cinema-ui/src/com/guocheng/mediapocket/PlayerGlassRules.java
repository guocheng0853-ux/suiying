package com.guocheng.mediapocket;
import java.util.*;
/** 0.1.140 播放器玻璃、剩余时间和底栏取舍的纯逻辑（不依赖 Android，便于测试）。 */
final class PlayerGlassRules {
 private PlayerGlassRules(){}
 /** 播放相关可点区域。Android 规范是 48dp，旧的 40 / 44 不够。 */
 static final int TOUCH_DP=48;
 /** 暖炭玻璃的不透明档：暗场用 airy，亮场白字至少加到 strong。 */
 static final int GLASS_RGB=0x1C1A18,AIRY_ALPHA=0x8C,STRONG_ALPHA=0xB3;
 /** 底栏「剩…看完」。比纯白收一档，叠在加厚玻璃上仍要 ≥4.5:1。 */
 static final int REMAIN_TEXT=0xFFE7E2D6,WHITE=0xFFFFFFFF;
 /** 旧版短暂快进反馈的兼容时长；当前控制层显示时三个播放键一起出现。 */
 static final long SEEK_FLASH_MS=1600L;
 static final double MIN_CONTRAST=4.5;

 /** 「剩 29 分，22:41 看完」。不到一分钟写「马上看完」；时长未知返回空。整点小时不写「0 分」。 */
 static String remaining(long posMs,long durMs,float speed,long nowMs,TimeZone zone){
  if(durMs<=0||posMs<0||posMs>durMs)return "";
  long left=durMs-posMs;float rate=speed<=0?1f:speed;long real=(long)(left/rate);
  if(real<60_000)return "马上看完";
  long minutes=(real+59_999)/60_000;
  Calendar c=Calendar.getInstance(zone==null?TimeZone.getDefault():zone);c.setTimeInMillis(nowMs+real);
  String at=String.format(Locale.ROOT,"%02d:%02d",c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE));
  long h=minutes/60,m=minutes%60;
  String span=h<=0?minutes+" 分":m==0?h+" 小时":h+" 小时 "+m+" 分";
  return "剩 "+span+"，"+at+" 看完";
 }

 /** ±N 在 until 之前可见。播放盘另算，拖进度时整座中岛收起。 */
 static boolean seekKeysOn(long nowMs,long untilMs){return nowMs<untilMs;}
 /** 底栏常驻只留传输：上一集、下一集、倍速。音轨、字幕、选集进「更多」。Media3 和 VLC 同一套。 */
 /** 0.1.141 参考 Apple TV：底部右侧一排小图标常驻——上一集、下一集、音轨、字幕、选集、倍速；「更多」里不再重复这几项。 */
 static boolean showsOnBar(String action){return "prev".equals(action)||"next".equals(action)||"speed".equals(action)||"audio".equals(action)||"subtitle".equals(action)||"episodes".equals(action);}
 static boolean tuckedInMore(String action){return false;}
 /** 控件出现时整块画面压暗的不透明度（Apple TV 做法：不靠面板托字，整体压暗约55%，保证白场中央白色控件对比度）。 */
 static final int DIM_ALPHA=0x8C;
 /** 已播时间格式「4:46」/「1:02:03」；播放栏剩余提示使用 remaining。 */
 static String clockText(long ms){long s=Math.max(0,ms)/1000,h=s/3600,m=(s%3600)/60,sec=s%60;return h>0?String.format(Locale.ROOT,"%d:%02d:%02d",h,m,sec):String.format(Locale.ROOT,"%d:%02d",m,sec);}
 static String minus(long posMs,long durMs){if(durMs<=0)return "";return "−"+clockText(Math.max(0,durMs-Math.max(0,posMs)));}

 static double channel(int c){double n=(c&255)/255.0;return n<=0.04045?n/12.92:Math.pow((n+0.055)/1.055,2.4);}
 static double luminance(int rgb){return 0.2126*channel(rgb>>16)+0.7152*channel(rgb>>8)+0.0722*channel(rgb);}
 static double contrast(int fg,int bg){double a=luminance(fg),b=luminance(bg);double hi=Math.max(a,b),lo=Math.min(a,b);return (hi+.05)/(lo+.05);}
 /** 暖炭玻璃叠在亮度 sceneLuma（0 黑，255 白）上之后的不透明底色。 */
 static int composite(int alpha,int sceneLuma){
  int s=Math.max(0,Math.min(255,sceneLuma)),a=Math.max(0,Math.min(255,alpha));
  int r=(a*0x1C+(255-a)*s)/255,g=(a*0x1A+(255-a)*s)/255,b=(a*0x18+(255-a)*s)/255;
  return (r<<16)|(g<<8)|b;
 }
 /**
  * fg 在这块玻璃上达到 4.5:1 所需的最小不透明度。
  * 暗场可以停在 airy；亮场至少加到 strong，白字或强调色仍不够时继续加厚。
  */
 static int glassAlpha(int sceneLuma,int fg){
  int need=255;
  for(int a=AIRY_ALPHA;a<=255;a++)if(contrast(fg,composite(a,sceneLuma))>=MIN_CONTRAST){need=a;break;}
  if(sceneLuma>=128)need=Math.max(need,STRONG_ALPHA);
  return need;
 }
 static int glassColor(int alpha){return (Math.max(0,Math.min(255,alpha))<<24)|GLASS_RGB;}
 /** 底栏同时有白字、剩余句和强调色，按最挑的那个加厚。gold 时强调色是影院暗金。 */
 static int panelAlpha(int sceneLuma,boolean gold){
  int accent=playbackAccent(gold);
  return Math.max(glassAlpha(sceneLuma,WHITE),Math.max(glassAlpha(sceneLuma,REMAIN_TEXT),glassAlpha(sceneLuma,accent)));
 }
 /** 播放强调色。影院暗金外观落到 #e0b25c，其余仍是暖陶。 */
 static int playbackAccent(boolean gold){return gold?0xffe0b25c:0xffe6a58d;}
}
