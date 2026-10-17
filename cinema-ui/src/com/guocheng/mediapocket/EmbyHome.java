package com.guocheng.mediapocket;
import android.widget.*;import org.json.*;import java.util.*;import java.util.concurrent.*;
/** 首页上的 Emby：按服务器首页分区排列继续观看、下一集和各库最新。没有的数据就跳过。 */
final class EmbyHome {
 static final ExecutorService IO=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"emby-home");t.setDaemon(true);return t;});
 private EmbyHome(){}
 static void show(ScrapedMainActivity a){final int ticket=a.epoch;JSONObject server=selected(a);LinearLayout target=section(a);if(server==null){target.addView(a.heading("Emby"));target.addView(a.text("还没有 Emby 服务器，请到设置里添加",13,a.muted()));target.addView(a.button("添加 Emby 服务器",()->EmbyLibraryActivity.manage(a)));return;}
  target.addView(a.text("正在读取 "+EmbyStore.label(server)+" …",12,a.muted()));
  IO.execute(()->{try{
   Exception failure=null;JSONArray resume;try{resume=rows(EmbyClient.resume(a,server));}catch(Exception e){resume=new JSONArray();failure=e;}
   JSONArray next;try{next=EmbyCue.withoutResume(rows(EmbyClient.nextUp(a,server)),resume);}catch(Exception e){next=new JSONArray();}
   JSONArray views;try{views=rows(EmbyClient.views(a,server));}catch(Exception e){views=new JSONArray();if(failure==null)failure=e;}
   if(failure!=null&&resume.length()==0&&views.length()==0)throw failure;
   String[] order;try{order=EmbyCue.homeOrder(rows(EmbyClient.homeSections(a,server)));}catch(Exception e){order=EmbyCue.homeOrder(null);}
   boolean latestWanted=false;for(String s:order)if("latest".equals(s)||"libraries".equals(s))latestWanted=true;
   ArrayList<JSONObject> libs=new ArrayList<>();for(JSONObject lib:libraries(views))if(!EmbyCue.hidden(server,lib.optString("Id")))libs.add(lib);
   ArrayList<JSONArray> latest=new ArrayList<>();JSONArray flat=new JSONArray();
   if(latestWanted)for(JSONObject lib:libs){JSONArray items=rows(safe(()->EmbyClient.latest(a,server,lib.optString("Id"))));latest.add(items);for(int i=0;i<items.length();i++)flat.put(items.opt(i));}
   JSONArray heroRaw=EmbyCue.pickHeroes(resume,flat);HashSet<String> resumeIds=ids(resume);
   final String[] sections=order;final JSONArray resumeItems=resume,nextItems=next,heroItems=heroRaw;
   a.runOnUiThread(()->{if(ticket!=a.epoch||a.isFinishing())return;target.removeAllViews();
    JSONArray heroes=new JSONArray();for(int i=0;i<heroItems.length();i++){JSONObject item=heroItems.optJSONObject(i);JSONObject m=card(a,server,item,item!=null&&resumeIds.contains(item.optString("Id")));if(m==null)continue;if("Movie".equals(item.optString("Type"))||"Episode".equals(item.optString("Type")))try{m.put("emby.directPlay",true);}catch(Exception ignored){}heroes.put(m);}
    if(heroes.length()>0){a.heroPriority=4;a.setHeroes(heroes,heroes.optJSONObject(0).optString("type","movie"));}
    if(a.homeChrome==null)a.homeChrome=new HomeChrome(a);
    boolean any=false;
    for(String section:sections){if("resume".equals(section)){JSONArray cont=metas(a,server,resumeItems,true);if(cont.length()>0){a.homeChrome.continueWatching(target,cont,"继续观看");any=true;}}
     else if("nextup".equals(section)){JSONArray row=metas(a,server,nextItems,false);if(row.length()>0){target.addView(a.heading("下一集"));LinearLayout shelf=new LinearLayout(a);shelf.setOrientation(1);target.addView(shelf);a.shelf(shelf,row,"series",false);any=true;}}
     else if("latest".equals(section)){for(int i=0;i<libs.size()&&i<latest.size();i++){JSONArray metas=metas(a,server,latest.get(i),false);if(metas.length()==0)continue;JSONObject lib=libs.get(i);TextView heading=a.heading("最新 · "+lib.optString("Name","媒体库")+" ›");String id=lib.optString("Id");heading.setOnClickListener(v->EmbyLibraryActivity.openLibrary(a,server.optString("id"),id));target.addView(heading);LinearLayout shelf=new LinearLayout(a);shelf.setOrientation(1);target.addView(shelf);a.shelf(shelf,metas,"movie",false);any=true;}}}
    if(!any)target.addView(a.text("这个服务器还没有可以展示的内容",13,a.muted()));
   });
  }catch(Exception e){a.runOnUiThread(()->{if(ticket!=a.epoch||a.isFinishing())return;target.removeAllViews();target.addView(a.heading("Emby"));target.addView(a.text(EmbyApi.explain(e),13,a.muted()));target.addView(a.button("重试",()->a.home()));});}});}
 static void supplement(ScrapedMainActivity a){if("emby".equals(a.homeProvider))return;JSONArray servers=EmbyStore.list(a);if(servers.length()==0)return;final int ticket=a.epoch;LinearLayout target=section(a);IO.execute(()->{try{JSONArray blocks=new JSONArray();
   for(int s=0;s<servers.length();s++){JSONObject server=servers.optJSONObject(s);if(server==null)continue;JSONArray resume=rows(safe(()->EmbyClient.resume(a,server)));JSONArray cont=metas(a,server,resume,true);if(cont.length()>0)blocks.put(new JSONObject().put("title","继续观看 · "+server.optString("name","Emby")).put("items",cont).put("continue",true));
    JSONArray next;try{next=metas(a,server,EmbyCue.withoutResume(rows(EmbyClient.nextUp(a,server)),resume),false);}catch(Exception e){next=new JSONArray();}if(next.length()>0)blocks.put(new JSONObject().put("title","下一集 · "+server.optString("name","Emby")).put("items",next));
    JSONArray views=rows(safe(()->EmbyClient.views(a,server)));int shown=0;for(JSONObject lib:libraries(views)){if(shown>=6||EmbyCue.hidden(server,lib.optString("Id")))continue;JSONArray metas=metas(a,server,rows(safe(()->EmbyClient.latest(a,server,lib.optString("Id")))),false);if(metas.length()>0){blocks.put(new JSONObject().put("title","最新 · "+lib.optString("Name","媒体库")).put("items",metas));shown++;}}}
   a.runOnUiThread(()->{if(ticket!=a.epoch||a.isFinishing()||!a.homeCanvas)return;target.removeAllViews();if(a.homeChrome==null)a.homeChrome=new HomeChrome(a);for(int i=0;i<blocks.length();i++){JSONObject b=blocks.optJSONObject(i);if(b==null)continue;if(b.optBoolean("continue"))a.homeChrome.continueWatching(target,b.optJSONArray("items"),b.optString("title"));else{target.addView(a.heading(b.optString("title")+" ›"));LinearLayout shelf=new LinearLayout(a);shelf.setOrientation(1);target.addView(shelf);a.shelf(shelf,b.optJSONArray("items"),"movie",false);}}});
  }catch(Exception ignored){}});}
 static JSONObject selected(ScrapedMainActivity a){String id=a.prefs.getString("home.source.v1","");JSONObject s=EmbyStore.get(a,id);if(s!=null)return s;JSONArray all=EmbyStore.list(a);return all.length()==0?null:all.optJSONObject(0);}
 static LinearLayout section(ScrapedMainActivity a){LinearLayout t=new LinearLayout(a);t.setOrientation(1);a.body.addView(t);return t;}
 static JSONArray rows(JSONObject response){if(response==null)return new JSONArray();JSONArray items=response.optJSONArray("Items");return items==null?new JSONArray():items;}
 static JSONObject safe(Call call){try{return call.run();}catch(Exception e){return new JSONObject();}}
 interface Call {JSONObject run()throws Exception;}
 static ArrayList<JSONObject> libraries(JSONArray views){ArrayList<JSONObject> out=new ArrayList<>();for(int i=0;i<views.length();i++){JSONObject v=views.optJSONObject(i);if(v!=null&&EmbyApi.library(v))out.add(v);}return out;}
 static HashSet<String> ids(JSONArray items){HashSet<String> out=new HashSet<>();for(int i=0;i<items.length();i++){JSONObject m=items.optJSONObject(i);if(m!=null)out.add(m.optString("Id"));}return out;}
 static JSONArray metas(ScrapedMainActivity a,JSONObject server,JSONArray items,boolean resume){JSONArray out=new JSONArray();for(int i=0;items!=null&&i<items.length();i++){JSONObject m=card(a,server,items.optJSONObject(i),resume);if(m!=null)out.put(m);}return out;}
 static JSONObject card(ScrapedMainActivity a,JSONObject server,JSONObject item,boolean resume){try{if(item==null||item.optString("Id").isEmpty())return null;JSONObject m=EmbyApi.meta(server,item);if(resume)m.put("emby.directPlay",true);long ms=m.optLong("emby.resumeMs");if(ms>0){String id=m.optString("id");int dur=(int)Math.min(Integer.MAX_VALUE,EmbyApi.ticksToMs(item.optLong("RunTimeTicks")));if(a.prefs.getInt("pos:"+id,0)<=0)a.prefs.edit().putInt("pos:"+id,(int)Math.min(Integer.MAX_VALUE,ms)).putInt("dur:"+id,dur).apply();}return m;}catch(Exception e){return null;}}
}
