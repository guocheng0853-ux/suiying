package com.guocheng.mediapocket;
/** 长按临时加速到 2.0。松手回到按下前的倍速，不写入 playbackSpeed。按住后左右拖：32dp 死区内保持 2.0，超出后满屏宽对应 ±8 倍，再夹到 0.1–8.0。 */
final class SpeedHold {
 static final float HOLD=2f;
 static final float SPAN=8f;
 private SpeedHold(){}
 static float tuned(float dx,float width){return tuned(dx,width,0f);}
 static float tuned(float dx,float width,float deadPx){
  if(!(width>0f)||Float.isNaN(dx))return HOLD;
  float dead=deadPx>0f?deadPx:0f;
  if(Math.abs(dx)<=dead)return HOLD;
  float signed=dx>0f?dx-dead:dx+dead;
  return PlaybackPrefs.clampRate(HOLD+(signed/width)*SPAN);
 }
 /** 顶部胶囊文案，例如「⏩ 2.0X」。 */
 static String capsule(float rate){return String.format(java.util.Locale.ROOT,"⏩ %.1fX",PlaybackPrefs.clampRate(rate));}
}
