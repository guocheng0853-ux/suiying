package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.*;import android.os.Handler;import android.os.Looper;import org.json.*;import java.util.*;import java.util.function.Consumer;
/** 0.1.124 Jellyfin Trickplay 预览：读条目的 Trickplay 信息，按需下载拼图大图（内存里最多留 4 张），裁出那一帧。 */
final class JellyfinTrickplay {
 final TrickplayRules.Info info;final String serverId,item,media;final Map<Integer,Bitmap> tiles=new LinkedHashMap<Integer,Bitmap>(8,.75f,true){protected boolean removeEldestEntry(Map.Entry<Integer,Bitmap> e){return size()>4;}};final Set<Integer> loading=new HashSet<>();static final Handler MAIN=new Handler(Looper.getMainLooper());
 JellyfinTrickplay(TrickplayRules.Info i,String s,String it,String m){info=i;serverId=s;item=it;media=m;}
 /** 在后台线程调用；服务器不是 Jellyfin 或没生成 Trickplay 时返回 null。 */
 static JellyfinTrickplay load(Context c,JSONObject server,String itemId,String mediaSourceId){try{JSONObject it=EmbyClient.json(c,server,"GET","/Users/"+EmbyApi.enc(server.optString("userId"))+"/Items/"+EmbyApi.enc(itemId)+"?Fields=Trickplay",null);TrickplayRules.Info i=TrickplayRules.pick(it,mediaSourceId,320);return i==null?null:new JellyfinTrickplay(i,server.optString("id"),itemId,mediaSourceId);}catch(Exception e){return null;}}
 /** 主线程调用；有缓存立刻回调，没有就后台下载后回调（期间位置变了也回调最新那张，调用方自己判断）。 */
 void frame(Context c,int ms,Consumer<Bitmap> done){int[] at=TrickplayRules.locate(info,ms);Bitmap tile;synchronized(tiles){tile=tiles.get(at[0]);}
  if(tile!=null){done.accept(crop(tile,at));return;}
  synchronized(loading){if(!loading.add(at[0]))return;}
  EmbyPlayback.IO.execute(()->{try{JSONObject server=EmbyStore.get(c,serverId);if(server==null)return;byte[] b=EmbyClient.bytes(c,server,TrickplayRules.path(item,media,info,at[0]));Bitmap bm=BitmapFactory.decodeByteArray(b,0,b.length);if(bm==null)return;synchronized(tiles){tiles.put(at[0],bm);}MAIN.post(()->done.accept(crop(bm,at)));}catch(Exception ignored){}finally{synchronized(loading){loading.remove(at[0]);}}});}
 Bitmap crop(Bitmap tile,int[] at){int w=Math.min(info.width,tile.getWidth()-at[1]),h=Math.min(info.height,tile.getHeight()-at[2]);if(w<=0||h<=0)return null;return Bitmap.createBitmap(tile,at[1],at[2],w,h);}
}
