package com.guocheng.mediapocket;
import org.json.*;import java.util.*;import java.util.regex.*;
/**
 * 0.1.127 聚合首页的纯逻辑：判断条目来自哪里、把影视仓分类名对到聚合行、多来源合并去重。不依赖 Android。
 * 合并规则：各来源按优先级（Emby → 影视仓 → Stremio）轮流取一条，保证一行里能看到不同来源；
 * 同名（去掉空格、标点、「第 X 季」）且年份相差不超过 1 年的视为同一部，只留优先级高的那条，并记下有几个来源。
 * 0.1.130：正则只编译一次；每条只算一次名字键和年份，用哈希表查重；浅拷贝字段，不再 toString 深拷贝。
 */
final class AggregateRules {
 private AggregateRules(){}
 static final String EMBY="emby",TVBOX="tvbox",STREMIO="stremio";
 /** 聚合行：键、标题、卡片类型。 */
 static final String[][] ROWS={{"fresh","最近更新","movie"},{"movie","电影","movie"},{"series","剧集","series"},{"anime","动漫","series"},{"doc","纪录片","movie"}};
 static final Pattern WS=Pattern.compile("\\s");
 static final Pattern CAT_FRESH=Pattern.compile(".*(每日更新|今日更新|最近更新|最新).*");
 static final Pattern CAT_ANIME=Pattern.compile(".*(动漫|动画|番剧|国漫|日漫).*");
 static final Pattern CAT_DOC=Pattern.compile(".*(纪录|记录片).*");
 static final Pattern CAT_SERIES=Pattern.compile(".*(电视剧|连续剧|剧集|国产剧|美剧|韩剧|日剧|港剧|台剧).*");
 static final Pattern CAT_MOVIE=Pattern.compile(".*(电影|影片).*");
 static final Pattern SEASON_CN=Pattern.compile("第\\s*[一二三四五六七八九十0-9]+\\s*季");
 static final Pattern SEASON_EN=Pattern.compile("\\bseason\\s*\\d+\\b");
 static final Pattern YEAR_PAREN=Pattern.compile("[（(]\\s*(19|20)\\d\\d\\s*[）)]");
 static final Pattern PUNCT=Pattern.compile("[\\s\\p{Punct}·：，。！？、“”‘’《》【】（）—…]");
 static final Pattern YEAR=Pattern.compile("(19|20)\\d\\d");
 static String source(JSONObject m){if(m==null)return STREMIO;if(m.has("emby.server"))return EMBY;if(m.has("fusion.site"))return TVBOX;return STREMIO;}
 static String label(String source){return EMBY.equals(source)?"EMBY":TVBOX.equals(source)?"影视仓":"Stremio";}
 /** 影视仓分类名 → 聚合行键；对不上返回空串。 */
 static String rowForCategory(String name){String n=name==null?"":WS.matcher(name).replaceAll("");
  if(CAT_FRESH.matcher(n).matches())return "fresh";
  if(CAT_ANIME.matcher(n).matches())return "anime";
  if(CAT_DOC.matcher(n).matches())return "doc";
  if(CAT_SERIES.matcher(n).matches())return "series";
  if(CAT_MOVIE.matcher(n).matches())return "movie";
  return "";}
 static String key(String name){if(name==null)return "";String s=name.toLowerCase(Locale.ROOT);
  s=SEASON_CN.matcher(s).replaceAll("");s=SEASON_EN.matcher(s).replaceAll("");s=YEAR_PAREN.matcher(s).replaceAll("");
  return PUNCT.matcher(s).replaceAll("");}
 static int year(JSONObject m){if(m==null)return 0;for(String f:new String[]{"year","releaseInfo","emby.year"}){Matcher x=YEAR.matcher(m.optString(f));if(x.find())return Integer.parseInt(x.group());}return 0;}
 /** 一条候选的名字键和年份，合并时只算一次。 */
 static final class Ident {
  final String key;final int year;
  Ident(JSONObject m){key=AggregateRules.key(m==null?"":m.optString("name"));year=AggregateRules.year(m);}
  boolean same(Ident o){if(o==null||key.isEmpty()||!key.equals(o.key))return false;return year==0||o.year==0||Math.abs(year-o.year)<=1;}
 }
 static boolean same(JSONObject a,JSONObject b){return new Ident(a).same(new Ident(b));}
 /** 顶层字段浅拷贝。嵌套对象仍共享，避免整段 JSON 再解析一遍。 */
 static JSONObject copy(JSONObject m){JSONObject c=new JSONObject();if(m==null)return c;for(Iterator<String> it=m.keys();it.hasNext();){String k=it.next();try{c.put(k,m.opt(k));}catch(JSONException ignored){}}return c;}
 static final class Slot {final Ident id;final JSONObject obj;Slot(Ident id,JSONObject obj){this.id=id;this.obj=obj;}}
 /** lists 已按来源优先级排好；结果里每条带 agg.source（来源）和 agg.count（几个来源有这部）。 */
 static JSONArray merge(List<JSONArray> lists,int limit){List<JSONObject> out=new ArrayList<>();int longest=0;if(lists==null)lists=Collections.emptyList();for(JSONArray l:lists)if(l!=null)longest=Math.max(longest,l.length());
  Map<String,List<Slot>> byKey=new HashMap<>();int cap=Math.max(0,limit)*3;
  for(int i=0;i<longest&&out.size()<cap;i++)for(JSONArray l:lists){if(l==null||i>=l.length())continue;JSONObject m=l.optJSONObject(i);if(m==null||m.optString("name").isEmpty())continue;
   Ident id=new Ident(m);Slot hit=null;if(!id.key.isEmpty()){List<Slot> slots=byKey.get(id.key);if(slots!=null)for(Slot s:slots)if(id.same(s.id)){hit=s;break;}}
   try{if(hit!=null){hit.obj.put("agg.count",hit.obj.optInt("agg.count",1)+1);continue;}JSONObject c=copy(m);c.put("agg.source",source(m));c.put("agg.count",1);out.add(c);
    if(!id.key.isEmpty()){List<Slot> slots=byKey.get(id.key);if(slots==null)byKey.put(id.key,slots=new ArrayList<Slot>(1));slots.add(new Slot(id,c));}}catch(JSONException ignored){}}
  JSONArray r=new JSONArray();for(int i=0;i<out.size()&&i<limit;i++)r.put(out.get(i));return r;}
 /** 继续观看：Emby 进度在前，本机记录在后，同一部只留第一条。 */
 static JSONArray resume(JSONArray emby,JSONArray local){JSONArray out=new JSONArray();Map<String,List<Ident>> byKey=new HashMap<>();Map<String,Boolean> ids=new HashMap<>();
  for(JSONArray l:new JSONArray[]{emby,local})for(int i=0;l!=null&&i<l.length();i++){JSONObject m=l.optJSONObject(i);if(m==null)continue;String id=m.optString("id");if(!id.isEmpty()&&ids.containsKey(id))continue;Ident ident=new Ident(m);boolean dup=false;
   if(!ident.key.isEmpty()){List<Ident> slots=byKey.get(ident.key);if(slots!=null)for(Ident s:slots)if(ident.same(s)){dup=true;break;}}if(dup)continue;
   if(!id.isEmpty())ids.put(id,Boolean.TRUE);if(!ident.key.isEmpty()){List<Ident> slots=byKey.get(ident.key);if(slots==null)byKey.put(ident.key,slots=new ArrayList<Ident>(1));slots.add(ident);}out.put(m);}return out;}
 /** 「最近更新」超过这么久才重新请求（毫秒）；下拉刷新时清掉时间戳强制重读。 */
 static final long FRESH_MS=6*60*60*1000L;
 static boolean stale(long savedAt,long now){return savedAt<=0||now-savedAt>=FRESH_MS;}
}
