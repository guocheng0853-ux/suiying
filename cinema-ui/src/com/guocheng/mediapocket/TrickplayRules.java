package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/**
 * 0.1.124 Jellyfin Trickplay（10.9 起）的拖动预览：条目的 Trickplay 字段给出
 * {媒体源 Id: {宽度: {Width, Height, TileWidth, TileHeight, ThumbnailCount, Interval}}}。
 * 预览图拼成一张张大图：第 n 张缩略图在第 n /(TileWidth×TileHeight) 张大图里，行列按余数算；时间 = n × Interval 毫秒。
 * 大图地址 /Videos/{条目}/Trickplay/{宽度}/{大图序号}.jpg?MediaSourceId=…。不依赖 Android。
 */
final class TrickplayRules {
 private TrickplayRules(){}
 static final class Info {final int width,height,tileW,tileH,count,interval;Info(int w,int h,int tw,int th,int c,int i){width=w;height=h;tileW=tw;tileH=th;count=c;interval=i;}}
 /** 选最接近 targetWidth（默认 320）的一档；没有 Trickplay 返回 null。 */
 static Info pick(JSONObject item,String mediaSourceId,int targetWidth){
  JSONObject all=item==null?null:item.optJSONObject("Trickplay");if(all==null||all.length()==0)return null;
  JSONObject bySize=mediaSourceId!=null&&!mediaSourceId.isEmpty()?all.optJSONObject(mediaSourceId):null;if(bySize==null){Iterator<String> it=all.keys();if(it.hasNext())bySize=all.optJSONObject(it.next());}if(bySize==null)return null;
  Info best=null;for(Iterator<String> it=bySize.keys();it.hasNext();){JSONObject o=bySize.optJSONObject(it.next());if(o==null)continue;Info i=new Info(o.optInt("Width"),o.optInt("Height"),Math.max(1,o.optInt("TileWidth",1)),Math.max(1,o.optInt("TileHeight",1)),o.optInt("ThumbnailCount"),o.optInt("Interval"));if(i.width<=0||i.height<=0||i.count<=0||i.interval<=0)continue;if(best==null||Math.abs(i.width-targetWidth)<Math.abs(best.width-targetWidth))best=i;}
  return best;}
 /** {大图序号, 左上角 x, 左上角 y}；超出范围时夹到最后一张。 */
 static int[] locate(Info i,long ms){int n=(int)Math.max(0,Math.min(i.count-1,ms/Math.max(1,i.interval)));int per=i.tileW*i.tileH;int tile=n/per,inTile=n%per;return new int[]{tile,(inTile%i.tileW)*i.width,(inTile/i.tileW)*i.height};}
 static String path(String itemId,String mediaSourceId,Info i,int tile){String q="/Videos/"+EmbyApi.enc(itemId)+"/Trickplay/"+i.width+"/"+tile+".jpg";if(mediaSourceId!=null&&!mediaSourceId.isEmpty())q+="?MediaSourceId="+EmbyApi.enc(mediaSourceId);return q;}
}
