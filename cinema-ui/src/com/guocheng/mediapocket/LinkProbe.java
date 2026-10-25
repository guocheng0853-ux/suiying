package com.guocheng.mediapocket;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;
import java.util.concurrent.*;
/**
 * 播放前的真实链接检查：带线路声明的全部请求头取首字节，识别视频容器、m3u8、网页、JSON 和 HTTP 错误。
 * m3u8 会继续读取子播放列表与首个分片，所以“目录/详情能打开”和“地址真的给出视频数据”是两件分开记录的事。
 * 只说明地址此刻是否返回了像视频的数据，不代表解码器一定能播放（编码格式、DRM 仍可能失败）。
 */
public final class LinkProbe {
 static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"link-probe-budget");t.setDaemon(true);return t;});
 static final ExecutorService WORK=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"link-probe-work");t.setDaemon(true);return t;});
 public static final class Result {
  public final boolean media;      // 读到了可识别的视频/音频数据（含 HLS 首分片）
  public final boolean definitive; // 明确不是视频：HTTP 错误、网页、JSON、空响应
  public final String kind,detail;
  Result(boolean m,boolean d,String k,String s){media=m;definitive=d;kind=k;detail=s;}
  public String summary(){return kind+(detail.isEmpty()?"":" · "+detail);}
 }
 interface Fetcher {Response get(String url,Map<String,String> headers,int limit,int timeoutMs)throws IOException;}
 static final class Response {final int code;final String type,finalUrl;final byte[] body;Response(int c,String t,String u,byte[] b){code=c;type=t==null?"":t;finalUrl=u;body=b;}}
 static Fetcher fetcher=LinkProbe::http;
 private LinkProbe(){}

 public static Result probe(String url,Map<String,String> headers,int budgetMs){
  int budget=Math.max(1000,budgetMs);
  long deadline=System.currentTimeMillis()+budget;
  Map<String,String> h=headers==null?new LinkedHashMap<>():HeaderRoute.canonical(headers);
  try{
   String scheme=URI.create(url.trim()).getScheme();
   if(scheme==null||!(scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https")))return new Result(false,false,"非 HTTP 地址","交给播放器处理："+scheme);
  }catch(Exception e){return new Result(false,false,"检查未完成",reason(e));}
  final String target=url.trim();
  Future<Result> future=WORK.submit(()->{
   try{return follow(target,h,deadline,0);}
   catch(Exception e){return new Result(false,false,"检查未完成",reason(e));}
  });
  try{return future.get(budget,TimeUnit.MILLISECONDS);}
  catch(TimeoutException e){future.cancel(true);return new Result(false,false,"检查未完成","超时");}
  catch(InterruptedException e){Thread.currentThread().interrupt();future.cancel(true);return new Result(false,false,"检查未完成","超时");}
  catch(ExecutionException e){Throwable c=e.getCause()==null?e:e.getCause();return new Result(false,false,"检查未完成",reason(c));}
 }
 static Result follow(String url,Map<String,String> h,long deadline,int depth)throws IOException{
  Response r=fetcher.get(url,h,512*1024,left(deadline));
  if(r.code>=400)return new Result(false,true,"HTTP "+r.code,host(url)+" 拒绝或没有这个文件");
  String kind=kind(r.body,r.type);
  if(kind.equals("m3u8")){
   String text=new String(r.body,StandardCharsets.UTF_8);
   if(depth<3&&text.contains("#EXT-X-STREAM-INF")){String next=firstUri(text);if(next==null)return new Result(false,true,"m3u8","主播放列表里没有子列表地址");return follow(new URL(new URL(r.finalUrl),next).toString(),h,deadline,depth+1);}
   String segment=firstUri(text);
   if(segment==null)return new Result(false,true,"m3u8","播放列表没有分片");
   String abs=new URL(new URL(r.finalUrl),segment).toString();
   Response s=fetcher.get(abs,h,4096,left(deadline));
   if(s.code>=400)return new Result(false,true,"m3u8 · 首分片 HTTP "+s.code,"播放列表可读，但分片被拒绝");
   String sk=kind(s.body,s.type);
   if(sk.equals("网页")||sk.equals("JSON")||sk.equals("空响应"))return new Result(false,true,"m3u8 · 首分片是"+sk,"分片地址没有返回视频数据");
   if(sk.equals("PNG 伪装分片"))return new Result(true,false,"m3u8 · 首分片带图片头","部分播放器无法解码这种伪装分片");
   return new Result(true,false,"m3u8 · 首分片 "+sk,"HTTP "+s.code+(text.contains("#EXT-X-KEY")&&!text.contains("METHOD=NONE")?" · 加密分片":""));
  }
  if(kind.equals("网页"))return new Result(false,true,"返回网页","这是播放页，不是视频地址（需要网页解析/嗅探）");
  if(kind.equals("JSON"))return new Result(false,true,"返回 JSON","接口还需要再解析一次才是视频地址");
  if(kind.equals("空响应"))return new Result(false,true,"空响应","服务器没有返回数据");
  if(kind.equals("未知"))return new Result(false,false,"未识别格式","HTTP "+r.code+(r.type.isEmpty()?"":" · "+r.type)+"，交给播放器尝试");
  return new Result(true,false,kind,"HTTP "+r.code);
 }
 static int left(long deadline)throws IOException{long l=deadline-System.currentTimeMillis();if(l<300)throw new SocketTimeoutException("检查超时");return (int)Math.min(l,15000);}
 static String firstUri(String text){for(String line:text.split("\r?\n")){String t=line.trim();if(!t.isEmpty()&&!t.startsWith("#"))return t;}return null;}
 static String kind(byte[] b,String type){
  int n=b.length;if(n==0)return "空响应";
  if(HeaderRoute.Sniff.isPlaylist(b,Math.min(n,16)))return "m3u8";
  if(n>=8&&new String(b,4,4,StandardCharsets.ISO_8859_1).matches("ftyp|moov|styp|moof|mdat|free|wide"))return "MP4";
  if(n>=4&&(b[0]&0xff)==0x1A&&(b[1]&0xff)==0x45&&(b[2]&0xff)==0xDF&&(b[3]&0xff)==0xA3)return "MKV/WebM";
  if(n>=3&&b[0]=='F'&&b[1]=='L'&&b[2]=='V')return "FLV";
  if((b[0]&0xff)==0x47&&(n<189||(b[188]&0xff)==0x47))return "MPEG-TS";
  if(n>=3&&b[0]=='I'&&b[1]=='D'&&b[2]=='3')return "MP3/ID3";
  if(n>=4&&new String(b,0,4,StandardCharsets.ISO_8859_1).equals("RIFF"))return "AVI/RIFF";
  if(n>=8&&(b[0]&0xff)==0x89&&b[1]=='P'&&b[2]=='N'&&b[3]=='G'){for(int i=8;i+188<n;i++)if((b[i]&0xff)==0x47&&(b[i+188]&0xff)==0x47)return "PNG 伪装分片";return "图片";}
  String head=new String(b,0,Math.min(n,512),StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
  if(head.startsWith("<!doctype html")||head.startsWith("<html")||head.contains("<head")||head.contains("<script")||type.toLowerCase(Locale.ROOT).contains("text/html"))return "网页";
  if(head.startsWith("{")||head.startsWith("["))return "JSON";
  String t=type.toLowerCase(Locale.ROOT);
  if(t.startsWith("video/")||t.startsWith("audio/"))return t;
  return "未知";
 }
 static Response http(String url,Map<String,String> headers,int limit,int timeoutMs)throws IOException{
  String current=url;
  long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMs);
  for(int hop=0;hop<6;hop++){
   long remaining=TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime());if(remaining<=0||Thread.currentThread().isInterrupted())throw new SocketTimeoutException("检查超时");
   URL u=new URL(current);if(!(u.getProtocol().equals("http")||u.getProtocol().equals("https")))throw new IOException("跳转到不支持的协议");
   HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout((int)Math.min(remaining,10000));
   // Response headers use the remaining budget; only body reads use short slices.
   // Applying the 250ms body slice here rejected healthy servers before their headers arrived.
   c.setReadTimeout((int)Math.max(1,remaining));
   final HttpURLConnection conn=c;final Thread reader=Thread.currentThread();
   ScheduledFuture<?> kill=TIMER.schedule(()->{try{conn.disconnect();}catch(Exception ignored){}reader.interrupt();},remaining,TimeUnit.MILLISECONDS);
   if(!headers.containsKey("User-Agent"))c.setRequestProperty("User-Agent","Mozilla/5.0");
   for(Map.Entry<String,String> e:headers.entrySet())c.setRequestProperty(e.getKey(),e.getValue());
   c.setRequestProperty("Accept-Encoding","identity");
   try{
    int code;try{code=c.getResponseCode();}catch(SocketTimeoutException ste){if(System.nanoTime()>=deadline||Thread.currentThread().isInterrupted())throw new SocketTimeoutException("检查超时");throw ste;}
    c.setReadTimeout((int)Math.min(Math.max(1,TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime())),250));
    if(code>=300&&code<400&&c.getHeaderField("Location")!=null){current=new URL(u,c.getHeaderField("Location")).toString();continue;}
    InputStream in=code>=400?c.getErrorStream():c.getInputStream();ByteArrayOutputStream b=new ByteArrayOutputStream();
    // 非播放列表只需首 4KB 判断容器；m3u8 需要完整读到 limit 内
    if(in!=null)try(InputStream s=in){byte[] buf=new byte[8192];int r,cap=limit;while(b.size()<cap){long leftMs=TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime());if(leftMs<=0||Thread.currentThread().isInterrupted())throw new SocketTimeoutException("检查超时或已取消");try{r=s.read(buf,0,Math.min(buf.length,cap-b.size()));}catch(SocketTimeoutException ste){if(System.nanoTime()>=deadline||Thread.currentThread().isInterrupted())throw new SocketTimeoutException("检查超时或已取消");continue;}if(r==-1)break;b.write(buf,0,r);if(b.size()>=16&&cap>4096&&!HeaderRoute.Sniff.isPlaylist(b.toByteArray(),16))cap=4096;}}
    return new Response(code,c.getContentType(),c.getURL().toString(),b.toByteArray());
   }finally{kill.cancel(false);try{c.disconnect();}catch(Exception ignored){}Thread.interrupted();}
  }
  throw new IOException("跳转次数过多");
 }
 static String host(String url){try{return new URL(url).getHost();}catch(Exception e){return "服务器";}}
 static String reason(Throwable e){
  Throwable t=e;while(t.getCause()!=null&&t.getCause()!=t)t=t.getCause();
  if(t instanceof SocketTimeoutException)return "超时";if(t instanceof UnknownHostException)return "域名解析失败";if(t instanceof javax.net.ssl.SSLException)return "TLS/证书错误";if(t instanceof ConnectException)return "连接被拒绝";
  String m=t.getMessage();if(m==null)return t.getClass().getSimpleName();return m.replaceAll("https?://\\S+","[地址]");
 }
}
