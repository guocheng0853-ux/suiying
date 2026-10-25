package com.guocheng.mediapocket;
import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;
/** Read-only diagnostics transport. API credentials never follow a different origin. */
final class EmbyHealthHttp implements EmbyHealthCheck.Backend {
 static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"emby-health-timeout");t.setDaemon(true);return t;});
 static final ExecutorService DNS=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"emby-health-dns");t.setDaemon(true);return t;});
 volatile boolean cancelled;volatile HttpURLConnection current;
 public int resolve(String host)throws Exception{Future<InetAddress[]> f=DNS.submit(()->InetAddress.getAllByName(host));try{return f.get(5,TimeUnit.SECONDS).length;}catch(TimeoutException e){f.cancel(true);throw new SocketTimeoutException();}catch(ExecutionException e){if(e.getCause() instanceof Exception)throw (Exception)e.getCause();throw new IOException();}finally{if(cancelled)f.cancel(true);}}
 public void cancel(){cancelled=true;HttpURLConnection c=current;if(c!=null)TIMER.execute(c::disconnect);}
 public EmbyHealthCheck.Response request(String method,String url,Map<String,String> headers,String body)throws Exception{return request(method,url,headers,body,10000);}
 EmbyHealthCheck.Response request(String method,String url,Map<String,String> headers,String body,int budget)throws Exception{
  long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(budget);URL first=new URL(url),target=first;
  if(first.getUserInfo()!=null)throw new IOException("invalid endpoint");
  for(int hop=0;hop<4;hop++){
   if(cancelled||Thread.currentThread().isInterrupted())throw new InterruptedIOException();
   long left=TimeUnit.NANOSECONDS.toMillis(end-System.nanoTime());if(left<=0)throw new SocketTimeoutException();
   HttpURLConnection c=(HttpURLConnection)target.openConnection();current=c;c.setInstanceFollowRedirects(false);c.setRequestMethod(method);c.setConnectTimeout((int)Math.min(left,8000));c.setReadTimeout((int)left);
   for(Map.Entry<String,String> h:headers.entrySet())c.setRequestProperty(h.getKey(),h.getValue());
   Thread reader=Thread.currentThread();ScheduledFuture<?> timer=TIMER.schedule(()->{c.disconnect();reader.interrupt();},left,TimeUnit.MILLISECONDS);
   try{
    if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");byte[] bytes=body.getBytes("UTF-8");c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
    int code=c.getResponseCode();
    if(code>=300&&code<400){String location=c.getHeaderField("Location");if(location==null)throw new IOException("invalid redirect");URL next=new URL(target,location);if(!sameOrigin(first,next)||next.getUserInfo()!=null)throw new IOException("cross-origin redirect");target=next;continue;}
    ByteArrayOutputStream out=new ByteArrayOutputStream();InputStream raw=code>=400?c.getErrorStream():c.getInputStream();
    if(raw!=null)try(InputStream in=raw){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(cancelled||System.nanoTime()>=end||Thread.currentThread().isInterrupted())throw new SocketTimeoutException();if(out.size()+n>1024*1024)throw new IOException("response too large");out.write(b,0,n);}}
    return new EmbyHealthCheck.Response(code,out.toString("UTF-8"));
   }finally{timer.cancel(false);c.disconnect();current=null;Thread.interrupted();}
  }
  throw new IOException("too many redirects");
 }
 static boolean sameOrigin(URL a,URL b){return a.getProtocol().equalsIgnoreCase(b.getProtocol())&&a.getHost().equalsIgnoreCase(b.getHost())&&(a.getPort()<0?a.getDefaultPort():a.getPort())==(b.getPort()<0?b.getDefaultPort():b.getPort());}
}
