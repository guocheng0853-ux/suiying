package com.guocheng.mediapocket;
import com.sun.net.httpserver.*;import org.json.*;import java.net.*;import java.util.concurrent.*;
public class StremioStreamsTest {
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 public static void main(String[] args)throws Exception{try{run();}catch(Throwable e){e.printStackTrace();System.exit(1);}}
 static void run()throws Exception{
  HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  s.createContext("/ok/stream/movie/tt1.json",ex->{check("MediaPocket/0.1".equals(ex.getRequestHeaders().getFirst("User-Agent")),"original UA kept");byte[] b="{\"streams\":[{\"name\":\"A\",\"url\":\"https://x/a.mp4\"},{\"name\":\"B\",\"infoHash\":\"abc\"},{\"name\":\"C\",\"ytId\":\"q\"}]}".getBytes();ex.sendResponseHeaders(200,b.length);ex.getResponseBody().write(b);ex.close();});
  s.createContext("/slow/stream/movie/tt1.json",ex->{try{Thread.sleep(6000);}catch(Exception e){}ex.close();});
  s.createContext("/err/stream/movie/tt1.json",ex->{ex.sendResponseHeaders(503,-1);ex.close();});
  s.setExecutor(Executors.newCachedThreadPool());s.start();String b="http://127.0.0.1:"+s.getAddress().getPort();
  JSONArray a=StremioStreams.fetch(b+"/ok/stream/movie/tt1.json",5000);check(a.length()==3,"three streams");
  check(StremioStreams.playable(a.getJSONObject(0))&&StremioStreams.kind(a.getJSONObject(0)).startsWith("直链"),"direct");
  check(StremioStreams.playable(a.getJSONObject(1))&&StremioStreams.kind(a.getJSONObject(1)).startsWith("P2P"),"p2p");
  check(!StremioStreams.playable(a.getJSONObject(2))&&StremioStreams.kind(a.getJSONObject(2)).contains("不支持"),"youtube not playable");
  long t=System.currentTimeMillis();try{StremioStreams.fetch(b+"/slow/stream/movie/tt1.json",1500);check(false,"slow must fail");}catch(Exception e){check(System.currentTimeMillis()-t<4000,"budget enforced ("+(System.currentTimeMillis()-t)+"ms)");}
  try{StremioStreams.fetch(b+"/err/stream/movie/tt1.json",3000);check(false,"503 must fail");}catch(Exception e){check(e.getMessage().contains("503"),"HTTP code reported");}
  check(StremioStreamDialogEpisode(),"episode label");
  s.stop(0);System.out.println("PASS: Stremio stream fetch keeps original UA, per-addon budget disconnects slow addon, HTTP errors reported, direct/P2P/unsupported kinds.");System.exit(0);
 }
 static boolean StremioStreamDialogEpisode(){return StremioStreams.episodeLabel("tt0944947:2:5").equals("第 2 季 · 第 5 集")&&StremioStreams.episodeLabel("tt1375666").isEmpty();}
}
