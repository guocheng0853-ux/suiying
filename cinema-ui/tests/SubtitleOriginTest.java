package com.guocheng.mediapocket;
import com.sun.net.httpserver.*;import java.net.*;import java.util.*;import java.util.concurrent.atomic.*;
public class SubtitleOriginTest{
 public static void main(String[] args)throws Exception{
  HttpServer other=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0),source=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);AtomicBoolean leaked=new AtomicBoolean();
  other.createContext("/sub",e->{leaked.set(e.getRequestHeaders().containsKey("Cookie")||e.getRequestHeaders().containsKey("Authorization"));byte[] b="subtitle".getBytes();e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();});
  source.createContext("/hop",e->{e.getResponseHeaders().set("Location","http://127.0.0.1:"+other.getAddress().getPort()+"/sub");e.sendResponseHeaders(302,-1);e.close();});
  source.start();other.start();try{byte[] body=SubtitleHttp.download("http://127.0.0.1:"+source.getAddress().getPort()+"/hop",Map.of("Cookie","private=1","Authorization","Bearer private"));if(body.length==0||leaked.get())throw new AssertionError("Credentials crossed port boundary");}finally{source.stop(0);other.stop(0);}
  System.out.println("PASS: subtitle redirect between same-host different-port origins strips credentials.");
 }
}
