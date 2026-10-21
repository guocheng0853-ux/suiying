package com.guocheng.mediapocket;
import com.sun.net.httpserver.*;import java.net.*;import java.io.*;import org.json.*;
public class OfflineHttpTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a)throws Exception{
  byte[] body=new byte[100000];for(int i=0;i<body.length;i++)body[i]=(byte)i;
  HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);final String[] seen=new String[3];
  s.createContext("/r",x->{x.getResponseHeaders().add("Location","/f");x.sendResponseHeaders(302,-1);x.close();});
  s.createContext("/f",x->{seen[0]=x.getRequestHeaders().getFirst("User-Agent");seen[1]=x.getRequestHeaders().getFirst("Referer");seen[2]=x.getRequestHeaders().getFirst("Cookie");String r=x.getRequestHeaders().getFirst("Range");int from=r==null?0:Integer.parseInt(r.replace("bytes=","").replace("-",""));
   x.getResponseHeaders().add("Content-Type","video/mp4");if(from>0){x.getResponseHeaders().add("Content-Range","bytes "+from+"-"+(body.length-1)+"/"+body.length);x.sendResponseHeaders(206,body.length-from);}else x.sendResponseHeaders(200,body.length);x.getResponseBody().write(body,from,body.length-from);x.close();});
  s.start();String base="http://127.0.0.1:"+s.getAddress().getPort();
  try{
   HttpURLConnection c=OfflineHttp.open(base+"/r",new JSONObject().put("Referer","https://site.example/").put("Cookie","sid=1"),0);check(c.getResponseCode()==200,"redirect followed");check("MediaPocket/0.1.3".equals(seen[0])&&"https://site.example/".equals(seen[1])&&"sid=1".equals(seen[2]),"player UA and same-origin headers carried across redirect: "+seen[0]+" cookie="+seen[2]);c.disconnect();
   c=OfflineHttp.open(base+"/f",new JSONObject().put("User-Agent","Custom/1"),40000);check(c.getResponseCode()==206&&OfflineRules.totalFromContentRange(c.getHeaderField("Content-Range"))==100000&&c.getContentLengthLong()==60000,"range resume");check("Custom/1".equals(seen[0]),"source UA wins");
   byte[] got=c.getInputStream().readAllBytes();check(got.length==60000&&got[0]==(byte)40000,"resumed bytes");c.disconnect();
  }finally{s.stop(0);}
  HttpServer other=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);final String[] hop=new String[5];
  other.createContext("/file",x->{hop[0]=x.getRequestHeaders().getFirst("User-Agent");hop[1]=x.getRequestHeaders().getFirst("Cookie");hop[2]=x.getRequestHeaders().getFirst("Authorization");hop[3]=x.getRequestHeaders().getFirst("X-Emby-Token");hop[4]=x.getRequestHeaders().getFirst("Referer");x.getResponseHeaders().add("Content-Type","video/mp4");x.sendResponseHeaders(200,4);x.getResponseBody().write(new byte[]{1,2,3,4});x.close();});
  other.start();String otherBase="http://127.0.0.1:"+other.getAddress().getPort();
  HttpServer gate=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  gate.createContext("/go",x->{x.getResponseHeaders().add("Location",otherBase+"/file");x.sendResponseHeaders(302,-1);x.close();});
  gate.start();
  try{JSONObject headers=new JSONObject().put("User-Agent","Custom/1").put("Cookie","sid=1").put("Authorization","Bearer t").put("X-Emby-Token","emby").put("Referer","https://src.example/a");
   HttpURLConnection c=OfflineHttp.open("http://127.0.0.1:"+gate.getAddress().getPort()+"/go",headers,0);check(c.getResponseCode()==200,"cross-port redirect followed");c.disconnect();
   check("Custom/1".equals(hop[0])&&hop[1]==null&&hop[2]==null&&hop[3]==null&&"https://src.example/a".equals(hop[4]),"credentials dropped on another port, referer kept: "+java.util.Arrays.toString(hop));
  }finally{gate.stop(0);other.stop(0);}
  URL https=new URL("https://cdn.example/a"),http=new URL("http://cdn.example/a"),otherHost=new URL("https://other.example/a"),port=new URL("https://cdn.example:8443/a"),same=new URL("https://CDN.example/b");
  check(!RequestHeaders.allow("Cookie",https,http)&&!RequestHeaders.allow("Referer",https,http)&&RequestHeaders.allow("User-Agent",https,http),"https to http drops cookie and referer");
  check(RequestHeaders.allow("Referer",https,otherHost)&&RequestHeaders.allow("Referer",https,port)&&!RequestHeaders.allow("X-Emby-Token",https,otherHost)&&!RequestHeaders.allow("Authorization",https,port),"cross-origin keeps referer and drops credentials");
  check(RequestHeaders.allow("Cookie",https,same)&&RequestHeaders.allow("Referer",https,same),"same origin keeps credentials");
  HttpRange.Plan ignored=HttpRange.plan(200,1000,500,100000,-1);
  check(ignored.skip==1000&&ignored.read==500&&!ignored.empty&&!ignored.reject,"200 skips to position and honors requested length");
  HttpRange.Plan rest=HttpRange.plan(200,1000,HttpRange.UNSET,100000,-1);
  check(rest.skip==1000&&rest.read==99000,"200 without a length reads what remains after the skip");
  HttpRange.Plan partial=HttpRange.plan(206,1000,500,500,100000);
  check(partial.skip==0&&partial.read==500,"206 does not skip");
  HttpRange.Plan atEnd=HttpRange.plan(416,100000,HttpRange.UNSET,-1,100000);
  check(atEnd.empty&&atEnd.read==0&&!atEnd.reject,"416 at the document size is an empty success");
  HttpRange.Plan atEndLen=HttpRange.plan(416,100000,20,-1,100000);
  check(atEndLen.empty&&atEndLen.read==20,"416 at the document size returns the requested length");
  HttpRange.Plan rejected=HttpRange.plan(416,50,10,-1,100000);
  check(rejected.reject&&!rejected.empty,"416 before the end is rejected");
  System.out.println("PASS: in-app downloader follows redirects with source headers, player User-Agent default, Range resume with Content-Range total, keeps Referer across hosts, and strips credentials on cross-origin and https-to-http.");
 }
}

