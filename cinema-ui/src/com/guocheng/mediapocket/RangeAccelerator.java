package com.guocheng.mediapocket;
import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;
/**
 * 多线程分段加速：网盘类直链常按单连接限速。对支持 Range 的大文件，同时开 THREADS 个连接按 1MB 分段预取，
 * 再按顺序写给播放器。首段直接用已打开的上游连接；预取窗口有上限（内存约 WINDOW MB），播放器断开/拖动时整组取消。
 * 只对 ≥ MIN_BYTES 且服务器确认支持 Range 的响应启用；m3u8 与小文件仍走原样转发。
 */
final class RangeAccelerator {
 static final long MIN_BYTES=8L*1024*1024;
 static final int CHUNK=1024*1024,THREADS=4,WINDOW=8;
 private RangeAccelerator(){}

 /** 返回本次响应覆盖的绝对字节区间 {start,end}；不可分段时返回 null。 */
 static long[] span(int code,long length,String contentRange,String acceptRanges){
  if(length<=0)return null;
  if(code==206&&contentRange!=null){java.util.regex.Matcher m=java.util.regex.Pattern.compile("bytes\\s+(\\d+)-(\\d+)/(\\d+|\\*)").matcher(contentRange.trim());if(m.find()){long a=Long.parseLong(m.group(1)),b=Long.parseLong(m.group(2));return b-a+1==length?new long[]{a,b}:null;}return null;}
  if(code==200&&acceptRanges!=null&&acceptRanges.toLowerCase(Locale.ROOT).contains("bytes"))return new long[]{0,length-1};
  return null;
 }

 static void stream(HttpURLConnection first,InputStream firstBody,String url,Map<String,String> headers,long start,long end,OutputStream out)throws IOException{
  ExecutorService pool=Executors.newFixedThreadPool(THREADS,r->{Thread t=new Thread(r,"mp-range");t.setDaemon(true);return t;});
  long chunks=(end-start)/CHUNK+1;ArrayDeque<Future<byte[]>> queue=new ArrayDeque<>();long scheduled=0;
  try{
   // 第 0 段：读已打开的连接，读满后断开（剩余部分由并行请求获取）
   long firstLen=Math.min(CHUNK,end-start+1);
   queue.add(pool.submit(()->{try{return readExactly(firstBody,firstLen);}finally{first.disconnect();}}));scheduled=1;
   for(long k=0;k<chunks;k++){
    while(scheduled<chunks&&scheduled<k+WINDOW){long a=start+scheduled*CHUNK,b=Math.min(end,a+CHUNK-1);queue.add(pool.submit(()->fetch(url,headers,a,b)));scheduled++;}
    byte[] data;
    try{data=queue.poll().get(60,TimeUnit.SECONDS);}
    catch(ExecutionException e){throw e.getCause() instanceof IOException?(IOException)e.getCause():new IOException(e.getCause());}
    catch(TimeoutException e){throw new SocketTimeoutException("分段下载超时");}
    catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException();}
    out.write(data);// 播放器断开时这里抛出，finally 里取消其余分段
   }
   out.flush();
  }finally{for(Future<byte[]> f:queue)f.cancel(true);pool.shutdownNow();}
 }
 /** 单段：带 Range 请求，必须返回 206 且长度正确；失败重试 2 次。读满即关闭输入流，连接回池复用。 */
 static byte[] fetch(String url,Map<String,String> headers,long a,long b)throws IOException{
  IOException last=null;
  for(int attempt=0;attempt<3;attempt++){
   if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
   HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);
   if(!headers.containsKey("User-Agent"))c.setRequestProperty("User-Agent","Mozilla/5.0");
   for(Map.Entry<String,String> e:headers.entrySet())c.setRequestProperty(e.getKey(),e.getValue());
   c.setRequestProperty("Range","bytes="+a+"-"+b);c.setRequestProperty("Accept-Encoding","identity");
   boolean ok=false;
   try{
    int code=c.getResponseCode();if(code!=206)throw new IOException("分段请求返回 HTTP "+code);
    byte[] data;try(InputStream in=c.getInputStream()){data=readExactly(in,b-a+1);}
    ok=true;return data;
   }catch(IOException e){last=e;}
   finally{if(!ok)c.disconnect();}
  }
  throw last;
 }
 static byte[] readExactly(InputStream in,long n)throws IOException{
  byte[] buf=new byte[(int)n];int off=0;
  while(off<n){int r=in.read(buf,off,(int)n-off);if(r<0)throw new EOFException("分段数据不完整");off+=r;}
  return buf;
 }
}
