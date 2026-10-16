package com.guocheng.mediapocket;
import java.io.*;import java.net.*;import java.util.*;
public class SubtitleCredentialScopeTest{
 static final List<String> seen=Collections.synchronizedList(new ArrayList<>());
 public static void main(String[]a)throws Exception{try(ServerSocket server=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"))){int port=server.getLocalPort();
  Thread t=new Thread(()->{try{for(int i=0;i<4;i++)try(Socket s=server.accept()){BufferedReader r=new BufferedReader(new InputStreamReader(s.getInputStream(),"US-ASCII"));String first=r.readLine();StringBuilder q=new StringBuilder(first.toLowerCase(Locale.ROOT)).append('\n');String l;while((l=r.readLine())!=null&&!l.isEmpty())q.append(l.toLowerCase(Locale.ROOT)).append('\n');seen.add(q.toString());
   String head;byte[] body="1\n00:00:01,000 --> 00:00:02,000\nx\n\n".getBytes("UTF-8");
   if(first.startsWith("GET /hop "))head="HTTP/1.1 302 Found\r\nLocation: http://localhost:"+port+"/final\r\nContent-Length: 0\r\n";else head="HTTP/1.1 200 OK\r\nContent-Length: "+body.length+"\r\n";
   s.getOutputStream().write((head+"Connection: close\r\n\r\n").getBytes("US-ASCII"));if(!first.startsWith("GET /hop "))s.getOutputStream().write(body);}}catch(Exception e){throw new RuntimeException(e);}});t.start();
  String base="http://127.0.0.1:"+port;Map<String,String> cred=Map.of("Cookie","sid=1","Authorization","Bearer x");
  SubtitleHttp.download(base+"/same",cred);
  SubtitleHttp.download(base+"/other",cred,"subs.example.com");
  SubtitleHttp.download(base+"/hop",cred);
  t.join(5000);
  String same=seen.get(0),other=seen.get(1),hop=seen.get(2),fin=seen.get(3);
  check(same.contains("cookie: sid=1")&&same.contains("authorization: bearer x"),"same host keeps declared credentials");
  check(!other.contains("cookie:")&&!other.contains("authorization:"),"credentials not sent to other host");
  check(hop.contains("cookie: sid=1"),"credentials sent to declared host before redirect");
  check(fin.startsWith("get /final")&&!fin.contains("cookie:")&&!fin.contains("authorization:"),"credentials dropped on cross-host redirect");
  System.out.println("PASS: subtitle credentials scoped to declaring host, dropped for other hosts and cross-host redirects");}}
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
}
