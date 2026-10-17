package com.guocheng.mediapocket;
/**
 * Roku BIF（Binary Index Format）只读解析。公开格式：8 字节魔数、版本、帧数、时间乘数、
 * 44 字节保留，然后是帧数+1 条索引（时间戳、绝对偏移），帧体是 JPEG。
 * 服务器没生成预览时调用方拿到 null，不显示缩略图。
 */
final class BifIndex {
 static final byte[] MAGIC={(byte)0x89,'B','I','F',0x0d,0x0a,0x1a,0x0a};
 final long[] timeMs;final int[] offset;final byte[] file;
 private BifIndex(long[] timeMs,int[] offset,byte[] file){this.timeMs=timeMs;this.offset=offset;this.file=file;}
 static BifIndex parse(byte[] file){
  if(file==null||file.length<64+16)return null;
  for(int i=0;i<MAGIC.length;i++)if(file[i]!=MAGIC[i])return null;
  if(u32(file,8)!=0)return null;
  int count=u32(file,12);if(count<=0||count>20000)return null;
  long mult=u32(file,16)&0xffffffffL;if(mult<=0)mult=1000L;
  if(64L+(count+1L)*8L>file.length)return null;
  long[] times=new long[count];int[] off=new int[count+1];
  for(int i=0;i<=count;i++){int at=64+i*8;int pos=u32(file,at);int start=u32(file,at+4);if(i<count)times[i]=(pos&0xffffffffL)*mult;off[i]=start;}
  for(int i=0;i<count;i++){if(off[i]<64||off[i+1]<=off[i]||off[i+1]>file.length)return null;if(i>0&&times[i]<times[i-1])return null;}
  return new BifIndex(times,off,file);
 }
 int size(){return timeMs.length;}
 /** 不晚于 timeMs 的那一帧；还没到第一帧时用第一帧。 */
 int nearest(long atMs){if(timeMs.length==0)return -1;int lo=0,hi=timeMs.length-1,ans=0;while(lo<=hi){int mid=(lo+hi)>>>1;if(timeMs[mid]<=atMs){ans=mid;lo=mid+1;}else hi=mid-1;}return ans;}
 byte[] jpeg(int index){if(index<0||index>=timeMs.length)return null;int a=offset[index],b=offset[index+1];byte[] out=new byte[b-a];System.arraycopy(file,a,out,0,out.length);return out;}
 static int u32(byte[] d,int at){return (d[at]&0xff)|((d[at+1]&0xff)<<8)|((d[at+2]&0xff)<<16)|((d[at+3]&0xff)<<24);}
}
