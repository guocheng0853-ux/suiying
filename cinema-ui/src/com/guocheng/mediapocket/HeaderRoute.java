package com.guocheng.mediapocket;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.security.SecureRandom;import java.util.*;import java.util.concurrent.*;import java.util.regex.*;
/**
 * 打包的 LibVLC 3.0.22 只认 :http-user-agent / :http-referrer，原播放器写的 :http-header= 会被直接忽略，
 * 线路声明的 Cookie / Origin / Authorization 等请求头从未发到服务器。
 * VlcPlayback.setSource 入口处调用 url()/headers()：只有存在 VLC 无法携带的请求头时，才把 HTTP(S) 地址改走本机代理，
 * 由代理补齐全部请求头；m3u8 内的分片、密钥、子播放列表地址同样改写为代理地址。只监听 127.0.0.1，路径带随机令牌。
 */
public final class HeaderRoute {
 static final int PLAYLIST_LIMIT=8*1024*1024;
 static volatile Server server;
 private HeaderRoute(){}

 /** 由主 DEX 的 VlcPlayback.setSource 调用；任何异常都退回原地址，绝不阻断原播放流程。 */
 public static String url(String url,Map<String,String> headers){
  try{
   if(url==null||headers==null||headers.isEmpty()||isProxied(url))return url;
   String scheme=URI.create(url.trim()).getScheme();
   if(scheme==null||!(scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https")))return url;
   Map<String,String> all=canonical(headers);
   if(!needsProxy(all))return url;
   return start().wrap(url.trim(),all);
  }catch(Throwable t){return url;}
 }
 /** 交给 VLC 的请求头：只保留它真正支持的两项，键名统一大小写（原实现按 "User-Agent" 精确取值，小写键会丢 UA）。 */
 public static Map<String,String> headers(Map<String,String> headers){
  LinkedHashMap<String,String> out=new LinkedHashMap<>();
  if(headers==null)return out;
  try{for(Map.Entry<String,String> e:canonical(headers).entrySet())if(e.getKey().equals("User-Agent")||e.getKey().equals("Referer"))out.put(e.getKey(),e.getValue());}catch(Throwable ignored){}
  return out;
 }
 static boolean needsProxy(Map<String,String> canonical){for(String k:canonical.keySet())if(!k.equals("User-Agent")&&!k.equals("Referer"))return true;return false;}
 static boolean isProxied(String url){Server s=server;return s!=null&&url.startsWith(s.prefix());}
 /** 统一键名；拒绝含换行的键值（防止请求头注入）；去掉由连接本身决定的逐跳头。 */
 static LinkedHashMap<String,String> canonical(Map<String,String> in){
  LinkedHashMap<String,String> out=new LinkedHashMap<>();
  for(Map.Entry<String,String> e:in.entrySet()){
   String k=e.getKey()==null?"":e.getKey().trim(),v=e.getValue()==null?"":e.getValue().trim();
   if(k.isEmpty()||v.isEmpty()||k.matches(".*[\\r\\n:].*")||v.matches("(?s).*[\\r\\n].*"))continue;
   String lower=k.toLowerCase(Locale.ROOT);
   if(lower.equals("user-agent"))k="User-Agent";else if(lower.equals("referer")||lower.equals("referrer"))k="Referer";else if(lower.equals("cookie"))k="Cookie";else if(lower.equals("origin"))k="Origin";
   if(Arrays.asList("host","content-length","connection","range","transfer-encoding","accept-encoding","keep-alive","upgrade","te").contains(lower))continue;
   out.put(k,v);
  }
  return out;
 }
 static synchronized Server start()throws IOException{Server s=server;if(s==null||s.socket.isClosed()){s=new Server();server=s;}return s;}

 static final class Server {
  final ServerSocket socket;final String token;final ConcurrentHashMap<String,Map<String,String>> headerSets=new ConcurrentHashMap<>();
  final ExecutorService pool=new ThreadPoolExecutor(0,24,30,TimeUnit.SECONDS,new SynchronousQueue<>(),r->{Thread t=new Thread(r,"mp-header-route");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
  Server()throws IOException{
   socket=new ServerSocket(0,64,InetAddress.getByName("127.0.0.1"));
   byte[] b=new byte[16];new SecureRandom().nextBytes(b);StringBuilder t=new StringBuilder();for(byte x:b)t.append(String.format(Locale.ROOT,"%02x",x));token=t.toString();
   Thread accept=new Thread(this::loop,"mp-header-route-accept");accept.setDaemon(true);accept.start();
  }
  String prefix(){return "http://127.0.0.1:"+socket.getLocalPort()+"/"+token+"/";}
  String wrap(String target,Map<String,String> headers){
   String id=digest(headers.toString());headerSets.putIfAbsent(id,new LinkedHashMap<>(headers));
   String encoded=Base64Url.encode(target.getBytes(StandardCharsets.UTF_8));
   return prefix()+id+"/"+encoded+"/"+tail(target);
  }
  static String digest(String s){try{byte[] d=java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(int i=0;i<10;i++)b.append(String.format(Locale.ROOT,"%02x",d[i]));return b.toString();}catch(Exception e){return Integer.toHexString(s.hashCode());}}
  /** 保留原文件名的扩展名，帮助 VLC 选择解复用器。 */
  static String tail(String target){try{String p=new URI(target).getPath();if(p!=null){String n=p.substring(p.lastIndexOf('/')+1).replaceAll("[^A-Za-z0-9._-]","");if(!n.isEmpty())return n.length()>60?n.substring(n.length()-60):n;}}catch(Exception ignored){}return "media";}
  void loop(){while(!socket.isClosed()){try{Socket c=socket.accept();try{pool.execute(()->serve(c));}catch(RejectedExecutionException busy){close(c);}}catch(IOException e){if(socket.isClosed())return;}}}
  static void close(Closeable c){try{c.close();}catch(Exception ignored){}}
  void serve(Socket client){
   try(Socket c=client){
    c.setSoTimeout(30000);
    InputStream in=new BufferedInputStream(c.getInputStream());OutputStream out=new BufferedOutputStream(c.getOutputStream(),65536);
    String line=readLine(in);if(line==null)return;String[] req=line.split(" ");
    Map<String,String> clientHeaders=new HashMap<>();String h;while((h=readLine(in))!=null&&!h.isEmpty()){int i=h.indexOf(':');if(i>0)clientHeaders.put(h.substring(0,i).trim().toLowerCase(Locale.ROOT),h.substring(i+1).trim());}
    if(req.length<2||!(req[0].equals("GET")||req[0].equals("HEAD"))){status(out,405,"text/plain","method");return;}
    String[] parts=req[1].split("/");// "",token,id,b64,name
    if(parts.length<4||!MessageDigestEq.equal(parts[1],token)){status(out,403,"text/plain","forbidden");return;}
    Map<String,String> headers=headerSets.get(parts[2]);if(headers==null){status(out,410,"text/plain","expired");return;}
    String target=new String(Base64Url.decode(parts[3]),StandardCharsets.UTF_8);
    relay(target,headers,clientHeaders.get("range"),req[0].equals("HEAD"),out);
   }catch(Exception ignored){}
  }
  void relay(String target,Map<String,String> headers,String range,boolean head,OutputStream out)throws IOException{
   HttpURLConnection conn=null;String current=target;
   for(int hop=0;hop<6;hop++){
    URL u=new URL(current);if(!(u.getProtocol().equals("http")||u.getProtocol().equals("https"))){status(out,502,"text/plain","bad scheme");return;}
    conn=(HttpURLConnection)u.openConnection();conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(15000);conn.setReadTimeout(30000);conn.setRequestMethod(head?"HEAD":"GET");
    if(!headers.containsKey("User-Agent"))conn.setRequestProperty("User-Agent","Mozilla/5.0");
    for(Map.Entry<String,String> e:headers.entrySet())conn.setRequestProperty(e.getKey(),e.getValue());
    if(range!=null&&range.matches("bytes=[0-9]*-[0-9]*"))conn.setRequestProperty("Range",range);
    conn.setRequestProperty("Accept-Encoding","identity");
    int code=conn.getResponseCode();
    if(code>=300&&code<400&&conn.getHeaderField("Location")!=null){current=new URL(u,conn.getHeaderField("Location")).toString();conn.disconnect();continue;}
    break;
   }
   if(conn==null){status(out,502,"text/plain","no response");return;}
   try{
    int code=conn.getResponseCode();
    if(code>=300&&code<400){status(out,502,"text/plain","too many redirects");return;}
    InputStream body=code>=400?conn.getErrorStream():conn.getInputStream();if(body==null)body=new ByteArrayInputStream(new byte[0]);
    PushbackInputStream peek=new PushbackInputStream(body,16);byte[] first=new byte[16];int n=0;
    if(!head&&code<300){while(n<first.length){int r=peek.read(first,n,first.length-n);if(r<0)break;n+=r;}if(n>0)peek.unread(first,0,n);}
    String type=conn.getContentType();
    if(code<300&&(Sniff.isPlaylist(first,n)||(head&&type!=null&&type.toLowerCase(Locale.ROOT).contains("mpegurl")))){
     if(head){header(out,200,"application/vnd.apple.mpegurl",-1,null);out.flush();return;}
     byte[] text=readAll(peek,PLAYLIST_LIMIT);byte[] rewritten=rewrite(new String(text,StandardCharsets.UTF_8),conn.getURL().toString(),headers).getBytes(StandardCharsets.UTF_8);
     header(out,200,"application/vnd.apple.mpegurl",rewritten.length,null);out.write(rewritten);out.flush();return;
    }
    LinkedHashMap<String,String> extra=new LinkedHashMap<>();
    for(String k:new String[]{"Content-Range","Accept-Ranges","Last-Modified","ETag"}){String v=conn.getHeaderField(k);if(v!=null)extra.put(k,v);}
    long length=conn.getContentLengthLong();
    header(out,code,type==null?"application/octet-stream":type,length,extra);
    if(!head){byte[] buf=new byte[65536];int r;while((r=peek.read(buf))!=-1)out.write(buf,0,r);}
    out.flush();
   }finally{conn.disconnect();}
  }
  static final Pattern URI_ATTR=Pattern.compile("URI=\"([^\"]+)\"");
  String rewrite(String playlist,String base,Map<String,String> headers){
   StringBuilder sb=new StringBuilder();
   for(String raw:playlist.split("\r?\n",-1)){
    String line=raw.trim();
    if(line.isEmpty()){sb.append(raw).append('\n');continue;}
    if(line.startsWith("#")){
     Matcher m=URI_ATTR.matcher(raw);StringBuffer b=new StringBuffer();
     while(m.find()){String abs=absolute(base,m.group(1));m.appendReplacement(b,Matcher.quoteReplacement("URI=\""+(abs==null?m.group(1):wrap(abs,headers))+"\""));}
     m.appendTail(b);sb.append(b).append('\n');continue;
    }
    String abs=absolute(base,line);sb.append(abs==null?raw:wrap(abs,headers)).append('\n');
   }
   return sb.toString();
  }
  static String absolute(String base,String ref){try{URL u=new URL(new URL(base),ref);return u.getProtocol().startsWith("http")?u.toString():null;}catch(Exception e){return null;}}
  static void status(OutputStream out,int code,String type,String text)throws IOException{byte[] b=text.getBytes(StandardCharsets.UTF_8);header(out,code,type,b.length,null);out.write(b);out.flush();}
  static void header(OutputStream out,int code,String type,long length,Map<String,String> extra)throws IOException{
   StringBuilder s=new StringBuilder("HTTP/1.1 ").append(code).append(code==200?" OK":code==206?" Partial Content":" Status").append("\r\nContent-Type: ").append(type).append("\r\nConnection: close\r\n");
   if(length>=0)s.append("Content-Length: ").append(length).append("\r\n");
   if(extra!=null)for(Map.Entry<String,String> e:extra.entrySet())s.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
   out.write(s.append("\r\n").toString().getBytes(StandardCharsets.ISO_8859_1));
  }
  static String readLine(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();int c;while((c=in.read())!=-1){if(c=='\n')break;if(c!='\r')b.write(c);if(b.size()>16384)throw new IOException("header too long");}return c==-1&&b.size()==0?null:b.toString("ISO-8859-1");}
  static byte[] readAll(InputStream in,int limit)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[16384];int r;while((r=in.read(buf))!=-1){b.write(buf,0,r);if(b.size()>limit)throw new IOException("playlist too large");}return b.toByteArray();}
 }
 static final class MessageDigestEq {static boolean equal(String a,String b){return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}}
 /** 不依赖 android.util.Base64 / java.util.Base64（minSdk 26 两者都有，但保持 JVM 单测与设备一致）。 */
 static final class Base64Url {
  static String encode(byte[] b){return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
  static byte[] decode(String s){return java.util.Base64.getUrlDecoder().decode(s);}
 }
 /** 媒体首字节识别，供代理与 LinkProbe 共用。 */
 static final class Sniff {
  static boolean isPlaylist(byte[] b,int n){int i=0;if(n>=3&&(b[0]&0xff)==0xEF&&(b[1]&0xff)==0xBB&&(b[2]&0xff)==0xBF)i=3;while(i<n&&(b[i]==' '||b[i]=='\r'||b[i]=='\n'||b[i]=='\t'))i++;return n-i>=7&&new String(b,i,7,StandardCharsets.ISO_8859_1).equals("#EXTM3U");}
 }
}
