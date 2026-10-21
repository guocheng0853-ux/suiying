package com.guocheng.mediapocket;
import com.sun.net.httpserver.*;import java.net.*;import java.io.*;import org.json.*;
public class OfflineHttpTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a)throws Exception{
  byte[] body=new byte[100000];for(int i=0;i<body.length;i++)body[i]=(byte)i;
  HttpServer s=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);final String[] seen=new String[2];
  s.createContext("/r",x->{x.getResponseHeaders().add("Location","/f");x.sendResponseHeaders(302,-1);x.close();});
  s.createContext("/f",x->{seen[0]=x.getRequestHeaders().getFirst("User-Agent");seen[1]=x.getRequestHeaders().getFirst("Referer");String r=x.getRequestHeaders().getFirst("Range");int from=r==null?0:Integer.parseInt(r.replace("bytes=","").replace("-",""));
   x.getResponseHeaders().add("Content-Type","video/mp4");if(from>0){x.getResponseHeaders().add("Content-Range","bytes "+from+"-"+(body.length-1)+"/"+body.length);x.sendResponseHeaders(206,body.length-from);}else x.sendResponseHeaders(200,body.length);x.getResponseBody().write(body,from,body.length-from);x.close();});
  s.start();String base="http://127.0.0.1:"+s.getAddress().getPort();
  try{
   HttpURLConnection c=OfflineHttp.open(base+"/r",new JSONObject().put("Referer","https://site.example/"),0);check(c.getResponseCode()==200,"redirect followed");check("MediaPocket/0.1.3".equals(seen[0])&&"https://site.example/".equals(seen[1]),"player UA and source headers carried across redirect: "+seen[0]);c.disconnect();
   c=OfflineHttp.open(base+"/f",new JSONObject().put("User-Agent","Custom/1"),40000);check(c.getResponseCode()==206&&OfflineRules.totalFromContentRange(c.getHeaderField("Content-Range"))==100000&&c.getContentLengthLong()==60000,"range resume");check("Custom/1".equals(seen[0]),"source UA wins");
   byte[] got=c.getInputStream().readAllBytes();check(got.length==60000&&got[0]==(byte)40000,"resumed bytes");c.disconnect();
  }finally{s.stop(0);}
  System.out.println("PASS: in-app downloader follows redirects with source headers, player User-Agent default, Range resume with Content-Range total.");
 }
}
