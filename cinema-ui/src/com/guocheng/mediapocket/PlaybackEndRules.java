package com.guocheng.mediapocket;
import java.util.Locale;
/** 0.1.155 播放收尾与状态的纯规则（可单测）。 */
final class PlaybackEndRules {
 private PlaybackEndRules(){}
 /** libvlc MediaPlayer.State.Ended。EndReached 之后播放器停在这里；提前断流不看状态栏文案。 */
 static final int VLC_ENDED=6;
 static boolean endReached(int playerState){return playerState==VLC_ENDED;}
 /**
  * VLC 报「读到结尾」时，判断是不是片源提前断了。底座只在 VLC 给出时长（&gt;15 秒）时才比较，
  * Emby 转码串流时长常是 0，于是断流也被当成播完：清掉续播位置，还会弹下一集。
  * 已知全长：离结尾还差 5 分钟且超过 10% 算提前结束（片尾字幕不算）。
  * 全长未知（没有 Emby runtime，VLC 时长也不可用）：两分钟内结束算提前结束。
  * 这时一部本身就短于两分钟、又没有时长的片子，和真正的提前断流无法区分，仍按提前断流处理；
  * 超过两分钟才结束的，当成正常播完。阈值与 0.1.155 相同。
  */
 static boolean premature(long lastPosMs,long runtimeMs,long durationMs){
  long full=runtimeMs>0?runtimeMs:durationMs>15000?durationMs:0;long pos=Math.max(0,lastPosMs);
  if(full<=0)return pos<120000;
  return pos<full-Math.max(300000,full/10);
 }
 /** 终端且播放器已 Ended，并且位置规则判定为提前。不读界面文字。 */
 static boolean prematureFlag(boolean terminal,int playerState,long lastPosMs,long runtimeMs,long durationMs){return terminal&&endReached(playerState)&&premature(lastPosMs,runtimeMs,durationMs);}
 /** 自动下一集只把「已经终端、且不是提前断流」当成播完。 */
 static boolean naturalFinish(boolean terminal,boolean premature){return terminal&&!premature;}
 static String mmss(long ms){long s=Math.max(0,ms)/1000;long h=s/3600,m=s/60%60,sec=s%60;return h>0?String.format(Locale.ROOT,"%d:%02d:%02d",h,m,sec):String.format(Locale.ROOT,"%d:%02d",m,sec);}
 /** 提前结束后仍显示给用户和调试日志的说明。失败卡片和 Emby 恢复看显式标记，不看这句话里有没有「失败」。 */
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
 /**
  * 播放中改过解码会留下 open 标记。正常退出会清掉；进程在播放中被杀则标记还在，
  * 下次冷启动按切换前的档还原。空串表示原来没有这项偏好。返回 null 表示这次不用动。
  */
 static String[] unwindDecodeSession(boolean open,String prevMode,String prevTier){if(!open)return null;return new String[]{prevMode==null?"":prevMode,prevTier==null?"":prevTier};}
 /** 换了一次播放（attempt 或片源 id）就丢掉上一次的提前断流标记。第一次见到不丢。 */
 static boolean dropPremature(String previousAttempt,String attempt){return previousAttempt!=null&&!previousAttempt.equals(attempt);}
}
