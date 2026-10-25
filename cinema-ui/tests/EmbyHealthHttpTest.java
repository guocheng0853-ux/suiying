package com.guocheng.mediapocket;
import com.sun.net.httpserver.*;import java.net.*;import java.io.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public class EmbyHealthHttpTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  HttpServer first=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0),other=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);ExecutorService threads=Executors.newCachedThreadPool(r->{Thread t=new Thread(r);t.setDaemon(true);return t;});first.setExecutor(threads);other.setExecutor(threads);AtomicInteger leaks=new AtomicInteger();
  other.createContext("/",e->{leaks.incrementAndGet();e.sendResponseHeaders(200,0);e.close();});other.start();
  first.createContext("/json",e->{byte[] b="{\"Version\":\"fixture\"}".getBytes();e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();});
  first.createContext("/same",e->{e.getResponseHeaders().add("Location","/json");e.sendResponseHeaders(302,-1);e.close();});
  first.createContext("/other",e->{e.getResponseHeaders().add("Location","http://127.0.0.1:"+other.getAddress().getPort()+"/");e.sendResponseHeaders(302,-1);e.close();});
  first.createContext("/drip",e->{e.sendResponseHeaders(200,500);try{for(int i=0;i<500;i++){e.getResponseBody().write('x');e.getResponseBody().flush();Thread.sleep(40);}}catch(Exception ignored){}finally{e.close();}});first.start();String base="http://127.0.0.1:"+first.getAddress().getPort();
  first.createContext("/headers",e->{try{Thread.sleep(650);byte[] b=new byte[4096];b[4]='f';b[5]='t';b[6]='y';b[7]='p';e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);}catch(Exception ignored){}finally{e.close();}});
  first.createContext("/headers-too-slow",e->{try{Thread.sleep(2500);e.sendResponseHeaders(200,0);}catch(Exception ignored){}finally{e.close();}});
  try{
   Map<String,String> auth=Collections.singletonMap("X-Emby-Token","fixture-secret");EmbyHealthHttp http=new EmbyHealthHttp();check(http.request("GET",base+"/same",auth,null,2000).code==200,"same-origin redirect allowed");
   boolean blocked=false;try{http.request("GET",base+"/other",auth,null,2000);}catch(IOException e){blocked=true;}check(blocked&&leaks.get()==0,"token cannot cross host/port redirect");
   long start=System.nanoTime();boolean expired=false;try{http.request("GET",base+"/drip",auth,null,1000);}catch(Exception e){expired=true;}long ms=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);check(expired&&ms<2200,"slow-drip body is bounded, elapsed="+ms);Thread.sleep(150);check(!Thread.currentThread().isInterrupted(),"deadline never leaves the caller thread interrupted");
   EmbyHealthHttp cancelled=new EmbyHealthHttp();cancelled.cancel();boolean stopped=false;try{cancelled.request("GET",base+"/json",auth,null,2000);}catch(InterruptedIOException e){stopped=true;}check(stopped,"cancel before request opens no connection");
   check(!EmbyHealthHttp.sameOrigin(new URL("https://example.com"),new URL("http://example.com")),"TLS downgrade is not the same origin");
   long headerStart=System.nanoTime();LinkProbe.Result delayed=LinkProbe.probe(base+"/headers",Collections.emptyMap(),2000);check(delayed.media,"video response headers may take more than 250ms within the total budget: "+delayed.summary());check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-headerStart)>=500,"fixture exercised delayed headers");
   long slowStart=System.nanoTime();LinkProbe.Result slowHeaders=LinkProbe.probe(base+"/headers-too-slow",Collections.emptyMap(),1000);long slowMs=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-slowStart);check(!slowHeaders.media&&slowMs>=800&&slowMs<2200,"header wait still respects the total deadline, elapsed="+slowMs);
   System.out.println("PASS: real local HTTP, same-origin redirects, credential isolation, cancellation and hard slow-drip timeout ("+ms+"ms).");
  }finally{first.stop(0);other.stop(0);threads.shutdownNow();}
 }
}
