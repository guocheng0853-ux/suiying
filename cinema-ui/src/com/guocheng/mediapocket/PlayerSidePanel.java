package com.guocheng.mediapocket;
import android.app.Activity;import android.graphics.drawable.GradientDrawable;import android.os.Handler;import android.os.Looper;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/**
 * 右侧滑出的统一面板。宽 480dp，小屏放不下时用宽度的 45%。
 * 页签：字幕、音轨、选集、章节、码率、线路、定时关闭、流信息。
 * OpenSubtitles 需要 API 密钥，这里不接，改走已经安装的字幕插件。
 */
final class PlayerSidePanel {
 static final String SUB="sub",AUDIO="audio",EPISODES="episodes",CHAPTERS="chapters",BITRATE="bitrate",ROUTES="routes",SLEEP="sleep",INFO="info";
 static final Map<Activity,PlayerSidePanel> ALL=new WeakHashMap<>();
 static final long[] RATES={500_000L,1_000_000L,2_000_000L,4_000_000L,8_000_000L,20_000_000L,40_000_000L,140_000_000L};
 static final Handler TIMER=new Handler(Looper.getMainLooper());
 static Runnable sleepTask;static long sleepDeadline;
 final Activity a;FrameLayout scrim;LinearLayout sheet;String tab=SUB;TextView body;HorizontalScrollView tabs;
 PlayerSidePanel(Activity a){this.a=a;}
 static boolean open(Activity a){PlayerSidePanel p=ALL.get(a);return p!=null&&p.scrim!=null&&p.scrim.getParent()!=null;}
 static void close(Activity a){PlayerSidePanel p=ALL.get(a);if(p!=null)p.dismiss();}
 static void open(Activity a,String tab){try{PlayerSidePanel p=ALL.get(a);if(p==null){p=new PlayerSidePanel(a);ALL.put(a,p);}p.show(tab==null||tab.isEmpty()?SUB:tab);}catch(Exception ignored){}}
 int dp(int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 void show(String next){tab=next;Object root=OnlineSubtitleSearch.field(a,"root");if(!(root instanceof FrameLayout))return;FrameLayout host=(FrameLayout)root;if(scrim==null)build(host);scrim.setVisibility(View.VISIBLE);fill();int w=sheet.getLayoutParams().width;sheet.setTranslationX(w>0?w:dp(480));sheet.animate().translationX(0).setDuration(220).start();}
 void build(FrameLayout host){
  scrim=new FrameLayout(a);scrim.setBackgroundColor(0x66000000);scrim.setOnClickListener(v->dismiss());
  sheet=new LinearLayout(a);sheet.setOrientation(LinearLayout.VERTICAL);sheet.setClickable(true);
  GradientDrawable bg=new GradientDrawable();bg.setColor(0xf01c1b22);bg.setCornerRadius(dp(16));sheet.setBackground(bg);sheet.setPadding(dp(12),dp(12),dp(12),dp(12));
  int width=PlayerOverlayRules.panelWidthPx(a.getResources().getDisplayMetrics().widthPixels,a.getResources().getDisplayMetrics().density);
  tabs=new HorizontalScrollView(a);tabs.setHorizontalScrollBarEnabled(false);sheet.addView(tabs,new LinearLayout.LayoutParams(-1,dp(40)));
  body=new TextView(a);ScrollView scroll=new ScrollView(a);LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setTag("mp-panel-box");scroll.addView(box);sheet.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
  FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width,-1,Gravity.END);lp.topMargin=dp(8);lp.bottomMargin=dp(8);scrim.addView(sheet,lp);host.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
 }
 LinearLayout box(){return sheet==null?null:(LinearLayout)((ScrollView)sheet.getChildAt(1)).getChildAt(0);}
 void fill(){LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);String[] ids={SUB,AUDIO,EPISODES,CHAPTERS,BITRATE,ROUTES,SLEEP,INFO};String[] names={"字幕","音轨","选集","章节","码率","线路","定时关闭","流信息"};
  for(int i=0;i<ids.length;i++){final String id=ids[i];TextView chip=new TextView(a);chip.setText(names[i]);chip.setTextColor(id.equals(tab)?0xff101820:0xffffffff);chip.setTextSize(13);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(12),0,dp(12),0);GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(14));g.setColor(id.equals(tab)?0xffd7e7f5:0x00000000);chip.setBackground(g);chip.setOnClickListener(v->{tab=id;fill();});row.addView(chip,new LinearLayout.LayoutParams(-2,dp(32)));}
  tabs.removeAllViews();tabs.addView(row);
  LinearLayout box=box();if(box==null)return;box.removeAllViews();
  if(SUB.equals(tab))subtitles(box);else if(AUDIO.equals(tab))audio(box);else if(EPISODES.equals(tab))episodes(box);else if(CHAPTERS.equals(tab))chapters(box);else if(BITRATE.equals(tab))bitrate(box);else if(ROUTES.equals(tab))routes(box);else if(SLEEP.equals(tab))sleep(box);else info(box);
 }
 void note(LinearLayout box,String text){TextView t=new TextView(a);t.setText(text);t.setTextColor(0xb3ffffff);t.setTextSize(13);t.setPadding(0,dp(8),0,dp(8));box.addView(t);}
 void row(LinearLayout box,String label,boolean selected,Runnable action){TextView t=new TextView(a);t.setText((selected?"● ":"○ ")+label);t.setTextColor(0xffffffff);t.setTextSize(15);t.setPadding(dp(4),dp(10),dp(4),dp(10));t.setOnClickListener(v->action.run());box.addView(t);}
 void subtitles(LinearLayout box){
  note(box,"主字幕");EmbyPlayerUi ui=EmbyPlayerUi.ALL.get(a);JSONArray subs=ui==null?new JSONArray():ui.subs;
  row(box,"关闭",false,()->{if(ui!=null)ui.chooseSub(direct(),-1,"off");else EmbyPlayerUi.fallback(a,"subtitles");dismiss();});
  if(subs.length()==0)row(box,"播放器里的字幕",false,()->{EmbyPlayerUi.fallback(a,"subtitles");dismiss();});
  for(int i=0;i<subs.length();i++){JSONObject t=subs.optJSONObject(i);if(t==null)continue;int index=t.optInt("index",-1);String lang=t.optString("lang");row(box,ui.describe(t,false),lang.equals(a.getSharedPreferences("data",0).getString("emby.subLang","")),()->{ui.chooseSub(direct(),index,lang);dismiss();});}
  note(box,"次字幕");note(box,"LibVLC 同时只能显示一条字幕。次字幕会记住，同步偏移作用在当前这条上。");
  row(box,"不使用次字幕",a.getSharedPreferences("data",0).getInt("sub.secondary",-2)==-2,()->{a.getSharedPreferences("data",0).edit().putInt("sub.secondary",-2).apply();fill();});
  for(int i=0;i<subs.length();i++){JSONObject t=subs.optJSONObject(i);if(t==null)continue;int index=t.optInt("index",-1);row(box,"次 · "+ui.describe(t,false),a.getSharedPreferences("data",0).getInt("sub.secondary",-2)==index,()->{a.getSharedPreferences("data",0).edit().putInt("sub.secondary",index).apply();fill();});}
  int offset=a.getSharedPreferences("data",0).getInt("sub.offsetMs",0);
  note(box,"字幕同步  "+(offset>0?"+":"")+offset+" ms");
  LinearLayout sync=new LinearLayout(a);
  sync.addView(chip("−100ms",()->offsetBy(-1)));sync.addView(chip("归零",()->{a.getSharedPreferences("data",0).edit().putInt("sub.offsetMs",0).apply();applyOffset(0);fill();}));sync.addView(chip("+100ms",()->offsetBy(1)));
  box.addView(sync);
  note(box,"在线搜索需要 OpenSubtitles 的 API 密钥，当前没有配置，已跳过。");
  row(box,"用已安装的字幕插件搜索",false,()->{dismiss();Object session=OnlineSubtitleSearch.field(a,"session");if(session!=null)OnlineSubtitleSearch.start(session);});
 }
 void offsetBy(int direction){int next=EmbyCue.stepSubtitleOffset(a.getSharedPreferences("data",0).getInt("sub.offsetMs",0),direction);a.getSharedPreferences("data",0).edit().putInt("sub.offsetMs",next).apply();applyOffset(next);fill();}
 void applyOffset(int ms){try{Object video=OnlineSubtitleSearch.field(a,"video");Object player=video==null?null:OnlineSubtitleSearch.field(video,"player");if(player!=null)OnlineSubtitleSearch.call(player,"setSpuDelay",new Class<?>[]{long.class},ms*1000L);}catch(Exception ignored){}}
 boolean direct(){JSONObject stream=TrackSelection.stream(a);return stream==null||stream.optBoolean("emby.direct",true);}
 TextView chip(String text,Runnable action){TextView t=new TextView(a);t.setText(text);t.setTextColor(0xffffffff);t.setTextSize(14);t.setPadding(dp(12),dp(8),dp(12),dp(8));t.setOnClickListener(v->action.run());return t;}
 void audio(LinearLayout box){EmbyPlayerUi ui=EmbyPlayerUi.ALL.get(a);JSONArray audio=ui==null?new JSONArray():ui.audio;if(audio.length()==0){row(box,"播放器里的音轨",false,()->{EmbyPlayerUi.fallback(a,"audio");dismiss();});return;}for(int i=0;i<audio.length();i++){JSONObject t=audio.optJSONObject(i);if(t==null)continue;int index=t.optInt("index",-1),ordinal=t.optInt("ordinal",i);String lang=t.optString("language"),codec=t.optString("codec");row(box,ui.describe(t,true),false,()->{ui.chooseAudio(direct(),index,ordinal,lang,codec);dismiss();});}}
 void episodes(LinearLayout box){JSONObject meta=PlayerOverlay.meta(a);ArrayList<JSONObject> list=EpisodeActions.list(meta);if(list.isEmpty()){note(box,"没有可选择的剧集");if(meta.has("fusion.config"))row(box,"打开片源选集",false,()->{dismiss();PlaybackSitePicker.selectEpisode(a);});return;}String cur=PlayerOverlay.id(a);for(JSONObject video:list){String id=video.optString("id");row(box,EpisodeActions.label(video),cur.equals(id),()->{dismiss();if(!id.isEmpty()&&!id.equals(cur))AutoNext.handoff(a,meta,id,PlayerOverlay.type(a));});}}
 void chapters(LinearLayout box){EmbyPlayerUi ui=EmbyPlayerUi.ALL.get(a);if(ui==null||ui.chapters.length()==0){note(box,"没有章节");return;}for(int i=0;i<ui.chapters.length();i++){JSONObject c=ui.chapters.optJSONObject(i);if(c==null)continue;long at=c.optLong("startMs");row(box,c.optString("name"),false,()->{EmbyPlayback.seek(a,(int)Math.min(Integer.MAX_VALUE,at));dismiss();});}}
 void bitrate(LinearLayout box){JSONObject stream=TrackSelection.stream(a);if(stream==null||stream.optString("emby.item").isEmpty()){note(box,"只有 Emby 片源可以按码率重新请求。");return;}note(box,"选择后会按这个 MaxStreamingBitrate 重新向服务器要播放地址。");long current=a.getSharedPreferences("data",0).getLong("emby.bitrate",140_000_000L);for(long rate:RATES){String label=rate>=1_000_000?(rate/1_000_000)+" Mbps":(rate/1000)+" Kbps";row(box,label,rate==current,()->{a.getSharedPreferences("data",0).edit().putLong("emby.bitrate",rate).apply();dismiss();EmbyPlayback.replayBitrate(a,rate);});}}
 void routes(LinearLayout box){JSONObject stream=TrackSelection.stream(a);JSONObject server=stream==null?null:EmbyStore.get(a,stream.optString("emby.server"));if(server==null){note(box,"当前片源没有可切换的线路。");return;}String[] bases=EmbyCue.bases(server.optString("url"),server.optJSONArray("backups"));if(bases.length==0)note(box,"没有备用线路。");for(String base:bases){row(box,base,base.equalsIgnoreCase(server.optString("url")),()->{try{JSONObject copy=new JSONObject(server.toString());copy.put("url",base);dismiss();EmbyPlayback.playItem(a,copy,stream.optString("emby.item"),stream.optString("emby.mediaSource"),true);}catch(Exception ignored){}});}note(box,"服务器插件提供的额外线路会在打开面板时尝试读取。");EmbyPlayback.IO.execute(()->{try{JSONObject extra=EmbyClient.json(a,server,"GET","/System/Ext/ServerDomains",null);JSONArray items=extra.optJSONArray("Items");if(items==null)items=extra.optJSONArray("Domains");if(items==null||items.length()==0)return;JSONArray found=items;a.runOnUiThread(()->{if(a.isFinishing()||!ROUTES.equals(tab))return;for(int i=0;i<found.length();i++){String domain=found.optString(i);if(domain.isEmpty()&&found.optJSONObject(i)!=null)domain=found.optJSONObject(i).optString("Url",found.optJSONObject(i).optString("Domain"));if(domain.isEmpty())continue;final String url=domain;LinearLayout host=box();if(host==null)return;row(host,url,false,()->{try{JSONObject copy=new JSONObject(server.toString());copy.put("url",url);dismiss();EmbyPlayback.playItem(a,copy,stream.optString("emby.item"),stream.optString("emby.mediaSource"),true);}catch(Exception ignored){}});}});}catch(Exception ignored){}});}
 void sleep(LinearLayout box){note(box,sleepDeadline>System.currentTimeMillis()?"将在 "+Math.max(1,(sleepDeadline-System.currentTimeMillis()+59999)/60000)+" 分钟后暂停并关闭播放页。":"到点后暂停并关闭播放页。");int[] mins={0,15,30,45,60,90};for(int m:mins){row(box,m==0?"关闭定时":m+" 分钟",false,()->arm(m));}}
 void arm(int minutes){TIMER.removeCallbacks(sleepTask);sleepDeadline=0;if(minutes<=0){fill();return;}sleepDeadline=System.currentTimeMillis()+minutes*60000L;sleepTask=()->{sleepDeadline=0;try{Object video=OnlineSubtitleSearch.field(a,"video");if(video!=null)OnlineSubtitleSearch.call(video,"pause",new Class<?>[0]);}catch(Exception ignored){}if(!a.isFinishing()){PlayerOverlay.toast(a,"定时关闭");a.finish();}};TIMER.postDelayed(sleepTask,minutes*60000L);dismiss();PlayerOverlay.toast(a,minutes+" 分钟后关闭");}
 void info(LinearLayout box){StringBuilder s=new StringBuilder();JSONObject stream=TrackSelection.stream(a);Object video=OnlineSubtitleSearch.field(a,"video");String name=PlayerOverlay.name(a);if(!name.isEmpty())s.append("名称  ").append(name).append('\n');if(stream!=null){s.append("方式  ").append(stream.optBoolean("emby.direct",true)?"直接串流":"转码").append('\n');if(!stream.optString("emby.mediaSource").isEmpty())s.append("媒体源  ").append(stream.optString("emby.mediaSource")).append('\n');String url=stream.optString("url");int q=url.indexOf('?');if(q>=0)url=url.substring(0,q);if(!url.isEmpty())s.append("地址  ").append(url).append('\n');}if(video!=null){Object w=OnlineSubtitleSearch.field(video,"vw"),h=OnlineSubtitleSearch.field(video,"vh");if(w instanceof Integer&&h instanceof Integer&&(Integer)w>0)s.append("分辨率  ").append(w).append("×").append(h).append('\n');}int offset=a.getSharedPreferences("data",0).getInt("sub.offsetMs",0);s.append("字幕偏移  ").append(offset).append(" ms\n");s.append("倍速  ").append(PlayerOverlayRules.speedPill(PlaybackPrefs.speed(a.getSharedPreferences("data",0)))).append('\n');if(s.length()==0)s.append("还没有流信息");note(box,s.toString().trim());}
 void dismiss(){if(sheet!=null)sheet.animate().translationX(sheet.getWidth()>0?sheet.getWidth():dp(480)).setDuration(180).withEndAction(()->{if(scrim!=null&&scrim.getParent() instanceof ViewGroup)((ViewGroup)scrim.getParent()).removeView(scrim);scrim=null;sheet=null;}).start();else if(scrim!=null&&scrim.getParent() instanceof ViewGroup){((ViewGroup)scrim.getParent()).removeView(scrim);scrim=null;}}
}
