package com.guocheng.mediapocket;
import java.util.Locale;
/** 0.1.155 播放收尾与状态的纯规则（可单测）。 */
final class PlaybackEndRules {
 private PlaybackEndRules(){}
 /**
  * VLC 报「读到结尾」时，判断是不是片源提前断了。底座只在 VLC 给出时长（&gt;15 秒）时才比较，
  * Emby 转码串流时长常是 0，于是断流也被当成「本集播放完毕」：清掉续播位置，还会弹下一集。
  * 已知全长：离结尾还差 5 分钟且超过 10% 算提前结束（片尾字幕不算）。全长未知：两分钟内就结束算提前结束。
  */
 static boolean premature(long lastPosMs,long runtimeMs,long durationMs){
  long full=runtimeMs>0?runtimeMs:durationMs>15000?durationMs:0;long pos=Math.max(0,lastPosMs);
  if(full<=0)return pos<120000;
  return pos<full-Math.max(300000,full/10);
 }
 static String mmss(long ms){long s=Math.max(0,ms)/1000;long h=s/3600,m=s/60%60,sec=s%60;return h>0?String.format(Locale.ROOT,"%d:%02d:%02d",h,m,sec):String.format(Locale.ROOT,"%d:%02d",m,sec);}
 /** 提前结束后的状态文字；含「失败」，失败卡片和 Emby 自动恢复都会接上。 */
 static String prematureText(long lastPosMs,long runtimeMs){return "读取失败：片源在 "+mmss(lastPosMs)+" 提前结束"+(runtimeMs>0?"（全长 "+mmss(runtimeMs)+"）":"");}
 /** Emby 的 RunTimeTicks（100 纳秒）换成毫秒。 */
 static long ticksToMs(long ticks){return ticks>0?ticks/10000:0;}
 /**
  * 底座只在 VLC 发出画面输出事件（Vout）后才认「出画面」。这台机器上这个事件不来，于是状态一直停在「准备连接…」，
  * 30 秒后还会按「启动等待超时」换源。视频轨已有尺寸、正在播放、时间在走，就可以认定已经出画面。
  */
 static boolean playingWithoutVout(boolean rendered,boolean outputReady,boolean playing,int videoWidth,long timeMs){return !rendered&&!outputReady&&playing&&videoWidth>0&&timeMs>0;}
 /** 图片字幕（PGS / DVD / DVB / VobSub）不能当文本时间轴下载解析，只能交给播放器内嵌字幕显示。 */
 static boolean imageSubtitle(String codec,String name){String c=((codec==null?"":codec)+" "+(name==null?"":name)).toLowerCase(Locale.ROOT);return c.contains("pgs")||c.contains("hdmv")||c.contains("dvd_sub")||c.contains("dvdsub")||c.contains("dvb_sub")||c.contains("dvbsub")||c.contains("vobsub")||c.contains("xsub")||c.matches(".*\\bsup\\b.*");}
 /** 日志里只记接口形状：去掉查询参数，长编号换成 {id}，不带服务器地址和令牌。 */
 static String pathPattern(String path){if(path==null)return "";String p=path;int q=p.indexOf('?');if(q>=0)p=p.substring(0,q);return p.replaceAll("/[0-9a-fA-F]{16,}(?=/|$)","/{id}").replaceAll("/\\d+(?=/|$)","/{n}");}
 /** 手动切换解码只对当前这次播放有效；旧版本留下的「软解」在升级后回到自动（硬解优先）。 */
 static boolean resetLegacyDecode(String mode,boolean migrated){return !migrated&&"software".equals(mode);}
}
