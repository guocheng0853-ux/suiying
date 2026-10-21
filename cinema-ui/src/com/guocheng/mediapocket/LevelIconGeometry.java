package com.guocheng.mediapocket;
/** 音量 / 亮度指示图标，画在 24×24 盒子里。播放页和预览脚本共用同一组路径。 */
final class LevelIconGeometry {
 static final int MUTE=1,LOW=2,MID=3,HIGH=4,SUN=5;
 interface Sink {void fill(float[] xy);void stroke(float[] xy);void circle(float cx,float cy,float r);void line(float x1,float y1,float x2,float y2);}
 private LevelIconGeometry(){}
 static int volume(int percent){if(percent<=0)return MUTE;if(percent<34)return LOW;if(percent<67)return MID;return HIGH;}
 static void draw(int type,Sink s){
  if(type==SUN){s.circle(12f,12f,3.6f);for(int i=0;i<8;i++){double a=Math.PI*i/4.0;float c=(float)Math.cos(a),n=(float)Math.sin(a);s.line(12f+c*6.5f,12f+n*6.5f,12f+c*9.6f,12f+n*9.6f);}return;}
  s.fill(new float[]{3.5f,9f,3.5f,15f,8f,15f,13.2f,19.2f,13.2f,4.8f,8f,9f});
  if(type==MUTE){s.line(16.2f,8.4f,21.4f,15.6f);s.line(21.4f,8.4f,16.2f,15.6f);return;}
  s.stroke(new float[]{16.2f,10.2f,17.8f,12f,16.2f,13.8f});
  if(type==LOW)return;
  s.stroke(new float[]{18.6f,8.2f,20.8f,12f,18.6f,15.8f});
  if(type==MID)return;
  s.stroke(new float[]{20.6f,6.2f,23.2f,12f,20.6f,17.8f});
 }
}
