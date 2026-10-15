package com.guocheng.mediapocket;
import java.io.*;import java.net.*;
public class CoverTimeoutTest {
 public static void main(String[] args)throws Exception{
  try(ServerSocket server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){
   Thread fixture=new Thread(()->{try(Socket c=server.accept()){BufferedReader in=new BufferedReader(new InputStreamReader(c.getInputStream(),"US-ASCII"));String line;while((line=in.readLine())!=null&&!line.isEmpty()){}c.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 3\r\nConnection: close\r\n\r\n".getBytes("US-ASCII"));c.getOutputStream().flush();Thread.sleep(700);}catch(Exception ignored){}});fixture.start();
   long start=System.nanoTime();boolean timedOut=false;
   try{TvPosterHttp.download(TvPosterRules.parse("http://127.0.0.1:"+server.getLocalPort()+"/slow.jpg"),120);}catch(SocketTimeoutException expected){timedOut=true;}
   long elapsed=(System.nanoTime()-start)/1000000L;if(!timedOut||elapsed>1000)throw new AssertionError("stalled cover did not time out promptly: "+elapsed);
   fixture.join(2000);System.out.println("PASS: stalled body times out after "+elapsed+"ms rather than occupying image worker indefinitely");
  }
 }
}
