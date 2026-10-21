package com.guocheng.mediapocket;
/** 封面解码尺寸。不依赖 Android，方便核对 inSampleSize。 */
final class PosterSample {
 private PosterSample(){}
 /**
  * 最大的 2 的幂，使抽样后的宽度仍然不小于目标宽。
  * 源图更窄、或目标无效时返回 1。调用方再用密度缩放或 createScaledBitmap 收到目标宽。
  */
 static int sampleSize(int sourceWidth,int targetWidth){
  if(sourceWidth<=0||targetWidth<=0)return 1;
  int sample=1;
  while(sample<1024&&sourceWidth/(sample*2)>=targetWidth)sample*=2;
  return sample;
 }
 /** 内存缓存大约是堆的八分之一，至少 8 MB。 */
 static int cacheBytes(long maxMemory){
  if(maxMemory<=0)return 8*1024*1024;
  long cap=maxMemory/8;
  if(cap<8L*1024*1024)cap=8L*1024*1024;
  if(cap>Integer.MAX_VALUE)cap=Integer.MAX_VALUE;
  return (int)cap;
 }
 /** 大图背景按屏幕宽度的 1 倍解，不再放宽到 2 倍。 */
 static int backdropWidth(int screenWidth){return screenWidth<=0?1:screenWidth;}
}
