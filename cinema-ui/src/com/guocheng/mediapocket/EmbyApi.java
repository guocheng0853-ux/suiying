package com.guocheng.mediapocket;
import org.json.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;
/**
 * Emby HTTP 约定：鉴权头、地址、ticks、PlaybackInfo 选流。不访问网络，便于单测。
 * 协议用 Emby 的 MediaBrowser 头和 /Users/AuthenticateByName；Jellyfin 也认这套，不另做分支。
 */
final class EmbyApi {
 static final String VERSION="0.1.72";
 static final String CLIENT="MediaPocket";
 /** 1 毫秒 = 10_000 个 100 纳秒 tick。 */
 static final long TICKS_PER_MS=10000L;
 static final long PLAYED_TAIL_TICKS=30L*10000000L;
 private EmbyApi(){}
 static final class Choice {
  final String url;final boolean direct;final String mediaSourceId,playSessionId;final JSONArray subtitles,audio;
  Choice(String url,boolean direct,String mediaSourceId,String playSessionId,JSONArray subtitles,JSONArray audio){this.url=url;this.direct=direct;this.mediaSourceId=mediaSourceId==null?"":mediaSourceId;this.playSessionId=playSessionId==null?"":playSessionId;this.subtitles=subtitles==null?new JSONArray():subtitles;this.audio=audio==null?new JSONArray():audio;}
 }
 /** 登录前不含 Token。值里的引号和换行去掉，避免把请求头拆开。 */
 static String authorization(String device,String deviceId,String version,String token){
  StringBuilder h=new StringBuilder("MediaBrowser Client=\"").append(clean(CLIENT)).append("\", Device=\"").append(clean(device)).append("\", DeviceId=\"").append(clean(deviceId)).append("\", Version=\"").append(clean(version==null||version.isEmpty()?VERSION:version)).append("\"");
  if(token!=null&&!token.trim().isEmpty())h.append(", Token=\"").append(token.trim().replace("\"","").replace("\r","").replace("\n","")).append("\"");
  return h.toString();
 }
 static String clean(String v){return v==null?"":v.replace("\"","").replace("\r","").replace("\n","").replace(",","").trim();}
 /** 保留 http/https、端口和路径前缀，去掉末尾斜杠、账号和片段。 */
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
 static String enc(String s){try{return URLEncoder.encode(s==null?"":s,"UTF-8");}catch(Exception e){return "";}}
 static String withKey(String url,String token){if(url==null||url.isEmpty()||token==null||token.isEmpty())return url==null?"":url;if(url.toLowerCase(Locale.ROOT).contains("api_key="))return url;return url+(url.contains("?")?"&":"?")+"api_key="+enc(token);}
 /** 地址指向 Emby 服务器本身吗？外部主机（.strm 远程地址、网盘直链）不能带 api_key，否则令牌会发给别人。 */
 static boolean sameServer(String base,String url){try{URI b=new URI(server(base)),u=new URI(url);if(u.getHost()==null)return true;return u.getHost().equalsIgnoreCase(b.getHost())&&port(u)==port(b);}catch(Exception e){return false;}}
 static int port(URI u){if(u.getPort()>0)return u.getPort();return "https".equalsIgnoreCase(u.getScheme())?443:80;}
 /** 只给本服务器地址加 api_key。 */
 static String keyed(String base,String url,String token){return sameServer(base,url)?withKey(url,token):url;}
 static String imageUrl(String base,String itemId,String kind,String token,int maxWidth,String tag){
  String k=kind==null||kind.isEmpty()?"Primary":kind;
  String q="/Items/"+enc(itemId)+"/Images/"+enc(k)+"?maxWidth="+Math.max(80,maxWidth)+"&quality=90";
  if(tag!=null&&!tag.isEmpty())q+="&tag="+enc(tag);
  return withKey(join(server(base),q),token);
 }
 static String chapterImage(String base,String itemId,int index,String token,String tag){
  String q="/Items/"+enc(itemId)+"/Images/Chapter/"+Math.max(0,index)+"?maxWidth=320&quality=80";
  if(tag!=null&&!tag.isEmpty())q+="&tag="+enc(tag);
  return withKey(join(server(base),q),token);
 }
 static String bifPath(String itemId,String mediaSourceId){String q="/Videos/"+enc(itemId)+"/index.bif?width=320";if(mediaSourceId!=null&&!mediaSourceId.isEmpty())q+="&mediaSourceId="+enc(mediaSourceId);return q;}
 static long msToTicks(long ms){if(ms<=0)return 0L;if(ms>Long.MAX_VALUE/TICKS_PER_MS)return Long.MAX_VALUE;return ms*TICKS_PER_MS;}
 static long ticksToMs(long ticks){if(ticks<=0)return 0L;return ticks/TICKS_PER_MS;}
 static boolean played(long positionTicks,long runtimeTicks){if(positionTicks<=0||runtimeTicks<=0)return false;if(runtimeTicks-positionTicks<=PLAYED_TAIL_TICKS)return true;return positionTicks*100L>=runtimeTicks*90L;}
 static String directUrl(String base,String itemId,String mediaSourceId,String playSessionId,String token){
  StringBuilder q=new StringBuilder("/Videos/").append(enc(itemId)).append("/stream?static=true");
  if(mediaSourceId!=null&&!mediaSourceId.isEmpty())q.append("&MediaSourceId=").append(enc(mediaSourceId));
  if(playSessionId!=null&&!playSessionId.isEmpty())q.append("&PlaySessionId=").append(enc(playSessionId));
  return withKey(join(server(base),q.toString()),token);
 }
 static boolean staticUrl(String url){return url!=null&&url.toLowerCase(Locale.ROOT).contains("static=true");}
 /** 优先 static=true 直连；没有可直连的源时用服务器给的转码地址。mediaSourceId 非空时只使用这一版。 */
 static Choice select(String base,String itemId,String token,JSONObject info)throws JSONException{return select(base,itemId,token,info,null);}
 static Choice select(String base,String itemId,String token,JSONObject info,String mediaSourceId)throws JSONException{
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
  return new Choice(url,isDirect,chosen.optString("Id"),play,subtitles(base,itemId,token,play,chosen),audio(chosen));
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
 static JSONObject playbackInfoBody(String userId,long startTicks,String mediaSourceId,int audioIndex,int subtitleIndex)throws JSONException{
  JSONArray containers=new JSONArray();
  for(String c:new String[]{"mkv","mp4","m4v","mov","avi","ts","m2ts","wmv","flv","webm","mpg","mpeg","3gp","iso"})containers.put(new JSONObject().put("Container",c).put("Type","Video").put("VideoCodec","h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1").put("AudioCodec","aac,mp3,ac3,eac3,dts,truehd,flac,opus,vorbis"));
  JSONObject trans=new JSONObject().put("Container","ts").put("Type","Video").put("VideoCodec","h264").put("AudioCodec","aac,mp3,ac3").put("Protocol","hls").put("Context","Streaming");
  JSONArray subs=new JSONArray();
  for(String f:new String[]{"srt","ass","ssa","vtt","subrip","pgs","pgssub","sub"}){subs.put(new JSONObject().put("Format",f).put("Method","External"));subs.put(new JSONObject().put("Format",f).put("Method","Embed"));}
  JSONObject profile=new JSONObject().put("MaxStreamingBitrate",140000000).put("DirectPlayProfiles",containers).put("TranscodingProfiles",new JSONArray().put(trans)).put("SubtitleProfiles",subs);
  JSONObject body=new JSONObject().put("UserId",userId==null?"":userId).put("MaxStreamingBitrate",140000000).put("StartTimeTicks",0L).put("AutoOpenLiveStream",true).put("DeviceProfile",profile);
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
  String poster=imageUrl(base,itemId,"Primary",token,480,primaryTag(item));
  String backdropTag=backdropTag(item);String background=backdropTag.isEmpty()?poster:imageUrl(base,itemId,"Backdrop",token,1280,backdropTag);
  JSONObject tags=item.optJSONObject("ImageTags");String logoTag=tags==null?"":tags.optString("Logo");String logo=logoTag.isEmpty()?"":imageUrl(base,itemId,"Logo",token,640,logoTag);
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
  if(m.startsWith("HTTP 403"))return "服务器拒绝访问，请检查账号权限";
  if(m.startsWith("HTTP 404"))return "服务器没有这个接口，请确认地址指向 Emby";
  if(m.startsWith("HTTP "))return "服务器返回错误（"+m+"）";
  if(m.contains("账号")||m.contains("地址")||m.contains("播放"))return m;
  return "无法完成请求，请检查网络后重试";
 }
}
