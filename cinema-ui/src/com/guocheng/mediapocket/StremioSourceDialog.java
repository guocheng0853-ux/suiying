package com.guocheng.mediapocket;
import android.app.*;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import org.json.*;import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;
/**
 * Stremio 播放来源：按插件分组的弹窗（与 0.1.45 影视仓弹窗同一交互）。
 * 每个插件独立请求 /stream/{type}/{id}.json，3 路并发、每插件 25 秒预算，显示等待/查询中/条目数/无条目/失败原因，可折叠、单插件重试。
 * 关闭即作废后台结果（代次 + 请求编号）。选中直链先做真实地址检查，再走原 MainActivity.play 流程（原播放器、原换源队列）。
 */
final class StremioSourceDialog {
 interface Play {void play(JSONObject stream,JSONArray all);}
 final Activity a;final JSONArray addons;final String id,type;final Play host;final Runnable addAddon;final boolean light;final int ink,muted,bg,accent;
 final ExecutorService pool=Executors.newFixedThreadPool(3);final ArrayList<Group> groups=new ArrayList<>();final Dialog dialog;final TextView summary;boolean closed;int generation;
 final class Group {JSONObject addon;String name;TextView header;LinearLayout content;JSONArray streams=new JSONArray();String status="等待查询";boolean expanded;int request;Future<?> task;}
 int dp(int n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
 TextView text(String s,int size,int color){TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(12),dp(8),dp(12),dp(8));return t;}
 Button button(String label,Runnable action){Button b=new Button(a);b.setText(label);b.setTextColor(ink);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);b.setOnClickListener(v->action.run());return b;}

 StremioSourceDialog(Activity owner,JSONArray all,String id,String type,JSONObject meta,Play play,Runnable add){
  a=owner;this.id=id;this.type=type;host=play;addAddon=add;addons=AddonRegistry.forResource(all==null?new JSONArray():all,"stream",type,id);
  light=a.getSharedPreferences("data",0).getString("themeMode","dark").equals("light");ink=light?0xff202830:0xfff1f4f6;muted=light?0xff697580:0xffb0c1cc;bg=light?0xfffafbfc:0xff16343e;accent=light?0xff2c6b8a:0xff8fc9e6;
  dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
  LinearLayout root=new LinearLayout(a);root.setOrientation(1);root.setPadding(dp(10),dp(6),dp(10),dp(14));GradientDrawable shape=new GradientDrawable();shape.setColor(bg);shape.setCornerRadius(dp(24));root.setBackground(shape);
  TextView handle=text("━━",14,muted);handle.setGravity(Gravity.CENTER);root.addView(handle);
  LinearLayout top=new LinearLayout(a);top.setGravity(Gravity.CENTER_VERTICAL);TextView heading=text("播放来源",22,ink);heading.setTypeface(null,1);top.addView(heading,new LinearLayout.LayoutParams(0,-2,1));top.addView(button("全部重试",()->start()));top.addView(button("关闭",()->dialog.dismiss()));root.addView(top);
  String name=meta==null?"":meta.optString("name");String ep=StremioStreams.episodeLabel(id);TextView sub=text(name+(ep.isEmpty()?"":"  ·  "+ep),13,ink);root.addView(sub);
  summary=text("正在准备插件…",12,muted);root.addView(summary);
  ScrollView scroll=new ScrollView(a);LinearLayout list=new LinearLayout(a);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  if(addons.length()==0){TextView none=text("还没有支持此影片的视频源插件（需要声明 stream 资源）。",14,muted);list.addView(none);if(addAddon!=null)list.addView(button("添加插件",()->{dialog.dismiss();addAddon.run();}));summary.setText("0 个插件");}
  for(int i=0;i<addons.length();i++){
   JSONObject addon=addons.optJSONObject(i);if(addon==null)continue;Group g=new Group();g.addon=addon;JSONObject manifest=addon.optJSONObject("manifest");g.name=manifest!=null&&!manifest.optString("name").isEmpty()?manifest.optString("name"):addon.optString("name","插件 "+(i+1));
   LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);g.header=text("",15,ink);g.header.setOnClickListener(v->{g.expanded=!g.expanded;render(g);});row.addView(g.header,new LinearLayout.LayoutParams(0,-2,1));
   row.addView(button("⋮",()->new AlertDialog.Builder(a).setItems(new String[]{"重新查询此插件","查看状态"},(d,n)->{if(n==0)query(g,generation);else new AlertDialog.Builder(a).setTitle(g.name).setMessage(g.status).setPositiveButton("关闭",null).show();}).show()));
   list.addView(row);g.content=new LinearLayout(a);g.content.setOrientation(1);list.addView(g.content);View line=new View(a);line.setBackgroundColor(light?0xffe7ebee:0xff294752);list.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));groups.add(g);render(g);
  }
  dialog.setContentView(root);dialog.setOnDismissListener(d->{closed=true;generation++;for(Group g:groups)if(g.task!=null)g.task.cancel(true);pool.shutdownNow();});dialog.show();
  Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);int width=a.getResources().getDisplayMetrics().widthPixels,height=a.getResources().getDisplayMetrics().heightPixels;w.setLayout(Math.min(width-dp(16),dp(640)),(int)(height*.84));w.setGravity(width<dp(600)?Gravity.BOTTOM:Gravity.CENTER);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=w.getAttributes();lp.dimAmount=.5f;w.setAttributes(lp);}
  start();
 }
 boolean valid(int token){return !closed&&!a.isFinishing()&&!a.isDestroyed()&&generation==token;}
 void start(){if(closed)return;int token=++generation;for(Group g:groups)query(g,token);}
 void query(Group g,int token){
  if(closed)return;if(g.task!=null)g.task.cancel(true);int request=++g.request;g.streams=new JSONArray();g.expanded=false;g.status="等待查询";render(g);
  String base=g.addon.optString("base");if(base.isEmpty()){g.status="查询失败 · 插件缺少地址";render(g);return;}
  String url=base+"/stream/"+type+"/"+Net.esc(id)+".json";
  g.task=pool.submit(()->{
   if(!valid(token)||request!=g.request)return;
   a.runOnUiThread(()->{if(valid(token)&&request==g.request){g.status="查询中…";render(g);}});
   try{JSONArray found=StremioStreams.fetch(url,25000);a.runOnUiThread(()->{if(!valid(token)||request!=g.request)return;g.streams=found;g.status=found.length()==0?"无条目":found.length()+" 个条目";g.expanded=found.length()>0&&expandedCount()<2;render(g);});}
   catch(Exception e){String reason=LinkProbe.reason(e);a.runOnUiThread(()->{if(!valid(token)||request!=g.request)return;g.status="查询失败 · "+reason;render(g);});}
  });
 }
 int expandedCount(){int n=0;for(Group g:groups)if(g.expanded)n++;return n;}
 JSONArray collected(){JSONArray all=new JSONArray();for(Group g:groups)for(int i=0;i<g.streams.length();i++)all.put(g.streams.optJSONObject(i));return all;}
 void render(Group g){
  String state=g.status.startsWith("查询失败")?"查询失败":g.status;
  g.header.setText(g.name+"\n"+state+"  "+(g.expanded?"⌃":"⌄"));g.content.removeAllViews();
  if(g.expanded){
   for(int i=0;i<Math.min(100,g.streams.length());i++){
    JSONObject s=g.streams.optJSONObject(i);if(s==null)continue;String kind=StremioStreams.kind(s);boolean playable=StremioStreams.playable(s);
    LinearLayout item=new LinearLayout(a);item.setOrientation(1);item.setPadding(dp(10),dp(6),dp(10),dp(6));
    String title=s.optString("name",kind).replace('\n',' ').trim();TextView t=text((playable?"▶  ":"·  ")+title,15,playable?ink:muted);t.setTypeface(null,1);t.setPadding(dp(4),dp(2),dp(4),dp(2));item.addView(t);
    TextView k=text(kind,11,accent);k.setPadding(dp(4),0,dp(4),0);item.addView(k);
    String desc=s.optString("title",s.optString("description","")).trim();if(!desc.isEmpty()){TextView d=text(desc,12,muted);d.setMaxLines(4);d.setPadding(dp(4),dp(2),dp(4),dp(2));item.addView(d);}
    item.setOnClickListener(v->choose(g,s));g.content.addView(item,new LinearLayout.LayoutParams(-1,-2));
   }
   if(g.streams.length()==0){TextView status=text(g.status,12,muted);g.content.addView(status);}
  }
  int count=0,done=0;for(Group x:groups){count+=x.streams.length();if(!x.status.equals("等待查询")&&!x.status.equals("查询中…"))done++;}
  summary.setText(groups.size()+" 个插件 · "+count+" 个条目 · 已完成 "+done+"/"+groups.size());
 }
 void choose(Group g,JSONObject s){
  if(!StremioStreams.playable(s)){new AlertDialog.Builder(a).setTitle(g.name).setMessage(StremioStreams.kind(s)+"\n\n这个条目没有本应用能播放的地址。").setPositiveButton("知道了",null).show();return;}
  JSONArray all=collected();
  if(!s.has("url")||s.has("infoHash")){dialog.dismiss();host.play(s,all);return;}
  // 直链：真实地址检查后再交给原播放流程
  ProgressDialog wait=ProgressDialog.show(a,"正在检查播放地址",g.name,true,true);int token=generation;
  Thread t=new Thread(()->{
   JSONObject hints=s.optJSONObject("behaviorHints");JSONObject proxy=hints==null?null:hints.optJSONObject("proxyHeaders");JSONObject req=proxy==null?null:proxy.optJSONObject("request");
   LinkProbe.Result r=LinkProbe.probe(s.optString("url"),req==null?new HashMap<>():TvBoxPlay.map(req),4000);
   a.runOnUiThread(()->{if(!wait.isShowing()||closed||token!=generation)return;wait.dismiss();PlaybackCheck.decide(a,g.name+" · "+s.optString("name").replace('\n',' '),s.optString("url"),"Stremio 直链",r,()->{dialog.dismiss();host.play(s,all);});});
  },"stremio-check");t.setDaemon(true);t.start();
  wait.setOnCancelListener(d->{});
 }
}
