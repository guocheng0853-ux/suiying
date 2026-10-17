package com.guocheng.mediapocket;
import java.util.Locale;
/** 强制高刷、缓冲速度文案、快进秒数。纯规则，方便单测。 */
final class PlayerSwitchRules {
 static final int[] SEEK_SECONDS={15,30,60};
 private PlayerSwitchRules(){}
 static int seekSeconds(int saved){for(int v:SEEK_SECONDS)if(v==saved)return v;return 15;}
 static int seekMs(int seconds){return seekSeconds(seconds)*1000;}
 /** 在与当前分辨率相同的显示模式里选刷新率最高的一项。没有更高的就返回 -1。 */
 static int fastestMode(int[] widths,int[] heights,float[] rates,int currentW,int currentH,float currentRate){int best=-1;float rate=currentRate;if(widths==null||heights==null||rates==null)return -1;int n=Math.min(widths.length,Math.min(heights.length,rates.length));for(int i=0;i<n;i++){if(widths[i]!=currentW||heights[i]!=currentH)continue;if(rates[i]>rate+0.4f){rate=rates[i];best=i;}}return best;}
 static String buffer(float bytesPerSec){if(Float.isNaN(bytesPerSec)||bytesPerSec<0f)return "";long b=(long)bytesPerSec;if(b>=1024L*1024L)return String.format(Locale.ROOT,"缓冲 %.1f MB/s",b/1048576.0);return "缓冲 "+(b/1024)+" KB/s";}
 static String seekLabel(int seconds){return "快进快退："+seekSeconds(seconds)+" 秒";}
}
