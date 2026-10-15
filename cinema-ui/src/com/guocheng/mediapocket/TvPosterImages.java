package com.guocheng.mediapocket;
import android.content.Context;import android.widget.ImageView;import android.graphics.*;import android.os.*;import android.util.*;
import java.io.*;import java.net.*;import java.security.*;import java.lang.ref.WeakReference;import java.util.*;import java.util.concurrent.*;
final class TvPosterImages {
 static final ExecutorService IO=new ThreadPoolExecutor(4,4,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(192));
 static final Handler MAIN=new Handler(Looper.getMainLooper());
 static final LruCache<String,Bitmap> MEMORY=new LruCache<String,Bitmap>(24*1024*1024){protected int sizeOf(String k,Bitmap v){return v.getByteCount();}};
 static final Map<String,List<WeakReference<ImageView>>> PENDING=new HashMap<>();
 static final ConcurrentLinkedDeque<String> REPORT=new ConcurrentLinkedDeque<>();
 static volatile String lastStatus="尚未加载封面图片";
 static File directory(Context c){return new File(c.getCacheDir(),"tv-posters-v2");}
 static boolean valid(byte[] b){BitmapFactory.Options p=new BitmapFactory.Options();p.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(b,0,b.length,p);return p.outWidth>0&&p.outHeight>0;}
 static void load(Context c,ImageView view,String raw,int width){
  if(raw==null||raw.trim().isEmpty()){view.setTag(null);record("","封面地址为空");return;}
  final Context app=c.getApplicationContext();final String tag="tvposter:"+hash(raw)+":"+width;
  view.setTag(tag);Bitmap memory=MEMORY.get(tag);if(memory!=null){view.setImageBitmap(memory);return;}
  synchronized(PENDING){List<WeakReference<ImageView>> group=PENDING.get(tag);if(group!=null){group.add(new WeakReference<>(view));return;}group=new ArrayList<>();group.add(new WeakReference<>(view));PENDING.put(tag,group);}
  try{IO.execute(()->{
   Bitmap result=null;String status="加载失败";
   try{
    byte[] bytes=CoverFiles.obtain(new File(directory(app),hash(raw)+".img"),()->fetch(raw),TvPosterImages::valid);
    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
    BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;
    while(bounds.outWidth/options.inSampleSize>Math.max(512,width)*2||bounds.outHeight/options.inSampleSize>4096||(long)bounds.outWidth*bounds.outHeight/options.inSampleSize/options.inSampleSize>8000000L)options.inSampleSize*=2;
    result=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(result==null)throw new IOException("图片解码失败");
    MEMORY.put(tag,result);status="加载成功";trim(directory(app));
   }catch(Exception e){status=reason(e);}
   record(raw,status);complete(tag,result,status);
  });}catch(RejectedExecutionException e){record(raw,"图片队列已满，请重新打开页面");complete(tag,null,"图片队列已满");}
 }
 static byte[] fetch(String raw)throws Exception{
  TvPosterRules.Picture p=TvPosterRules.parse(raw);
  if(p.url.startsWith("data:image/")){int comma=p.url.indexOf(',');if(comma<0||p.url.length()>3*1024*1024)throw new IOException("内嵌图片格式不支持");return android.util.Base64.decode(p.url.substring(comma+1),android.util.Base64.DEFAULT);}
  long until=System.nanoTime()+12000000000L;Exception failure=null;
  for(TvPosterRules.Picture candidate:TvPosterRules.candidates(p)){
   int remaining=(int)((until-System.nanoTime())/1000000L);if(remaining<=0)break;
   try{byte[] bytes=TvPosterHttp.download(candidate,remaining);if(!valid(bytes))throw new IOException("返回内容不是图片");record(candidate.url,"下载成功");return bytes;}
   catch(Exception e){failure=e;record(candidate.url,reason(e));}
  }
  throw failure==null?new SocketTimeoutException("封面读取超时"):failure;
 }
 static void complete(String tag,Bitmap bitmap,String status){
  final List<WeakReference<ImageView>> targets;synchronized(PENDING){targets=PENDING.remove(tag);}
  if(targets==null)return;MAIN.post(()->{for(WeakReference<ImageView> ref:targets){ImageView v=ref.get();if(v!=null&&tag.equals(v.getTag())){if(bitmap!=null)v.setImageBitmap(bitmap);else v.setContentDescription(status);}}});
 }
 static String reason(Exception e){if(e instanceof SocketTimeoutException)return "连接或读取超时";if(e instanceof UnknownHostException)return "域名解析失败";if(e instanceof javax.net.ssl.SSLException)return "TLS连接失败";String s=e.getMessage();return s!=null&&(s.matches("HTTP [0-9]{3}")||s.equals("返回内容不是图片")||s.equals("图片解码失败"))?s:e.getClass().getSimpleName();}
 static void record(String raw,String result){String host=raw.isEmpty()?"目录数据":"内嵌图片";try{String url=TvPosterRules.parse(raw).url;if(!url.isEmpty()&&!url.startsWith("data:"))host=new URI(url).getHost();}catch(Exception ignored){}lastStatus=String.valueOf(host)+"："+result;REPORT.remove(lastStatus);REPORT.addFirst(lastStatus);while(REPORT.size()>20)REPORT.pollLast();}
 static String diagnostic(){StringBuilder text=new StringBuilder();for(String line:REPORT)text.append(line).append('\n');return text.length()==0?lastStatus:text.toString();}
 static void clearDisk(Context c){MEMORY.evictAll();File[] files=directory(c).listFiles((d,n)->n.endsWith(".img"));if(files!=null)for(File f:files)f.delete();}
 static long diskSize(Context c){long size=0;File[] files=directory(c).listFiles((d,n)->n.endsWith(".img"));if(files!=null)for(File f:files)size+=f.length();return size;}
 static byte[] read(InputStream stream,int limit)throws Exception{return CoverFiles.read(stream,limit);}
 static String hash(String s){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}catch(Exception e){return String.valueOf(s.hashCode());}}
 static void trim(File dir){File[] files=dir.listFiles((d,n)->n.endsWith(".img"));if(files==null)return;long total=0;for(File f:files)total+=f.length();if(total<=128L*1024*1024&&files.length<=800)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(File f:files){long size=f.length();if(f.delete())total-=size;if(total<=96L*1024*1024)break;}}
}
