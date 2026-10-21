package com.guocheng.mediapocket;
import android.graphics.Typeface;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/**
 * 0.1.127 聚合首页（原「融合」）：不再按来源一块一块往下排，而是按内容排——
 * 继续观看 → 最近更新 → 电影 → 剧集 → 动漫 → 纪录片，每一行同时混排 Emby、影视仓、Stremio 的条目，同名去重，
 * 海报左上角标来源。每个来源先用缓存立即画出，后台读到新数据再整行重画；某个来源失败只是这一行少了它。
 * 最底下保留影视仓分类入口和站点切换。
 */
final class AggregateHome {
 final ScrapedMainActivity a;final int ticket;final Map<String,Row> rows=new LinkedHashMap<>();final List<String> order=new ArrayList<>();
 LinearLayout resumeHost,footer;JSONArray local;final Map<String,JSONArray> embyResume=new LinkedHashMap<>();
 AggregateHome(ScrapedMainActivity a){this.a=a;ticket=a.epoch;}

 static void show(ScrapedMainActivity a){new AggregateHome(a).build();}

 boolean alive(){return ticket==a.epoch&&!a.isFinishing()&&a.homeCanvas;}
 void ui(Runnable r){a.runOnUiThread(()->{if(alive())r.run();});}

 void build(){
  JSONArray servers=EmbyStore.list(a);for(int i=0;i<servers.length();i++)order.add(AggregateRules.EMBY+"#"+i);
  boolean tvbox=false;try{tvbox=HomeSources.list(a).length()>0;}catch(Exception ignored){}if(tvbox)order.add(AggregateRules.TVBOX);order.add(AggregateRules.STREMIO);
  // 继续观看：本机记录先画，Emby 进度回来后合并重画
  resumeHost=new LinearLayout(a);resumeHost.setOrientation(1);a.body.addView(resumeHost);
  {JSONArray raw=a.array("history");local=HomeRules.dedupeHistory(raw);if(local.length()!=raw.length())a.prefs.edit().putString("history",local.toString()).apply();}
  paintResume();
  for(String[] r:AggregateRules.ROWS){if(!a.sectionOn("agg-"+r[0]))continue;Row row=new Row(r[0],r[1],r[2]);rows.put(r[0],row);}
  footer=new LinearLayout(a);footer.setOrientation(1);a.body.addView(footer);
  // Stremio 只给电影、剧集、动漫、纪录片；最近更新没有它
  for(Row r:rows.values())if(r.key.equals("fresh"))r.skip(AggregateRules.STREMIO);
  for(int i=0;i<servers.length();i++){JSONObject server=servers.optJSONObject(i);if(server!=null)emby(i,server);}
  if(tvbox)tvbox();
  stremio("movie","movie","");stremio("series","series","");stremio("anime","series","Animation");stremio("doc","movie","Documentary");
 }

 /** 一行：标题 + 海报横排；parts 按来源存条目，任何来源有新结果就整行重画。 */
 final class Row {
  final String key,title,type;final TextView head;final LinearLayout host;final Map<String,JSONArray> parts=new HashMap<>();final Set<String> waiting=new HashSet<>(order);String fusionCategory="";
  Row(String key,String title,String type){this.key=key;this.title=title;this.type=type;head=a.sectionHead("agg-"+key,title+" ›");head.setOnClickListener(v->open());a.body.addView(head);host=new LinearLayout(a);host.setOrientation(1);a.body.addView(host);host.addView(Skeleton.row(a,a.card(),false,4));}
  void open(){if(key.equals("fresh")){if(!fusionCategory.isEmpty())a.openFusion(fusionCategory);return;}if(key.equals("anime"))a.genreBrowse("series","Animation");else if(key.equals("doc"))a.genreBrowse("movie","Documentary");else a.genreBrowse(type,"");}
  void skip(String source){waiting.remove(source);}
  void put(String source,JSONArray items,boolean last){if(items!=null)parts.put(source,items);if(last)waiting.remove(source);paint();}
  void paint(){List<JSONArray> lists=new ArrayList<>();for(String s:order)lists.add(parts.get(s));JSONArray merged=AggregateRules.merge(lists,20);
   if(merged.length()==0){if(waiting.isEmpty()){head.setVisibility(View.GONE);host.setVisibility(View.GONE);}return;}
   head.setVisibility(View.VISIBLE);host.setVisibility(View.VISIBLE);host.removeAllViews();a.shelf(host,merged,type,false);badges(host,merged);}
 }

 /** 海报左上角小标签：来源；几个来源都有时写「EMBY +1」。 */
 void badges(LinearLayout host,JSONArray merged){if(host.getChildCount()==0||!(host.getChildAt(host.getChildCount()-1) instanceof HorizontalScrollView))return;HorizontalScrollView hs=(HorizontalScrollView)host.getChildAt(host.getChildCount()-1);if(hs.getChildCount()==0||!(hs.getChildAt(0) instanceof LinearLayout))return;LinearLayout line=(LinearLayout)hs.getChildAt(0);
  for(int i=0;i<line.getChildCount()&&i<merged.length();i++){View card=line.getChildAt(i);if(!(card instanceof LinearLayout)||((LinearLayout)card).getChildCount()==0||!(((LinearLayout)card).getChildAt(0) instanceof FrameLayout))continue;FrameLayout frame=(FrameLayout)((LinearLayout)card).getChildAt(0);JSONObject m=merged.optJSONObject(i);
   int extra=m.optInt("agg.count",1)-1;TextView tag=new TextView(a);tag.setText(AggregateRules.label(m.optString("agg.source"))+(extra>0?" +"+extra:""));tag.setTextSize(9);tag.setTypeface(Typeface.DEFAULT_BOLD);tag.setTextColor(a.editorial()?a.onHi():0xffffffff);tag.setIncludeFontPadding(false);tag.setPadding(a.dp(5),a.dp(3),a.dp(5),a.dp(3));
   int bg=AggregateRules.EMBY.equals(m.optString("agg.source"))?(a.editorial()?a.hi():0xe62e7d32):0xcc000000;if(bg==0xcc000000)tag.setTextColor(0xffffffff);tag.setBackground(a.rounded(bg,4));FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);lp.setMargins(a.dp(5),a.dp(5),0,0);frame.addView(tag,lp);}}

 void paintResume(){resumeHost.removeAllViews();JSONArray emby=new JSONArray();for(JSONArray l:embyResume.values())for(int i=0;i<l.length();i++)emby.put(l.opt(i));JSONArray merged=AggregateRules.resume(emby,local);if(merged.length()>0)a.homeChrome.continueWatching(resumeHost,merged,"继续观看");}

 // ---------- Emby ----------
 void emby(int index,JSONObject server){String src=AggregateRules.EMBY+"#"+index;
  EmbyHome.IO.execute(()->{JSONObject cached=EmbyHome.readCache(a,server);if(cached!=null){JSONObject c=cached;ui(()->applyEmby(index,src,server,c,false));}
   try{JSONObject data=EmbyHome.fetch(a,server,null);EmbyHome.writeCache(a,server,data);ui(()->applyEmby(index,src,server,data,true));}
   catch(Exception e){DebugLog.event("聚合首页","Emby 读取失败 · "+EmbyApi.explain(e));ui(()->{for(Row r:rows.values())r.put(src,null,true);});}});}
 void applyEmby(int index,String src,JSONObject server,JSONObject data,boolean last){
  JSONArray resume=EmbyHome.metas(a,server,data.optJSONArray("resume"),true);if(EmbyHome.mergeNext(a))resume=EmbyHome.withNextUp(resume,EmbyHome.metas(a,server,data.optJSONArray("next"),false));embyResume.put(src,resume);paintResume();
  JSONArray latest=data.optJSONArray("latest");List<JSONArray> libs=new ArrayList<>();for(int i=0;latest!=null&&i<latest.length();i++)libs.add(EmbyHome.metas(a,server,latest.optJSONArray(i),false));
  JSONArray fresh=AggregateRules.merge(libs,30),movies=new JSONArray(),series=new JSONArray();for(int i=0;i<fresh.length();i++){JSONObject m=fresh.optJSONObject(i);if("series".equals(m.optString("type")))series.put(m);else movies.put(m);}
  for(Row r:rows.values()){JSONArray items=r.key.equals("fresh")?fresh:r.key.equals("movie")?movies:r.key.equals("series")?series:new JSONArray();r.put(src,items,last);}
  if(index==0&&a.heroPriority<4){JSONArray heroes=new JSONArray();JSONArray picks=data.optJSONArray("heroes");if(picks==null)picks=new JSONArray();for(int i=0;i<picks.length()&&heroes.length()<5;i++){JSONObject item=picks.optJSONObject(i);JSONObject m=EmbyHome.card(a,server,item,false);if(m==null)continue;String t=item.optString("Type");if("Movie".equals(t)||"Episode".equals(t))try{m.put("emby.directPlay",true);}catch(Exception ignored){}heroes.put(m);}
   if(heroes.length()>0){a.heroPriority=4;a.setHeroes(heroes,heroes.optJSONObject(0).optString("type","movie"));}}}
 static JSONArray flat(JSONArray latest){JSONArray out=new JSONArray();for(int i=0;latest!=null&&i<latest.length();i++){JSONArray l=latest.optJSONArray(i);for(int j=0;l!=null&&j<l.length();j++)out.put(l.opt(j));}return out;}

 // ---------- 影视仓 ----------
 void tvbox(){String src=AggregateRules.TVBOX;final String sourceId=a.prefs.getString("home.source.v1","fty");
  FusionCatalog.IO.execute(()->{try{FusionCatalog.Session session=HomeSources.session(a,sourceId);String homeKey="fusion.home:"+session.url+"|"+session.site.optString("key");JSONObject response=null;String saved=a.prefs.getString(homeKey,"");if(!saved.isEmpty())try{response=new JSONObject(saved);}catch(Exception ignored){}
    if(response==null||response.optJSONArray("class")==null||response.optJSONArray("class").length()==0){response=FusionCatalog.request(a,session,"home","","",1);}
    JSONArray categories=response.optJSONArray("class");if(categories==null||categories.length()==0)throw new Exception(session.site.optString("name")+"：站点没有返回分类");a.prefs.edit().putString(homeKey,response.toString()).apply();
    Map<String,JSONObject> picked=new LinkedHashMap<>();for(int i=0;i<categories.length();i++){JSONObject c=categories.optJSONObject(i);if(c==null)continue;String row=AggregateRules.rowForCategory(c.optString("type_name"));if(!row.isEmpty()&&!picked.containsKey(row))picked.put(row,c);}
    ui(()->{a.fusionSession=session;footer(session,categories);for(Row r:rows.values()){JSONObject c=picked.get(r.key);if(c==null){r.put(src,null,true);continue;}r.fusionCategory=c.optString("type_id");}});
    for(Map.Entry<String,JSONObject> e:picked.entrySet())category(session,e.getKey(),e.getValue());
   }catch(Exception e){String why=e.getMessage()==null?"站点暂未响应":e.getMessage();ui(()->{for(Row r:rows.values())r.put(src,null,true);footerError(why);});}});}
 void category(FusionCatalog.Session s,String rowKey,JSONObject c){String src=AggregateRules.TVBOX,cat=c.optString("type_id"),key=FusionCatalog.cacheKey(s,cat);
  JSONArray cached=null;try{cached=new JSONArray(a.prefs.getString(key,""));}catch(Exception ignored){}boolean fresh=cached!=null&&cached.length()>0&&!AggregateRules.stale(a.prefs.getLong(key+".time",0),System.currentTimeMillis());
  if(cached!=null&&cached.length()>0){JSONArray c0=cached;ui(()->{Row r=rows.get(rowKey);if(r!=null)r.put(src,c0,fresh);heroFrom(c0,3);});}if(fresh)return;
  try{JSONObject r=FusionCatalog.request(a,s,"category",cat,"",1);JSONArray items=FusionCatalog.normalize(s,r.optJSONArray("list"),cat);a.prefs.edit().putString(key,items.toString()).putLong(key+".time",System.currentTimeMillis()).apply();ui(()->{Row row=rows.get(rowKey);if(row!=null)row.put(src,items,true);heroFrom(items,3);});}
  catch(Exception e){ui(()->{Row row=rows.get(rowKey);if(row!=null)row.put(src,null,true);});}}
 void footer(FusionCatalog.Session session,JSONArray categories){footer.removeAllViews();LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);TextView h=a.heading("影视仓分类 · "+session.site.optString("name","影视仓"));head.addView(h,new LinearLayout.LayoutParams(0,-2,1f));TextView more=a.text("切换站点",13,a.muted());more.setPadding(a.dp(12),a.dp(6),a.dp(4),a.dp(6));more.setOnClickListener(v->a.chooseFusionSite());head.addView(more);footer.addView(head);
  HorizontalScrollView hs=new HorizontalScrollView(a);hs.setHorizontalScrollBarEnabled(false);LinearLayout line=new LinearLayout(a);hs.addView(line);footer.addView(hs);
  for(int j=0;j<categories.length();j++){JSONObject c=categories.optJSONObject(j);if(c==null)continue;TextView tile=a.text(c.optString("type_name"),14,a.ink());tile.setGravity(Gravity.CENTER);tile.setPadding(a.dp(18),0,a.dp(18),0);tile.setSingleLine(true);tile.setBackground(a.rounded(a.light?0x14000000:0x24ffffff,20));tile.setOnClickListener(v->a.openFusion(c.optString("type_id")));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,a.dp(40));lp.rightMargin=a.dp(8);line.addView(tile,lp);}}
 void footerError(String why){footer.removeAllViews();footer.addView(a.heading("影视仓"));footer.addView(a.text("读取失败："+why,12,a.muted()));footer.addView(a.button("切换站点",()->a.chooseFusionSite()));footer.addView(a.button("管理接口",()->a.startActivity(new android.content.Intent(a,TvBoxActivity.class))));}

 // ---------- Stremio ----------
 void stremio(String rowKey,String type,String genre){Row row=rows.get(rowKey);if(row==null)return;String src=AggregateRules.STREMIO,key="cinema.catalog."+type+"."+genre;
  JSONArray cached=null;try{cached=new JSONArray(a.prefs.getString(key,""));}catch(Exception ignored){}boolean fresh=cached!=null&&cached.length()>0&&!AggregateRules.stale(a.prefs.getLong(key+".time",0),System.currentTimeMillis());
  if(cached!=null&&cached.length()>0){row.put(src,cached,fresh);heroFrom(cached,genre.isEmpty()?(type.equals("series")?2:1):0);}if(fresh)return;
  a.job(()->{try{JSONArray items=CinemaCatalogClient.json("https://v3-cinemeta.strem.io/catalog/"+type+"/top"+(genre.isEmpty()?"":"/genre="+Net.esc(genre))+".json").optJSONArray("metas");if(items==null||items.length()==0)throw new IllegalStateException("empty");a.prefs.edit().putString(key,items.toString()).putLong(key+".time",System.currentTimeMillis()).apply();ui(()->{row.put(src,items,true);heroFrom(items,genre.isEmpty()?(type.equals("series")?2:1):0);});}
   catch(Exception e){ui(()->row.put(src,null,true));}});}

 /** 大图：Emby 优先（4），其次影视仓（3），再次 Stremio 热门（1–2）。 */
 void heroFrom(JSONArray items,int priority){if(items==null||items.length()==0||priority<=a.heroPriority)return;a.heroPriority=priority;JSONObject first=items.optJSONObject(0);a.setHeroes(items,first!=null&&"series".equals(first.optString("type"))?"series":"movie");}
}
