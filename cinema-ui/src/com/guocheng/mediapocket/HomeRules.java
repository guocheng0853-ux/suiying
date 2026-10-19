package com.guocheng.mediapocket;
import org.json.*;
import java.util.*;
/** Hills/Emby 首页纯规则：底栏别名、片源选项、续播进度、集数文案、角标、信息行。 */
final class HomeRules {
 static final String[] TABS={"首页","收藏","搜索","我的"};
 static final int[] TAB_ICONS={0,2,1,4};
 /** 底栏 4 项对应的 NavIcon 种类（首页/心形/发现罗盘/齿轮）。 */
 static int tabIcon(int i){return i>=0&&i<TAB_ICONS.length?TAB_ICONS[i]:0;}
 /** 把 base APK 的「发现/插件/设置」映射到 Hills 四栏。 */
 static String navAlias(String selected){
  if(selected==null)return "首页";
  if(selected.equals("发现"))return "搜索";
  if(selected.equals("设置")||selected.equals("插件"))return "我的";
  for(String t:TABS)if(t.equals(selected))return selected;
  return selected;
 }
 static int tabIndex(String selected){String a=navAlias(selected);for(int i=0;i<TABS.length;i++)if(TABS[i].equals(a))return i;return 0;}
 /** 续播进度 0..1；优先用时长，其次 runtime 分钟，否则按 45 分钟估算。 */
 static float progress(int positionMs,int durationMs,int runtimeMinutes){
  if(positionMs<=0)return 0f;
  long total=durationMs>0?durationMs:(runtimeMinutes>0?runtimeMinutes*60000L:45L*60000L);
  if(total<=0)return 0f;
  float f=positionMs/(float)total;
  if(f<0.02f)return 0.02f;
  return f>0.98f?0.98f:f;
 }
 /** 片长可信（≥ 10 秒）且与已存值相差超过 1 秒时才写入，避免每帧写 prefs。 */
 static boolean shouldStoreDuration(int durationMs,int storedMs){return durationMs>=10000&&Math.abs(durationMs-storedMs)>1000;}
 static int runtimeMinutes(JSONObject meta){
  if(meta==null)return 0;
  int m=StreamInfo.minutes(meta.optString("runtime"));
  if(m>0)return m;
  String y=meta.optString("vod_duration",meta.optString("duration",""));
  return StreamInfo.minutes(y.isEmpty()?meta.optString("runtime",""):y);
 }
 /** lastEpisode 形如 tt123:1:161 → S1E161 · 第161集；否则「最近观看」。 */
 /** 继续观看卡片标题：影视仓记录的名字是「片名 · 线路/文件名」，只显示片名。 */
 static String cardTitle(JSONObject m){
  if(m==null)return "影片";String t=m.optString("fusion.title");if(!t.trim().isEmpty())return t.trim();
  String name=m.optString("name","影片");if(m.has("fusion.site")){int dot=name.indexOf(" · ");if(dot>0)return name.substring(0,dot).trim();}
  // 剧集记录的名字常带「 · S1E105 · 第 105 集」，副标题已经写了集数，标题里去掉
  String trimmed=name.replaceAll("\\s*·\\s*S\\d+\\s*E\\d+.*$","").trim();
  return trimmed.isEmpty()?name:trimmed;
 }
 /** 继续观看副标题：集数 + 剩余时长；不知道时长时把「续播」换成「继续播放」。 */
 static String resumeLine(String base,int posMs,int durMs){
  String b=base==null?"":base.trim();boolean generic=b.isEmpty()||b.equals("续播")||b.equals("最近观看");
  String left="";if(durMs>0&&posMs>0&&durMs>posMs){long min=(durMs-posMs+59999L)/60000L;left=min>=60?"剩 "+(min/60)+" 小时"+(min%60>0?" "+(min%60)+" 分":""):"剩 "+min+" 分钟";}
  if(generic)return left.isEmpty()?"继续播放":left;
  return left.isEmpty()?b:b+" · "+left;
 }
 /** 首页来源下拉每项下面的一行说明。 */
 static String sourceHint(String id,String type){
  String i=id==null?"":id,t=type==null?"":type;
  if(i.equals("provider:fusion"))return "合并你配置的所有来源";
  if(i.equals("provider:tvbox"))return "当前影视仓接口的站点";
  if(i.equals("provider:stremio"))return "已安装的 Stremio 插件目录";
  if(i.startsWith("provider:emby:")||t.equals("emby"))return "Emby 服务器的媒体库";
  if(i.startsWith("action:"))return "添加或管理 Emby 服务器";
  return "影视仓接口";
 }
 static String episodeLabel(String lastEpisode){
  if(lastEpisode==null||lastEpisode.isEmpty())return "最近观看";
  String[] p=lastEpisode.split(":");
  if(p.length>=3){
   try{int s=Integer.parseInt(p[p.length-2]),e=Integer.parseInt(p[p.length-1]);
    if(s>=0&&e>=0)return "S"+s+"E"+e+" · 第"+e+"集";}catch(Exception ignored){}
  }
  return "续播";
 }
 /** 海报角标：优先短数字（集数/更新），否则评分。 */
 static String badge(JSONObject meta){
  if(meta==null)return "";
  for(String k:new String[]{"vod_remarks","releaseInfo"}){
   String v=meta.optString(k,"").trim();if(v.isEmpty()||v.equals("null"))continue;
   String num=numericBadge(v);if(!num.isEmpty())return num;
  }
  String r=meta.optString("imdbRating","").trim();
  return r.isEmpty()||r.equals("null")?"":"★ "+r;
 }
 /** 从「更新至48」「全16集」「48」等提取短数字角标。 */
 static String numericBadge(String raw){
  if(raw==null||raw.matches("\\d+\\.\\d+"))return "";
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d{1,4})").matcher(raw);
  String last="";while(m.find())last=m.group(1);
  if(last.isEmpty())return "";
  if(last.length()==4){int y=Integer.parseInt(last);if(y>=1900&&y<=2100)return "";}
  return last;
 }
 /** Hero 信息行：★ 评分 · 年份 · 类型。影视仓不写站点、清晰度、年份和类型。 */
 static String heroFacts(JSONObject m){
  if(m==null||HomeHeroRules.tvboxHero(m))return "";
  ArrayList<String> parts=new ArrayList<>();
  String score=m.optString("imdbRating","").trim();
  if(!score.isEmpty()&&!score.equals("null"))parts.add("★ "+score);
  String year=m.optString("releaseInfo",m.optString("vod_year","")).trim();
  if(!year.isEmpty())parts.add(year.split("[–-]")[0].trim());
  String g=genresJoined(m,"/");
  if(!g.isEmpty())parts.add(g);
  return String.join("  ·  ",parts);
 }
 static String genresJoined(JSONObject m,String sep){
  JSONArray genres=m==null?null:m.optJSONArray("genres");
  if(genres==null||genres.length()==0){
   String t=m==null?"":m.optString("vod_class",m.optString("genre",""));
   return t.replace(",","/").replace("，","/");
  }
  ArrayList<String> g=new ArrayList<>();
  for(int i=0;i<genres.length()&&i<4;i++){String s=genres.optString(i);if(!s.isEmpty())g.add(s);}
  return String.join(sep,g);
 }
 /** 官方分级徽章：Emby 的 OfficialRating 或 Stremio 的 certification，tvbox 没有。 */
 static String heroCert(JSONObject m){
  if(m==null||HomeHeroRules.tvboxHero(m))return "";
  String cert=m.optString("certification",m.optString("OfficialRating","")).trim();
  if(cert.isEmpty()||cert.equalsIgnoreCase("null"))return "";
  return cert;
 }
 static String heroDescription(JSONObject m){
  if(m==null)return "";
  String d=m.optString("description",m.optString("vod_content","")).trim();
  if(d.isEmpty())return HomeHeroRules.tvboxHero(m)?"":"点开影片，查看简介与选集";
  d=d.replaceAll("<[^>]+>","").replace("&nbsp;"," ").replaceAll("\\s+"," ").trim();
  return d.length()>90?d.substring(0,90)+"…":d;
 }
 /** 添加或管理 Emby。选中它不改已保存的首页片源。 */
 static final String EMBY_MANAGE="action:emby";
 /** 片源下拉：Stremio / 影视仓 / 融合 + 公开来源，并始终带上 Emby。没有服务器时是「添加」，有服务器时每个服务器一条，末尾再加「管理」。 */
 static JSONArray sourceOptions(JSONArray homeSources)throws Exception{return sourceOptions(homeSources,null);}
 static JSONArray sourceOptions(JSONArray homeSources,JSONArray embyServers)throws Exception{
  JSONArray out=new JSONArray();
  out.put(opt("provider:stremio","Stremio 目录","provider"));
  out.put(opt("provider:tvbox","影视仓","provider"));
  out.put(opt("provider:fusion","融合","provider"));
  if(homeSources!=null)for(int i=0;i<homeSources.length();i++){
   JSONObject s=homeSources.optJSONObject(i);if(s==null)continue;
   String id=s.optString("id");if(id.isEmpty())continue;
   out.put(opt("source:"+id,s.optString("name",id),"tvbox"));
  }
  int emby=0;
  if(embyServers!=null)for(int i=0;i<embyServers.length();i++){
   JSONObject s=embyServers.optJSONObject(i);if(s==null)continue;
   String id=s.optString("id");if(id.isEmpty())continue;
   out.put(opt("provider:emby:"+id,s.optString("name","Emby")+" · Emby","emby"));
   emby++;
  }
  out.put(opt(EMBY_MANAGE,emby==0?"添加 Emby 服务器":"管理 Emby 服务器","action"));
  return out;
 }
 /** 「我的」第一项：没有服务器写未添加，有则写个数。只数带 id 的服务器。 */
 static String embyServersLabel(JSONArray servers){
  int n=0;
  if(servers!=null)for(int i=0;i<servers.length();i++){JSONObject s=servers.optJSONObject(i);if(s!=null&&!s.optString("id").isEmpty())n++;}
  return n<=0?"Emby 服务器 · 未添加":"Emby 服务器 · "+n+" 个";
 }
 static boolean keepsHomeProvider(String optionId){return optionId!=null&&optionId.startsWith("action:");}
 static JSONObject opt(String id,String label,String kind)throws Exception{
  return new JSONObject().put("id",id).put("label",label).put("kind",kind);
 }
 /** 当前选中项 id。 */
 static String selectedId(String provider,String sourceId){
  if("emby".equals(provider))return "provider:emby:"+(sourceId==null||sourceId.isEmpty()?"current":sourceId);
  if(sourceId!=null&&!sourceId.isEmpty()&&!sourceId.equals("current")&&("tvbox".equals(provider)||"fusion".equals(provider)))
   return "source:"+sourceId;
  if("tvbox".equals(provider))return "provider:tvbox";
  if("fusion".equals(provider))return "provider:fusion";
  return "provider:stremio";
 }
 static String labelFor(JSONArray options,String id){
  if(options!=null)for(int i=0;i<options.length();i++){
   JSONObject o=options.optJSONObject(i);
   if(o!=null&&id.equals(o.optString("id")))return o.optString("label",id);
  }
  if(id.startsWith("provider:"))return id.substring(9);
  if(id.startsWith("source:"))return id.substring(7);
  return "片源";
 }
 /** 解析选项 → [provider, sourceId]。添加/管理返回 null，调用方不得写入片源。source 选项落到 tvbox。 */
 static String[] apply(String optionId){
  if(optionId==null||optionId.isEmpty())return new String[]{"stremio","current"};
  if(keepsHomeProvider(optionId))return null;
  if(optionId.startsWith("provider:emby:")){String id=optionId.substring("provider:emby:".length());return new String[]{"emby",id.isEmpty()?"current":id};}
  if(optionId.startsWith("provider:")){
   String p=optionId.substring(9);
   if(!p.equals("stremio")&&!p.equals("tvbox")&&!p.equals("fusion"))p="stremio";
   return new String[]{p,"current"};
  }
  if(optionId.startsWith("source:"))return new String[]{"tvbox",optionId.substring(7)};
  return new String[]{"stremio","current"};
 }
 /** 从播放页回到已经画好的首页时，不要再走 home() 重拉整页。 */
 static boolean keepOnResume(boolean homeVisible,int childCount){return homeVisible&&childCount>0;}
 /** 首页分区默认都显示。hidden 是逗号分隔的分区 id，出现就隐藏。 */
 static boolean sectionShown(String hidden,String key){if(key==null||key.isEmpty()||hidden==null||hidden.isEmpty())return true;for(String part:hidden.split(","))if(key.equals(part))return false;return true;}
 static String toggleSection(String hidden,String key){
  if(key==null||key.isEmpty())return hidden==null?"":hidden;
  java.util.LinkedHashSet<String> set=new java.util.LinkedHashSet<>();
  if(hidden!=null)for(String part:hidden.split(","))if(!part.isEmpty())set.add(part);
  if(!set.add(key))set.remove(key);
  StringBuilder s=new StringBuilder();for(String part:set){if(s.length()>0)s.append(',');s.append(part);}return s.toString();
 }
 /** 从继续观看里去掉的 id。store 是 JSON 数组字符串。 */
 static String hideResume(String store,String id){
  JSONArray out=new JSONArray();if(id==null||id.isEmpty())return store==null?"[]":store;
  try{JSONArray old=store==null||store.isEmpty()?new JSONArray():new JSONArray(store);for(int i=0;i<old.length();i++){String v=old.optString(i);if(!v.isEmpty()&&!v.equals(id))out.put(v);}}catch(Exception ignored){}
  out.put(id);return out.toString();
 }
 static boolean resumeHidden(String store,String id){if(id==null||id.isEmpty()||store==null||store.isEmpty())return false;try{JSONArray old=new JSONArray(store);for(int i=0;i<old.length();i++)if(id.equals(old.optString(i)))return true;}catch(Exception ignored){}return false;}
 /**
  * 同一部片子的观看记录键。影视仓的记录 id 是按“接口+站点+片子+集名”算的哈希，每集（每个版本）都不同，
  * 原版按 id 去重会让同一部剧每看一集多一条。这里按片子归并：影视仓用接口+站点+vod_id，Emby 用剧集（没有剧集用条目），其余用 id。
  */
 static String showKey(JSONObject m){
  if(m==null)return "";
  String site=m.optString("fusion.site"),video=m.optString("fusion.video");
  if(!site.isEmpty()&&!video.isEmpty())return "tvbox|"+m.optString("fusion.config")+"|"+site+"|"+video;
  String server=m.optString("emby.server");
  if(!server.isEmpty()){String series=m.optString("emby.series");return "emby|"+server+"|"+(series.isEmpty()?m.optString("emby.item",m.optString("id")):series);}
  return m.optString("id");
 }
 /** 每部片子只留最近的一条（记录按新到旧排列，保留第一次出现的）。 */
 static JSONArray dedupeHistory(JSONArray items){
  JSONArray out=new JSONArray();if(items==null)return out;java.util.HashSet<String> seen=new java.util.HashSet<>();
  for(int i=0;i<items.length();i++){JSONObject m=items.optJSONObject(i);if(m==null)continue;String key=showKey(m);if(!key.isEmpty()&&!seen.add(key))continue;out.put(m);}
  return out;
 }
 static JSONArray withoutHidden(JSONArray items,String store){
  JSONArray out=new JSONArray();if(items==null)return out;
  for(int i=0;i<items.length();i++){JSONObject m=items.optJSONObject(i);if(m==null)continue;String id=m.optString("id");if(id.isEmpty())id=m.optString("Id");if(resumeHidden(store,id))continue;out.put(m);}
  return out;
 }
 /** 媒体库分类瓦片定义：title, type, genre。 */
 static String[][] libraryTiles(){
  return new String[][]{
   {"电影","movie",""},
   {"剧集","series",""},
   {"动画电影","movie","Animation"},
   {"动画剧集","series","Animation"}
  };
 }
}
