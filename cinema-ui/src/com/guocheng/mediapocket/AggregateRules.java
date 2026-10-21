package com.guocheng.mediapocket;
import org.json.*;import java.util.*;import java.util.regex.*;
/**
 * 0.1.127 聚合首页的纯逻辑：判断条目来自哪里、把影视仓分类名对到聚合行、多来源合并去重。不依赖 Android。
 * 合并规则：各来源按优先级（Emby → 影视仓 → Stremio）轮流取一条，保证一行里能看到不同来源；
 * 同名（去掉空格、标点、「第 X 季」）且年份相差不超过 1 年的视为同一部，只留优先级高的那条，并记下有几个来源。
 */
final class AggregateRules {
 private AggregateRules(){}
 static final String EMBY="emby",TVBOX="tvbox",STREMIO="stremio";
 /** 聚合行：键、标题、卡片类型。 */
 static final String[][] ROWS={{"fresh","最近更新","movie"},{"movie","电影","movie"},{"series","剧集","series"},{"anime","动漫","series"},{"doc","纪录片","movie"}};
 static String source(JSONObject m){if(m==null)return STREMIO;if(m.has("emby.server"))return EMBY;if(m.has("fusion.site"))return TVBOX;return STREMIO;}
 static String label(String source){return EMBY.equals(source)?"EMBY":TVBOX.equals(source)?"影视仓":"Stremio";}
 /** 影视仓分类名 → 聚合行键；对不上返回空串。 */
 static String rowForCategory(String name){String n=name==null?"":name.replaceAll("\\s","");
  if(n.matches(".*(每日更新|今日更新|最近更新|最新).*"))return "fresh";
  if(n.matches(".*(动漫|动画|番剧|国漫|日漫).*"))return "anime";
  if(n.matches(".*(纪录|记录片).*"))return "doc";
  if(n.matches(".*(电视剧|连续剧|剧集|国产剧|美剧|韩剧|日剧|港剧|台剧).*"))return "series";
  if(n.matches(".*(电影|影片).*"))return "movie";
  return "";}
 static String key(String name){if(name==null)return "";String s=name.toLowerCase(Locale.ROOT);
  s=s.replaceAll("第\\s*[一二三四五六七八九十0-9]+\\s*季","").replaceAll("\\bseason\\s*\\d+\\b","").replaceAll("[（(]\\s*(19|20)\\d\\d\\s*[）)]","");
  return s.replaceAll("[\\s\\p{Punct}·：，。！？、“”‘’《》【】（）—…]","");}
 static final Pattern YEAR=Pattern.compile("(19|20)\\d\\d");
 static int year(JSONObject m){if(m==null)return 0;for(String f:new String[]{"year","releaseInfo","emby.year"}){Matcher x=YEAR.matcher(m.optString(f));if(x.find())return Integer.parseInt(x.group());}return 0;}
 static boolean same(JSONObject a,JSONObject b){String ka=key(a.optString("name")),kb=key(b.optString("name"));if(ka.isEmpty()||!ka.equals(kb))return false;int ya=year(a),yb=year(b);return ya==0||yb==0||Math.abs(ya-yb)<=1;}
 /** lists 已按来源优先级排好；结果里每条带 agg.source（来源）和 agg.count（几个来源有这部）。 */
 static JSONArray merge(List<JSONArray> lists,int limit){List<JSONObject> out=new ArrayList<>();int longest=0;for(JSONArray l:lists)if(l!=null)longest=Math.max(longest,l.length());
  for(int i=0;i<longest&&out.size()<limit*3;i++)for(JSONArray l:lists){if(l==null||i>=l.length())continue;JSONObject m=l.optJSONObject(i);if(m==null||m.optString("name").isEmpty())continue;
   JSONObject hit=null;for(JSONObject o:out)if(same(o,m)){hit=o;break;}
   try{if(hit!=null){hit.put("agg.count",hit.optInt("agg.count",1)+1);continue;}JSONObject c=new JSONObject(m.toString());c.put("agg.source",source(m));c.put("agg.count",1);out.add(c);}catch(JSONException ignored){}}
  JSONArray r=new JSONArray();for(int i=0;i<out.size()&&i<limit;i++)r.put(out.get(i));return r;}
 /** 继续观看：Emby 进度在前，本机记录在后，同一部只留第一条。 */
 static JSONArray resume(JSONArray emby,JSONArray local){JSONArray out=new JSONArray();List<JSONObject> seen=new ArrayList<>();for(JSONArray l:new JSONArray[]{emby,local})for(int i=0;l!=null&&i<l.length();i++){JSONObject m=l.optJSONObject(i);if(m==null)continue;boolean dup=false;String id=m.optString("id");for(JSONObject s:seen)if((!id.isEmpty()&&id.equals(s.optString("id")))||same(s,m)){dup=true;break;}if(dup)continue;seen.add(m);out.put(m);}return out;}
 /** 「最近更新」超过这么久才重新请求（毫秒）；下拉刷新时清掉时间戳强制重读。 */
 static final long FRESH_MS=6*60*60*1000L;
 static boolean stale(long savedAt,long now){return savedAt<=0||now-savedAt>=FRESH_MS;}
}
