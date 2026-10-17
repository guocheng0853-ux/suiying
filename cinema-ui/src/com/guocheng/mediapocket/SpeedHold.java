package com.guocheng.mediapocket;
/** 长按临时加速。松手回到原来的档位，不写入 playbackSpeed。左右拖动按现有倍速档吸附。 */
final class SpeedHold {
 static final float HOLD=2f;
 /** 画面宽度的这个比例算一档。 */
 static final float STEP=0.18f;
 private SpeedHold(){}
 static float tuned(float dx,float width){
  int base=0;for(int i=0;i<PlaybackPrefs.SPEEDS.length;i++)if(PlaybackPrefs.SPEEDS[i]==HOLD)base=i;
  if(!(width>0f)||Float.isNaN(dx))return PlaybackPrefs.SPEEDS[base];
  int steps=Math.round(dx/(width*STEP));
  int at=base+steps;if(at<0)at=0;if(at>=PlaybackPrefs.SPEEDS.length)at=PlaybackPrefs.SPEEDS.length-1;
  return PlaybackPrefs.SPEEDS[at];
 }
}
