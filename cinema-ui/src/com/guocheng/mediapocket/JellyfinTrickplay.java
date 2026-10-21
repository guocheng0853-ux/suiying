package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.*;import android.os.Handler;import android.os.Looper;import org.json.*;import java.io.ByteArrayInputStream;import java.util.*;import java.util.function.Consumer;
/** 0.1.130 Jellyfin Trickplay：只在内存里留最近两张拼图的 JPEG，用区域解码取出那一格，RGB_565。 */
final class JellyfinTrickplay {
 final TrickplayRules.Info info;final String serverId,item,media;
 final Map<Integer,byte[]> sheets=new LinkedHashMap<Integer,byte[]>(4,.75f,true){protected boolean removeEldestEntry(Map.Entry<Integer,byte[]> e){return size()>2;}};
 final Set<Integer> loading=new HashSet<>();static final Handler MAIN=new Handler(Looper.getMainLooper());
 JellyfinTrickplay(TrickplayRules.Info i,String s,String it,String m){info=i;serverId=s;item=it;media=m;}
 /** 在后台线程调用；服务器不是 Jellyfin 或没生成 Trickplay 时返回 null。 */
 static JellyfinTrickplay load(Context c,JSONObject server,String itemId,String mediaSourceId){try{JSONObject it=EmbyClient.json(c,server,"GET","/Users/"+EmbyApi.enc(server.optString("userId"))+"/Items/"+EmbyApi.enc(itemId)+"?Fields=Trickplay",null);TrickplayRules.Info i=TrickplayRules.pick(it,mediaSourceId,320);return i==null?null:new JellyfinTrickplay(i,server.optString("id"),itemId,mediaSourceId);}catch(Exception e){return null;}}
 /** 主线程调用；有缓存立刻回调，没有就在预览线程下载后回调。 */
 void frame(Context c,int ms,Consumer<Bitmap> done){int[] at=TrickplayRules.locate(info,ms);byte[] jpeg;synchronized(sheets){jpeg=sheets.get(at[0]);}
  if(jpeg!=null){done.accept(region(jpeg,at));return;}
  synchronized(loading){if(!loading.add(at[0]))return;}
  final int tile=at[0];final int[] where=at;
  PreviewFetch.IO.execute(()->{try{JSONObject server=EmbyStore.get(c,serverId);if(server==null)return;byte[] b=EmbyClient.bytes(c,server,TrickplayRules.path(item,media,info,tile));if(b==null||b.length==0)return;synchronized(sheets){sheets.put(tile,b);}Bitmap bm=region(b,where);if(bm!=null)MAIN.post(()->done.accept(bm));}catch(Exception ignored){}finally{synchronized(loading){loading.remove(tile);}}});}
 Bitmap region(byte[] jpeg,int[] at){BitmapRegionDecoder dec=null;try{dec=BitmapRegionDecoder.newInstance(jpeg,0,jpeg.length,false);BitmapFactory.Options opt=new BitmapFactory.Options();opt.inPreferredConfig=Bitmap.Config.RGB_565;int left=Math.max(0,at[1]),top=Math.max(0,at[2]);int right=Math.min(dec.getWidth(),left+Math.max(1,info.width)),bottom=Math.min(dec.getHeight(),top+Math.max(1,info.height));if(right<=left||bottom<=top)return null;return dec.decodeRegion(new Rect(left,top,right,bottom),opt);}catch(Exception e){BitmapFactory.Options opt=new BitmapFactory.Options();opt.inPreferredConfig=Bitmap.Config.RGB_565;opt.inSampleSize=Math.max(1,info.tileW);return BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,opt);}finally{if(dec!=null)dec.recycle();}}
}
