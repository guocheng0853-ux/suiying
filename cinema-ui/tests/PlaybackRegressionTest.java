package com.guocheng.mediapocket;
import com.sun.net.httpserver.*;import java.net.*;import java.io.*;import java.util.*;import java.util.concurrent.*;import org.json.*;
/** Negative fixtures for regressions found during the 0.1.48 review. No public media or upstream API required. */
public class PlaybackRegressionTest {
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 public static void main(String[] args)throws Exception{
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(Executors.newCachedThreadPool(r->{Thread t=new Thread(r);t.setDaemon(true);return t;}));
  server.createContext("/wrong",e->{byte[] b=new byte[32];e.getResponseHeaders().add("Content-Range","bytes 0-31/1024");e.sendResponseHeaders(206,b.length);e.getResponseBody().write(b);e.close();});
  server.createContext("/chunked",e->{byte[] b=new byte[32];e.getResponseHeaders().add("Content-Range","bytes 256-287/1024");e.sendResponseHeaders(206,0);e.getResponseBody().write(b);e.close();});
  server.createContext("/sub",e->{boolean ok="sid=1".equals(e.getRequestHeaders().getFirst("Cookie"))&&"Bearer fixture".equals(e.getRequestHeaders().getFirst("Authorization"));byte[] b="1\n00:00:01,000 --> 00:00:02,000\nhello\n".getBytes();e.sendResponseHeaders(ok?200:403,b.length);e.getResponseBody().write(b);e.close();});
  server.createContext("/media",e->{byte[] b={0,0,0,16,'f','t','y','p',0,0,0,0,0,0,0,0};e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();});
  server.createContext("/slow",e->{e.sendResponseHeaders(200,128);try{for(int i=0;i<128;i++){e.getResponseBody().write(0);e.getResponseBody().flush();Thread.sleep(40);}}catch(Exception ignored){}finally{e.close();}});server.start();String base="http://127.0.0.1:"+server.getAddress().getPort();
  try{
   boolean rejected=false;try{RangeAccelerator.fetch(base+"/wrong",new HashMap<>(),256,287);}catch(IOException e){rejected=true;}check(rejected,"wrong Content-Range must fail, never splice unrelated bytes");
   check(RangeAccelerator.fetch(base+"/chunked",new HashMap<>(),256,287).length==32,"correct chunked 206 accepted");
   check(SubtitleHttp.download(base+"/sub",Map.of("Cookie","sid=1","Authorization","Bearer fixture")).length>0,"subtitle auth headers retained");
   FusionCatalog.Session session=new FusionCatalog.Session(base+"/config.json",new JSONObject(),new JSONObject().put("name","fixture"));
   JSONObject subtitle=new JSONObject().put("url","sub").put("lang","zho").put("header",new JSONObject().put("Cookie","sid=1"));
   JSONObject response=new JSONObject().put("url",base+"/media").put("subt",new JSONArray().put(subtitle).toString()).put("subtitles",new JSONArray().put(subtitle));
   TvBoxPlay.Resolved resolved=TvBoxPlay.resolve(null,session,"fixture",response,4000);JSONArray subs=resolved.stream.getJSONArray("subtitles");
   check(subs.length()==1&&subs.getJSONObject(0).getString("url").equals(base+"/sub"),"player pipeline preserves, resolves and deduplicates subtitles");
   check(subs.getJSONObject(0).getJSONObject("header").getString("Cookie").equals("sid=1"),"subtitle-specific headers retained");
   long t=System.nanoTime();LinkProbe.Result result=LinkProbe.probe(base+"/slow",Map.of(),1000);long elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-t);
   check(elapsed<2000&&!result.media,"slow drip must respect total probe budget, elapsed="+elapsed);Thread.sleep(150);check(!Thread.currentThread().isInterrupted(),"probe deadline never leaves the caller thread interrupted");
   System.out.println("PASS: wrong range rejected; chunked range retained; subtitle auth and player metadata preserved; slow-drip probe bounded ("+elapsed+"ms).");
  }finally{server.stop(0);}
 }
}
