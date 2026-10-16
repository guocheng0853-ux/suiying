package com.guocheng.mediapocket;
import org.json.*;import com.sun.net.httpserver.*;import java.net.*;import java.util.*;import java.util.concurrent.*;
public class DetailMetadataTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  check(!DetailMetadata.useful(new JSONObject().put("name","IMDb 11540908").put("description","Metadata is still warming up. Streams may still be available.")),"warm placeholder rejected");
  check(!DetailMetadata.useful(new JSONObject().put("name","tt11540908")),"identifier rejected");
  check(!DetailMetadata.useful(new JSONObject().put("name","影片资料加载中")),"local loading state never cached");
  JSONObject cached=new JSONObject().put("name","旧片名").put("poster","cached.jpg").put("videos",new JSONArray().put(new JSONObject().put("id","tt123456:1:1")));
  JSONObject fresh=new JSONObject().put("name","新片名").put("poster","").put("videos",new JSONArray()).put("description","更新简介");
  JSONObject merged=DetailMetadata.merge(cached,fresh);
  check(merged.optString("name").equals("新片名")&&merged.optString("poster").equals("cached.jpg"),"fresh name and cached art");
  check(merged.optJSONArray("videos").length()==1,"partial response preserves episodes");
  check(cached.optString("name").equals("旧片名"),"cached object unchanged");
  check(DetailMetadata.useful(merged),"valid response accepted");
  check(DetailMetadata.info(new JSONObject().put("released","2024-08-26T00:00:00.000Z").put("runtime","74 min")).equals("2024/08/26  74 min"),"full release date");
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);ExecutorService workers=Executors.newCachedThreadPool();server.setExecutor(workers);
  server.createContext("/slow",ex->{try{Thread.sleep(1500);ex.sendResponseHeaders(503,-1);}catch(Exception ignored){}finally{ex.close();}});
  server.createContext("/warm",ex->{byte[] b="{\"meta\":{\"name\":\"IMDb 11540908\",\"description\":\"Metadata is still warming up\"}}".getBytes("UTF-8");ex.sendResponseHeaders(200,b.length);ex.getResponseBody().write(b);ex.close();});
  server.createContext("/ok",ex->{byte[] b="{\"meta\":{\"name\":\"Real title\",\"poster\":\"art.jpg\"}}".getBytes("UTF-8");ex.sendResponseHeaders(200,b.length);ex.getResponseBody().write(b);ex.close();});server.start();String base="http://127.0.0.1:"+server.getAddress().getPort();
  try{long start=System.nanoTime();JSONObject result=DetailMetadata.lookup(Arrays.asList(base+"/slow",base+"/warm",base+"/ok"),2500);check(result.optString("name").equals("Real title"),"ignore warm placeholder and use actual result");check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<1400,"slow provider does not block fast result");try{DetailMetadata.lookup(Arrays.asList(base+"/warm"),1000);throw new AssertionError("placeholder must fail");}catch(Exception expected){}long began=System.nanoTime();try{DetailMetadata.lookup(Arrays.asList(base+"/slow"),150);throw new AssertionError("timeout must fail");}catch(Exception expected){}check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-began)<800,"total UI wait bounded");}finally{server.stop(0);workers.shutdownNow();}
  System.out.println("PASS: metadata placeholder rejection, partial response merge preserves artwork/episodes, immutable cache, full release date.");
 }
}
