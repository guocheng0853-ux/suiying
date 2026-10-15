package com.guocheng.mediapocket;
import okhttp3.*;import java.net.*;import java.io.*;import java.util.*;import java.util.concurrent.*;
/** Host-scoped transport fallback. Every HTTPS profile retains platform certificate and hostname validation. */
public final class ClawCompatibleNetwork {
 static final String[] LABELS={"标准 HTTP/1.1","IPv4 HTTP/1.1","IPv4 + TLS 1.2"};
 static final Dns IPV4=host->ipv4(Dns.SYSTEM.lookup(host),host);
 static final OkHttpClient BASE=new OkHttpClient.Builder().protocols(Collections.singletonList(Protocol.HTTP_1_1)).connectTimeout(6,TimeUnit.SECONDS).readTimeout(8,TimeUnit.SECONDS).retryOnConnectionFailure(true).build();
 static final ConnectionSpec TLS12=new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions(TlsVersion.TLS_1_2).build();
 static final OkHttpClient[] CLIENTS={BASE,BASE.newBuilder().dns(IPV4).connectionPool(new ConnectionPool()).build(),BASE.newBuilder().dns(IPV4).connectionSpecs(Arrays.asList(TLS12,ConnectionSpec.CLEARTEXT)).connectionPool(new ConnectionPool()).build()};
 static volatile String lastReport="尚未运行兼容连接检查";static volatile int preferred=0;
 static List<InetAddress> ipv4(List<InetAddress> addresses,String host)throws UnknownHostException{List<InetAddress> out=new ArrayList<>();for(InetAddress a:addresses)if(a instanceof Inet4Address)out.add(a);if(out.isEmpty())throw new UnknownHostException("该主机没有 IPv4 地址");return out;}
 public static boolean handles(String address){try{URI uri=URI.create(address);String host=uri.getHost(),scheme=uri.getScheme();return ("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme))&&host!=null&&(host.equalsIgnoreCase("torrentclaw.com")||host.toLowerCase(Locale.ROOT).endsWith(".torrentclaw.com"));}catch(Exception e){return false;}}
 public static byte[] bytes(String address)throws IOException{return fetch(address,false);}
 static byte[] fetch(String address,boolean testLocal)throws IOException{
  if(!testLocal&&!handles(address))throw new IOException("不是 TorrentClaw API 地址");long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);IOException last=null;StringBuilder report=new StringBuilder();int[] order=preferred==0?new int[]{0,1,2}:preferred==1?new int[]{1,0,2}:new int[]{2,0,1};
  for(int index:order){long remaining=TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime());if(remaining<=0)break;
   Request request=new Request.Builder().url(address).header("User-Agent","Mozilla/5.0 MediaPocket/0.1.25").header("Accept","application/json").header("Connection","close").get().build();Call call=CLIENTS[index].newCall(request);call.timeout().timeout(Math.min(10000,remaining),TimeUnit.MILLISECONDS);
   try(Response response=call.execute()){
    report.append(LABELS[index]).append("：HTTP ").append(response.code()).append('\n');
    if(response.code()!=200)throw new HttpStatus(response.code());ResponseBody body=response.body();if(body==null)throw new IOException("接口响应为空");if(body.contentLength()>16*1024*1024)throw new HttpStatus(-1);
    try(InputStream in=body.byteStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>16*1024*1024)throw new HttpStatus(-1);out.write(b,0,n);}preferred=index;lastReport=report.toString();return out.toByteArray();}
   }catch(HttpStatus e){lastReport=report.toString();throw e;}catch(IOException e){last=e;report.append(LABELS[index]).append("：").append(reason(e)).append('\n');if(Thread.currentThread().isInterrupted()){lastReport=report.toString();throw e;}}
  }
  lastReport=report.toString();throw new IOException("TorrentClaw 备用连接失败",last);
 }
 static String reason(Throwable error){StringBuilder out=new StringBuilder();Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());for(Throwable e=error;e!=null&&seen.add(e)&&seen.size()<=4;e=e.getCause()){if(out.length()>0)out.append(" → ");out.append(e.getClass().getSimpleName());String m=e.getMessage();if(m!=null&&!m.isEmpty()){m=m.replaceAll("(?i)(https?|stremio)://[^\\s]+","[地址已隐藏]").replaceAll("[\\r\\n]"," ");if(m.length()>120)m=m.substring(0,120)+"…";out.append(": ").append(m);}}return out.toString();}
 static final class HttpStatus extends IOException{HttpStatus(int status){super(status<0?"响应超过大小限制":"HTTP "+status);}}
}
