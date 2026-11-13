package com.guocheng.mediapocket;
import java.util.Locale;
/**
 * 0.1.183：影视仓片源播到一半断掉后的自动重连。不依赖 Android。
 * 影视仓站点给的播放地址大多带时效（几十分钟的 token、会换节点的 m3u8），播到一半地址失效，播放器只会报读取失败或
 * 「提前结束」。原来的处理是直接弹「暂时无法播放」，手动点「重新连接」也只是拿同一个旧地址重开，所以总是不行。
 * 现在的顺序：① 向同一站点、同一线路重新要一次播放地址，从断掉的位置续播；② 还不行，换同一集的其他线路（最多 ALT_MAX 条）；
 * ③ 都不行才交回原来的流程（去其他站点找同片 / 弹卡片）。
 */
final class FusionReconnectRules {
 private FusionReconnectRules(){}
 /** 同一线路连续最多重取几次；同一集最多换几条其他线路；同一集一共最多自动处理几次。 */
 static final int SAME_MAX=2,ALT_MAX=3,TOTAL_MAX=8;
 /** 至少播到这里才算「播到一半」。一开始就打不开的地址刚取到，重取没有意义，仍走原来的换站流程。 */
 static final long MIN_PLAYED_MS=5000;
 /** 重连后又正常播了这么久，连续计数清零：隔几十分钟地址再过期时还能再重连。 */
 static final long RESET_AFTER_MS=30000;
 static final int NONE=0,SAME=1,ALT=2;
 /** 能不能重取：要有站点配置、站点、线路名和线路值；磁力和 Emby 不走这里。 */
 static boolean route(String config,String site,String flag,String value,String embyItem){
  if(config==null||config.isEmpty()||site==null||site.isEmpty()||flag==null||flag.isEmpty()||value==null||value.isEmpty())return false;
  if(embyItem!=null&&!embyItem.isEmpty())return false;
  return !value.trim().toLowerCase(Locale.ROOT).startsWith("magnet:");
 }
 /** blocked：用户正开着面板或菜单（可能正在自己选片源），这时不抢。 */
 static boolean eligible(boolean enabled,boolean route,boolean failure,boolean active,boolean blocked,long playedMs,int total){
  return enabled&&route&&failure&&active&&!blocked&&playedMs>=MIN_PLAYED_MS&&total<TOTAL_MAX;
 }
 /** 下一步做什么。same：同一线路已连续重取的次数；altTried / altCount：其他线路已试过几条、一共有几条（还没查过详情时传 -1）。 */
 static int next(int same,int altTried,int altCount,int total){
  if(total>=TOTAL_MAX)return NONE;
  if(same<SAME_MAX)return SAME;
  if(altCount<0)return altTried<ALT_MAX?ALT:NONE;
  return altTried<Math.min(ALT_MAX,altCount)?ALT:NONE;
 }
 /** 上次重连之后是否已经稳定播放过一段，可以把连续计数清零。往回拖进度不算。 */
 static boolean settled(long positionMs,long attemptPositionMs){return attemptPositionMs>=0&&positionMs-attemptPositionMs>=RESET_AFTER_MS;}
 static String title(){return "正在重新连接";}
 static String hint(int step,int same,String flag){
  if(step==ALT)return "原线路没有恢复，正在换到「"+(flag==null||flag.isEmpty()?"其他线路":flag)+"」…";
  return "播放地址失效了，正在重新获取（"+Math.max(1,same)+"/"+SAME_MAX+"）…";
 }
 static String resumed(long positionMs){return "已重新连接 · 从 "+PlaybackEndRules.mmss(positionMs)+" 继续";}
 static String exhausted(){return "已自动重新获取地址并换过线路，仍然连不上。可以再试一次，或选择其他片源。";}
}
