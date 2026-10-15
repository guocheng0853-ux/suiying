package com.guocheng.mediapocket;
import com.sun.net.httpserver.*;import java.io.*;import java.net.*;import java.nio.file.*;import java.security.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/**
 * 多线程加速实测（JVM，非 Android）：源站每个连接限速 2MB/s（模拟网盘单线程限速），必须带 Cookie。
 * 比较单连接直下与经 HeaderRoute 4 连接分段的耗时与字节一致性；中间 Range、无 Range 支持的服务器、ffmpeg 解码。
 */
public class RangeAcceleratorTest {
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 static final int RATE=2*1024*1024;static final AtomicInteger maxParallel=new AtomicInteger(),active=new AtomicInteger(),rangeRequests=new AtomicInteger();
 static void throttled(OutputStream o,byte[] b,int off,int len)throws IOException{long t0=System.nanoTime();int sent=0;while(sent<len){int n=Math.min(65536,len-sent);o.write(b,off+sent,n);sent+=n;long due=t0+(long)sent*1_000_000_000L/RATE;long wait=due-System.nanoTime();if(wait>0)try{Thread.sleep(wait/1_000_000,(int)(wait%1_000_000));}catch(InterruptedException e){return;}}}
 static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 static byte[] get(String url,Map<String,String> h)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();for(Map.Entry<String,String> e:h.entrySet())c.setRequestProperty(e.getKey(),e.getValue());try(InputStream in=c.getInputStream()){return in.readAllBytes();}}
 public static void main(String[] args)throws Exception{try{run();}catch(Throwable e){e.printStackTrace();System.exit(1);}}
 static void run()throws Exception{
  HeaderRoute.accelerate=true;
  Path dir=Files.createTempDirectory("accel");
  byte[] file=new byte[16*1024*1024];new Random(3).nextBytes(file);
  Process p=new ProcessBuilder("ffmpeg","-y","-loglevel","error","-f","lavfi","-i","testsrc=size=1280x720:rate=25,noise=alls=40:allf=t","-t","16","-c:v","libx264","-b:v","6M","-maxrate","6M","-bufsize","6M","-movflags","+faststart",dir.resolve("big.mp4").toString()).inheritIO().start();check(p.waitFor()==0,"mp4 gen");
  byte[] mp4=Files.readAllBytes(dir.resolve("big.mp4"));check(mp4.length>RangeAccelerator.MIN_BYTES,"test mp4 large enough: "+mp4.length);
  HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  HttpHandler serve=ex->{
   String path=ex.getRequestURI().getPath();byte[] all=path.endsWith(".mp4")?mp4:file;boolean ranges=!path.startsWith("/norange");
   if(!"sid=1".equals(ex.getRequestHeaders().getFirst("Cookie"))){ex.sendResponseHeaders(403,-1);ex.close();return;}
   int now=active.incrementAndGet();maxParallel.accumulateAndGet(now,Math::max);
   try{
    String range=ranges?ex.getRequestHeaders().getFirst("Range"):null;
    if(ranges)ex.getResponseHeaders().add("Accept-Ranges","bytes");ex.getResponseHeaders().add("Content-Type",path.endsWith(".mp4")?"video/mp4":"video/x-matroska");
    if(range!=null){rangeRequests.incrementAndGet();String[] r=range.substring(6).split("-",-1);int a=Integer.parseInt(r[0]),b=r[1].isEmpty()?all.length-1:Math.min(all.length-1,Integer.parseInt(r[1]));ex.getResponseHeaders().add("Content-Range","bytes "+a+"-"+b+"/"+all.length);ex.sendResponseHeaders(206,b-a+1);throttled(ex.getResponseBody(),all,a,b-a+1);}
    else{ex.sendResponseHeaders(200,all.length);throttled(ex.getResponseBody(),all,0,all.length);}
   }catch(IOException closed){}finally{active.decrementAndGet();ex.close();}
  };
  s.createContext("/",serve);s.setExecutor(Executors.newCachedThreadPool(r->{Thread t=new Thread(r);t.setDaemon(true);return t;}));s.start();
  String base="http://127.0.0.1:"+s.getAddress().getPort();Map<String,String> line=Map.of("Cookie","sid=1","User-Agent","T/1");

  long t=System.nanoTime();byte[] direct=get(base+"/movie.mkv",line);double single=(System.nanoTime()-t)/1e9;
  check(Arrays.equals(direct,file),"direct bytes");
  maxParallel.set(0);String proxied=HeaderRoute.url(base+"/movie.mkv",line);
  t=System.nanoTime();byte[] fast=get(proxied,Map.of("User-Agent","VLC"));double accel=(System.nanoTime()-t)/1e9;
  check(sha(fast).equals(sha(file)),"accelerated bytes identical (sha256)");
  check(maxParallel.get()>=3,"multiple upstream connections used: "+maxParallel.get());
  check(accel<single/2.5,String.format("speedup >= 2.5x (single %.1fs, accelerated %.1fs)",single,accel));

  // 播放器拖动：从中间开始
  byte[] tail=get(proxied,Map.of("Range","bytes=5000001-"));
  check(Arrays.equals(tail,Arrays.copyOfRange(file,5000001,file.length)),"mid-file Range through accelerator correct");
  byte[] small=get(proxied,Map.of("Range","bytes=100-1099"));check(Arrays.equals(small,Arrays.copyOfRange(file,100,1100)),"small range passthrough");

  // 服务器不支持 Range：原样单连接转发
  int before=rangeRequests.get();byte[] nr=get(HeaderRoute.url(base+"/norange/movie.mkv",line),Map.of());
  check(Arrays.equals(nr,file)&&rangeRequests.get()==before,"no-range server passthrough intact");

  // 关闭加速 + 无额外请求头：不经代理
  HeaderRoute.accelerate=false;check(HeaderRoute.url(base+"/x.mkv",Map.of("User-Agent","a")).equals(base+"/x.mkv"),"accelerate off leaves plain links direct");
  HeaderRoute.accelerate=true;check(HeaderRoute.url("http://127.0.0.1:9978/proxy?x=1",Map.of("User-Agent","a")).equals("http://127.0.0.1:9978/proxy?x=1"),"loopback spider proxy not wrapped for acceleration");
  HeaderRoute.accelerate=true;

  // ffmpeg 经加速代理解码约 12MB 的 MP4
  String m=HeaderRoute.url(base+"/big.mp4",line);
  Process f=new ProcessBuilder("ffmpeg","-loglevel","info","-i",m,"-f","null","-").redirectErrorStream(true).start();String out=new String(f.getInputStream().readAllBytes());check(f.waitFor()==0,"ffmpeg decode via accelerator\n"+out.substring(Math.max(0,out.length()-500)));
  java.util.regex.Matcher fm=java.util.regex.Pattern.compile("frame=\\s*(\\d+)").matcher(out);int frames=0;while(fm.find())frames=Integer.parseInt(fm.group(1));check(frames>=399,"all 400 frames decoded: "+frames);
  System.out.println(String.format("PASS: 2MB/s-per-connection origin: single connection %.1fs vs HeaderRoute 4-connection %.1fs (%.1fx), sha256 identical, up to %d parallel upstream connections; mid-file Range and small ranges correct; no-Range server passthrough; accelerate off/loopback untouched; ffmpeg decoded %d frames of a %.1fMB MP4 via accelerator.",single,accel,single/accel,maxParallel.get(),frames,mp4.length/1048576.0));
  System.exit(0);
 }
}
