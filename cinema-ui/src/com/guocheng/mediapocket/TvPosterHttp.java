package com.guocheng.mediapocket;
import java.io.*;import java.net.*;import java.util.*;
final class TvPosterHttp {
 static byte[] download(TvPosterRules.Picture p)throws Exception{return download(p,12000);}
 static byte[] download(TvPosterRules.Picture p,int budget)throws Exception{
  long until=System.nanoTime()+Math.max(1,budget)*1000000L;
  HttpURLConnection c=(HttpURLConnection)new URL(p.url).openConnection();
  c.setConnectTimeout(Math.max(1,Math.min(4000,budget)));c.setReadTimeout(Math.max(1,Math.min(4000,budget)));
  for(Map.Entry<String,String> h:p.headers.entrySet())c.setRequestProperty(h.getKey(),h.getValue());
  try{int code=c.getResponseCode();if(code!=200)throw new IOException("HTTP "+code);
   try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[] b=new byte[8192];int n;
    while(true){long remaining=(until-System.nanoTime())/1000000L;if(remaining<=0)throw new SocketTimeoutException("封面读取超时");c.setReadTimeout((int)Math.max(1,Math.min(4000,remaining)));n=in.read(b);if(n==-1)break;if(out.size()+n>16*1024*1024)throw new IOException("图片过大");out.write(b,0,n);}
    return out.toByteArray();
   }
  }finally{c.disconnect();}
 }
}
