package com.guocheng.mediapocket;
import android.content.Context;import android.os.Handler;import android.os.Looper;import org.json.*;import java.io.*;import java.util.concurrent.*;import java.util.function.Consumer;
/** BIF 和 Trickplay 下载走单独线程，不占用 Emby 开播 / 进度回报那条单线程。同一部片子只下一份 BIF。 */
final class PreviewFetch {
 static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"emby-preview");t.setDaemon(true);return t;});
 static final Handler MAIN=new Handler(Looper.getMainLooper());
 static final ConcurrentHashMap<String,Object> LOCKS=new ConcurrentHashMap<>();
 /** 最近两部片子的 BIF。播放页退出时清掉，避免连看把整份预览文件留在内存里。 */
 static final BifCache MEMORY=new BifCache();
 private PreviewFetch(){}
 static void evict(){MEMORY.clear();}
 static String key(String sid,String item,String media){return (sid==null?"":sid)+"|"+(item==null?"":item)+"|"+(media==null?"":media);}
 static File bifFile(Context c,String sid,String item,String media){File dir=new File(c.getCacheDir(),"emby-bif");return new File(dir,EmbyPlayerUi.safe(sid)+"-"+EmbyPlayerUi.safe(item)+"-"+EmbyPlayerUi.safe(media)+".bif");}
 /** 可在预览线程里直接调用。缓存损坏或写到一半的文件会删掉再下一次。 */
 static BifIndex fetchBif(Context c,JSONObject server,String sid,String item,String media){
  String id=key(sid,item,media);Object lock=LOCKS.computeIfAbsent(id,k->new Object());
  synchronized(lock){BifIndex mem=MEMORY.get(id);if(mem!=null)return mem;File dest=bifFile(c,sid,item,media);BifIndex parsed=readIfValid(dest);
   if(parsed==null&&server!=null){try{byte[] data=EmbyClient.bytes(c,server,EmbyApi.bifPath(item,media));parsed=BifIndex.parse(data);if(parsed!=null)writeAtomic(dest,data);}catch(Exception ignored){parsed=null;}}
   if(parsed!=null)MEMORY.put(id,parsed);return parsed;}}
 static void loadBif(Context c,String sid,String item,String media,Consumer<BifIndex> done){final Context app=c.getApplicationContext();IO.execute(()->{BifIndex parsed=null;try{JSONObject server=EmbyStore.get(app,sid);parsed=fetchBif(app,server,sid,item,media);}catch(Exception ignored){}BifIndex ready=parsed;MAIN.post(()->{if(done!=null)done.accept(ready);});});}
 static BifIndex readIfValid(File dest){if(dest==null||!dest.isFile()||dest.length()<64)return null;try{BifIndex parsed=BifIndex.parse(EmbyPlayerUi.readFile(dest));if(parsed!=null)return parsed;}catch(Exception ignored){}dest.delete();return null;}
 static void writeAtomic(File dest,byte[] data)throws IOException{File dir=dest.getParentFile();if(dir!=null)dir.mkdirs();File tmp=new File(dir,dest.getName()+".tmp");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(data);out.flush();}if(dest.exists()&&!dest.delete()){tmp.delete();return;}if(!tmp.renameTo(dest))tmp.delete();}
}
