package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/**
 * Emby 播放和首页的纯规则：章节标记、下一集去重、音轨对应、备用地址、版本、首页分区。
 * 服务器没给的数据一律当成没有，调用方不报错。
 */
final class EmbyCue {
 static final String ASK="ask",AUTO="auto",OFF="off";
 static final long BUTTON_MS=4000L,NEXT_COUNTDOWN_MS=10000L,NEXT_FALLBACK_MS=30000L;
 static final int INTRO_COUNTDOWN_S=5;
 private EmbyCue(){}
 /** 冷却或 5xx 时，缓存里这个库还有条目就包成调用方本来就认识的 Items；没有上次的内容才是空对象。不另建缓存。converted 过的卡片不能当原始条目。 */
 static JSONObject cachedLatest(JSONObject cached,String libId){JSONObject empty=new JSONObject();if(cached==null||cached.optBoolean("converted")||libId==null||libId.isEmpty())return empty;JSONArray libs=cached.optJSONArray("libs"),latest=cached.optJSONArray("latest");if(libs==null||latest==null)return empty;for(int i=0;i<libs.length()&&i<latest.length();i++){JSONObject lib=libs.optJSONObject(i);JSONArray items=latest.optJSONArray(i);if(lib==null||items==null||items.length()==0||!libId.equals(lib.optString("Id")))continue;try{return new JSONObject().put("Items",items);}catch(JSONException e){return empty;}}return empty;}
 static final class Span {final long introStart,introEnd,creditsStart;Span(long a,long b,long c){introStart=a;introEnd=b;creditsStart=c;}boolean any(){return introStart>=0||creditsStart>=0;}}
 static String skipMode(String raw){if(AUTO.equals(raw)||"自动".equals(raw))return AUTO;if(OFF.equals(raw)||"关闭".equals(raw))return OFF;return ASK;}
 static Span spans(JSONArray chapters){
  long intro=-1,end=-1,credits=-1;
  if(chapters!=null)for(int i=0;i<chapters.length();i++){JSONObject c=chapters.optJSONObject(i);if(c==null)continue;long ms=EmbyApi.ticksToMs(c.optLong("StartPositionTicks"));String t=c.optString("MarkerType");
   if("IntroStart".equalsIgnoreCase(t)&&intro<0)intro=ms;else if("IntroEnd".equalsIgnoreCase(t)&&end<0)end=ms;else if("CreditsStart".equalsIgnoreCase(t)&&credits<0)credits=ms;}
  return new Span(intro,end,credits);
 }
 /** 给章节条用的普通章节。片头片尾标记本身不占一条。 */
 static JSONArray chapters(JSONArray raw)throws JSONException{
  JSONArray out=new JSONArray();if(raw==null)return out;
  for(int i=0;i<raw.length();i++){JSONObject c=raw.optJSONObject(i);if(c==null)continue;String marker=c.optString("MarkerType");if(isMarker(marker))continue;String name=c.optString("Name");if(name.isEmpty()&&c.optString("ImageTag").isEmpty())continue;
   out.put(new JSONObject().put("index",i).put("name",name.isEmpty()?"章节 "+(out.length()+1):name).put("startMs",EmbyApi.ticksToMs(c.optLong("StartPositionTicks"))).put("tag",c.optString("ImageTag")));}
  return out;
 }
 static boolean isMarker(String marker){return "IntroStart".equalsIgnoreCase(marker)||"IntroEnd".equalsIgnoreCase(marker)||"CreditsStart".equalsIgnoreCase(marker);}
 /** 章节里已经有片头就用章节。没有才用这部剧本地记下的开始/结束。片尾标记保留。 */
 static Span prefer(Span chapters,long manualStart,long manualEnd){
  long credits=chapters==null?-1:chapters.creditsStart;
  if(chapters!=null&&chapters.introStart>=0&&chapters.introEnd>chapters.introStart)return chapters;
  if(manualStart>=0&&manualEnd>manualStart)return new Span(manualStart,manualEnd,credits);
  return chapters==null?new Span(-1,-1,credits):chapters;
 }
 static String seriesKey(JSONObject meta,String name){
  if(meta!=null){String series=meta.optString("emby.series");if(!series.isEmpty())return "emby:"+series;String title=meta.optString("fusion.title");if(!title.isEmpty())return "title:"+title;}
  String series=PlayerOverlayRules.seriesTitle(name,meta);return series==null||series.isEmpty()?"":"title:"+series;
 }
 /** 本地片头表。start 或 end 传 null 表示这一项保持原值。系列 id 为空时原样返回。 */
 static String writeIntro(String json,String seriesId,Long start,Long end){
  if(seriesId==null||seriesId.isEmpty())return json==null?"{}":json;
  JSONObject all;try{all=json==null||json.isEmpty()?new JSONObject():new JSONObject(json);}catch(Exception e){all=new JSONObject();}
  JSONObject row=all.optJSONObject(seriesId);if(row==null)row=new JSONObject();
  try{if(start!=null)row.put("start",start.longValue());if(end!=null)row.put("end",end.longValue());all.put(seriesId,row);}catch(Exception ignored){}
  return all.toString();
 }
 static String clearIntro(String json,String seriesId){
  if(seriesId==null||seriesId.isEmpty())return json==null?"{}":json;
  JSONObject all;try{all=json==null||json.isEmpty()?new JSONObject():new JSONObject(json);}catch(Exception e){return "{}";}
  all.remove(seriesId);return all.toString();
 }
 /** 没有记录时返回 {-1,-1}。 */
 static long[] readIntro(String json,String seriesId){
  if(json==null||seriesId==null||seriesId.isEmpty())return new long[]{-1,-1};
  try{JSONObject row=new JSONObject(json).optJSONObject(seriesId);if(row==null)return new long[]{-1,-1};return new long[]{row.has("start")?row.optLong("start",-1):-1,row.has("end")?row.optLong("end",-1):-1};}catch(Exception e){return new long[]{-1,-1};}
 }
 static int introLeft(long shownAtMs,long nowMs){if(shownAtMs<0)return INTRO_COUNTDOWN_S;int gone=(int)Math.max(0L,(nowMs-shownAtMs)/1000L);int left=INTRO_COUNTDOWN_S-gone;return left<0?0:left;}
 static String introCountdown(int seconds){return Math.max(0,seconds)+" 秒后跳过片头，点击取消";}
 /** 字幕同步，一步 100 毫秒，停在 ±5 秒。 */
 static int stepSubtitleOffset(int currentMs,int direction){int n=currentMs+(direction<0?-100:100);if(n<-5000)n=-5000;if(n>5000)n=5000;return n;}
 /** 当前进度若落在片头或片尾，返回要跳到的毫秒；否则 null。关闭模式永远不跳。 */
 static Long skipTo(Span span,long posMs,long durMs,String mode){
  if(span==null||OFF.equals(skipMode(mode))||posMs<0)return null;
  if(span.introStart>=0&&span.introEnd>span.introStart&&posMs>=span.introStart&&posMs<span.introEnd)return span.introEnd;
  if(span.creditsStart>=0&&durMs>span.creditsStart+1500&&posMs>=span.creditsStart&&posMs<durMs-1500)return durMs-800;
  return null;
 }
 static String skipText(Span span,long posMs,long durMs){
  if(span==null||posMs<0)return null;
  if(span.introStart>=0&&span.introEnd>span.introStart&&posMs>=span.introStart&&posMs<span.introEnd)return "跳过片头";
  if(span.creditsStart>=0&&durMs>span.creditsStart+1500&&posMs>=span.creditsStart&&posMs<durMs-1500)return "跳过片尾";
  return null;
 }
 /** 询问模式下按钮停留 BUTTON_MS；控件可见时一直留着。自动和关闭不显示按钮。 */
 static boolean showChapterMarks(boolean controlsShown,boolean seekShown,int duration,boolean hasMarkers){return controlsShown&&seekShown&&duration>0&&hasMarkers;}
 static boolean showSkipButton(String mode,String text,boolean controlsOn,long visibleMs){
  if(!ASK.equals(skipMode(mode))||text==null)return false;
  if(controlsOn)return true;
  return visibleMs>=0&&visibleMs<BUTTON_MS;
 }
 static boolean nextDue(boolean enabled,boolean hasNext,boolean cancelled,long posMs,long durMs,long creditsStartMs){
  if(!enabled||!hasNext||cancelled||posMs<0)return false;
  if(creditsStartMs>=0)return posMs>=creditsStartMs;
  if(durMs<=60000)return false;
  return durMs-posMs<=NEXT_FALLBACK_MS;
 }
 static int countdownSeconds(long elapsedMs){long left=NEXT_COUNTDOWN_MS-Math.max(0,elapsedMs);return left<=0?0:(int)((left+999)/1000);}
 /** 下一集行去掉已经出现在继续观看里的同一条目。 */
 static JSONArray withoutResume(JSONArray nextUp,JSONArray resume)throws JSONException{
  JSONArray out=new JSONArray();HashSet<String> seen=new HashSet<>();
  if(resume!=null)for(int i=0;i<resume.length();i++){JSONObject r=resume.optJSONObject(i);if(r!=null&&!r.optString("Id").isEmpty())seen.add(r.optString("Id"));}
  if(nextUp!=null)for(int i=0;i<nextUp.length();i++){JSONObject n=nextUp.optJSONObject(i);if(n==null||n.optString("Id").isEmpty()||seen.contains(n.optString("Id")))continue;out.put(n);}
  return out;
 }
 static String playLabel(long resumeMs){
  if(resumeMs<10000)return "播放";
  long s=resumeMs/1000L,h=s/3600,m=(s%3600)/60,sec=s%60;
  return h>0?String.format(Locale.ROOT,"继续 %d:%02d:%02d",h,m,sec):String.format(Locale.ROOT,"继续 %d:%02d",m,sec);
 }
 /** 轮播优先用有横版背景的继续观看，再补最新，最多 5 条，按 Id 去重。 */
 static JSONArray pickHeroes(JSONArray resume,JSONArray latest)throws JSONException{
  JSONArray out=new JSONArray();HashSet<String> seen=new HashSet<>();
  take(out,seen,resume,true);take(out,seen,latest,true);if(out.length()<3){take(out,seen,resume,false);take(out,seen,latest,false);}
  return out;
 }
 static void take(JSONArray out,HashSet<String> seen,JSONArray items,boolean needBackdrop)throws JSONException{
  if(items==null)return;
  for(int i=0;i<items.length()&&out.length()<5;i++){JSONObject item=items.optJSONObject(i);if(item==null||item.optString("Id").isEmpty()||seen.contains(item.optString("Id")))continue;if(needBackdrop&&EmbyApi.backdropTag(item).isEmpty())continue;seen.add(item.optString("Id"));out.put(item);}
 }
 /** 服务器首页分区。认不出任何一项时回到继续观看、下一集、各库最新。 */
 static String[] homeOrder(JSONArray sections){
  ArrayList<String> out=new ArrayList<>();
  if(sections!=null)for(int i=0;i<sections.length();i++){JSONObject s=sections.optJSONObject(i);String key=sectionKey(s);if(key!=null&&!out.contains(key))out.add(key);}
  if(out.isEmpty())return new String[]{"resume","nextup","latest"};
  return out.toArray(new String[0]);
 }
 static String sectionKey(JSONObject s){
  if(s==null)return null;
  String raw=(s.optString("Type")+" "+s.optString("Section")+" "+s.optString("Name")+" "+s.optString("Id")).toLowerCase(Locale.ROOT);
  if(s.has("Type")&&!s.isNull("Type")&&s.opt("Type") instanceof Number){int n=s.optInt("Type");if(n==4)return "resume";if(n==7)return "latest";if(n==1||n==2)return "libraries";return null;}
  if(raw.contains("nextup")||raw.contains("next up")||raw.contains("下一集"))return "nextup";
  if(raw.contains("latest")||raw.contains("最新"))return "latest";
  if(raw.contains("resume")||raw.contains("continue")||raw.contains("继续"))return "resume";
  if(raw.contains("library")||raw.contains("媒体库"))return "libraries";
  return null;
 }
 static boolean hidden(JSONObject server,String libraryId){JSONArray ids=server==null?null:server.optJSONArray("hidden");if(ids==null||libraryId==null)return false;for(int i=0;i<ids.length();i++)if(libraryId.equals(ids.optString(i)))return true;return false;}
 static JSONArray toggleHidden(JSONArray hidden,String libraryId)throws JSONException{
  JSONArray out=new JSONArray();boolean found=false;
  if(hidden!=null)for(int i=0;i<hidden.length();i++){String id=hidden.optString(i);if(id.isEmpty())continue;if(id.equals(libraryId))found=true;else out.put(id);}
  if(!found&&libraryId!=null&&!libraryId.isEmpty())out.put(libraryId);
  return out;
 }
 /** 多于一个可播放版本才需要选择。 */
 static JSONArray versions(JSONArray sources)throws JSONException{
  JSONArray out=new JSONArray();if(sources==null||sources.length()<2)return out;
  for(int i=0;i<sources.length();i++){JSONObject s=sources.optJSONObject(i);if(s==null||s.optString("Id").isEmpty())continue;String name=s.optString("Name");if(name.isEmpty())name=fileName(s.optString("Path"));if(name.isEmpty())name="版本 "+(out.length()+1);String container=s.optString("Container");
   out.put(new JSONObject().put("id",s.optString("Id")).put("name",(container.isEmpty()?name:name+" · "+container.toUpperCase(Locale.ROOT))+DolbyRules.tag(DolbyRules.kind(s))));}
  return out.length()<2?new JSONArray():out;
 }
 static String fileName(String path){if(path==null||path.isEmpty())return "";int slash=Math.max(path.lastIndexOf('/'),path.lastIndexOf('\\'));String n=slash>=0?path.substring(slash+1):path;int q=n.indexOf('?');return q>=0?n.substring(0,q):n;}
 /** 主地址在前，备用地址去掉空的、非法的和重复的。 */
 static String[] bases(String primary,JSONArray backups){
  ArrayList<String> out=new ArrayList<>();addBase(out,primary);
  if(backups!=null)for(int i=0;i<backups.length();i++)addBase(out,backups.optString(i));
  return out.toArray(new String[0]);
 }
 static void addBase(ArrayList<String> out,String raw){if(raw==null||raw.trim().isEmpty())return;String base;try{base=EmbyApi.server(raw);}catch(Exception e){return;}for(String have:out)if(have.equalsIgnoreCase(base))return;out.add(base);}
 static JSONArray parseBackups(String text)throws JSONException{JSONArray out=new JSONArray();if(text==null)return out;for(String line:text.split("\\r?\\n")){String s=line.trim();if(!s.isEmpty())out.put(s);}return out;}
 static boolean retryable(Throwable e){
  if(e==null)return false;Throwable c=e;while(c.getCause()!=null&&c.getCause()!=c)c=c.getCause();
  if(c instanceof java.net.SocketTimeoutException||c instanceof java.net.UnknownHostException||c instanceof java.net.ConnectException||c instanceof javax.net.ssl.SSLException)return true;
  String m=c.getMessage()==null?"":c.getMessage();
  return m.startsWith("HTTP 5")||m.contains("没有可播放")||m.contains("没有返回播放信息")||m.contains("没有返回可播放");
 }
 /**
  * 把 Emby 音轨对上 VLC 轨号。先按流索引精确对应，再按语言和编码对名称，最后按音轨顺序。
  * 对不上返回 -1。
  */
 static int matchTrack(int embyIndex,String language,String codec,int ordinal,int[] vlcIds,String[] vlcNames){
  if(vlcIds==null||vlcIds.length==0)return -1;
  for(int id:vlcIds)if(id>=0&&id==embyIndex)return id;
  String lang=language==null?"":language.toLowerCase(Locale.ROOT),code=codec==null?"":codec.toLowerCase(Locale.ROOT);
  int best=-1,score=0;
  for(int i=0;i<vlcIds.length;i++){if(vlcIds[i]<0)continue;String n=vlcNames!=null&&i<vlcNames.length&&vlcNames[i]!=null?vlcNames[i].toLowerCase(Locale.ROOT):"";int s=0;if(!lang.isEmpty()&&containsLang(n,lang))s+=3;if(!code.isEmpty()&&n.contains(code))s+=2;if(i==ordinal)s+=1;if(s>score){score=s;best=vlcIds[i];}}
  if(score>=3)return best;
  if(ordinal>=0&&ordinal<vlcIds.length&&vlcIds[ordinal]>=0)return vlcIds[ordinal];
  return -1;
 }
 static boolean containsLang(String name,String lang){if(lang.length()<=2)return PlaybackPrefs.has(name,lang);return name.contains(lang);}
 /**
  * 剧集按页拉取。有的服务器不带 Limit 只回第一页。
  * ImageTags / BackdropImageTags 用来画选集瓦片和换集后的主视觉。
  * SeriesPrimaryImageTag、ParentBackdrop* 是系列图兜底要用的字段。
  * 这里不要加 ThumbnailSet：整季抽帧又慢又容易 500。
  */
 static final String EPISODE_FIELDS="Overview,RunTimeTicks,UserData,ImageTags,BackdropImageTags,SeriesPrimaryImageTag,ParentBackdropItemId,ParentBackdropImageTag";
 static String episodesPath(String userId,String seriesId,String seasonId,int start,int limit){String q="/Shows/"+EmbyApi.enc(seriesId)+"/Episodes?UserId="+EmbyApi.enc(userId)+"&Fields="+EPISODE_FIELDS+"&StartIndex="+Math.max(0,start)+"&Limit="+Math.max(1,limit);if(seasonId!=null&&!seasonId.isEmpty())q+="&SeasonId="+EmbyApi.enc(seasonId);return q;}
 /** 空页、不满一页，或已经盖住 TotalRecordCount 就停。总数未知且这一页是满的，继续下一页。 */
 static boolean episodePageDone(int start,int pageCount,int limit,int total){if(pageCount<=0||pageCount<limit)return true;return total>=0&&start+pageCount>=total;}
 static String libraryPath(String userId,String parentId,String types,int start,String genre,boolean favorite){
  String q="/Users/"+EmbyApi.enc(userId)+"/Items?ParentId="+EmbyApi.enc(parentId)+"&Recursive=true&IncludeItemTypes="+EmbyApi.enc(types)+"&SortBy=SortName&SortOrder=Ascending&StartIndex="+Math.max(0,start)+"&Limit="+EmbyClient.PAGE+"&Fields=Overview,ProductionYear,CommunityRating,RunTimeTicks,Genres,UserData";
  if(genre!=null&&!genre.isEmpty())q+="&Genres="+EmbyApi.enc(genre);
  if(favorite)q+="&Filters=IsFavorite";
  return q;
 }
 static float mark(long startMs,long durMs){if(durMs<=0||startMs<0)return -1f;float f=startMs/(float)durMs;return f<0f?0f:f>1f?1f:f;}
}
