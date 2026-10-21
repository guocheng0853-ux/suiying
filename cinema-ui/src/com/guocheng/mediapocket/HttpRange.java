package com.guocheng.mediapocket;
/**
 * 服务器没按 Range 回答时，要跳过多少、还能读多少。不依赖 Android，便于测试。
 * requested / contentLength / documentSize 未知时用 UNSET（-1）。
 */
final class HttpRange {
 static final long UNSET=-1;
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
