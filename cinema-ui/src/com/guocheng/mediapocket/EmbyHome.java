package com.guocheng.mediapocket;
import android.widget.*;import org.json.*;import java.util.*;import java.util.concurrent.*;
/** 首页上的 Emby：按服务器首页分区排列继续观看、下一集和各库最新。没有的数据就跳过。 */
final class EmbyHome {
 static final ExecutorService IO=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"emby-home");t.setDaemon(true);return t;});
 private EmbyHome(){}
 /**
  * 0.1.105：首页 Emby 分区先用上次的结果立刻画出来，同时后台刷新，数据有变化才重画。
  * 刷新时第一个请求单独发（令牌过期会在这里重新登录），其余请求和各库「最新」并行，不再一个接一个等。
  */
 static void show(ScrapedMainActivity a){final int ticket=a.epoch;JSONObject server=selected(a);LinearLayout target=section(a);if(server==null){target.addView(a.heading("Emby"));target.addView(a.text("还没有 Emby 服务器，请到设置里添加",13,a.muted()));target.addView(a.button("添加 Emby 服务器",()->EmbyLibraryActivity.manage(a)));return;}
  target.addView(a.text("正在读取 "+EmbyStore.label(server)+" …",12,a.muted()));
  // 0.1.123：没有缓存时先放骨架（继续观看横卡 + 两行海报），缓存或结果一到 render() 会整体替换
  target.addView(Skeleton.shelf(a,a.card(),true,3));target.addView(Skeleton.shelf(a,a.card(),false,4));target.addView(Skeleton.shelf(a,a.card(),false,4));
  final boolean[] done={false};final long started=System.currentTimeMillis();
  IO.execute(()->{final JSONObject cached=readCache(a,server);if(cached!=null)a.runOnUiThread(()->{if(done[0]||ticket!=a.epoch||a.isFinishing())return;render(a,server,target,cached);});
   try{JSONObject data=fetch(a,server,cached!=null?null:partial->a.runOnUiThread(()->{if(done[0]||ticket!=a.epoch||a.isFinishing())return;render(a,server,target,partial);target.addView(a.text("正在读取更多分区…",12,a.muted()));}));done[0]=true;writeCache(a,server,data);DebugLog.event("Emby 首页","读取用时 "+(System.currentTimeMillis()-started)/100/10.0+" 秒"+(cached!=null?"（先显示了缓存）":""));
    a.runOnUiThread(()->{if(ticket!=a.epoch||a.isFinishing())return;if(cached!=null&&cached.toString().equals(data.toString()))return;render(a,server,target,data);});
   }catch(Exception e){done[0]=true;DebugLog.event("Emby 首页","读取失败 · "+EmbyApi.explain(e));a.runOnUiThread(()->{if(ticket!=a.epoch||a.isFinishing())return;if(cached!=null){boolean hinted=false;for(int i=0;i<target.getChildCount();i++)if("emby-offline".equals(String.valueOf(target.getChildAt(i).getTag())))hinted=true;if(target.getChildCount()<=1||target.findViewWithTag("mp-skeleton")!=null)render(a,server,target,cached);if(!hinted){TextView tag=a.text("离线数据 · 刷新失败，显示的是上次保存的内容",12,a.muted());tag.setTag("emby-offline");target.addView(tag,0);}return;}target.removeAllViews();target.addView(a.heading("Emby"));target.addView(a.text(EmbyApi.explain(e),13,a.muted()));target.addView(a.button("重试",()->a.home()));});}});}

 static final ExecutorService FETCH=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"emby-home-fetch");t.setDaemon(true);return t;});
 /** 拉一次首页需要的全部数据，结果是可直接缓存的 JSON：resume / next / heroes / order / libs / latest。 */
 static JSONObject fetch(ScrapedMainActivity a,JSONObject server,java.util.function.Consumer<JSONObject> partial)throws Exception{
  Exception failure=null;JSONArray resume;try{resume=rows(EmbyClient.resume(a,server));}catch(Exception e){resume=new JSONArray();failure=e;}
  // 没有缓存时：继续观看一回来就先画出来（海报 + 继续观看），其余分区读完再补上
  if(partial!=null&&resume.length()>0){JSONArray order=new JSONArray();for(String sec:EmbyCue.homeOrder(null))order.put(sec);partial.accept(new JSONObject().put("resume",resume).put("next",new JSONArray()).put("heroes",EmbyCue.pickHeroes(resume,new JSONArray())).put("order",order).put("libs",new JSONArray()).put("latest",new JSONArray()));}
  Future<JSONObject> nextF=FETCH.submit(()->EmbyClient.nextUp(a,server)),viewsF=FETCH.submit(()->EmbyClient.views(a,server)),orderF=FETCH.submit(()->EmbyClient.homeSections(a,server));
  JSONArray next;try{next=EmbyCue.withoutResume(rows(nextF.get(30,TimeUnit.SECONDS)),resume);}catch(Exception e){next=new JSONArray();}
  JSONArray views;try{views=rows(viewsF.get(30,TimeUnit.SECONDS));}catch(Exception e){views=new JSONArray();if(failure==null)failure=e;}
  if(failure!=null&&resume.length()==0&&views.length()==0)throw failure;
  String[] order;try{order=EmbyCue.homeOrder(rows(orderF.get(30,TimeUnit.SECONDS)));}catch(Exception e){order=EmbyCue.homeOrder(null);}
  boolean latestWanted=false;for(String s:order)if("latest".equals(s)||"libraries".equals(s))latestWanted=true;
  JSONArray libs=new JSONArray();for(JSONObject lib:libraries(views))if(!EmbyCue.hidden(server,lib.optString("Id")))libs.put(lib);
  JSONArray latest=new JSONArray(),flat=new JSONArray();
  if(latestWanted){ArrayList<Future<JSONObject>> jobs=new ArrayList<>();for(int i=0;i<libs.length();i++){String id=libs.optJSONObject(i).optString("Id");jobs.add(FETCH.submit(()->EmbyClient.latest(a,server,id)));}
   for(Future<JSONObject> f:jobs){JSONArray items;try{items=rows(f.get(30,TimeUnit.SECONDS));}catch(Exception e){items=new JSONArray();}latest.put(items);for(int i=0;i<items.length();i++)flat.put(items.opt(i));}}
  JSONArray orderJson=new JSONArray();for(String s:order)orderJson.put(s);
  return new JSONObject().put("resume",resume).put("next",next).put("heroes",EmbyCue.pickHeroes(resume,flat)).put("order",orderJson).put("libs",libs).put("latest",latest);
 }
 static void render(ScrapedMainActivity a,JSONObject server,LinearLayout target,JSONObject data){target.removeAllViews();
  JSONArray resumeItems=data.optJSONArray("resume"),nextItems=data.optJSONArray("next"),heroItems=data.optJSONArray("heroes"),libs=data.optJSONArray("libs"),latest=data.optJSONArray("latest"),orderJson=data.optJSONArray("order");
  if(resumeItems==null)resumeItems=new JSONArray();if(heroItems==null)heroItems=new JSONArray();if(libs==null)libs=new JSONArray();if(latest==null)latest=new JSONArray();
  HashSet<String> resumeIds=ids(resumeItems);
  JSONArray heroes=new JSONArray();for(int i=0;i<heroItems.length();i++){JSONObject item=heroItems.optJSONObject(i);JSONObject m=card(a,server,item,item!=null&&resumeIds.contains(item.optString("Id")));if(m==null)continue;if("Movie".equals(item.optString("Type"))||"Episode".equals(item.optString("Type")))try{m.put("emby.directPlay",true);}catch(Exception ignored){}heroes.put(m);}
  if(heroes.length()>0){a.heroPriority=4;a.setHeroes(heroes,heroes.optJSONObject(0).optString("type","movie"));}
  if(a.homeChrome==null)a.homeChrome=new HomeChrome(a);
  boolean any=false;
  for(int s=0;orderJson!=null&&s<orderJson.length();s++){String section=orderJson.optString(s);
   if("resume".equals(section)){if(!a.sectionOn("resume"))continue;JSONArray cont=metas(a,server,resumeItems,true);if(mergeNext(a))cont=withNextUp(cont,metas(a,server,nextItems,false));if(cont.length()>0){a.homeChrome.continueWatching(target,cont,"继续观看");any=true;}}
   else if("nextup".equals(section)){if(!a.sectionOn("nextup")||mergeNext(a))continue;JSONArray row=metas(a,server,nextItems,false);if(row.length()>0){TextView heading=a.sectionHead("nextup","下一集");target.addView(heading);LinearLayout shelf=new LinearLayout(a);shelf.setOrientation(1);target.addView(shelf);a.shelf(shelf,row,"series",false);any=true;}}
   else if("latest".equals(section)){if(!a.sectionOn("latest"))continue;for(int i=0;i<libs.length()&&i<latest.length();i++){JSONArray metas=metas(a,server,latest.optJSONArray(i),false);if(metas.length()==0)continue;JSONObject lib=libs.optJSONObject(i);TextView heading=a.sectionHead("latest","最新 · "+lib.optString("Name","媒体库")+" ›");String id=lib.optString("Id");heading.setOnClickListener(v->EmbyLibraryActivity.openLibrary(a,server.optString("id"),id));target.addView(heading);LinearLayout shelf=new LinearLayout(a);shelf.setOrientation(1);target.addView(shelf);a.shelf(shelf,metas,"movie",false);any=true;}}}
  if(!any)target.addView(a.text("这个服务器还没有可以展示的内容",13,a.muted()));
 }
 /** 缓存放在应用缓存目录（不进备份）；按服务器编号 + 用户分文件。读坏了就当没有。 */
 static java.io.File cacheFile(ScrapedMainActivity a,JSONObject server){String key=(server.optString("id")+"_"+server.optString("userId")).replaceAll("[^A-Za-z0-9_-]","");return new java.io.File(a.getCacheDir(),"emby-home-"+key+".json");}
 static JSONObject readCache(ScrapedMainActivity a,JSONObject server){try{java.io.File f=cacheFile(a,server);if(!f.isFile()||f.length()>4_000_000)return null;return new JSONObject(new String(java.nio.file.Files.readAllBytes(f.toPath()),"UTF-8"));}catch(Exception e){return null;}}
 static void writeCache(ScrapedMainActivity a,JSONObject server,JSONObject data){try{java.io.File f=cacheFile(a,server),tmp=new java.io.File(f.getPath()+".tmp");java.nio.file.Files.write(tmp.toPath(),data.toString().getBytes("UTF-8"));if(!tmp.renameTo(f))tmp.delete();}catch(Exception ignored){}}
 static void supplement(ScrapedMainActivity a){if("emby".equals(a.homeProvider))return;JSONArray servers=EmbyStore.list(a);if(servers.length()==0)return;final int ticket=a.epoch;LinearLayout target=section(a);IO.execute(()->{try{JSONArray blocks=new JSONArray();
   for(int s=0;s<servers.length();s++){JSONObject server=servers.optJSONObject(s);if(server==null)continue;JSONArray resume=rows(safe(()->EmbyClient.resume(a,server)));JSONArray cont=metas(a,server,resume,true);if(cont.length()>0)blocks.put(new JSONObject().put("title","继续观看 · "+server.optString("name","Emby")).put("items",cont).put("continue",true));
    JSONArray next;try{next=metas(a,server,EmbyCue.withoutResume(rows(EmbyClient.nextUp(a,server)),resume),false);}catch(Exception e){next=new JSONArray();}if(next.length()>0)blocks.put(new JSONObject().put("title","下一集 · "+server.optString("name","Emby")).put("items",next));
    JSONArray views=rows(safe(()->EmbyClient.views(a,server)));int shown=0;for(JSONObject lib:libraries(views)){if(shown>=6||EmbyCue.hidden(server,lib.optString("Id")))continue;JSONArray metas=metas(a,server,rows(safe(()->EmbyClient.latest(a,server,lib.optString("Id")))),false);if(metas.length()>0){blocks.put(new JSONObject().put("title","最新 · "+lib.optString("Name","媒体库")).put("items",metas));shown++;}}}
   a.runOnUiThread(()->{if(ticket!=a.epoch||a.isFinishing()||!a.homeCanvas)return;target.removeAllViews();if(a.homeChrome==null)a.homeChrome=new HomeChrome(a);for(int i=0;i<blocks.length();i++){JSONObject b=blocks.optJSONObject(i);if(b==null)continue;if(b.optBoolean("continue"))a.homeChrome.continueWatching(target,b.optJSONArray("items"),b.optString("title"));else{target.addView(a.heading(b.optString("title")+" ›"));LinearLayout shelf=new LinearLayout(a);shelf.setOrientation(1);target.addView(shelf);a.shelf(shelf,b.optJSONArray("items"),"movie",false);}}});
  }catch(Exception ignored){}});}
 /** 0.1.123（参考 Wholphin）：「下一集」并进「继续观看」同一行，排在看了一半的后面；同一部剧已在继续观看里就不重复。「我的」里可关。 */
 static boolean mergeNext(ScrapedMainActivity a){return a.prefs.getBoolean("home.mergeNextUp",true);}
 static JSONArray withNextUp(JSONArray resume,JSONArray next){JSONArray out=new JSONArray();HashSet<String> series=new HashSet<>();for(int i=0;i<resume.length();i++){JSONObject m=resume.optJSONObject(i);if(m==null)continue;out.put(m);String sid=m.optString("emby.series");if(!sid.isEmpty())series.add(sid);}
  for(int i=0;next!=null&&i<next.length();i++){JSONObject m=next.optJSONObject(i);if(m==null)continue;String sid=m.optString("emby.series");if(!sid.isEmpty()&&!series.add(sid))continue;try{JSONObject c=new JSONObject(m.toString());String line=c.optString("emby.line");c.put("emby.line","下一集"+(line.isEmpty()?"":" · "+line));c.put("emby.directPlay",true);out.put(c);}catch(Exception ignored){}}
  return out;}
 static JSONObject selected(ScrapedMainActivity a){return HomeRules.selectedEmbyServer(EmbyStore.list(a),a.prefs.getString("home.source.v1",""));}
 static LinearLayout section(ScrapedMainActivity a){LinearLayout t=new LinearLayout(a);t.setOrientation(1);a.body.addView(t);return t;}
 static JSONArray rows(JSONObject response){if(response==null)return new JSONArray();JSONArray items=response.optJSONArray("Items");return items==null?new JSONArray():items;}
 static JSONObject safe(Call call){try{return call.run();}catch(Exception e){return new JSONObject();}}
 interface Call {JSONObject run()throws Exception;}
 static ArrayList<JSONObject> libraries(JSONArray views){ArrayList<JSONObject> out=new ArrayList<>();for(int i=0;i<views.length();i++){JSONObject v=views.optJSONObject(i);if(v!=null&&EmbyApi.library(v))out.add(v);}return out;}
 static HashSet<String> ids(JSONArray items){HashSet<String> out=new HashSet<>();for(int i=0;i<items.length();i++){JSONObject m=items.optJSONObject(i);if(m!=null)out.add(m.optString("Id"));}return out;}
 static JSONArray metas(ScrapedMainActivity a,JSONObject server,JSONArray items,boolean resume){JSONArray out=new JSONArray();for(int i=0;items!=null&&i<items.length();i++){JSONObject m=card(a,server,items.optJSONObject(i),resume);if(m!=null)out.put(m);}return out;}
 static JSONObject card(ScrapedMainActivity a,JSONObject server,JSONObject item,boolean resume){try{if(item==null||item.optString("Id").isEmpty())return null;JSONObject m=EmbyApi.meta(server,item);if(resume)m.put("emby.directPlay",true);long ms=m.optLong("emby.resumeMs");if(ms>0){String id=m.optString("id");int dur=(int)Math.min(Integer.MAX_VALUE,EmbyApi.ticksToMs(item.optLong("RunTimeTicks")));if(a.prefs.getInt("pos:"+id,0)<=0)a.prefs.edit().putInt("pos:"+id,(int)Math.min(Integer.MAX_VALUE,ms)).putInt("dur:"+id,dur).apply();}return m;}catch(Exception e){return null;}}
}
