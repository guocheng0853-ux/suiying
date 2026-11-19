package com.guocheng.mediapocket;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public class TvBoxParserFallbackTest {
 static void reply(HttpExchange request,int status,String type,String body)throws IOException{
  byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
  request.getResponseHeaders().set("Content-Type",type);
  request.sendResponseHeaders(status,bytes.length);
  try(java.io.OutputStream output=request.getResponseBody()){output.write(bytes);}
 }

 static JSONObject parser(String name,String url)throws Exception{
  return new JSONObject().put("name",name).put("type",1).put("url",url);
 }

 public static void main(String[] args)throws Exception{
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  String base="http://127.0.0.1:"+server.getAddress().getPort();
  server.createContext("/parser-old",r->reply(r,200,"application/json","{\"url\":\""+base+"/expired.mp4\"}"));
  server.createContext("/expired.mp4",r->reply(r,403,"text/plain","expired"));
  server.createContext("/parser-new",r->reply(r,200,"application/json","{\"url\":\""+base+"/live.mp4\",\"header\":{\"Referer\":\"https://example.test/\"}}"));
  server.createContext("/live.mp4",r->{
   boolean valid="https://example.test/".equals(r.getRequestHeaders().getFirst("Referer"));
   reply(r,valid?200:403,"video/mp4","1234ftyptest");
  });
  server.start();
  try{
   JSONArray parsers=new JSONArray()
    .put(parser("old",base+"/parser-old?url="))
    .put(parser("new",base+"/parser-new?url="));
   FusionCatalog.Session session=new FusionCatalog.Session(base+"/config",new JSONObject().put("parses",parsers),new JSONObject());
   JSONObject player=new JSONObject().put("url",base+"/watch").put("jx",1);
   TvBoxPlay.Resolved resolved=TvBoxPlay.resolve(null,session,"line",player,10000);
   if(!resolved.check.media||!resolved.stream.getString("url").equals(base+"/live.mp4")){
    throw new AssertionError("Parser with an expired result prevented fallback to a playable URL: "+resolved.check.summary());
   }
   if(!"https://example.test/".equals(resolved.stream.getJSONObject("behaviorHints").getJSONObject("proxyHeaders").getJSONObject("request").getString("Referer"))){
    throw new AssertionError("Fallback parser request headers were lost");
   }
   parsers.remove(1);
   resolved=TvBoxPlay.resolve(null,session,"line",player,10000);
   if(!resolved.check.definitive||!"HTTP 403".equals(resolved.check.kind)){
    throw new AssertionError("The original rejection should remain visible when no fallback works");
   }
  }finally{server.stop(0);}
  System.out.println("PASS: TVBox parser falls back after an expired URL and preserves playback headers and the final failure.");
 }
}
