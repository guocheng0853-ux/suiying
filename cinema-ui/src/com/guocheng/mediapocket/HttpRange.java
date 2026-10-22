package com.guocheng.mediapocket;
/**
 * 服务器没按 Range 回答时，要跳过多少、还能读多少。不依赖 Android，便于测试。
 * requested / contentLength / documentSize 未知时用 UNSET（-1）。
 */
final class HttpRange {
 static final long UNSET=-1;
 /** 0.1.121：对方忽略 Range 时，用户拖动最多跳过 64 MB，再多就放弃并提示。首次打开和读 moov 不套这道上限。 */
 static final long MAX_SKIP=64L*1024*1024;
 /** 只有用户拖动进度时才拒绝过大的跳过。续播、第一次打开、读文件尾部的 moov 都允许。 */
 static boolean rejectIgnoredRange(long skip,boolean userSeek){return userSeek&&skip>MAX_SKIP;}
 /**
  * ExoPlayer 对普通 IOException 会在大约 0/1/3 秒重试。这个错误码是
  * PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE，加载策略直接放弃，不再重试。
  */
 static final int SKIP_ERROR=2008;
 static final String SKIP_MESSAGE="服务器不支持从中间读取。拖到这里要先下载很大一段，已停止。请从头播放或换一条线路。";
 /**
  * 一次用户拖动。用代数和目标时间认领这次 seek 的字节请求；同一位置的重试仍然算这一次。
  * 位置 0 的头、以及别的字节位置，都不套 64 MB 上限。
  * 缓冲里的拖动可能不再报 READY。播放头跳到目标、这次打开已经读到、或超过 TIMEOUT_MS，都放下闸。
  */
 static final class SeekCap {
  /** 和 Player.DISCONTINUITY_REASON_SEEK / SEEK_ADJUSTMENT 的值一致。 */
  static final int REASON_SEEK=1,REASON_SEEK_ADJUSTMENT=2;
  /** 连接 15 秒、读取 20 秒，留出一次拖动自己的打开和短重试，之后不再套上限。 */
  static final long TIMEOUT_MS=30000L;
  /** 比这更小的成功打开仍可能是探测，先不放下，后面更大的忽略 Range 还要能拦住。 */
  static final long PROBE_SKIP=64L*1024;
  int generation;long targetMs=-1;long byteAt=Long.MIN_VALUE;long skipSeen=-1;boolean armed;long deadlineMs;
  void arm(long targetMs){arm(targetMs,System.currentTimeMillis());}
  void arm(long targetMs,long nowMs){generation++;this.targetMs=Math.max(0,targetMs);byteAt=Long.MIN_VALUE;skipSeen=-1;armed=true;deadlineMs=nowMs+TIMEOUT_MS;}
  void disarm(){armed=false;}
  /** 播放器已经落到目标附近，这次拖动结束，后面的重新缓冲不再套上限。 */
  void landed(long positionMs){if(armed&&Math.abs(positionMs-targetMs)<=3000)armed=false;}
  /**
   * 缓冲内的拖动不会再进 BUFFERING，也不会再报 READY，播放头已经在目标上，闸可以放下。
   * 还在缓冲的拖动要等这次打开：这时就放下的话，后面忽略 Range 的大跳过就拦不住了。
   * 不是这次拖动引起的跳动（切集、跳过片头）直接放下。
   */
  void discontinuity(int reason,long positionMs,boolean ready){if(!armed)return;if(reason==REASON_SEEK||reason==REASON_SEEK_ADJUSTMENT){if(ready&&Math.abs(positionMs-targetMs)<=3000)armed=false;return;}armed=false;}
  /** 这次打开已经开始读。位置 0 的文件头不算。对方接受了 Range 就放下。忽略 Range 时，只有认领的那个位置、而且跳过超过探测大小才放下。 */
  void opened(long position,long skip){if(!armed||position<=0)return;if(skip<=0){armed=false;return;}if(position==byteAt&&skip<=MAX_SKIP&&skip>PROBE_SKIP)armed=false;}
  void expire(long nowMs){if(armed&&nowMs>deadlineMs)armed=false;}
  boolean rejects(long requestPosition,long skip){return rejects(requestPosition,skip,System.currentTimeMillis());}
  boolean rejects(long requestPosition,long skip,long nowMs){
   if(!armed||requestPosition<=0)return false;
   if(nowMs>deadlineMs){armed=false;return false;}
   if(byteAt==Long.MIN_VALUE||skip>skipSeen){byteAt=requestPosition;skipSeen=skip;}
   return requestPosition==byteAt&&skip>MAX_SKIP;
  }
 }
 static final Object GATE=new Object();
 static final SeekCap gate=new SeekCap();
 static void armSeek(long targetMs){synchronized(GATE){gate.arm(targetMs);}}
 static void disarmSeek(){synchronized(GATE){gate.disarm();}}
 static void landedSeek(long positionMs){synchronized(GATE){gate.landed(positionMs);}}
 static void discontinuitySeek(int reason,long positionMs,boolean ready){synchronized(GATE){gate.discontinuity(reason,positionMs,ready);}}
 static void openedSeek(long position,long skip){synchronized(GATE){gate.opened(position,skip);}}
 static void expireSeek(){expireSeek(System.currentTimeMillis());}
 static void expireSeek(long nowMs){synchronized(GATE){gate.expire(nowMs);}}
 static boolean rejectSeek(long requestPosition,long skip){synchronized(GATE){return gate.rejects(requestPosition,skip);}}
 static final class Plan {
  final long skip,read;final boolean empty,reject;
  Plan(long skip,long read,boolean empty,boolean reject){this.skip=skip;this.read=read;this.empty=empty;this.reject=reject;}
 }
 private HttpRange(){}
 /**
  * 200 且起点大于 0：对方忽略了 Range，跳过 position 再读。调用方指定了长度时以该长度为准。
  * 416 且起点正好是文件末尾：当作已经读完。其余 416 拒绝。
  */
 static Plan plan(int code,long position,long requested,long contentLength,long documentSize){
  if(code==416){if(documentSize>=0&&position==documentSize)return new Plan(0,requested!=UNSET?requested:0,true,false);return new Plan(0,UNSET,false,true);}
  long skip=code==200&&position>0?position:0;
  long read;if(requested!=UNSET)read=requested;else if(contentLength>=0)read=contentLength>=skip?contentLength-skip:UNSET;else read=UNSET;
  return new Plan(skip,read,false,false);
 }
}
