package com.guocheng.mediapocket;
import com.sun.net.httpserver.*;import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/**
 * 真实链路测试（JVM + ffmpeg，非 Android）：
 * 本机源站只有带齐 Cookie+Origin+UA 才返回数据，内容是 ffmpeg 生成的 主列表→子列表→AES-128 加密 TS 分片，以及一个 MP4。
 * 1) 只带 VLC 能发的 UA/Referer 直连 → 403（复现原播放器丢头问题）
 * 2) 经 HeaderRoute 代理 → ffmpeg 实际解码出视频帧；源站每个请求都带了 Cookie
 * 3) MP4 经代理拖动 → 源站收到 Range 并返回 206，ffmpeg 解码成功
 */
public class HeaderRouteTest {
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 static final AtomicInteger denied=new AtomicInteger(),served=new AtomicInteger(),ranged=new AtomicInteger();
 static final Set<String> seenPaths=ConcurrentHashMap.newKeySet();
 static String run(String... cmd)throws Exception{Process p=new ProcessBuilder(cmd).redirectErrorStream(true).start();String out=new String(p.getInputStream().readAllBytes());if(!p.waitFor(180,TimeUnit.SECONDS)){p.destroyForcibly();throw new AssertionError("timeout "+String.join(" ",cmd));}return p.exitValue()+"\n"+out;}
 static int frames(String out){int n=-1;java.util.regex.Matcher m=java.util.regex.Pattern.compile("frame=\\s*(\\d+)").matcher(out);while(m.find())n=Integer.parseInt(m.group(1));return n;}
 public static void main(String[] args)throws Exception{
  System.setProperty("sun.net.http.allowRestrictedHeaders","true");// JDK 默认丢弃 Origin；Android 的 HttpURLConnection 不限制
  Path dir=Files.createTempDirectory("hroute");Path v=dir.resolve("v");Files.createDirectories(v);
  // AES-128 key + keyinfo（key URI 为相对路径，用来验证 #EXT-X-KEY URI 改写）
  byte[] key=new byte[16];new Random(7).nextBytes(key);Files.write(v.resolve("k.bin"),key);
  Files.write(dir.resolve("keyinfo"),("k.bin\n"+v.resolve("k.bin")+"\n").getBytes());
  String r=run("ffmpeg","-y","-loglevel","error","-f","lavfi","-i","testsrc=size=320x180:rate=25","-f","lavfi","-i","sine=frequency=440","-t","6","-c:v","libx264","-g","25","-c:a","aac","-hls_time","2","-hls_list_size","0","-hls_key_info_file",dir.resolve("keyinfo").toString(),"-hls_segment_filename",v.resolve("seg%d.ts").toString(),v.resolve("index.m3u8").toString());
  check(r.startsWith("0"),"ffmpeg hls generation: "+r);
  Files.write(dir.resolve("master.m3u8"),"#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=400000,RESOLUTION=320x180\nv/index.m3u8\n".getBytes());
  r=run("ffmpeg","-y","-loglevel","error","-f","lavfi","-i","testsrc=size=320x180:rate=25","-t","8","-c:v","libx264","-movflags","+faststart",dir.resolve("movie.mp4").toString());check(r.startsWith("0"),"mp4 generation");
  Files.write(dir.resolve("page.html"),"<!DOCTYPE html><html><head><script>var player={}</script></head></html>".getBytes());
  Files.write(dir.resolve("api.json"),"{\"url\":\"https://x/y.m3u8\"}".getBytes());

  HttpServer origin=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  origin.createContext("/",ex->{
   Headers h=ex.getRequestHeaders();String path=ex.getRequestURI().getPath();
   boolean ok="sid=abc123".equals(h.getFirst("Cookie"))&&"https://site.example".equals(h.getFirst("Origin"))&&"TestUA/1".equals(h.getFirst("User-Agent"));
   if(!ok){denied.incrementAndGet();byte[] b="denied".getBytes();ex.sendResponseHeaders(403,b.length);ex.getResponseBody().write(b);ex.close();return;}
   Path f=dir.resolve(path.substring(1)).normalize();if(path.equals("/redirect.m3u8")){ex.getResponseHeaders().add("Location","/master.m3u8");ex.sendResponseHeaders(302,-1);ex.close();return;}
   if(!f.startsWith(dir)||!Files.isRegularFile(f)){ex.sendResponseHeaders(404,-1);ex.close();return;}
   served.incrementAndGet();seenPaths.add(path);byte[] all=Files.readAllBytes(f);
   String type=path.endsWith(".m3u8")?"application/octet-stream":path.endsWith(".html")?"text/html":path.endsWith(".json")?"application/json":path.endsWith(".mp4")?"video/mp4":"video/mp2t";
   ex.getResponseHeaders().add("Content-Type",type);ex.getResponseHeaders().add("Accept-Ranges","bytes");
   String range=h.getFirst("Range");
   if(range!=null&&range.startsWith("bytes=")){ranged.incrementAndGet();String[] p=range.substring(6).split("-",-1);long s=Long.parseLong(p[0]),e=p[1].isEmpty()?all.length-1:Math.min(all.length-1,Long.parseLong(p[1]));
    ex.getResponseHeaders().add("Content-Range","bytes "+s+"-"+e+"/"+all.length);ex.sendResponseHeaders(206,e-s+1);ex.getResponseBody().write(all,(int)s,(int)(e-s+1));ex.close();return;}
   ex.sendResponseHeaders(200,all.length);ex.getResponseBody().write(all);ex.close();
  });
  origin.setExecutor(Executors.newCachedThreadPool());origin.start();
  String base="http://127.0.0.1:"+origin.getAddress().getPort();
  Map<String,String> line=new LinkedHashMap<>();line.put("user-agent","TestUA/1");line.put("Referer","https://site.example/play");line.put("cookie","sid=abc123");line.put("Origin","https://site.example");

  // 1) VLC 实际能带的只有 UA/Referer：直连被拒
  r=run("ffmpeg","-loglevel","error","-user_agent","TestUA/1","-referer","https://site.example/play","-i",base+"/master.m3u8","-t","1","-f","null","-");
  check(!r.startsWith("0")&&denied.get()>0,"without Cookie/Origin the origin must refuse (reproduces dropped :http-header)");

  // headers()：给 VLC 的只剩 UA/Referer，且小写键被规范化
  Map<String,String> vlc=HeaderRoute.headers(line);
  check(vlc.size()==2&&"TestUA/1".equals(vlc.get("User-Agent"))&&vlc.containsKey("Referer"),"VLC header set canonicalized: "+vlc);
  // url()：只有 UA/Referer 时不走代理；非 HTTP 不改；注入头被丢弃
  check(HeaderRoute.url(base+"/master.m3u8",vlc).equals(base+"/master.m3u8"),"no proxy when VLC can carry all headers");
  check(HeaderRoute.url("/sdcard/a.mp4",line).equals("/sdcard/a.mp4"),"non-http untouched");
  Map<String,String> evil=new LinkedHashMap<>();evil.put("Cookie","a=b\r\nX-Evil: 1");evil.put("X:Bad","1");check(HeaderRoute.canonical(evil).isEmpty(),"CR/LF and colon header injection rejected");

  // 2) 代理后真实解码：主列表→子列表→加密分片
  String proxied=HeaderRoute.url(base+"/redirect.m3u8",line);
  check(proxied.startsWith("http://127.0.0.1:")&&!proxied.contains("abc123"),"proxied URL is local and does not leak cookie");
  check(HeaderRoute.url(proxied,line).equals(proxied),"idempotent on proxied URL (VLC recovery re-calls setSource)");
  int before=denied.get();
  r=run("ffmpeg","-loglevel","info","-user_agent","TestUA/1","-allowed_extensions","ALL","-i",proxied,"-f","null","-");
  int decoded=frames(r);
  check(r.startsWith("0")&&decoded>=140,"HLS via proxy decodes (frames="+decoded+")\n"+r.substring(Math.max(0,r.length()-600)));
  check(denied.get()==before,"every origin request through proxy carried Cookie/Origin/UA");
  check(seenPaths.contains("/v/k.bin")&&seenPaths.contains("/v/seg0.ts")&&seenPaths.contains("/v/index.m3u8"),"key, variant and segments all fetched via rewritten URIs: "+seenPaths);

  // 3) MP4 拖动：Range 透传
  int rangedBefore=ranged.get();
  String mp4=HeaderRoute.url(base+"/movie.mp4",line);
  r=run("ffmpeg","-loglevel","info","-ss","5","-i",mp4,"-f","null","-");
  check(r.startsWith("0")&&frames(r)>0&&ranged.get()>rangedBefore,"MP4 seek via proxy uses Range/206 (frames="+frames(r)+")");

  // 4) LinkProbe 用同一套头检查真实链路
  LinkProbe.Result p=LinkProbe.probe(base+"/redirect.m3u8",line,15000);
  check(p.media&&p.kind.contains("首分片")&&p.detail.contains("加密"),"probe follows master→variant→first segment: "+p.summary());
  p=LinkProbe.probe(base+"/master.m3u8",vlc,15000);check(!p.media&&p.definitive&&p.kind.contains("403"),"probe reports 403 when required headers missing: "+p.summary());
  p=LinkProbe.probe(base+"/page.html",line,15000);check(p.definitive&&p.kind.equals("返回网页"),"web page is not reported as video: "+p.summary());
  p=LinkProbe.probe(base+"/api.json",line,15000);check(p.definitive&&p.kind.contains("JSON"),"JSON is not reported as video: "+p.summary());
  p=LinkProbe.probe(base+"/movie.mp4",line,15000);check(p.media&&p.kind.equals("MP4"),"mp4 container recognised: "+p.summary());
  p=LinkProbe.probe(base+"/missing.mp4",line,15000);check(p.definitive&&p.kind.equals("HTTP 404"),"404 definitive: "+p.summary());
  p=LinkProbe.probe("http://127.0.0.1:1/x.m3u8",line,4000);check(!p.media&&!p.definitive,"network failure is not called a definitive non-video: "+p.summary());
  origin.stop(0);
  System.out.println("PASS: VLC-only headers refused (403); via HeaderRoute HLS master→variant→AES segments decoded "+decoded+" frames, key/segment URIs rewritten, Cookie/Origin on every origin request; MP4 seek via Range/206; probe distinguishes media / 403 / web page / JSON / 404 / network failure.");System.exit(0);
 }
}
