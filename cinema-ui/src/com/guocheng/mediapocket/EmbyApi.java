package com.guocheng.mediapocket;
import org.json.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;
/**
 * Emby HTTP 约定：鉴权头、地址、ticks、PlaybackInfo 选流。不访问网络，便于单测。
 * 协议用 Emby 的 MediaBrowser 头和 /Users/AuthenticateByName；Jellyfin 也认这套，不另做分支。
 */
final class EmbyApi {
 static final String VERSION="0.1.72";
 static final String CLIENT="MediaPocket";
 /** 官方客户端名。只在服务器拒绝当前名称时再试这一次，不写死 Hills 等第三方名字。 */
 static final String OFFICIAL_CLIENT="Emby";
 /** explain 认这个前缀，后面是服务器原文。不要把密码或令牌拼进来。 */
 static final String CLIENT_ERROR="EMBY_CLIENT ";
 /** 1 毫秒 = 10_000 个 100 纳秒 tick。 */
 static final long TICKS_PER_MS=10000L;
 static final long PLAYED_TAIL_TICKS=30L*10000000L;
 private EmbyApi(){}
 static final class Choice {
  final String url;final boolean direct;final String mediaSourceId,playSessionId;final JSONArray subtitles,audio;
  Choice(String url,boolean direct,String mediaSourceId,String playSessionId,JSONArray subtitles,JSONArray audio){this.url=url;this.direct=direct;this.mediaSourceId=mediaSourceId==null?"":mediaSourceId;this.playSessionId=playSessionId==null?"":playSessionId;this.subtitles=subtitles==null?new JSONArray():subtitles;this.audio=audio==null?new JSONArray():audio;}
 }
 /** 一次登录尝试的结论。retry 为真时只用 client 再请求一次。 */
 static final class AuthAttempt {
  final String client;final boolean retry;final String error;
  AuthAttempt(String client,boolean retry,String error){this.client=client==null?CLIENT:client;this.retry=retry;this.error=error;}
 }
 /** 登录前不含 Token。值里的引号和换行去掉，避免把请求头拆开。 */
 static String authorization(String device,String deviceId,String version,String token){return authorization(CLIENT,device,deviceId,version,token);}
 static String authorization(String client,String device,String deviceId,String version,String token){
  String name=clean(client);if(name.isEmpty())name=CLIENT;
  StringBuilder h=new StringBuilder("MediaBrowser Client=\"").append(name).append("\", Device=\"").append(clean(device)).append("\", DeviceId=\"").append(clean(deviceId)).append("\", Version=\"").append(clean(version==null||version.isEmpty()?VERSION:version)).append("\"");
  if(token!=null&&!token.trim().isEmpty())h.append(", Token=\"").append(token.trim().replace("\"","").replace("\r","").replace("\n","")).append("\"");
  return h.toString();
 }
 /** 已保存的服务器沿用上次被接受的名称；没有则仍是 MediaPocket。 */
 static String clientOf(JSONObject server){String name=server==null?"":clean(server.optString("client"));return name.isEmpty()?CLIENT:name;}
 /**
  * 明文 403，且正文在说客户端不被允许。401 是密码错误。JSON 错误体不是这种拒绝，不因此换名称。
  */
 static boolean clientRejected(int code,String body){
  if(code!=403)return false;
  String text=body==null?"":body.trim();
  if(text.isEmpty()||text.startsWith("{")||text.startsWith("["))return false;
  return text.contains("客户端")||text.toLowerCase(Locale.ROOT).contains("client");
 }
 /** 已经是官方名称就返回空，调用方不得再重试。 */
 static String nextClient(String current){String name=clean(current);if(name.isEmpty())name=CLIENT;return OFFICIAL_CLIENT.equals(name)?"":OFFICIAL_CLIENT;}
 /**
  * 登录结果。200 保留当前名称；401 不重试；只有客户端拒绝且还没用官方名称时，才换 Emby 再试一次。
  * alreadyRetried 为真时不再给第二次机会。error 不含密码和令牌。
  */
 static AuthAttempt authAttempt(String client,int code,String body,boolean alreadyRetried){
  String name=clean(client);if(name.isEmpty())name=CLIENT;
  if(code>=200&&code<300)return new AuthAttempt(name,false,null);
  if(code==401)return new AuthAttempt(name,false,"HTTP 401");
  if(!alreadyRetried&&clientRejected(code,body)){String next=nextClient(name);if(!next.isEmpty())return new AuthAttempt(next,true,null);}
  if(clientRejected(code,body))return new AuthAttempt(name,false,CLIENT_ERROR+(body==null?"":body));
  return new AuthAttempt(name,false,"HTTP "+code);
 }
 /** 给界面的客户端拒绝文案。正文不可读时用固定短句，并去掉疑似令牌。 */
 static String clientNotice(String raw){
  String text=raw==null?"":raw.replace('\r',' ').replace('\n',' ').trim();
  text=text.replaceAll("(?i)access[_ ]?token[=:]\\S+","").replaceAll("\\b[0-9a-fA-F]{20,}\\b","…").trim();
  boolean named=text.contains("客户端")||text.toLowerCase(Locale.ROOT).contains("client");
  if(text.isEmpty()||!named||text.length()>80||text.indexOf('<')>=0||text.indexOf('{')>=0)return "服务器不允许这个客户端";
  return text;
 }
 /** 直链不带令牌头。改用官方名称的服务器要在地址上标明客户端，否则取流同样被拒绝。 */
 static String withClient(String base,String url,String client){
  if(url==null||url.isEmpty())return url==null?"":url;
  if(!OFFICIAL_CLIENT.equals(clean(client)))return url;
  if(!sameServer(base,url))return url;
  if(url.toLowerCase(Locale.ROOT).contains("x-emby-client="))return url;
  return url+(url.contains("?")?"&":"?")+"X-Emby-Client="+enc(OFFICIAL_CLIENT);
 }
 static String clean(String v){return v==null?"":v.replace("\"","").replace("\r","").replace("\n","").replace(",","").trim();}
 /** 保留 http/https、端口和路径前缀，去掉末尾斜杠、账号和片段。没写端口时 https 用 443、http 用 80。 */
 static String server(String raw){
  if(raw==null||raw.trim().isEmpty())throw new IllegalArgumentException("请填写服务器地址");
  String s=raw.trim();while(s.endsWith("/"))s=s.substring(0,s.length()-1);
  URI u;try{u=new URI(s);}catch(Exception e){throw new IllegalArgumentException("服务器地址无效，请填写以 http:// 或 https:// 开头的地址（可带端口）");}
  String scheme=u.getScheme();
  if(scheme==null||!(scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https")))throw new IllegalArgumentException("服务器地址需要以 http:// 或 https:// 开头");
  if(u.getHost()==null||u.getHost().isEmpty())throw new IllegalArgumentException("服务器地址缺少主机名");
  if(u.getUserInfo()!=null)throw new IllegalArgumentException("请不要把账号写在地址里，填写下方的用户名和密码");
  String path=u.getRawPath()==null?"":u.getRawPath();while(path.endsWith("/")&&path.length()>1)path=path.substring(0,path.length()-1);if("/".equals(path))path="";
  try{return new URI(scheme.toLowerCase(Locale.ROOT),null,u.getHost(),u.getPort(),path.isEmpty()?null:path,null,null).toASCIIString();}
  catch(Exception e){throw new IllegalArgumentException("服务器地址无效，请检查端口和主机名");}
 }
 static String join(String base,String path){if(path==null||path.isEmpty())return base;if(path.startsWith("http://")||path.startsWith("https://"))return path;return base+(path.startsWith("/")?path:"/"+path);}
 static String authenticateUrl(String base){return join(server(base),"/Users/AuthenticateByName");}
 static JSONObject authenticateBody(String username,String password)throws JSONException{String pw=password==null?"":password;return new JSONObject().put("Username",username==null?"":username).put("Pw",pw).put("Password",pw);}
 /** 新服务器允许空密码，原样放进 Pw。编辑时输入为空则沿用已保存的密码。 */
 static String loginPassword(boolean editing,String typed,String saved){String pw=typed==null?"":typed;if(editing&&pw.isEmpty())return saved==null?"":saved;return pw;}
 static String passwordHint(boolean editing){return editing?"密码（留空则不修改）":"密码（没有可留空）";}
 static String serverHint(){return "服务器地址，含 http:// 或 https://，端口可省略";}
 static String enc(String s){try{return URLEncoder.encode(s==null?"":s,"UTF-8");}catch(Exception e){return "";}}
 static String withKey(String url,String token){if(url==null||url.isEmpty()||token==null||token.isEmpty())return url==null?"":url;if(url.toLowerCase(Locale.ROOT).contains("api_key="))return url;return url+(url.contains("?")?"&":"?")+"api_key="+enc(token);}
 /** 地址指向 Emby 服务器本身吗？外部主机（.strm 远程地址、网盘直链）不能带 api_key，否则令牌会发给别人。 */
 static boolean sameServer(String base,String url){try{URI b=new URI(server(base)),u=new URI(url);if(u.getHost()==null)return true;return u.getHost().equalsIgnoreCase(b.getHost())&&port(u)==port(b);}catch(Exception e){return false;}}
 static int port(URI u){if(u.getPort()>0)return u.getPort();return "https".equalsIgnoreCase(u.getScheme())?443:80;}
 /** 只给本服务器地址加 api_key。 */
 static String keyed(String base,String url,String token){return sameServer(base,url)?withKey(url,token):url;}
 /** 宽高向上取整到 100 的倍数，同一张图更容易命中缓存。0 或负数按 100。 */
 static int roundUp100(int px){if(px<=0)return 100;int m=px%100;return m==0?px:px+(100-m);}
 static String imageUrl(String base,String itemId,String kind,String token,int maxWidth,String tag){return imageUrl(base,itemId,kind,token,maxWidth,0,tag);}
 static String imageUrl(String base,String itemId,String kind,String token,int maxWidth,int maxHeight,String tag){
  String k=kind==null||kind.isEmpty()?"Primary":kind;
  String q="/Items/"+enc(itemId)+"/Images/"+enc(k)+"?maxWidth="+roundUp100(Math.max(100,maxWidth))+"&quality=90";
  if(maxHeight>0)q+="&maxHeight="+roundUp100(maxHeight);
  if(tag!=null&&!tag.isEmpty())q+="&tag="+enc(tag);
  return withKey(join(server(base),q),token);
 }
 /** 缺图时换条目或换类型。Primary → SeriesPrimary → PrimaryImageItemId；Backdrop → ParentBackdrop；Logo → ParentLogo。 */
 static final class ImageRef {final String itemId,kind,tag;ImageRef(String itemId,String kind,String tag){this.itemId=itemId==null?"":itemId;this.kind=kind==null?"Primary":kind;this.tag=tag==null?"":tag;}}
 static ImageRef fallbackImage(JSONObject item,String kind){
  String id=item==null?"":item.optString("Id");JSONObject tags=item==null?null:item.optJSONObject("ImageTags");String want=kind==null||kind.isEmpty()?"Primary":kind;
  if("Primary".equals(want)){String tag=tags==null?"":tags.optString("Primary");if(!tag.isEmpty())return new ImageRef(id,"Primary",tag);String series=tags==null?"":tags.optString("SeriesPrimary");if(!series.isEmpty())return new ImageRef(id,"SeriesPrimary",series);String pid=item==null?"":item.optString("PrimaryImageItemId");if(!pid.isEmpty())return new ImageRef(pid,"Primary",item.optString("PrimaryImageTag"));return new ImageRef(id,"Primary","");}
  if("Backdrop".equals(want)){String tag=backdropTag(item);if(!tag.isEmpty())return new ImageRef(id,"Backdrop",tag);String parent=item==null?"":item.optString("ParentBackdropItemId");if(!parent.isEmpty())return new ImageRef(parent,"Backdrop",item.optString("ParentBackdropImageTag"));return new ImageRef(id,"Backdrop","");}
  if("Logo".equals(want)){String tag=tags==null?"":tags.optString("Logo");if(!tag.isEmpty())return new ImageRef(id,"Logo",tag);String parent=item==null?"":item.optString("ParentLogoItemId");if(!parent.isEmpty())return new ImageRef(parent,"Logo",item.optString("ParentLogoImageTag"));return new ImageRef(id,"Logo","");}
  return new ImageRef(id,want,tags==null?"":tags.optString(want));
 }
 static String thumbnailSetPath(String itemId,String mediaSourceId,int width){String q="/Items/"+enc(itemId)+"/ThumbnailSet?Width="+roundUp100(Math.max(100,width));if(mediaSourceId!=null&&!mediaSourceId.isEmpty())q+="&MediaSourceId="+enc(mediaSourceId);return q;}
 static long[] thumbnailTicks(JSONArray items){if(items==null||items.length()==0)return new long[0];long[] raw=new long[items.length()];int n=0;for(int i=0;i<items.length();i++){JSONObject o=items.optJSONObject(i);if(o==null||!o.has("PositionTicks"))continue;raw[n++]=o.optLong("PositionTicks");}if(n==raw.length)return raw;long[] out=new long[n];System.arraycopy(raw,0,out,0,n);return out;}
 static String[] thumbnailTags(JSONArray items){if(items==null||items.length()==0)return new String[0];ArrayList<String> list=new ArrayList<>();for(int i=0;i<items.length();i++){JSONObject o=items.optJSONObject(i);if(o==null||!o.has("PositionTicks"))continue;list.add(o.optString("ImageTag"));}return list.toArray(new String[0]);}
 /** 二分查找不超过目标时刻的最后一张。没有则 -1。调用方应传入按时间升序的刻度。 */
 static int thumbnailIndex(long[] ticks,long target){if(ticks==null||ticks.length==0)return -1;int lo=0,hi=ticks.length-1,ans=-1;while(lo<=hi){int mid=(lo+hi)>>>1;if(ticks[mid]<=target){ans=mid;lo=mid+1;}else hi=mid-1;}return ans;}
 static String thumbnailImageUrl(String base,String itemId,String token,long positionTicks,String mediaSourceId,String tag){
  String q="/Items/"+enc(itemId)+"/Images/Thumbnail?quality=90&PositionTicks="+Math.max(0L,positionTicks);
  if(mediaSourceId!=null&&!mediaSourceId.isEmpty())q+="&MediaSourceId="+enc(mediaSourceId);
  if(tag!=null&&!tag.isEmpty())q+="&tag="+enc(tag);
  return withKey(join(server(base),q),token);
 }
 /** 进度上报去重键：会话、是否暂停、位置刻度。三者都相同就不再发。 */
 static String progressKey(String session,boolean paused,long positionTicks){return (session==null?"":session)+"|"+paused+"|"+Math.max(0L,positionTicks);}
 static boolean sameProgress(String previous,String next){return next!=null&&next.equals(previous);}
 static String hideResumePath(String userId,String itemId){return hideResumePath(userId,itemId,true);}
 static String hideResumePath(String userId,String itemId,boolean hide){return "/Users/"+enc(userId)+"/Items/"+enc(itemId)+"/HideFromResume?Hide="+(hide?"true":"false");}
 static String chapterImage(String base,String itemId,int index,String token,String tag){
  String q="/Items/"+enc(itemId)+"/Images/Chapter/"+Math.max(0,index)+"?maxWidth=320&quality=80";
  if(tag!=null&&!tag.isEmpty())q+="&tag="+enc(tag);
  return withKey(join(server(base),q),token);
 }
 static String bifPath(String itemId,String mediaSourceId){String q="/Videos/"+enc(itemId)+"/index.bif?width=320";if(mediaSourceId!=null&&!mediaSourceId.isEmpty())q+="&mediaSourceId="+enc(mediaSourceId);return q;}
 static long msToTicks(long ms){if(ms<=0)return 0L;if(ms>Long.MAX_VALUE/TICKS_PER_MS)return Long.MAX_VALUE;return ms*TICKS_PER_MS;}
 static long ticksToMs(long ticks){if(ticks<=0)return 0L;return ticks/TICKS_PER_MS;}
 static boolean played(long positionTicks,long runtimeTicks){if(positionTicks<=0||runtimeTicks<=0)return false;if(runtimeTicks-positionTicks<=PLAYED_TAIL_TICKS)return true;return positionTicks*100L>=runtimeTicks*90L;}
 /**
  * 0.1.106 转码兜底地址：让服务器转成 H.264 + AAC 的 HLS（手机一定能解）。直放失败、自动换源时作为第二个片源。
  * 码率上限 20 Mbps、最长边 1920；字幕不烧进画面（字幕仍走外挂）。需要服务器给这个用户开了「允许转码」。
  */
 static String transcodeUrl(String base,String itemId,String mediaSourceId,String playSessionId,String token,String deviceId){
  StringBuilder q=new StringBuilder("/Videos/").append(enc(itemId)).append("/master.m3u8?VideoCodec=h264&AudioCodec=aac&MaxStreamingBitrate=20000000&VideoBitrate=18000000&AudioBitrate=192000&MaxWidth=1920&TranscodingMaxAudioChannels=2&SegmentContainer=ts&MinSegments=1&BreakOnNonKeyFrames=True&SubtitleMethod=External");
  if(mediaSourceId!=null&&!mediaSourceId.isEmpty())q.append("&MediaSourceId=").append(enc(mediaSourceId));
  q.append("&PlaySessionId=").append(enc(playSessionId==null||playSessionId.isEmpty()?"suiying-"+System.currentTimeMillis():playSessionId+"-tc"));
  if(deviceId!=null&&!deviceId.isEmpty())q.append("&DeviceId=").append(enc(deviceId));
  return withKey(join(server(base),q.toString()),token);
 }
 /** 在直放片源后面接一个转码片源（同一集、同一进度）；已经是转码的就不加。 */
 static JSONArray withTranscode(JSONObject stream,String base,String token,String deviceId)throws JSONException{
  JSONArray out=new JSONArray().put(stream);if(!stream.optBoolean("emby.direct",false))return out;
  JSONObject t=new JSONObject(stream.toString());t.put("url",transcodeUrl(base,stream.optString("emby.item"),stream.optString("emby.mediaSource"),stream.optString("emby.playSession"),token,deviceId));
  t.put("title","Emby 服务器转码（兼容模式）").put("emby.direct",false).put("emby.transcodeFallback",true);out.put(t);return out;
 }
 static String directUrl(String base,String itemId,String mediaSourceId,String playSessionId,String token){
  StringBuilder q=new StringBuilder("/Videos/").append(enc(itemId)).append("/stream?static=true");
  if(mediaSourceId!=null&&!mediaSourceId.isEmpty())q.append("&MediaSourceId=").append(enc(mediaSourceId));
  if(playSessionId!=null&&!playSessionId.isEmpty())q.append("&PlaySessionId=").append(enc(playSessionId));
  return withKey(join(server(base),q.toString()),token);
 }
 static boolean staticUrl(String url){return url!=null&&url.toLowerCase(Locale.ROOT).contains("static=true");}
 /** 优先 static=true 直连；没有可直连的源时用服务器给的转码地址。mediaSourceId 非空时只使用这一版。 */
 static Choice select(String base,String itemId,String token,JSONObject info)throws JSONException{return select(base,itemId,token,info,null);}
 static Choice select(String base,String itemId,String token,JSONObject info,String mediaSourceId)throws JSONException{return select(base,itemId,token,info,mediaSourceId,CLIENT);}
 static Choice select(String base,String itemId,String token,JSONObject info,String mediaSourceId,String client)throws JSONException{
  if(info==null)throw new IllegalArgumentException("服务器没有返回播放信息");
  String play=info.optString("PlaySessionId");JSONArray sources=info.optJSONArray("MediaSources");
  if(sources==null||sources.length()==0)throw new IllegalArgumentException("服务器没有返回可播放的媒体");
  JSONObject directSource=null,transSource=null;String direct=null,trans=null;
  for(int i=0;i<sources.length();i++){JSONObject s=sources.optJSONObject(i);if(s==null)continue;if(mediaSourceId!=null&&!mediaSourceId.isEmpty()&&!mediaSourceId.equals(s.optString("Id")))continue;
   String explicit=explicitStatic(base,s,token,play);
   boolean can=explicit!=null||s.optBoolean("SupportsDirectStream")||s.optBoolean("SupportsDirectPlay");
   if(directSource==null&&can){directSource=s;direct=explicit!=null?explicit:directUrl(base,itemId,s.optString("Id"),play,token);}
   String t=s.optString("TranscodingUrl");
   if(transSource==null&&!t.isEmpty()){transSource=s;trans=keyed(base,join(server(base),t),token);}
  }
  JSONObject chosen;String url;boolean isDirect;
  if(direct!=null){chosen=directSource;url=direct;isDirect=true;}
  else if(trans!=null){chosen=transSource;url=trans;isDirect=false;}
  else throw new IllegalArgumentException("没有可播放的视频流");
  JSONArray subs=subtitles(base,itemId,token,play,chosen);
  for(int i=0;i<subs.length();i++){JSONObject sub=subs.optJSONObject(i);if(sub!=null)sub.put("url",withClient(base,sub.optString("url"),client));}
  return new Choice(withClient(base,url,client),isDirect,chosen.optString("Id"),play,subs,audio(chosen));
 }
 static String explicitStatic(String base,JSONObject source,String token,String playSessionId){
  String raw=source.optString("DirectStreamUrl");
  if(raw.isEmpty()){String path=source.optString("Path");if(path.startsWith("http://")||path.startsWith("https://"))raw=path;}
  if(raw.isEmpty())return null;
  String abs=join(server(base),raw);if(!sameServer(base,abs))return null;
  String url=withKey(abs,token);
  if(!staticUrl(url))url+=(url.contains("?")?"&":"?")+"static=true";
  if(playSessionId!=null&&!playSessionId.isEmpty()&&!url.toLowerCase(Locale.ROOT).contains("playsessionid="))url+="&PlaySessionId="+enc(playSessionId);
  return url;
 }
 static String subtitleExt(String codec){String c=codec==null?"":codec.toLowerCase(Locale.ROOT);if(c.contains("ass")||c.equals("ssa"))return "ass";if(c.contains("vtt"))return "vtt";return "srt";}
 static JSONArray subtitles(String base,String itemId,String token,String play,JSONObject source)throws JSONException{
  JSONArray out=new JSONArray(),streams=source.optJSONArray("MediaStreams");String media=source.optString("Id");
  for(int i=0;streams!=null&&i<streams.length();i++){JSONObject s=streams.optJSONObject(i);if(s==null||!"Subtitle".equalsIgnoreCase(s.optString("Type")))continue;
   int index=s.optInt("Index",-1);String delivery=s.optString("DeliveryUrl");
   if(delivery.isEmpty()&&index>=0&&!media.isEmpty())delivery="/Videos/"+enc(itemId)+"/"+enc(media)+"/Subtitles/"+index+"/Stream."+subtitleExt(s.optString("Codec"));
   if(delivery.isEmpty())continue;
   String abs=join(server(base),delivery);boolean own=sameServer(base,abs);
   String url=own?withKey(abs,token):abs;
   if(own&&play!=null&&!play.isEmpty()&&!url.toLowerCase(Locale.ROOT).contains("playsessionid="))url+=(url.contains("?")?"&":"?")+"PlaySessionId="+enc(play);
   boolean external=s.optBoolean("IsExternal")||"External".equalsIgnoreCase(s.optString("DeliveryMethod"));
   String name=s.optString("DisplayTitle",s.optString("Language","字幕"));if(name.isEmpty())name=external?"外挂字幕":"内嵌字幕";
   name=name+(external?" · 外挂":" · 内嵌");
   out.put(new JSONObject().put("url",url).put("lang",s.optString("Language")).put("name",name).put("external",external).put("index",index).put("codec",s.optString("Codec")).put("forced",s.optBoolean("IsForced")).put("isDefault",s.optBoolean("IsDefault")));
  }
  return out;
 }
 static JSONArray audio(JSONObject source)throws JSONException{
  JSONArray out=new JSONArray(),streams=source.optJSONArray("MediaStreams");
  for(int i=0;streams!=null&&i<streams.length();i++){JSONObject s=streams.optJSONObject(i);if(s==null||!"Audio".equalsIgnoreCase(s.optString("Type")))continue;
   String label=s.optString("DisplayTitle",s.optString("Language","音轨"));int ch=s.optInt("Channels",0);if(ch>0)label=label+" · "+ch+"声道";
   String codec=s.optString("Codec");if(!codec.isEmpty())label=label+" · "+codec.toUpperCase(Locale.ROOT);
   out.put(new JSONObject().put("id",s.optInt("Index",i)).put("index",s.optInt("Index",i)).put("label",label).put("language",s.optString("Language")).put("codec",codec).put("channels",ch).put("isDefault",s.optBoolean("IsDefault")).put("ordinal",out.length()));
  }
  return out;
 }
 static JSONObject playbackInfoBody(String userId,long startTicks)throws JSONException{return playbackInfoBody(userId,startTicks,null,-1,-1);}
 static JSONObject playbackInfoBody(String userId,long startTicks,String mediaSourceId,int audioIndex,int subtitleIndex)throws JSONException{return playbackInfoBody(userId,startTicks,mediaSourceId,audioIndex,subtitleIndex,140000000L);}
 static JSONObject playbackInfoBody(String userId,long startTicks,String mediaSourceId,int audioIndex,int subtitleIndex,long maxBitrate)throws JSONException{
  long rate=maxBitrate>0?maxBitrate:140000000L;
  JSONArray containers=new JSONArray();
  for(String c:new String[]{"mkv","mp4","m4v","mov","avi","ts","m2ts","wmv","flv","webm","mpg","mpeg","3gp","iso"})containers.put(new JSONObject().put("Container",c).put("Type","Video").put("VideoCodec","h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1").put("AudioCodec","aac,mp3,ac3,eac3,dts,truehd,flac,opus,vorbis"));
  JSONObject trans=new JSONObject().put("Container","ts").put("Type","Video").put("VideoCodec","h264").put("AudioCodec","aac,mp3,ac3").put("Protocol","hls").put("Context","Streaming");
  JSONArray subs=new JSONArray();
  for(String f:new String[]{"srt","ass","ssa","vtt","subrip","pgs","pgssub","sub"}){subs.put(new JSONObject().put("Format",f).put("Method","External"));subs.put(new JSONObject().put("Format",f).put("Method","Embed"));}
  JSONObject profile=new JSONObject().put("MaxStreamingBitrate",rate).put("DirectPlayProfiles",containers).put("TranscodingProfiles",new JSONArray().put(trans)).put("SubtitleProfiles",subs);
  JSONObject body=new JSONObject().put("UserId",userId==null?"":userId).put("MaxStreamingBitrate",rate).put("StartTimeTicks",0L).put("AutoOpenLiveStream",true).put("DeviceProfile",profile);
  if(mediaSourceId!=null&&!mediaSourceId.isEmpty())body.put("MediaSourceId",mediaSourceId);
  if(audioIndex>=0)body.put("AudioStreamIndex",audioIndex);
  if(subtitleIndex>=0)body.put("SubtitleStreamIndex",subtitleIndex);
  return body;
 }
 static JSONObject sessionBody(String itemId,String mediaSourceId,String playSessionId,long positionTicks,boolean paused,boolean direct)throws JSONException{
  return new JSONObject().put("ItemId",itemId).put("MediaSourceId",mediaSourceId==null?"":mediaSourceId).put("PlaySessionId",playSessionId==null?"":playSessionId).put("PositionTicks",Math.max(0L,positionTicks)).put("IsPaused",paused).put("CanSeek",true).put("PlayMethod",direct?"DirectStream":"Transcode");
 }
 static String watchId(String serverId,String itemId){return "emby:"+serverId+":"+itemId;}
 static boolean isEmbyId(String id){return id!=null&&id.startsWith("emby:")&&id.split(":").length>=3;}
 static String itemOf(String watchId){if(!isEmbyId(watchId))return "";String[] p=watchId.split(":",3);return p.length<3?"":p[2];}
 static String serverOf(String watchId){if(!isEmbyId(watchId))return "";String[] p=watchId.split(":",3);return p.length<3?"":p[1];}
 static String runtimeLabel(long ticks){long min=ticksToMs(ticks)/60000L;if(min<=0)return "";long h=min/60,m=min%60;return h>0?(m>0?h+"h "+m+"m":h+"h"):min+" min";}
 static String rating(JSONObject item){if(item==null||!item.has("CommunityRating")||item.isNull("CommunityRating"))return "";double v=item.optDouble("CommunityRating",0);if(v<=0)return "";return String.format(Locale.ROOT,"%.1f",v);}
 static String primaryTag(JSONObject item){JSONObject tags=item==null?null:item.optJSONObject("ImageTags");return tags==null?"":tags.optString("Primary");}
 static String backdropTag(JSONObject item){if(item==null)return "";JSONArray tags=item.optJSONArray("BackdropImageTags");if(tags!=null&&tags.length()>0)return tags.optString(0);return "";}
 static JSONObject meta(JSONObject server,JSONObject item)throws JSONException{
  String base=server.optString("url"),token=server.optString("token"),sid=server.optString("id"),itemId=item.optString("Id");
  String type=item.optString("Type"),media="Episode".equals(type)||"Series".equals(type)||"Season".equals(type)?"series":"movie";
  ImageRef primary=fallbackImage(item,"Primary");String poster=imageUrl(base,primary.itemId.isEmpty()?itemId:primary.itemId,primary.kind,token,480,primary.tag);
  ImageRef backdrop=fallbackImage(item,"Backdrop");String background=backdrop.tag.isEmpty()?poster:imageUrl(base,backdrop.itemId.isEmpty()?itemId:backdrop.itemId,backdrop.kind,token,1280,backdrop.tag);
  ImageRef logoRef=fallbackImage(item,"Logo");String logo=logoRef.tag.isEmpty()?"":imageUrl(base,logoRef.itemId.isEmpty()?itemId:logoRef.itemId,logoRef.kind,token,640,logoRef.tag);
  JSONArray genres=new JSONArray();JSONArray raw=item.optJSONArray("Genres");for(int i=0;raw!=null&&i<raw.length()&&i<4;i++)if(!raw.optString(i).isEmpty())genres.put(raw.optString(i));
  String name=item.optString("Name","影片");
  JSONObject m=new JSONObject().put("id",watchId(sid,itemId)).put("type",media).put("name",name).put("poster",poster).put("background",background).put("logo",logo)
   .put("description",item.optString("Overview")).put("releaseInfo",item.optInt("ProductionYear",0)>0?String.valueOf(item.optInt("ProductionYear")):"").put("imdbRating",rating(item))
   .put("runtime",runtimeLabel(item.optLong("RunTimeTicks"))).put("genres",genres).put("emby.server",sid).put("emby.item",itemId).put("emby.type",type).put("emby.line",line(item));
  if("Episode".equals(type)){m.put("emby.series",item.optString("SeriesId"));if(!item.optString("SeriesName").isEmpty())m.put("name",item.optString("SeriesName"));}
  JSONObject user=item.optJSONObject("UserData");long resume=user==null?0:ticksToMs(user.optLong("PlaybackPositionTicks"));if(resume>0)m.put("emby.resumeMs",resume);m.put("emby.play",EmbyCue.playLabel(resume));
  return m;
 }
 static String line(JSONObject item){if(item==null)return "";if("Episode".equals(item.optString("Type"))){int s=item.optInt("ParentIndexNumber",0),e=item.optInt("IndexNumber",0);String ep=s>0&&e>0?"S"+s+"E"+e:"";String title=item.optString("Name");String series=item.optString("SeriesName");if(!ep.isEmpty()&&!title.isEmpty())return ep+" · "+title;if(!ep.isEmpty())return ep;if(!series.isEmpty())return series;}JSONObject user=item.optJSONObject("UserData");if(user!=null&&user.optLong("PlaybackPositionTicks")>0)return "续播";return "";}
 static JSONArray videos(JSONObject server,JSONArray episodes)throws JSONException{
  JSONArray out=new JSONArray();
  for(int i=0;episodes!=null&&i<episodes.length()&&i<3000;i++){JSONObject ep=episodes.optJSONObject(i);if(ep==null||ep.optString("Id").isEmpty())continue;
   out.put(new JSONObject().put("id",watchId(server.optString("id"),ep.optString("Id"))).put("title",ep.optString("Name")).put("season",ep.optInt("ParentIndexNumber",ep.optInt("SeasonNumber",0))).put("episode",ep.optInt("IndexNumber",0)).put("overview",ep.optString("Overview")).put("poster",imageUrl(server.optString("url"),ep.optString("Id"),"Primary",server.optString("token"),320,primaryTag(ep))));
  }
  return out;
 }
 static JSONObject stream(JSONObject server,String itemId,Choice choice,long resumeMs)throws JSONException{
  // 令牌只放在本服务器地址的 api_key 里。不放进请求头：本地代理跟随 302 时会把请求头一起带到网盘/CDN 主机。
  JSONObject headers=new JSONObject();
  JSONArray audio=new JSONArray();for(int i=0;i<choice.audio.length();i++){JSONObject a=choice.audio.optJSONObject(i);if(a!=null)audio.put(new JSONObject().put("id",a.optInt("id")).put("label",a.optString("label")));}
  return new JSONObject().put("url",choice.url).put("name","Emby").put("title",choice.direct?"Emby 直接串流":"Emby 转码").put("subtitles",choice.subtitles).put("emby.audio",choice.audio).put("emby.subs",choice.subtitles)
   .put("behaviorHints",new JSONObject().put("proxyHeaders",new JSONObject().put("request",headers)))
   .put("mp.tracks",new JSONObject().put("audio",audio).put("subtitles",new JSONArray()))
   .put("emby.server",server.optString("id")).put("emby.item",itemId).put("emby.mediaSource",choice.mediaSourceId).put("emby.playSession",choice.playSessionId).put("emby.direct",choice.direct).put("emby.resumeMs",Math.max(0L,resumeMs));
 }
 static boolean library(JSONObject view){if(view==null)return false;String t=view.optString("CollectionType");if(t.isEmpty())return true;return t.equals("movies")||t.equals("tvshows")||t.equals("homevideos")||t.equals("boxsets")||t.equals("folders")||t.equals("mixed");}
 static String explain(Throwable e){
  if(e==null)return "未知错误";
  if(e instanceof IllegalArgumentException&&e.getMessage()!=null&&!e.getMessage().isEmpty())return e.getMessage();
  Throwable c=e;while(c.getCause()!=null&&c.getCause()!=c)c=c.getCause();
  String m=c.getMessage()==null?"":c.getMessage();
  if(c instanceof SocketTimeoutException||e instanceof SocketTimeoutException)return "连接服务器超时，请稍后重试";
  if(c instanceof UnknownHostException||e instanceof UnknownHostException)return "找不到服务器，请检查地址是否正确";
  if(c instanceof ConnectException||e instanceof ConnectException)return "无法连接服务器，请检查地址、端口和网络";
  if(c instanceof javax.net.ssl.SSLException||e instanceof javax.net.ssl.SSLException)return "安全连接失败，请检查 https 证书或改用 http";
  if(m.startsWith("HTTP 401"))return "用户名或密码不正确";
  if(m.startsWith(CLIENT_ERROR))return clientNotice(m.substring(CLIENT_ERROR.length()));
  if(m.startsWith("HTTP 403"))return "服务器拒绝访问，请检查账号权限";
  if(m.startsWith("HTTP 404"))return "服务器没有这个接口，请确认地址指向 Emby";
  if(m.startsWith("HTTP "))return "服务器返回错误（"+m+"）";
  if(m.contains("账号")||m.contains("地址")||m.contains("播放"))return m;
  return "无法完成请求，请检查网络后重试";
 }
}
