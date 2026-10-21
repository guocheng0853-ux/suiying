package com.guocheng.mediapocket;
import android.net.Uri;import androidx.media3.common.C;import androidx.media3.datasource.*;import java.io.*;import java.net.*;import java.util.*;
/**
 * Media3 播放用的 HTTP 源。自己跟随重定向，凭证头按 RequestHeaders 裁剪：
 * 跨主机 / 跨端口去掉 Cookie、Authorization、X-Emby-Token；Referer 仍然带上。
 * https 降到 http 时这些头和 Referer 都不带。User-Agent 等普通头保留。
 * 对方忽略 Range 而返回 200 时，跳到请求的位置并按请求长度截断；416 且起点已是文件末尾则当作读完。
 * 跳过之前先 transferStarted：bytesTransferred 会把 DataSpec 交给 DefaultBandwidthMeter，此时字段还是 null 会空指针。
 */
final class ScopedHttpDataSource extends BaseDataSource implements HttpDataSource {
 final String userAgent;final Map<String,String> headers=new LinkedHashMap<>();
 DataSpec spec;HttpURLConnection connection;InputStream input;Uri uri;Map<String,List<String>> responseHeaders=new HashMap<>();int responseCode;long remaining=-1;boolean opened;
 ScopedHttpDataSource(String userAgent,Map<String,String> headers){super(true);this.userAgent=userAgent==null||userAgent.isEmpty()?OfflineHttp.UA:userAgent;if(headers!=null)for(Map.Entry<String,String> e:headers.entrySet())if(e.getKey()!=null&&e.getValue()!=null)this.headers.put(e.getKey(),e.getValue());}
 @Override public void setRequestProperty(String name,String value){if(name!=null&&value!=null)headers.put(name,value);}
 @Override public void clearRequestProperty(String name){headers.remove(name);}
 @Override public void clearAllRequestProperties(){headers.clear();}
 @Override public int getResponseCode(){return responseCode;}
 @Override public Map<String,List<String>> getResponseHeaders(){return responseHeaders;}
 @Override public Uri getUri(){return uri;}
 @Override public long open(DataSpec spec)throws HttpDataSourceException{
  this.spec=spec;transferInitializing(spec);HttpURLConnection c=null;
  try{URL origin=new URL(spec.uri.toString());URL current=origin;Map<String,String> req=new LinkedHashMap<>(headers);if(spec.httpRequestHeaders!=null)req.putAll(spec.httpRequestHeaders);
   for(int hop=0;hop<8;hop++){if(c!=null){c.disconnect();c=null;}if(!("http".equals(current.getProtocol())||"https".equals(current.getProtocol())))throw new IOException("不支持的地址");
    c=(HttpURLConnection)current.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestMethod(spec.httpMethod==DataSpec.HTTP_METHOD_HEAD?"HEAD":"GET");
    boolean ua=false;for(Map.Entry<String,String> e:req.entrySet()){String k=e.getKey(),v=e.getValue();if(k==null||v==null||k.isEmpty()||v.isEmpty()||k.equalsIgnoreCase("Range")||k.equalsIgnoreCase("Host"))continue;if(!RequestHeaders.allow(k,origin,current))continue;c.setRequestProperty(k,v);if(k.equalsIgnoreCase("User-Agent"))ua=true;}
    if(!ua)c.setRequestProperty("User-Agent",userAgent);
    if(spec.position>0||spec.length!=C.LENGTH_UNSET){String end=spec.length==C.LENGTH_UNSET?"":Long.toString(spec.position+spec.length-1);c.setRequestProperty("Range","bytes="+spec.position+"-"+end);}
    c.setRequestProperty("Accept-Encoding","identity");int code=c.getResponseCode();
    if(code==301||code==302||code==303||code==307||code==308){String loc=c.getHeaderField("Location");if(loc==null||loc.isEmpty())throw new IOException("重定向没有目标地址");current=new URL(current,loc);continue;}
    responseCode=code;responseHeaders=copy(c.getHeaderFields());
    long contentLength=c.getContentLengthLong();long documentSize=OfflineRules.totalFromContentRange(c.getHeaderField("Content-Range"));
    HttpRange.Plan plan=HttpRange.plan(code,spec.position,spec.length==C.LENGTH_UNSET?HttpRange.UNSET:spec.length,contentLength<0?HttpRange.UNSET:contentLength,documentSize<0?HttpRange.UNSET:documentSize);
    if(plan.reject||((code<200||code>299)&&!plan.empty)){byte[] err=readLimited(c.getErrorStream());HttpURLConnection bad=c;c=null;bad.disconnect();throw new InvalidResponseCodeException(code,"",null,responseHeaders,spec,err);}
    if(plan.empty){c.disconnect();c=null;input=new ByteArrayInputStream(new byte[0]);remaining=0;uri=Uri.parse(current.toString());transferStarted(spec);opened=true;return plan.read;}
    input=c.getInputStream();connection=c;c=null;uri=Uri.parse(current.toString());transferStarted(spec);opened=true;
    // 64 MB 上限只拦这一次用户拖动认领的字节位置。续播、第一次打开、读 moov 没有 armSeek，不拦。
    // 错误码 2008，Exo 不重试；倒计时过了再重试就会把整段下下来。
    if(HttpRange.rejectSeek(spec.position,plan.skip)){InputStream openedIn=input;HttpURLConnection openedConn=connection;input=null;connection=null;try{if(openedIn!=null)openedIn.close();}catch(IOException ignored){}if(openedConn!=null)openedConn.disconnect();if(opened){opened=false;transferEnded();}throw new HttpDataSource.HttpDataSourceException(HttpRange.SKIP_MESSAGE,spec,HttpRange.SKIP_ERROR,HttpDataSource.HttpDataSourceException.TYPE_OPEN);}
    if(plan.skip>0)skipFully(input,plan.skip);
    HttpRange.openedSeek(spec.position,plan.skip);
    remaining=plan.read;return remaining>=0?remaining:C.LENGTH_UNSET;}
   throw new IOException("重定向次数过多");
  }catch(HttpDataSourceException e){throw e;}catch(IOException e){if(c!=null)c.disconnect();if(connection!=null||input!=null||opened)try{close();}catch(HttpDataSourceException ignored){}throw HttpDataSourceException.createForIOException(e,spec,HttpDataSourceException.TYPE_OPEN);}
 }
 @Override public int read(byte[] buffer,int offset,int length)throws HttpDataSourceException{try{if(length==0)return 0;if(remaining==0)return C.RESULT_END_OF_INPUT;int n=input.read(buffer,offset,remaining>0?(int)Math.min(length,remaining):length);if(n==-1)return C.RESULT_END_OF_INPUT;if(remaining>0)remaining-=n;bytesTransferred(n);return n;}catch(IOException e){throw HttpDataSourceException.createForIOException(e,spec,HttpDataSourceException.TYPE_READ);}}
 @Override public void close()throws HttpDataSourceException{try{if(input!=null)try{input.close();}catch(IOException ignored){}input=null;if(connection!=null){connection.disconnect();connection=null;}}finally{if(opened){opened=false;transferEnded();}}}
 static Map<String,List<String>> copy(Map<String,List<String>> in){Map<String,List<String>> out=new HashMap<>();if(in!=null)for(Map.Entry<String,List<String>> e:in.entrySet())if(e.getKey()!=null)out.put(e.getKey(),e.getValue());return out;}
 static byte[] readLimited(InputStream in){if(in==null)return new byte[0];try{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[1024];int n,total=0;while(total<4096&&(n=in.read(buf))>0){b.write(buf,0,n);total+=n;}return b.toByteArray();}catch(IOException e){return new byte[0];}finally{try{in.close();}catch(IOException ignored){}}}
 /** 调用前必须已经 transferStarted。跳过的字节同样通知监听器，和 DefaultHttpDataSource 一致。 */
 void skipFully(InputStream in,long n)throws IOException{byte[] buf=new byte[8192];long left=n;while(left>0){int r=in.read(buf,0,(int)Math.min(buf.length,left));if(r<0)throw new IOException("服务器忽略了断点，文件比请求的位置短");left-=r;bytesTransferred(r);}}
}
