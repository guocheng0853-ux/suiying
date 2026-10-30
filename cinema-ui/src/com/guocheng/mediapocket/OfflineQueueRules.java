package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/**
 * 0.1.164 离线下载入队（不依赖 Android）：去重、跳过不支持的片源、批量保持顺序。
 * 队列数组的约定和 OfflineDownloads 一致：下标 0 是最新入队的，下载从数组末尾往前找排队项，所以先入队的先下。
 */
final class OfflineQueueRules {
 private OfflineQueueRules(){}
 static final String TRANSCODE="这个片源服务器只允许转码播放，无法下载原文件";
 static final String DONE="这一集已经下载完成";
 static final String QUEUED="这一集已在下载队列里";
 static final String PAUSED="这一集已暂停，可在「我的 → 离线下载」里继续";
 static final String IN_BATCH="本批已经包含这一集";
 static final String LOOPBACK="这个地址只在播放时临时有效，无法下载";

 /** 已有记录不能再入队时的中文原因。失败的可以重新下，返回 null。 */
 static String duplicate(String status){
  if(status==null)return null;
  switch(status){
   case "done":return DONE;
   case "queued":
   case "running":return QUEUED;
   case "paused":return PAUSED;
   default:return null;
  }
 }
 static String embyKey(String server,String item){return "emby\n"+nz(server)+"\n"+nz(item);}
 static String fusionKey(String config,String site,String flag,String value){return "fusion\n"+nz(config)+"\n"+nz(site)+"\n"+nz(flag)+"\n"+nz(value);}
 static String urlKey(String url){return "url\n"+nz(url);}
 /** 一条下载记录对应的片源。同一集不管链接是否换过，键都一样。 */
 static String keyOf(JSONObject rec){
  if(rec==null)return "";
  JSONObject src=rec.optJSONObject("src");
  if(src!=null){
   String kind=src.optString("kind");
   if("emby".equals(kind))return embyKey(src.optString("server"),src.optString("item"));
   if("fusion".equals(kind))return fusionKey(src.optString("config"),src.optString("site"),src.optString("flag"),src.optString("value"));
   if("url".equals(kind))return urlKey(src.optString("url"));
  }
  String url=rec.optString("url");
  return url.isEmpty()?"":urlKey(url);
 }
 /** 同一片源有多条记录时，留下更该拦住再次入队的状态。 */
 static Map<String,String> index(JSONArray all){
  Map<String,String> out=new LinkedHashMap<>();
  if(all==null)return out;
  for(int i=0;i<all.length();i++){
   JSONObject o=all.optJSONObject(i);if(o==null)continue;
   String key=keyOf(o);if(key.isEmpty())continue;
   String st=o.optString("status");
   String prev=out.get(key);
   if(prev==null||rank(st)>rank(prev))out.put(key,st);
  }
  return out;
 }
 static int rank(String status){if("done".equals(status))return 4;if("running".equals(status))return 3;if("queued".equals(status)||"paused".equals(status))return 2;return 0;}

 static final class Item {
  final String key,name,url;final boolean transcodeOnly;
  Item(String key,String name,String url,boolean transcodeOnly){this.key=key==null?"":key;this.name=name==null?"":name;this.url=url;this.transcodeOnly=transcodeOnly;}
 }
 static final class Row {
  final String key,name,reason;final boolean enqueue;
  Row(String key,String name,boolean enqueue,String reason){this.key=key;this.name=name;this.enqueue=enqueue;this.reason=reason;}
 }
 /**
  * 按传入顺序决定入队或跳过。url 为空、或还不是 http(s) 地址时，先不因为地址拒绝（等解析）。
  * 已经是磁力、m3u8、DASH 的地址直接跳过。本批里同一个片源只留第一次能下的。
  */
 static List<Row> plan(List<Item> items,Map<String,String> existing){
  ArrayList<Row> out=new ArrayList<>();
  HashSet<String> taken=new HashSet<>();
  if(existing==null)existing=Collections.emptyMap();
  if(items==null)return out;
  for(Item it:items){
   if(it==null)continue;
   if(it.key.isEmpty()){out.add(new Row("",it.name,false,"没有可识别的片源"));continue;}
   String dup=duplicate(existing.get(it.key));
   if(dup!=null){out.add(new Row(it.key,it.name,false,dup));continue;}
   if(it.transcodeOnly){out.add(new Row(it.key,it.name,false,TRANSCODE));continue;}
   String early=earlyRefuse(it.url);
   if(early!=null){out.add(new Row(it.key,it.name,false,early));continue;}
   if(!taken.add(it.key)){out.add(new Row(it.key,it.name,false,IN_BATCH));continue;}
   out.add(new Row(it.key,it.name,true,null));
  }
  return out;
 }
 /** 选集里的原始地址：磁力 / 已是 m3u8、DASH 的 http 直接拒绝；站点内部编号还要解析，返回 null。 */
 static String earlyRefuse(String raw){
  if(raw==null)return null;
  String trim=raw.trim();if(trim.isEmpty())return null;
  if(MagnetLinks.isMagnet(trim)||trim.regionMatches(true,0,"magnet:",0,7))return OfflineRules.refuse(null);
  String low=trim.toLowerCase(Locale.ROOT);
  if(low.startsWith("http://")||low.startsWith("https://"))return OfflineRules.refuse(trim);
  return null;
 }
 /** 播放页此刻能不能下。refetch 为真表示还能向 Emby / 影视仓另取原文件，临时回环地址不必挡住。 */
 static String playerBlock(String url,boolean infoHash,boolean emby,boolean knowDirect,boolean direct,boolean refetch){
  if(infoHash)return OfflineRules.refuse(null);
  if(emby&&knowDirect&&!direct)return TRANSCODE;
  boolean empty=url==null||url.trim().isEmpty();
  if(empty)return refetch?null:OfflineRules.refuse(null);
  if(loopback(url))return refetch?null:LOOPBACK;
  return OfflineRules.refuse(url);
 }
 static boolean loopback(String url){
  if(url==null)return false;
  String low=url.trim().toLowerCase(Locale.ROOT);
  return low.startsWith("http://127.0.0.1")||low.startsWith("http://localhost")||low.startsWith("https://127.0.0.1")||low.startsWith("https://localhost");
 }
 /** 新的一批插到数组头部，下载顺序仍是：已有排队、再本批第 1 集、第 2 集…… */
 static JSONArray prependFifo(JSONArray existing,List<JSONObject> batchInOrder)throws JSONException{
  JSONArray next=new JSONArray();
  if(batchInOrder!=null)for(int i=batchInOrder.size()-1;i>=0;i--){JSONObject rec=batchInOrder.get(i);if(rec!=null)next.put(rec);}
  if(existing!=null)for(int i=0;i<existing.length();i++)next.put(existing.opt(i));
  return next;
 }
 /** 按数组约定，排队项谁先下。 */
 static List<String> downloadOrder(JSONArray all){
  ArrayList<String> out=new ArrayList<>();
  if(all==null)return out;
  for(int i=all.length()-1;i>=0;i--){JSONObject o=all.optJSONObject(i);if(o!=null&&"queued".equals(o.optString("status")))out.add(o.optString("name"));}
  return out;
 }
 static String summary(int enqueued,List<String> skipReasons){
  LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();
  int skip=0;
  if(skipReasons!=null)for(String reason:skipReasons){skip++;String key=reason==null||reason.isEmpty()?"未加入":reason;Integer n=counts.get(key);counts.put(key,n==null?1:n+1);}
  StringBuilder b=new StringBuilder();
  b.append("已加入 ").append(Math.max(0,enqueued)).append(" 集");
  if(skip>0){b.append("，跳过 ").append(skip).append(" 集");for(Map.Entry<String,Integer> e:counts.entrySet())b.append('\n').append(e.getKey()).append(" · ").append(e.getValue()).append(" 集");}
  return b.toString();
 }
 /** 从 current 起（含）到结尾的下标。越界时收成最后一集；空列表得到空数组。 */
 static int[] tail(int current,int size){
  if(size<=0)return new int[0];
  int at=current<0?0:Math.min(current,size-1);
  int[] out=new int[size-at];
  for(int i=0;i<out.length;i++)out[i]=at+i;
  return out;
 }
 /** 这一季里「当前集」：指定 id、否则第一条看了一部分的、否则第一条没看完的、否则第一集。 */
 static int resumeIndex(String[] ids,boolean[] played,boolean[] started,String preferred){
  int n=ids==null?0:ids.length;
  if(n==0)return 0;
  if(preferred!=null&&!preferred.isEmpty())for(int i=0;i<n;i++)if(preferred.equals(ids[i]))return i;
  if(started!=null)for(int i=0;i<n&&i<started.length;i++)if(started[i])return i;
  if(played!=null)for(int i=0;i<n&&i<played.length;i++)if(!played[i])return i;
  return 0;
 }
 /** 有播放进度的最后一集；一集都没有进度时是第一集。 */
 static int lastTrue(boolean[] flags){int at=0;if(flags==null)return 0;for(int i=0;i<flags.length;i++)if(flags[i])at=i;return at;}
 static String nz(String s){return s==null?"":s;}
}
