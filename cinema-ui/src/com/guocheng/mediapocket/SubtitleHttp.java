package com.guocheng.mediapocket;
import java.net.*;import java.io.*;import java.util.*;
/**
 * 字幕下载。extra 只能是字幕条目自己声明的请求头（不得传入播放请求头）。
 * Cookie / Authorization 只发往声明它们的主机（默认即字幕条目地址的主机）；跨主机重定向时会被去掉。
 */
final class SubtitleHttp {
 static int effectivePort(URL u){return u.getPort()<0?u.getDefaultPort():u.getPort();}
 static boolean credential(String n){return n.equalsIgnoreCase("Cookie")||n.equalsIgnoreCase("Authorization");}
 static byte[] download(String raw,Map<String,String> extra)throws Exception{return download(raw,extra,null);}
 static byte[] download(String raw,Map<String,String> extra,String credentialHost)throws Exception{
  TvPosterRules.Picture p=TvPosterRules.parse(raw);URL u=new URL(p.url);
  URL declaring=u;
  String owner=(credentialHost==null||credentialHost.isEmpty()?u.getHost():credentialHost).toLowerCase(Locale.ROOT);
  Map<String,String> headers=new LinkedHashMap<>(p.headers);
  if(extra!=null)for(Map.Entry<String,String> h:extra.entrySet()){String n=h.getKey(),v=h.getValue();if(n==null||v==null)continue;if(!(n.equalsIgnoreCase("Referer")||n.equalsIgnoreCase("User-Agent")||n.equalsIgnoreCase("Origin")||n.equalsIgnoreCase("Accept")||credential(n)))continue;if(v.indexOf('\r')>=0||v.indexOf('\n')>=0||v.indexOf('\0')>=0)throw new IOException("非法字幕请求头");headers.put(n,v);}
  for(int hop=0;hop<6;hop++){
   if(!u.getProtocol().equals("http")&&!u.getProtocol().equals("https"))throw new IOException("字幕地址类型不支持");
   boolean sameHost=u.getHost().toLowerCase(Locale.ROOT).equals(owner)&&u.getProtocol().equals(declaring.getProtocol())&&effectivePort(u)==effectivePort(declaring);
   HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(15000);
   for(Map.Entry<String,String> h:headers.entrySet()){if(credential(h.getKey())&&!sameHost)continue;c.setRequestProperty(h.getKey(),h.getValue());}
   try{int status=c.getResponseCode();
    if(status==301||status==302||status==303||status==307||status==308){String loc=c.getHeaderField("Location");if(loc==null)throw new IOException("HTTP "+status);URL next=new URL(u,loc);if("https".equals(u.getProtocol())&&"http".equals(next.getProtocol()))throw new IOException("字幕重定向不允许降级到 HTTP");u=next;continue;}
    if(status!=200)throw new IOException("HTTP "+status);
    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("字幕文件过大");out.write(b,0,n);}return out.toByteArray();}
   }finally{c.disconnect();}
  }
  throw new IOException("字幕重定向过多");
 }
}
