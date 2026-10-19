package com.guocheng.mediapocket;
import android.app.Activity;import android.graphics.drawable.GradientDrawable;import android.os.Handler;import android.os.Looper;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;
/**
 * 右侧滑出的统一面板。宽 480dp，小屏放不下时用宽度的 45%。
 * 页签：字幕、音轨、选集、线路、章节（有章节时）、码率（Emby）、定时关闭、流信息。
 * 0.1.93：这里是“当前这部片子”的唯一入口：获取字幕（在线 / Whisper / 插件）在字幕页顶部，
 * 换片源 / 换版本 / 换线路在线路页。「更多」只留弹幕、播放设置和播放器级设置，不再重复列这些。
 * OpenSubtitles 需要 API 密钥，这里不接，改走已经安装的字幕插件。
 */
final class PlayerSidePanel {
 static final String SUB="sub",AUDIO="audio",EPISODES="episodes",CHAPTERS="chapters",BITRATE="bitrate",ROUTES="routes",SLEEP="sleep",INFO="info";
 static final Map<Activity,PlayerSidePanel> ALL=new WeakHashMap<>();
 static final long[] RATES={500_000L,1_000_000L,2_000_000L,4_000_000L,8_000_000L,20_000_000L,40_000_000L,140_000_000L};
 static final Handler TIMER=new Handler(Looper.getMainLooper());
 static Runnable sleepTask;static long sleepDeadline;
 final Activity a;FrameLayout scrim;LinearLayout sheet;String tab=SUB;TextView body;HorizontalScrollView tabs;int seasonPick=-2,segmentPick=-1;
 PlayerSidePanel(Activity a){this.a=a;}
 static boolean open(Activity a){PlayerSidePanel p=ALL.get(a);return p!=null&&p.scrim!=null&&p.scrim.getParent()!=null;}
 static void close(Activity a){PlayerSidePanel p=ALL.get(a);if(p!=null)p.dismiss();}
 static void open(Activity a,String tab){try{PlayerSidePanel p=ALL.get(a);if(p==null){p=new PlayerSidePanel(a);ALL.put(a,p);}p.show(tab==null||tab.isEmpty()?SUB:tab);}catch(Exception ignored){}}
 int dp(int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
 void show(String next){tab=next;if(EPISODES.equals(tab))segmentPick=-1;Object root=OnlineSubtitleSearch.field(a,"root");if(!(root instanceof FrameLayout))return;FrameLayout host=(FrameLayout)root;if(scrim==null)build(host);scrim.setVisibility(View.VISIBLE);fill();int w=sheet.getLayoutParams().width;sheet.setTranslationX(w>0?w:dp(480));sheet.animate().translationX(0).setDuration(PlayerMotionRules.PANEL_IN_MS).start();}

 void build(FrameLayout host){
  scrim=new FrameLayout(a);scrim.setBackgroundColor(PlayerTheme.SCRIM);scrim.setOnClickListener(v->dismiss());
  sheet=new LinearLayout(a);sheet.setOrientation(LinearLayout.VERTICAL);sheet.setClickable(true);
  GradientDrawable bg=new GradientDrawable();bg.setColor(PlayerTheme.PANEL);bg.setCornerRadius(dp(PlayerTheme.RADIUS_PANEL_DP));sheet.setBackground(bg);sheet.setPadding(dp(12),dp(12),dp(12),dp(12));
  int width=PlayerOverlayRules.panelWidthPx(a.getResources().getDisplayMetrics().widthPixels,a.getResources().getDisplayMetrics().density);
  tabs=new HorizontalScrollView(a);tabs.setHorizontalScrollBarEnabled(false);sheet.addView(tabs,new LinearLayout.LayoutParams(-1,dp(40)));
  body=new TextView(a);ScrollView scroll=new ScrollView(a);LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setTag("mp-panel-box");scroll.addView(box);sheet.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
  FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width,-1,Gravity.END);lp.topMargin=dp(8);lp.bottomMargin=dp(8);scrim.addView(sheet,lp);host.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
 }
 LinearLayout box(){return sheet==null?null:(LinearLayout)((ScrollView)sheet.getChildAt(1)).getChildAt(0);}
 void fill(){LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);ArrayList<String> idList=new ArrayList<>(),nameList=new ArrayList<>();boolean emby=TrackSelection.stream(a)!=null&&!TrackSelection.stream(a).optString("emby.item").isEmpty();
  idList.add(SUB);nameList.add("字幕");idList.add(AUDIO);nameList.add("音轨");idList.add(EPISODES);nameList.add(PlaybackReviewRules.listTitle(PlaybackReviewRules.series(PlayerOverlay.meta(a),PlayerOverlay.type(a))));idList.add(ROUTES);nameList.add("线路");
  if(EmbyPlayerUi.hasChapters(a)){idList.add(CHAPTERS);nameList.add("章节");}if(emby){idList.add(BITRATE);nameList.add("码率");}
  idList.add(SLEEP);nameList.add("定时关闭");idList.add(INFO);nameList.add("流信息");
  if(!idList.contains(tab))tab=SUB;
  String[] ids=idList.toArray(new String[0]);String[] names=nameList.toArray(new String[0]);
  for(int i=0;i<ids.length;i++){final String id=ids[i];boolean on=id.equals(tab);Button chip=new Button(a);chip.setAllCaps(false);chip.setText(names[i]);chip.setTextColor(on?0xff101820:0xffffffff);chip.setTextSize(13);chip.setTypeface(android.graphics.Typeface.create(on?"sans-serif-medium":"sans-serif-light",android.graphics.Typeface.NORMAL));chip.setLetterSpacing(0.04f);chip.setGravity(Gravity.CENTER);chip.setMinWidth(0);chip.setMinimumWidth(0);chip.setMinHeight(dp(32));chip.setMinimumHeight(dp(32));chip.setPadding(dp(12),0,dp(12),0);GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(14));g.setColor(on?0xffd7e7f5:0x00000000);chip.setBackground(g);chip.setOnClickListener(v->{tab=id;if(EPISODES.equals(id))segmentPick=-1;fill();});row.addView(chip,new LinearLayout.LayoutParams(-2,dp(32)));}
  tabs.removeAllViews();tabs.addView(row);
  LinearLayout box=box();if(box==null)return;box.removeAllViews();
  if(SUB.equals(tab))subtitles(box);else if(AUDIO.equals(tab))audio(box);else if(EPISODES.equals(tab))episodes(box);else if(CHAPTERS.equals(tab))chapters(box);else if(BITRATE.equals(tab))bitrate(box);else if(ROUTES.equals(tab))routes(box);else if(SLEEP.equals(tab))sleep(box);else info(box);
 }
 void note(LinearLayout box,String text){TextView t=new TextView(a);t.setText(text);t.setTextColor(0xb3ffffff);t.setTextSize(13);t.setPadding(0,dp(8),0,dp(8));box.addView(t);}
 void row(LinearLayout box,String label,boolean selected,Runnable action){Button t=new Button(a);t.setAllCaps(false);t.setText((selected?"● ":"○ ")+label);t.setTextColor(selected?0xff101820:0xffffffff);t.setTextSize(15);t.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);t.setMinWidth(0);t.setMinimumWidth(0);t.setMinHeight(dp(44));t.setMinimumHeight(dp(44));t.setPadding(dp(12),dp(8),dp(12),dp(8));GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(12));g.setColor(selected?0x33d7e7f5:0x00000000);t.setBackground(g);t.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(4);box.addView(t,lp);}
 void subtitles(LinearLayout box){
  // 0.1.93：获取字幕的入口统一放在字幕页最上面（原来在「更多」和本页底部各有一份）
  note(box,"获取字幕");
  row(box,"搜索在线字幕（迅雷免密钥"+(OnlineSubtitleRules.missing(OnlineSubtitleController.assrt(a.getSharedPreferences("data",0)),OnlineSubtitleController.openKey(a.getSharedPreferences("data",0)))==null?" · 射手 · OpenSubtitles）":"）"),false,()->{dismiss();OnlineSubtitleController.open(a);});
  row(box,"Whisper 语音生成字幕",false,()->{dismiss();PlayerFeatureSettings.whisper(a);});
  row(box,"用已安装的字幕插件搜索",false,()->{dismiss();Object session=OnlineSubtitleSearch.field(a,"session");if(session!=null)OnlineSubtitleSearch.start(session);});
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
 }
 void offsetBy(int direction){int next=EmbyCue.stepSubtitleOffset(a.getSharedPreferences("data",0).getInt("sub.offsetMs",0),direction);a.getSharedPreferences("data",0).edit().putInt("sub.offsetMs",next).apply();applyOffset(next);fill();}
 void applyOffset(int ms){try{Object video=OnlineSubtitleSearch.field(a,"video");Object player=video==null?null:OnlineSubtitleSearch.field(video,"player");if(player!=null)OnlineSubtitleSearch.call(player,"setSpuDelay",new Class<?>[]{long.class},ms*1000L);}catch(Exception ignored){}}
 boolean direct(){JSONObject stream=TrackSelection.stream(a);return stream==null||stream.optBoolean("emby.direct",true);}
 TextView chip(String text,Runnable action){TextView t=new TextView(a);t.setText(text);t.setTextColor(0xffffffff);t.setTextSize(14);t.setPadding(dp(12),dp(8),dp(12),dp(8));t.setOnClickListener(v->action.run());return t;}
 void audio(LinearLayout box){EmbyPlayerUi ui=EmbyPlayerUi.ALL.get(a);JSONArray audio=ui==null?new JSONArray():ui.audio;if(audio.length()==0){row(box,"播放器里的音轨",false,()->{EmbyPlayerUi.fallback(a,"audio");dismiss();});return;}for(int i=0;i<audio.length();i++){JSONObject t=audio.optJSONObject(i);if(t==null)continue;int index=t.optInt("index",-1),ordinal=t.optInt("ordinal",i);String lang=t.optString("language"),codec=t.optString("codec");row(box,ui.describe(t,true),false,()->{ui.chooseAudio(direct(),index,ordinal,lang,codec);dismiss();});}}
 Button seasonChip(String text,boolean on,Runnable action){Button b=new Button(a);b.setAllCaps(false);b.setText(text);b.setTextSize(13);b.setTextColor(on?0xff101820:0xffffffff);b.setTypeface(android.graphics.Typeface.create(on?"sans-serif-medium":"sans-serif-light",android.graphics.Typeface.NORMAL));b.setLetterSpacing(0.04f);b.setGravity(Gravity.CENTER);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(32));b.setMinimumHeight(dp(32));b.setPadding(dp(12),0,dp(12),0);GradientDrawable g=new GradientDrawable();g.setCornerRadius(dp(14));g.setColor(on?0xffd7e7f5:0x00000000);b.setBackground(g);b.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(32));lp.rightMargin=dp(6);b.setLayoutParams(lp);return b;}
 void episodes(LinearLayout box){
  JSONObject meta=PlayerOverlay.meta(a);ArrayList<JSONObject> list=EpisodeActions.list(meta);
  boolean episodic=PlaybackReviewRules.series(meta,PlayerOverlay.type(a));
  if(!episodic){note(box,PlaybackReviewRules.count(false,list.size()));String cur=PlayerOverlay.id(a);for(JSONObject video:list){String id=video.optString("id");row(box,PlaybackReviewRules.version(video),cur.equals(id),()->{dismiss();if(id.isEmpty()||id.equals(cur))return;String value=video.optString("fusion.value");if(!value.isEmpty())PlaybackSitePicker.playRoute(a,video.optString("fusion.flag"),video.optString("fusion.name",video.optString("title")),value);else AutoNext.handoff(a,meta,id,PlayerOverlay.type(a));});}if(list.isEmpty()&&EmbyPlayerUi.hasVersions(a))row(box,"选择服务器版本",false,()->{dismiss();EmbyPlayerUi.openVersions(a);});return;}
  if(list.isEmpty()){note(box,"没有可选择的剧集");if(meta.has("fusion.config"))row(box,"打开片源选集",false,()->{dismiss();PlaybackSitePicker.selectEpisode(a);});return;}
  String cur=PlayerOverlay.id(a);ArrayList<Integer> seasons=EpisodeActions.seasons(list);int at=EpisodeActions.indexOf(list,cur);
  if(seasons.size()<=1)seasonPick=-1;else{if(seasonPick<-1)seasonPick=at>=0&&list.get(at).has("season")?list.get(at).optInt("season"):-1;if(at>=0&&seasonPick>=0&&!EpisodeActions.inSeason(list.get(at),seasonPick))seasonPick=list.get(at).has("season")?list.get(at).optInt("season"):-1;if(seasonPick>=0&&!seasons.contains(seasonPick))seasonPick=-1;}
  if(seasons.size()>1){HorizontalScrollView chips=new HorizontalScrollView(a);chips.setHorizontalScrollBarEnabled(false);LinearLayout line=new LinearLayout(a);line.setOrientation(LinearLayout.HORIZONTAL);line.addView(seasonChip("全部",seasonPick<0,()->{seasonPick=-1;segmentPick=-1;fill();}));for(int s:seasons){final int season=s;line.addView(seasonChip("第 "+season+" 季",seasonPick==season,()->{seasonPick=season;segmentPick=-1;fill();}));}chips.addView(line);box.addView(chips,new LinearLayout.LayoutParams(-1,dp(40)));}
  ArrayList<JSONObject> shown=new ArrayList<>();for(JSONObject video:list)if(seasonPick<0||EpisodeActions.inSeason(video,seasonPick))shown.add(video);
  ArrayList<int[]> ranges=EpisodeActions.segments(shown.size());int here=-1;for(int i=0;i<shown.size();i++)if(cur.equals(shown.get(i).optString("id")))here=i;
  if(!ranges.isEmpty()){if(segmentPick<0||segmentPick>=ranges.size())segmentPick=here<0?0:EpisodeActions.segmentAt(shown.size(),here);HorizontalScrollView chips=new HorizontalScrollView(a);chips.setHorizontalScrollBarEnabled(false);LinearLayout line=new LinearLayout(a);line.setOrientation(LinearLayout.HORIZONTAL);for(int i=0;i<ranges.size();i++){final int pick=i;int[] range=ranges.get(i);Button chip=seasonChip(EpisodeActions.segmentLabel(range[0],range[1]),segmentPick==i,()->{segmentPick=pick;fill();});if(segmentPick==i)chip.setTag("mp-current-segment");line.addView(chip);}chips.addView(line);box.addView(chips,new LinearLayout.LayoutParams(-1,dp(40)));chips.post(()->{View target=line.findViewWithTag("mp-current-segment");if(target!=null)chips.scrollTo(Math.max(0,target.getLeft()-dp(12)),0);});}
  int from=0,to=shown.size();if(!ranges.isEmpty()){int[] range=ranges.get(Math.max(0,Math.min(segmentPick,ranges.size()-1)));from=range[0];to=range[1];}
  note(box,"共 "+shown.size()+" 集");
  for(int i=from;i<to;i++){JSONObject video=shown.get(i);String id=video.optString("id");boolean on=cur.equals(id);row(box,EpisodeActions.label(video),on,()->{dismiss();if(id.isEmpty()||id.equals(cur))return;String value=video.optString("fusion.value");if(!value.isEmpty())PlaybackSitePicker.playRoute(a,video.optString("fusion.flag"),video.optString("fusion.name",video.optString("title")),value);else AutoNext.handoff(a,meta,id,PlayerOverlay.type(a));});if(on)box.getChildAt(box.getChildCount()-1).setTag("mp-current-episode");}
  View scroller=sheet==null?null:sheet.getChildAt(1);if(scroller instanceof ScrollView){ScrollView scroll=(ScrollView)scroller;scroll.post(()->{View target=box.findViewWithTag("mp-current-episode");if(target==null)return;int y=target.getTop()-Math.max(0,(scroll.getHeight()-target.getHeight())/2);scroll.scrollTo(0,Math.max(0,y));});}
 }
 void chapters(LinearLayout box){EmbyPlayerUi ui=EmbyPlayerUi.ALL.get(a);if(ui==null||ui.chapters.length()==0){note(box,"没有章节");return;}for(int i=0;i<ui.chapters.length();i++){JSONObject c=ui.chapters.optJSONObject(i);if(c==null)continue;long at=c.optLong("startMs");row(box,c.optString("name"),false,()->{EmbyPlayback.seek(a,(int)Math.min(Integer.MAX_VALUE,at));dismiss();});}}
 void bitrate(LinearLayout box){JSONObject stream=TrackSelection.stream(a);if(stream==null||stream.optString("emby.item").isEmpty()){note(box,"只有 Emby 片源可以按码率重新请求。");return;}note(box,"选择后会按这个 MaxStreamingBitrate 重新向服务器要播放地址。");long current=a.getSharedPreferences("data",0).getLong("emby.bitrate",140_000_000L);for(long rate:RATES){String label=rate>=1_000_000?(rate/1_000_000)+" Mbps":(rate/1000)+" Kbps";row(box,label,rate==current,()->{a.getSharedPreferences("data",0).edit().putLong("emby.bitrate",rate).apply();dismiss();EmbyPlayback.replayBitrate(a,rate);});}}
 void routes(LinearLayout box){
  // 0.1.93：换片源 / 换版本 / 换线路统一放在这一页（原来分散在「更多」里）
  boolean any=false;JSONObject meta=PlayerOverlay.meta(a);
  if(meta.has("fusion.config")){row(box,"切换片源（影视仓其他站点 / 线路）",false,()->{dismiss();PlaybackSitePicker.show(a);});any=true;}
  if(EmbyPlayerUi.hasVersions(a)){row(box,"切换版本",false,()->{dismiss();EmbyPlayerUi.openVersions(a);});any=true;}
  Object more=PlayerOverlay.baseMore(a);if(more instanceof Button){final Button b=(Button)more;row(box,"自动换源 · 换下一个源 · 等待时间",false,()->{dismiss();b.performClick();});any=true;}
  embyRoutes(box,any);
 }
 void embyRoutes(LinearLayout box,boolean any){JSONObject stream=TrackSelection.stream(a);JSONObject server=stream==null?null:EmbyStore.get(a,stream.optString("emby.server"));if(server==null){if(!any)note(box,"当前片源没有可切换的线路。");return;}note(box,"Emby 服务器线路");String[] bases=EmbyCue.bases(server.optString("url"),server.optJSONArray("backups"));if(bases.length==0)note(box,"没有备用线路。");for(String base:bases){row(box,base,base.equalsIgnoreCase(server.optString("url")),()->{try{JSONObject copy=new JSONObject(server.toString());copy.put("url",base);dismiss();EmbyPlayback.playItem(a,copy,stream.optString("emby.item"),stream.optString("emby.mediaSource"),true);}catch(Exception ignored){}});}note(box,"服务器插件提供的额外线路会在打开面板时尝试读取。");EmbyPlayback.IO.execute(()->{try{JSONObject extra=EmbyClient.json(a,server,"GET","/System/Ext/ServerDomains",null);JSONArray items=extra.optJSONArray("Items");if(items==null)items=extra.optJSONArray("Domains");if(items==null||items.length()==0)return;JSONArray found=items;a.runOnUiThread(()->{if(a.isFinishing()||!ROUTES.equals(tab))return;for(int i=0;i<found.length();i++){String domain=found.optString(i);if(domain.isEmpty()&&found.optJSONObject(i)!=null)domain=found.optJSONObject(i).optString("Url",found.optJSONObject(i).optString("Domain"));if(domain.isEmpty())continue;final String url=domain;LinearLayout host=box();if(host==null)return;row(host,url,false,()->{try{JSONObject copy=new JSONObject(server.toString());copy.put("url",url);dismiss();EmbyPlayback.playItem(a,copy,stream.optString("emby.item"),stream.optString("emby.mediaSource"),true);}catch(Exception ignored){}});}});}catch(Exception ignored){}});}
 void sleep(LinearLayout box){note(box,sleepDeadline>System.currentTimeMillis()?"将在 "+Math.max(1,(sleepDeadline-System.currentTimeMillis()+59999)/60000)+" 分钟后暂停并关闭播放页。":"到点后暂停并关闭播放页。");int[] mins={0,15,30,45,60,90};for(int m:mins){row(box,m==0?"关闭定时":m+" 分钟",false,()->arm(m));}}
 void arm(int minutes){TIMER.removeCallbacks(sleepTask);sleepDeadline=0;if(minutes<=0){fill();return;}sleepDeadline=System.currentTimeMillis()+minutes*60000L;sleepTask=()->{sleepDeadline=0;try{Object video=OnlineSubtitleSearch.field(a,"video");if(video!=null)OnlineSubtitleSearch.call(video,"pause",new Class<?>[0]);}catch(Exception ignored){}if(!a.isFinishing()){PlayerOverlay.toast(a,"定时关闭");a.finish();}};TIMER.postDelayed(sleepTask,minutes*60000L);dismiss();PlayerOverlay.toast(a,minutes+" 分钟后关闭");}
 void info(LinearLayout box){StringBuilder s=new StringBuilder();Object diagnostic=OnlineSubtitleSearch.field(a,"state");if(diagnostic instanceof TextView)s.append("播放状态  ").append(((TextView)diagnostic).getText()).append("\n");JSONObject stream=TrackSelection.stream(a);Object video=OnlineSubtitleSearch.field(a,"video");String name=PlayerOverlay.name(a);if(!name.isEmpty())s.append("名称  ").append(name).append('\n');if(stream!=null){s.append("方式  ").append(stream.optBoolean("emby.direct",true)?"直接串流":"转码").append('\n');if(!stream.optString("emby.mediaSource").isEmpty())s.append("媒体源  ").append(stream.optString("emby.mediaSource")).append('\n');String url=stream.optString("url");int q=url.indexOf('?');if(q>=0)url=url.substring(0,q);if(!url.isEmpty())s.append("地址  ").append(url).append('\n');}if(video!=null){Object w=OnlineSubtitleSearch.field(video,"vw"),h=OnlineSubtitleSearch.field(video,"vh");if(w instanceof Integer&&h instanceof Integer&&(Integer)w>0)s.append("分辨率  ").append(w).append("×").append(h).append('\n');}int offset=a.getSharedPreferences("data",0).getInt("sub.offsetMs",0);s.append("字幕偏移  ").append(offset).append(" ms\n");s.append("倍速  ").append(PlayerOverlayRules.speedPill(PlaybackPrefs.speed(a.getSharedPreferences("data",0)))).append('\n');if(s.length()==0)s.append("还没有流信息");note(box,s.toString().trim());}
 void dismiss(){if(sheet!=null)sheet.animate().translationX(sheet.getWidth()>0?sheet.getWidth():dp(480)).setDuration(PlayerMotionRules.PANEL_OUT_MS).withEndAction(()->{if(scrim!=null&&scrim.getParent() instanceof ViewGroup)((ViewGroup)scrim.getParent()).removeView(scrim);scrim=null;sheet=null;}).start();else if(scrim!=null&&scrim.getParent() instanceof ViewGroup){((ViewGroup)scrim.getParent()).removeView(scrim);scrim=null;}}
}
