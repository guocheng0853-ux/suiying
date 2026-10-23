package com.guocheng.mediapocket;
import android.graphics.Typeface;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;import java.util.concurrent.*;
/**
 * 聚合首页：继续观看 → 最近更新 → 电影 → 剧集 → 动漫 → 纪录片。
 * 每一行混排 Emby、影视仓、Stremio，同名去重，海报左上角标来源。
 * 0.1.130：合并在 agg-merge 单线程；来源到达后 300ms 合并一次；内容没变不重画。
 * 0.1.136：各行按类型另取；第一次画出后，再等来源安静约半秒才画最终结果。
 */
final class AggregateHome {
 static final ExecutorService MERGE=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"agg-merge");t.setDaemon(true);return t;});
 static final long EMBY_BUDGET_MS=12000L;
 final ScrapedMainActivity a;final int ticket;final Map<String,Row> rows=new LinkedHashMap<>();final List<String> order=new ArrayList<>();
 LinearLayout resumeHost,footer;volatile JSONArray local;final Map<String,JSONArray> embyResume=new LinkedHashMap<>();
 final Map<String,JSONArray> embyCatalog=new LinkedHashMap<>();final Map<String,JSONArray> stremioParts=new LinkedHashMap<>();
 final Set<String> embyClosed=new HashSet<>();JSONArray heroResume=new JSONArray(),heroEmby=new JSONArray(),heroTv=new JSONArray(),heroSt=new JSONArray();
 int embyCount,resumeGen,heroGen;String resumePainted="";boolean tvConfigured,tvFinished;
 AggregateHome(ScrapedMainActivity a){this.a=a;ticket=a.epoch;}

 static void show(ScrapedMainActivity a){new AggregateHome(a).build();}

 boolean alive(){return ticket==a.epoch&&!a.isFinishing()&&a.homeCanvas;}
 void ui(Runnable r){a.runOnUiThread(()->{if(alive())r.run();});}

 void build(){
  JSONArray servers=EmbyStore.list(a);embyCount=servers.length();for(int i=0;i<servers.length();i++)order.add(AggregateRules.EMBY+"#"+i);
  boolean tvbox=false;try{tvbox=HomeSources.list(a).length()>0;}catch(Exception ignored){}tvConfigured=tvbox;if(!tvConfigured)tvFinished=true;if(tvbox)order.add(AggregateRules.TVBOX);order.add(AggregateRules.STREMIO);
  resumeHost=new LinearLayout(a);resumeHost.setOrientation(1);a.body.addView(resumeHost);
  MERGE.execute(()->{JSONArray raw;try{raw=new JSONArray(a.prefs.getString("history","[]"));}catch(Exception e){raw=new JSONArray();}JSONArray deduped=HomeRules.dedupeHistory(raw);if(deduped.length()!=raw.length())a.prefs.edit().putString("history",deduped.toString()).apply();local=deduped;ui(this::scheduleResume);});
  for(String[] r:AggregateRules.ROWS){if(!a.sectionOn("agg-"+r[0]))continue;rows.put(r[0],new Row(r[0],r[1],r[2]));}
  footer=new LinearLayout(a);footer.setOrientation(1);a.body.addView(footer);
  for(Row r:rows.values())if(r.key.equals("fresh"))r.skip(AggregateRules.STREMIO);
  for(int i=0;i<servers.length();i++){JSONObject server=servers.optJSONObject(i);if(server!=null)emby(i,server);}
  if(tvbox)tvbox();
  stremio("movie","movie","");stremio("series","series","");stremio("anime","series","Animation");stremio("doc","movie","Documentary");
 }

 /** 一行海报。opened 在真正开画时同步置上，之后的到达要等约半秒安静再画。 */
 final class Row {
  final String key,title,type;final TextView head;final LinearLayout host;final Map<String,JSONArray> parts=new HashMap<>();final Set<String> waiting=new HashSet<>(order);String fusionCategory="",painted="";JSONArray lastMerged;int paintGen;boolean opened;
  final Runnable paintLater=this::paintNow;
  Row(String key,String title,String type){this.key=key;this.title=title;this.type=type;head=a.sectionHead("agg-"+key,title+" ›");head.setOnClickListener(v->open());a.body.addView(head);host=new LinearLayout(a);host.setOrientation(1);a.body.addView(host);host.addView(Skeleton.row(a,a.card(),false,4));}
  void open(){if(key.equals("fresh")){if(!fusionCategory.isEmpty())a.openFusion(fusionCategory);return;}if(key.equals("anime"))a.genreBrowse("series","Animation");else if(key.equals("doc"))a.genreBrowse("movie","Documentary");else a.genreBrowse(type,"");}
  void skip(String source){waiting.remove(source);if(waiting.isEmpty())schedule();}
  void put(String source,JSONArray items,boolean last){if(items!=null)parts.put(source,items);if(last)waiting.remove(source);schedule();}
  void schedule(){a.carousel.removeCallbacks(paintLater);if(!opened){if(waiting.isEmpty())paintNow();else a.carousel.postDelayed(paintLater,300);}else a.carousel.postDelayed(paintLater,500);}
  void paintNow(){a.carousel.removeCallbacks(paintLater);opened=true;final List<JSONArray> lists=new ArrayList<>();for(String s:order)lists.add(parts.get(s));final List<JSONArray> earlier=new ArrayList<>();for(Row r:rows.values()){if(r==this)break;if(r.lastMerged!=null)earlier.add(r.lastMerged);}final int gen=++paintGen;final boolean done=waiting.isEmpty();final boolean freshRow=key.equals("fresh");
   MERGE.execute(()->{JSONArray merged=AggregateRules.merge(lists,80,taste(freshRow));Set<String> ids=new HashSet<>();List<AggregateRules.Ident> idents=new ArrayList<>();for(JSONArray prev:earlier)AggregateRules.collectShown(prev,ids,idents);JSONArray filtered=AggregateRules.withoutShown(merged,ids,idents);JSONArray capped=new JSONArray();for(int i=0;i<filtered.length()&&i<20;i++)capped.put(filtered.opt(i));ui(()->applyPaint(gen,capped,done));});}
  void applyPaint(int gen,JSONArray merged,boolean done){if(gen!=paintGen||merged==null)return;
   if(merged.length()==0){if(!done)return;boolean changed=lastMerged!=null&&lastMerged.length()>0;lastMerged=merged;painted="";Skeleton.clear(host);head.setVisibility(View.GONE);host.setVisibility(View.GONE);if(changed)cascade();return;}
   StringBuilder sig=new StringBuilder();for(int i=0;i<merged.length();i++){JSONObject m=merged.optJSONObject(i);if(m==null)continue;sig.append(m.optString("id")).append('|').append(m.optInt("agg.count")).append(';');}String next=sig.toString();if(next.equals(painted))return;painted=next;lastMerged=merged;
   head.setVisibility(View.VISIBLE);host.setVisibility(View.VISIBLE);host.removeAllViews();a.shelf(host,merged,type,false);badges(host,merged);cascade();}
  void cascade(){boolean after=false;for(Row r:rows.values()){if(after)r.schedule();if(r==this)after=true;}}
 }

 /** 海报左上角来源；看完的片子右上角标「已看完」。 */
 void badges(LinearLayout host,JSONArray merged){if(host.getChildCount()==0||!(host.getChildAt(host.getChildCount()-1) instanceof HorizontalScrollView))return;HorizontalScrollView hs=(HorizontalScrollView)host.getChildAt(host.getChildCount()-1);if(hs.getChildCount()==0||!(hs.getChildAt(0) instanceof LinearLayout))return;LinearLayout line=(LinearLayout)hs.getChildAt(0);
  for(int i=0;i<line.getChildCount()&&i<merged.length();i++){View card=line.getChildAt(i);if(!(card instanceof LinearLayout)||((LinearLayout)card).getChildCount()==0||!(((LinearLayout)card).getChildAt(0) instanceof FrameLayout))continue;FrameLayout frame=(FrameLayout)((LinearLayout)card).getChildAt(0);JSONObject m=merged.optJSONObject(i);if(m==null)continue;
   int extra=m.optInt("agg.count",1)-1;TextView tag=new TextView(a);tag.setText(AggregateRules.label(m.optString("agg.source"))+(extra>0?" +"+extra:""));tag.setTextSize(9);tag.setTypeface(Typeface.DEFAULT_BOLD);tag.setTextColor(a.editorial()?a.onHi():0xffffffff);tag.setIncludeFontPadding(false);tag.setPadding(a.dp(5),a.dp(3),a.dp(5),a.dp(3));
   int bg=AggregateRules.EMBY.equals(m.optString("agg.source"))?(a.editorial()?a.hi():0xe62e7d32):0xcc000000;if(bg==0xcc000000)tag.setTextColor(0xffffffff);tag.setBackground(a.rounded(bg,4));FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);lp.setMargins(a.dp(5),a.dp(5),0,0);frame.addView(tag,lp);
   if(m.optBoolean("agg.finished")||m.optBoolean("emby.played")){TextView done=new TextView(a);done.setText("已看完");done.setTextSize(9);done.setTypeface(Typeface.DEFAULT_BOLD);done.setTextColor(0xffffffff);done.setIncludeFontPadding(false);done.setPadding(a.dp(5),a.dp(3),a.dp(5),a.dp(3));done.setBackground(a.rounded(0xcc000000,4));FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);rp.setMargins(0,a.dp(5),a.dp(5),0);frame.addView(done,rp);}}
 }

 /** 继续观看按服务器列表顺序合并，本机快看完的不进这一行。内容没变不重画。 */
 void scheduleResume(){final int gen=++resumeGen;final List<JSONArray> servers=new ArrayList<>();for(String s:order)if(s.startsWith(AggregateRules.EMBY))servers.add(embyResume.get(s));final JSONArray hist=local;
  MERGE.execute(()->{JSONArray src=hist;if(src==null){try{src=HomeRules.dedupeHistory(new JSONArray(a.prefs.getString("history","[]")));}catch(Exception e){src=new JSONArray();}}JSONArray kept=new JSONArray();for(int i=0;src!=null&&i<src.length();i++){JSONObject m=src.optJSONObject(i);if(m==null||historyAlmostDone(m))continue;kept.put(m);}
   JSONArray merged=AggregateRules.orderedResume(servers,kept);StringBuilder sig=new StringBuilder();for(int i=0;i<merged.length();i++){JSONObject m=merged.optJSONObject(i);if(m!=null)sig.append(m.optString("id")).append(';');}String next=sig.toString();
   ui(()->{if(gen!=resumeGen)return;heroResume=merged;if(!next.equals(resumePainted)){resumePainted=next;resumeHost.removeAllViews();if(merged.length()>0){if(a.homeChrome==null)a.homeChrome=new HomeChrome(a);a.homeChrome.continueWatching(resumeHost,merged,"继续观看");}}scheduleHero();});});}
 boolean historyAlmostDone(JSONObject m){String id=m.optString("id"),ep=m.optString("lastEpisode",id);int pos=a.prefs.getInt("pos:"+ep,0),dur=a.prefs.getInt("dur:"+ep,0);if(pos<=0&&!id.isEmpty()){pos=a.prefs.getInt("pos:"+id,0);dur=a.prefs.getInt("dur:"+id,dur);}return AggregateRules.almostFinished(pos,dur);}
 AggregateRules.Mix taste(boolean fresh){Set<String> fav=new HashSet<>(),fin=new HashSet<>();try{JSONArray favorites=new JSONArray(a.prefs.getString("favorites","[]"));for(int i=0;i<favorites.length();i++)AggregateRules.markKey(fav,favorites.optJSONObject(i));}catch(Exception ignored){}
  JSONArray hist=local;if(hist==null){try{hist=HomeRules.dedupeHistory(new JSONArray(a.prefs.getString("history","[]")));}catch(Exception e){hist=new JSONArray();}}for(int i=0;hist!=null&&i<hist.length();i++){JSONObject m=hist.optJSONObject(i);if(m!=null&&historyAlmostDone(m))AggregateRules.markKey(fin,m);}return new AggregateRules.Mix(fresh,fav,fin);}
 Set<String> finishedKeys(){return taste(false).finished;}

 void scheduleHero(){final int gen=++heroGen;final JSONArray resume=heroResume,emby=heroEmby,tv=heroTv,st=heroSt;final boolean ready=embyReady()&&tvFinished;
  MERGE.execute(()->{JSONArray heroes=AggregateRules.pickHomeHeroes(resume,emby,tv,st,ready,finishedKeys());ui(()->{if(gen!=heroGen||heroes.length()==0)return;JSONObject first=heroes.optJSONObject(0);a.offerHeroes(heroes,first!=null&&"series".equals(first.optString("type"))?"series":"movie",4);});});}
 boolean embyReady(){return embyCount==0||embyClosed.size()>=embyCount;}
 void rebuildEmbyHero(){JSONArray all=new JSONArray();for(String s:order){if(!s.startsWith(AggregateRules.EMBY))continue;JSONArray p=embyCatalog.get(s);for(int i=0;p!=null&&i<p.length();i++)all.put(p.opt(i));}heroEmby=all;}
 void rebuildStremioHero(){JSONArray all=new JSONArray();for(String[] r:AggregateRules.ROWS){JSONArray p=stremioParts.get(r[0]);for(int i=0;p!=null&&i<p.length();i++)all.put(p.opt(i));}heroSt=all;}

 // ---------- Emby ----------
 void emby(int index,JSONObject server){final String src=AggregateRules.EMBY+"#"+index;
  EmbyHome.IO.execute(()->{JSONObject cached=EmbyHome.readCache(a,server);if(cached!=null){EmbySlice slice=slice(server,cached);ui(()->applyEmby(src,slice,true,false));}
   try{JSONObject data=EmbyHome.fetch(a,server,partial->{EmbySlice early=slice(server,partial);ui(()->applyEmby(src,early,false,false));},EMBY_BUDGET_MS);EmbyHome.writeCache(a,server,data);EmbySlice slice=slice(server,data);ui(()->applyEmby(src,slice,true,true));}
   catch(Exception e){DebugLog.event("聚合首页","Emby 读取失败 · "+EmbyApi.explain(e));ui(()->finishEmby(src));}});}
 static final class EmbySlice {final JSONArray resume,fresh,movies,series,anime,doc;EmbySlice(JSONArray resume,JSONArray fresh,JSONArray movies,JSONArray series,JSONArray anime,JSONArray doc){this.resume=resume;this.fresh=fresh;this.movies=movies;this.series=series;this.anime=anime;this.doc=doc;}
  JSONArray forRow(String key){if("fresh".equals(key))return fresh;if("movie".equals(key))return movies;if("series".equals(key))return series;if("anime".equals(key))return anime;if("doc".equals(key))return doc;return new JSONArray();}}
 /** 各库按类型归行，同时都作为「最近更新」的候选。不再把 30 条混合结果切成电影和剧集。 */
 EmbySlice slice(JSONObject server,JSONObject data){if(data==null)data=new JSONObject();android.content.SharedPreferences.Editor ed=a.prefs.edit();boolean[] wrote={false};
  JSONArray resume=EmbyHome.metas(a,server,data.optJSONArray("resume"),true,ed,wrote);if(EmbyHome.mergeNext(a))resume=EmbyHome.withNextUp(resume,EmbyHome.metas(a,server,data.optJSONArray("next"),false,ed,wrote));
  JSONArray latest=data.optJSONArray("latest"),libs=data.optJSONArray("libs"),fresh=new JSONArray(),movies=new JSONArray(),series=new JSONArray(),anime=new JSONArray(),doc=new JSONArray();
  for(int i=0;latest!=null&&i<latest.length();i++){JSONObject lib=libs!=null&&i<libs.length()?libs.optJSONObject(i):null;String name=lib==null?"":lib.optString("Name"),ctype=lib==null?"":lib.optString("CollectionType");if(AggregateRules.blockedLibrary(ctype,name))continue;
   String libraryRow=AggregateRules.rowForEmby(null,name,ctype);if(!"anime".equals(libraryRow)&&!"doc".equals(libraryRow))libraryRow="";JSONArray cards=EmbyHome.metas(a,server,latest.optJSONArray(i),false,ed,wrote);
   for(int j=0;j<cards.length();j++){JSONObject m=cards.optJSONObject(j);if(m==null)continue;fresh.put(m);String row=libraryRow.isEmpty()?AggregateRules.rowForEmby(m,"",""):libraryRow;if("anime".equals(row))anime.put(m);else if("doc".equals(row))doc.put(m);else if("series".equals(row))series.put(m);else movies.put(m);}}
  if(wrote[0])ed.apply();return new EmbySlice(resume,fresh,movies,series,anime,doc);}
 void applyEmby(String src,EmbySlice slice,boolean rowsOn,boolean last){if(slice==null)return;if(slice.resume!=null)embyResume.put(src,slice.resume);scheduleResume();
  if(rowsOn&&slice.fresh!=null&&slice.fresh.length()>0){embyCatalog.put(src,slice.fresh);rebuildEmbyHero();for(Row r:rows.values())r.put(src,slice.forRow(r.key),last);}
  else if(rowsOn&&last){for(Row r:rows.values())r.put(src,null,true);}
  if(last)embyClosed.add(src);scheduleHero();}
 void finishEmby(String src){for(Row r:rows.values())r.put(src,null,true);embyClosed.add(src);scheduleHero();}

 // ---------- 影视仓 ----------
 void tvbox(){final String sourceId=a.prefs.getString("home.source.v1","fty");
  FusionCatalog.IO.execute(()->{try{FusionCatalog.Session session=HomeSources.session(a,sourceId);CacheFiles files=CacheFiles.of(a);String homeKey="fusion.home:"+session.url+"|"+session.site.optString("key");long now=System.currentTimeMillis();
    JSONObject cached=null;String saved=files.readMigrating(a,homeKey);if(saved!=null&&!saved.isEmpty())try{cached=new JSONObject(saved);}catch(Exception ignored){}
    boolean cachedOk=cached!=null&&cached.optJSONArray("class")!=null&&cached.optJSONArray("class").length()>0;boolean homeFresh=cachedOk&&files.fresh(homeKey,now,AggregateRules.FRESH_MS);JSONObject response=cachedOk?cached:null;
    if(!homeFresh){try{JSONObject net=FusionCatalog.request(a,session,"home","","",1);JSONArray cls=net==null?null:net.optJSONArray("class");if(cls!=null&&cls.length()>0){response=net;String body=net.toString();if(!files.write(homeKey,body)){String current=files.read(homeKey);if(body.equals(current))files.touch(homeKey,System.currentTimeMillis());}}else if(!cachedOk)throw new Exception(session.site.optString("name")+"：站点没有返回分类");}
     catch(Exception e){if(!cachedOk)throw e;response=cached;}}
    JSONArray categories=response==null?null:response.optJSONArray("class");if(categories==null||categories.length()==0)throw new Exception(session.site.optString("name")+"：站点没有返回分类");
    final Map<String,JSONArray> grouped=AggregateRules.groupCategories(categories);final FusionCatalog.Session sessionFinal=session;final JSONArray categoriesFinal=categories;
    ui(()->{a.fusionSession=sessionFinal;footer(sessionFinal,categoriesFinal);for(String rowKey:AggregateRules.FETCH_ROWS){Row row=rows.get(rowKey);if(row==null)continue;JSONArray cats=grouped.get(rowKey);if(cats==null||cats.length()==0)row.put(AggregateRules.TVBOX,null,true);else row.fusionCategory=cats.optJSONObject(0).optString("type_id");}});
    Map<String,JSONArray> tvParts=new LinkedHashMap<>();
    for(String rowKey:AggregateRules.FETCH_ROWS){if(rows.get(rowKey)==null)continue;JSONArray cats=grouped.get(rowKey);if(cats==null||cats.length()==0)continue;JSONArray[] buckets=new JSONArray[cats.length()];boolean[] fresh=new boolean[cats.length()];
     for(int i=0;i<cats.length();i++){JSONObject c=cats.optJSONObject(i);String cat=c==null?"":c.optString("type_id");String key=FusionCatalog.cacheKey(session,cat);JSONArray hit=readArray(files,key);fresh[i]=hit!=null&&hit.length()>0&&files.fresh(key,System.currentTimeMillis(),AggregateRules.FRESH_MS);buckets[i]=hit;}
     publishTv(rowKey,buckets,allFresh(fresh),tvParts);
     for(int i=0;i<cats.length();i++){if(fresh[i])continue;JSONObject c=cats.optJSONObject(i);String cat=c==null?"":c.optString("type_id");JSONArray fetched=fetchCategory(session,files,cat);if(fetched!=null)buckets[i]=fetched;fresh[i]=true;publishTv(rowKey,buckets,allFresh(fresh),tvParts);}}
    ui(()->{tvFinished=true;scheduleHero();});
   }catch(Exception e){String why=e.getMessage()==null?"站点暂未响应":e.getMessage();ui(()->{for(Row r:rows.values())r.put(AggregateRules.TVBOX,null,true);footerError(why);tvFinished=true;scheduleHero();});}});}
 static boolean allFresh(boolean[] fresh){for(boolean f:fresh)if(!f)return false;return true;}
 JSONArray readArray(CacheFiles files,String key){String raw=files.readMigrating(a,key);if(raw==null||raw.isEmpty())return null;try{return new JSONArray(raw);}catch(Exception e){return null;}}
 JSONArray fetchCategory(FusionCatalog.Session s,CacheFiles files,String cat){try{JSONObject r=FusionCatalog.request(a,s,"category",cat,"",1);JSONArray items=FusionCatalog.normalize(s,r.optJSONArray("list"),cat);files.write(FusionCatalog.cacheKey(s,cat),items.toString());return items;}catch(Exception e){return null;}}
 void publishTv(String rowKey,JSONArray[] buckets,boolean last,Map<String,JSONArray> tvParts){JSONArray combined=new JSONArray();for(JSONArray b:buckets)for(int j=0;b!=null&&j<b.length();j++)combined.put(b.opt(j));tvParts.put(rowKey,combined);JSONArray hero=new JSONArray();for(JSONArray p:tvParts.values())for(int i=0;p!=null&&i<p.length();i++)hero.put(p.opt(i));final JSONArray rowSnap=combined,heroSnap=hero;final boolean lastFinal=last;final String keyFinal=rowKey;
  ui(()->{Row row=rows.get(keyFinal);if(row!=null)row.put(AggregateRules.TVBOX,rowSnap,lastFinal);heroTv=heroSnap;scheduleHero();});}
 void footer(FusionCatalog.Session session,JSONArray categories){footer.removeAllViews();LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);TextView h=a.heading("影视仓分类 · "+session.site.optString("name","影视仓"));head.addView(h,new LinearLayout.LayoutParams(0,-2,1f));TextView more=a.text("切换站点",13,a.muted());more.setPadding(a.dp(12),a.dp(6),a.dp(4),a.dp(6));more.setOnClickListener(v->a.chooseFusionSite());head.addView(more);footer.addView(head);
  HorizontalScrollView hs=new HorizontalScrollView(a);hs.setHorizontalScrollBarEnabled(false);LinearLayout line=new LinearLayout(a);hs.addView(line);footer.addView(hs);
  for(int j=0;j<categories.length();j++){JSONObject c=categories.optJSONObject(j);if(c==null)continue;TextView tile=a.text(c.optString("type_name"),14,a.ink());tile.setGravity(Gravity.CENTER);tile.setPadding(a.dp(18),0,a.dp(18),0);tile.setSingleLine(true);tile.setBackground(a.rounded(a.light?0x14000000:0x24ffffff,20));tile.setOnClickListener(v->a.openFusion(c.optString("type_id")));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,a.dp(40));lp.rightMargin=a.dp(8);line.addView(tile,lp);}}
 void footerError(String why){footer.removeAllViews();footer.addView(a.heading("影视仓"));footer.addView(a.text("读取失败："+why,12,a.muted()));footer.addView(a.button("切换站点",()->a.chooseFusionSite()));footer.addView(a.button("管理接口",()->a.startActivity(new android.content.Intent(a,TvBoxActivity.class))));}

 // ---------- Stremio ----------
 void stremio(String rowKey,String type,String genre){Row row=rows.get(rowKey);if(row==null)return;String src=AggregateRules.STREMIO,key="cinema.catalog."+type+"."+genre;
  a.job(()->{CacheFiles files=CacheFiles.of(a);JSONArray cached=null;String raw=files.readMigrating(a,key);try{if(raw!=null&&!raw.isEmpty())cached=new JSONArray(raw);}catch(Exception ignored){}boolean fresh=cached!=null&&cached.length()>0&&files.fresh(key,System.currentTimeMillis(),AggregateRules.FRESH_MS);
   if(cached!=null&&cached.length()>0){JSONArray shown=cached;ui(()->{row.put(src,shown,fresh);noteStremio(rowKey,shown);});if(fresh)return;}
   try{JSONArray items=CinemaCatalogClient.json("https://v3-cinemeta.strem.io/catalog/"+type+"/top"+(genre.isEmpty()?"":"/genre="+Net.esc(genre))+".json").optJSONArray("metas");if(items==null||items.length()==0)throw new IllegalStateException("empty");files.write(key,items.toString());ui(()->{row.put(src,items,true);noteStremio(rowKey,items);});}
   catch(Exception e){ui(()->row.put(src,null,true));}});}
 void noteStremio(String rowKey,JSONArray items){if(items!=null)stremioParts.put(rowKey,items);rebuildStremioHero();scheduleHero();}
}
