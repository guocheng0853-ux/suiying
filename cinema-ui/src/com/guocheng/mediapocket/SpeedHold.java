package com.guocheng.mediapocket;
 /** 长按临时加速。默认 2.0，可改成 1.5 / 2 / 2.5 / 3。松手回到按下前的倍速，不写入 playbackSpeed。按住后左右拖：死区内保持基准倍率，超出后满屏宽对应 ±8 倍，再夹到 0.1–8.0。 */
final class SpeedHold {
 static final float HOLD=2f;
 static final float SPAN=8f;
 private SpeedHold(){}
 static float tuned(float dx,float width){return tuned(dx,width,0f);}
 static float tuned(float dx,float width,float deadPx){return tuned(dx,width,deadPx,HOLD);}
 static float tuned(float dx,float width,float deadPx,float base){
  float hold=PlaybackPrefs.clampRate(base);
  if(!(width>0f)||Float.isNaN(dx))return hold;
  float dead=deadPx>0f?deadPx:0f;
  if(Math.abs(dx)<=dead)return hold;
  float signed=dx>0f?dx-dead:dx+dead;
  return PlaybackPrefs.clampRate(hold+(signed/width)*SPAN);
 }
 /** 顶部胶囊文案，例如「加速 2.0×」。 */
 static String capsule(float rate){return String.format(java.util.Locale.ROOT,"加速 %.1f×",PlaybackPrefs.clampRate(rate));}
}
