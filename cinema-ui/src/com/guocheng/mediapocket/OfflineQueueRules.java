package com.guocheng.mediapocket;
import org.json.*;import java.net.URL;import java.util.*;
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
 /** 批量对话框里这一集能不能勾。已下完、排队、下载中、暂停，以及现在就能判断的不支持片源，返回原因；能勾返回 null。 */
 static String dialogBlock(String status,String earlyUrl,boolean transcodeOnly){
  String dup=duplicate(status);
  if(dup!=null)return dup;
  if(transcodeOnly)return TRANSCODE;
  return earlyRefuse(earlyUrl);
 }
 /** 一键范围：能下的下标留下，预先拦住的原因留给汇总。手动勾选只传用户选中的下标。 */
 static final class Scope {
  final int[] enqueue;final List<String> skips;
  Scope(int[] enqueue,List<String> skips){this.enqueue=enqueue;this.skips=skips;}
 }
 static Scope scope(String[] block,int[] indexes){
  ArrayList<Integer> keep=new ArrayList<>();ArrayList<String> skips=new ArrayList<>();
  if(indexes!=null)for(int i:indexes){if(selectable(block,i))keep.add(i);else if(block!=null&&i>=0&&i<block.length&&block[i]!=null)skips.add(block[i]);}
  int[] out=new int[keep.size()];for(int n=0;n<out.length;n++)out[n]=keep.get(n);return new Scope(out,skips);
 }
 /**
  * 解析返回之后：1 入队，0 跳过并继续，-1 这一集返回时已经取消。
  * 取消生效的那一集不能入队，调用方还要停掉后面还没解析的集。
  */
 static int accept(boolean cancelled,String skip){if(cancelled)return -1;return skip==null?1:0;}
 /** 全选和一键全部只用能勾的下标。 */
 static int[] allowed(String[] block,int[] indexes){
  if(indexes==null||indexes.length==0)return new int[0];
  int n=0;for(int i:indexes)if(selectable(block,i))n++;
  int[] out=new int[n];int w=0;for(int i:indexes)if(selectable(block,i))out[w++]=i;return out;
 }
 static boolean selectable(String[] block,int i){return block!=null&&i>=0&&i<block.length&&block[i]==null;}
 /**
  * 播放页不能弹通知权限：系统权限框会让播放器 onPause，SystemPlayerActivity 会暂停并上报。
  * 播放页先记下，回到非播放页再问。已问过或已授权不再问。
  */
 static boolean requestNotifyNow(boolean player){return !player;}
 static boolean deferNotify(boolean player,boolean asked,boolean granted){return player&&!asked&&!granted;}
 static boolean requestNotifyOnResume(boolean player,boolean deferred,boolean asked,boolean granted){return !player&&deferred&&!asked&&!granted;}
 /**
  * 给用户看的错误里去掉完整地址，只留主机名。磁力和解析不出主机的地址不留路径和参数。
  * 不用 \\b：汉字在 Unicode 词边界里也算词，紧贴在地址前面时会整段漏掉。
  */
 static String redact(String text){
  if(text==null||text.isEmpty())return text==null?"":text;
  StringBuilder out=new StringBuilder();int i=0;
  while(i<text.length()){int at=findProtocol(text,i);if(at<0){out.append(text.substring(i));break;}out.append(text,i,at);int end=scanUrl(text,at);out.append(hostOf(text.substring(at,end)));i=end;}
  return redactMagnets(out.toString());
 }
 static int findProtocol(String text,int from){for(int i=Math.max(0,from);i<text.length();i++){if(i>0&&asciiWord(text.charAt(i-1)))continue;if(region(text,i,"https://")||region(text,i,"http://")||region(text,i,"rtsp://")||region(text,i,"rtmp://"))return i;}return -1;}
 static int scanUrl(String text,int at){int end=at;while(end<text.length()&&!urlStop(text.charAt(end)))end++;while(end>at&&urlTrail(text.charAt(end-1)))end--;return end;}
 static String redactMagnets(String text){String key="magnet:?";StringBuilder out=new StringBuilder();int i=0;while(i<text.length()){int at=indexOfIgnore(text,key,i);if(at<0){out.append(text.substring(i));break;}if(at>0&&asciiWord(text.charAt(at-1))){out.append(text,i,at+1);i=at+1;continue;}out.append(text,i,at);int end=at+key.length();while(end<text.length()&&!urlStop(text.charAt(end)))end++;while(end>at&&urlTrail(text.charAt(end-1)))end--;out.append("磁力");i=end;}return out.toString();}
 static int indexOfIgnore(String text,String key,int from){for(int i=Math.max(0,from);i+key.length()<=text.length();i++)if(text.regionMatches(true,i,key,0,key.length()))return i;return -1;}
 static boolean region(String text,int i,String p){return i+p.length()<=text.length()&&text.regionMatches(true,i,p,0,p.length());}
 static boolean asciiWord(char c){return c>='0'&&c<='9'||c>='A'&&c<='Z'||c>='a'&&c<='z';}
 static boolean urlTrail(char c){return c=='.'||c==','||c==';'||c==':';}
 static boolean urlStop(char c){if(Character.isWhitespace(c))return true;switch(c){case '"':case '\'':case '<':case '>':case '(':case ')':case '[':case ']':case '{':case '}':case '（':case '）':case '【':case '】':case '「':case '」':case '，':case '。':case '、':case '；':case '：':return true;default:return false;}}
 static String hostOf(String url){try{String h=new URL(url).getHost();return h==null||h.isEmpty()?"服务器":h;}catch(Exception e){return "服务器";}}
 static String summary(int enqueued,List<String> skipReasons){
  LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();
  int skip=0;
  if(skipReasons!=null)for(String reason:skipReasons){skip++;String key=redact(reason==null||reason.isEmpty()?"未加入":reason);Integer n=counts.get(key);counts.put(key,n==null?1:n+1);}
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
 /**
  * 播放页下载记下的片源。正在播的 flag/value（stream 优先，其次 meta）优先于选集里按集去重后的第一条线路。
  * Emby 仍按条目。都没有影视仓线路时，退回当前地址。
  */
 static JSONObject downloadSource(JSONObject stream,JSONObject meta,String watch){
  try{if(stream!=null&&!stream.optString("emby.item").isEmpty())return new JSONObject().put("kind","emby").put("server",stream.optString("emby.server")).put("item",stream.optString("emby.item"));
   JSONObject playing=playingFusion(stream,meta);if(playing!=null)return playing;
   JSONObject video=fusionVideo(meta,watch);if(video==null||meta==null)return stream!=null&&!stream.optString("url").isEmpty()?urlSource(stream.optString("url")):null;
   return new JSONObject().put("kind","fusion").put("config",meta.optString("fusion.config")).put("site",meta.optString("fusion.site")).put("flag",video.optString("fusion.flag")).put("value",video.optString("fusion.value"));
  }catch(JSONException e){return null;}
 }
 /** flag 和 value 必须来自同一份，避免用第一条线路的 value 配上当前线路的 flag。 */
 static JSONObject playingFusion(JSONObject stream,JSONObject meta)throws JSONException{
  JSONObject line=lineOf(stream);if(line==null)line=lineOf(meta);if(line==null)return null;
  String config=line.optString("fusion.config"),site=line.optString("fusion.site");
  if(config.isEmpty()&&meta!=null)config=meta.optString("fusion.config");
  if(site.isEmpty()&&meta!=null)site=meta.optString("fusion.site");
  if(config.isEmpty()||site.isEmpty())return null;
  return new JSONObject().put("kind","fusion").put("config",config).put("site",site).put("flag",line.optString("fusion.flag")).put("value",line.optString("fusion.value"));
 }
 static JSONObject lineOf(JSONObject o){if(o==null||o.optString("fusion.flag").isEmpty()||o.optString("fusion.value").isEmpty())return null;return o;}
 static JSONObject fusionVideo(JSONObject meta,String watch){JSONArray videos=meta==null?null:meta.optJSONArray("videos");if(videos==null||watch==null||watch.isEmpty())return null;for(int i=0;i<videos.length();i++){JSONObject v=videos.optJSONObject(i);if(v!=null&&watch.equals(v.optString("id"))&&!v.optString("fusion.value").isEmpty())return v;}return null;}
 static JSONObject urlSource(String url){try{return new JSONObject().put("kind","url").put("url",url==null?"":url);}catch(JSONException e){return new JSONObject();}}
 /** 开播和换源时把真实线路写进 stream 和 meta。下载读这份，不读 episodeChoices 留下的第一条。 */
 static void rememberLine(JSONObject stream,JSONObject meta,String config,String site,String flag,String value){
  try{String f=flag==null?"":flag,v=value==null?"":value;if(f.isEmpty()||v.isEmpty())return;String c=config==null?"":config,s=site==null?"":site;
   if(stream!=null){if(!c.isEmpty())stream.put("fusion.config",c);if(!s.isEmpty())stream.put("fusion.site",s);stream.put("fusion.flag",f).put("fusion.value",v);}
   if(meta!=null){if(!c.isEmpty())meta.put("fusion.config",c);if(!s.isEmpty())meta.put("fusion.site",s);meta.put("fusion.flag",f).put("fusion.value",v);}
  }catch(JSONException ignored){}
 }
}
