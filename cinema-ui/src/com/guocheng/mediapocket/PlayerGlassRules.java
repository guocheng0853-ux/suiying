package com.guocheng.mediapocket;
import java.util.*;
/** 0.1.139 播放器底部面板「剩余时间」一行的纯逻辑（不依赖 Android，便于测试）。 */
final class PlayerGlassRules {
 private PlayerGlassRules(){}
 /** 剩余时间和预计看完时刻（方向 3 借来的一行）：「剩 28 分钟，22:41 看完」。不到一分钟写「马上看完」；时长未知返回空。 */
 static String remaining(long posMs,long durMs,float speed,long nowMs,TimeZone zone){if(durMs<=0||posMs<0||posMs>durMs)return "";long left=durMs-posMs;float rate=speed<=0?1f:speed;long real=(long)(left/rate);if(real<60_000)return "马上看完";long minutes=(real+59_999)/60_000;Calendar c=Calendar.getInstance(zone==null?TimeZone.getDefault():zone);c.setTimeInMillis(nowMs+real);String at=String.format(Locale.ROOT,"%02d:%02d",c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE));String span=minutes>=60?(minutes/60)+" 小时 "+(minutes%60)+" 分钟":minutes+" 分钟";return "剩 "+span+"，"+at+" 看完";}
}
