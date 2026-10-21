package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.*;import android.os.Handler;import android.os.Looper;import org.json.*;import java.util.*;import java.util.function.Consumer;
/** Jellyfin Trickplay：每张拼图只建一个区域解码器，解码在预览线程，只要最新一次拖动的那一格。失败就返回 null，不把整张拼图缩小后当这一帧。 */
final class JellyfinTrickplay {
 final TrickplayRules.Info info;final String serverId,item,media;
 final Map<Integer,Tile> sheets=new LinkedHashMap<Integer,Tile>(4,.75f,true){protected boolean removeEldestEntry(Map.Entry<Integer,Tile> e){if(size()<=2)return false;e.getValue().close();return true;}};
 volatile int wantedMs=Integer.MIN_VALUE;volatile boolean busy;Consumer<Bitmap> sink;
 static final Handler MAIN=new Handler(Looper.getMainLooper());
 JellyfinTrickplay(TrickplayRules.Info i,String s,String it,String m){info=i;serverId=s;item=it;media=m;}
 /** 在后台线程调用；服务器不是 Jellyfin 或没生成 Trickplay 时返回 null。 */
 static JellyfinTrickplay load(Context c,JSONObject server,String itemId,String mediaSourceId){try{JSONObject it=EmbyClient.json(c,server,"GET","/Users/"+EmbyApi.enc(server.optString("userId"))+"/Items/"+EmbyApi.enc(itemId)+"?Fields=Trickplay",null);TrickplayRules.Info i=TrickplayRules.pick(it,mediaSourceId,320);return i==null?null:new JellyfinTrickplay(i,server.optString("id"),itemId,mediaSourceId);}catch(Exception e){return null;}}
 /** 可以在主线程调用。真正解码在预览线程；连着拖时只保留最后一次。 */
 void frame(Context c,int ms,Consumer<Bitmap> done){final Context app=c.getApplicationContext();synchronized(this){wantedMs=ms;sink=done;if(busy)return;busy=true;}PreviewFetch.IO.execute(()->pump(app));}
 void pump(Context app){try{while(true){int ms;Consumer<Bitmap> cb;synchronized(this){ms=wantedMs;cb=sink;}if(ms==Integer.MIN_VALUE){synchronized(this){busy=false;}return;}int[] at=TrickplayRules.locate(info,ms);Tile tile=obtain(app,at[0],ms);synchronized(this){if(wantedMs!=ms)continue;}Bitmap bm=tile==null?null:tile.crop(info,at[1],at[2]);synchronized(this){if(wantedMs!=ms){if(bm!=null)bm.recycle();continue;}busy=false;}Bitmap ready=bm;MAIN.post(()->{if(cb!=null)cb.accept(ready);});return;}}catch(Exception ignored){synchronized(this){busy=false;}}}
 Tile obtain(Context c,int index,int ms){synchronized(sheets){Tile have=sheets.get(index);if(have!=null)return have;}if(wantedMs!=ms)return null;try{JSONObject server=EmbyStore.get(c,serverId);if(server==null||wantedMs!=ms)return null;byte[] b=EmbyClient.bytes(c,server,TrickplayRules.path(item,media,info,index));if(b==null||b.length==0||wantedMs!=ms)return null;Tile tile=new Tile(b);synchronized(sheets){sheets.put(index,tile);return sheets.get(index)==null?tile:sheets.get(index);}}catch(Exception e){return null;}}
 void close(){synchronized(this){wantedMs=Integer.MIN_VALUE;sink=null;}synchronized(sheets){for(Tile t:sheets.values())t.close();sheets.clear();}}
 static final class Tile {
  final byte[] jpeg;BitmapRegionDecoder dec;
  Tile(byte[] jpeg){this.jpeg=jpeg;}
  synchronized Bitmap crop(TrickplayRules.Info info,int x,int y){try{if(dec==null)dec=BitmapRegionDecoder.newInstance(jpeg,0,jpeg.length,false);BitmapFactory.Options opt=new BitmapFactory.Options();opt.inPreferredConfig=Bitmap.Config.RGB_565;int left=Math.max(0,x),top=Math.max(0,y);int right=Math.min(dec.getWidth(),left+Math.max(1,info.width)),bottom=Math.min(dec.getHeight(),top+Math.max(1,info.height));if(right<=left||bottom<=top)return null;return dec.decodeRegion(new Rect(left,top,right,bottom),opt);}catch(Exception e){close();return null;}}
  synchronized void close(){if(dec!=null){try{dec.recycle();}catch(Exception ignored){}dec=null;}}
 }
}
