package com.guocheng.mediapocket;
import android.app.Activity;import android.content.Context;import android.view.Gravity;import android.widget.LinearLayout;import android.widget.TextView;import android.widget.Toast;import org.json.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/**
 * 0.1.164 季详情上的批量下载。勾选、全选，或直接下整季 / 从当前集往后。
 * 解析和入队都在下载线程上，一次写完队列再启动 OfflineService，不挡住界面。
 */
final class OfflineBatchMenu {
 private OfflineBatchMenu(){}
 interface Episode {
  String label();String key();String earlyUrl();boolean transcodeOnly();
  JSONObject record(Context c,long now)throws Exception;
 }
 static void emby(Activity a,JSONObject server,String series,JSONArray items,String preferredId){
  if(server==null){toast(a,"找不到这个 Emby 服务器");return;}
  if(items==null){toast(a,"正在读取这一季…");return;}
  ArrayList<Episode> jobs=new ArrayList<>();ArrayList<String> ids=new ArrayList<>();ArrayList<Boolean> played=new ArrayList<>();ArrayList<Boolean> started=new ArrayList<>();
  for(int i=0;i<items.length();i++){JSONObject ep=items.optJSONObject(i);if(ep==null||ep.optString("Id").isEmpty())continue;
   final String id=ep.optString("Id"),label=MediaBadgeRules.episodeTitle(ep);final String show=series==null||series.isEmpty()?ep.optString("SeriesName","剧集"):series;final String name=show+" · "+label;final String serverId=server.optString("id");
   JSONObject ud=ep.optJSONObject("UserData");boolean seen=ud!=null&&ud.optBoolean("Played");boolean progress=ud!=null&&ud.optLong("PlaybackPositionTicks")>0&&!seen;
   ids.add(id);played.add(seen);started.add(progress);
   jobs.add(new Episode(){public String label(){return label;}public String key(){return OfflineQueueRules.embyKey(serverId,id);}public String earlyUrl(){return "";}public boolean transcodeOnly(){return false;}public JSONObject record(Context c,long now)throws Exception{return OfflineDownloads.embyRecord(c,server,id,name,null,now);}});}
  if(jobs.isEmpty()){toast(a,"这一季没有可下载的剧集");return;}
  String[] idArr=ids.toArray(new String[0]);boolean[] playArr=flags(played),startArr=flags(started);
  open(a,"批量下载",jobs,OfflineQueueRules.resumeIndex(idArr,playArr,startArr,preferredId));
 }
 static void fusion(Activity a,FusionCatalog.Session session,JSONObject vod,String id,String title,String flag,List<String[]> episodes,int current){
  if(session==null){toast(a,"找不到这个影视仓站点");return;}
  if(episodes==null||episodes.isEmpty()){toast(a,"这一线路没有可下载的选集");return;}
  ArrayList<Episode> jobs=new ArrayList<>();
  for(String[] ep:episodes){if(ep==null||ep.length<2||ep[1].isEmpty())continue;final String episode=ep[0],value=ep[1];final String name=(title==null||title.isEmpty()?"影片":title)+" · "+episode;final String watch=watchId(session,id,episode);final String site=session.site.optString("key"),config=session.url,useFlag=flag==null?"":flag;
   jobs.add(new Episode(){public String label(){return episode;}public String key(){return OfflineQueueRules.fusionKey(config,site,useFlag,value);}public String earlyUrl(){return value;}public boolean transcodeOnly(){return false;}public JSONObject record(Context c,long now)throws Exception{return OfflineDownloads.fusionRecord(c,session,useFlag,value,name,watch,vod,now);}});}
  if(jobs.isEmpty()){toast(a,"这一线路没有可下载的选集");return;}
  open(a,"批量下载",jobs,current);
 }
 static String watchId(FusionCatalog.Session session,String id,String episode){try{return "tvbox:"+TvSpiderRules.digest((session.url+"|"+session.site.optString("key")+"|"+id+"|"+episode).getBytes("UTF-8"),"SHA-256");}catch(Exception e){return "";}}
 static boolean[] flags(List<Boolean> values){boolean[] out=new boolean[values.size()];for(int i=0;i<out.length;i++)out[i]=Boolean.TRUE.equals(values.get(i));return out;}
 static void open(Activity a,String title,List<Episode> jobs,int current){
  final boolean[] on=new boolean[jobs.size()];final TextView[] rows=new TextView[jobs.size()];final String[] labels=new String[jobs.size()];final String[] block=blocks(a,jobs);
  int at=current<0?0:Math.min(current,Math.max(0,jobs.size()-1));
  LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);
  TextView hint=new TextView(a);hint.setText("灰色的是下不了，或已经在下载队列里的，点一下可看原因。全选和整季下载会跳过它们。");hint.setTextColor(PlayerTheme.secondary(a));hint.setTextSize(Ui.sp(13));hint.setPadding(CinemaMenu.dp(a,8),CinemaMenu.dp(a,4),CinemaMenu.dp(a,8),CinemaMenu.dp(a,8));body.addView(hint);
  LinearLayout tools=new LinearLayout(a);tools.setOrientation(LinearLayout.HORIZONTAL);
  tools.addView(link(a,"全选",()->setAll(a,rows,labels,on,block,at,true)),new LinearLayout.LayoutParams(0,CinemaMenu.dp(a,48),1f));
  tools.addView(link(a,"取消全选",()->setAll(a,rows,labels,on,block,at,false)),new LinearLayout.LayoutParams(0,CinemaMenu.dp(a,48),1f));
  body.addView(tools);
  body.addView(link(a,"下载所选",()->confirm(a,chosen(jobs,on),java.util.Collections.<String>emptyList())));
  body.addView(link(a,"下载本季全部",()->{OfflineQueueRules.Scope s=OfflineQueueRules.scope(block,range(jobs.size()));confirm(a,pick(jobs,s.enqueue),s.skips);}));
  body.addView(link(a,"从当前集往后全部",()->{OfflineQueueRules.Scope s=OfflineQueueRules.scope(block,OfflineQueueRules.tail(at,jobs.size()));confirm(a,pick(jobs,s.enqueue),s.skips);}));
  for(int i=0;i<jobs.size();i++){final int index=i;labels[i]=jobs.get(i).label();TextView row=new TextView(a);rows[i]=row;paint(a,row,labels[i],i==at,false,block[i]);row.setMinHeight(CinemaMenu.dp(a,44));row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(CinemaMenu.dp(a,8),CinemaMenu.dp(a,4),CinemaMenu.dp(a,8),CinemaMenu.dp(a,4));row.setOnClickListener(v->{if(block[index]!=null){toast(a,block[index]);return;}on[index]=!on[index];paint(a,row,labels[index],index==at,on[index],null);});body.addView(row);}
  if(!CinemaMenu.open(a,title,body,new ArrayList<CinemaMenu.Item>(),null))toast(a,"现在打不开下载菜单");
 }
 static String[] blocks(Activity a,List<Episode> jobs){Map<String,String> existing;synchronized(OfflineDownloads.class){existing=OfflineQueueRules.index(OfflineDownloads.list(a));}String[] block=new String[jobs.size()];for(int i=0;i<jobs.size();i++)block[i]=OfflineQueueRules.dialogBlock(existing.get(jobs.get(i).key()),jobs.get(i).earlyUrl(),jobs.get(i).transcodeOnly());return block;}
 static int[] range(int size){int[] all=new int[Math.max(0,size)];for(int i=0;i<all.length;i++)all[i]=i;return all;}
 static List<Episode> pick(List<Episode> jobs,int[] indexes){ArrayList<Episode> out=new ArrayList<>();if(indexes==null)return out;for(int i:indexes)if(i>=0&&i<jobs.size())out.add(jobs.get(i));return out;}
 static List<Episode> chosen(List<Episode> jobs,boolean[] on){ArrayList<Episode> out=new ArrayList<>();for(int i=0;i<on.length;i++)if(on[i])out.add(jobs.get(i));return out;}
 static void setAll(Activity a,TextView[] rows,String[] labels,boolean[] on,String[] block,int current,boolean value){for(int i=0;i<on.length;i++){if(block[i]!=null){on[i]=false;paint(a,rows[i],labels[i],i==current,false,block[i]);continue;}on[i]=value;paint(a,rows[i],labels[i],i==current,value,null);}}
 static void paint(Activity a,TextView row,String label,boolean current,boolean checked,String block){String name=label+(current?"  · 当前":"");if(block!=null){row.setText("○  "+name+"\n"+block);row.setTextColor(PlayerTheme.secondary(a));row.setAlpha(.72f);}else{row.setText((checked?"✓  ":"○  ")+name);row.setTextColor(checked?PlayerTheme.selectedText(a):PlayerTheme.text(a));row.setAlpha(1f);}row.setTextSize(Ui.sp(15));}
 static TextView link(Activity a,String label,Runnable run){TextView t=new TextView(a);t.setText(label);t.setTextColor(PlayerTheme.selectedText(a));t.setTextSize(Ui.sp(15));t.setGravity(Gravity.CENTER_VERTICAL);t.setMinHeight(CinemaMenu.dp(a,48));t.setPadding(CinemaMenu.dp(a,8),0,CinemaMenu.dp(a,8),0);t.setOnClickListener(v->run.run());return t;}
 static void confirm(Activity a,List<Episode> jobs,List<String> presetSkips){
  final ArrayList<String> prior=new ArrayList<>();if(presetSkips!=null)prior.addAll(presetSkips);
  if(jobs==null||jobs.isEmpty()){if(!prior.isEmpty()){new CinemaDialog(a).setTitle("下载队列").setMessage(OfflineQueueRules.summary(0,prior)).setPositiveButton("知道了",null).show();return;}toast(a,"还没有选择剧集");return;}
  final ArrayList<Episode> chosen=new ArrayList<>(jobs);
  String extra=prior.isEmpty()?"":"\n这一批里已经有 "+prior.size()+" 集会跳过，完成后按原因列出。";
  new CinemaDialog(a).setTitle("批量下载").setMessage("按顺序加入下载队列，共 "+chosen.size()+" 集。\n已下载、已在队列或不支持的片源会跳过，并说明原因。"+extra+"\n大文件建议连 Wi-Fi。").setPositiveButton("开始下载",(d,w)->run(a,chosen,prior)).setNegativeButton("取消",null).show();
 }
 static void run(Activity a,List<Episode> chosen,List<String> presetSkips){
  CinemaWait wait=CinemaWait.show(a,"批量下载","正在加入下载队列…",true,true);AtomicBoolean cancel=new AtomicBoolean();wait.setOnCancelListener(d->cancel.set(true));Context app=a.getApplicationContext();
  OfflineDownloads.IO.execute(()->{ArrayList<String> skips=new ArrayList<>();if(presetSkips!=null)skips.addAll(presetSkips);ArrayList<Episode> todo=new ArrayList<>();
   Map<String,String> existing;synchronized(OfflineDownloads.class){existing=OfflineQueueRules.index(OfflineDownloads.list(app));}
   ArrayList<OfflineQueueRules.Item> items=new ArrayList<>();for(Episode ep:chosen)items.add(new OfflineQueueRules.Item(ep.key(),ep.label(),ep.earlyUrl(),ep.transcodeOnly()));
   List<OfflineQueueRules.Row> plan=OfflineQueueRules.plan(items,existing);
   for(int i=0;i<plan.size();i++){OfflineQueueRules.Row row=plan.get(i);if(row.enqueue)todo.add(chosen.get(i));else if(row.reason!=null)skips.add(row.reason);}
   ArrayList<JSONObject> ready=new ArrayList<>();long base=System.currentTimeMillis();
   for(int i=0;i<todo.size();i++){if(OfflineQueueRules.accept(cancel.get(),null)<0)break;final int at=i+1,total=todo.size();a.runOnUiThread(()->{if(!a.isFinishing()&&wait.isShowing())wait.setMessage("正在加入下载队列… "+at+" / "+total);});
    JSONObject rec=null;String skip=null;try{rec=todo.get(i).record(app,base+ready.size());skip=OfflineRules.refuse(rec.optString("url"));}catch(Exception e){skip=reason(e);}
    int decision=OfflineQueueRules.accept(cancel.get(),skip);if(decision<0)break;if(decision==0)skips.add(skip);else if(rec!=null)ready.add(rec);}
   int wrote=OfflineDownloads.commitAll(app,ready);if(wrote<0){skips.add("无法写入下载队列");wrote=0;}else for(int i=0;i<ready.size()-wrote;i++)skips.add(OfflineQueueRules.QUEUED);
   final int added=wrote;final String text=OfflineQueueRules.summary(added,skips)+(cancel.get()?"\n已取消，还没解析的集数没有加入。":"");
   a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed())return;wait.dismiss();if(added>0)FollowUp.askDownloadNotify(a);new CinemaDialog(a).setTitle("下载队列").setMessage(text).setPositiveButton("知道了",null).show();});});
 }
 static String reason(Exception e){if(e instanceof IllegalStateException&&e.getMessage()!=null&&!e.getMessage().isEmpty())return e.getMessage();String explained=EmbyApi.explain(e);if(explained!=null&&!explained.isEmpty()&&!"无法完成请求，请检查网络后重试".equals(explained))return explained;String m=e.getMessage();return m==null||m.isEmpty()?"无法下载":m;}
 static void toast(Activity a,String text){try{Toast.makeText(a,text,Toast.LENGTH_LONG).show();}catch(Exception ignored){}}
}
