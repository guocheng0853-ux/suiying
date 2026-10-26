package com.guocheng.mediapocket;
import com.sun.net.httpserver.*;import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/**
 * 0.1.147 继续验收 C2 / C4：取消的请求不能占住线程，让用户紧接着发起的新检查 / 新重连排队。
 * 真实本机 HTTP：响应头前挂起（C4 的 delayed 模式）和发出首字节后正文停顿（C2 的 health_body_delay）。
 */
public class EmbyCancelTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static long ms(long since){return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-since);}
 public static void main(String[] args)throws Exception{
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);ExecutorService threads=Executors.newCachedThreadPool(r->{Thread t=new Thread(r);t.setDaemon(true);return t;});server.setExecutor(threads);
  AtomicInteger hangs=new AtomicInteger(),bodies=new AtomicInteger(),hits=new AtomicInteger();CountDownLatch hang=new CountDownLatch(2),body=new CountDownLatch(2);
  server.createContext("/hang",e->{hangs.incrementAndGet();hang.countDown();try{Thread.sleep(20000);e.sendResponseHeaders(200,-1);}catch(Exception ignored){}finally{e.close();}});
  server.createContext("/stall",e->{try{e.sendResponseHeaders(200,100);e.getResponseBody().write('{');e.getResponseBody().flush();bodies.incrementAndGet();body.countDown();Thread.sleep(3000);e.getResponseBody().write(new byte[99]);}catch(Exception ignored){}finally{e.close();}});
  server.createContext("/json",e->{hits.incrementAndGet();byte[] b="{\"Version\":\"fixture\"}".getBytes();e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();});
  server.start();String base="http://127.0.0.1:"+server.getAddress().getPort();
  try{
   // C4：开播请求在等响应头时被取消，工作线程马上退出，而不是等到 15 秒读取超时
   ExecutorService open=EmbyCall.pool("test-open");EmbyCall call=new EmbyCall();
   Future<?> old=open.submit(()->{EmbyCall.CURRENT.set(call);try{return EmbyClient.exchange("GET",base+"/hang",null,null,null,false);}finally{EmbyCall.CURRENT.remove();}});
   check(hang.await(5,TimeUnit.SECONDS)||hangs.get()>0,"fixture saw the hanging PlaybackInfo request");
   long cancelAt=System.nanoTime();call.cancel();Throwable why=null;try{old.get(3,TimeUnit.SECONDS);}catch(ExecutionException e){why=e.getCause();}long exit=ms(cancelAt);
   check(why!=null&&EmbyCall.isCancel(why),"cancelled request ends as a cancel, not a retryable timeout: "+why);check(exit<1500,"cancel releases the worker while waiting for headers, elapsed="+exit);
   check(!EmbyCue.retryable(why),"a cancel never falls through to the next server address");
   // C4：没被取消、仍挂着的旧请求也不挡住新的重连
   Future<?> stuck=open.submit(()->EmbyClient.exchange("GET",base+"/hang",null,null,null,false));Thread.sleep(200);
   long fresh=System.nanoTime();EmbyClient.HttpResult r=open.submit(()->EmbyClient.exchange("GET",base+"/json",null,null,null,false)).get(3,TimeUnit.SECONDS);long wait=ms(fresh);
   check(r.code==200&&wait<1000,"new reconnect starts at once beside an old request, elapsed="+wait);stuck.cancel(true);
   // 先取消再发：不会再打到服务器
   int before=hits.get();EmbyCall gone=new EmbyCall();gone.cancel();boolean refused=false;EmbyCall.CURRENT.set(gone);try{EmbyClient.exchange("GET",base+"/json",null,null,null,false);}catch(InterruptedIOException e){refused=EmbyCall.isCancel(e);}finally{EmbyCall.CURRENT.remove();}
   check(refused&&hits.get()==before,"cancelled call opens no new connection");
   check(!EmbyCall.abortableWhileReading((HttpURLConnection)new URL(base+"/json").openConnection()),"desktop JDK keeps the no-disconnect-while-reading rule");
   // C2：两次体检都停在正文时取消，第三次体检立刻开始（原来 2 线程池要排到旧读取结束）
   Map<String,String> none=Collections.emptyMap();EmbyHealthHttp h1=new EmbyHealthHttp(),h2=new EmbyHealthHttp();
   CountDownLatch exited=new CountDownLatch(2);
   Future<?> f1=EmbyHealthHttp.WORKERS.submit(()->{try{return h1.request("GET",base+"/stall",none,null,10000);}finally{exited.countDown();}}),f2=EmbyHealthHttp.WORKERS.submit(()->{try{return h2.request("GET",base+"/stall",none,null,10000);}finally{exited.countDown();}});
   check(body.await(5,TimeUnit.SECONDS),"both checks reached the body");Thread.sleep(200);
   h1.cancel();f1.cancel(true);h2.cancel();f2.cancel(true);
   long third=System.nanoTime();EmbyHealthCheck.Response quick=EmbyHealthHttp.WORKERS.submit(()->new EmbyHealthHttp().request("GET",base+"/json",none,null,3000)).get(3,TimeUnit.SECONDS);long queued=ms(third);
   check(quick.code==200&&queued<1000,"a new check does not queue behind cancelled body reads, elapsed="+queued);
   // 取消的旧检查自己在服务器补完正文后退出，不永久占线程
   check(exited.await(8,TimeUnit.SECONDS),"cancelled checks still leave on their own, within the body budget");
   java.nio.file.Path src=java.nio.file.Paths.get("src/com/guocheng/mediapocket");
   check(java.nio.file.Files.readString(src.resolve("EmbyDiagnostics.java")).contains("static final ExecutorService IO=EmbyHealthHttp.WORKERS;"),"the health screen runs on the elastic workers");
   String playback=java.nio.file.Files.readString(src.resolve("EmbyPlayback.java"));
   int run=playback.indexOf("OPEN.execute(new Runnable(){public void run(){try{");int release=playback.indexOf("EmbySessions.release(a);",run),enter=playback.indexOf("EmbyCall.CURRENT.set(call);",run);
   check(run>0&&release>run&&enter>release,"playback requests run on OPEN, and cleaning the previous session is not aborted by this cancel");
   check(playback.contains("wait.setOnCancelListener(d->{cancel[0]=true;call.cancel();handoffDone(a);});"),"closing the wait dialog aborts the request");
   check(playback.contains("if(call.cancelled()||cancel[0]||EmbyCall.isCancel(e))return;"),"a cancelled request shows no late error and cannot reach the recovery fallback");
   check(playback.contains("finally{EmbyCall.CURRENT.remove();}"),"pool threads do not keep a stale call");
   check(java.nio.file.Files.readString(src.resolve("EmbyAutoRecovery.java")).contains("t.cancelled=true;t.pending=false;t.call.cancel();"),"取消重连 aborts the in-flight PlaybackInfo");
   System.out.println("PASS: cancelled PlaybackInfo released in "+exit+"ms, new reconnect waited "+wait+"ms, new health check waited "+queued+"ms beside two cancelled body reads.");
  }finally{server.stop(0);threads.shutdownNow();}
 }
}
