package com.guocheng.mediapocket;
/**
 * 0.1.150 进度条上的「已缓存」段（纯逻辑，可单测）。
 * VLC 播 P2P 时，从当前播放位置对应的分块往后数连续已下载的分块，换算成进度条位置（按平均码率把时间换成字节）。
 * 直链在 VLC 里只预读约 1.5 秒（network-caching），不画假的缓存段；Media3 用自己的已缓冲位置。
 */
final class BufferRules {
 private BufferRules(){}
 interface Have {boolean has(int piece);}
 /** 一次最多看这么多分块，避免每次刷新都调用上万次。 */
 static final int MAX_SCAN=4096;
 /** VLC 刷新是 250ms 一次，缓存段 1 秒算一次就够。 */
 static final long INTERVAL_MS=1000;
 /** 播放位置换成文件内字节位置（按平均码率）。 */
 static long byteAt(long posMs,long durMs,long fileSize){if(durMs<=0||fileSize<=0)return 0;long p=Math.max(0,Math.min(durMs,posMs));return (long)((double)p/durMs*fileSize);}
 /** 从 fromByte 起连续已下载到的文件内字节位置；起点所在分块还没下好就返回 fromByte。 */
 static long contiguousEnd(long fileOffset,long fileSize,int pieceSize,long fromByte,Have have){
  if(pieceSize<=0||fileSize<=0||have==null)return 0;long from=Math.max(0,Math.min(fileSize,fromByte));
  int p=(int)((fileOffset+from)/pieceSize),last=(int)((fileOffset+fileSize-1)/pieceSize),scanned=0;
  while(p<=last&&scanned<MAX_SCAN&&have.has(p)){p++;scanned++;}
  if(scanned==0)return from;return Math.min(fileSize,Math.max(from,(long)p*pieceSize-fileOffset));
 }
 /** 已缓存到的字节位置换成进度条刻度（0..max）；不会小于已播放刻度。 */
 static int secondary(long bufferedByte,long fileSize,int max,int progress){if(fileSize<=0||max<=0)return Math.max(0,progress);int v=(int)Math.min(max,Math.max(0,bufferedByte)*(long)max/fileSize);return Math.max(progress,v);}
}
