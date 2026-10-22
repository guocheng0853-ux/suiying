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
 final ExecutorService pool=Executors.newFixedThreadPool(3);final ArrayList<Group> groups=new ArrayList<>();final Dialog dialog;final TextView summary;ScrollView resultScroll;boolean closed;int generation;
 String quality="all",hdr="all",fps="all",kind="all";int minutes;LinearLayout empty;TextView emptyText;final ArrayList<Button> chips=new ArrayList<>();
 static final String[][] FILTERS={{"画质","all","全部","4k","4K","1080","1080p","720","720p 及以下"},{"HDR","all","全部","hdr","HDR","dv","杜比视界","sdr","SDR"},{"帧率","all","全部","60","60fps"},{"类型","all","全部","direct","直链","p2p","P2P"}};
 final class Group {JSONObject addon;String name;TextView header;LinearLayout content;JSONArray streams=new JSONArray();String status="等待查询";boolean expanded;int request;Future<?> task;}
 int dp(int n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
 TextView text(String s,int size,int color){TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(12),dp(8),dp(12),dp(8));return t;}
 Button button(String label,Runnable action){Button b=new Button(a);b.setText(label);b.setTextColor(ink);b.setAllCaps(false);b.setBackgroundColor(Color.TRANSPARENT);b.setOnClickListener(v->action.run());return b;}

 StremioSourceDialog(Activity owner,JSONArray all,String id,String type,JSONObject meta,Play play,Runnable add){
  a=owner;this.id=id;this.type=type;host=play;addAddon=add;addons=AddonRegistry.forResource(all==null?new JSONArray():all,"stream",type,id);
  light=CinemaTheme.light(a);boolean goldUi=!light&&CinemaTheme.gold(a),clayUi=light&&CinemaTheme.clay(a);ink=light?(clayUi?CinemaTheme.SLATE:0xff202830):goldUi?CinemaTheme.INK:0xfff1f4f6;muted=light?(clayUi?CinemaTheme.STONE:0xff697580):goldUi?CinemaTheme.MUTED:0xffb0c1cc;bg=light?(clayUi?CinemaTheme.PAPER:0xfffafbfc):goldUi?CinemaTheme.CARD:0xff1c1b21;accent=light?(clayUi?CinemaTheme.CLAY_DEEP:0xff2c6b8a):goldUi?CinemaTheme.GOLD:0xffd0bfff;
  dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
  LinearLayout root=new LinearLayout(a);root.setOrientation(1);root.setPadding(dp(10),dp(6),dp(10),dp(14));GradientDrawable shape=new GradientDrawable();shape.setColor(bg);shape.setCornerRadius(dp(24));root.setBackground(shape);
  TextView handle=text("━━",14,muted);handle.setGravity(Gravity.CENTER);root.addView(handle);
  LinearLayout top=new LinearLayout(a);top.setGravity(Gravity.CENTER_VERTICAL);TextView heading=text("搜索其他播放源",20,ink);heading.setTypeface(null,1);top.addView(heading,new LinearLayout.LayoutParams(0,-2,1));top.addView(button("全部重试",()->start()));top.addView(button("关闭",()->dialog.dismiss()));root.addView(top);
  String name=meta==null?"":meta.optString("name");String ep=StremioStreams.episodeLabel(id);TextView sub=text(name+(ep.isEmpty()?"":"  ·  "+ep),13,ink);root.addView(sub);
  summary=text("正在准备插件…",12,muted);root.addView(summary);minutes=meta==null?0:StreamInfo.minutes(meta.optString("runtime"));
  HorizontalScrollView chipScroll=new HorizontalScrollView(a);chipScroll.setHorizontalScrollBarEnabled(false);LinearLayout chipRow=new LinearLayout(a);chipRow.setPadding(dp(6),0,dp(6),dp(4));for(int f=0;f<FILTERS.length;f++){final int which=f;Button c=chip("");c.setOnClickListener(v->{pickFilter(which);});chips.add(c);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(34));cp.rightMargin=dp(6);chipRow.addView(c,cp);}chipScroll.addView(chipRow);root.addView(chipScroll);updateChips();
  empty=new LinearLayout(a);empty.setOrientation(1);empty.setGravity(Gravity.CENTER);empty.setVisibility(View.GONE);TextView symbol=text("▱",62,muted);symbol.setGravity(Gravity.CENTER);empty.addView(symbol);emptyText=text("",16,muted);emptyText.setGravity(Gravity.CENTER);empty.addView(emptyText);Button clear=chip("清除筛选");clear.setOnClickListener(v->{quality=hdr=fps=kind="all";updateChips();renderAll();});empty.addView(clear,new LinearLayout.LayoutParams(-2,dp(36)));root.addView(empty,new LinearLayout.LayoutParams(-1,0,1));
  ScrollView scroll=new ScrollView(a);resultScroll=scroll;LinearLayout list=new LinearLayout(a);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  if(addons.length()==0){TextView none=text("还没有支持此影片的视频源插件（需要声明 stream 资源）。",14,muted);list.addView(none);if(addAddon!=null)list.addView(button("添加插件",()->{dialog.dismiss();addAddon.run();}));summary.setText("0 个插件");}
  for(int i=0;i<addons.length();i++){
   JSONObject addon=addons.optJSONObject(i);if(addon==null)continue;Group g=new Group();g.addon=addon;JSONObject manifest=addon.optJSONObject("manifest");g.name=manifest!=null&&!manifest.optString("name").isEmpty()?manifest.optString("name"):addon.optString("name","插件 "+(i+1));
   LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);g.header=text("",15,ink);g.header.setOnClickListener(v->{g.expanded=!g.expanded;render(g);});row.addView(g.header,new LinearLayout.LayoutParams(0,-2,1));
   row.addView(button("⋮",()->new AlertDialog.Builder(a).setItems(new String[]{"重新查询此插件","查看状态"},(d,n)->{if(n==0)query(g,generation);else new AlertDialog.Builder(a).setTitle(g.name).setMessage(g.status).setPositiveButton("关闭",null).show();}).show()));
   list.addView(row);g.content=new LinearLayout(a);g.content.setOrientation(1);list.addView(g.content);View line=new View(a);line.setBackgroundColor(light?0xffe7ebee:0xff294752);list.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));groups.add(g);render(g);
  }
  dialog.setContentView(root);dialog.setOnDismissListener(d->{closed=true;generation++;for(Group g:groups)if(g.task!=null)g.task.cancel(true);pool.shutdownNow();});dialog.show();
  Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);int width=a.getResources().getDisplayMetrics().widthPixels,height=a.getResources().getDisplayMetrics().heightPixels;w.setLayout(Math.min(width,dp(640)),(int)(height*.76));w.setGravity(width<dp(600)?Gravity.BOTTOM:Gravity.CENTER);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=w.getAttributes();lp.dimAmount=.5f;w.setAttributes(lp);}
  start();
 }
 Button chip(String label){Button b=new Button(a);b.setText(label);b.setAllCaps(false);b.setTextSize(13);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(14),0,dp(14),0);return b;}
 String value(int f){return f==0?quality:f==1?hdr:f==2?fps:kind;}
 void pickFilter(int f){String[] opts=FILTERS[f];String[] labels=new String[(opts.length-1)/2];int checked=0;for(int i=0;i<labels.length;i++){labels[i]=opts[2+i*2];if(opts[1+i*2].equals(value(f)))checked=i;}new AlertDialog.Builder(a).setTitle(opts[0]).setSingleChoiceItems(labels,checked,(d,n)->{String next=opts[1+n*2];if(f==0)quality=next;else if(f==1)hdr=next;else if(f==2)fps=next;else kind=next;updateChips();renderAll();d.dismiss();}).setNegativeButton("取消",null).show();}
 void cycle(int f){String[] o=FILTERS[f];int at=1;for(int i=1;i<o.length;i+=2)if(o[i].equals(value(f)))at=i;String next=o[at+2<o.length?at+2:1];if(f==0)quality=next;else if(f==1)hdr=next;else if(f==2)fps=next;else kind=next;updateChips();renderAll();}
 void updateChips(){for(int f=0;f<chips.size();f++){String[] o=FILTERS[f];String label=o[2];for(int i=1;i<o.length;i+=2)if(o[i].equals(value(f)))label=o[i+1];boolean on=!value(f).equals("all");Button c=chips.get(f);c.setText(o[0]+" · "+label+"  ⌄");c.setTextColor(ink);GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(17));if(on){g.setColor(light?0xffeee8ff:0xff302a3e);g.setStroke(dp(1),accent);}else{g.setColor(0);g.setStroke(dp(1),light?0xffc9d3da:0xff3d5f6b);}c.setBackground(g);}}
 boolean visible(JSONObject s){return StreamInfo.parse(s).matches(quality,hdr,fps,kind);}
 void renderAll(){for(Group g:groups)render(g);}
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
   int shown=0;for(int i=0;i<Math.min(100,g.streams.length());i++){
    JSONObject s=g.streams.optJSONObject(i);if(s==null||!visible(s))continue;shown++;StreamInfo info=StreamInfo.parse(s);String kind=StremioStreams.kind(s);boolean playable=StremioStreams.playable(s);
    LinearLayout item=new LinearLayout(a);item.setOrientation(1);item.setPadding(dp(10),dp(6),dp(10),dp(6));
    String title=s.optString("name",kind).replace('\n',' ').trim();TextView t=text((playable?"▶  ":"·  ")+title,15,playable?ink:muted);t.setTypeface(null,1);t.setPadding(dp(4),dp(2),dp(4),dp(2));item.addView(t);
    List<String> tags=info.tags(minutes);TextView k=text(kind+(tags.isEmpty()?"":"  ·  "+String.join(" · ",tags)),11,accent);k.setPadding(dp(4),0,dp(4),0);item.addView(k);
    String desc=s.optString("title",s.optString("description","")).trim();if(!desc.isEmpty()){TextView d=text(desc,12,muted);d.setMaxLines(4);d.setPadding(dp(4),dp(2),dp(4),dp(2));item.addView(d);}
    item.setOnClickListener(v->choose(g,s));g.content.addView(item,new LinearLayout.LayoutParams(-1,-2));
   }
   if(g.streams.length()==0){TextView status=text(g.status,12,muted);g.content.addView(status);}else if(shown==0){g.content.addView(text(g.streams.length()+" 个条目不符合当前筛选",12,muted));}
  }
  int count=0,done=0,match=0;for(Group x:groups){count+=x.streams.length();for(int i=0;i<x.streams.length();i++)if(visible(x.streams.optJSONObject(i)))match++;if(!x.status.equals("等待查询")&&!x.status.equals("查询中…"))done++;}
  boolean filtered=!(quality+hdr+fps+kind).equals("allallallall");
  summary.setText((done<groups.size()?"查询中 · ":"")+"已返回 "+done+"/"+groups.size()+" 个插件 · "+(filtered?"符合筛选 "+match+"/":"")+count+" 个条目");
  if(empty!=null){boolean finished=done==groups.size()&&!groups.isEmpty();boolean none=finished&&count==0,hidden=count>0&&match==0;empty.setVisibility(none||hidden?View.VISIBLE:View.GONE);emptyText.setText(none?"暂无播放源 · 可在下方查看插件状态":"暂无符合筛选的播放源");if(resultScroll!=null){LinearLayout.LayoutParams rp=(LinearLayout.LayoutParams)resultScroll.getLayoutParams();rp.weight=(none||hidden)?0:1;rp.height=(none||hidden)?dp(110):0;resultScroll.setLayoutParams(rp);}if(empty.getChildCount()>2)empty.getChildAt(2).setVisibility(hidden?View.VISIBLE:View.GONE);}
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
