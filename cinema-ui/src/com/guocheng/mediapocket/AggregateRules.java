package com.guocheng.mediapocket;
import org.json.*;import java.util.*;import java.util.regex.*;
/**
 * 聚合首页的纯逻辑，不依赖 Android。
 * 0.1.130：合并在调用方的后台线程；正则只编译一次；浅拷贝，不再 toString 深拷贝。
 * 0.1.136：各行按类型取，不再从「最近更新」里切；续集标记或缺少年份时不误并；
 * 看完的沉底、收藏靠前，最近更新按时间排并在同一档里轮流露出各来源。
 */
final class AggregateRules {
 private AggregateRules(){}
 static final String EMBY="emby",TVBOX="tvbox",STREMIO="stremio";
 /** 聚合行：键、标题、卡片类型。从上到下，后面的行会跳过前面已经出现的片子。 */
 static final String[][] ROWS={{"fresh","最近更新","movie"},{"movie","电影","movie"},{"series","剧集","series"},{"anime","动漫","series"},{"doc","纪录片","movie"}};
 /** 影视仓拉分类的顺序：电影、剧集先到，每日更新放最后。 */
 static final String[] FETCH_ROWS={"movie","series","anime","doc","fresh"};
 static final Pattern WS=Pattern.compile("\\s");
 static final Pattern CAT_FRESH=Pattern.compile(".*(每日更新|今日更新|最近更新|最新).*");
 static final Pattern CAT_ANIME=Pattern.compile(".*(动漫|动画|番剧|国漫|日漫|anime|animation).*",Pattern.CASE_INSENSITIVE);
 static final Pattern CAT_DOC=Pattern.compile(".*(纪录|记录片|documentary).*",Pattern.CASE_INSENSITIVE);
 static final Pattern CAT_SERIES=Pattern.compile(".*(电视剧|连续剧|剧集|国产剧|美剧|韩剧|日剧|港剧|台剧).*");
 static final Pattern CAT_MOVIE=Pattern.compile(".*(电影|影片).*");
 static final Pattern ANIME_TEXT=Pattern.compile("动漫|动画|番剧|国漫|日漫|anime|animation",Pattern.CASE_INSENSITIVE);
 static final Pattern DOC_TEXT=Pattern.compile("纪录|记录片|documentary",Pattern.CASE_INSENSITIVE);
 static final Pattern SEASON_CN=Pattern.compile("第\\s*[一二三四五六七八九十0-9]+\\s*季");
 static final Pattern SEASON_EN=Pattern.compile("\\bseason\\s*\\d+\\b",Pattern.CASE_INSENSITIVE);
 static final Pattern YEAR_PAREN=Pattern.compile("[（(]\\s*(19|20)\\d\\d\\s*[）)]");
 static final Pattern PUNCT=Pattern.compile("[\\s\\p{Punct}·：，。！？、“”‘’《》【】（）—…]");
 static final Pattern YEAR=Pattern.compile("(19|20)\\d\\d");
 static final Pattern PART_CN=Pattern.compile("第\\s*([0-9一二三四五六七八九十]+)\\s*部");
 static final Pattern PART_EN=Pattern.compile("\\bpart\\s*(ii|iii|iv|two|three|four|[2-9])\\b",Pattern.CASE_INSENSITIVE);
 static final Pattern ROMAN=Pattern.compile("\\b(ii|iii|iv)\\b",Pattern.CASE_INSENSITIVE);
 static final Pattern TRAIL_DIGIT=Pattern.compile("(?:^|[^0-9])([2-9])$");
 static final Pattern EPISODE_SUFFIX=Pattern.compile("\\s*·\\s*第\\s*[0-9一二三四五六七八九十]+\\s*[集话回].*$");
 static final Pattern STAMP=Pattern.compile("((19|20)\\d\\d)-(\\d\\d)-(\\d\\d)(?:[T ](\\d\\d):(\\d\\d)(?::(\\d\\d))?)?");
 static final String[] BLOCKED={"music","books","photos","musicvideos","livetv","playlists","audiobooks"};
 static String source(JSONObject m){if(m==null)return STREMIO;if(m.has("emby.server"))return EMBY;if(m.has("fusion.site"))return TVBOX;return STREMIO;}
 static String label(String source){return EMBY.equals(source)?"EMBY":TVBOX.equals(source)?"影视仓":"Stremio";}
 static boolean animeText(String s){return s!=null&&ANIME_TEXT.matcher(s).find();}
 static boolean docText(String s){return s!=null&&DOC_TEXT.matcher(s).find();}
 /** 音乐、图书、照片、直播这类库不进首页。动漫、纪录片即使类型名不在标准名单里也留下。 */
 static boolean blockedLibrary(String type,String name){String t=type==null?"":type.toLowerCase(Locale.ROOT);for(String b:BLOCKED)if(b.equals(t))return true;return false;}
 /** 影视仓分类名 → 聚合行键。具体类型优先于「最新」，这样「最新动漫」进动漫而不是最近更新。对不上返回空串。 */
 static String rowForCategory(String name){String n=name==null?"":WS.matcher(name).replaceAll("");
  if(CAT_ANIME.matcher(n).matches())return "anime";
  if(CAT_DOC.matcher(n).matches())return "doc";
  boolean series=CAT_SERIES.matcher(n).matches(),movie=CAT_MOVIE.matcher(n).matches();
  if(series&&!movie)return "series";
  if(movie)return "movie";
  if(series)return "series";
  if(CAT_FRESH.matcher(n).matches())return "fresh";
  return "";}
 /**
  * Emby 条目归到哪一行。库名或库类型是动漫 / 纪录时整库归位；否则看题材；再按电影或剧集。
  * 卡片为空时只根据库判断，标准电影库返回空串，让调用方逐条再分。
  */
 static String rowForEmby(JSONObject card,String libraryName,String collectionType){
  if(blockedLibrary(collectionType,libraryName))return "";
  String blob=(collectionType==null?"":collectionType)+" "+(libraryName==null?"":libraryName);
  if(animeText(blob))return "anime";
  if(docText(blob))return "doc";
  String genres="";
  if(card!=null){JSONArray g=card.optJSONArray("genres");for(int i=0;g!=null&&i<g.length();i++)genres+=" "+g.optString(i);}
  if(animeText(genres))return "anime";
  if(docText(genres))return "doc";
  if(card==null)return "";
  return "series".equals(card.optString("type"))?"series":"movie";
 }
 /** 一个分类可以进同一行的多个栏目，按拉取顺序排好。 */
 static Map<String,JSONArray> groupCategories(JSONArray categories)throws JSONException{
  Map<String,JSONArray> out=new LinkedHashMap<>();for(String row:FETCH_ROWS)out.put(row,new JSONArray());
  if(categories==null)return out;
  for(int i=0;i<categories.length();i++){JSONObject c=categories.optJSONObject(i);if(c==null)continue;String row=rowForCategory(c.optString("type_name"));if(row.isEmpty()||!out.containsKey(row))continue;out.get(row).put(c);}
  return out;
 }
 static String key(String name){if(name==null)return "";String s=name.toLowerCase(Locale.ROOT);
  s=SEASON_CN.matcher(s).replaceAll("");s=SEASON_EN.matcher(s).replaceAll("");s=YEAR_PAREN.matcher(s).replaceAll("");
  return PUNCT.matcher(s).replaceAll("");}
 /** 去掉续集标记后再做 key，公开的 key() 本身不变，避免「Dune: Part Two」这类已有结果被改掉。 */
 static String stripSequel(String name){String s=fold(name);
  s=PART_CN.matcher(s).replaceAll(" ");
  s=s.replace("上部"," ").replace("下部"," ").replace("续集"," ").replace("续篇"," ").replace("续作"," ").replace("前传"," ").replace("后传"," ");
  s=PART_EN.matcher(s).replaceAll(" ");s=ROMAN.matcher(s).replaceAll(" ");
  s=s.replaceAll("(?<![0-9])[2-9](?![0-9])\\s*$"," ");
  return s;}
 /** 续集标记。空串表示不是续集。第 2 部和「2」「II」视为同一种续集号。 */
 static String sequelTag(String name){String s=fold(name).toLowerCase(Locale.ROOT);
  s=SEASON_CN.matcher(s).replaceAll(" ");s=SEASON_EN.matcher(s).replaceAll(" ");
  Matcher part=PART_CN.matcher(s);if(part.find())return "n"+cnNum(part.group(1));
  if(s.contains("上部"))return "up";if(s.contains("下部"))return "down";
  if(s.contains("续集")||s.contains("续篇")||s.contains("续作"))return "seq";
  if(s.contains("前传"))return "pre";if(s.contains("后传"))return "post";
  Matcher en=PART_EN.matcher(s);if(en.find())return "n"+enNum(en.group(1));
  Matcher rom=ROMAN.matcher(s);if(rom.find())return "n"+enNum(rom.group(1));
  String compact=PUNCT.matcher(s).replaceAll("");Matcher trail=TRAIL_DIGIT.matcher(compact);if(trail.find())return "n"+trail.group(1);
  return "";}
 static String fold(String name){if(name==null)return "";StringBuilder b=new StringBuilder(name.length());
  for(int i=0;i<name.length();i++){char c=name.charAt(i);if(c>='０'&&c<='９')c=(char)('0'+(c-'０'));b.append(c);}
  return b.toString().replace("Ⅱ"," II ").replace("Ⅲ"," III ").replace("Ⅳ"," IV ");}
 static int cnNum(String s){if(s==null||s.isEmpty())return 0;if(s.matches("\\d+"))return Integer.parseInt(s);String d="零一二三四五六七八九";
  if(s.equals("十"))return 10;int ten=s.indexOf('十');if(ten>=0){int hi=ten==0?1:d.indexOf(s.charAt(0));int lo=ten==s.length()-1?0:d.indexOf(s.charAt(ten+1));if(hi<0||lo<0)return 0;return hi*10+lo;}
  return s.length()==1?Math.max(0,d.indexOf(s.charAt(0))):0;}
 static int enNum(String s){if(s==null)return 0;String t=s.toLowerCase(Locale.ROOT);if(t.matches("\\d"))return Integer.parseInt(t);if(t.equals("ii")||t.equals("two"))return 2;if(t.equals("iii")||t.equals("three"))return 3;if(t.equals("iv")||t.equals("four"))return 4;return 0;}
 /** 继续观看标题：卡片标题再去掉「· 第 N 集」，本机记录才能和 Emby 的剧名对上。 */
 static String resumeTitle(JSONObject m){String t=HomeRules.cardTitle(m);String cut=EPISODE_SUFFIX.matcher(t).replaceAll("").trim();return cut.isEmpty()?t:cut;}
 static int year(JSONObject m){if(m==null)return 0;for(String f:new String[]{"year","releaseInfo","emby.year"}){Matcher x=YEAR.matcher(m.optString(f));if(x.find())return Integer.parseInt(x.group());}
  Matcher name=YEAR.matcher(m.optString("name"));if(name.find())return Integer.parseInt(name.group());return 0;}
 /** 名字键、年份、续集标记。合并时只算一次。resumeNames 为真时用继续观看标题。 */
 static final class Ident {
  final String key;final int year;final String sequel;
  Ident(JSONObject m){this(m,false);}
  Ident(JSONObject m,boolean resumeNames){String raw=m==null?"":m.optString("name");String titled=resumeNames?resumeTitle(m):raw;key=AggregateRules.key(stripSequel(titled));year=AggregateRules.year(m);sequel=sequelTag(resumeNames?titled:raw);}
  boolean same(Ident o){if(o==null||key.isEmpty()||!key.equals(o.key)||!sequel.equals(o.sequel))return false;return year==0||o.year==0||Math.abs(year-o.year)<=1;}
 }
 static boolean same(JSONObject a,JSONObject b){return new Ident(a).same(new Ident(b));}
 /** 顶层字段浅拷贝。嵌套对象仍共享，避免整段 JSON 再解析一遍。 */
 static JSONObject copy(JSONObject m){JSONObject c=new JSONObject();if(m==null)return c;for(Iterator<String> it=m.keys();it.hasNext();){String k=it.next();try{c.put(k,m.opt(k));}catch(JSONException ignored){}}return c;}
 /** 胜出那条缺海报、简介或横图时，用下一条补上。不改 id，也不把下一条的播放入口拷过来。 */
 static void fillEmpty(JSONObject win,JSONObject other){if(win==null||other==null)return;try{
  if(win.optString("poster").isEmpty()&&!other.optString("poster").isEmpty())win.put("poster",other.optString("poster"));
  if(win.optString("description").isEmpty()&&!other.optString("description").isEmpty())win.put("description",other.optString("description"));
  if(win.optString("logo").isEmpty()&&!other.optString("logo").isEmpty())win.put("logo",other.optString("logo"));
  String bg=win.optString("background"),poster=win.optString("poster"),obg=other.optString("background");
  if(!obg.isEmpty()&&(bg.isEmpty()||(!poster.isEmpty()&&bg.equals(poster))))win.put("background",obg);
 }catch(JSONException ignored){}}
 static long epoch(String s){if(s==null)return 0;String t=s.trim();if(t.isEmpty())return 0;try{
  if(t.matches("(19|20)\\d\\d"))return utc(Integer.parseInt(t),1,1,0,0,0);
  Matcher m=STAMP.matcher(t);if(!m.find())return 0;
  return utc(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(3)),Integer.parseInt(m.group(4)),m.group(5)==null?0:Integer.parseInt(m.group(5)),m.group(6)==null?0:Integer.parseInt(m.group(6)),m.group(7)==null?0:Integer.parseInt(m.group(7)));
 }catch(Exception e){return 0;}}
 static long utc(int y,int mo,int d,int h,int mi,int se){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("UTC"));c.clear();c.set(y,mo-1,d,h,mi,se);return c.getTimeInMillis();}
 static long timeOf(JSONObject m){if(m==null)return 0;long t=m.optLong("agg.time",0);if(t>0)return t;t=epoch(m.optString("released"));if(t>0)return t;return epoch(m.optString("releaseInfo"));}
 static boolean finishedItem(JSONObject m){return m!=null&&(m.optBoolean("emby.played")||m.optBoolean("agg.finished"));}
 static boolean landscape(JSONObject m){if(m==null)return false;String bg=m.optString("background"),poster=m.optString("poster");return !bg.isEmpty()&&!bg.equals(poster);}
 /** 接近看完：进度和时长都已知，且已看到 90%。不用 45 分钟的显示估算。 */
 static boolean almostFinished(long pos,long dur){return pos>0&&dur>0&&pos*10>=dur*9;}
 /** 收藏 / 看完集合里的检索键：id、标题、剧集归并键。 */
 static void markKey(Set<String> set,JSONObject m){if(set==null||m==null)return;String id=m.optString("id");if(!id.isEmpty())set.add("id:"+id);
  String titled=key(resumeTitle(m));if(!titled.isEmpty())set.add("k:"+titled);String raw=key(m.optString("name"));if(!raw.isEmpty())set.add("k:"+raw);
  String show=HomeRules.showKey(m);if(show!=null&&!show.isEmpty())set.add("s:"+show);}
 /** fresh：最近更新按时间排。favorites / finished 是 markKey 写进去的键。 */
 static final class Mix {
  final boolean fresh;final Set<String> favorites,finished;
  Mix(boolean fresh,Set<String> favorites,Set<String> finished){this.fresh=fresh;this.favorites=favorites==null?Collections.<String>emptySet():favorites;this.finished=finished==null?Collections.<String>emptySet():finished;}
  boolean hit(Set<String> set,JSONObject m){if(set==null||set.isEmpty()||m==null)return false;String id=m.optString("id");if(!id.isEmpty()&&set.contains("id:"+id))return true;
   String titled=key(resumeTitle(m));if(!titled.isEmpty()&&set.contains("k:"+titled))return true;String raw=key(m.optString("name"));if(!raw.isEmpty()&&set.contains("k:"+raw))return true;
   String show=HomeRules.showKey(m);return show!=null&&!show.isEmpty()&&set.contains("s:"+show);}
  /** 看完 0，普通 1，收藏且未看完 2。 */
  int band(JSONObject m){if(finishedItem(m)||hit(finished,m))return 0;if(hit(favorites,m))return 2;return 1;}
 }
 static final class Slot {final Ident id;final JSONObject obj;Slot(Ident id,JSONObject obj){this.id=id;this.obj=obj;}}
 /** 不打分的合并：各来源轮流取，和 0.1.130 一样。 */
 static JSONArray merge(List<JSONArray> lists,int limit){return merge(lists,limit,null);}
 /**
  * lists 已按来源优先级排好。同名只留先出现的那条，记下 agg.count，缺的海报和简介用后面的补。
  * mix 不为空时再按收藏 / 普通 / 看完分档，档内轮流，最近更新再按入库时间。
  */
 static JSONArray merge(List<JSONArray> lists,int limit,Mix mix){List<JSONObject> out=new ArrayList<>();int longest=0;if(lists==null)lists=Collections.emptyList();for(JSONArray l:lists)if(l!=null)longest=Math.max(longest,l.length());
  Map<String,List<Slot>> byKey=new HashMap<>();int cap=mix==null?Math.max(0,limit)*3:100000;
  for(int i=0;i<longest&&out.size()<cap;i++)for(int lane=0;lane<lists.size();lane++){JSONArray l=lists.get(lane);if(l==null||i>=l.length())continue;JSONObject m=l.optJSONObject(i);if(m==null||m.optString("name").isEmpty())continue;
   Ident id=new Ident(m);Slot hit=null;if(!id.key.isEmpty()){List<Slot> slots=byKey.get(id.key);if(slots!=null)for(Slot s:slots)if(id.same(s.id)){hit=s;break;}}
   try{if(hit!=null){fillEmpty(hit.obj,m);hit.obj.put("agg.count",hit.obj.optInt("agg.count",1)+1);continue;}JSONObject c=copy(m);c.put("agg.source",source(m));c.put("agg.count",1);if(mix!=null){c.put("agg.lane",lane);if(mix.band(c)==0)c.put("agg.finished",true);}out.add(c);
    if(!id.key.isEmpty()){List<Slot> slots=byKey.get(id.key);if(slots==null)byKey.put(id.key,slots=new ArrayList<Slot>(1));slots.add(new Slot(id,c));}}catch(JSONException ignored){}}
  if(mix!=null)out=arrange(out,mix);
  JSONArray r=new JSONArray();for(int i=0;i<out.size()&&i<limit;i++)r.put(out.get(i));return r;}
 /** 分档后，每一档里按来源下标轮流取。最近更新先把每个来源按时间从新到旧排好。 */
 static List<JSONObject> arrange(List<JSONObject> items,Mix mix){List<JSONObject> out=new ArrayList<>();if(items==null)return out;
  for(int band=2;band>=0;band--){Map<Integer,List<JSONObject>> lanes=new TreeMap<>();
   for(JSONObject m:items){if(m==null||mix.band(m)!=band)continue;int lane=m.optInt("agg.lane",0);List<JSONObject> laneItems=lanes.get(Integer.valueOf(lane));if(laneItems==null)lanes.put(Integer.valueOf(lane),laneItems=new ArrayList<JSONObject>());laneItems.add(m);}
   if(mix.fresh)for(List<JSONObject> laneItems:lanes.values())Collections.sort(laneItems,(a,b)->Long.compare(timeOf(b),timeOf(a)));
   int max=0;for(List<JSONObject> laneItems:lanes.values())max=Math.max(max,laneItems.size());
   for(int i=0;i<max;i++)for(List<JSONObject> laneItems:lanes.values())if(i<laneItems.size())out.add(laneItems.get(i));
  }return out;}
 /** 继续观看：Emby 进度在前，本机记录在后，按卡片标题只留第一条。 */
 static JSONArray resume(JSONArray emby,JSONArray local){JSONArray out=new JSONArray();Map<String,List<Ident>> byKey=new HashMap<>();Map<String,Boolean> ids=new HashMap<>();
  for(JSONArray l:new JSONArray[]{emby,local})for(int i=0;l!=null&&i<l.length();i++){JSONObject m=l.optJSONObject(i);if(m==null)continue;String id=m.optString("id");if(!id.isEmpty()&&ids.containsKey(id))continue;Ident ident=new Ident(m,true);boolean dup=false;
   if(!ident.key.isEmpty()){List<Ident> slots=byKey.get(ident.key);if(slots!=null)for(Ident s:slots)if(ident.same(s)){dup=true;break;}}if(dup)continue;
   if(!id.isEmpty())ids.put(id,Boolean.TRUE);if(!ident.key.isEmpty()){List<Ident> slots=byKey.get(ident.key);if(slots==null)byKey.put(ident.key,slots=new ArrayList<Ident>(1));slots.add(ident);}out.put(m);}return out;}
 /** 多台 Emby 按传入顺序接上，再接本机记录。 */
 static JSONArray orderedResume(List<JSONArray> servers,JSONArray local){JSONArray emby=new JSONArray();if(servers!=null)for(JSONArray l:servers)for(int i=0;l!=null&&i<l.length();i++)emby.put(l.opt(i));return resume(emby,local);}
 static void collectShown(JSONArray items,Set<String> ids,List<Ident> idents){if(items==null)return;for(int i=0;i<items.length();i++){JSONObject m=items.optJSONObject(i);if(m==null)continue;String id=m.optString("id");if(!id.isEmpty()&&ids!=null)ids.add(id);Ident ident=new Ident(m);if(!ident.key.isEmpty()&&idents!=null)idents.add(ident);}}
 /** 丢掉上一行已经出现过的 id，以及同名同年的另一条。 */
 static JSONArray withoutShown(JSONArray items,Set<String> ids,List<Ident> idents){JSONArray out=new JSONArray();if(items==null)return out;if(ids==null)ids=Collections.emptySet();if(idents==null)idents=Collections.emptyList();
  for(int i=0;i<items.length();i++){JSONObject m=items.optJSONObject(i);if(m==null)continue;String id=m.optString("id");if(!id.isEmpty()&&ids.contains(id))continue;Ident ident=new Ident(m);boolean dup=false;if(!ident.key.isEmpty())for(Ident s:idents)if(ident.same(s)){dup=true;break;}if(!dup)out.put(m);}return out;}
 static boolean done(JSONObject m,Set<String> fin){if(m==null||finishedItem(m))return true;if(fin==null||fin.isEmpty())return false;String id=m.optString("id");if(!id.isEmpty()&&fin.contains("id:"+id))return true;String raw=key(m.optString("name"));if(!raw.isEmpty()&&fin.contains("k:"+raw))return true;String titled=key(resumeTitle(m));return !titled.isEmpty()&&fin.contains("k:"+titled);}
 /**
  * 大图最多 5 张。继续观看只占第 1 张；其余要有横图、还没看完，并且来自 Emby 或影视仓。
  * 这两边都没有合格的片子、而且它们都已经拉完时，才用 Stremio 补。
  */
 static JSONArray pickHomeHeroes(JSONArray resume,JSONArray emby,JSONArray tvbox,JSONArray stremio,boolean othersReady){return pickHomeHeroes(resume,emby,tvbox,stremio,othersReady,null);}
 static JSONArray pickHomeHeroes(JSONArray resume,JSONArray emby,JSONArray tvbox,JSONArray stremio,boolean othersReady,Set<String> finished){JSONArray out=new JSONArray();Set<String> ids=new HashSet<>();List<Ident> idents=new ArrayList<>();
  JSONObject cont=null;for(int i=0;resume!=null&&i<resume.length();i++){JSONObject m=resume.optJSONObject(i);if(m==null||done(m,finished))continue;if(m.optString("background").isEmpty()&&m.optString("poster").isEmpty())continue;cont=m;break;}
  if(cont!=null){JSONObject c=copy(cont);try{c.put("agg.hero","resume");c.put("agg.source",source(cont));}catch(JSONException ignored){}out.put(c);remember(c,ids,idents);}
  boolean haveOwn=hasLandscape(emby,finished)||hasLandscape(tvbox,finished)||(cont!=null&&(EMBY.equals(source(cont))||TVBOX.equals(source(cont))));
  JSONArray pool;if(haveOwn){pool=new JSONArray();append(pool,emby);append(pool,tvbox);}else if(othersReady)pool=stremio==null?new JSONArray():stremio;else return out;
  for(int i=0;pool!=null&&i<pool.length()&&out.length()<5;i++){JSONObject m=pool.optJSONObject(i);if(m==null||done(m,finished)||!landscape(m)||seen(m,ids,idents))continue;JSONObject c=copy(m);try{c.put("agg.hero","new");c.put("agg.source",source(m));}catch(JSONException ignored){}out.put(c);remember(c,ids,idents);}
  return out;}
 static boolean hasLandscape(JSONArray items,Set<String> finished){for(int i=0;items!=null&&i<items.length();i++){JSONObject m=items.optJSONObject(i);if(m!=null&&!done(m,finished)&&landscape(m))return true;}return false;}
 static void append(JSONArray out,JSONArray items){for(int i=0;items!=null&&i<items.length();i++)out.put(items.opt(i));}
 static void remember(JSONObject m,Set<String> ids,List<Ident> idents){String id=m.optString("id");if(!id.isEmpty())ids.add(id);Ident ident=new Ident(m);if(!ident.key.isEmpty())idents.add(ident);}
 static boolean seen(JSONObject m,Set<String> ids,List<Ident> idents){String id=m.optString("id");if(!id.isEmpty()&&ids.contains(id))return true;Ident ident=new Ident(m);if(ident.key.isEmpty())return false;for(Ident s:idents)if(ident.same(s))return true;return false;}
 /** 前 5 张的 id 相同才算同一组大图。张数不同也算变了。 */
 static boolean sameHeroIds(JSONArray a,JSONArray b){if(a==null||b==null)return false;int na=Math.min(5,a.length()),nb=Math.min(5,b.length());if(na!=nb)return false;for(int i=0;i<na;i++){JSONObject x=a.optJSONObject(i),y=b.optJSONObject(i);String ix=x==null?"":x.optString("id"),iy=y==null?"":y.optString("id");if(!ix.equals(iy))return false;}return true;}
 /** 聚合大图的小标题。没有 agg 字段的纯 Emby 首页不要走这里。 */
 static String heroKicker(JSONObject m){if(m==null)return "今日推荐";boolean resume="resume".equals(m.optString("agg.hero"));String src=m.optString("agg.source");if(src.isEmpty())src=source(m);return (resume?"继续观看":"推荐")+" · "+label(src);}
 /** 这一次最多再等多少毫秒。deadlineNs ≤ 0 表示不设总时限，用 capMs。到点返回 0。 */
 static int waitMs(long deadlineNs,long nowNs,int capMs){if(deadlineNs<=0)return capMs;long left=(deadlineNs-nowNs)/1000000L;if(left<1)return 0;return left>capMs?capMs:(int)left;}
 /** Emby 首页缓存里「各库最新」的条目数。空库、缺字段都算 0。 */
 static int latestCount(JSONObject data){if(data==null)return 0;JSONArray latest=data.optJSONArray("latest");if(latest==null)return 0;int n=0;for(int i=0;i<latest.length();i++){JSONArray lib=latest.optJSONArray(i);if(lib!=null)n+=lib.length();}return n;}
 /**
  * 聚合拉取预算用尽时，成功返回的 latest 可能是空的，或只剩一两部。
  * 已有缓存里有片子，而这次一条都没有，或不到原来的一半，就视为更弱，调用方不得写盘、也不得换掉已经画上的行。
  */
 static boolean weakerEmbyLatest(JSONObject cached,JSONObject incoming){int oldN=latestCount(cached);if(oldN<=0)return false;int newN=latestCount(incoming);return newN==0||newN*2<oldN;}
 /** writeCache：这次可以落盘。keepRows：行和 embyCatalog 保持已画内容，只收尾这个来源。失败时一律不写盘。 */
 static final class EmbyKeep {final boolean writeCache,keepRows;EmbyKeep(boolean writeCache,boolean keepRows){this.writeCache=writeCache;this.keepRows=keepRows;}}
 static EmbyKeep embyKeep(JSONObject cached,JSONObject incoming,boolean failed){if(failed)return new EmbyKeep(false,latestCount(cached)>0);boolean keep=weakerEmbyLatest(cached,incoming);return new EmbyKeep(!keep,keep);}
 /** 「最近更新」超过这么久才重新请求（毫秒）；下拉刷新时清掉时间戳强制重读。 */
 static final long FRESH_MS=6*60*60*1000L;
 static boolean stale(long savedAt,long now){return savedAt<=0||now-savedAt>=FRESH_MS;}
}
