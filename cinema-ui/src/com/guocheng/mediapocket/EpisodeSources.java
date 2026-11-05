package com.guocheng.mediapocket;
import android.app.Activity;import android.app.Dialog;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/**
 * 0.1.173：播放页「选集」里点一集，左边弹出一块同样的雾面玻璃窗口，直接列出这一集的片源。
 * 插件并行查询（同详情页 DetailChrome.autoPick），先返回的先显示，后到的按分数插进去；最上面一行「自动选择最佳」。
 * 选中后关掉播放页，由主界面直接用这条片源起播（和详情页点播放走同一个 play，带上同一集的全部片源做备用），
 * 中间不再打开剧集详情页让用户重选。Emby 和影视仓的剧集仍然原地切换，不经过这里。
 */
final class EpisodeSources {
 static final Map<Activity,EpisodeSources> ALL=new WeakHashMap<>();
 final Activity a;Dialog dialog;LinearLayout list;TextView status,title;String episode="";int ticket;final JSONArray found=new JSONArray();final HashSet<String> seen=new HashSet<>();
 JSONObject meta;String type;
 EpisodeSources(Activity a){this.a=a;}
 int dp(float v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 static void close(Activity a){EpisodeSources s=ALL.remove(a);if(s!=null)s.dismiss();}
 static boolean open(Activity a){EpisodeSources s=ALL.get(a);return s!=null&&s.dialog!=null&&s.dialog.isShowing();}
 /** 返回 false 表示没有能查片源的插件（或窗口弹不出来），调用方退回原来的详情页流程。 */
 static boolean show(Activity a,JSONObject meta,String id,String type,String label){
  if(a==null||a.isFinishing()||id==null||id.isEmpty())return false;
  JSONArray addons=AddonRegistry.forResource(addons(a),"stream",type,id);if(addons.length()==0)return false;
  EpisodeSources s=ALL.get(a);if(s==null){s=new EpisodeSources(a);ALL.put(a,s);}
  return s.load(meta,id,type,label,addons);
 }
 static JSONArray addons(Activity a){String raw=a.getIntent()==null?null:a.getIntent().getStringExtra("addons");if(raw==null||raw.isEmpty())raw=a.getSharedPreferences("data",0).getString("addons","[]");try{return new JSONArray(raw);}catch(JSONException e){return new JSONArray();}}
 boolean load(JSONObject meta,String id,String type,String label,JSONArray addons){
  this.meta=meta;this.type=type;episode=id;final int token=++ticket;
  while(found.length()>0)found.remove(found.length()-1);seen.clear();
  if(dialog==null||!dialog.isShowing()){if(!build())return false;}
  title.setText(label==null||label.isEmpty()?"选择片源":label);
  status.setText("正在向 "+addons.length()+" 个插件查找片源…");list.removeAllViews();
  final String pref=PlaybackPrefs.audio(a.getSharedPreferences("data",0));
  new Thread(()->{ArrayList<String> urls=new ArrayList<>();for(int i=0;i<addons.length();i++){JSONObject ad=addons.optJSONObject(i);String base=ad==null?"":ad.optString("base");if(!base.isEmpty())urls.add(base+"/stream/"+type+"/"+Net.esc(id)+".json");}
   java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(SourceCardRules.poolSize(urls.size()));int[] answered={0};
   try{java.util.concurrent.ExecutorCompletionService<JSONArray> done=new java.util.concurrent.ExecutorCompletionService<>(pool);for(String url:urls)done.submit(()->StremioStreams.fetch(url,SourceCardRules.FETCH_MS));
    long end=System.nanoTime()+java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(SourceCardRules.FETCH_MS+2000);
    for(int n=0;n<urls.size();n++){long left=end-System.nanoTime();java.util.concurrent.Future<JSONArray> f=left>0?done.poll(left,java.util.concurrent.TimeUnit.NANOSECONDS):null;if(f==null)break;JSONArray r;try{r=f.get();}catch(Exception failed){continue;}answered[0]++;if(r==null||r.length()==0)continue;
     final JSONArray batch=r;a.runOnUiThread(()->{if(token!=ticket||dialog==null)return;for(int i=0;i<batch.length();i++){JSONObject s=batch.optJSONObject(i);if(s==null||!seen.add(EpisodeSourceRules.key(s)))continue;found.put(s);}render(pref,false);});}
   }catch(InterruptedException stopped){Thread.currentThread().interrupt();}finally{pool.shutdownNow();}
   a.runOnUiThread(()->{if(token!=ticket||dialog==null)return;render(pref,true);});},"episode-sources").start();
  return true;
 }
 void render(String pref,boolean finished){
  int[] order=EpisodeSourceRules.order(found,pref,EpisodeSourceRules.LIMIT);
  int total=0;for(int i=0;i<found.length();i++)if(StremioStreams.playable(found.optJSONObject(i)))total++;
  status.setText(order.length==0?(finished?"这一集没有找到可播放的片源":"正在查找片源…"):(finished?"共 "+total+" 个片源":"已找到 "+total+" 个，还在查找…")+(total>order.length?"，列出最好的 "+order.length+" 个":""));
  list.removeAllViews();if(order.length==0)return;
  int minutes=StreamInfo.minutes(meta==null?"":meta.optString("runtime"));
  row("自动选择最佳","按清晰度、做种数和音轨偏好挑第一条",true,()->pick(found.optJSONObject(order[0])));
  for(int at:order){JSONObject s=found.optJSONObject(at);StreamInfo info=StreamInfo.parse(s);String line=EpisodeSourceRules.detail(info.tags(minutes),s.optString("name"));if(!s.optString("externalUrl").isEmpty())line=StremioStreams.kind(s);row(SourceCardRules.name(s),line,false,()->pick(s));}
 }
 void pick(JSONObject stream){if(stream==null)return;JSONArray all=new JSONArray();for(int at:EpisodeSourceRules.order(found,PlaybackPrefs.audio(a.getSharedPreferences("data",0)),EpisodeSourceRules.LIMIT))all.put(found.optJSONObject(at));
  ticket++;PlayerSidePanel.close(a);dismiss();ALL.remove(a);AutoNext.handoffStream(a,meta,episode,type,stream,all);}
 void row(String head,String detail,boolean accent,Runnable act){
  int gold=PlayerTheme.playbackAccent(CinemaTheme.gold(a));LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setMinimumHeight(dp(52));row.setPadding(dp(12),dp(8),dp(12),dp(8));
  TextView t=new TextView(a);t.setText(head);t.setTextSize(14);t.setTextColor(accent?gold:0xffffffff);t.setMaxLines(2);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setIncludeFontPadding(false);row.addView(t);
  if(detail!=null&&!detail.isEmpty()){TextView d=new TextView(a);d.setText(detail);d.setTextSize(12);d.setTextColor(0x99ffffff);d.setMaxLines(2);d.setEllipsize(android.text.TextUtils.TruncateAt.END);d.setPadding(0,dp(4),0,0);row.addView(d);}
  Glass.row(a,row,null,gold);row.setOnClickListener(x->act.run());CinemaControls.group(row,head+(detail==null?"":"，"+detail));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(2);list.addView(row,lp);
 }
 boolean build(){
  dialog=new Dialog(a,android.R.style.Theme_Translucent_NoTitleBar);dialog.setCanceledOnTouchOutside(true);dialog.setOnCancelListener(d->{ticket++;dismiss();ALL.remove(a);});
  LinearLayout sheet=new LinearLayout(a);sheet.setOrientation(LinearLayout.VERTICAL);sheet.setPadding(dp(12),dp(12),dp(12),dp(12));
  LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);title=new TextView(a);title.setTextSize(16);title.setTextColor(0xffffffff);title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setPadding(dp(8),0,dp(8),0);head.addView(title,new LinearLayout.LayoutParams(0,-2,1f));
  TextView x=new TextView(a);x.setText("✕");x.setTextSize(16);x.setTextColor(0xccffffff);x.setGravity(Gravity.CENTER);Glass.row(a,x,null,0);x.setContentDescription("关闭片源列表");x.setOnClickListener(v->{ticket++;dismiss();ALL.remove(a);});head.addView(x,new LinearLayout.LayoutParams(dp(40),dp(40)));
  sheet.addView(head,new LinearLayout.LayoutParams(-1,dp(48)));
  status=new TextView(a);status.setTextSize(12);status.setTextColor(PlayerTheme.note());status.setPadding(dp(8),dp(2),dp(8),dp(8));sheet.addView(status);
  ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);sheet.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
  dialog.setContentView(sheet);
  Window w=dialog.getWindow();if(w!=null){Glass.window(a,w,GlassRules.RADIUS_DP);View decor=a.getWindow().getDecorView();int screenW=decor.getWidth()>0?decor.getWidth():a.getResources().getDisplayMetrics().widthPixels;int screenH=decor.getHeight()>0?decor.getHeight():a.getResources().getDisplayMetrics().heightPixels;
   int width=PlayerOverlayRules.panelWidthPx(a.getResources().getDisplayMetrics().widthPixels,a.getResources().getDisplayMetrics().density);int edge=dp(GlassRules.EDGE_DP);int[] size=GlassRules.sideWindow(screenH,width,edge);w.setLayout(size[0],size[1]);
   WindowManager.LayoutParams lp=w.getAttributes();lp.gravity=Gravity.END|Gravity.CENTER_VERTICAL;lp.x=EpisodeSourceRules.windowX(screenW,width,edge);lp.y=0;w.setAttributes(lp);w.setWindowAnimations(0);}
  dialog.setOnKeyListener((d,code,e)->{if(code==KeyEvent.KEYCODE_BACK){if(e.getAction()==KeyEvent.ACTION_UP){ticket++;dismiss();ALL.remove(a);}return true;}return false;});
  if(!Glass.showImmersive(a,dialog)){dialog=null;return false;}
  sheet.setAlpha(0f);sheet.setTranslationX(dp(24));sheet.animate().alpha(1f).translationX(0).setDuration(PlayerMotionRules.PANEL_IN_MS).start();
  return true;
 }
 void dismiss(){final Dialog d=dialog;dialog=null;if(d!=null)try{d.dismiss();}catch(RuntimeException ignored){}}
}
