package com.guocheng.mediapocket;
import java.util.Locale;
/** 顶栏状态行：系统时间、网速、电量。每一项都能单独关掉。 */
final class PlayerStatus {
 private PlayerStatus(){}
 static String clock(int hour,int minute){int h=Math.floorMod(hour,24),m=Math.floorMod(minute,60);return String.format(Locale.ROOT,"%02d:%02d",h,m);}
 static String net(long bytesPerSec){long b=bytesPerSec<0?0:bytesPerSec;return (b/1024)+" KB/s";}
 /** 0–6 格、7 满格、8 充电，一共 9 档。 */
 static int batteryLevel(int percent,boolean charging){if(charging)return 8;if(percent>=100)return 7;if(percent<0)percent=0;int bars=percent*7/100;return bars>6?6:bars;}
 static String battery(int percent,boolean charging){
  int level=batteryLevel(percent,charging);int p=percent<0?0:percent>100?100:percent;
  String[] bars={"▁","▂","▃","▄","▅","▆","▇","█","⚡"};
  return bars[level]+p+"%";
 }
 static String batteryPercent(int percent){int p=percent<0?0:percent>100?100:percent;return p+"%";}
 static String line(boolean time,boolean net,boolean battery,String clock,String speed,String batt){
  StringBuilder s=new StringBuilder();
  if(time&&clock!=null&&!clock.isEmpty())s.append(clock);
  if(net&&speed!=null&&!speed.isEmpty()){if(s.length()>0)s.append("  ");s.append(speed);}
  if(battery&&batt!=null&&!batt.isEmpty()){if(s.length()>0)s.append("  ");s.append(batt);}
  return s.toString();
 }
}
